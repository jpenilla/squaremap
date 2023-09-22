plugins {
  id("loom-platform-conventions")
}

loom.forge.mixinConfig("squaremap-forge.mixins.json")

dependencies {
  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())
  forge(libs.forge)

  squaremap(projects.squaremapCommon) {
    exclude("cloud.commandframework", "cloud-core")
    exclude("cloud.commandframework", "cloud-minecraft-extras")
    exclude("io.leangen.geantyref")
  }

  implementation(platform(libs.adventureBom))
  include(platform(libs.adventureBom))
  implementation(libs.adventureApi)
  include(libs.adventureApi)
  include(libs.examinationApi)
  include(libs.examinationString)
  include(libs.adventureKey)
  include(libs.miniMessage)
  implementation(libs.adventureTextSerializerGson)
  include(libs.adventureTextSerializerGson)
  include(libs.adventureTextSerializerJson)
  include(libs.adventureTextSerializerPlain)

  modImplementation(libs.cloudForge)
  include(libs.cloudForge)

  implementation(libs.cloudMinecraftExtras) {
    isTransitive = false // we depend on adventure separately
  }
  include(libs.cloudMinecraftExtras)
}

tasks.remapJar {
  archiveFileName.set(productionJarName(libs.versions.minecraft))
}

squaremapLoomPlatform.modInfoFilePath.set("META-INF/mods.toml")

// todo https://github.com/FabricMC/fabric-loom/pull/838
configurations.include {
  isTransitive = true
  withDependencies {
    all {
      if (this !is ModuleDependency) return@all
      val category: Category? = attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
      if (category != null && (category.name == Category.REGULAR_PLATFORM || category.name == Category.ENFORCED_PLATFORM)) {
        return@all
      }
      isTransitive = false
    }
  }
}
