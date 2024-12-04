plugins {
  id("squaremap.platform.mod")
  id("net.neoforged.moddev")
}

val prod = tasks.register<Zip>("productionJar") {
  destinationDirectory = layout.buildDirectory.dir("libs")
  from(zipTree(tasks.shadowJar.flatMap { it.archiveFile }))
  // for some reason the inner jars were getting unpacked when from'ing directly to shadowJar...?
  from(tasks.jarJar)
}

val platformExt = extensions.getByType<SquaremapPlatformExtension>()
platformExt.productionJar = prod.flatMap { it.archiveFile }
