import io.papermc.paperweight.tasks.BaseTask
import io.papermc.paperweight.util.Constants
import io.papermc.paperweight.util.Constants.paperTaskOutput
import io.papermc.paperweight.util.cache
import io.papermc.paperweight.util.defaultOutput
import io.papermc.paperweight.util.ensureDeleted
import io.papermc.paperweight.util.ensureParentExists
import io.papermc.paperweight.util.registering
import io.papermc.paperweight.util.runJar

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.papermc.paperweight.patcher") version "1.1.6"
    id("net.minecrell.plugin-yml.bukkit") version "0.4.0"
    id("xyz.jpenilla.run-paper") version "1.0.3"
}

val mojangMappedServer: Configuration by configurations.creating
configurations.compileOnly {
    extendsFrom(mojangMappedServer)
}

dependencies {
    implementation(project(":pl3xmap-api"))
    val cloudVersion = "1.5.0-SNAPSHOT"
    implementation("cloud.commandframework", "cloud-paper", cloudVersion)
    implementation("cloud.commandframework", "cloud-minecraft-extras", cloudVersion)
    implementation("net.kyori", "adventure-text-minimessage", "4.1.0-SNAPSHOT")
    implementation("io.undertow", "undertow-core", "2.2.3.Final")
    implementation("org.bstats", "bstats-bukkit", "2.2.1")
    mojangMappedServer("io.papermc.paper", "paper", "1.17-R0.1-SNAPSHOT", classifier = "mojang-mapped")
    remapper("org.quiltmc", "tiny-remapper", "0.4.1")
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}-mojang-mapped.jar")
        archiveClassifier.set("mojang-mapped")
        from(rootProject.projectDir.resolve("LICENSE"))
        minimize {
            exclude { it.moduleName == "pl3xmap-api" }
            exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
        }
        listOf(
            "cloud.commandframework",
            "io.leangen.geantyref",
            "net.kyori.adventure.text.minimessage",
            "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.plugin.lib.$it") }
    }
    val productionMappedJar by registering<RemapJar2> {
        inputJar.set(shadowJar.flatMap { it.archiveFile })
        outputJar.set(project.layout.buildDirectory.file("libs/${rootProject.name}-${rootProject.version}.jar"))
        mappingsFile.set(project.layout.projectDirectory.file("mojang+yarn-spigot-reobf-patched.tiny"))
        fromNamespace.set(Constants.DEOBF_NAMESPACE)
        toNamespace.set(Constants.SPIGOT_NAMESPACE)
        remapper.from(project.configurations.remapper)
        remapClasspath.from(mojangMappedServer)
    }
    build {
        dependsOn(productionMappedJar)
    }
    runServer {
        minecraftVersion("1.17")
        pluginJars.from(productionMappedJar.flatMap { it.outputJar })
    }
}

runPaper {
    disablePluginJarDetection()
}

bukkit {
    main = "net.pl3x.map.plugin.Pl3xMapPlugin"
    name = rootProject.name
    apiVersion = "1.17"
    website = project.property("githubUrl") as String
    authors = listOf("BillyGalbreath", "jmp")
}

/*
 * paperweight is a Gradle plugin for the PaperMC project.
 *
 * Copyright (c) 2021 Kyle Wood (DemonWav)
 *                    Contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * version 2.1 only, no later versions.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
/**
 * Copy pasted from Paperweight [io.papermc.paperweight.tasks.RemapJar]
 *
 * [https://github.com/PaperMC/paperweight/pull/44]
 */
abstract class RemapJar2 : BaseTask() {

    @get:InputFile
    abstract val inputJar: RegularFileProperty

    @get:InputFile
    abstract val mappingsFile: RegularFileProperty

    @get:Input
    abstract val fromNamespace: Property<String>

    @get:Input
    abstract val toNamespace: Property<String>

    @get:Input
    abstract val rebuildSourceFilenames: Property<Boolean>

    @get:Internal
    abstract val jvmargs: ListProperty<String>

    @get:Internal
    abstract val singleThreaded: Property<Boolean>

    @get:Classpath
    abstract val remapper: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    // Pl3xMap start
    @get:InputFiles
    abstract val remapClasspath: ConfigurableFileCollection
    // Pl3xmap end

    override fun init() {
        outputJar.convention(defaultOutput())
        singleThreaded.convention(true)
        jvmargs.convention(listOf("-Xmx1G"))
        rebuildSourceFilenames.convention(true)
    }

    @TaskAction
    fun run() {
        val logFile = layout.cache.resolve(paperTaskOutput("log"))
        ensureDeleted(logFile)

        val args = mutableListOf(
            inputJar.get().asFile.absolutePath,
            outputJar.get().asFile.absolutePath,
            mappingsFile.get().asFile.absolutePath,
            fromNamespace.get(),
            toNamespace.get(),
            *remapClasspath.asFileTree.map { it.absolutePath }.toTypedArray(), // Pl3xMap
            "--fixpackageaccess",
            "--renameinvalidlocals"
        )
        if (singleThreaded.get()) {
            args += "--threads=1"
        }
        if (rebuildSourceFilenames.get()) {
            args += "--rebuildsourcefilenames"
        }

        ensureParentExists(logFile)
        runJar(remapper, layout.cache, logFile, jvmArgs = jvmargs.get(), args = args.toTypedArray())
    }
}
