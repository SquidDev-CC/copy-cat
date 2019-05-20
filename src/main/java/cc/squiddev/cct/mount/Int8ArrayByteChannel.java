package cc.squiddev.cct.mount;

import cc.squiddev.cct.stub.SeekableByteChannel;
import org.teavm.jso.typedarrays.Int8Array;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * A seekable, readable byte channel which is backed by a simple byte array.
 */
public class Int8ArrayByteChannel extends InputStream implements SeekableByteChannel {
    private boolean closed = false;
    private int position = 0;

    private final Int8Array backing;

    public Int8ArrayByteChannel(Int8Array backing) {
        this.backing = backing;
    }

    @Override
    public int read(ByteBuffer destination) throws IOException {
        if (closed) throw new IOException("Closed");
        Objects.requireNonNull(destination, "destination");

        if (position >= backing.getLength()) return -1;

        int remaining = Math.min(backing.getLength() - position, destination.remaining());
        for (int i = 0; i < remaining; i++) destination.put(backing.get(position + i));
        position += remaining;
        return remaining;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (closed) throw new IOException("Closed");
        throw new IOException("Not writable");
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
        return backing.getLength();
    }

    @Override
    public int read(byte[] into, int off, int len) throws IOException {
        if (closed) throw new IOException("Closed");
        if (into == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > into.length - off) {
            throw new IndexOutOfBoundsException();
        }

        if (position >= backing.getLength()) return -1;

        int available = backing.getLength() - position;
        if (len > available) len = available;
        if (len <= 0) return 0;
        for (int i = 0; i < len; i++) into[i + off] = backing.get(position + i);
        position += len;
        return len;
    }

    @Override
    public int read() throws IOException {
        if (closed) throw new IOException("Closed");
        return position < backing.getLength() ? backing.get(position++) & 0xff : -1;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public InputStream asInputStream() {
        return this;
    }

    @Override
    public OutputStream asOutputStream() {
        throw new IllegalStateException("Not writable");
    }
}

