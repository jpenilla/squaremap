import xyz.jpenilla.runpaper.task.RunServerTask

plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.papermc.paperweight.userdev") version "1.1.9-SNAPSHOT"
    id("net.minecrell.plugin-yml.bukkit") version "0.4.0"
    id("xyz.jpenilla.run-paper") version "1.0.3"
}

dependencies {
    paperweightDevBundle(group = "net.pl3x.paper", version = "1.17.1-R0.1-SNAPSHOT")

    implementation(project(":pl3xmap-api"))
    val cloudVersion = "1.5.0"
    implementation("cloud.commandframework", "cloud-paper", cloudVersion)
    implementation("cloud.commandframework", "cloud-minecraft-extras", cloudVersion)
    implementation("net.kyori", "adventure-text-minimessage", "4.1.0-SNAPSHOT")
    implementation("io.undertow", "undertow-core", "2.2.3.Final")
    implementation("org.bstats", "bstats-bukkit", "2.2.1")
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}-mojang-mapped.jar")
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
    reobfJar {
        outputJar.set(project.layout.buildDirectory.file("libs/${rootProject.name}-${rootProject.version}.jar"))
    }
    build {
        dependsOn(reobfJar)
    }
    runServer {
        minecraftVersion("1.17.1")
        pluginJars(reobfJar.flatMap { it.outputJar })
    }
    register<RunServerTask>("runMojangMappedServer") {
        minecraftVersion("1.17.1")
        pluginJars(shadowJar.flatMap { it.archiveFile })
        paperclip(paperweight.mojangMappedPaperServerJar)
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
