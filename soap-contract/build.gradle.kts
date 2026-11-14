plugins { `java-library` }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
dependencies {
    compileOnly("jakarta.xml.ws:jakarta.xml.ws-api:4.0.2")
    compileOnly("jakarta.jws:jakarta.jws-api:3.0.0")
    compileOnly("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
}
