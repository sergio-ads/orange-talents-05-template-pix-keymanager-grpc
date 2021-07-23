package br.com.zupacademy.extension

import br.com.zupacademy.grpc.RegistraChavePixRequestGRPC
import br.com.zupacademy.grpc.TipoDeChaveGRPC.*
import br.com.zupacademy.grpc.TipoDeContaGRPC.*
import br.com.zupacademy.model.enums.TipoDeChave
import br.com.zupacademy.model.enums.TipoDeConta
import br.com.zupacademy.model.request.RegistraChavePixRequest

fun RegistraChavePixRequestGRPC.toModel() : RegistraChavePixRequest {
    return RegistraChavePixRequest(
        clienteId = clienteId,
        tipo = when (tipoDeChave) {
            UNKNOWN_TIPO_CHAVE -> null
            else -> TipoDeChave.valueOf(tipoDeChave.name)
        },
        chave = chave,
        tipoDeConta = when (tipoDeConta) {
            UNKNOWN_TIPO_CONTA -> null
            else -> TipoDeConta.valueOf(tipoDeConta.name)
        }
    )
}