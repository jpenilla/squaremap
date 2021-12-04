plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.papermc.paperweight.userdev") version "1.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    id("xyz.jpenilla.run-paper") version "1.0.5"
}

dependencies {
    paperDevBundle("1.18-R0.1-SNAPSHOT")

    implementation(project(":pl3xmap-api"))
    implementation(platform("cloud.commandframework:cloud-bom:1.6.0"))
    implementation("cloud.commandframework", "cloud-paper")
    implementation("cloud.commandframework", "cloud-minecraft-extras")
    implementation("net.kyori", "adventure-text-minimessage", "4.2.0-SNAPSHOT")
    implementation("io.undertow", "undertow-core", "2.2.3.Final")
    compileOnly("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")
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
}

bukkit {
    main = "net.pl3x.map.plugin.Pl3xMapPlugin"
    name = rootProject.name
    apiVersion = "1.18"
    website = project.property("githubUrl") as String
    authors = listOf("jmp", "BillyGalbreath")
}
