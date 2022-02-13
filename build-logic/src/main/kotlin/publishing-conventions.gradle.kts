plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
}

signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
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
