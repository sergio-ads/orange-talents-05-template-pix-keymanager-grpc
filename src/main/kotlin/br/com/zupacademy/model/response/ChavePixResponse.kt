package br.com.zupacademy.model.response

import br.com.zupacademy.model.ChavePix
import br.com.zupacademy.model.ContaAssociada
import br.com.zupacademy.model.enums.TipoDeChave
import br.com.zupacademy.model.enums.TipoDeConta
import java.time.LocalDateTime
import java.util.*

data class ChavePixResponse(
    val pixId: String? = null,
    val clienteId: String? = null,
    val tipo: TipoDeChave,
    val chave: String,
    val tipoDeConta: TipoDeConta,
    val conta: ContaAssociada,
    val registradaEm: LocalDateTime = LocalDateTime.now()
) {

    companion object {
        fun of(chave: ChavePix): ChavePixResponse {
            return ChavePixResponse(
                pixId = chave.id,
                clienteId = chave.clienteId,
                tipo = chave.tipo,
                chave = chave.chave,
                tipoDeConta = chave.tipoDeConta,
                conta = chave.conta,
                registradaEm = chave.criadaEm
            )
        }
    }
}