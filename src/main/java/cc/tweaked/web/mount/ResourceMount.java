package cc.tweaked.web.mount;

import cc.tweaked.web.stub.SeekableByteChannel;
import com.google.common.io.ByteStreams;
import dan200.computercraft.api.filesystem.Mount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourceMount implements Mount {
    private static final byte[] TEMP_BUFFER = new byte[8192];

    private final String subPath;

    private final FileEntry root;

    public ResourceMount(String subPath) {
        this.subPath = subPath;

        FileEntry newRoot = new FileEntry(subPath);
        for (String file : Resources.FILES) create(newRoot, file);
        root = newRoot;
    }

    private FileEntry get(String path) {
        FileEntry lastEntry = root;
        int lastIndex = 0;

        while (lastEntry != null && lastIndex < path.length()) {
            int nextIndex = path.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = path.length();

            lastEntry = lastEntry.children == null ? null : lastEntry.children.get(path.substring(lastIndex, nextIndex));
            lastIndex = nextIndex + 1;
        }

        return lastEntry;
    }

    private void create(FileEntry lastEntry, String path) {
        int lastIndex = 0;
        while (lastIndex < path.length()) {
            int nextIndex = path.indexOf('/', lastIndex);
            if (nextIndex < 0) nextIndex = path.length();

            String part = path.substring(lastIndex, nextIndex);
            if (lastEntry.children == null) lastEntry.children = new HashMap<>();

            FileEntry nextEntry = lastEntry.children.get(part);
            if (nextEntry == null) {
                lastEntry.children.put(part, nextEntry = new FileEntry(subPath + "/" + path));
            }

            lastEntry = nextEntry;
            lastIndex = nextIndex + 1;
        }
    }

    @Override
    public boolean exists(@Nonnull String path) {
        return get(path) != null;
    }

    @Override
    public boolean isDirectory(@Nonnull String path) {
        FileEntry file = get(path);
        return file != null && file.isDirectory();
    }

    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
        FileEntry file = get(path);
        if (file == null || !file.isDirectory()) throw new IOException("/" + path + ": Not a directory");

        file.list(contents);
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        FileEntry file = get(path);
        if (file != null) {
            if (file.size != -1) return file.size;
            if (file.isDirectory()) return file.size = 0;

            byte[] contents = file.contents;
            if (contents != null) return file.size = contents.length;

            try (InputStream s = ResourceMount.class.getClassLoader().getResourceAsStream(file.path)) {
                int total = 0, read = 0;
                do {
                    total += read;
                    read = s.read(TEMP_BUFFER);
                } while (read > 0);

                return file.size = total;
            }
        }

        throw new IOException("/" + path + ": No such file");
    }

    @Nonnull
    @Override
    public SeekableByteChannel openForRead(@Nonnull String path) throws IOException {
        FileEntry file = get(path);
        if (file != null && !file.isDirectory()) {
            byte[] contents = file.contents;
            if (contents != null) return new ArrayByteChannel(contents);

            try (InputStream stream = ResourceMount.class.getClassLoader().getResourceAsStream(file.path)) {
                contents = file.contents = ByteStreams.toByteArray(stream);
                return new ArrayByteChannel(contents);
            }
        }

        throw new IOException("/" + path + ": No such file");
    }

    private static class FileEntry {
        final String path;
        byte[] contents;
        Map<String, FileEntry> children;
        long size = -1;

        FileEntry(String path) {
            this.path = path;
        }

        boolean isDirectory() {
            return children != null;
        }

        void list(List<String> contents) {
            if (children != null) contents.addAll(children.keySet());
        }
    }
}

