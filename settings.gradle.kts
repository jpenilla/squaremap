pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

rootProject.name = "squaremap"

setupSubproject("squaremap-api") {
    projectDir = file("api")
}
setupSubproject("squaremap-plugin") {
    projectDir = file("plugin")
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
