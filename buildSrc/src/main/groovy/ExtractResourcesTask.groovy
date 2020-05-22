import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.FileSystems
import java.nio.file.Files

class ExtractResourcesTask extends DefaultTask {
    @InputFile
    File input

    @OutputDirectory
    File output

    @TaskAction
    def run() {
        def resources = output.toPath()
        output.deleteDir()

        def fs = FileSystems.newFileSystem(URI.create("jar:" + input.toPath().toUri()), Collections.emptyMap(), null)
        try {
            for (def root : fs.rootDirectories) {
                Files.walk(root).filter({ Files.isRegularFile(it) && !it.toString().endsWith(".class") }).forEach({
                    def str = it.toString()
                    def target = resources.resolve(str.startsWith("/") ? str.substring(1) : str)
                    Files.createDirectories(target.parent)
                    Files.copy(it, target)
                })
            }
        } finally {
            fs.close()
        }
    }
}
