import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CopyFile : DefaultTask() {
  @get:InputFile
  abstract val fileToCopy: RegularFileProperty

  @get:OutputFile
  abstract val destination: RegularFileProperty

  @TaskAction
  private fun copyFile() {
    destination.get().asFile.parentFile.mkdirs()
    fileToCopy.get().asFile.copyTo(destination.get().asFile, overwrite = true)
  }
}
