package br.com.zupacademy.endpoint

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.grpc.KeymanagerListaGRPCServiceGrpc
import br.com.zupacademy.grpc.ListaChavePixRequestGRPC
import br.com.zupacademy.grpc.RemoveChavePixRequestGRPC
import br.com.zupacademy.grpc.TipoDeChaveGRPC
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
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.*
import org.mockito.Mockito
import java.util.*


@MicronautTest(transactional = false)
internal class ListaChaveEndpointTest(
    private val repository: ChavePixRepository,
    private val grpcClient: KeymanagerListaGRPCServiceGrpc.KeymanagerListaGRPCServiceBlockingStub
) {

    val RANDOM_CLIENT_ID = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        repository.save(chave(TipoDeChave.EMAIL, "tiago.freitas@zup.com.br"))
        repository.save(chave(TipoDeChave.CELULAR, "+5561991001234"))
    }

    @AfterEach
    fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `deve listar todas as chaves do cliente`() {
        // Executa
        val response = grpcClient.listaGRPC(
            ListaChavePixRequestGRPC.newBuilder()
                .setClienteId(RANDOM_CLIENT_ID)
                .build()
        )

        // Valida
        with(response.chavesList) {
            assertThat(this, hasSize(2))
            assertThat(this.map { Pair(it.tipo, it.chave) }.toList(),
                containsInAnyOrder(
                    Pair(TipoDeChaveGRPC.EMAIL, "tiago.freitas@zup.com.br"),
                    Pair(TipoDeChaveGRPC.CELULAR, "+5561991001234")
                )
            )
        }
    }

    @Test
    fun `deve listar nenhuma chave do cliente`() {
        // Executa
        val response = grpcClient.listaGRPC(
            ListaChavePixRequestGRPC.newBuilder()
                .setClienteId(UUID.randomUUID().toString())
                .build()
        )

        // Valida
        with(response.chavesList) {
            assertThat(this, hasSize(0))
        }
    }

    @Test
    fun `deve verificar erros de validacao`() {
        // Executa
        val thrown = assertThrows<StatusRuntimeException> {
            grpcClient.listaGRPC(ListaChavePixRequestGRPC.newBuilder().build())
        }

        // Valida
        with(thrown) {
            Assertions.assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            Assertions.assertEquals("Dados inválidos", status.description)
            assertThat(violations(),
                containsInAnyOrder(
                    Pair("clienteId", "não deve estar em branco"),
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
    class ClientsLista  {
        @Bean
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                KeymanagerListaGRPCServiceGrpc.KeymanagerListaGRPCServiceBlockingStub? {
            return KeymanagerListaGRPCServiceGrpc.newBlockingStub(channel)
        }
    }

    private fun chave(
        tipoDeChave: TipoDeChave,
        chave: String
    ): ChavePix {
        return ChavePix( // Homenagem ao Tiago
            clienteId = RANDOM_CLIENT_ID,
            tipo = tipoDeChave,
            chave = chave,
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