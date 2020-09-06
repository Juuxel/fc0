import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input

@FunctionalInterface
interface Decompiler {
    @Input
    String getName()

    void decompile(File input, FileCollection libraries, File output)
}
