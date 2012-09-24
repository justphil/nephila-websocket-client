Nephila
=======

Nephila is a pure **Java Socket** implementation of the latest **WebSocket** specification as defined in
[RFC 6455](http://tools.ietf.org/html/rfc6455 "RFC 6455 The WebSocket Protocol").
It was especially built with mobile devices in mind (Android). Therefore it causes a minimal memory footprint and does not use any NIO libraries like
[Grizzly](http://grizzly.java.net/ "Java NIO and Web framework") or [Netty](https://netty.io/ "an asynchronous event-driven network application framework"),
because it is hard to get one of these libraries up and running on Android.
Of course it is also perfectly okay to use Nephila in a context outside of Android, if you are looking for
a simple and lightweight plain old Socket implementation.


Features
--------

- Simple and clean API
- Zero dependency pure Java implementation
- ws:// and wss:// support
- Works on the Android platform
- Built using conventional Java Socket API instead of NIO (because there are NIO limitations on the Android platform)
- Implements the complete spec, including sending and streaming of text and binary frames, ping/pong frames, etc.


API
---

### WebSocket Interface aka 'How can I act?'

Nephila provides a simple API for sending and streaming text and binary data. Moreover it properly implements the sending
of ping/pong frames. Consequently it is very easy to develop application protocols including connection
maintenance mechanisms. The **WebSocket** interface is the abstraction layer for these functionalities.

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
    

### WebSocketListener Interface aka 'How can I react?'

It is probable that your application is interested in common events like incoming data or a server going down.
For this purpose you need to pass the DefaultWebSocket constructor a WebSocketListener implementation. In
particular you have to implement to following callback methods.

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


Usage
-----

This is a complete executable example.

    // create a WebSocket object, you have to provide a WebSocketListener implementation
    WebSocket ws = new DefaultWebSocket(new WebSocketListener() {
            ...
    });

    // establish a WebSocket connection
    ws.connect("ws://example.com/app");
    
    // send text data
    ws.send("Hello World!");
    
    // send binary data
    byte[] bytes = new byte[] {0xA, 0xB, 0xC};
    ws.send(bytes);
    
    // stream text data
    String helloWorld = "Hello World!";
    int i = 0;
    while (i++ < helloWorld.length()) {
        ws.stream(helloWorld.substring(i,1), i == helloWorld.length()-1);
        //stream char by char and end the stream with a final chunk when (i == helloWorld.length()-1) is true
    }
    
    // stream binary data
    byte[] bytes = new byte[] {0xA, 0xB, 0xC};
    int i = 0;
    while (i++ < bytes.length) {
        ws.stream(new byte[]{ bytes[i] }, i == bytes.length-1);
        //stream byte by byte and end the stream with a final chunk when (i == bytes.length-1) is true
    }
    
    // ping
    ws.ping();
    // ping with text data
    ws.ping("Are you there?");
    // ping with binary data
    ws.ping(new byte[] {0x1});
    
    // pong
    ws.pong();
    // pong with text data
    ws.pong("I'm still alive!");
    // pong with binary data
    ws.pong(new byte[] {0x2});
    
    // close a WebSocket connection
    ws.close();
    // close a WebSocket connection providing a reason
    ws.close("It's over!");