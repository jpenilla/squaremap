import org.gradle.api.file.RegularFileProperty
import javax.inject.Inject

abstract class SquaremapPlatformExtension @Inject constructor() {
  abstract val productionJar: RegularFileProperty
}
