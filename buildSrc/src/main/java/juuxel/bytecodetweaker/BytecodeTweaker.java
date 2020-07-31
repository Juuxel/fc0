package juuxel.bytecodetweaker;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.SyntaxError;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class BytecodeTweaker {
    private final Map<String, UnaryOperator<ClassVisitor>> visitors;

    public BytecodeTweaker(File file) throws IOException, SyntaxError {
        this(TweakerConfig.fromJson(Jankson.builder().build().load(file)));
    }

    public BytecodeTweaker(TweakerConfig config) {
        this.visitors = new HashMap<>();

        Set<String> classNames = new HashSet<>();
        classNames.addAll(config.bridges.keySet());

        for (String className : classNames) {
            Set<String> bridges = config.bridges.getOrDefault(className, Collections.emptySet());
            visitors.put(className, cv -> new Tweak(cv, bridges));
        }
    }

    public void run(Path jar) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put("create", false);

        try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:" + jar.toUri()), params)) {
            for (Map.Entry<String, UnaryOperator<ClassVisitor>> entry : visitors.entrySet()) {
                String className = entry.getKey();

                String[] split = className.split("/");
                split[split.length - 1] += ".class"; // append the .class extension to the final component

                String[] rest = new String[split.length - 1];
                System.arraycopy(split, 1, rest, 0, rest.length);

                Path classPath = fs.getPath(split[0], rest);

                try (InputStream in = Files.newInputStream(classPath)) {
                    ClassReader reader = new ClassReader(in);
                    ClassWriter writer = new ClassWriter(reader, 0);

                    reader.accept(entry.getValue().apply(writer), 0);
                    Files.write(classPath, writer.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                } catch (Exception e) {
                    throw new IOException("Could not transform class " + className, e);
                }
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private static final class Tweak extends ClassVisitor {
        private final Set<String> bridges;

        Tweak(ClassVisitor classVisitor, Set<String> bridges) {
            super(Opcodes.ASM5, classVisitor);
            this.bridges = bridges;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (bridges.contains(name + descriptor)) {
                access |= Opcodes.ACC_BRIDGE;
            }

            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }
}
