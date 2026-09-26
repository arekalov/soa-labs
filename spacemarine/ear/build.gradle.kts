plugins {
    ear
}

dependencies {
    deploy(project(":spacemarine:ejb")) { isTransitive = false }
    deploy(project(path = ":spacemarine:web", configuration = "earModule"))
    earlib(project(":soa-api"))
}

tasks.ear {
    archiveFileName.set("spacemarine.ear")
    // Контекст-рут «/»: в спецификации нет блока servers, префикс /space-marines зашит в paths.
    deploymentDescriptor {
        applicationName = "spacemarine"
        webModule("spacemarine-web.war", "/")
    }
}
