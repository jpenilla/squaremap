import org.incendo.cloudbuildlogic.jmp

plugins {
  id("squaremap.base-conventions")
  id("net.kyori.indra.publishing")
  id("org.incendo.cloud-build-logic.publishing")
  id("org.incendo.cloud-build-logic.javadoc-links")
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
        jmp()
      }
    }
  }
}
