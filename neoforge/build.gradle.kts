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
    exclude("cloud.commandframework", "cloud-core")
    exclude("cloud.commandframework", "cloud-minecraft-extras")
    exclude("io.leangen.geantyref")
  }

  implementation(platform(libs.adventureBom))
  include(platform(libs.adventureBom.get().group + ':' + libs.adventureBom.get().name + ":4.15.0-SNAPSHOT")) // todo
  implementation(libs.adventureApi)
  include(libs.adventureApi)
  include(libs.examinationApi)
  include(libs.examinationString)
  include(libs.adventureKey)
  include(libs.miniMessage)
  implementation(libs.adventureTextSerializerGson)
  include(libs.adventureTextSerializerGson)
  include(libs.adventureTextSerializerJson)
  include(libs.adventureTextSerializerPlain)

  modImplementation(libs.cloudNeoForge)
  include(libs.cloudNeoForge)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)
}

tasks.remapJar {
  archiveFileName = productionJarName(libs.versions.minecraft)
}

squaremapPlatform.loom.modInfoFilePath = "META-INF/mods.toml"

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("neoforge")
}
