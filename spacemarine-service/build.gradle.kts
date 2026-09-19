plugins {
    war
    alias(libs.plugins.kotlin.jvm)
    // noArg: JPA требует конструктор без аргументов, которого у классов Kotlin нет
    alias(libs.plugins.kotlin.jpa)
    // allOpen: Hibernate строит прокси наследованием, а классы Kotlin по умолчанию final
    alias(libs.plugins.kotlin.allopen)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.Embeddable")
    annotation("jakarta.persistence.MappedSuperclass")
}

// Плагин kotlin-jpa — это noArg с преднастройкой под JPA; дополняем его аннотацией CDI.
//
// Для normal-scoped бина Weld строит прокси наследованием и вызывает конструктор
// без аргументов. У класса Kotlin с параметрами конструктора такого нет, и развёртывание
// падает с WELD-001435 "not proxyable because it has no no-args constructor".
// Сгенерированный конструктор используется только прокси-подклассом; настоящий экземпляр
// по-прежнему создаётся через @Inject-конструктор, поэтому поля не остаются пустыми.
noArg {
    annotation("jakarta.enterprise.context.ApplicationScoped")
}

dependencies {
    // Всё это предоставляет WildFly: компилируем против API, но в WAR не пакуем.
    // JDBC-драйвер тоже не пакуем — он ставится в сервер модулем, датасорс берётся по JNDI.
    providedCompile(libs.jakartaee.api)
    providedCompile(libs.jackson.databind)
    providedCompile(libs.jackson.jsr310)

    // А вот этот модуль WildFly НЕ предоставляет, поэтому кладём его в WAR.
    // Без него Jackson не может построить data-класс: у val-свойств нет сеттеров,
    // а имена параметров конструктора без модуля не видны.
    implementation(libs.jackson.kotlin)

    // jackson-module-kotlin тянет kotlin-reflect своей версии (1.9.x), которая
    // расходится со stdlib проекта. Разные ветки Kotlin в одном classpath дают
    // ошибки связывания, поэтому версию выравниваем явно.
    implementation(kotlin("reflect"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertj)
    testImplementation(libs.archunit.junit5)
    // Валидация в юнит-тестах вне контейнера
    testImplementation(libs.hibernate.validator)
    testImplementation(libs.expressly)
}

tasks.war {
    // Имя фиксировано: с версией в имени она попала бы в context path и сломала бы пути из спеки.
    // Контекст-рут задаётся в WEB-INF/jboss-web.xml, поэтому имя файла на URL не влияет.
    archiveFileName.set("spacemarine-service.war")
}
