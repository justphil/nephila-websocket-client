Nephila
=======

Nephila is a plain old **Java Socket** client implementation of the latest **WebSocket** specification as defined in
[RFC 6455](http://tools.ietf.org/html/rfc6455 "RFC 6455 The WebSocket Protocol").
It was especially built with mobile devices in mind (Android) and therefore causes a minimal memory footprint. Nephila does not use any NIO libraries like
[Grizzly](http://grizzly.java.net/ "Java NIO and Web framework") or [Netty](https://netty.io/ "an asynchronous event-driven network application framework") (although they are pretty neat!),
because one the hand it is hard to get these libraries up and running on Android and on the other hand their memory
consumption is significantly higher than conventional IO's memory consumption.
Of course it is also perfectly okay to use Nephila in a context outside of Android, if you are looking for
a lightweight plain old Socket implementation instead of a NIO implementation.


Features
--------

- Simple and clean API
- Zero dependency
- ws:// and wss:// support
- Thread safe
- Works on the Android platform
- Built using plain old Java Socket API instead of NIO (because there are NIO limitations on the Android platform)
- Implements the complete spec, including sending and streaming of text and binary frames, ping/pong frames, etc.
- Supports sub protocol negotiation


Purpose
-------

The WebSocket protocol is an application layer network protocol (developed by the IETF) above the TCP/IP stack
and was primarily developed in order to allow Web pages to establish an asynchronous two-way communication channel to a Web server. To emulate
a similar behaviour many Web pages still use diverse workaround techniques like long polling, HTTP streaming or
Flash sockets.

In order to enable Web developers to make use of the WebSocket protocol the W3C has developed an official
WebSocket JavaScript API. Although this API only pays attention to a subset of the WebSocket protocol it
enables Web developers to easily build novel "real-time" (as defined in the context of the Web) web applications running in the browser.

Besides a WebSocket enabled browser another requirement is that the application's backend also need to speak the WebSocket
protocol. Consequently, novel applications need to build application protocols on top of the WebSocket protocol in order to
realize their specified use cases.

So the question that arises is: How can native applications (e.g. Android or iOS) be integrated into this context?
Although the most popular mobile platforms provide browsers with WebSocket support, there are many use cases that
require the development of a native application instead of a mobile Web app. The causes are mainly: bad performance,
unsatisfactory user experience or the browser not allowing hardware access to e.g. the accelerometer or compass.
So, even if you need to develop a native application you probably would like to access your WebSocket enabled backend
in the same way your Web page (created for desktop usage) does, because it is time-consuming and complex to develop multiple backend
interfaces for different terminals.

To cut a long story short, Nephila can help you to build cross-platform applications that access a WebSocket enabled
backend interface in a unified way. While building your Web app on top of the WebSocket JavaScript API, you can use Nephila to
build a native Android application accessing the same backend API.

A common use case for this kind of application are games. If you are about to develop a stunning new multiplayer online game that
should run on multiple platforms (desktop browser, Android, iOS, etc.) and want to enable your players to play against each other
although running the game on a different platform, a proven (but time-consuming and complex) method is to develop a backend with multiple interfaces
for the different platforms. E.g. you could develop a long polling interface for the desktop browser access and a
TCP based protocol for the Android and iOS app. Then you have to find a way to handle both interfaces in a similar way
to allow the communication between desktop browser players and Android/iOS players: an extremely complex task.

A much easier and less time-consuming design is to build a unified WebSocket enabled backend interface and access
it from different platforms in the same way. Nephila is your way to go for the Android platform!



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

This is a complete usage example. Further usage examples can be found in the JUnit test cases located in the
*DefaultWebSocketTest* class.

    // create a WebSocket object and provide a WebSocketListener implementation
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
    
    
Architecture
------------

<p align="center">
  <img src="http://cuckoo.io/github/nephila_arch.png" /><br>
  <b>Fig. 1:</b> Nephila's Internal Components
</p>

Nephila's core class is *DefaultWebSocket* class which implements the *WebSocket* interface. As can be seen in fig. 1, it
holds a reference to two further objects, one implementing the *WebSocketListener* interface and the other of
type *WebSocketReceiver*. Under the hood Nephila uses a plain old Java Socket and stream-based IO (InputStream and OutputStream) in order to
dispatch all network level operations.

The *DefaultWebSocket* is responsible for establishing the underlying TCP connection, initiating the opening handshake,
processing the server's opening handshake and dispatching all outgoing operations (exposed to your application through the *WebSocket* interface).
It is important to mention that all operations that are exposed through the *WebSocket* interface are execute in your
application's **main thread**.

The *WebSocketReceiver* also holds a reference to your implementation of the *WebSocketListener* interface
and is responsible for the dispatching of the incoming network traffic. For this purpose it spawns a separate thread.
Consequently all callback methods (except onConnect()) that you provide by implementing the *WebSocketListener* interface are
invoked in the separate **WebSocketReceiver thread**.
This is particularly important if you use Nephila in an Android application, because Android does not allow applications
to modify UI components within other threads except the main application thread. So in case you want to populate
an incoming WebSocket event to your UI (and this is what you most likely want to do!), you have to trigger one of Android's
built-in mechanisms (e.g. [Handlers](http://developer.android.com/reference/android/os/Handler.html)) in order to dispatch
the UI modification in your application's main thread and not the separate **WebSocketReceiver thread**.


Performance / Memory Footprint
------------------------------

TODO

License
-------

Nephila is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0 "Apache License, Version 2.0").