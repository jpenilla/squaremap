plugins {
  id("squaremap.publishing")
}

description = "API for extending squaremap, a minimalistic and lightweight world map viewer for Minecraft servers"

java {
  disableAutoTargetJvm()
}

dependencies {
  compileOnly(libs.paperApi)
  compileOnlyApi(libs.checkerQual)
  compileOnlyApi(platform(libs.adventureBom))
  compileOnlyApi(libs.adventureApi)
}

indra {
  javaVersions {
    target(8)
  }
}

tasks.withType<Javadoc>().configureEach {
  val options = options as StandardJavadocDocletOptions
  options.links(
    "https://jd.advntr.dev/api/${libs.versions.adventure.get()}/",
    "https://checkerframework.org/api/",
    "https://jd.papermc.io/paper/${libs.versions.minecraft.get().split(".", "-").take(2).joinToString(".")}/",
  )
}

tasks.withType(JavaCompile::class).configureEach {
  // Don't warn about missing forRemoval in @Deprecated
  options.compilerArgs.add("-Xlint:-classfile")
}
