import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class GeneratePatchesTask extends DefaultTask {
    @InputDirectory
    File cleanSources

    @InputDirectory
    File revisedSources

    @OutputDirectory
    File patchOutput

    @Input
    int contextSize = 2

    @TaskAction
    def run() {
        patchOutput.deleteDir()
        patchOutput.mkdirs()

        Files.walk(cleanSources.toPath())
            .filter { Files.isRegularFile(it) }
            .forEach {
                def relative = cleanSources.toPath().relativize(it)
                def revised = revisedSources.toPath().resolve(relative)
                def cleanLines = Files.readAllLines(it)
                def revisedLines = Files.readAllLines(revised)
                def patch = DiffUtils.diff(cleanLines, revisedLines)
                def patchLines = UnifiedDiffUtils.generateUnifiedDiff("a/$relative", "b/$relative", cleanLines, patch, contextSize)

                if (!patchLines.isEmpty()) {
                    def sourceName = relative.toString()
                    def patchName = sourceName.substring(0, sourceName.length() - ".java".length()) + ".patch"
                    def patchPath = patchOutput.toPath().resolve(patchName)
                    Files.createDirectories(patchPath.parent)
                    Files.write(patchPath, patchLines, StandardCharsets.UTF_8)
                }
            }
    }
}
