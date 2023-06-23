plugins {
  id("base-conventions")
  id("org.spongepowered.gradle.vanilla")
}

minecraft {
  version(libs.versions.minecraft.get())
  accessWideners(layout.projectDirectory.file("src/main/resources/squaremap-common.accesswidener"))
}

dependencies {
  api(projects.squaremapApi)
  api(libs.caffeine) {
    exclude("com.google.errorprone")
  }
  api(libs.guice) {
    exclude("com.google.guava") // provided by minecraft
  }
  api(libs.guiceAssistedInject) {
    exclude("com.google.guava") // provided by minecraft
  }

  api(platform(libs.adventureBom))
  compileOnlyApi(libs.adventureApi)
  compileOnlyApi(libs.adventureTextSerializerPlain)
  compileOnly(libs.adventureTextSerializerGson)
  compileOnlyApi(libs.miniMessage)

  api(libs.cloudCore)
  compileOnly(libs.cloudBrigadier)
  api(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }

  api(platform(libs.configurateBom))
  api(libs.configurateYaml)

  api(libs.undertow)
  compileOnlyApi(libs.jBossLoggingAnnotations)

  api(libs.htmlSanitizer) {
    isTransitive = false // depends on guava, provided by mc at runtime
  }
}
