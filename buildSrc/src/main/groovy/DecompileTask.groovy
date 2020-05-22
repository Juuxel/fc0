import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.SinkReturns
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class DecompileTask extends DefaultTask {
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

        def driver = new CfrDriver.Builder()
            .withOptions(
                renamedupmembers: 'true',
                usenametable: 'false',
                sugarenums: 'true',
                extraclasspath: libraries.collect { it.absolutePath }.join(File.pathSeparator)
            )
            .withOutputSink(new SinkFactory())
            .build()
        driver.analyse([input.getAbsolutePath()])

        // https://stackoverflow.com/a/6214823
        new AntBuilder().copy(toDir: cleanOutput.absolutePath) {
            fileset(dir: output.absolutePath)
        }
    }

    private def accept(SinkReturns.Decompiled decompiled) {
        def dir = output.toPath().resolve(decompiled.packageName.replace('.', '/'))
        Files.createDirectories(dir)
        def sourceFile = dir.resolve(decompiled.className + ".java")

        Files.write(sourceFile, decompiled.java.getBytes(StandardCharsets.UTF_8))
    }

    private class SinkFactory implements OutputSinkFactory {
        @Override
        List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
            return [SinkClass.DECOMPILED]
        }

        @Override
        <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
            return (sinkClass == SinkClass.DECOMPILED && sinkType == SinkType.JAVA)
                ? { data -> accept(data as SinkReturns.Decompiled) }
                : null
        }
    }
}
