package cc.squiddev.cct.stub;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface ReadableByteChannel extends Channel {
    int read(ByteBuffer buffer) throws IOException;

    InputStream asInputStream();
}
