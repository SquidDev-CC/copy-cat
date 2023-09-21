package cc.tweaked.web.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public final class Console {
    private Console() {
    }

    @JSBody(params = "x", script = "console.log(x);")
    public static native void log(String message);

    @JSBody(params = "x", script = "console.warn(x);")
    public static native void warn(String message);

    @JSBody(params = "x", script = "console.error(x);")
    public static native void error(String message);

    @JSBody(params = "x", script = "console.error(x);")
    public static native void error(JSObject object);
}
