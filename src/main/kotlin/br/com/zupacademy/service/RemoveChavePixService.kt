package br.com.zupacademy.service

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.consumer.bcb.DeletePixKeyRequest
import br.com.zupacademy.error.exceptions.ChavePixNaoEncontradaException
import br.com.zupacademy.grpc.KeymanagerRemoveGRPCServiceGrpc
import br.com.zupacademy.grpc.RemoveChavePixRequestGRPC
import br.com.zupacademy.grpc.RemoveChavePixResponseGRPC
import br.com.zupacademy.repository.ChavePixRepository
import br.com.zupacademy.validation.ValidUUID
import io.grpc.stub.StreamObserver
import io.micronaut.http.HttpStatus
import io.micronaut.validation.Validated
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import javax.transaction.Transactional
import javax.validation.constraints.NotBlank

@Validated
@Singleton
class RemoveChavePixService(
    @Inject val repository: ChavePixRepository,
    @Inject val bcbClient: BancoCentralClient) {

    private val LOGGER = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun remove(
        @NotBlank @ValidUUID clienteId: String,
        @NotBlank @ValidUUID pixId: String
    ) {
        // 1. verifica se a chave existe no sistema
        val chave = repository.findByIdAndClienteId(pixId, clienteId)
            .orElseThrow {
                ChavePixNaoEncontradaException("Chave pix não encontrada no sistema para este cliente")
            }

        // 2. remove a chave
        repository.delete(chave)

        // 3. remove do sistema do BCB
        val request = DeletePixKeyRequest(chave.chave).also {
            LOGGER.info("Removendo chave Pix no Banco Central do Brasil (BCB): ${chave.chave}")
        }
        val bcbResponse = bcbClient.delete(chave.chave, request)

        if (bcbResponse.status != HttpStatus.OK) {
            throw IllegalStateException("Erro ao remover chave pix no Banco Central do Brasil (BCB)")
        }
    }
}