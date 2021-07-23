package br.com.zupacademy.service

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.consumer.bcb.CreatePixKeyRequest
import br.com.zupacademy.consumer.itau.ItauContasClient
import br.com.zupacademy.error.exceptions.ChavePixExistenteException
import br.com.zupacademy.grpc.RegistraChavePixResponseGRPC
import br.com.zupacademy.model.ChavePix
import br.com.zupacademy.model.enums.TipoDeChave
import br.com.zupacademy.model.request.RegistraChavePixRequest
import br.com.zupacademy.repository.ChavePixRepository
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.Valid

@Validated
@Singleton
class RegistraChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val itauContasClient: ItauContasClient,
    @Inject val bcbClient: BancoCentralClient
) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun registra(@Valid novaChave: RegistraChavePixRequest): RegistraChavePixResponseGRPC {

        // 1. verifica se chave já existe no sistema
        if (repository.existsByChave(novaChave.chave))
            throw ChavePixExistenteException("Chave Pix '${novaChave.chave}' existente")

        // 2. busca dados da conta no ERP do ITAU
        val response = itauContasClient.buscaContaPorTipo(novaChave.clienteId!!, novaChave.tipoDeConta!!.name)
        val conta = response.body()?.toModel() ?: throw IllegalStateException("Cliente não encontrado no Itaú")

        // 3. grava no banco de dados
        val chave = novaChave.toModel(conta)
        repository.save(chave)

        // 4. registra chave no BCB
        if(chave.tipo == TipoDeChave.ALEATORIA)
            chave.chave = ""

        val bcbRequest = CreatePixKeyRequest.of(chave).also {
            LOGGER.info("Registrando chave Pix no Banco Central do Brasil (BCB): ${it.key}")
        }

        val bcbResponse = bcbClient.create(bcbRequest)
        if (bcbResponse.status != HttpStatus.CREATED)
            throw IllegalStateException("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)")

        // 5. atualiza chave do dominio com chave gerada pelo BCB
        chave.atualiza(bcbResponse.body()!!.key)

        return RegistraChavePixResponseGRPC.newBuilder()
            .setClienteId(chave.clienteId)
            .setPixId(chave.id)
            .build()
    }

}
