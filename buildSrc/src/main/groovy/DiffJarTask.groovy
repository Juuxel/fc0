import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files

class DiffJarTask extends DefaultTask {
    @InputFile
    File originJar

    @InputFiles
    FileCollection patches

    @InputFile
    File modifiedJar

    @OutputFile
    File output

    private static FileSystem createFs(File file, boolean create) {
        return FileSystems.newFileSystem(URI.create("jar:" + file.toURI()), [create: create.toString()])
    }

    @TaskAction
    def run() {
        Files.deleteIfExists(output.toPath())

        def originFs = createFs(originJar, false)
        def modifiedFs = createFs(modifiedJar, false)
        def diffFs = createFs(output, true)

        try {
            for (def root : modifiedFs.rootDirectories) {
                Files.walk(root).filter { Files.isRegularFile(it) }
                    .forEach {
                        def relative = root.relativize(it)
                        def target = diffFs.getPath(relative.toString())
                        boolean shouldCopy
                        if (it.toString().endsWith(".class")) {
                            def hasPatch = patches.any {
                                def sourceName = relative.toString()
                                def patchName = sourceName.substring(0, sourceName.length() - ".class".length()) + ".patch"
                                Files.exists(it.toPath().resolve(patchName))
                            }

                            shouldCopy = hasPatch || Files.notExists(originFs.getPath(relative.toString()))
                        } else { // Resources: check equality
                            def origin = originFs.getPath(relative.toString())
                            byte[] bytesA = Files.readAllBytes(origin)
                            byte[] bytesB = Files.readAllBytes(it)

                            shouldCopy = !Arrays.equals(bytesA, bytesB)
                        }

                        if (shouldCopy) {
                            def parent = target.parent
                            if (parent != null) Files.createDirectories(parent)
                            Files.copy(it, target)
                        }
                    }
            }
        } finally {
            originFs.close()
            modifiedFs.close()
            diffFs.close()
        }
    }
}
