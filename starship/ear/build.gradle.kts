plugins {
    ear
}

dependencies {
    deploy(project(":starship:ejb")) { isTransitive = false }
    deploy(project(path = ":starship:web", configuration = "earModule"))
    earlib(project(":soa-api"))
    earlib(project(":soap-contract"))
}

tasks.ear {
    archiveFileName.set("starship.ear")
    deploymentDescriptor {
        applicationName = "starship"
        webModule("starship-web.war", "/")
    }
}
