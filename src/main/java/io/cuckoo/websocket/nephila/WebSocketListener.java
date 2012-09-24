package io.cuckoo.websocket.nephila;

public interface WebSocketListener {
    void onConnect();
    void onClose();

    void onMessage(String message);
    void onMessage(byte[] message);

    void onMessageChunk(String messageChunk, boolean isFinalChunk);
    void onMessageChunk(byte[] messageChunk, boolean isFinalChunk);

    void onPing();
    void onPing(byte[] data);

    void onPong();
    void onPong(byte[] data);
}
