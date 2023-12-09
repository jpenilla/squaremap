plugins {
  id("squaremap.platform")
  id("xyz.jpenilla.quiet-architectury-loom")
}

val platformExt = extensions.getByType<SquaremapPlatformExtension>()
platformExt.productionJar = tasks.remapJar.flatMap { it.archiveFile }

val shade: Configuration by configurations.creating
configurations.implementation {
  extendsFrom(shade)
}
val shadeFiltered: Configuration by configurations.creating {
  extendsFrom(shade)

  exclude("org.checkerframework")
  exclude("com.google.errorprone")
}

tasks {
  shadowJar {
    configurations = listOf(shadeFiltered)
    listOf(
      "javax.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
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
    filesMatching(platformExt.loom.modInfoFilePath.get()) {
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
