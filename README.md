Nephila
=======

Nephila is a pure **Java** implementation of the latest **WebSocket** specification as defined in
[RFC 6455](http://tools.ietf.org/html/rfc6455 "RFC 6455 The WebSocket Protocol").
It was especially built with mobile devices in mind (Android). Therefore it tries
to cause a minimal memory footprint and does not use any NIO libraries like
[Grizzly](http://grizzly.java.net/ "Java NIO and Web framework") or [Netty](https://netty.io/ "an asynchronous event-driven network application framework").

Features
========

- Simple and clean API
- Zero dependency pure Java implementation
- Works on the Android platform
- Built using conventional Java Socket API instead of NIO (because there are NIO limitations on the Android platform)
- Tries to implement complete spec, including sending & streaming of text and binary frames, ping, pong, etc.