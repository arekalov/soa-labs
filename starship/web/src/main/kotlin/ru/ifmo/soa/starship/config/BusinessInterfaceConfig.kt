package ru.ifmo.soa.starship.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import ru.ifmo.soa.starship.api.StarshipRemote
import ru.ifmo.soa.starship.soap.SoapStarshipClient
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory

@Configuration
class BusinessInterfaceConfig {
    @Bean
    fun starshipRemote(environment: Environment): StarshipRemote {
        fun setting(property: String, variable: String): String? =
            environment.getProperty(property) ?: System.getenv(variable)

        val offset = environment.getProperty("jboss.socket.binding.port-offset", Int::class.java, 0)
        val endpoint = setting("starship.soap.url", "STARSHIP_SOAP_URL")
            ?: "https://localhost:${8444 + offset}/starship-soap/StarshipService"
        val uri = java.net.URI(endpoint)
        require(uri.scheme == "https" && uri.host != null) { "starship.soap.url must be an absolute HTTPS URL" }
        val trustManagers = setting("soa.truststore.path", "SOA_TRUSTSTORE_PATH")?.let { path ->
            val password = requireNotNull(setting("soa.truststore.password", "SOA_TRUSTSTORE_PASSWORD")) {
                "SOA_TRUSTSTORE_PASSWORD is required with SOA_TRUSTSTORE_PATH"
            }.toCharArray()
            try {
                val store = KeyStore.getInstance("PKCS12")
                Files.newInputStream(Path.of(path)).use { store.load(it, password) }
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(store) }.trustManagers
            } finally {
                password.fill('\u0000')
            }
        }
        return SoapStarshipClient(endpoint, trustManagers)
    }
}
