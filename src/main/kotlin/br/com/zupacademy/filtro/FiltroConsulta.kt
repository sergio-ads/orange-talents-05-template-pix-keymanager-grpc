package br.com.zupacademy.filtro

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.error.exceptions.ChavePixNaoEncontradaException
import br.com.zupacademy.grpc.ConsultaChavePixResponseGRPC
import br.com.zupacademy.grpc.TipoDeChaveGRPC
import br.com.zupacademy.grpc.TipoDeContaGRPC
import br.com.zupacademy.model.ChavePix
import br.com.zupacademy.repository.ChavePixRepository
import br.com.zupacademy.validation.ValidUUID
import com.google.protobuf.Timestamp
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpStatus
import org.slf4j.LoggerFactory
import java.time.ZoneOffset
import javax.validation.constraints.NotBlank

@Introspected
sealed class FiltroConsulta {

    abstract fun toModel(repository: ChavePixRepository, bcbClient: BancoCentralClient): ConsultaChavePixResponseGRPC

    @Introspected
    data class PorPixId(
        @field:NotBlank @field:ValidUUID val clienteId: String,
        @field:NotBlank @field:ValidUUID val pixId: String
    ): FiltroConsulta() {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        override fun toModel(
            repository: ChavePixRepository,
            bcbClient: BancoCentralClient
        ): ConsultaChavePixResponseGRPC {
            val chaveInfo = repository.findById(pixId)
                .filter { it.pertenceAo(clienteId) }
                .also { LOGGER.info("Consultando chave Pix '${pixId}' localmente")  }
                .orElseThrow { ChavePixNaoEncontradaException("Chave Pix não encontrada") }

            return convertToGRPC(chaveInfo)
        }
    }

    @Introspected
    data class PorChave(
        @field:NotBlank val chave: String
    ): FiltroConsulta() {
        private val LOGGER = LoggerFactory.getLogger(this::class.java)

        override fun toModel(
            repository: ChavePixRepository,
            bcbClient: BancoCentralClient
        ): ConsultaChavePixResponseGRPC {

            val chaveInfo = repository.findByChave(chave)
                .also { LOGGER.info("Consultando chave Pix '${chave}'") }
                .orElseGet {
                    LOGGER.info("Consultando chave Pix '${chave}' no Banco Central do Brasil (BCB)")

                    val response = bcbClient.findByKey(chave)
                    when(response?.status) {
                        HttpStatus.OK -> response.body()?.toModel()
                        else -> throw ChavePixNaoEncontradaException("Chave Pix não encontrada")
                    }
                }

            return convertToGRPC(chaveInfo)
        }
    }

    fun convertToGRPC(chaveInfo: ChavePix): ConsultaChavePixResponseGRPC {
        return ConsultaChavePixResponseGRPC.newBuilder()
            .setClienteId(chaveInfo.clienteId)
            .setPixId(chaveInfo.id?: "")
            .setChave(
                ConsultaChavePixResponseGRPC.ChavePix
                    .newBuilder()
                    .setTipo(TipoDeChaveGRPC.valueOf(chaveInfo.tipo.name))
                    .setChave(chaveInfo.chave)
                    .setConta(
                        ConsultaChavePixResponseGRPC.ChavePix.ContaInfo.newBuilder()
                            .setTipo(TipoDeContaGRPC.valueOf(chaveInfo.tipoDeConta.name))
                            .setInstituicao(chaveInfo.conta.instituicao)
                            .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
                            .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
                            .setAgencia(chaveInfo.conta.agencia)
                            .setNumeroDaConta(chaveInfo.conta.numeroDaConta)
                            .build()
                    )
                    .setCriadaEm(
                        chaveInfo.criadaEm.let {
                            Timestamp.newBuilder()
                                .setSeconds(it.toEpochSecond(ZoneOffset.UTC))
                                .setNanos(it.nano)
                                .build()

                        }
                    )
            )
            .build()
    }

}