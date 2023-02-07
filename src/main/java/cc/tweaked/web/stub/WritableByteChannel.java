package cc.tweaked.web.stub;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @see java.nio.channels.WritableByteChannel
 */
public interface WritableByteChannel extends Channel {
    int write(ByteBuffer buffer) throws IOException;
}
