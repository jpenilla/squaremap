plugins {
  id("base-conventions")
  id("com.github.johnrengelman.shadow")
  id("com.modrinth.minotaur")
}

val platformExt = extensions.create(
  SquaremapPlatformExtension::class,
  "squaremapPlatform",
  SquaremapPlatformExtension.Loom::class
)

tasks {
  jar {
    manifest {
      attributes(
        "squaremap-version" to project.version,
        "squaremap-branch" to currentBranch(),
      )
    }
  }
  shadowJar {
    from(rootProject.projectDir.resolve("LICENSE")) {
      rename { "LICENSE_${rootProject.name}" }
    }
    minimize {
      exclude { it.moduleName.contains("squaremap") }
      exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
    }
    listOf(
      "org.owasp.html",
      "org.spongepowered.configurate",
      "org.yaml.snakeyaml"
    ).forEach(::reloc)
  }
  val copyJar = register("copyJar", CopyFile::class) {
    fileToCopy.set(platformExt.productionJar)
    destination.set(platformExt.productionJar.flatMap { rootProject.layout.buildDirectory.file("libs/${it.asFile.name}") })
  }
  assemble {
    dependsOn(copyJar)
  }
}

modrinth {
  projectId.set("PFb7ZqK6")
  versionType.set("release")
  file.set(platformExt.productionJar)
  changelog.set(releaseNotes)
  token.set(providers.environmentVariable("MODRINTH_TOKEN"))
}
