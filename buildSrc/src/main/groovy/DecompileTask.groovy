import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.SinkReturns
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files

class DecompileTask extends DefaultTask {
    @Input
    File input

    @Input
    File output

    @TaskAction
    def run() {
        output.mkdirs()

        def driver = new CfrDriver.Builder()
                .withOptions(renamedupmembers: 'true')
                .withOutputSink(new SinkFactory())
                .build()
        driver.analyse([input.getAbsolutePath()])
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
