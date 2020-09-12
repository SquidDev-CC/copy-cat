package cc.squiddev.cct.js;

import org.teavm.jso.JSObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ComputerCallbacks extends JSObject {
    /**
     * Set the computer's label.
     *
     * @param label The computer's label.
     */
    void setLabel(@Nullable String label);

    /**
     * Queue an event on the computer.
     */
    void event(@Nonnull String event, String[] args);

    /**
     * Shut the computer down.
     */
    void shutdown();

    /**
     * Turn the computer on.
     */
    void turnOn();

    /**
     * Reboot the computer.
     */
    void reboot();

    /**
     * Dispose of this computer, marking it as no longer running.
     */
    void dispose();

    /**
     * Set the width and height of the computer. If not given, this will be synced with the
     *
     * @param width  The computer's width
     * @param height The computer's height.
     */
    void resize(int width, int height);
}
