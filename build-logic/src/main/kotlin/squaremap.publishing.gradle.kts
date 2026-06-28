import org.incendo.cloudbuildlogic.jmp

plugins {
  id("squaremap.base-conventions")
  id("net.kyori.indra.publishing")
  id("org.incendo.cloud-build-logic.publishing")
  id("org.incendo.cloud-build-logic.javadoc-links")
}

signing {
  val signingKey = project.findProperty("signingKey") as String?
  val signingPassword = project.findProperty("signingPassword") as String?
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

javadocLinks {
  defaultJavadocProvider = "https://www.javadocs.dev/{group}/{name}/{version}"
}
