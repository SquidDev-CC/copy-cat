package cc.tweaked.copycat.js;

import cc.tweaked.web.js.ComputerHandle;

import javax.annotation.Nullable;

public interface ExtendedComputerHandle extends ComputerHandle {
    /**
     * Set the computer's label.
     *
     * @param label The computer's label.
     */
    void setLabel(@Nullable String label);

    /**
     * Set the width and height of the computer. If not given, this will be synced with the
     *
     * @param width  The computer's width
     * @param height The computer's height.
     */
    void resize(int width, int height);
}
