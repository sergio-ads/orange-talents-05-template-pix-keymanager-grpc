package br.com.zupacademy.endpoint

import br.com.zupacademy.error.ErrorHandler
import br.com.zupacademy.grpc.KeymanagerListaGRPCServiceGrpc
import br.com.zupacademy.grpc.ListaChavePixRequestGRPC
import br.com.zupacademy.grpc.ListaChavePixResponseGRPC
import br.com.zupacademy.repository.ChavePixRepository
import br.com.zupacademy.service.ListaChavePixService
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class ListaChaveEndpoint(
    @Inject private val repository: ChavePixRepository,
    @Inject private val service: ListaChavePixService
): KeymanagerListaGRPCServiceGrpc.KeymanagerListaGRPCServiceImplBase() {

    override fun listaGRPC(
        request: ListaChavePixRequestGRPC,
        responseObserver: StreamObserver<ListaChavePixResponseGRPC>
    ) {
        val response = service.lista(request.clienteId)

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}