package cc.tweaked.web.mount;

import org.teavm.jso.typedarrays.Int8Array;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Objects;

/**
 * A seekable, readable byte channel which is backed by a simple byte array.
 */
public class Int8ArrayByteChannel implements SeekableByteChannel {
    private boolean closed = false;
    private int position = 0;

    private final Int8Array backing;

    public Int8ArrayByteChannel(Int8Array backing) {
        this.backing = backing;
    }

    @Override
    public int read(ByteBuffer destination) throws IOException {
        Objects.requireNonNull(destination, "destination");
        if (closed) throw new ClosedChannelException();

        if (position >= backing.getLength()) return -1;

        int remaining = Math.min(backing.getLength() - position, destination.remaining());
        for (int i = 0; i < remaining; i++) destination.put(backing.get(position + i));
        position += remaining;
        return remaining;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (closed) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public long position() throws IOException {
        if (closed) throw new ClosedChannelException();
        return position;
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        if (closed) throw new ClosedChannelException();
        if (newPosition < 0 || newPosition > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Position out of bounds");
        }
        position = (int) newPosition;
        return this;
    }

    @Override
    public long size() throws IOException {
        if (closed) throw new ClosedChannelException();
        return backing.getLength();
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        if (closed) throw new ClosedChannelException();
        throw new NonWritableChannelException();
    }

    @Override
    public boolean isOpen() {
        return !closed;
    }
}

