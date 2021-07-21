package br.com.zupacademy.endpoints

import br.com.zupacademy.error.ErrorHandler
import br.com.zupacademy.grpc.KeymanagerRemoveGRPCServiceGrpc
import br.com.zupacademy.grpc.RemoveChavePixRequestGRPC
import br.com.zupacademy.grpc.RemoveChavePixResponseGRPC
import br.com.zupacademy.service.RemoveChavePixService
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton

@ErrorHandler
@Singleton
class RemoveChaveEndpoint(@Inject private val service: RemoveChavePixService)
    : KeymanagerRemoveGRPCServiceGrpc.KeymanagerRemoveGRPCServiceImplBase() {

    override fun removeGRPC(
        requestGRPC: RemoveChavePixRequestGRPC,
        responseObserver: StreamObserver<RemoveChavePixResponseGRPC>
    ) {
        service.remove(requestGRPC.clienteId, requestGRPC.pixId)

        responseObserver.onNext(RemoveChavePixResponseGRPC.newBuilder()
            .setClienteId(requestGRPC.clienteId)
            .setPixId(requestGRPC.pixId)
            .build())
        responseObserver.onCompleted()
    }

}