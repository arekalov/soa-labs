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

// Плагин kotlin-jpa — это noArg с преднастройкой под JPA; дополняем его аннотацией CDI.
noArg {
    annotation("jakarta.enterprise.context.ApplicationScoped")
}

dependencies {
    // Всё это предоставляет WildFly: компилируем против API, но в WAR не пакуем.
    // JDBC-драйвер тоже не пакуем — он ставится в сервер модулем, датасорс берётся по JNDI.
    providedCompile(libs.jakartaee.api)
    providedCompile(libs.jackson.databind)
    providedCompile(libs.jackson.jsr310)


    implementation(libs.jackson.kotlin)
    implementation(kotlin("reflect"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.expressly)
}

tasks.war {
    archiveFileName.set("spacemarine-service.war")
}
