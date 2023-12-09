import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

abstract class SquaremapPlatformExtension @Inject constructor(objects: ObjectFactory) {
  abstract val productionJar: RegularFileProperty
  val loom: Loom = objects.newInstance(Loom::class)

  abstract class Loom {
      abstract val modInfoFilePath: Property<String>
  }
}
