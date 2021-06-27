import io.papermc.paperweight.tasks.RemapJar
import io.papermc.paperweight.util.Constants
import io.papermc.paperweight.util.registering

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.papermc.paperweight.patcher") version "1.1.7"
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
    val productionMappedJar by registering<RemapJar> {
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
