package br.com.zupacademy.endpoint

import br.com.zupacademy.error.ErrorHandler
import br.com.zupacademy.grpc.KeymanagerRegistraGRPCServiceGrpc
import br.com.zupacademy.grpc.RegistraChavePixRequestGRPC
import br.com.zupacademy.grpc.RegistraChavePixResponseGRPC
import br.com.zupacademy.extension.toModel
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
        val response = service.registra(novaChave)

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

}