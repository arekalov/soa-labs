package ru.ifmo.soa.starship.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import tools.jackson.databind.ObjectMapper
import javax.net.ssl.SSLContext
import ru.ifmo.soa.starship.client.MuleStarshipClient
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory

@Configuration
class BusinessInterfaceConfig {
    @Bean
    fun muleStarshipClient(environment: Environment, mapper: ObjectMapper): MuleStarshipClient {
        fun setting(property: String, variable: String): String? =
            environment.getProperty(property) ?: System.getenv(variable)

        val endpoint = requireNotNull(setting("starship.mule.url", "STARSHIP_MULE_URL")) {
            "starship.mule.url must point to the Mule JSON route for this replica"
        }
        val uri = java.net.URI(endpoint)
        require(uri.scheme == "https" && uri.host != null) { "starship.mule.url must be an absolute HTTPS URL" }
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
        val tls = SSLContext.getInstance("TLS").apply { init(null, trustManagers, null) }
        return MuleStarshipClient(endpoint, tls.socketFactory, mapper)
    }
}
