plugins {
  id("platform-conventions")
  id("dev.architectury.loom")
}

extensions.add(
  SquaremapPlatformExtension.Loom::class.java,
  "squaremapLoomPlatform",
  the<SquaremapPlatformExtension>() as SquaremapPlatformExtension.Loom
)
val loomExt = the<SquaremapPlatformExtension.Loom>()
loomExt.productionJar.set(tasks.remapJar.flatMap { it.archiveFile })

val squaremap: Configuration by configurations.creating
configurations.implementation {
  extendsFrom(squaremap)
}

loom.silentMojangMappingsLicense()

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
  remapJar {
    inputFile.set(shadowJar.flatMap { it.archiveFile })
  }
}

afterEvaluate {
  tasks.processResources {
    val props = mapOf(
      "version" to project.version,
      "github_url" to rootProject.providers.gradleProperty("githubUrl").get(),
      "description" to project.description,
    )
    inputs.properties(props)
    filesMatching(loomExt.modInfoFilePath.get()) {
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
}
