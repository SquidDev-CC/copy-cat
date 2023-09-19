package cc.tweaked.web.stub;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

/**
 * @see java.nio.channels.Channels
 */
public class Channels {
    public static Reader newReader(ReadableByteChannel readable, CharsetDecoder dec, int minBufferCap) {
        return new InputStreamReader(new InputStream() {
            private final ByteBuffer oneByte = ByteBuffer.allocateDirect(1);

            @Override
            public int read() throws IOException {
                oneByte.position(0);
                if (readable.read(oneByte) <= 0) return -1;
                return oneByte.get(0);
            }

            @Override
            public int read(byte[] buffer, int off, int len) throws IOException {
                return readable.read(ByteBuffer.wrap(buffer, off, len));
            }
        }, dec);
    }

    public static Writer newWriter(WritableByteChannel writer, CharsetEncoder dec, int minBufferCap) {
        return new OutputStreamWriter(new OutputStream() {
            private final ByteBuffer oneByte = ByteBuffer.allocateDirect(1);

            @Override
            public void write(int b) throws IOException {
                oneByte.position(0).put(0, (byte) b).limit(1);
                writer.write(oneByte);
            }

            @Override
            public void write(byte[] buffer, int off, int len) throws IOException {
                writer.write(ByteBuffer.wrap(buffer, off, len));
            }
        }, dec);
    }
}
