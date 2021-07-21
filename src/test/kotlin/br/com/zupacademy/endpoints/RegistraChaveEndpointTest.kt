package br.com.zupacademy.endpoints

import br.com.zupacademy.consumer.bcb.*
import br.com.zupacademy.consumer.itau.DadosDaContaResponse
import br.com.zupacademy.consumer.itau.InstituicaoResponse
import br.com.zupacademy.consumer.itau.ItauContasClient
import br.com.zupacademy.consumer.itau.TitularResponse
import br.com.zupacademy.error.exceptions.ChavePixExistenteException
import br.com.zupacademy.grpc.*
import br.com.zupacademy.model.ChavePix
import br.com.zupacademy.model.ContaAssociada
import br.com.zupacademy.model.enums.TipoDeChave
import br.com.zupacademy.model.enums.TipoDeConta
import br.com.zupacademy.model.request.RegistraChavePixRequest
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
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import java.time.LocalDateTime
import java.util.*
import javax.inject.Inject

@MicronautTest(transactional = false)
internal class RegistraChaveEndpointTest(
    val repository: ChavePixRepository,
    val grpcClient: KeymanagerRegistraGRPCServiceGrpc.KeymanagerRegistraGRPCServiceBlockingStub
) {

    @Inject
    lateinit var bcbClient: BancoCentralClient
    @Inject
    lateinit var itauClient: ItauContasClient
    
    val RANDOM_CLIENT_ID = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve registrar nova chave pix`() {
        // Cenário
        `when`(itauClient.buscaContaPorTipo(clienteId = RANDOM_CLIENT_ID, tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))
        `when`(bcbClient.create(createPixKeyRequest()))
            .thenReturn(HttpResponse.created(createPixKeyResponse()))

        // Executa
        val response = grpcClient.registraGRPC(
            RegistraChavePixRequestGRPC.newBuilder()
            .setClienteId(RANDOM_CLIENT_ID)
            .setTipoDeChave(TipoDeChaveGRPC.EMAIL)
            .setChave("tiago.freitas@zup.com.br")
            .setTipoDeConta(TipoDeContaGRPC.CONTA_CORRENTE)
            .build())

        // Valida
        with(response) {
            assertEquals(RANDOM_CLIENT_ID, clienteId)
            assertNotNull(pixId)
        }
    }

    @Test
    fun `deve informar chave pix existente`() {
        // Cenário
        val chavePix = repository.save(chave())

        // Executa
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registraGRPC(RegistraChavePixRequestGRPC.newBuilder()
                .setClienteId(RANDOM_CLIENT_ID)
                .setTipoDeChave(TipoDeChaveGRPC.EMAIL)
                .setChave("tiago.freitas@zup.com.br")
                .setTipoDeConta(TipoDeContaGRPC.CONTA_CORRENTE)
                .build())
        }

        // Valida
        with(exception) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Chave Pix '${chavePix.chave}' existente", status.description)
        }
    }

    @Test
    fun `deve informar conta nao encontrada no itau`() {
        // Cenário
        `when`(itauClient.buscaContaPorTipo(clienteId = RANDOM_CLIENT_ID, tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.notFound())

        // Executa
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registraGRPC(RegistraChavePixRequestGRPC.newBuilder()
                .setClienteId(RANDOM_CLIENT_ID)
                .setTipoDeChave(TipoDeChaveGRPC.EMAIL)
                .setChave("tiago.freitas@zup.com.br")
                .setTipoDeConta(TipoDeContaGRPC.CONTA_CORRENTE)
                .build())
        }

        // Valida
        with(exception) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Cliente não encontrado no Itaú", status.description)
        }
    }

    @Test
    fun `deve informar erro ao registrar no BCB`() {
        // Cenário
        `when`(itauClient.buscaContaPorTipo(clienteId = RANDOM_CLIENT_ID, tipo = "CONTA_CORRENTE"))
            .thenReturn(HttpResponse.ok(dadosDaContaResponse()))
        `when`(bcbClient.create(createPixKeyRequest()))
            .thenReturn(HttpResponse.badRequest())

        // Executa
        val exception = assertThrows<StatusRuntimeException> {
            grpcClient.registraGRPC(RegistraChavePixRequestGRPC.newBuilder()
                .setClienteId(RANDOM_CLIENT_ID)
                .setTipoDeChave(TipoDeChaveGRPC.EMAIL)
                .setChave("tiago.freitas@zup.com.br")
                .setTipoDeConta(TipoDeContaGRPC.CONTA_CORRENTE)
                .build())
        }

        // Valida
        with(exception) {
            assertEquals(Status.FAILED_PRECONDITION.code, status.code)
            assertEquals("Erro ao registrar chave Pix no Banco Central do Brasil (BCB)", status.description)
        }
    }

    @Test
    fun `deve verificar erros de validacao`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.registraGRPC(RegistraChavePixRequestGRPC.newBuilder().build())
        }

        // Valida
        with(thrown) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("Dados inválidos", status.description)
            MatcherAssert.assertThat(
                violations(),
                Matchers.containsInAnyOrder(
                    Pair("clienteId", "não deve estar em branco"),
                    Pair("tipoDeConta", "não deve ser nulo"),
                    Pair("clienteId", "não é um formato válido de UUID"),
                    Pair("tipo", "não deve ser nulo"),
                )
            )
        }
    }

    @MockBean(BancoCentralClient::class)
    fun bcbClient(): BancoCentralClient? {
        return Mockito.mock(BancoCentralClient::class.java)
    }

    @MockBean(ItauContasClient::class)
    fun itauClient(): ItauContasClient? {
        return Mockito.mock(ItauContasClient::class.java)
    }

    @Factory
    class ClientsRegistra  {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeymanagerRegistraGRPCServiceGrpc.KeymanagerRegistraGRPCServiceBlockingStub? {
            return KeymanagerRegistraGRPCServiceGrpc.newBlockingStub(channel)
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

    private fun createPixKeyRequest(): CreatePixKeyRequest {
        return CreatePixKeyRequest(PixKeyType.EMAIL, "tiago.freitas@zup.com.br", bankAccount(), owner())
    }

    private fun createPixKeyResponse(): CreatePixKeyResponse {
        return CreatePixKeyResponse(PixKeyType.EMAIL, "tiago.freitas@zup.com.br", bankAccount(), owner(), LocalDateTime.now())
    }

    private fun bankAccount(): BankAccount {
        return BankAccount(ContaAssociada.ITAU_UNIBANCO_ISPB, "0001", "889976", BankAccount.AccountType.CACC)
    }

    private fun owner(): Owner {
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