plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.papermc.paperweight.userdev") version "1.3.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    id("xyz.jpenilla.run-paper") version "1.0.6"
}

dependencies {
    paperDevBundle("1.18.1-R0.1-SNAPSHOT")

    implementation(project(":squaremap-api"))
    implementation(platform("cloud.commandframework:cloud-bom:1.6.0"))
    implementation("cloud.commandframework", "cloud-paper")
    implementation("cloud.commandframework", "cloud-minecraft-extras") {
        isTransitive = false // Paper provides adventure
    }
    implementation("net.kyori", "adventure-text-minimessage", "4.2.0-SNAPSHOT") {
        isTransitive = false // Paper provides adventure
    }
    implementation("io.undertow", "undertow-core", "2.2.3.Final")
    compileOnly("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")
    implementation("org.bstats", "bstats-bukkit", "2.2.1")
    implementation("xyz.jpenilla:reflection-remapper:0.1.0-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-${project.version}-dev-all.jar")
        from(rootProject.projectDir.resolve("LICENSE"))
        minimize {
            exclude { it.moduleName == "squaremap-api" }
            exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
        }
        listOf(
            "cloud.commandframework",
            "io.leangen.geantyref",
            "net.kyori.adventure.text.minimessage",
            "org.bstats",
            "xyz.jpenilla.reflectionremapper",
            "net.fabricmc.mappingio",
        ).forEach { relocate(it, "squaremap.libraries.$it") }
    }
    reobfJar {
        outputJar.set(project.layout.buildDirectory.file("libs/${rootProject.name}-${project.version}.jar"))
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
    provides = listOf("Pl3xMap")
}
