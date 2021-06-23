dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.pl3x.net/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.incendo.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public/")
        mavenLocal()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

rootProject.name = "Pl3xMap"

setupSubproject("pl3xmap-api") {
    projectDir = file("api")
}

include(":plugin")

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
