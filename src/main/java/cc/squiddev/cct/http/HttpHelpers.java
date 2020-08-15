package cc.squiddev.cct.http;

import cc.squiddev.cct.Main;
import cc.squiddev.cct.mount.Int8ArrayByteChannel;
import cc.squiddev.cct.mount.ArrayByteChannel;
import cc.squiddev.cct.stub.SeekableByteChannel;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.EncodedReadableHandle;
import dan200.computercraft.core.apis.handles.HandleGeneric;
import dan200.computercraft.core.apis.http.request.HttpRequest;
import dan200.computercraft.core.apis.http.request.HttpResponseHandle;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HttpHelpers {
    public static void makeRequest(HttpRequest self, URI uri, String method, Map<String, String> headers, String postBuffer) {
        XMLHttpRequest request = XMLHttpRequest.create();
        request.setOnReadyStateChange(() -> {
            if (request.getReadyState() != XMLHttpRequest.DONE) return;
            if (request.getStatus() == 0) {
                self.failure("Could not connect");
                return;
            }
            HandleGeneric reader;
            if (self.isBinary()) {
                ArrayBuffer buffer = request.getResponse().cast();
                SeekableByteChannel contents = new Int8ArrayByteChannel(Int8Array.create(buffer));
                reader = BinaryReadableHandle.of(contents);
            } else {
                SeekableByteChannel contents = new ArrayByteChannel(encode(request.getResponseText()));
                reader = new EncodedReadableHandle(EncodedReadableHandle.openUtf8(contents));
            }

            Map<String, String> responseHeaders = new HashMap<>();
            for (String header : request.getAllResponseHeaders().split("\r\n")) {
                int index = header.indexOf(':');
                if (index < 0) continue;

                // Normalise the header (so "content-type" becomes "Content-Type")
                boolean upcase = true;
                StringBuilder headerBuilder = new StringBuilder(index);
                for (int i = 0; i < index; i++) {
                    char c = header.charAt(i);
                    headerBuilder.append(upcase ? Character.toUpperCase(c) : c);
                    upcase = c == '-';
                }
                responseHeaders.put(headerBuilder.toString(), header.substring(index + 1).trim());
            }
            HttpResponseHandle stream = new HttpResponseHandle(reader, request.getStatus(), request.getStatusText(), responseHeaders);
            if (request.getStatus() >= 200 && request.getStatus() < 400) {
                self.success(stream);
            } else {
                self.failure(request.getStatusText(), stream);
            }
        });
        request.setResponseType(self.isBinary() ? "arraybuffer" : "text");
        String address = uri.toASCIIString();
        request.open(method, Main.corsProxy.isEmpty() ? address : Main.corsProxy.replace("{}", address));
        for (Map.Entry<String, String> header : headers.entrySet()) {
            request.setRequestHeader(header.getKey(), header.getValue());
        }
        request.send(postBuffer);
    }

    public static byte[] encode(String string) {
        byte[] chars = new byte[string.length()];
        for (int i = 0; i < chars.length; i++) {
            char c = string.charAt(i);
            chars[i] = c < 256 ? (byte) c : 63;
        }
        return chars;
    }
}
