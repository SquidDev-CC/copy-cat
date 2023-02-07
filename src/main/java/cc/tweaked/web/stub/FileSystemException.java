package cc.tweaked.web.stub;

import java.io.IOException;

/**
 * @see java.nio.file.FileSystemException
 */
public class FileSystemException extends IOException {
    public String getReason() {
        return getMessage();
    }
}
