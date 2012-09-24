Nephila
=======

Nephila is a pure **Java** implementation of the latest **WebSocket** specification as defined in
[RFC 6455](http://tools.ietf.org/html/rfc6455 "RFC 6455 The WebSocket Protocol").
It was especially built with mobile devices in mind (Android). Therefore it tries
to cause a minimal memory footprint and does not use any NIO libraries like
[Grizzly](http://grizzly.java.net/ "Java NIO and Web framework") or [Netty](https://netty.io/ "an asynchronous event-driven network application framework").


Features
--------

- Simple and clean API
- Zero dependency pure Java implementation
- ws:// and wss:// support
- Works on the Android platform
- Built using conventional Java Socket API instead of NIO (because there are NIO limitations on the Android platform)
- Tries to implement the complete spec, including sending and streaming of text and binary frames, ping/pong frames, etc.


Usage and API
-------------

### WebSocket Interface

    // create a WebSocket object
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