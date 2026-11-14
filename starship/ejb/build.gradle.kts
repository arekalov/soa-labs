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

noArg {
    annotation("jakarta.enterprise.context.ApplicationScoped")
}

dependencies {
    testImplementation(project(":soap-contract"))
    testImplementation(libs.jakartaee.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation("org.apache.cxf:cxf-rt-frontend-jaxws:4.0.10")
    testImplementation("org.apache.cxf:cxf-rt-transports-http-jetty:4.0.10")
    compileOnly(project(":soap-contract"))
    compileOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
    compileOnly("jakarta.jws:jakarta.jws-api:3.0.0")
    compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    implementation(project(":soa-api"))
    compileOnly(libs.jakartaee.api)
}

tasks.jar {
    archiveFileName.set("starship-ejb.jar")
}
