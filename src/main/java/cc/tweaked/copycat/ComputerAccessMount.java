package cc.tweaked.copycat;

import cc.tweaked.copycat.js.ExtendedComputerDisplay;
import cc.tweaked.copycat.js.ExtendedComputerDisplay.Result;
import cc.tweaked.copycat.js.FileSystemEntry;
import cc.tweaked.copycat.js.JsFileAttributes;
import cc.tweaked.copycat.js.MoreJavascriptConv;
import dan200.computercraft.api.filesystem.FileAttributes;
import dan200.computercraft.api.filesystem.FileOperationException;
import dan200.computercraft.api.filesystem.WritableMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.typedarrays.Int8Array;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ComputerAccessMount implements WritableMount {
    private final ExtendedComputerDisplay computer;

    public ComputerAccessMount(ExtendedComputerDisplay computer) {
        this.computer = computer;
    }

    @Override
    public void makeDirectory(@Nonnull String path) throws IOException {
        computer.createDirectory(path).getOrThrow();
    }

    @Override
    public void delete(@Nonnull String path) throws IOException {
        if (path.isEmpty()) throw new FileOperationException(path, "Access denied");

        computer.deleteEntry(path);
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        if (source.isEmpty()) throw new FileOperationException(source, "Access denied");
        // TODO: Implement me again.
        throw new FileOperationException("Cannot move files");
    }

    @Nonnull
    @Override
    public SeekableByteChannel openForWrite(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.createFile(path).getOrThrow();
        return new Writer(entry);
    }

    @Nonnull
    @Override
    public SeekableByteChannel openForAppend(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.createFile(path).getOrThrow();
        return new Writer(entry, entry.getContents());
    }

    @Override
    public long getRemainingSpace() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getCapacity() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean exists(@Nonnull String path) {
        return computer.getEntry(path) != null;
    }

    @Override
    public boolean isDirectory(@Nonnull String path) {
        FileSystemEntry entry = computer.getEntry(path);
        return entry != null && entry.isDirectory();
    }

    @Override
    public void list(@Nonnull String path, @Nonnull List<String> contents) throws IOException {
        FileSystemEntry entry = computer.getEntry(path);
        if (entry == null || !entry.isDirectory()) throw new FileOperationException(path, "Not a directory");
        Collections.addAll(contents, entry.getChildren());
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.getEntry(path);
        if (entry == null) throw new FileOperationException(path, "No such file");
        return entry.isDirectory() ? 0 : entry.getContents().length;
    }

    @Nonnull
    @Override
    public SeekableByteChannel openForRead(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.getEntry(path);
        if (entry == null || entry.isDirectory()) throw new FileOperationException(path, "No such file");
        return new ArrayByteChannel(entry.getContents());
    }

    @Nonnull
    @Override
    public BasicFileAttributes getAttributes(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.getEntry(path);
        if (entry == null) throw new FileOperationException(path, "No such file");
        JsFileAttributes attr = entry.getAttributes();
        return new FileAttributes(
            attr.getDirectory(), (long) attr.getSize(),
            FileTime.fromMillis((long) attr.getCreation()), FileTime.fromMillis((long) attr.getModification())
        );
    }

    private static class Writer implements SeekableByteChannel {
        private static final Int8Array EMPTY = Int8Array.create(0);

        private final FileSystemEntry entry;

        private boolean closed = false;
        private byte[] contents;
        private int position;
        private int size;

        private Writer(@Nonnull FileSystemEntry entry) {
            this.entry = entry;
        }

        private Writer(@Nonnull FileSystemEntry entry, byte[] existing) {
            this.entry = entry;
            this.contents = existing;
            this.position = this.size = existing.length;
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new IOException("Cannot truncate");
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if (newPosition > Integer.MAX_VALUE) throw new IOException("Cannot seek beyond 2^31");
            this.position = (int) newPosition;
            return this;
        }

        private void preWrite(int extra) throws IOException {
            int end = position + extra;
            if (end < 0 || end > Integer.MAX_VALUE >> 1) throw new IOException("File is too large");

            if (contents == null) {
                // Allocate an initial buffer
                contents = new byte[Math.max(16, end)];
            } else if (end > contents.length) {
                // Grow our existing buffer
                int newCapacity = contents.length << 1;
                if (newCapacity - end < 0) newCapacity = end;
                contents = Arrays.copyOf(contents, newCapacity);
            }
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            if (closed) throw new IOException("Stream is closed");

            int length = buffer.remaining();
            preWrite(length);
            int position = this.position;
            for (int i = 0; i < length; i++) contents[position + i] = buffer.get();
            size = Math.max(size, this.position += length);
            return length;
        }

        @Override
        public boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() throws IOException {
            if (closed) return;
            closed = true;

            Int8Array contentsArray;
            if (contents == null) {
                contentsArray = EMPTY;
            } else {
                var wrapped = MoreJavascriptConv.ofByteArray(contents);
                contentsArray = Int8Array.create(wrapped.getBuffer(), wrapped.getByteOffset(), size);
            }
            Result<JSBoolean> result = entry.setContents(contentsArray);
            contents = null;
            result.getOrThrow();
        }

        @Override
        public int read(ByteBuffer buffer) throws IOException {
            throw new IOException("Cannot read from this buffer");
        }
    }
}
