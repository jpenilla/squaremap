plugins {
  id("squaremap.platform")
}

val shade = configurations.register("shade")
configurations.implementation {
  extendsFrom(shade)
}
val shadeFiltered = configurations.register("shadeFiltered") {
  extendsFrom(shade)

  exclude("org.checkerframework")
}

tasks {
  shadowJar {
    configurations = listOf(shadeFiltered.get())
    listOf(
      "jakarta.inject",
      "com.google.inject",
      "org.aopalliance",
    ).forEach(::reloc)
  }
}
