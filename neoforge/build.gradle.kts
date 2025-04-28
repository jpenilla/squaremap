import net.neoforged.moddevgradle.internal.RunGameTask

plugins {
  id("squaremap.platform.mdg")
}

repositories {
  maven("https://maven.neoforged.net/releases/")
}

neoForge {
  enable {
    version = libs.versions.neoforge.get()
  }
  runs {
    register("client") {
      client()
    }
    register("server") {
      server()
      programArgument("nogui")
    }
    configureEach {
      loadedMods.set(emptySet())
      runProps(layout, providers).forEach { (key, value) ->
        systemProperty(key, value)
      }
    }
  }
}

tasks.withType<RunGameTask>().configureEach {
  dependsOn(tasks.productionJar)
  doFirst {
    val mods = gameDirectory.get().asFile.resolve("mods")
    mods.mkdirs()
    tasks.productionJar.get().archiveFile.get().asFile.copyTo(
      mods.resolve("squaremap.jar"),
      overwrite = true
    )
  }
  standardInput = System.`in`
}

dependencies {
  shade(projects.squaremapCommon) {
    exclude("org.incendo", "cloud-core")
    exclude("org.incendo", "cloud-minecraft-extras")
    exclude("org.incendo", "cloud-processors-confirmation")
    exclude("io.leangen.geantyref")
  }

  implementation(libs.adventurePlatformNeoforge)
  jarJar(libs.adventurePlatformNeoforge)

  implementation(libs.cloudNeoForge)
  jarJar(libs.cloudNeoForge)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  jarJar(libs.cloudMinecraftExtras)
  implementation(libs.cloudConfirmation)
  jarJar(libs.cloudConfirmation)
  jarJar(libs.cloudProcessorsCommon)
}

tasks.productionJar {
  archiveFileName = productionJarName(libs.versions.minecraft)
}

squaremapPlatform.modInfoFilePath = "META-INF/neoforge.mods.toml"

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("neoforge")
}
