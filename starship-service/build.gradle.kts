import org.springframework.boot.gradle.tasks.bundling.BootWar
import java.security.KeyStore
import java.security.cert.CertificateFactory

plugins {
    war
    alias(libs.plugins.kotlin.jvm)
    // allOpen для @Component/@Configuration/@Transactional
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.depmgmt)
    alias(libs.plugins.springdoc)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.springframework.boot:spring-boot-starter-web") {
        exclude(group = "org.apache.tomcat.embed")
    }
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation(libs.jackson.kotlin)

    runtimeOnly(libs.postgresql)

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // Только для generateOpenApiDocs: сервис поднимается локально на встроенном Tomcat
    // с H2 вместо Postgres и отдаёт спецификацию из кода. В WAR ничего из этого не попадает.
    developmentOnly("org.springframework.boot:spring-boot-starter-tomcat")
    developmentOnly(libs.springdoc.webmvc.api)
    developmentOnly(libs.h2)

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

// RestClientConfig читает truststore при старте, а настоящий лежит только на helios.
// Для локального запуска собираем свой из публичных сертификатов в certs/.
val openApiTrustStore by tasks.registering {
    val certs = rootProject.layout.projectDirectory.files("certs/spacemarine.crt", "certs/starship.crt")
    val store = layout.buildDirectory.file("openapi/truststore.p12")
    inputs.files(certs)
    outputs.file(store)
    doLast {
        val keyStore = KeyStore.getInstance("PKCS12").apply { load(null, null) }
        val factory = CertificateFactory.getInstance("X.509")
        certs.forEach { file ->
            file.inputStream().use { keyStore.setCertificateEntry(file.nameWithoutExtension, factory.generateCertificate(it)) }
        }
        store.get().asFile.apply { parentFile.mkdirs() }
            .outputStream().use { keyStore.store(it, "changeit".toCharArray()) }
    }
}

openApi {
    apiDocsUrl.set("http://localhost:18080/starship/v3/api-docs.yaml")
    outputDir.set(layout.buildDirectory.dir("openapi").get().asFile)
    outputFileName.set("starship-service.yaml")
    waitTimeInSeconds.set(90)
    customBootRun {
        args.set(
            listOf(
                "--spring.profiles.active=openapi",
                "--soa.truststore.path=${layout.buildDirectory.file("openapi/truststore.p12").get().asFile}",
            ),
        )
    }
}

tasks.named("forkedSpringBootRun") {
    dependsOn(openApiTrustStore)
}

