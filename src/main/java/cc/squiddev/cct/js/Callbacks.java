package cc.squiddev.cct.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

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

    @JSBody(params = {"callback", "delay"}, script = "callbacks.setInterval(callback, delay);")
    public static native void setInterval(Callback callback, int delay);

    @JSBody(params = {"callback"}, script = "callbacks.setImmediate(callback);")
    public static native void setImmediate(Callback callback);
}
