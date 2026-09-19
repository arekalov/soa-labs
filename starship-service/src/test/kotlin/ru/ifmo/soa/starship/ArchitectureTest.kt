package ru.ifmo.soa.starship

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Во втором сервисе планка выше, чем в первом: прикладной слой не должен зависеть
 * даже от Spring. Именно ради этого бины объявлены отдельной конфигурацией
 * в адаптере, а не аннотациями на самих сценариях.
 */
class ArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("ru.ifmo.soa.starship")

    @Test
    @DisplayName("домен не знает ни о JPA, ни о Spring, ни о Jackson")
    fun `domain is free of frameworks`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "jakarta.persistence..",
                "org.springframework..",
                "com.fasterxml.jackson..",
            )
            .because("доменные правила обязаны проверяться без контейнера и базы")
            .check(classes)
    }

    @Test
    @DisplayName("сценарии не зависят от Spring и не знают о деталях хранения и транспорта")
    fun `application is framework free`() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "..adapter..",
            )
            .because("сценарии работают с портами; бины объявлены конфигурацией в адаптере")
            .check(classes)
    }

    @Test
    @DisplayName("домен не зависит от внешних слоёв")
    fun `domain does not depend on outer layers`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
            .because("зависимости направлены только внутрь")
            .check(classes)
    }

    @Test
    @DisplayName("веб не обращается к персистентности и к клиенту напрямую")
    fun `web goes through use cases`() {
        noClasses()
            .that().resideInAPackage("..adapter.web..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..adapter.persistence..",
                "..adapter.client..",
            )
            .because("контроллер вызывает сценарии, а не адаптеры напрямую")
            .check(classes)
    }
}
