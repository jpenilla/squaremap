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

    java {
        targetCompatibility = JavaVersion.toVersion(16)
        sourceCompatibility = JavaVersion.toVersion(16)
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
