pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
  }
}

rootProject.name = "squaremap"

setupSubproject("squaremap-api") {
  projectDir = file("api")
}
setupSubproject("squaremap-common") {
  projectDir = file("common")
}
setupSubproject("squaremap-paper") {
  projectDir = file("paper")
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
  include(name)
  project(":$name").apply(block)
}
