package cc.squiddev.cct.stub;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ChannelHelpers {
    @CanIgnoreReturnValue
    public static void copy(ReadableByteChannel from, WritableByteChannel to) throws IOException {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        long oldPosition;

        ByteBuffer buf = ByteBuffer.allocate(8192);
        oldPosition = 0L;

        while (from.read(buf) != -1) {
            buf.flip();

            while (buf.hasRemaining()) {
                oldPosition += (long) to.write(buf);
            }

            buf.clear();
        }
    }
}
