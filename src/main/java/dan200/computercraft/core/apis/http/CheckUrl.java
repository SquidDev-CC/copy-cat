package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;

import java.net.URI;

public class CheckUrl extends Resource<CheckUrl> {
    private static final String EVENT = "http_check";

    private final IAPIEnvironment environment;
    private final String address;

    public CheckUrl(ResourceGroup<CheckUrl> limiter, IAPIEnvironment environment, String address, URI uri) {
        super(limiter);
        this.environment = environment;
        this.address = address;
    }

    public void run() {
        if (isClosed()) return;
        environment.queueEvent(EVENT, address, true);
    }
}
