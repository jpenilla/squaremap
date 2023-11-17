import org.gradlex.javaecosystem.capabilities.rules.GuavaListenableFutureRule

plugins {
  id("loom-platform-conventions")
}

repositories {
  maven("https://maven.neoforged.net/releases/")
}

dependencies {
  components.withModule(GuavaListenableFutureRule.MODULES[0]) {
    // Ad-hoc rule to revert the effect of 'GuavaListenableFutureRule' (NeoForge has broken dependencies)
    allVariants {
      withCapabilities {
        removeCapability(GuavaListenableFutureRule.CAPABILITY_GROUP, GuavaListenableFutureRule.CAPABILITY_NAME)
      }
    }
  }

  minecraft(libs.minecraft)
  mappings(loom.officialMojangMappings())
  neoForge(libs.neoforge)

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

  modImplementation(libs.cloudNeoForge)
  include(libs.cloudNeoForge)

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

publishMods.modrinth {
  minecraftVersions.add(libs.versions.minecraft)
  modLoaders.add("neoforge")
}
