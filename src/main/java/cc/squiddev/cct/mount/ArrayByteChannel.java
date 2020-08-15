
package cc.squiddev.cct.mount;

import cc.squiddev.cct.stub.SeekableByteChannel;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A seekable, readable byte channel which is backed by a simple byte array.
 */
public class ArrayByteChannel extends InputStream implements SeekableByteChannel {
    private boolean closed = false;
    private int position = 0;

    private final byte[] backing;

    public ArrayByteChannel(byte[] backing) {
        this.backing = backing;
    }

    @Override
    public int read(ByteBuffer destination) throws IOException {
        if (closed) throw new IOException("Closed");
        Objects.requireNonNull(destination, "destination");

        if (position >= backing.length) return -1;

        int remaining = Math.min(backing.length - position, destination.remaining());
        destination.put(backing, position, remaining);
        position += remaining;
        return remaining;
    }

    @Override
    public int read(@Nonnull byte[] destination, int start, int length) throws IOException {
        if (closed) throw new IOException("Closed");
        Objects.requireNonNull(destination, "destination");

        if (position >= backing.length) return -1;

        int remaining = Math.min(backing.length - position, length);
        System.arraycopy(backing, position, destination, start, remaining);
        position += remaining;
        return remaining;
    }

    @Override
    public InputStream asInputStream() {
        return this;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (closed) throw new IOException("Closed");
        throw new IOException("Not writable");
    }

    @Override
    public OutputStream asOutputStream() {
        return null;
    }

    @Override
    public long position() throws IOException {
        if (closed) throw new IOException("Closed");
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (closed) throw new IOException("Closed");
        if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        if (closed) throw new IOException("Closed");
        return backing.length;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("Closed");
        return position < backing.length ? backing[position++] & 0xff : -1;
    }
}
