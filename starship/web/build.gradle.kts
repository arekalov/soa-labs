import org.springframework.boot.gradle.tasks.bundling.BootWar

plugins {
    war
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.depmgmt)
}

dependencies {
    // Встроенный Tomcat исключаем: сервлет-контейнер даёт WildFly.
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.apache.tomcat.embed")
    }

    // Контракт и всё, что вокруг Kotlin, лежит в EAR/lib и в WAR не пакуется.
    // Вторая копия kotlin-stdlib дала бы второй класс kotlin.Metadata, и KotlinModule
    // не распознал бы DTO из EAR/lib: конструкторы не заполнялись бы, поля приезжали null.
    compileOnly(project(":soa-api"))
    compileOnly(kotlin("stdlib"))
    compileOnly(libs.jackson.kotlin)
    compileOnly(libs.jakartaee.api)
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}

tasks.named<War>("war") {
    enabled = false
}

tasks.named<BootWar>("bootWar") {
    enabled = true
    archiveFileName.set("starship-web.war")
}

val earModule: Configuration by configurations.creating

artifacts {
    add(earModule.name, tasks.named<BootWar>("bootWar"))
}
