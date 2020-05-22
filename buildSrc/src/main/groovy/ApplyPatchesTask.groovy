import com.github.difflib.UnifiedDiffUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

class ApplyPatchesTask extends DefaultTask {
    @InputDirectory
    File patches

    @InputDirectory
    File cleanSources

    @OutputDirectory
    File intermediateSources

    @OutputDirectory
    File userSources

    @TaskAction
    def run() {
        intermediateSources.deleteDir()
        intermediateSources.mkdirs()
        userSources.deleteDir()
        userSources.mkdirs()

        Files.walk(cleanSources.toPath())
            .filter { Files.isRegularFile(it) }
            .forEach {
                def relative = cleanSources.toPath().relativize(it)
                def sourceName = relative.toString()
                def patchName = sourceName.substring(0, sourceName.length() - ".java".length()) + ".patch"
                def patchPath = patches.toPath().resolve(patchName)

                def target = intermediateSources.toPath().resolve(relative)
                Files.createDirectories(target.parent)

                if (Files.exists(patchPath)) {
                    println "Patching $relative"
                    def patch = UnifiedDiffUtils.parseUnifiedDiff(Files.readAllLines(patchPath))
                    def patched = patch.applyTo(Files.readAllLines(it))
                    Files.write(target, patched)
                } else {
                    Files.copy(it, target)
                }
            }

        // Move clean -> main
        // https://stackoverflow.com/a/6214823
        new AntBuilder().copy(toDir: userSources.absolutePath) {
            fileset(dir: intermediateSources.absolutePath)
        }
    }
}
