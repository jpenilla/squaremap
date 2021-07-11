plugins {
    `java-library`
}

allprojects {
    val build = System.getenv("BUILD_NUMBER") ?: "SNAPSHOT"
    group = "net.pl3x.map"
    version = "1.0.0-BETA-$build"
    description = "Minimalistic and lightweight world map viewer for Paper servers"
}

subprojects {
    apply<JavaLibraryPlugin>()

    repositories {
        mavenCentral()
        maven("https://repo.pl3x.net/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.incendo.org/content/repositories/snapshots/")
        maven("https://repo.codemc.org/repository/maven-public/")
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = Charsets.UTF_8.name()
            options.release.set(16)
        }
        withType<Javadoc> {
            options.encoding = Charsets.UTF_8.name()
        }
    }
}
