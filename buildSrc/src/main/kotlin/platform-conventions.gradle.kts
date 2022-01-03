plugins {
  id("base-conventions")
  id("com.github.johnrengelman.shadow")
}

val platformExt = extensions.create("squaremapPlatform", SquaremapPlatformExtension::class)

decorateVersion()

tasks {
  shadowJar {
    from(rootProject.projectDir.resolve("LICENSE")) {
      rename { "LICENSE_${rootProject.name}" }
    }
    minimize {
      exclude { it.moduleName.contains("squaremap") }
      exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
    }
    listOf(
      "net.kyori.adventure.text.minimessage",
      "org.owasp.html",
      "org.spongepowered.configurate",
      "org.yaml.snakeyaml",
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
