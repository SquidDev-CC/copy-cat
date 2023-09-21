package cc.tweaked.builder;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class RemappingClassLoader extends ClassLoader {
    private final Map<String, String> remappedClasses = new HashMap<>();
    private final Map<String, Path> remappedResources = new HashMap<>();

    private final Remapper remapper = new Remapper() {
        @Override
        public String map(String internalName) {
            return remappedClasses.getOrDefault(internalName, internalName);
        }
    };

    private final List<Path> classpath;

    // Cache of the last transformed file - TeaVM tends to call getResourceAsStream multiple times.
    private TransformedClass lastFile;

    public RemappingClassLoader(List<Path> classpath) {
        this.classpath = classpath;
    }

    public void add(String from, String to, Path fromLocation) {
        remappedClasses.put(from, to);
        remappedResources.put(to + ".class", fromLocation);
    }

    public void add(String from, String to) {
        remappedClasses.put(from, to);
    }

    private Path findUnmappedFile(String name) {
        return classpath.stream().map(x -> x.resolve(name)).filter(Files::exists).findFirst().orElse(null);
    }

    private Path findFile(String name) {
        var path = remappedResources.get(name);
        return path != null ? path : findUnmappedFile(name);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // findClass is only called for compile time, and so we load the original class, not the remapped one.
        // Yes, this is super cursed.
        var path = findUnmappedFile(name.replace('.', '/') + ".class");
        if (path == null) throw new ClassNotFoundException();

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ClassNotFoundException("Failed reading " + name, e);
        }

        return defineClass(name, bytes, 0, bytes.length);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        if (!name.endsWith(".class")) return super.getResourceAsStream(name);

        var lastFile = this.lastFile;
        if (lastFile != null && lastFile.name().equals(name)) return new ByteArrayInputStream(lastFile.contents());

        var path = findFile(name);
        if (path == null) return null;

        ClassReader reader;
        try (InputStream stream = Files.newInputStream(path)) {
            reader = new ClassReader(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading " + name, e);
        }

        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new ClassRemapper(writer, remapper), 0);

        var bytes = writer.toByteArray();
        this.lastFile = new TransformedClass(name, bytes);
        return new ByteArrayInputStream(bytes);
    }

    @Override
    protected URL findResource(String name) {
        var path = findFile(name);
        return path == null ? null : toURL(path);
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        var path = remappedResources.get(name);
        return new IteratorEnumeration<>(
            (path == null ? classpath.stream().map(x -> x.resolve(name)) : Stream.of(path))
                .filter(Files::exists)
                .map(RemappingClassLoader::toURL)
                .iterator()
        );
    }

    private record IteratorEnumeration<T>(Iterator<T> iterator) implements Enumeration<T> {
        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }
    }

    private static URL toURL(Path path) {
        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Cannot convert " + path + " to a URL", e);
        }
    }


    private record TransformedClass(String name, byte[] contents) {
    }
}
