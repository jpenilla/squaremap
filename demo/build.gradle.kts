import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration
import xyz.jpenilla.runpaper.task.RunServer

plugins {
  id("squaremap.base-conventions")
  id("io.papermc.paperweight.userdev")
  alias(libs.plugins.run.paper)
  alias(libs.plugins.resource.factory.bukkit)
}

repositories {
  maven("https://repo.codemc.io/repository/maven-public/") {
    mavenContent { includeGroup("org.popcraft") }
  }
  exclusiveContent {
    forRepository { maven("https://api.modrinth.com/maven") }
    filter { includeGroup("maven.modrinth") }
  }
}

val squaremapPlugin = configurations.register("squaremapPlugin") {
  isTransitive = false
}

dependencies {
  paperweight.paperDevBundle(libs.versions.paper)
  compileOnly(projects.squaremapCommon)
  compileOnly(libs.chunkyApi)
  compileOnly(libs.checkerQual)
  add(squaremapPlugin.name, project(projects.squaremapPaper.path, "shadowRuntimeElements"))
}

paperweight {
  injectPaperRepository = false
  reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

bukkitPluginYaml {
  name = "squaremap-demo"
  main = "xyz.jpenilla.squaremap.demo.SquaremapDemo"
  apiVersion = libs.versions.minecraft
  depend = listOf("squaremap", "Chunky")
}

// Override with -PdemoRadius=<blocks> to test with a smaller map
val demoRadius = providers.gradleProperty("demoRadius").orElse("10240")
// Per server. Threads defaults to Paper's and squaremap's own sizing.
val demoThreads = providers.gradleProperty("demoThreads")
val demoMaxHeap = providers.gradleProperty("demoMaxHeap").orElse("6G")
val demoSeed = "7257778360917119258"

class DemoWorld(val webName: String, val displayName: String, val order: Int, val siteName: String = webName)

class DemoServer(
  val name: String,
  val port: Int,
  val worlds: List<DemoWorld>,
  val datapacks: List<Provider<MinimalExternalModuleDependency>> = listOf(),
)

val demoServers = listOf(
  DemoServer(
    "vanilla",
    25581,
    listOf(
      DemoWorld("minecraft_overworld", "Overworld", 0),
      DemoWorld("minecraft_the_nether", "Nether", 1),
      DemoWorld("minecraft_the_end", "The End", 2),
    ),
  ),
  DemoServer(
    "datapacks",
    25582,
    listOf(
      DemoWorld("minecraft_overworld", "Terralith + Terratonic Overworld", 3, "terralith_overworld"),
      DemoWorld("minecraft_the_nether", "Incendium Nether", 4, "incendium_nether"),
      DemoWorld("minecraft_the_end", "Nullscape End", 5, "nullscape_end"),
    ),
    // Load order matters: Terratonic must override Terralith's terrain
    listOf(libs.demoTerralith, libs.demoTerratonic, libs.demoIncendium, libs.demoNullscape),
  ),
)

@UntrackedTask(because = "Always starts from a fresh server")
abstract class PrepareDemoServer : DefaultTask() {
  @get:OutputDirectory
  abstract val serverDir: DirectoryProperty

  @get:InputFiles
  abstract val datapacks: ConfigurableFileCollection

  @get:Input
  abstract val seed: Property<String>

  @get:Input
  abstract val port: Property<Int>

  @get:Input
  abstract val squaremapConfig: Property<String>

  @TaskAction
  fun run() {
    val dir = serverDir.get().asFile
    dir.deleteRecursively()
    dir.resolve("plugins/squaremap").mkdirs()
    dir.resolve("eula.txt").writeText("eula=true\n")
    dir.resolve("server.properties").writeText(
      """
      server-port=${port.get()}
      level-seed=${seed.get()}
      online-mode=false
      view-distance=2
      simulation-distance=2
      """.trimIndent() + "\n"
    )
    dir.resolve("plugins/squaremap/config.yml").writeText(squaremapConfig.get())
    val datapackDir = dir.resolve("world/datapacks")
    datapackDir.mkdirs()
    datapacks.files.forEachIndexed { i, file ->
      file.copyTo(datapackDir.resolve("${i + 1}-${file.name}"))
    }
  }
}

fun squaremapConfig(server: DemoServer): String = buildString {
  appendLine("settings:")
  appendLine("  update-checker: false")
  appendLine("  internal-webserver:")
  appendLine("    enabled: false")
  appendLine("world-settings:")
  appendLine("  default:")
  appendLine("    map:")
  appendLine("      render:")
  appendLine("        background:")
  appendLine("          enabled: false")
  server.worlds.forEach { world ->
    appendLine("  '${world.webName.replaceFirst('_', ':')}':")
    appendLine("    map:")
    appendLine("      display-name: ${world.displayName}")
    appendLine("      order: ${world.order}")
  }
}

val renderTasks = demoServers.map { server ->
  val taskSuffix = server.name.replaceFirstChar(Char::uppercase)
  val serverDir = layout.buildDirectory.dir("servers/${server.name}")
  // One configuration per datapack keeps the load order
  val datapacks = server.datapacks.mapIndexed { i, datapack ->
    val configuration = configurations.register("${server.name}Datapack$i") {
      isTransitive = false
    }
    // Added through DependencyHandler since the configuration's DependencySet drops the artifact type
    // (https://github.com/gradle/gradle/issues/30351)
    dependencies.addProvider(configuration.name, dependencies.variantOf(datapack) { artifactType("zip") })
    configuration
  }

  val prepare = tasks.register<PrepareDemoServer>("prepareDemo$taskSuffix") {
    this.serverDir = serverDir
    datapacks.forEach { this.datapacks.from(it) }
    seed = demoSeed
    port = server.port
    squaremapConfig = squaremapConfig(server)
  }

  fun RunServer.configureDemoServer(mode: String) {
    minecraftVersion(libs.versions.minecraft.get())
    runDirectory(serverDir.get().asFile)
    pluginJars(squaremapPlugin)
    pluginJars(tasks.jar.flatMap { it.archiveFile })
    downloadPlugins {
      modrinth("chunky", libs.versions.chunkyPlugin.get())
    }
    maxHeapSize = demoMaxHeap.get()
    systemProperty("squaremap.demo.mode", mode)
    systemProperty("squaremap.demo.radius", demoRadius.get())
    demoThreads.orNull?.let { threads ->
      // squaremap renders chunks loaded or decoded by the Moonrise workers, so keep the two counts balanced
      systemProperty("Paper.WorkerThreadCount", threads)
      systemProperty("squaremap.map.render.radius.workers", threads)
    }
    val result = serverDir.map { it.file("demo-result.json").asFile }
    doFirst {
      result.get().delete()
    }
    doLast {
      val file = result.get()
      check(file.isFile) { "Server exited without writing ${file.name}" }
      val status = file.readText()
      check(status.contains("\"ok\"")) { "Demo $mode failed: $status" }
    }
  }

  val pregen = tasks.register<RunServer>("pregenDemo$taskSuffix") {
    mustRunAfter(prepare)
    configureDemoServer("pregen")
  }

  tasks.register<RunServer>("renderDemo$taskSuffix") {
    mustRunAfter(prepare, pregen)
    configureDemoServer("render")
  }
}

@UntrackedTask(because = "Inputs are server output directories rewritten by every render")
abstract class AssembleDemoSite : DefaultTask() {
  @get:Internal
  abstract val vanillaWeb: DirectoryProperty

  @get:Internal
  abstract val datapacksWeb: DirectoryProperty

  /** Web name on the datapacks server to name in the site. */
  @get:Input
  abstract val datapackWorlds: MapProperty<String, String>

  /** Committed files copied over the generated site. */
  @get:InputDirectory
  abstract val overlay: DirectoryProperty

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Inject
  abstract val fs: FileSystemOperations

  @TaskAction
  fun run() {
    val site = outputDir.get().asFile
    fs.delete { delete(site) }
    fs.copy {
      from(vanillaWeb)
      into(site)
      exclude("**/changes.json")
    }
    datapackWorlds.get().forEach { (webName, siteName) ->
      fs.copy {
        from(datapacksWeb.dir("tiles/$webName"))
        into(site.resolve("tiles/$siteName"))
        exclude("changes.json")
      }
    }

    val slurper = JsonSlurper()

    @Suppress("UNCHECKED_CAST")
    val settings = slurper.parse(site.resolve("tiles/settings.json")) as MutableMap<String, Any>

    @Suppress("UNCHECKED_CAST")
    val datapackSettings = slurper.parse(datapacksWeb.file("tiles/settings.json").get().asFile) as Map<String, Any>

    @Suppress("UNCHECKED_CAST")
    val worlds = settings["worlds"] as MutableList<Any>

    @Suppress("UNCHECKED_CAST")
    (datapackSettings["worlds"] as List<MutableMap<String, Any>>).forEach { world ->
      world["name"] = datapackWorlds.get().getValue(world["name"] as String)
      worlds.add(world)
    }
    settings["static"] = true
    site.resolve("tiles/settings.json").writeText(JsonOutput.toJson(settings))

    fs.copy {
      from(overlay)
      into(site)
    }
  }
}

tasks.register<AssembleDemoSite>("assembleDemoSite") {
  mustRunAfter(renderTasks)
  vanillaWeb = layout.buildDirectory.dir("servers/vanilla/plugins/squaremap/web")
  datapacksWeb = layout.buildDirectory.dir("servers/datapacks/plugins/squaremap/web")
  datapackWorlds = demoServers.single { it.name == "datapacks" }.worlds.associate { it.webName to it.siteName }
  overlay = layout.projectDirectory.dir("site")
  outputDir = layout.buildDirectory.dir("site")
}
