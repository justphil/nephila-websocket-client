package io.cuckoo.websocket.nephila;

public interface WebSocketConfig {
    boolean isDebug();
    String getDateTimeFormat();
    int getSocketTimeout();
    int getOutputBufferSize();
}
