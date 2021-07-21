package br.com.zupacademy.endpoints

import br.com.zupacademy.error.ErrorHandler
import br.com.zupacademy.grpc.KeymanagerRegistraGRPCServiceGrpc
import br.com.zupacademy.grpc.RegistraChavePixRequestGRPC
import br.com.zupacademy.grpc.RegistraChavePixResponseGRPC
import br.com.zupacademy.model.request.toModel
import br.com.zupacademy.service.RegistraChavePixService
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RegistraChaveEndpoint(@Inject private val service: RegistraChavePixService)
    : KeymanagerRegistraGRPCServiceGrpc.KeymanagerRegistraGRPCServiceImplBase() {

    override fun registraGRPC(
        request: RegistraChavePixRequestGRPC,
        responseObserver: StreamObserver<RegistraChavePixResponseGRPC>
    ) {
        val novaChave = request.toModel()
        val chaveCriada = service.registra(novaChave)

        responseObserver.onNext(RegistraChavePixResponseGRPC.newBuilder()
                                    .setClienteId(chaveCriada.clienteId.toString())
                                    .setPixId(chaveCriada.id.toString())
                                    .build())
        responseObserver.onCompleted()
    }

}