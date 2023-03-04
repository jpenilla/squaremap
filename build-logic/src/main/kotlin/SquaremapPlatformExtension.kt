import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class SquaremapPlatformExtension {
  abstract val productionJar: RegularFileProperty

  abstract class Loom : SquaremapPlatformExtension() {
      abstract val modInfoFilePath: Property<String>
  }
}
