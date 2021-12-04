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
setupSubproject("pl3xmap-plugin") {
    projectDir = file("plugin")
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
