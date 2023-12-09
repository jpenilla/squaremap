plugins {
  id("squaremap.base-conventions")
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
          id = "jmp"
          name = "Jason Penilla"
          timezone = "America/Phoenix"
        }
      }
    }
  }
}
