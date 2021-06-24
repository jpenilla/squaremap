dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.pl3x.net/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.incendo.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://maven.quiltmc.org/repository/release/") {
            mavenContent {
                releasesOnly()
                includeModule("org.quiltmc", "tiny-remapper")
            }
        }
        mavenLocal()
    }
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
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
