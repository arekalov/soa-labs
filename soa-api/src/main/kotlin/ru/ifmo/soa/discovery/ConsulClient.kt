package ru.ifmo.soa.discovery

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

private val TIMEOUT: Duration = Duration.ofSeconds(5)

/** Адрес узла, под которым сервис виден потребителям. */
data class ServiceNode(
    val name: String,
    val host: String,
    val port: Int,
    val healthPath: String,
) {
    val id: String get() = "$name-$port"
}

/**
 * Регистрация в реестре и поиск живых экземпляров.
 *
 * Проверку здоровья выполняет сам Consul, поэтому сертификат ему не подсунуть:
 * ходит по HTTPS с отключённой проверкой имени.
 */
class ConsulClient(private val baseUrl: String) {
    private val log: Logger = Logger.getLogger(ConsulClient::class.java.name)
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()

    fun register(node: ServiceNode) {
        val body = """
            {
              "ID": "${node.id}",
              "Name": "${node.name}",
              "Address": "${node.host}",
              "Port": ${node.port},
              "Check": {
                "HTTP": "https://${node.host}:${node.port}${node.healthPath}",
                "Interval": "10s",
                "Timeout": "5s",
                "TLSSkipVerify": true,
                "DeregisterCriticalServiceAfter": "1m"
              }
            }
        """.trimIndent()
        send("PUT", "/v1/agent/service/register", body, "регистрация ${node.id}")
    }

    fun deregister(node: ServiceNode) =
        send("PUT", "/v1/agent/service/deregister/${node.id}", "", "снятие регистрации ${node.id}")

    /** Живые экземпляры сервиса: адреса вида `https://host:port`. Пустой список — сервиса нет. */
    fun healthyInstances(serviceName: String): List<String> {
        val response = request("GET", "/v1/health/service/$serviceName?passing=true", "") ?: return emptyList()
        if (response.statusCode() != 200) return emptyList()
        return ADDRESS.findAll(response.body()).map { "https://${it.groupValues[1]}:${it.groupValues[2]}" }.toList()
    }

    private fun send(method: String, path: String, body: String, what: String) {
        val response = request(method, path, body)
        when {
            response == null -> log.warning("Consul недоступен: $what не выполнено")
            response.statusCode() != 200 -> log.warning("Consul отклонил $what: ${response.statusCode()}")
            else -> log.info("Consul: $what выполнено")
        }
    }

    private fun request(method: String, path: String, body: String): HttpResponse<String>? = try {
        http.send(
            HttpRequest.newBuilder(URI.create("$baseUrl$path"))
                .timeout(TIMEOUT)
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
    } catch (e: Exception) {
        log.log(Level.FINE, "Запрос к Consul не удался", e)
        null
    }

    private companion object {
        val ADDRESS = Regex("\"ServiceAddress\":\"([^\"]*)\".*?\"ServicePort\":(\\d+)")
    }
}
