package io.cuckoo.websocket.nephila.impl;

import io.cuckoo.websocket.nephila.WebSocketConfig;

public class DefaultWebSocketConfig implements WebSocketConfig {

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    public static final boolean DEBUG           = true;

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static final int SOCKET_TIMEOUT      = 10000;

    public static final int OUTPUT_BUFFER_SIZE  = 8192;

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Override
    public boolean isDebug() {
        return DEBUG;
    }

    @Override
    public String getDateTimeFormat() {
        return DATE_TIME_FORMAT;
    }

    @Override
    public int getSocketTimeout() {
        return SOCKET_TIMEOUT;
    }

    @Override
    public int getOutputBufferSize() {
        return OUTPUT_BUFFER_SIZE;
    }
}
