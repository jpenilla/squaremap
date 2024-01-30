plugins {
  id("squaremap.publishing")
  id("org.incendo.cloud-build-logic.javadoc-links") version "0.0.8"
}

description = "API for extending squaremap, a minimalistic and lightweight world map viewer for Minecraft servers"

java {
  disableAutoTargetJvm()
}

dependencies {
  compileOnly(libs.paperApi)
  javadocLinks(libs.paperApi) {
    isTransitive = false
  }
  compileOnlyApi(libs.checkerQual)
  compileOnlyApi(platform(libs.adventureBom))
  compileOnlyApi(libs.adventureApi)
}

indra {
  javaVersions {
    target(8)
  }
}

tasks.withType(JavaCompile::class).configureEach {
  // Don't warn about missing forRemoval in @Deprecated
  options.compilerArgs.add("-Xlint:-classfile")
}
