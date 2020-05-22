import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class RemapJarTask extends DefaultTask {
    @InputFile
    File inputJar

    @InputFile
    File mappings

    @InputFiles
    FileCollection libraries

    @OutputFile
    File outputJar

    @Input
    String from

    @Input
    String to

    @TaskAction
    def run() {
        def remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), from, to))
            .build()

        JarRemapping.remapJar(remapper, inputJar, outputJar, libraries)
    }
}
