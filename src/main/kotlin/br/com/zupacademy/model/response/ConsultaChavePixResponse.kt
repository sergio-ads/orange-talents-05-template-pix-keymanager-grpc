package br.com.zupacademy.model.response

//import br.com.zupacademy.consumer.bcb.BankAccount
//import br.com.zupacademy.consumer.bcb.Owner
//import br.com.zupacademy.consumer.bcb.PixKeyType
//import br.com.zupacademy.grpc.ConsultaChavePixResponseGRPC
//import br.com.zupacademy.grpc.TipoDeChaveGRPC
//import br.com.zupacademy.grpc.TipoDeContaGRPC
//import br.com.zupacademy.model.ChavePix
//import br.com.zupacademy.model.ContaAssociada
//import br.com.zupacademy.model.enums.Instituicoes
//import br.com.zupacademy.model.enums.TipoDeChave
//import br.com.zupacademy.model.enums.TipoDeConta
//import com.google.protobuf.Timestamp
//import java.time.LocalDateTime
//import java.time.ZoneOffset
//
//data class ConsultaChavePixResponse(
//    val keyType: PixKeyType,
//    val key: String,
//    val bankAccount: BankAccount,
//    val owner: Owner,
//    val createdAt: LocalDateTime
//) {
//    fun toModel(): ChavePixResponseBcb {
//        return ChavePixResponseBcb(
//            tipo = keyType.domainType!!,
//            chave = this.key,
//            tipoDeConta = when (this.bankAccount.accountType) {
//                BankAccount.AccountType.CACC -> TipoDeConta.CONTA_CORRENTE
//                BankAccount.AccountType.SVGS -> TipoDeConta.CONTA_POUPANCA
//            },
//            conta = ContaAssociada(
//                instituicao = Instituicoes.nome(bankAccount.participant),
//                nomeDoTitular = owner.name,
//                cpfDoTitular = owner.taxIdNumber,
//                agencia = bankAccount.branch,
//                numeroDaConta = bankAccount.accountNumber
//            )
//        )
//    }
//
//    fun toGRPC(chaveInfo: ChavePix): ConsultaChavePixResponseGRPC {
//        return ConsultaChavePixResponseGRPC.newBuilder()
//            .setClienteId(chaveInfo.clienteId?: "")
//            .setPixId(chaveInfo.id?: "")
//            .setChave(
//                ConsultaChavePixResponseGRPC.ChavePix
//                    .newBuilder()
//                    .setTipo(TipoDeChaveGRPC.valueOf(chaveInfo.tipo.name))
//                    .setChave(chaveInfo.chave)
//                    .setConta(
//                        ConsultaChavePixResponseGRPC.ChavePix.ContaInfo.newBuilder()
//                            .setTipo(TipoDeContaGRPC.valueOf(chaveInfo.tipoDeConta.name))
//                            .setInstituicao(chaveInfo.conta.instituicao)
//                            .setNomeDoTitular(chaveInfo.conta.nomeDoTitular)
//                            .setCpfDoTitular(chaveInfo.conta.cpfDoTitular)
//                            .setAgencia(chaveInfo.conta.agencia)
//                            .setNumeroDaConta(chaveInfo.conta.numeroDaConta)
//                            .build()
//                    )
//                    .setCriadaEm(
//                        chaveInfo.criadaEm.let {
//                            Timestamp.newBuilder()
//                                .setSeconds(it.toEpochSecond(ZoneOffset.UTC))
//                                .setNanos(it.nano)
//                                .build()
//
//                        }
//                    )
//            )
//            .build()
//    }
//}
//data class ChavePixResponseBcb(
//    val pixId: String? = null,
//    val clienteId: String? = null,
//    val tipo: TipoDeChave,
//    val chave: String,
//    val tipoDeConta: TipoDeConta,
//    val conta: ContaAssociada,
//    val registradaEm: LocalDateTime = LocalDateTime.now()
//) {
//    companion object {
//        fun of(chave: ChavePix): ChavePixResponseBcb {
//            return ChavePixResponseBcb(
//                pixId = chave.id,
//                clienteId = chave.clienteId,
//                tipo = chave.tipo,
//                chave = chave.chave,
//                tipoDeConta = chave.tipoDeConta,
//                conta = chave.conta,
//                registradaEm = chave.criadaEm
//            )
//        }
//    }
//}
