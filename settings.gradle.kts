pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://repo.jpenilla.xyz/snapshots/")
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
setupSubproject("squaremap-fabric") {
  projectDir = file("fabric")
}
setupSubproject("squaremap-sponge") {
  projectDir = file("sponge")
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
  include(name)
  project(":$name").apply(block)
}
