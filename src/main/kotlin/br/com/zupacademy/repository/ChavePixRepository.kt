package br.com.zupacademy.repository

import br.com.zupacademy.model.ChavePix
import io.micronaut.data.annotation.Repository
import io.micronaut.data.jpa.repository.JpaRepository
import java.util.*

@Repository
interface ChavePixRepository : JpaRepository<ChavePix, String> {

    fun existsByChave(chave: String?): Boolean

    fun findByChave(chave: String): Optional<ChavePix>

    fun findByIdAndClienteId(id: String, clienteId: String): Optional<ChavePix>

    fun findAllByClienteId(clienteId: String): List<ChavePix>

}