package ru.ifmo.soa.starship.adapter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.ifmo.soa.starship.application.port.SpaceMarineGateway
import ru.ifmo.soa.starship.application.port.StarshipRepository
import ru.ifmo.soa.starship.application.usecase.CreateStarship
import ru.ifmo.soa.starship.application.usecase.UnloadSpaceMarine

/**
 * Сборка сценариев.
 *
 * Бины объявлены здесь, а не аннотациями на самих классах, чтобы прикладной слой
 * не зависел от Spring. Это не педантизм: благодаря такому разделению архитектурный
 * тест может механически запретить импорт фреймворка внутрь сценариев, а сами
 * сценарии проверяются обычными юнит-тестами без поднятия контекста.
 */
@Configuration
class UseCaseConfiguration {

    @Bean
    fun createStarship(repository: StarshipRepository): CreateStarship =
        CreateStarship(repository)

    @Bean
    fun unloadSpaceMarine(
        repository: StarshipRepository,
        spaceMarines: SpaceMarineGateway,
    ): UnloadSpaceMarine = UnloadSpaceMarine(repository, spaceMarines)
}
