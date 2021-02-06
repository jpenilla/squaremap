plugins {
    `java-library`
}

allprojects {
    group = "net.pl3x.map"
    version = "1.0.0-BETA"
    description = "Minimalistic and lightweight world map viewer for Paper servers"
}

ext["url"] = "https://github.com/pl3xgaming/Pl3xMap/"

subprojects {
    apply<JavaLibraryPlugin>()

    repositories {
        mavenCentral()
        maven("https://repo.pl3x.net/")
        maven("https://repo.jpenilla.xyz/snapshots/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.codemc.org/repository/maven-public/")
        mavenLocal()
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
