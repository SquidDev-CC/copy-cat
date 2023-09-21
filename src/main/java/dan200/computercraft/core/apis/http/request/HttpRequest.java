package dan200.computercraft.core.apis.http.request;

import cc.tweaked.web.Main;
import cc.tweaked.web.http.HttpHelpers;
import cc.tweaked.web.mount.Int8ArrayByteChannel;
import dan200.computercraft.core.Logging;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.BinaryReadableHandle;
import dan200.computercraft.core.apis.handles.EncodedReadableHandle;
import dan200.computercraft.core.apis.handles.HandleGeneric;
import dan200.computercraft.core.apis.http.HTTPRequestException;
import dan200.computercraft.core.apis.http.Resource;
import dan200.computercraft.core.apis.http.ResourceGroup;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class HttpRequest extends Resource<HttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpRequest.class);
    private static final String SUCCESS_EVENT = "http_success";
    private static final String FAILURE_EVENT = "http_failure";

    private final IAPIEnvironment environment;

    private final String address;
    private final @Nullable String postBuffer;
    private final HttpHeaders headers;
    private final boolean binary;

    public HttpRequest(
        ResourceGroup<HttpRequest> limiter, IAPIEnvironment environment, String address, @Nullable String postText,
        HttpHeaders headers, boolean binary, boolean followRedirects, int timeout
    ) {
        super(limiter);
        this.environment = environment;
        this.address = address;
        postBuffer = postText;
        this.headers = headers;
        this.binary = binary;

        if (postText != null) {
            if (!headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
                headers.set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
            }
        }
    }

    public static URI checkUri(String address) throws HTTPRequestException {
        URI url;
        try {
            url = new URI(address);
        } catch (URISyntaxException e) {
            throw new HTTPRequestException("URL malformed");
        }

        checkUri(url);
        return url;
    }

    public static void checkUri(URI url) throws HTTPRequestException {
        // Validate the URL
        if (url.getScheme() == null) throw new HTTPRequestException("Must specify http or https");
        if (url.getHost() == null) throw new HTTPRequestException("URL malformed");

        var scheme = url.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            throw new HTTPRequestException("Invalid protocol '" + scheme + "'");
        }
    }

    public void request(URI uri, HttpMethod method) {
        if (isClosed()) return;

        try {
            XMLHttpRequest request = XMLHttpRequest.create();
            request.setOnReadyStateChange(() -> onResponseStateChange(request));
            request.setResponseType(binary ? "arraybuffer" : "text");
            String address = uri.toASCIIString();
            request.open(method.toString(), Main.corsProxy.isEmpty() ? address : Main.corsProxy.replace("{}", address));
            for (Iterator<Map.Entry<String, String>> iterator = headers.iteratorAsString(); iterator.hasNext(); ) {
                var header = iterator.next();
                request.setRequestHeader(header.getKey(), header.getValue());
            }
            request.send(postBuffer);
            checkClosed();
        } catch (Exception e) {
            failure("Could not connect");
            LOG.error(Logging.HTTP_ERROR, "Error in HTTP request", e);
        }
    }

    private void onResponseStateChange(XMLHttpRequest request) {
        if (request.getReadyState() != XMLHttpRequest.DONE) return;
        if (request.getStatus() == 0) {
            this.failure("Could not connect");
            return;
        }

        HandleGeneric reader;
        if (binary) {
            ArrayBuffer buffer = request.getResponse().cast();
            SeekableByteChannel contents = new Int8ArrayByteChannel(Int8Array.create(buffer));
            reader = BinaryReadableHandle.of(contents);
        } else {
            SeekableByteChannel contents = new ArrayByteChannel(HttpHelpers.encode(request.getResponseText()));
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
            if (tryClose()) environment.queueEvent(SUCCESS_EVENT, address, stream);
        } else {
            if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, request.getStatusText(), stream);
        }
    }

    void failure(String message) {
        if (tryClose()) environment.queueEvent(FAILURE_EVENT, address, message);
    }
}
