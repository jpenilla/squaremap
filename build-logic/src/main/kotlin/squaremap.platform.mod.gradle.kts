plugins {
  id("squaremap.platform")
}

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
