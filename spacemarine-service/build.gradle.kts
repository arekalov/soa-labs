plugins {
    war
    alias(libs.plugins.kotlin.jvm)
    // noArg: JPA требует конструктор без аргументов, которого у классов Kotlin нет
    alias(libs.plugins.kotlin.jpa)
    // allOpen: Hibernate строит прокси наследованием, а классы Kotlin по умолчанию final
    alias(libs.plugins.kotlin.allopen)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencies {
    // Всё это предоставляет WildFly: компилируем против API, но в WAR не пакуем.
    // JDBC-драйвер тоже не пакуем — он ставится в сервер модулем, датасорс берётся по JNDI.
    providedCompile(libs.jakartaee.api)
    providedCompile(libs.jackson.databind)
    providedCompile(libs.jackson.kotlin)
    providedCompile(libs.jackson.jsr310)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.archunit.junit5)
    // Валидация в юнит-тестах вне контейнера
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.expressly)
}

tasks.war {
    // Имя фиксировано: с версией в имени она попала бы в context path и сломала бы пути из спеки.
    // Контекст-рут задаётся в WEB-INF/jboss-web.xml, поэтому имя файла на URL не влияет.
    archiveFileName.set("spacemarine-service.war")
}
