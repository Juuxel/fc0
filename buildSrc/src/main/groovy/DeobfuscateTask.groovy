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
        def inputTiny = Files.createTempFile("raw.", ".tiny")
        def mergedTiny = Files.createTempFile("merged.", ".tiny")
        def invertedIntermediary = Files.createTempFile("inverted_intermediary.", ".tiny")
        def unorderedTiny = Files.createTempFile("unordered.", ".tiny")
        def converter = new ConvertMappingsCommand()

        // Convert to tiny v2
        converter.run("enigma", mappings.absolutePath, "tinyv2:intermediary:named", inputTiny.toString())
        new InvertMappingsCommand().run("tinyv2", intermediaryMappings.absolutePath, "tinyv2:intermediary:official", invertedIntermediary.toString())

        // Merge
        new CommandMergeTinyV2().run(invertedIntermediary.toString(), inputTiny.toString(), mergedTiny.toString())

        // Propose names
        new CommandProposeV2FieldNames().run([inputJar.absolutePath, mergedTiny.toString(), unorderedTiny.toString(), "false"] as String[])

        // Reorder
        new CommandReorderTinyV2().run([unorderedTiny.toString(), outputMappings.absolutePath, "official", "intermediary", "named"] as String[])

        def remapper = TinyRemapper.newRemapper()
            .fixPackageAccess(true)
            .withMappings(TinyUtils.createTinyMappingProvider(outputMappings.toPath(), "intermediary", "named")) // FIXME: why does this put intermediary classes in the jar????
            .renameInvalidLocals(true)
            .build()

        JarRemapping.remapJar(remapper, inputJar, outputJar, libraries)
    }
}
