import net.kyori.indra.git.IndraGitExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the

fun Project.lastCommitHash(): String = the<IndraGitExtension>().commit()?.name?.substring(0, 7)
  ?: error("Could not determine commit hash")

fun Project.decorateVersion() {
  val versionString = version as String
  version = if (versionString.endsWith("-SNAPSHOT")) {
    "$versionString+${lastCommitHash()}"
  } else {
    versionString
  }
}
