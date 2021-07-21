package br.com.zupacademy
import io.micronaut.runtime.EmbeddedApplication
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.inject.Inject

@MicronautTest
class KeymanagerTest {

    @Inject
    lateinit var application: EmbeddedApplication<*>

    @Test
    fun `aplicacao rodando`() {
        assertTrue(application.isRunning)
    }

}
