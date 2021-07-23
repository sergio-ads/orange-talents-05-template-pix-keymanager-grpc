package br.com.zupacademy.endpoint

import br.com.zupacademy.consumer.bcb.*
import br.com.zupacademy.grpc.ConsultaChavePixRequestGRPC
import br.com.zupacademy.grpc.KeymanagerConsultaGRPCServiceGrpc
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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*

@MicronautTest(transactional = false)
internal class ConsultaChaveEndpointTest(
    private val bcbClient: BancoCentralClient,
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerConsultaGRPCServiceGrpc.KeymanagerConsultaGRPCServiceBlockingStub
) {

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
    fun `deve consultar a chave do cliente por chave existente`() {
        // Executa
        val response = grpcClient.consultaGRPC(
            ConsultaChavePixRequestGRPC.newBuilder()
                .setChave(CHAVE_EXISTENTE.chave)
                .build()
        )

        // Valida
        with(response) {
            assertEquals(response.chave.chave, CHAVE_EXISTENTE.chave)
        }
    }

    @Test
    fun `deve consultar a chave do cliente por chave nao existente`() {
        // Cenário
        val CHAVE_RANDOM = "outro.email@zup.com.br"
        `when`(bcbClient.findByKey(CHAVE_RANDOM))
            .thenReturn(HttpResponse.ok(
                consultaPixKeyResponse(
                    PixKeyType.valueOf(CHAVE_EXISTENTE.tipo.toString()),
                    CHAVE_RANDOM)))

        // Executa
        val response = grpcClient.consultaGRPC(
            ConsultaChavePixRequestGRPC.newBuilder()
                .setChave(CHAVE_RANDOM)
                .build()
        )

        // Valida
        with(response) {
            assertEquals(response.chave.chave, CHAVE_RANDOM)
        }
    }

    @Test
    fun `deve consultar a chave do cliente por client e pix id`() {
        // Cenário
        `when`(bcbClient.findByKey(CHAVE_EXISTENTE.chave))
            .thenReturn(HttpResponse.ok(
                consultaPixKeyResponse(
                    PixKeyType.valueOf(CHAVE_EXISTENTE.tipo.toString()),
                    CHAVE_EXISTENTE.chave)))

        // Executa
        val response = grpcClient.consultaGRPC(
            ConsultaChavePixRequestGRPC.newBuilder()
                .setPixId(
                    ConsultaChavePixRequestGRPC.FiltroPixId.newBuilder()
                        .setClienteId(CHAVE_EXISTENTE.clienteId)
                        .setPixId(CHAVE_EXISTENTE.id)
                        .build())
                .build()
        )

        // Valida
        with(response) {
            assertEquals(response.chave.chave, CHAVE_EXISTENTE.chave)
            assertEquals(response.clienteId, CHAVE_EXISTENTE.clienteId)
            assertEquals(response.pixId, CHAVE_EXISTENTE.id)
        }
    }

    @Test
    fun `deve informar chave nao encontrada por client e pix id`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consultaGRPC(ConsultaChavePixRequestGRPC.newBuilder()
                .setPixId(ConsultaChavePixRequestGRPC.FiltroPixId.newBuilder()
                    .setClienteId(UUID.randomUUID().toString())
                    .setPixId(UUID.randomUUID().toString())
                    .build()
                )
                .build()
            )
        }

        // Valida
        with(thrown) {
            assertEquals(Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve informar chave nao encontrada por chave`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consultaGRPC(
                ConsultaChavePixRequestGRPC.newBuilder()
                    .setChave(UUID.randomUUID().toString())
                    .build()
            )
        }

        // Valida
        with(thrown) {
            assertEquals(io.grpc.Status.NOT_FOUND.code, status.code)
            assertEquals("Chave Pix não encontrada", status.description)
        }
    }

    @Test
    fun `deve verificar erros de validacao por client e pix id`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consultaGRPC(ConsultaChavePixRequestGRPC.newBuilder()
                .setPixId(ConsultaChavePixRequestGRPC.FiltroPixId.newBuilder()
                    .setClienteId("")
                    .setPixId("")
                    .build()
                )
                .build()
            )
        }

        // Valida
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(violations(),
                containsInAnyOrder(
                    Pair("clienteId", "não deve estar em branco"),
                    Pair("clienteId", "não é um formato válido de UUID"),
                    Pair("pixId", "não deve estar em branco"),
                    Pair("pixId", "não é um formato válido de UUID"),
                )
            )
        }
    }

    @Test
    fun `deve verificar erros de validacao por chave`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consultaGRPC(
                ConsultaChavePixRequestGRPC.newBuilder()
                    .setChave("")
                    .build()
            )
        }

        // Valida
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            assertThat(violations(),
                containsInAnyOrder(
                    Pair("chave", "não deve estar em branco")
                )
            )
        }
    }

    @Test
    fun `deve informar chave invalida`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.consultaGRPC(
                ConsultaChavePixRequestGRPC.newBuilder().build()
            )
        }

        // Valida
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Chave Pix inválida ou não informada", status.description)
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return Mockito.mock(BancoCentralClient::class.java)
    }

    @Factory
    class ClientsConsulta  {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeymanagerConsultaGRPCServiceGrpc.KeymanagerConsultaGRPCServiceBlockingStub? {
            return KeymanagerConsultaGRPCServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun consultaPixKeyResponse(
        keyType: PixKeyType,
        key: String
    ): FindPixKeyResponse {
        return FindPixKeyResponse(
            keyType = keyType,
            key = key,
            bankAccount = bankAccount(),
            owner = owner(),
            createdAt = LocalDateTime.now()
        )
    }
    fun bankAccount(): BankAccount {
        return BankAccount(ContaAssociada.ITAU_UNIBANCO_ISPB, "0001", "889976", BankAccount.AccountType.CACC)
    }

    fun owner(): Owner {
        return Owner(Owner.OwnerType.NATURAL_PERSON, "Tiago de Freitas", "64370752019")
    }

    private fun chave(): ChavePix {
        return ChavePix( // Homenagem ao Tiago
            clienteId = UUID.randomUUID().toString(),
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

}