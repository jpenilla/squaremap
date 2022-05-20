plugins {
  id("quiet-fabric-loom")
  id("platform-conventions")
}

val minecraftVersion = libs.versions.minecraft.get()

val squaremap: Configuration by configurations.creating
configurations.implementation {
  extendsFrom(squaremap)
}

repositories {
  maven("https://ladysnake.jfrog.io/artifactory/mods/") {
    mavenContent {
      includeGroup("dev.onyxstudios.cardinal-components-api")
    }
  }
}

dependencies {
  minecraft(libs.fabricMinecraft)
  mappings(loom.officialMojangMappings())
  modImplementation(libs.fabricLoader)
  modImplementation(libs.fabricApi)

  squaremap(projects.squaremapCommon) {
    exclude("cloud.commandframework", "cloud-core")
    exclude("cloud.commandframework", "cloud-minecraft-extras")
    exclude("io.leangen.geantyref")
  }

  modImplementation(libs.adventurePlatformFabric) {
    exclude("ca.stellardrift", "colonel")
  }
  include(libs.adventurePlatformFabric)

  modImplementation(libs.cloudFabric)
  include(libs.cloudFabric)
  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)

  modImplementation(libs.cardinalComponentsBase)
  include(libs.cardinalComponentsBase)
  modImplementation(libs.cardinalComponentsEntity)
  include(libs.cardinalComponentsEntity)
}

squaremapPlatform {
  productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}

loom {
  accessWidenerPath.set(layout.projectDirectory.file("src/main/resources/squaremap-fabric.accesswidener"))
}

tasks {
  shadowJar {
    configurations = listOf(squaremap)
    listOf(
      "javax.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
  }
  processResources {
    val props = mapOf(
      "version" to project.version,
      "github_url" to rootProject.providers.gradleProperty("githubUrl").get(),
      "description" to project.description,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
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
    archiveFileName.set("${project.name}-mc$minecraftVersion-${project.version}.jar")
  }
}
