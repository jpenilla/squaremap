plugins {
  id("squaremap.base-conventions")
  id("net.neoforged.moddev")
}

neoForge {
  enable {
    neoFormVersion = libs.versions.neoform.get()
  }
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
  api(libs.configurateYaml) {
    // Provided by the adventure platform
    exclude("net.kyori", "option")
  }

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

@UntrackedTask(because = "Up-to-date checking for this needs further thought")
abstract class BuildFrontend : DefaultTask() {
  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Internal
  abstract val workingDir: DirectoryProperty

  @get:Input
  abstract val command: ListProperty<String>

  @get:Inject
  abstract val exec: ExecOperations

  @TaskAction
  fun run() {
    exec.exec {
      commandLine(command.get())
      workingDir(this@BuildFrontend.workingDir.asFile)
    }
  }
}

val buildFrontend = tasks.register<BuildFrontend>("buildFrontend") {
  outputDir = layout.buildDirectory.dir("web")
  workingDir = layout.settingsDirectory.dir("web")
  command = listOf("bun", "run", "build")
}

tasks.processResources {
  from(buildFrontend.flatMap { it.outputDir }) {
    into("web")
  }
}
