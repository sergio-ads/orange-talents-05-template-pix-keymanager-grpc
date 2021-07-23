package br.com.zupacademy.service

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.consumer.itau.ItauContasClient
import br.com.zupacademy.grpc.ListaChavePixResponseGRPC
import br.com.zupacademy.grpc.TipoDeChaveGRPC
import br.com.zupacademy.grpc.TipoDeContaGRPC
import br.com.zupacademy.repository.ChavePixRepository
import br.com.zupacademy.validation.ValidUUID
import com.google.protobuf.Timestamp
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class ListaChavePixService(
    @Inject val repository: ChavePixRepository
) {
    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun lista(@NotBlank @ValidUUID clienteId: String): ListaChavePixResponseGRPC {
        val listaChaves = repository.findAllByClienteId(clienteId).map {
            ListaChavePixResponseGRPC.ChavePix.newBuilder()
                .setPixId(it.id)
                .setTipo(TipoDeChaveGRPC.valueOf(it.tipo.name))
                .setChave(it.chave)
                .setTipoDeConta(TipoDeContaGRPC.valueOf(it.tipoDeConta.name))
                .setCriadaEm(
                    it.criadaEm.let {
                        Timestamp.newBuilder()
                            .setSeconds(it.toEpochSecond(ZoneOffset.UTC))
                            .setNanos(it.nano)
                            .build()
                })
                .build()
        }

        return ListaChavePixResponseGRPC.newBuilder()
            .setClienteId(clienteId)
            .addAllChaves(listaChaves)
            .build()

    }

}