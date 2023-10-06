package cc.tweaked.copycat.js;

import cc.tweaked.web.js.JavascriptConv;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * Utility methods for converting between Java and Javascript representations.
 */
public class MoreJavascriptConv extends JavascriptConv {
    /**
     * Unwrap {@code byte[]} to its underlying {@link Int8Array}.
     *
     * @param contents The array to unwrap.
     * @return The underlying contents.
     */
    @JSBody(params = "x", script = "return x;")
    public static native Int8Array ofByteArray(@JSByRef byte[] contents);
}
