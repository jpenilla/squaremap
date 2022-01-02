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
  }
  val copyJar = register("copyJar", CopyFile::class) {
    fileToCopy.set(platformExt.productionJar)
    destination.set(platformExt.productionJar.flatMap { rootProject.layout.buildDirectory.file("libs/${it.asFile.name}") })
  }
  assemble {
    dependsOn(copyJar)
  }
}
