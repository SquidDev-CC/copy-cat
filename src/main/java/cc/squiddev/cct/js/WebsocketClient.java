package cc.squiddev.cct.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;

import javax.annotation.Nullable;

public abstract class WebsocketClient implements JSObject {
    public static final int CONNECTING = 0;

    public static final int OPEN = 1;

    public static final int CLOSING = 2;

    public static final int CLOSED = 3;

    public abstract void send(ArrayBuffer object);

    public abstract void send(String object);

    public abstract void close();

    public abstract void close(int code);

    public abstract void close(int code, String reason);

    @JSProperty
    public abstract void setBinaryType(String type);

    @JSProperty("onopen")
    public abstract void setOnOpen(EventListener<Event> handler);

    @JSProperty("onclose")
    public abstract void setOnClose(EventListener<CloseEvent> handler);

    @JSProperty("onerror")
    public abstract void setOnError(EventListener<Event> handler);

    @JSProperty("onmessage")
    public abstract void setOnMessage(EventListener<WebsocketMessageEvent> handler);

    @JSBody(params = "url", script = "return new WebSocket(url);")
    public static native WebsocketClient create(String url);

    public abstract class WebsocketMessageEvent implements MessageEvent {
        @JSBody(script = "return this.data instanceof ArrayBuffer;")
        public native boolean isBinary();
    }

    public abstract class CloseEvent implements Event {
        @JSProperty
        public abstract int getCode();

        @Nullable
        @JSProperty
        public abstract String getReason();
    }
}
