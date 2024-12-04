import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class SquaremapPlatformExtension @Inject constructor() {
  abstract val productionJar: RegularFileProperty

  abstract val modInfoFilePath: Property<String>
}
