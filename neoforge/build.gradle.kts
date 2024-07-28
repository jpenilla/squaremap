plugins {
  id("squaremap.platform.loom")
}

repositories {
  maven("https://maven.neoforged.net/releases/")
}

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())
  neoForge(libs.neoforge)

  shade(projects.squaremapCommon) {
    exclude("org.incendo", "cloud-core")
    exclude("org.incendo", "cloud-minecraft-extras")
    exclude("org.incendo", "cloud-processors-confirmation")
    exclude("io.leangen.geantyref")
  }

  modImplementation(libs.adventurePlatformNeoforge)
  include(libs.adventurePlatformNeoforge)

  modImplementation(libs.cloudNeoForge)
  include(libs.cloudNeoForge)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)
  implementation(libs.cloudConfirmation)
  include(libs.cloudConfirmation)
  include(libs.cloudProcessorsCommon)
}

tasks.remapJar {
  archiveFileName = productionJarName(libs.versions.minecraft)
}

squaremapPlatform.loom.modInfoFilePath = "META-INF/neoforge.mods.toml"

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("neoforge")
}
