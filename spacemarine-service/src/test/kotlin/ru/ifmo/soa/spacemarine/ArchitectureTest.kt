package ru.ifmo.soa.spacemarine

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Направление зависимостей закреплено тестом, а не договорённостью.
 *
 * Без такой проверки слоистость разъезжается на второй неделе: достаточно одного импорта
 * JPA в домене, чтобы бизнес-правила стало невозможно проверить без базы. На защите
 * этот тест — предъявляемый аргумент, а не обещание.
 */
class ArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("ru.ifmo.soa.spacemarine")

    @Test
    @DisplayName("домен не знает ни о JPA, ни о веб-слое, ни о Jackson")
    fun `domain is free of frameworks`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "jakarta.persistence..",
                "jakarta.ws.rs..",
                "jakarta.enterprise..",
                "com.fasterxml.jackson..",
                "org.hibernate..",
            )
            .because("доменные правила обязаны проверяться без контейнера и базы")
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
    @DisplayName("прикладной слой не зависит от адаптеров и не знает о JPA и HTTP")
    fun `application does not depend on adapters`() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..adapter..",
                "jakarta.persistence..",
                "jakarta.ws.rs..",
            )
            .because("сценарии работают с портами, а не с конкретными технологиями")
            .check(classes)
    }

    @Test
    @DisplayName("веб-слой не обращается к персистентности напрямую")
    fun `web does not reach into persistence`() {
        noClasses()
            .that().resideInAPackage("..adapter.web..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.persistence..")
            .because("веб общается с приложением через сценарии, а не через хранилище")
            .check(classes)
    }
}
