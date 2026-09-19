import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
    war
    alias(libs.plugins.kotlin.jvm)
    // allOpen для @Component/@Configuration/@Transactional
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.depmgmt)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Без него Jackson не видит параметры конструкторов data-классов и ломается на nullability
    implementation(libs.jackson.kotlin)

    runtimeOnly(libs.postgresql)

    // Контейнер даёт внешний Tomcat — встроенный в WAR не пакуем
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.archunit.junit5)
    // См. комментарий в каталоге версий: без явного launcher'а Gradle подставит свой
    // и он разойдётся по версии с движком JUnit.
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Деплоим bootWar: он содержит SpringBootServletInitializer и разворачивается
// как обычным Tomcat, так и запуском напрямую. Обычный war при этом не нужен.
tasks.named<War>("war") {
    enabled = false
}

tasks.named<BootWar>("bootWar") {
    enabled = true
    // Имя задаёт context path: starship.war -> /starship, ровно как в спеке
    archiveFileName.set("starship.war")
}
