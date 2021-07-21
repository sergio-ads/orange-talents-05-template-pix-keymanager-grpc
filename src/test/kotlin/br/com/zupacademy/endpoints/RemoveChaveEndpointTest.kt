package br.com.zupacademy.endpoints

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.consumer.bcb.DeletePixKeyRequest
import br.com.zupacademy.consumer.bcb.DeletePixKeyResponse
import br.com.zupacademy.consumer.itau.DadosDaContaResponse
import br.com.zupacademy.consumer.itau.InstituicaoResponse
import br.com.zupacademy.consumer.itau.TitularResponse
import br.com.zupacademy.grpc.KeymanagerRemoveGRPCServiceGrpc
import br.com.zupacademy.grpc.RemoveChavePixRequestGRPC
import br.com.zupacademy.model.ChavePix
import br.com.zupacademy.model.ContaAssociada
import br.com.zupacademy.model.enums.TipoDeChave
import br.com.zupacademy.model.enums.TipoDeConta
import br.com.zupacademy.repository.ChavePixRepository
import br.com.zupacademy.utils.violations
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.http.HttpResponse
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RemoveChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerRemoveGRPCServiceGrpc.KeymanagerRemoveGRPCServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BancoCentralClient

    lateinit var CHAVE_EXISTENTE: ChavePix

    @BeforeEach
    fun setUp() {
        CHAVE_EXISTENTE = repository.save(chave())
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve remover chave pix`() {
        // Cenário
        `when`(bcbClient.delete(CHAVE_EXISTENTE.chave, deletePixKeyRequest()))
            .thenReturn(HttpResponse.ok(deletePixKeyResponse()))

        // Executa
        val response = grpcClient.removeGRPC(
            RemoveChavePixRequestGRPC.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id)
                .setClienteId(CHAVE_EXISTENTE.clienteId)
                .build()
        )

        // Valida
        with(response) {
            assertEquals(CHAVE_EXISTENTE.clienteId, clienteId)
            assertEquals(CHAVE_EXISTENTE.id, pixId)
        }
    }

    @Test
    fun `deve informar chave pix nao encontrada`() {
        // Executa
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.removeGRPC(RemoveChavePixRequestGRPC.newBuilder()
                .setClienteId(UUID.randomUUID().toString())
                .setPixId(UUID.randomUUID().toString())
                .build())
        }

        // Valida
        with(exception) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave pix não encontrada no sistema para este cliente", status.description)
        }
    }

    @Test
    fun `deve informar erro ao remover do BCB`() {
        // Cenário
        `when`(bcbClient.delete(Mockito.anyString(), any()))
            .thenReturn(HttpResponse.notFound())

        // Executa
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.removeGRPC(RemoveChavePixRequestGRPC.newBuilder()
                .setPixId(CHAVE_EXISTENTE.id)
                .setClienteId(CHAVE_EXISTENTE.clienteId)
                .build())
        }

        // Valida
        with(exception) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao remover chave pix no Banco Central do Brasil (BCB)", status.description)
        }
    }


    @Test
    fun `deve verificar erros de validacao`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.removeGRPC(RemoveChavePixRequestGRPC.newBuilder().build())
        }

        // Valida
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(violations(),
                containsInAnyOrder(
                    Pair("pixId", "não deve estar em branco"),
                    Pair("clienteId", "não deve estar em branco"),
                    Pair("pixId", "não é um formato válido de UUID"),
                    Pair("clienteId", "não é um formato válido de UUID"),
                )
            )
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return Mockito.mock(BancoCentralClient::class.java)
    }

    @Factory
    class ClientsRemove  {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeymanagerRemoveGRPCServiceGrpc.KeymanagerRemoveGRPCServiceBlockingStub? {
            return KeymanagerRemoveGRPCServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun dadosDaContaResponse(): DadosDaContaResponse {
        return DadosDaContaResponse(
            tipo = "CONTA_CORRENTE",
            instituicao = InstituicaoResponse(nome = "ITAÚ UNIBANCO S.A.", ispb = ContaAssociada.ITAU_UNIBANCO_ISPB),
            agencia = "0001",
            numero = "889976",
            titular = TitularResponse(nome = "Tiago de Freitas", cpf = "64370752019")
        )
    }

    private fun chave(): ChavePix {
        return ChavePix( // Homenagem ao Tiago
            clienteId = "bc35591d-b547-4151-a325-4a9d2cd19614",
            tipo = TipoDeChave.EMAIL,
            chave = "tiago.freitas@zup.com.br",
            tipoDeConta = TipoDeConta.CONTA_CORRENTE,
            conta = ContaAssociada(
                instituicao = "ITAÚ UNIBANCO S.A.",
                nomeDoTitular = "Tiago de Freitas",
                cpfDoTitular = "64370752019",
                agencia = "0001",
                numeroDaConta = "889976"
            )
        )
    }

    private fun deletePixKeyRequest(): DeletePixKeyRequest {
        return DeletePixKeyRequest(CHAVE_EXISTENTE.chave)
    }

    private fun deletePixKeyResponse(): DeletePixKeyResponse {
        return DeletePixKeyResponse(CHAVE_EXISTENTE.chave, ContaAssociada.ITAU_UNIBANCO_ISPB, LocalDateTime.now())
    }

}