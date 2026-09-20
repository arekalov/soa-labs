package ru.ifmo.soa.spacemarine

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Слои проверяются механически: договорённость, которую нельзя нарушить незаметно,
 * стоит дороже той, что записана только в README.
 */
class ArchitectureTest {
    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("ru.ifmo.soa.spacemarine")

    @Test
    @DisplayName("зависимости идут сверху вниз: контроллер → сервис → репозиторий")
    fun `layers depend downwards only`() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Контроллеры").definedBy("..controller..")
            .layer("Сервисы").definedBy("..service..")
            .layer("Репозитории").definedBy("..repository..")
            .whereLayer("Контроллеры").mayNotBeAccessedByAnyLayer()
            .whereLayer("Сервисы").mayOnlyBeAccessedByLayers("Контроллеры")
            .whereLayer("Репозитории").mayOnlyBeAccessedByLayers("Сервисы")
            .check(classes)
    }

    @Test
    @DisplayName("модель не знает ни о вебе, ни о транспортных типах")
    fun `model stays independent`() {
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..model..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..dto..", "..service..", "..repository..", "jakarta.ws.rs..")
            .check(classes)
    }

    @Test
    @DisplayName("сервис не зависит от JAX-RS: веб — деталь доставки, а не бизнес-правило")
    fun `service is free of web types`() {
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.ws.rs..", "..controller..")
            .check(classes)
    }

    @Test
    @DisplayName("транспортные типы не протекают в репозиторий")
    fun `repository does not speak dto`() {
        com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..dto..", "..controller..")
            .check(classes)
    }
}
