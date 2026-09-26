plugins {
    war
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.smallrye.openapi)
}

dependencies {
    // Всё это лежит в EAR/lib или предоставлено WildFly — в WAR не пакуем.
    providedCompile(project(":soa-api"))
    providedCompile(kotlin("stdlib"))
    providedCompile(libs.jakartaee.api)
    providedCompile(libs.jackson.databind)
    providedCompile(libs.jackson.jsr310)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
}

val earModule: Configuration by configurations.creating

artifacts {
    add(earModule.name, tasks.named<War>("war"))
}

tasks.war {
    archiveFileName.set("spacemarine-web.war")
}

smallryeOpenApi {
    configProperties.set(file("openapi.properties"))
    infoTitle.set("SpaceMarine Service")
    infoVersion.set("1.0.0")
    openApiVersion.set("3.0.3")
    schemaFilename.set("spacemarine-service")
    outputFileTypeFilter.set("YAML")
    operationIdStrategy.set("METHOD")
}
