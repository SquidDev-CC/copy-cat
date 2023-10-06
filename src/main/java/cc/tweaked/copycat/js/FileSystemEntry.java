package cc.tweaked.copycat.js;

import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.typedarrays.Int8Array;

import javax.annotation.Nonnull;

/**
 * An entry in the file system
 */
public interface FileSystemEntry extends JSObject {
    /**
     * If this entry is a directory.
     */
    boolean isDirectory();

    /**
     * Get the filenames of all child entries
     *
     * @return The child entries. Note, this is the relative path, rather than the absolute one.
     * @throws RuntimeException If this is not a directory
     */
    @Nonnull
    String[] getChildren();

    /**
     * Get the contents of this filesystem entry
     *
     * @return This file's contents
     * @throws RuntimeException If this is not a file
     */
    @JSByRef
    @Nonnull
    byte[] getContents();

    /**
     * Set the contents of this filesystem entry
     *
     * @param contents This files's contents
     * @return Whether this file could be successfully written
     * @throws RuntimeException If this is not a file
     */
    @Nonnull
    ExtendedComputerDisplay.Result<JSBoolean> setContents(@Nonnull Int8Array contents);

    /**
     * Get the attrribut
     *
     * @return
     */
    @Nonnull
    JsFileAttributes getAttributes();
}
