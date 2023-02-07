package cc.tweaked.web.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSProperty;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.websocket.WebSocket;

public abstract class WebsocketClient extends WebSocket {
    public abstract void send(ArrayBuffer object);

    @JSProperty("onmessage")
    public abstract void setOnMessage(EventListener<WebsocketMessageEvent> handler);

    @JSBody(params = "url", script = "return new WebSocket(url);")
    public static native WebsocketClient create(String url);

    public abstract class WebsocketMessageEvent implements MessageEvent {
        @JSBody(script = "return this.data instanceof ArrayBuffer;")
        public native boolean isBinary();
    }
}
