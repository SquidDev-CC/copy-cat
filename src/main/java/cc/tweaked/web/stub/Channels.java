package cc.tweaked.web.stub;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Objects;

/**
 * @see java.nio.channels.Channels
 */
public class Channels {
    public static InputStream newInputStream(ReadableByteChannel channel) {
        Objects.requireNonNull(channel, "channel");
        return new ChannelInputStream(channel);
    }

    public static OutputStream newOutputStream(WritableByteChannel channel) {
        Objects.requireNonNull(channel, "channel");
        return new ChannelOutputStream(channel);
    }

    public static Reader newReader(ReadableByteChannel channel, CharsetDecoder dec, int minBufferCap) {
        return new InputStreamReader(newInputStream(channel), dec);
    }

    public static Writer newWriter(WritableByteChannel channel, CharsetEncoder dec, int minBufferCap) {
        return new OutputStreamWriter(newOutputStream(channel), dec);
    }

    private static class ChannelInputStream extends InputStream {
        private final ReadableByteChannel channel;
        private final ByteBuffer oneByte = ByteBuffer.allocateDirect(1);

        private ChannelInputStream(ReadableByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public int read() throws IOException {
            oneByte.position(0);
            if (channel.read(oneByte) <= 0) return -1;
            return oneByte.get(0) & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int off, int len) throws IOException {
            return channel.read(ByteBuffer.wrap(buffer, off, len));
        }
    }

    private static class ChannelOutputStream extends OutputStream {
        private final ByteBuffer oneByte;
        private final WritableByteChannel channel;

        public ChannelOutputStream(WritableByteChannel channel) {
            this.channel = channel;
            oneByte = ByteBuffer.allocateDirect(1);
        }

        @Override
        public void write(int b) throws IOException {
            oneByte.position(0).put(0, (byte) b).limit(1);
            channel.write(oneByte);
        }

        @Override
        public void write(byte[] buffer, int off, int len) throws IOException {
            channel.write(ByteBuffer.wrap(buffer, off, len));
        }
    }
}
