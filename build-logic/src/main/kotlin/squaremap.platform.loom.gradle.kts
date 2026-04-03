plugins {
  id("squaremap.platform.mod")
  id("xyz.jpenilla.quiet-fabric-loom")
}

val platformExt = extensions.getByType<SquaremapPlatformExtension>()
platformExt.productionJar = tasks.shadowJar.flatMap { it.archiveFile }
