import net.fabricmc.tinyremapper.OutputConsumerPath
import net.fabricmc.tinyremapper.TinyRemapper
import org.gradle.api.file.FileCollection

class JarRemapping {
    static void remapJar(TinyRemapper remapper, File input, File output, FileCollection libraries) {
        def consumer = new OutputConsumerPath.Builder(output.toPath()).build()
        try {
            consumer.addNonClassFiles(input.toPath())

            remapper.readInputs(input.toPath())
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
