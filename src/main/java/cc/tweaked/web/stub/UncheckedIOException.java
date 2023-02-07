package cc.tweaked.web.stub;

import java.io.IOException;

/**
 * @see java.io.UncheckedIOException
 */
public class UncheckedIOException extends RuntimeException {
    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
