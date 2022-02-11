plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
}

signing {
  useInMemoryPgpKeys(
    providers.gradleProperty("signingKey").orNull,
    providers.gradleProperty("signingPassword").orNull
  )
}

indra {
  github("jpenilla", "squaremap")
  mitLicense()

  configurePublications {
    pom {
      developers {
        developer {
          id.set("jmp")
          name.set("Jason Penilla")
          timezone.set("America/Phoenix")
        }
      }
    }
  }
}
