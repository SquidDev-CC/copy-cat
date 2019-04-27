package cc.squiddev.cct.stub;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public interface WritableByteChannel extends Channel {
    int write(ByteBuffer buffer) throws IOException;

    OutputStream asOutputStream();
}
