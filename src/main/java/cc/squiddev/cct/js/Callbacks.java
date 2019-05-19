package cc.squiddev.cct.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Callbacks {
    @JSFunctor
    @FunctionalInterface
    public interface Callback extends JSObject {
        void run();
    }

    /**
     * Get the current callback instance
     *
     * @return The callback instance
     */
    @JSBody(script = "return callbacks.getComputer();")
    public static native ComputerAccess computer();

    /**
     * Get or create a config group
     *
     * @param name        The display name of this group
     * @param description A short description of this group
     * @return The constructed config group
     */
    @JSBody(params = {"name", "description"}, script = "return callbacks.config(name, description);")
    public static native ConfigGroup config(@Nonnull String name, @Nullable String description);

    @JSBody(params = {"callback", "delay"}, script = "callbacks.setInterval(callback, delay);")
    public static native void setInterval(Callback callback, int delay);

    @JSBody(params = {"callback"}, script = "callbacks.setImmediate(callback);")
    public static native void setImmediate(Callback callback);
}
