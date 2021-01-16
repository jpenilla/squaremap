plugins {
    `java-library`
}

allprojects {
    group = "net.pl3x.map"
    version = "1.0.0-SNAPSHOT"
    description = "Minimalistic and lightweight world map viewer for Paper servers"
}

ext["url"] = "https://github.com/pl3xgaming/Pl3xMap/"

subprojects {
    apply<JavaLibraryPlugin>()

    repositories {
        mavenCentral()
        maven("https://repo.pl3x.net/")
        maven("https://repo.jpenilla.xyz/snapshots/")
        mavenLocal()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = Charsets.UTF_8.name()
        }
        withType<Javadoc> {
            options.encoding = Charsets.UTF_8.name()
        }
    }
}
