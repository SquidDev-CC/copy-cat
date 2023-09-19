package cc.tweaked.web.stub;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * @see com.google.common.io.ByteStreams
 */
public class ByteStreams {
    public static void copy(ReadableByteChannel from, WritableByteChannel to) throws IOException {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);

        ByteBuffer buf = ByteBuffer.allocate(8192);
        while (from.read(buf) != -1) {
            buf.flip();
            while (buf.hasRemaining()) to.write(buf);
            buf.clear();
        }
    }
}
