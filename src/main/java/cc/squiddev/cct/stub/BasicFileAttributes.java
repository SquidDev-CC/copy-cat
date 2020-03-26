package cc.squiddev.cct.stub;

/**
 * @see java.nio.file.attribute.BasicFileAttributes
 */
public class BasicFileAttributes {
    private final long created;
    private final long modified;
    private final boolean isDirectory;
    private final long size;

    public BasicFileAttributes(long created, long modified, boolean isDirectory, long size) {
        this.created = created;
        this.modified = modified;
        this.isDirectory = isDirectory;
        this.size = size;
    }

    public BasicFileAttributes(boolean isDirectory, long size) {
        this.created = 0;
        this.modified = 0;
        this.isDirectory = isDirectory;
        this.size = size;
    }

    public long creationTime() {
        return created;
    }

    public long lastModifiedTime() {
        return modified;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long size() {
        return size;
    }
}
