plugins {
  id("net.kyori.indra.publishing")
}

java {
  disableAutoTargetJvm()
}

dependencies {
  compileOnly(libs.paperApi)
  compileOnlyApi(libs.checkerQual)
}

indra {
  javaVersions {
    target(8)
  }

  publishSnapshotsTo("jmp", "https://repo.jpenilla.xyz/snapshots/")
}
