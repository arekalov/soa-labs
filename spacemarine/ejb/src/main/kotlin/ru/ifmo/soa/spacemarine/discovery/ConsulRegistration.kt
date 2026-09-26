package ru.ifmo.soa.spacemarine.discovery

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.ejb.Singleton
import jakarta.ejb.Startup
import ru.ifmo.soa.discovery.ConsulClient
import ru.ifmo.soa.discovery.ServiceNode

@Singleton
@Startup
class ConsulRegistration {
    private val consul = ConsulClient(System.getProperty("soa.consul.url", "http://127.0.0.1:27500"))

    private val node = ServiceNode(
        name = System.getProperty("soa.service.name", "spacemarine"),
        host = System.getProperty("soa.service.host", "helios.cs.ifmo.ru"),
        port = System.getProperty("soa.service.port", "27443").toInt(),
        healthPath = "/space-marines?size=1",
    )

    @PostConstruct
    fun register() = consul.register(node)

    @PreDestroy
    fun deregister() = consul.deregister(node)
}
