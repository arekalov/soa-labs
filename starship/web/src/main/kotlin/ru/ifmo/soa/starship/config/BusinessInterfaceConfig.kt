package ru.ifmo.soa.starship.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.ifmo.soa.starship.api.StarshipRemote
import javax.naming.InitialContext

private const val JNDI_NAME =
    "java:global/starship/starship-ejb/StarshipBean!ru.ifmo.soa.starship.api.StarshipRemote"

/**
 * Бизнес-компонент достаётся из JNDI, а не через @EJB: аннотацию обрабатывает контейнер
 * и только у своих компонентов, а контроллер создаёт Spring.
 */
@Configuration
class BusinessInterfaceConfig {
    @Bean
    fun starshipRemote(): StarshipRemote = InitialContext().lookup(JNDI_NAME) as StarshipRemote
}
