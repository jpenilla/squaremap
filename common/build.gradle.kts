plugins {
  id("squaremap.base-conventions")
  id("net.neoforged.moddev")
}

neoForge {
  neoFormVersion = libs.versions.neoform
  accessTransformers.from(layout.projectDirectory.file("src/main/resources/squaremap-common-at.cfg"))
}

dependencies {
  api(projects.squaremapApi)
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

  api(platform(libs.cloudBom))
  api(platform(libs.cloudMinecraftBom))
  api(platform(libs.cloudProcessorsBom))
  api(libs.cloudCore)
  api(libs.cloudConfirmation)
  compileOnly(libs.cloudBrigadier)
  api(libs.cloudMinecraftExtras)

  api(platform(libs.configurateBom))
  api(libs.configurateYaml)

  api(libs.undertow)

  api(libs.htmlSanitizer) {
    isTransitive = false // depends on guava, provided by mc at runtime
  }
  api(libs.htmlSanitizerJ8) {
    isTransitive = false // depends on guava, provided by mc at runtime
  }
  api(libs.htmlSanitizerJ10) {
    isTransitive = false // depends on guava, provided by mc at runtime
  }

  compileOnly("curse.maven:moonrise-1096335:6008360")
}
