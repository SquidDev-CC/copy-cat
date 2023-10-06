package cc.tweaked.copycat.js;

import cc.tweaked.web.js.ComputerDisplay;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Controls a specific computer on the Javascript side. See {@code computer/callbacks.ts}.
 */
public interface ExtendedComputerDisplay extends ComputerDisplay {
    /**
     * Find a file system entry with the given name.
     *
     * @param path The path to find
     * @return A file entry, or {@code null} if none could be found.
     */
    @Nullable
    FileSystemEntry getEntry(@Nonnull String path);

    /**
     * Create a new directory with the given path
     *
     * @param path The directory to create
     * @return A file entry if the directory exists or could be created, an empty one otherwise.
     */
    @Nonnull
    Result<FileSystemEntry> createDirectory(@Nonnull String path);

    /**
     * Create a new file with the given path. The owning folder must exist already.
     *
     * @param path The file to create
     * @return A file entry if the file exists or could be created, an empty one otherwise.
     */
    @Nonnull
    Result<FileSystemEntry> createFile(@Nonnull String path);

    /**
     * Recursively delete a file system entry.
     *
     * @param path The path to delete
     */
    void deleteEntry(@Nonnull String path);

    /**
     * A naive Either type, instead of wrangling JS/Java exceptions.
     */
    interface Result<T extends JSObject> extends JSObject {
        @Nullable
        @JSProperty
        T getValue();

        @Nullable
        @JSProperty
        String getError();

        @Nonnull
        default T getOrThrow() throws IOException {
            T result = getValue();
            if (result == null) throw new IOException(getError());
            return result;
        }
    }
}
