package io.cuckoo.websocket.nephila;

import java.net.URI;
import java.util.List;

public interface WebSocket {
    WebSocketListener getWebSocketListener();
    WebSocketConfig getWebSocketConfig();
    List<String> getNegotiatedSubProtocols();

    void connect(URI uri) throws WebSocketException;
    void connect(String uri) throws WebSocketException;
    boolean isConnected();

    void send(String data) throws WebSocketException;
    void send(byte[] data) throws WebSocketException;

    void stream(String data, boolean isFinalChunk) throws WebSocketException;
    void stream(byte[] data, boolean isFinalChunk) throws WebSocketException;

    void ping() throws WebSocketException;
    void ping(String data) throws WebSocketException;
    void ping(byte[] data) throws WebSocketException;

    void pong() throws WebSocketException;
    void pong(String data) throws WebSocketException;
    void pong(byte[] data) throws WebSocketException;

    void close() throws WebSocketException;
    void close(String data) throws WebSocketException;
}
