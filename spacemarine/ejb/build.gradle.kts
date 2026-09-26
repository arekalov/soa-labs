plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.allopen)
}

allOpen {
    // Спека EJB требует, чтобы класс бина не был final, а классы Kotlin final по умолчанию:
    // иначе WFLYEJB0119 и бин молча не регистрируется.
    annotation("jakarta.ejb.Stateless")
    annotation("jakarta.ejb.Singleton")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

// Weld строит прокси нормально-скоупного бина наследованием и зовёт конструктор без
// аргументов; у класса Kotlin с параметрами такого нет, и развёртывание падает WELD-001435.
noArg {
    annotation("jakarta.enterprise.context.ApplicationScoped")
}

dependencies {
    implementation(project(":soa-api"))
    compileOnly(libs.jakartaee.api)
    compileOnly(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.jakartaee.api)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.jsr310)
}

tasks.jar {
    archiveFileName.set("spacemarine-ejb.jar")
}
