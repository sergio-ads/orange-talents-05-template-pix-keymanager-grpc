package br.com.zupacademy.extension

import br.com.zupacademy.filtro.FiltroConsulta
import br.com.zupacademy.grpc.ConsultaChavePixRequestGRPC
import br.com.zupacademy.grpc.ConsultaChavePixRequestGRPC.FiltroCase.*
import javax.validation.ConstraintViolationException
import javax.validation.Validator

fun ConsultaChavePixRequestGRPC.filter(
    validator: Validator
): FiltroConsulta {

    val filtro = when(filtroCase!!) {
        PIXID -> FiltroConsulta.PorPixId(pixId.clienteId, pixId.pixId)
        CHAVE -> FiltroConsulta.PorChave(chave)
        FILTRO_NOT_SET -> throw IllegalArgumentException("Chave Pix inválida ou não informada")
    }

    val violations = validator.validate(filtro)
    if (violations.isNotEmpty()) {
        throw ConstraintViolationException(violations);
    }

    return filtro
}