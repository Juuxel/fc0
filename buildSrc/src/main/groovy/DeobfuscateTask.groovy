import cuchaz.enigma.command.DeobfuscateCommand
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class DeobfuscateTask extends DefaultTask {
    @InputDirectory
    File mappings

    @InputFile
    File inputJar

    @OutputFile
    File outputJar

    @TaskAction
    def run() {
        outputJar.parentFile.mkdirs()
        new DeobfuscateCommand().run(inputJar.absolutePath, outputJar.absolutePath, mappings.absolutePath)
    }
}
