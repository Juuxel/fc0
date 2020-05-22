import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ObfuscateTask extends DefaultTask {
    @InputFile
    File inputJar

    @InputFile
    File mappings

    @InputFiles
    FileCollection libraries

    @OutputFile
    File outputJar

    @TaskAction
    def run() {
        def remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappings.toPath(), "named", "official"))
            .build()

        JarRemapping.remapJar(remapper, inputJar, outputJar, libraries)
    }
}
