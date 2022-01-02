plugins {
  id("org.spongepowered.gradle.vanilla") version "0.2.1-SNAPSHOT"
}

minecraft {
  version("1.18.1")
  accessWideners(layout.projectDirectory.dir("src/main/resources/common.accesswidener"))
}

dependencies {
  api(project(":squaremap-api"))

  api(platform("net.kyori:adventure-bom:4.9.3"))
  compileOnlyApi("net.kyori:adventure-api")
  compileOnlyApi("net.kyori:adventure-text-serializer-plain")
  api("net.kyori:adventure-text-minimessage:4.2.0-SNAPSHOT") {
    isTransitive = false // we depend on adventure separately
  }

  api(platform("cloud.commandframework:cloud-bom:1.6.1"))
  api("cloud.commandframework:cloud-core")
  api("cloud.commandframework:cloud-minecraft-extras") {
    isTransitive = false // we depend on adventure separately
  }

  api(platform("org.spongepowered:configurate-bom:4.1.2"))
  api("org.spongepowered:configurate-yaml")

  api("io.undertow:undertow-core:2.2.14.Final")
  compileOnlyApi("org.jboss.logging:jboss-logging-annotations:2.2.1.Final")

  api("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20211018.2") {
    isTransitive = false // depends on guava, provided by mc at runtime
  }
}
