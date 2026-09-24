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
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.apache.tomcat.embed")
    }
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.jackson.kotlin)

    runtimeOnly(libs.postgresql)

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.mockito.kotlin)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<War>("war") {
    enabled = false
}

tasks.named<BootWar>("bootWar") {
    enabled = true
    archiveFileName.set("starship.war")
}
