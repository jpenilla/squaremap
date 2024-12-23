import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Not worth caching")
abstract class CopyFile : DefaultTask() {
  @get:InputFile
  abstract val fileToCopy: RegularFileProperty

  @get:OutputFile
  abstract val destination: RegularFileProperty

  @TaskAction
  fun copyFile() {
    destination.get().asFile.parentFile.mkdirs()
    fileToCopy.get().asFile.copyTo(destination.get().asFile, overwrite = true)
  }
}
