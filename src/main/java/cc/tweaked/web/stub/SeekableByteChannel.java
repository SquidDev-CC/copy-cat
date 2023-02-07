package cc.tweaked.web.stub;

import com.google.errorprone.annotations.DoNotCall;

import java.io.IOException;

/**
 * @see java.nio.channels.SeekableByteChannel
 */
public interface SeekableByteChannel extends WritableByteChannel, ReadableByteChannel {
    long position() throws IOException;

    long size() throws IOException;

    SeekableByteChannel position(long newPosition) throws IOException;

    @DoNotCall
    default SeekableByteChannel truncate(long size) throws IOException {
        throw new NonWritableChannelException();
    }
}
