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
}

tasks {
  shadowJar {
    configurations = listOf(shadeFiltered)
    listOf(
      "jakarta.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
  }
}

afterEvaluate {
  tasks.processResources {
    if (platformExt.loom.modInfoFilePath.isPresent) {
      expandIn(platformExt.loom.modInfoFilePath.get(), mapOf(
        "version" to project.version,
        "github_url" to githubUrl,
        "description" to project.description,
      ))
    }
  }
}
