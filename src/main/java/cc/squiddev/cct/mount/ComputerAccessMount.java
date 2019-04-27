package cc.squiddev.cct.mount;

import cc.squiddev.cct.js.ComputerAccess;
import cc.squiddev.cct.js.ComputerAccess.Result;
import cc.squiddev.cct.js.FileSystemEntry;
import cc.squiddev.cct.stub.ReadableByteChannel;
import cc.squiddev.cct.stub.SeekableByteChannel;
import cc.squiddev.cct.stub.WritableByteChannel;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.shared.util.StringUtil;
import org.squiddev.cobalt.LuaString;
import org.teavm.jso.core.JSBoolean;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ComputerAccessMount implements IWritableMount {
    private final ComputerAccess computer;

    public ComputerAccessMount(ComputerAccess computer) {
        this.computer = computer;
    }

    @Override
    public void makeDirectory(@Nonnull String path) throws IOException {
        computer.createDirectory(path).getOrThrow();
    }

    @Override
    public void delete(@Nonnull String path) throws IOException {
        if (path.isEmpty()) throw new IOException("/: Access denied");

        computer.deleteEntry(path);
    }

    @Nonnull
    @Override
    public WritableByteChannel openChannelForWrite(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.createFile(path).getOrThrow();
        return new Writer(entry);
    }

    @Nonnull
    @Override
    public WritableByteChannel openChannelForAppend(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.createFile(path).getOrThrow();
        return new Writer(entry, StringUtil.encodeString(entry.getContents()));
    }

    @Override
    public long getRemainingSpace() {
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
        if (entry == null || !entry.isDirectory()) throw new IOException("/" + path + ": Not a directory");
        Collections.addAll(contents, entry.getChildren());
    }

    @Override
    public long getSize(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.getEntry(path);
        if (entry == null) throw new IOException("/" + path + ": No such file");
        return entry.isDirectory() ? 0 : entry.getContents().length();
    }

    @Nonnull
    @Override
    public ReadableByteChannel openChannelForRead(@Nonnull String path) throws IOException {
        FileSystemEntry entry = computer.getEntry(path);
        if (entry == null || entry.isDirectory()) throw new IOException("/" + path + ": No such file");
        return new ArrayByteChannel(StringUtil.encodeString(entry.getContents()));
    }

    private static class Writer extends OutputStream implements SeekableByteChannel {
        private final FileSystemEntry entry;

        private boolean closed = false;
        private byte[] contents;
        private long position;
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
        public SeekableByteChannel position(long newPosition) {
            this.position = newPosition;
            return this;
        }

        private void preWrite(int extra) throws IOException {
            long end = position + extra;
            if (end < 0 || end > Integer.MAX_VALUE >> 1) throw new IOException("File is too large");

            int required = (int) end;
            if (contents == null) {
                // Allocate an initial buffer
                contents = new byte[Math.max(16, required)];
            } else if (required > contents.length) {
                // Grow our existing buffer
                int newCapacity = contents.length << 1;
                if (newCapacity - required < 0) newCapacity = required;
                contents = Arrays.copyOf(contents, newCapacity);
            }
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            if (closed) throw new IOException("Stream is closed");

            int length = buffer.remaining();
            preWrite(length);
            buffer.put(contents, (int) position, length);
            size = (int) (position += length);
            System.out.println(buffer + " / " + Arrays.toString(contents));
            return length;
        }

        @Override
        public void write(int b) throws IOException {
            if (closed) throw new IOException("Stream is closed");

            preWrite(1);
            contents[(int) position] = (byte) b;
            size = (int) (position += 1);
            System.out.println(Arrays.toString(contents));
        }

        @Override
        public void write(@Nonnull byte[] b, int offset, int length) throws IOException {
            if (closed) throw new IOException("Stream is closed");

            preWrite(length);
            System.arraycopy(b, offset, contents, (int) position, length);
            size = (int) (position += length);
            System.out.println(Arrays.toString(b) + " / " + Arrays.toString(contents));
        }

        @Override
        public OutputStream asOutputStream() {
            return this;
        }

        @Override
        public void close() throws IOException {
            System.out.println("Closing " + closed);
            if (closed) return;
            closed = true;

            Result<JSBoolean> result = entry.setContents(contents == null ? "" : LuaString.decode(contents, 0, size));
            contents = null;
            result.getOrThrow();
        }

        @Override
        public int read(ByteBuffer buffer) throws IOException {
            throw new IOException("Cannot read from this buffer");
        }

        @Override
        public InputStream asInputStream() {
            throw new IllegalStateException("Cannot read from this buffer");
        }
    }
}
