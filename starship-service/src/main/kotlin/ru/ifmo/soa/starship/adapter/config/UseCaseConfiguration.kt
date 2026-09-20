package ru.ifmo.soa.starship.adapter.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.ifmo.soa.starship.application.port.SpaceMarineGateway
import ru.ifmo.soa.starship.application.port.StarshipRepository
import ru.ifmo.soa.starship.application.usecase.BoardSpaceMarine
import ru.ifmo.soa.starship.application.usecase.CreateStarship
import ru.ifmo.soa.starship.application.usecase.CrewReconciler
import ru.ifmo.soa.starship.application.usecase.CreateStarshipWithGeneratedId
import ru.ifmo.soa.starship.application.usecase.DeleteStarship
import ru.ifmo.soa.starship.application.usecase.GetStarship
import ru.ifmo.soa.starship.application.usecase.ListStarships
import ru.ifmo.soa.starship.application.usecase.RenameStarship
import ru.ifmo.soa.starship.application.usecase.UnloadSpaceMarine

/**
 * Сборка сценариев.
 *
 * Бины объявлены здесь, а не аннотациями на самих классах, чтобы прикладной слой
 * не зависел от Spring. Благодаря этому архитектурный тест может механически запретить
 * импорт фреймворка внутрь сценариев, а сами сценарии проверяются без контекста.
 */
@Configuration
class UseCaseConfiguration {

    @Bean fun createStarship(repository: StarshipRepository) = CreateStarship(repository)

    @Bean fun createStarshipWithGeneratedId(repository: StarshipRepository) = CreateStarshipWithGeneratedId(repository)

    @Bean fun crewReconciler(repository: StarshipRepository, spaceMarines: SpaceMarineGateway) =
        CrewReconciler(repository, spaceMarines)

    @Bean fun listStarships(repository: StarshipRepository, crew: CrewReconciler) = ListStarships(repository, crew)

    @Bean fun getStarship(repository: StarshipRepository, crew: CrewReconciler) = GetStarship(repository, crew)

    @Bean fun renameStarship(repository: StarshipRepository) = RenameStarship(repository)

    @Bean fun deleteStarship(repository: StarshipRepository) = DeleteStarship(repository)

    @Bean fun boardSpaceMarine(repository: StarshipRepository, spaceMarines: SpaceMarineGateway) =
        BoardSpaceMarine(repository, spaceMarines)

    @Bean fun unloadSpaceMarine(repository: StarshipRepository, spaceMarines: SpaceMarineGateway) =
        UnloadSpaceMarine(repository, spaceMarines)
}
