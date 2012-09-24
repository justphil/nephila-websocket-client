package io.cuckoo.websocket.nephila.helpers;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import java.util.Arrays;
import java.util.List;

public class SubProtocolWebSocketApplication extends WebSocketApplication {

    private final String[] supportedSubProtocols = new String[] {
            "chat.cuckoo.io", "bazzinga.cuckoo.io", "shootout.cuckoo.io"
    };

    @Override
    public boolean isApplicationRequest(HttpRequestPacket httpRequestPacket) {
        return "/subprotocoltest".equals(httpRequestPacket.getRequestURI());
    }

    @Override
    public List<String> getSupportedProtocols(List<String> subProtocol) {
        return Arrays.asList(supportedSubProtocols);
    }
}
