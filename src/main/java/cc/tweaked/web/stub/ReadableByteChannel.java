package cc.tweaked.web.stub;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @see java.nio.channels.ReadableByteChannel
 */
public interface ReadableByteChannel extends Channel {
    int read(ByteBuffer buffer) throws IOException;
}
