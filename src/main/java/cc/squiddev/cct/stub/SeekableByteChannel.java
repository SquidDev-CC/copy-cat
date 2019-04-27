package cc.squiddev.cct.stub;


import java.io.IOException;

public interface SeekableByteChannel extends WritableByteChannel, ReadableByteChannel {
    long position() throws IOException;

    long size() throws IOException;

    SeekableByteChannel position(long newPosition) throws IOException;
}
