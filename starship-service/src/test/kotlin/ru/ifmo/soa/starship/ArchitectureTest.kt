package ru.ifmo.soa.starship

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("ru.ifmo.soa.starship")

    @Test
    @DisplayName("зависимости идут сверху вниз: контроллер → сервис → репозиторий и клиент")
    fun `layers depend downwards only`() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Контроллеры").definedBy("..controller..")
            .layer("Сервисы").definedBy("..service..")
            .layer("Репозитории").definedBy("..repository..")
            .layer("Клиенты").definedBy("..client..")
            .whereLayer("Контроллеры").mayNotBeAccessedByAnyLayer()
            .whereLayer("Сервисы").mayOnlyBeAccessedByLayers("Контроллеры")
            .whereLayer("Репозитории").mayOnlyBeAccessedByLayers("Сервисы")
            .whereLayer("Клиенты").mayOnlyBeAccessedByLayers("Сервисы")
            .check(classes)
    }

    @Test
    @DisplayName("модель не знает ни о вебе, ни о транспортных типах")
    fun `model stays independent`() {
        noClasses()
            .that().resideInAPackage("..model..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..controller..", "..dto..", "..service..", "..repository..", "org.springframework..")
            .check(classes)
    }

    @Test
    @DisplayName("сервис не зависит от Spring MVC: веб — деталь доставки")
    fun `service is free of web types`() {
        noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework.web..", "org.springframework.http..", "..controller..")
            .check(classes)
    }

    @Test
    @DisplayName("транспортные типы не протекают в репозиторий")
    fun `repository does not speak dto`() {
        noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..dto..", "..controller..")
            .check(classes)
    }
}
