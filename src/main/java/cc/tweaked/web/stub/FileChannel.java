package cc.tweaked.web.stub;

import java.io.IOException;

/**
 * This is never constructed, only used in {@code instanceof} checks.
 *
 * @see java.nio.channels.FileChannel
 */
public abstract class FileChannel implements SeekableByteChannel {
    private FileChannel() {
    }

    public void force(boolean metadata) throws IOException {

    }
}
