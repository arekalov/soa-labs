plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
    compileOnly("jakarta.jws:jakarta.jws-api:3.0.0")
    compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
}
