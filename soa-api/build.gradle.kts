plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(kotlin("stdlib"))
    // Едет в EAR/lib: WildFly модуль Kotlin не предоставляет, а без него Jackson
    // не соберёт data-класс. Jackson-ядро приходит от сервера, поэтому исключено.
    api(libs.jackson.kotlin) { exclude(group = "com.fasterxml.jackson.core") }
    api(kotlin("reflect"))

    compileOnly(libs.jakartaee.api)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.jsr310)
}
