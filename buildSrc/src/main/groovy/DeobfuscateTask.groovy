import cuchaz.enigma.command.ConvertMappingsCommand
import net.fabricmc.stitch.commands.tinyv2.CommandProposeV2FieldNames
import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import net.fabricmc.tinyremapper.TinyUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

class DeobfuscateTask extends DefaultTask {
    @InputDirectory
    File mappings

    @InputFiles
    FileCollection libraries

    @InputFile
    File inputJar

    @OutputFile
    File outputJar

    @TaskAction
    def run() {
        def inputTiny = Files.createTempFile("raw.", ".tiny")
        def proposedTiny = Files.createTempFile("proposed.", ".tiny")
        def converter = new ConvertMappingsCommand()

        // Convert to tiny v2
        converter.run("enigma", mappings.absolutePath, "tinyv2:official:named", inputTiny.toString())

        // Propose names
        new CommandProposeV2FieldNames().run([inputJar.absolutePath, inputTiny.toString(), proposedTiny.toString(), "false"] as String[])

        def remapper = TinyRemapper.newRemapper()
            .fixPackageAccess(true)
            .withMappings(TinyUtils.createTinyMappingProvider(proposedTiny, "official", "named"))
            .renameInvalidLocals(true)
            .build()

        def consumer = new OutputConsumerPath.Builder(outputJar.toPath()).build()
        try {
            consumer.addNonClassFiles(inputJar.toPath())

            remapper.readInputs(inputJar.toPath())
            for (def lib : libraries) {
                remapper.readClassPath(lib.toPath())
            }

            remapper.apply(consumer)
        } finally {
            consumer.close()
            remapper.finish()
        }
    }
}
