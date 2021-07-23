package br.com.zupacademy.endpoint

import br.com.zupacademy.consumer.bcb.BancoCentralClient
import br.com.zupacademy.error.ErrorHandler
import br.com.zupacademy.grpc.ConsultaChavePixRequestGRPC
import br.com.zupacademy.grpc.ConsultaChavePixResponseGRPC
import br.com.zupacademy.grpc.KeymanagerConsultaGRPCServiceGrpc
import br.com.zupacademy.extension.filter
import br.com.zupacademy.repository.ChavePixRepository
import io.grpc.stub.StreamObserver
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.Validator

@ErrorHandler
@Singleton
class ConsultaChaveEndpoint(
    @Inject private val bcbClient: BancoCentralClient,
    @Inject private val repository: ChavePixRepository,
    @Inject private val validator: Validator
): KeymanagerConsultaGRPCServiceGrpc.KeymanagerConsultaGRPCServiceImplBase() {

    override fun consultaGRPC(
        request: ConsultaChavePixRequestGRPC,
        responseObserver: StreamObserver<ConsultaChavePixResponseGRPC>
    ) {

        val filtro = request.filter(validator)
        val chaveInfo = filtro.toModel(repository, bcbClient)

        responseObserver.onNext(chaveInfo)
        responseObserver.onCompleted()
    }

}