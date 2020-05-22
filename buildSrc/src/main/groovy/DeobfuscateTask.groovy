import cuchaz.enigma.command.DeobfuscateCommand
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class DeobfuscateTask extends DefaultTask {
    @Input
    File mappings

    @Input
    File inputJar

    @Input
    File outputJar

    @TaskAction
    def run() {
        outputJar.parentFile.mkdirs()
        new DeobfuscateCommand().run(inputJar.absolutePath, outputJar.absolutePath, mappings.absolutePath)
    }
}
