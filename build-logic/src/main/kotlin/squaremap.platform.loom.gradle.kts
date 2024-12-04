plugins {
  id("squaremap.platform.mod")
  id("quiet-fabric-loom")
}

val platformExt = extensions.getByType<SquaremapPlatformExtension>()
platformExt.productionJar = tasks.remapJar.flatMap { it.archiveFile }
