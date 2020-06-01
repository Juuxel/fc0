import cuchaz.enigma.command.ConvertMappingsCommand
import cuchaz.enigma.command.InvertMappingsCommand
import net.fabricmc.stitch.commands.tinyv2.CommandMergeTinyV2
import net.fabricmc.stitch.commands.tinyv2.CommandProposeV2FieldNames
import net.fabricmc.stitch.commands.tinyv2.CommandReorderTinyV2
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

    @InputFile
    File intermediaryMappings

    @InputFiles
    FileCollection libraries

    @InputFile
    File inputJar

    @OutputFile
    File outputJar

    @OutputFile
    File outputMappings

    @TaskAction
    def run() {
        Files.deleteIfExists(outputJar.toPath())

        def inputTiny = Files.createTempFile("raw.", ".tiny")
        def proposedTiny = Files.createTempFile("proposed.", ".tiny")
        def mergedTiny = Files.createTempFile("merged.", ".tiny")
        def invertedIntermediary = Files.createTempFile("inverted_intermediary.", ".tiny")
        def converter = new ConvertMappingsCommand()

        // Convert to tiny v2
        converter.run("enigma", mappings.absolutePath, "tinyv2:intermediary:named", inputTiny.toString())
        new InvertMappingsCommand().run("tiny", intermediaryMappings.absolutePath, "tinyv2:intermediary:official", invertedIntermediary.toString())

        // Propose names
        new CommandProposeV2FieldNames().run([inputJar.absolutePath, inputTiny.toString(), proposedTiny.toString(), "false"] as String[])

        // Merge
        new CommandMergeTinyV2().run(invertedIntermediary.toString(), proposedTiny.toString(), mergedTiny.toString())

        // Reorder
        new CommandReorderTinyV2().run([mergedTiny.toString(), outputMappings.absolutePath, "official", "intermediary", "named"] as String[])

        def remapper = TinyRemapper.newRemapper()
            .fixPackageAccess(true)
            .withMappings(TinyUtils.createTinyMappingProvider(outputMappings.toPath(), "intermediary", "named"))
            .renameInvalidLocals(true)
            .fixPackageAccess(true)
            .build()

        JarRemapping.remapJar(remapper, inputJar, outputJar, libraries)
    }
}
