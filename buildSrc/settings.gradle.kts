import ca.stellardrift.build.configurate.ConfigFormats
import ca.stellardrift.build.configurate.catalog.PolyglotVersionCatalogExtension

plugins {
  id("ca.stellardrift.polyglot-version-catalogs")
}

extensions.configure<PolyglotVersionCatalogExtension> {
  from(ConfigFormats.YAML, file("../gradle/libs.versions.yml"))
}
