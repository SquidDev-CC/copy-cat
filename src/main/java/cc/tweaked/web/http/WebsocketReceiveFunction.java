package cc.tweaked.web.http;

import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.core.apis.http.websocket.WebsocketHandle;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.debug.DebugFrame;
import org.squiddev.cobalt.function.ResumableVarArgFunction;

/**
 * Roll our own {@code websocket.receive} function using Cobalt's yielding system, rather than CC's.
 */
public class WebsocketReceiveFunction extends ResumableVarArgFunction<Void> {
    private static final LuaString CLOSE_EVENT = ValueFactory.valueOf("websocket_closed");
    private static final LuaString MESSAGE_EVENT = ValueFactory.valueOf("websocket_message");
    private static final LuaString TERMINATE_EVENT = ValueFactory.valueOf("terminate");

    private final WebsocketHandle handle;
    private final Websocket websocket;

    public WebsocketReceiveFunction(WebsocketHandle handle, Websocket websocket) {
        this.handle = handle;
        this.websocket = websocket;
    }

    @Override
    protected Varargs invoke(LuaState state, DebugFrame di, Varargs args) throws LuaError, UnwindThrowable {
        if (handle.closed) throw new LuaError("attempt to use a closed file");
        return handle(state, LuaThread.yield(state, Constants.NONE));
    }

    @Override
    protected Varargs resumeThis(LuaState state, Void object, Varargs value) throws LuaError, UnwindThrowable {
        return handle(state, value);
    }

    private Varargs handle(LuaState state, Varargs args) throws LuaError, UnwindThrowable {
        while (true) {
            LuaValue event = args.first();
            LuaValue arg = args.arg(2);

            if (event.equals(MESSAGE_EVENT) && arg.isString() && arg.toString().equals(websocket.address())) {
                return args.subargs(3);
            } else if (event.equals(CLOSE_EVENT) && arg.isString() && arg.toString().equals(websocket.address()) && handle.closed) {
                return null;
            } else if (event.equals(TERMINATE_EVENT)) {
                throw new LuaError("Terminated", 0);
            }

            args = LuaThread.yield(state, Constants.NONE);
        }
    }
}
