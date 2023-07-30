import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.kyori.indra.git.IndraGitExtension
import org.eclipse.jgit.lib.Repository
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.the

val Project.releaseNotes: Provider<String>
  get() = providers.environmentVariable("RELEASE_NOTES")

fun ShadowJar.reloc(pkg: String) {
  relocate(pkg, "squaremap.libraries.$pkg")
}

fun Project.currentBranch(): String {
  System.getenv("GITHUB_HEAD_REF")?.takeIf { it.isNotEmpty() }
    ?.let { return it }
  System.getenv("GITHUB_REF")?.takeIf { it.isNotEmpty() }
    ?.let { return it.replaceFirst("refs/heads/", "") }

  val indraGit = the<IndraGitExtension>().takeIf { it.isPresent }

  val ref = indraGit?.git()?.repository?.exactRef("HEAD")?.target
    ?: return "detached-head"

  return Repository.shortenRefName(ref.name)
}

fun Project.productionJarName(mcVer: Provider<String>): Provider<String> = the<BasePluginExtension>()
  .archivesName.zip(mcVer) { archivesName, mc -> "$archivesName-mc$mc-$version.jar" }

fun Project.productionJarLocation(mcVer: Provider<String>): Provider<RegularFile> =
  productionJarName(mcVer).flatMap { layout.buildDirectory.file("libs/$it") }
