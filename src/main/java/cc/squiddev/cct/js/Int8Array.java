package cc.squiddev.cct.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;

public abstract class Int8Array extends org.teavm.jso.typedarrays.Int8Array {
    public abstract void set(ArrayBufferView view);

    public abstract void set(ArrayBufferView view, int index);

    @JSProperty
    public abstract int getByteOffset();

    @JSBody(params = "length", script = "return new Int8Array(length);")
    public static native Int8Array create(int length);

    @JSBody(params = "buffer", script = "return new Int8Array(buffer);")
    public static native Int8Array create(ArrayBuffer buffer);

    @JSBody(params = {"buffer", "offset", "length"}, script = "return new Int8Array(buffer, offset, length);")
    public static native Int8Array create(ArrayBuffer buffer, int offset, int length);
}
