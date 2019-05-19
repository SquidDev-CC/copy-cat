package cc.squiddev.cct.js;

import cc.squiddev.cct.js.Callbacks.Callback;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Controls a specific computer on the Javascript side. See {@code computer/callbacks.ts}.
 */
public interface ComputerAccess extends JSObject {
    /**
     * Get the computer's label.
     *
     * @return The current label, or {@code null} if not set.
     */
    @Nullable
    String getLabel();

    /**
     * Set this computer's current state
     *
     * @param label This computer's label
     * @param on    If this computer is on right now
     */
    void setState(@Nullable String label, boolean on);

    /**
     * Update the terminal's properties
     *
     * @param width        The terminal width
     * @param height       The terminal height
     * @param x            The X cursor
     * @param y            The Y cursor
     * @param blink        Whether the cursor is blinking
     * @param cursorColour The cursor's colour
     */
    void updateTerminal(int width, int height, int x, int y, boolean blink, int cursorColour);

    /**
     * Set a line on the terminal
     *
     * @param line The line index to set
     * @param text The line's text
     * @param fore The line's foreground
     * @param back The line's background
     */
    void setTerminalLine(int line, @Nonnull String text, @Nonnull String fore, @Nonnull String back);

    /**
     * Set the palette colour for a specific index
     *
     * @param colour The colour index to set
     * @param r      The red value, between 0 and 1
     * @param g      The green value, between 0 and 1
     * @param b      The blue value, between 0 and 1
     */
    void setPaletteColour(int colour, double r, double g, double b);

    /**
     * Mark the terminal as having changed. Should be called after all other terminal methods.
     */
    void flushTerminal();

    /**
     * Set the callback used when an event is received.
     *
     * @param handler The event handler
     */
    void onEvent(@Nonnull QueueEventHandler handler);

    /**
     * Set the callback used when the computer must be shut down
     *
     * @param handler The event handler
     */
    void onShutdown(@Nonnull Callback handler);

    /**
     * Set the callback used when the computer must be turned on
     *
     * @param handler The event handler
     */
    void onTurnOn(@Nonnull Callback handler);

    /**
     * Set the callback used when the computer must be restarted
     *
     * @param handler The event handler
     */
    void onReboot(@Nonnull Callback handler);

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

    @JSFunctor
    @FunctionalInterface
    interface QueueEventHandler extends JSObject {
        void queueEvent(String event, String[] args);
    }

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
