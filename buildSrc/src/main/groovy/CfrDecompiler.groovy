import juuxel.fc0.tools.source.EnumFixer
import org.benf.cfr.reader.api.CfrDriver
import org.benf.cfr.reader.api.OutputSinkFactory
import org.benf.cfr.reader.api.SinkReturns
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input

import java.nio.charset.StandardCharsets
import java.nio.file.Files

class CfrDecompiler implements Decompiler {
    @Input
    @Override
    String getName() {
        return "CFR"
    }

    @Override
    void decompile(File input, FileCollection libraries, File output) {
        def driver = new CfrDriver.Builder()
            .withOptions(
                usenametable: 'false',
                sugarenums: 'true',
                extraclasspath: libraries.collect { it.absolutePath }.join(File.pathSeparator)
            )
            .withOutputSink(new SinkFactory(output))
            .build()
        driver.analyse([input.getAbsolutePath()])

        Files.walk(output.toPath())
            .filter { Files.isRegularFile(it) && it.toString().endsWith(".java") }
            .forEach {
                def lines = Files.readAllLines(it)
                def newLines = EnumFixer.fixEnums(lines)

                if (newLines != null) {
                    Files.write(it, newLines, StandardCharsets.UTF_8)
                }
            }
    }

    private static void accept(File output, SinkReturns.Decompiled decompiled) {
        def dir = output.toPath().resolve(decompiled.packageName.replace('.', '/'))
        Files.createDirectories(dir)
        def sourceFile = dir.resolve(decompiled.className + ".java")

        Files.write(sourceFile, decompiled.java.getBytes(StandardCharsets.UTF_8))
    }

    private class SinkFactory implements OutputSinkFactory {
        private File output

        SinkFactory(File output) {
            this.output = output
        }

        @Override
        List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
            return [SinkClass.DECOMPILED]
        }

        @Override
        <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
            return (sinkClass == SinkClass.DECOMPILED && sinkType == SinkType.JAVA)
                ? { data -> accept(output, data as SinkReturns.Decompiled) }
                : null
        }
    }
}
