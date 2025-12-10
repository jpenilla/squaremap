import net.neoforged.moddevgradle.internal.RunGameTask

plugins {
  id("squaremap.platform.mdg")
  alias(libs.plugins.resource.factory.neoforge)
}

repositories {
  maven("https://maven.neoforged.net/releases/") {
    mavenContent { releasesOnly() }
  }
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

neoForgeModsToml {
  mitLicense()
  issueTrackerUrl = githubUrl.map { "${it}issues/" }
  showAsResourcePack = false
  mixins("squaremap-forge.mixins.json")

  conventionMod("squaremap") {
    displayName = "squaremap"
    displayUrl = githubUrl
    authors = "jmp"

    dependencies {
      required("neoforge", "[21.0,)")
      required("minecraft", libs.versions.minecraft.get())
      required("cloud", "*") {
        after()
      }
      required("adventure_platform_neoforge", "*") {
        after()
      }
    }
  }
}

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("neoforge")
}
