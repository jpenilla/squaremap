plugins {
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("kr.entree.spigradle") version "2.2.3"
}

dependencies {
    implementation(project(":pl3xmap-api"))
    val cloudVersion = "1.4.0"
    implementation("cloud.commandframework", "cloud-paper", cloudVersion)
    implementation("cloud.commandframework", "cloud-minecraft-extras", cloudVersion)
    implementation("net.kyori", "adventure-platform-bukkit", "4.0.0-SNAPSHOT")
    implementation("net.kyori", "adventure-text-minimessage", "4.1.0-SNAPSHOT")
    implementation("io.undertow", "undertow-core", "2.2.3.Final")
    compileOnly("net.pl3x.purpur", "purpur", "1.16.5-R0.1-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")
        destinationDirectory.set(rootProject.rootDir.resolve("build").resolve("libs"))
        from(rootProject.projectDir.resolve("LICENSE"))
        minimize {
            exclude { it.moduleName == "pl3xmap-api" }
            exclude(dependency("io.undertow:.*:.*")) // does not like being minimized _or_ relocated (xnio errors)
        }
        listOf(
            "cloud.commandframework",
            "io.leangen.geantyref",
            "net.kyori"
        ).forEach { relocate(it, "${rootProject.group}.plugin.lib.$it") }
    }
    build {
        dependsOn(shadowJar)
    }
}

spigot {
    name = rootProject.name
    apiVersion = "1.16"
    website = rootProject.ext["url"].toString()
    authors("BillyGalbreath", "jmp")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
