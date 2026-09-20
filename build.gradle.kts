plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.depmgmt) apply false
}

allprojects {
    group = "ru.ifmo.soa"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // Применяется только там, где реально подключён Kotlin — чтобы не ломать будущие не-Kotlin модули.
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            // helios работает на OpenJDK 21; собираем ровно под него, а не под локальный JDK 23.
            jvmToolchain(21)
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }

        tasks.withType<JavaCompile>().configureEach {
            // Сообщения об ошибках валидации на русском — без этого получим mojibake в details.
            options.encoding = "UTF-8"
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            systemProperty("file.encoding", "UTF-8")
        }
    }
}
