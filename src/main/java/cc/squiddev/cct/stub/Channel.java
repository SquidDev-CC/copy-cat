package cc.squiddev.cct.stub;

import java.io.Closeable;

/**
 * So TeaVM doesn't support most of NIO, so we stub the various channel interfaces instead.
 *
 * @see ReadableByteChannel
 * @see java.nio.channels.ReadableByteChannel
 * @see WritableByteChannel
 * @see java.nio.channels.WritableByteChannel
 * @see SeekableByteChannel
 * @see java.nio.channels.SeekableByteChannel
 */
public interface Channel extends Closeable {
}
