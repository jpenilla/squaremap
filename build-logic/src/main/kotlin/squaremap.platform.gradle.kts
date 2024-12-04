import me.modmuss50.mpp.ReleaseType

plugins {
  id("squaremap.base-conventions")
  id("com.gradleup.shadow")
  id("me.modmuss50.mod-publish-plugin")
}

val platformExt = extensions.create("squaremapPlatform", SquaremapPlatformExtension::class)

decorateVersion()

tasks {
  jar {
    manifest {
      attributes(
        "squaremap-version" to project.version,
        "squaremap-commit" to lastCommitHash(),
        "squaremap-branch" to currentBranch(),
      )
    }
  }
  shadowJar {
    from(rootProject.projectDir.resolve("LICENSE")) {
      rename("LICENSE", "META-INF/LICENSE_${rootProject.name}")
    }
    minimize {
      exclude { it.moduleName.contains("squaremap") }
      exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
    }
    dependencies {
      exclude(dependency("com.google.errorprone:.*:.*"))
    }
    listOf(
      "org.owasp.html",
      "org.owasp.shim",
      "org.spongepowered.configurate",
      "org.yaml.snakeyaml"
    ).forEach(::reloc)
  }
  val copyJar = register("copyJar", CopyFile::class) {
    fileToCopy = platformExt.productionJar
    destination = platformExt.productionJar.flatMap {
      rootProject.layout.buildDirectory.file("libs/${it.asFile.name}")
    }
  }
  assemble {
    dependsOn(copyJar)
  }
}

publishMods.modrinth {
  projectId = "PFb7ZqK6"
  type = ReleaseType.STABLE
  file = platformExt.productionJar
  changelog = releaseNotes
  accessToken = providers.environmentVariable("MODRINTH_TOKEN")
}

afterEvaluate {
  if (platformExt.modInfoFilePath.isPresent) {
    tasks.processResources {
      expandIn(platformExt.modInfoFilePath.get(), mapOf(
        "version" to project.version,
        "github_url" to rootProject.providers.gradleProperty("githubUrl").get(),
        "description" to project.description,
      ))
    }
  }
}
