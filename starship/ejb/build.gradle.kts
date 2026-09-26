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
    implementation(project(":soa-api"))
    compileOnly(libs.jakartaee.api)
}

tasks.jar {
    archiveFileName.set("starship-ejb.jar")
}
