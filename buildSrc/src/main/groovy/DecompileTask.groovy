import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class DecompileTask extends DefaultTask {
    @Nested
    Decompiler decompiler = new CfrDecompiler()

    @InputFile
    File input

    @InputFiles
    FileCollection libraries

    @OutputDirectory
    File output

    @OutputDirectory
    File cleanOutput

    @TaskAction
    def run() {
        output.deleteDir()
        output.mkdirs()
        cleanOutput.deleteDir()
        cleanOutput.mkdirs()

        decompiler.decompile(input, libraries, output)

        // https://stackoverflow.com/a/6214823
        new AntBuilder().copy(toDir: cleanOutput.absolutePath) {
            fileset(dir: output.absolutePath)
        }
    }
}
