package cc.tweaked.web.js;

import org.jetbrains.annotations.Contract;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSBoolean;
import org.teavm.jso.core.JSNumber;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.core.JSString;

import javax.annotation.Nullable;

public class JavascriptConv {
    @JSBody(params = "data", script = "return data instanceof ArrayBuffer;")
    public static native boolean isArrayBuffer(JSObject object);

    @Contract("null -> null; !null -> !null")
    public static @Nullable Object[] toJava(@Nullable JSObject[] value) {
        if (value == null) return null;
        var out = new Object[value.length];
        for (var i = 0; i < value.length; i++) out[i] = toJava(value[i]);
        return out;
    }

    public static @Nullable Object toJava(@Nullable JSObject value) {
        if (value == null) return null;
        return switch (JSObjects.typeOf(value)) {
            case "string" -> ((JSString) value).stringValue();
            case "number" -> ((JSNumber) value).doubleValue();
            case "boolean" -> ((JSBoolean) value).booleanValue();
            default -> null;
        };
    }
}
