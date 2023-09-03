plugins {
  id("publishing-conventions")
}

description = "API for extending squaremap, a minimalistic and lightweight world map viewer for Minecraft servers"

java {
  disableAutoTargetJvm()
}

dependencies {
  compileOnly(libs.paperApi)
  compileOnlyApi(libs.checkerQual)
  compileOnlyApi(libs.adventureApi)
}

indra {
  javaVersions {
    target(8)
  }
}
