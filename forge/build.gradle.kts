plugins {
  id("dev.architectury.loom")
  id("platform-conventions")
}

// todo reduce duplication with fabric script

val squaremap: Configuration by configurations.creating
configurations.implementation {
  extendsFrom(squaremap)
}

loom.silentMojangMappingsLicense()

loom {
  forge.mixinConfig("squaremap-forge.mixins.json")
}

dependencies {
  // todo move dependencies to catalog
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())
  forge("net.minecraftforge:forge:1.19.3-44.1.0")

  squaremap(projects.squaremapCommon) {
    exclude("cloud.commandframework", "cloud-core")
    exclude("cloud.commandframework", "cloud-minecraft-extras")
    exclude("io.leangen.geantyref")
  }

  implementation(platform(libs.adventureBom))
  include(platform(platform(libs.adventureBom)))
  implementation(libs.adventureApi)
  include(libs.adventureApi)
  include("net.kyori:examination-api:1.3.0")
  include("net.kyori:examination-string:1.3.0")
  include("net.kyori:adventure-key")
  include(libs.miniMessage)
  implementation(libs.adventureTextSerializerGson)
  include(libs.adventureTextSerializerGson)

  modImplementation(libs.cloudForge)
  include(libs.cloudForge)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)
}

squaremapPlatform {
  productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}

tasks {
  shadowJar {
    configurations = listOf(squaremap)
    listOf(
      "javax.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
    dependencies {
      exclude { it.moduleGroup == "org.checkerframework" || it.moduleGroup == "com.google.errorprone" }
    }
  }
  processResources {
    val props = mapOf(
      "version" to project.version,
      "github_url" to rootProject.providers.gradleProperty("githubUrl").get(),
      "description" to project.description,
    )
    inputs.properties(props)
    filesMatching("META-INF/mods.toml") {
      // filter manually to avoid trying to replace $Initializer in initializer class name...
      filter { string ->
        var result = string
        for ((key, value) in props) {
          result = result.replace("\${$key}", value.toString())
        }
        result
      }
    }
  }
  remapJar {
    inputFile.set(shadowJar.flatMap { it.archiveFile })
    archiveFileName.set(productionJarName(libs.versions.minecraft))
  }
}

// todo https://github.com/FabricMC/fabric-loom/pull/838
configurations.include {
  isTransitive = true
  withDependencies {
    all {
      if (this !is ModuleDependency) return@all
      val category: Category? = attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
      if (category != null && (category.name == Category.REGULAR_PLATFORM || category.name == Category.ENFORCED_PLATFORM)) {
        return@all
      }
      isTransitive = false
    }
  }
}
