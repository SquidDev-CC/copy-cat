package cc.squiddev.cct.js;

import cc.squiddev.cct.js.Callbacks.Callback;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Controls a specific computer on the Javascript side. See {@code computer/callbacks.ts}.
 */
public interface ComputerAccess extends JSObject {
    /**
     * Set this computer's current state
     *
     * @param label This computer's label
     * @param on    If this computer is on right now
     */
    void setState(String label, boolean on);

    /**
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
    void setTerminalLine(int line, String text, String fore, String back);

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
    void onEvent(QueueEventHandler handler);

    /**
     * Set the callback used when the computer must be shut down
     *
     * @param handler The event handler
     */
    void onShutdown(Callback handler);

    /**
     * Set the callback used when the computer must be turned on
     *
     * @param handler The event handler
     */
    void onTurnOn(Callback handler);

    /**
     * Set the callback used when the computer must be restarted
     *
     * @param handler The event handler
     */
    void onReboot(Callback handler);

    @JSFunctor
    @FunctionalInterface
    interface QueueEventHandler extends JSObject {
        void queueEvent(String event, String[] args);
    }
}
