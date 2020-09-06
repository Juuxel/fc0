import com.strobel.assembler.metadata.CompositeTypeLoader
import com.strobel.assembler.metadata.ITypeLoader
import com.strobel.assembler.metadata.JarTypeLoader
import com.strobel.assembler.metadata.MetadataSystem
import com.strobel.assembler.metadata.TypeDefinition
import com.strobel.assembler.metadata.TypeReference
import com.strobel.decompiler.DecompilationOptions
import com.strobel.decompiler.DecompilerSettings
import com.strobel.decompiler.PlainTextOutput
import com.strobel.decompiler.languages.java.BraceStyle
import com.strobel.decompiler.languages.java.JavaFormattingOptions
import cuchaz.enigma.source.procyon.typeloader.NoRetryMetadataSystem
import org.gradle.api.file.FileCollection

import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile

class ProcyonDecompiler implements Decompiler {
    @Override
    String getName() {
        return "Procyon"
    }

    @Override
    void decompile(File input, FileCollection libraries, File output) {
        JarFile inputJar = new JarFile(input)
        List<ITypeLoader> typeLoaders = []
        typeLoaders.add(new JarTypeLoader(inputJar))

        for (File library : libraries) {
            typeLoaders.add(new JarTypeLoader(new JarFile(library)))
        }

        ITypeLoader typeLoader = new CompositeTypeLoader(typeLoaders as ITypeLoader[])

        DecompilerSettings settings = DecompilerSettings.javaDefaults()
        settings.forceExplicitImports = true
        settings.typeLoader = typeLoader

        JavaFormattingOptions format = settings.javaFormattingOptions
        format.ClassBraceStyle = BraceStyle.EndOfLine
        format.InterfaceBraceStyle = BraceStyle.EndOfLine
        format.EnumBraceStyle = BraceStyle.EndOfLine

        MetadataSystem metadataSystem = new NoRetryMetadataSystem(typeLoader)
        DecompilationOptions options = new DecompilationOptions()
        options.settings = settings

        Path out = output.toPath()
        FileSystem inputFs = FileSystems.newFileSystem(URI.create("jar:" + input.toURI()), [create: "false"])
        try {
            for (Path rootDirectory : inputFs.rootDirectories) {
                Files.find(rootDirectory, Integer.MAX_VALUE, { path, attributes -> attributes.isRegularFile() }).forEach {
                    Path relative = rootDirectory.relativize(it)

                    if (it.toString().endsWith(".class")) {
                        String relativePath = relative.toString()
                        String className = relativePath.substring(0, relativePath.length() - ".class".length())

                        Path target = out.resolve(className + ".java")
                        Path parent = target.parent

                        if (parent != null) {
                            Files.createDirectories(parent)
                        }

                        TypeReference type = metadataSystem.lookupType(className)
                        TypeDefinition resolvedType = type.resolve()

                        def stream = new ByteArrayOutputStream()
                        def writer = new OutputStreamWriter(stream)

                        try {
                            PlainTextOutput arrayOutput = new PlainTextOutput(writer)
                            settings.language.decompileType(resolvedType, arrayOutput, options)

                            writer.flush()
                            byte[] bytes = stream.toByteArray()
                            String code = new String(bytes, StandardCharsets.UTF_8)
                            String[] lines = code.split("\n")
                            Files.write(target, sortImports(lines), StandardCharsets.UTF_8)
                        } finally {
                            stream.close()
                            writer.close()
                        }
                    }
                }
            }
        } finally {
            inputFs.close()
        }
    }

    private static List<String> sortImports(String[] lines) {
        boolean foundImports = false
        int importStart = -1 // inclusive
        int importEnd = -1   // exclusive

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i]

            if (line.startsWith("import ")) {
                if (!foundImports) {
                    foundImports = true
                    importStart = i
                }
            } else if (foundImports) {
                importEnd = i
                break
            }
        }

        if (!foundImports) {
            return lines.toList()
        }

        String[] imports = new String[importEnd - importStart]
        System.arraycopy(lines, importStart, imports, 0, imports.length)
        Arrays.sort(imports)

        List<String> result = new ArrayList<>()

        for (int i = 0; i < importStart; i++) {
            result.add(lines[i])
        }

        for (String line : imports) {
            result.add(line)
        }

        for (int i = importEnd; i < lines.length; i++) {
            result.add(lines[i])
        }

        return result
    }
}
