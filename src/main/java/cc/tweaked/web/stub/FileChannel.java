package cc.tweaked.web.stub;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * This is never constructed, only used in {@code instanceof} checks.
 *
 * @see java.nio.channels.FileChannel
 */
public abstract class FileChannel implements SeekableByteChannel {
    private FileChannel() {
    }

    public abstract FileChannel position(long newPosition) throws IOException;

    public abstract void force(boolean metadata) throws IOException;

    public abstract long transferTo(long position, long count, WritableByteChannel target) throws IOException;
}
