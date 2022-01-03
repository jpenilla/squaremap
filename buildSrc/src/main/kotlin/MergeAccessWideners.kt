import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class MergeAccessWideners : DefaultTask() {
  @get:InputFiles
  abstract val aw: ConfigurableFileCollection

  @get:OutputFile
  abstract val merged: RegularFileProperty

  @TaskAction
  fun run() {
    val lines = arrayListOf<String>()
    lines += "accessWidener\tv1\tnamed"
    for (file in aw.files) {
      for (line in file.readLines()) {
        if (!line.startsWith('#') // comment
          && !line.startsWith("accessWidener") // header
          && line.isNotBlank() // empty lines
        ) {
          lines += line
        }
      }
    }
    merged.asFile.get().parentFile.mkdirs()
    merged.asFile.get().writeText(lines.joinToString("\n"))
  }
}
