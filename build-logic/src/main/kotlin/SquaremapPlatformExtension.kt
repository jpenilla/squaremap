import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class SquaremapPlatformExtension @Inject constructor(objects: ObjectFactory) {
  val productionJar: RegularFileProperty = objects.fileProperty()
}
