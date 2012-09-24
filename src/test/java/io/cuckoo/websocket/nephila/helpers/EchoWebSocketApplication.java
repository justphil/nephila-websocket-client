package io.cuckoo.websocket.nephila.helpers;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.*;

import java.util.Vector;


public class EchoWebSocketApplication extends WebSocketApplication {


    @Override
    public boolean isApplicationRequest(HttpRequestPacket httpRequestPacket) {
        return "/echo".equals(httpRequestPacket.getRequestURI());
    }

    @Override
    public WebSocket createSocket(ProtocolHandler handler, HttpRequestPacket requestPacket, WebSocketListener... listeners) {
        return super.createSocket(handler, requestPacket, listeners);
    }

    @Override
    public void onConnect(WebSocket socket) {
        // ignore
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        /*
        try {
            byte[] completePayload = frame.getBytes();
            System.out.println("first byte: " + completePayload[0]);
            System.out.println("second byte: " + completePayload[1]);

            int code = ByteBuffer.wrap(new byte[] {0, 0, completePayload[0], completePayload[1]}).getInt();

            int code2 = (0 | completePayload[0]) << 8;
            code2 = code2 | (completePayload[1] & 0xFF);


            int code3 = (3 << 8) | (-23 & 0xFF);


            byte[] utf8Data = new byte[completePayload.length-2];
            System.arraycopy(completePayload, 2, utf8Data, 0, utf8Data.length);
            System.out.println("EchoWebSocketApplication.onClose() # code -> " + code + " # data -> " + new String(utf8Data, "UTF-8"));
            System.out.println("code2 -> " + code2);
            System.out.println("code3 -> " + code3);
        } catch (UnsupportedEncodingException ignored) {}
        */
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        if ("disconnect".equals(text)) {
            socket.close();
        }
        else {
            // simple echo
            socket.send( text );
        }
    }

    @Override
    public void onMessage(WebSocket socket, byte[] bytes) {
        // simple echo
        socket.send( bytes );
    }

    @Override
    public void onPing(WebSocket socket, byte[] bytes) {
        // echo with pong
        socket.sendPong(bytes);
    }

    @Override
    public void onPong(WebSocket socket, byte[] bytes) {
        // for test purposes: send a ping on every pong
        socket.sendPing(bytes);
    }



    @Override
    public void onFragment(WebSocket socket, String fragment, boolean last) {
        // simple echo
        socket.stream(last, fragment);
    }

    @Override
    public void onFragment(WebSocket socket, byte[] fragment, boolean last) {
        // buffer and echo when got complete message

        synchronized(this) {
            buffer.add(fragment);
            if (last) {
                //log.debug(getClass(), "Streaming");
                for(byte[] bytes : buffer) {
                    //log.debug(getClass(), "CHUNK");
                    socket.stream(buffer.indexOf(bytes) == buffer.size()-1, bytes, 0, bytes.length);
                }
            }
            else {
                //log.debug(getClass(), "Buffered");
            }
        }
    }

    private final Vector<byte[]> buffer = new Vector<byte[]>();
}
