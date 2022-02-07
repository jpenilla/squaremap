plugins {
  id("base-conventions")
  id("net.kyori.indra.publishing")
}

signing {
  useInMemoryPgpKeys(
    providers.gradleProperty("signingKey").forUseAtConfigurationTime().orNull,
    providers.gradleProperty("signingPassword").forUseAtConfigurationTime().orNull
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
