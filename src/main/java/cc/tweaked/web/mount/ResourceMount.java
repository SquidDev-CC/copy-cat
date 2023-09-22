package cc.tweaked.web.mount;

import cc.tweaked.web.js.Callbacks;
import dan200.computercraft.core.filesystem.AbstractInMemoryMount;
import org.teavm.jso.typedarrays.Int8Array;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;

/**
 * Mounts in files from an external {@code resources.js} file.
 *
 * @see Callbacks#listResources()
 * @see Callbacks#getResource(String)
 */
public class ResourceMount extends AbstractInMemoryMount<ResourceMount.FileEntry> {
    private static final String PREFIX = "rom/";

    private static ResourceMount instance;
    private static Int8Array bios;

    private ResourceMount() {
        root = new FileEntry("");
        for (var file : Callbacks.listResources()) {
            if (file.startsWith(PREFIX)) getOrCreateChild(root, file.substring(PREFIX.length()), FileEntry::new);
        }
    }

    /**
     * Get the global {@code rom} mount
     *
     * @return The rom mount.
     */
    public static ResourceMount rom() {
        return instance != null ? instance : (instance = new ResourceMount());
    }

    /**
     * Get an input stream for {@code bios.lua}.
     *
     * @return An input stream for the bios.
     */
    public static InputStream bios() {
        var biosContents = bios != null ? bios : (bios = Callbacks.getResource("bios.lua"));
        return Channels.newInputStream(new Int8ArrayByteChannel(biosContents));
    }

    @Override
    protected long getSize(FileEntry file) throws IOException {
        return file.isDirectory() ? 0 : getContents(file).getLength();
    }

    @Override
    protected SeekableByteChannel openForRead(FileEntry file) throws IOException {
        return new Int8ArrayByteChannel(getContents(file));
    }

    private Int8Array getContents(FileEntry file) {
        return file.contents != null ? file.contents : (file.contents = Callbacks.getResource(PREFIX + file.path));
    }

    static final class FileEntry extends AbstractInMemoryMount.FileEntry<FileEntry> {
        private @Nullable Int8Array contents;

        FileEntry(String path) {
            super(path);
        }
    }
}
