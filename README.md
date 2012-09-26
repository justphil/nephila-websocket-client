Nephila
=======

Nephila is a plain old **Java Socket** implementation of the latest **WebSocket** specification as defined in
[RFC 6455](http://tools.ietf.org/html/rfc6455 "RFC 6455 The WebSocket Protocol").
It was especially built with mobile devices in mind (Android). Therefore it causes a minimal memory footprint and does not use any NIO libraries like
[Grizzly](http://grizzly.java.net/ "Java NIO and Web framework") or [Netty](https://netty.io/ "an asynchronous event-driven network application framework"),
because it is hard to get one of these libraries up and running on Android.
Of course it is also perfectly okay to use Nephila in a context outside of Android, if you are looking for
a simple and lightweight plain old Socket implementation instead of a NIO implementation.


Features
--------

- Simple and clean API
- Zero dependency pure Java implementation
- ws:// and wss:// support
- Thread safety
- Works on the Android platform
- Built using plain old Java Socket API instead of NIO (because there are NIO limitations on the Android platform)
- Implements the complete spec, including sending and streaming of text and binary frames, ping/pong frames, etc.


API
---

### WebSocket Interface aka 'How can I act?'

Nephila provides a simple API for sending and streaming text and binary data. Moreover it properly implements the sending
of ping/pong frames. Consequently it is very easy to develop application protocols that include connection
maintenance mechanisms. The **WebSocket** interface is the abstraction layer for these functionalities.

    public interface WebSocket {
        // some basic getters/setters
        WebSocketListener getWebSocketListener();
        void setWebSocketListener(WebSocketListener webSocketListener);
        WebSocketConfig getWebSocketConfig();
        List<String> getNegotiatedSubProtocols();
    
        // establish a connection to a WebSocket server
        void connect(URI uri) throws WebSocketException;
        void connect(String uri) throws WebSocketException;
        boolean isConnected();
    
        // send text and binary data
        void send(String data) throws WebSocketException;
        void send(byte[] data) throws WebSocketException;
    
        // stream text and binary data
        void stream(String data, boolean isFinalChunk) throws WebSocketException;
        void stream(byte[] data, boolean isFinalChunk) throws WebSocketException;
    
        // send ping frame allowing you to implement connection maintenance mechanisms
        void ping() throws WebSocketException;
        void ping(String data) throws WebSocketException;
        void ping(byte[] data) throws WebSocketException;
    
        // send pong frame for heartbeats or to respond to server's ping
        void pong() throws WebSocketException;
        void pong(String data) throws WebSocketException;
        void pong(byte[] data) throws WebSocketException;
    
        // close the connection to the server
        void close() throws WebSocketException;
        void close(String reason) throws WebSocketException;
    }
    

### WebSocketListener Interface aka 'How can I react?'

It is probable that your application is interested in common events like incoming data or a server going down.
For this purpose you have to pass the DefaultWebSocket constructor a WebSocketListener implementation or use the
corresponding setter method to inject it. In particular it is mandatory to implement to following callback methods.

    public interface WebSocketListener {
        // do something when the connection has been established
        void onConnect();
        // do something when the connection has been closed
        void onClose();
    
        // react on incoming text / binary data
        void onMessage(String message);
        void onMessage(byte[] message);
    
        // react on streamed incoming text / binary data
        void onMessageChunk(String messageChunk, boolean isFinalChunk);
        void onMessageChunk(byte[] messageChunk, boolean isFinalChunk);
    
        // react on incoming ping frame
        void onPing();
        void onPing(byte[] data);
    
        // react on incoming pong frame
        void onPong();
        void onPong(byte[] data);
    }


Usage
-----

This is a complete usage example. Further usage examples can be looked up in the JUnit test cases located in the
*DefaultWebSocketTest* class.

    // create a WebSocket object providing a WebSocketListener implementation
    final WebSocket ws = new DefaultWebSocket();
    ws.setWebSocketListener(new WebSocketListener() {
        @Override
        public void onConnect() {
            System.out.println("connected to server");
        }

        @Override
        public void onClose() {
            System.out.println("disconnected from server");
        }

        @Override
        public void onMessage(String message) {
            System.out.println("server has sent some text: " + message);
        }

        @Override
        public void onMessage(byte[] message) {
            System.out.println("server has sent some bytes: " + Arrays.toString(message));
        }

        @Override
        public void onMessageChunk(String messageChunk, boolean isFinalChunk) {
            System.out.println("server has sent a text chunk: " + messageChunk + " # final chunk: " + isFinalChunk);
        }

        @Override
        public void onMessageChunk(byte[] messageChunk, boolean isFinalChunk) {
            System.out.println("server has sent a binary chunk: " + Arrays.toString(messageChunk) + " # final chunk: " + isFinalChunk);
        }

        @Override
        public void onPing() {
            try {
                ws.pong();
            } catch (WebSocketException ignored) {}
        }

        @Override
        public void onPing(byte[] data) {
            try {
                ws.pong(data);
            } catch (WebSocketException ignored) {}
        }

        @Override
        public void onPong() {
            System.out.println("server is still alive");
        }

        @Override
        public void onPong(byte[] data) {
            System.out.println("server is still alive, data: " + Arrays.toString(data));
        }
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