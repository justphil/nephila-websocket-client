/**
 *
 * This file is part of the
 *
 *          Nephila WebSocket Client (https://github.com/justphil/nephila-websocket-client)
 *
 * Copyright 2012 Philipp Tarasiewicz <philipp.tarasiewicz@googlemail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.cuckoo.websocket.nephila.impl;

import io.cuckoo.websocket.nephila.*;
import io.cuckoo.websocket.nephila.util.ConsoleLogger;
import io.cuckoo.websocket.nephila.util.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class DefaultWebSocket implements WebSocket {

    private volatile WebSocketListener  webSocketListener;
    private final WebSocketConfig       webSocketConfig;
    private final String[]              acceptingSubProtocols;
    private final List<String>          negotiatedSubProtocols;
    private final Random                random;
    private final Logger                log;
    private Socket                      socket;
    private InputStream                 input;
    private BufferedOutputStream        output;
    private WebSocketReceiver           receiver;
    private volatile boolean            connected;  // WebSocketReceiver-Thread may change the value
                                                    // [onServerClosingHandshake()]!
                                                    // Therefore this property must be marked as 'volatile'.
    private boolean                     streaming;

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    public DefaultWebSocket() {
        this(null);
    }

    public DefaultWebSocket(WebSocketListener webSocketListener) {
        this(webSocketListener, new String[0]);
    }

    public DefaultWebSocket(WebSocketListener webSocketListener, String[] acceptingSubProtocols) {
        this(webSocketListener, acceptingSubProtocols, new DefaultWebSocketConfig());
    }

    public DefaultWebSocket(WebSocketListener webSocketListener, WebSocketConfig webSocketConfig) {
        this(webSocketListener, new String[0], webSocketConfig);
    }

    public DefaultWebSocket(WebSocketListener webSocketListener, String[] acceptingSubProtocols, WebSocketConfig webSocketConfig) {
        if (webSocketConfig == null) {
            throw new IllegalArgumentException("webSocketConfig is null");
        }

        if (acceptingSubProtocols == null) {
            throw new IllegalArgumentException("acceptingSubProtocols is null");
        }

        this.webSocketListener      = webSocketListener;
        this.webSocketConfig        = webSocketConfig;
        this.acceptingSubProtocols  = acceptingSubProtocols;
        this.negotiatedSubProtocols = new ArrayList<String>(acceptingSubProtocols.length);
        this.random                 = new Random(System.nanoTime());
        this.log                    = new ConsoleLogger(webSocketConfig);
        this.connected              = false;
        this.streaming              = false;
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Override
    public WebSocketListener getWebSocketListener() {
        return webSocketListener;
    }

    @Override
    public void setWebSocketListener(WebSocketListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }

    @Override
    public WebSocketConfig getWebSocketConfig() {
        return webSocketConfig;
    }

    @Override
    public List<String> getNegotiatedSubProtocols() {
        return negotiatedSubProtocols;
    }

    @Override
    public synchronized void connect(URI uri) throws WebSocketException {
        failFastOnInvalidUri(uri);
        failFastOnAlreadyEstablishedConnection();

        try {
            // create opening handshake
            WebSocketHandshake handshake    = new WebSocketHandshake(uri);

            // establish tcp socket connection
            socket                          = createTcpConnection(uri);
            input                           = socket.getInputStream();
            output                          = new BufferedOutputStream(
                    socket.getOutputStream(),
                    webSocketConfig.getOutputBufferSize()
            );

            // send opening handshake
            sendOpeningHandshake(handshake.getHandshakeBytes(getAcceptingSubProtocolsAsCSV()));

            // wait for response containing server opening handshake and process it
            processServerOpeningHandshake(handshake);

            // instantiate and start receiver thread
            receiver = new WebSocketReceiver(this, input);
            receiver.start();

            // set connected flag to true
            connected = true;

            // notify listener
            if (webSocketListener != null) {
                webSocketListener.onConnect();
            }
        }
        catch (IOException ioe) {
            throw new WebSocketException("error while connecting to " + uri.toString() + ": " + ioe.getMessage());
        }
    }

    @Override
    public void connect(String uri) throws WebSocketException {
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }

        if (uri.trim().isEmpty()) {
            throw new IllegalArgumentException("uri is empty");
        }

        connect(URI.create(uri));
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public synchronized void send(String data) throws WebSocketException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        if (streaming) {
            throw new IllegalStateException("cannot send unfragmented payload data while in a streaming sequence");
        }

        try {
            write(data.getBytes("UTF-8"), true, (byte) 0x1, true);
        }
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
        }
    }

    @Override
    public synchronized void send(byte[] data) throws WebSocketException {
        if (streaming) {
            throw new IllegalStateException("cannot send unfragmented payload data while in a streaming sequence");
        }

        write(data, true, (byte) 0x2, true);
    }

    @Override
    public synchronized void stream(String data, boolean isFinalChunk) throws WebSocketException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        try {
            stream(data.getBytes("UTF-8"), isFinalChunk, (byte) 0x1);
        }
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
        }
    }

    @Override
    public synchronized void stream(byte[] data, boolean isFinalChunk) throws WebSocketException {
        stream(data, isFinalChunk, (byte) 0x2);
    }

    @Override
    public void ping() throws WebSocketException {
        ping(new byte[0]);
    }

    @Override
    public void ping(String data) throws WebSocketException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        try {
            ping(data.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
        }
    }

    @Override
    public void ping(byte[] data) throws WebSocketException {
        write(data, true, (byte) 0x9, true);
    }

    @Override
    public void pong() throws WebSocketException {
        pong(new byte[0]);
    }

    @Override
    public void pong(String data) throws WebSocketException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        try {
            pong(data.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
        }
    }

    @Override
    public void pong(byte[] data) throws WebSocketException {
        write(data, true, (byte) 0xA, true);
    }

    @Override
    public void close() throws WebSocketException {
        close("");
    }

    @Override
    public void close(String reason) throws WebSocketException {
        close(reason, true);
    }

    public synchronized void close(String reason, boolean clientInitiatingConnectionClose) throws WebSocketException {
        if (!connected) {
            throw new IllegalStateException("not connected");
        }

        if (reason == null) {
            throw new IllegalArgumentException("reason is null");
        }

        if (clientInitiatingConnectionClose) {
            //log.debug(getClass(), "client initiating connection close");

            // enable receiver to wait for server closing handshake
            receiver.enableWaitingForServerClosingHandshake();

            // send closing handshake
            sendClosingHandshake(WebSocketClosureStatusCode.NORMAL, reason);
        }
        else {
            //log.debug(getClass(), "server initiating connection close");

            // send closing handshake if socket is still connected
            if (socket.isConnected() && !socket.isOutputShutdown()) {
                sendClosingHandshake(WebSocketClosureStatusCode.NORMAL, reason);
            }
            onServerClosingHandshake();
        }
    }

    public synchronized void onServerClosingHandshake() {
        try {
            connected = false;
            receiver.stopIt();
            closeTcpConnection();
        }
        catch (IOException ignored) {
            log.error(getClass(), "error while closing websocket connection: " + ignored.getMessage());
        }
        finally {
            if (webSocketListener != null) {
                webSocketListener.onClose();
            }
        }
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    public void handleReceiverError(String reason) {
        try {
            if (connected) {
                close(reason, false);
            }
        }
        catch (WebSocketException ignore) {

        }
    }

    public synchronized void closeSilently() {
        try {
            close("", false);
        } catch (WebSocketException e) {
            // ignored
        }
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private void failFastOnAlreadyEstablishedConnection() {
        if (connected) {
            throw new IllegalStateException("websocket is already connected");
        }
    }

    private void failFastOnInvalidUri(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }

        if (uri.getScheme() == null) {
            throw new IllegalArgumentException("invalid uri: scheme is null");
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException("invalid uri: host is null");
        }
    }

    private Socket createTcpConnection(URI uri) throws WebSocketException {
        final String scheme = uri.getScheme();
        final String host   = uri.getHost();
        final int port      = (uri.getPort() == -1) ? 80 : uri.getPort();
        Socket socket;

        try {
            if (scheme.equals("ws")) {
                SocketAddress sockAddr = new InetSocketAddress(host, port);
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.connect(sockAddr, webSocketConfig.getSocketTimeout());
            }
            else if (scheme.equals("wss")) {
                SocketFactory factory = SSLSocketFactory.getDefault();
                socket = factory.createSocket(host, port);
            }
            else {
                throw new WebSocketException("unsupported protocol: " + scheme);
            }
        }
        catch (SocketTimeoutException ste) {
            throw new WebSocketException("socket timeout while connecting to " + host + ":" + port);
        }
        catch (UnknownHostException uhe) {
            throw new WebSocketException("unknown host: " + host);
        }
        catch (IOException ioe) {
            throw new WebSocketException("error while creating tcp connection to " + host + ":" + port, ioe);
        }

        return socket;
    }

    private void closeTcpConnection() throws IOException {
        if (input != null) {
            input.close();
        }

        if (output != null) {
            output.close();
        }

        if (socket != null) {
            socket.close();
        }
    }

    private String getAcceptingSubProtocolsAsCSV() {
        String out = null;

        if (acceptingSubProtocols.length > 0) {
            StringBuilder sb = new StringBuilder(64);
            int i = 0;
            for (String acceptingSubProtocol : acceptingSubProtocols) {
                sb.append(acceptingSubProtocol);

                if (i < acceptingSubProtocols.length-1) {
                    sb.append(", ");
                }

                i++;
            }

            if (sb.length() > 0) {
                out = sb.toString();
            }
        }

        return out;
    }

    private void sendOpeningHandshake(byte[] handshakeBytes) throws IOException {
        output.write(handshakeBytes);
        output.flush();
    }

    private synchronized void sendClosingHandshake(
            WebSocketClosureStatusCode code, String reason) throws WebSocketException {
        try {
            byte[] reasonBytes = reason.getBytes("UTF-8");

            byte[] oversizedPayload = ByteBuffer
                            .allocate(4 + reasonBytes.length)   // 4 bytes (= 32 bit) for the integer holding the code
                                                                //      + number of bytes for the reason
                            .order(ByteOrder.BIG_ENDIAN)
                            .putInt(code.code())
                            .put(reasonBytes)
                            .array();

            // but real payload is 2 bytes smaller because spec says that the code is denoted by a 16 bit integer
            // (NOT 32 bit like Java int)
            byte[] realPayload = new byte[oversizedPayload.length - 2];
            // so remove the leading two bytes
            System.arraycopy(oversizedPayload, 2, realPayload, 0, realPayload.length);

            // send closing handshake
            write(realPayload, true, (byte) 0x8, true);
        }
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
        }
    }

    private void processServerOpeningHandshake(WebSocketHandshake handshake) throws IOException, WebSocketException {
        List<String> serverHandshakeLines = new ArrayList<String>();
        BufferedReader bufferedIn = new BufferedReader(new InputStreamReader(input, "ISO-8859-1"));

        /*
         * HTTP spec says that the headers must be separated from the body by two CRLF symbols.
         * So, we have to listen for an empty line while reading lines with the BufferedReader.
         */

        String s;
        boolean ready = false;
        while( (!ready && (s = bufferedIn.readLine()) != null)) {   // the !ready must be on the left hand side of the
                                                                    // boolean expression because readLine() is
                                                                    // a blocking call!
            if (!s.isEmpty()) {
                serverHandshakeLines.add(s);
                //log.debug(getClass(), "processServerOpeningHandshake() # " + s);
            }
            else {
                ready = true;
            }
        }

        if (serverHandshakeLines.size() == 0) {
            throw new WebSocketException("connection couldn't be established due to an invalid server opening handshake");
        }

        handshake.verifyServerStatusLine(serverHandshakeLines.get(0));
        handshake.verifySecWebSocketAccept(getSecWebSocketAccept(serverHandshakeLines));
        negotiatedSubProtocols.addAll(
                handshake.negotiateSubProtocols(acceptingSubProtocols, getSecWebSocketProtocol(serverHandshakeLines, false))
        );

        serverHandshakeLines.remove(0);
        HashMap<String, String> headers = new HashMap<String, String>();
        String[] keyValue;
        for (String line : serverHandshakeLines) {
            keyValue = line.split( Pattern.quote(":") );
            headers.put(keyValue[0].trim(), keyValue[1].trim());
        }
        handshake.verifyServerHandshakeHeaders(headers);
    }

    /*
    private boolean checkReady(String s) {
        return s.isEmpty();

        boolean ready = false;

        final String secWebSocketProtocol   = "Sec-WebSocket-Protocol";
        final String secWebSocketAccept     = "Sec-WebSocket-Accept";

        if (handshake.getProtocol() != null) {
            String tmp = s.trim();
            if (tmp.length() >= secWebSocketProtocol.length()) {
                if (secWebSocketProtocol.equalsIgnoreCase(tmp.substring(0, secWebSocketProtocol.length()))) {
                    ready = true;
                }
            }
        }
        else {
            String tmp = s.trim();
            if (tmp.length() >= secWebSocketAccept.length()) {
                if (secWebSocketAccept.equalsIgnoreCase(tmp.substring(0, secWebSocketAccept.length()))) {
                    ready = true;
                }
            }
        }

        return ready;
    }
    */

    private String getSecWebSocketAccept(List<String> handshakeLines) throws WebSocketException {
        final String secWebSocketAccept = "Sec-WebSocket-Accept";

        String out = getResponseHeaderLine(handshakeLines, secWebSocketAccept);

        if (out != null) {
            return out;
        }

        throw new WebSocketException(
                "response header '" + secWebSocketAccept.toLowerCase() + "' is missing in the server opening handshake");
    }

    private String getSecWebSocketProtocol(final List<String> handshakeLines, final boolean failOnAbsence) throws WebSocketException {
        final String secWebSocketProtocol = "Sec-WebSocket-Protocol";

        String out = getResponseHeaderLine(handshakeLines, secWebSocketProtocol);

        if (out != null) {
            return out;
        }

        if (failOnAbsence) {
            throw new WebSocketException(
                    "response header '" + secWebSocketProtocol.toLowerCase() + "' is missing in the server opening handshake");
        }
        else {
            return null;
        }
    }

    private String getResponseHeaderLine(List<String> handshakeLines, String header) throws WebSocketException {
        if (handshakeLines == null) {
            throw new IllegalArgumentException("handshakeLines is null");
        }

        if (header == null) {
            throw new IllegalArgumentException("header is null");
        }

        for (String line : handshakeLines) {
            if (line.length() >= header.length()
                    && header.equalsIgnoreCase(line.trim().substring(0, header.length()))) {
                return line;
            }
        }

        return null;
    }

    private int getRandomInt() {
        return random.nextInt(256);
    }

    private byte[] maskPayload(byte[] payload, byte[] maskingKey) {
        byte[] out = new byte[payload.length];

        for(int i = 0; i < out.length; i++) {
            out[i] = (byte) (payload[i] ^ maskingKey[i%4]);
        }

        return out;
    }

    private synchronized void write(byte[] data, boolean fin, byte opCode, boolean mask) throws WebSocketException {
        if (data == null) {
            throw new IllegalArgumentException("data is null");
        }

        if (!connected) {
            throw new IllegalStateException("error while sending data: not connected to a websocket server");
        }

        try {
            int payloadSize = data.length;

            // copy data because it will be manipulated (mask)
            // byte is primitive, so we get a deep copy although cloning
            byte[] payloadBytes = data.clone();

            // FIN bit
            //log.debug(getClass(), "write() # fin -> " + fin);
            int b;
            if (fin) {
                b = 0x80;
            }
            else {
                b = 0;
            }

            // op code
            b = b | opCode;

            // FIN bit set / rsv1/2/3 set to 0 / op code %x1 denotes a text frame
            //output.write(0x81); // fin (1), rsv1 (1), rsv2 (1), rsv3 (1) and op code (4)
            output.write(b);


            /*
                Payload length:  7 bits, 7+16 bits, or 7+64 bits

                  The length of the "Payload data", in bytes: if 0-125, that is the
                  payload length.  If 126, the following 2 bytes interpreted as a
                  16-bit unsigned integer are the payload length.  If 127, the
                  following 8 bytes interpreted as a 64-bit unsigned integer (the
                  most significant bit MUST be 0) are the payload length.  Multibyte
                  length quantities are expressed in network byte order.  Note that
                  in all cases, the minimal number of bytes MUST be used to encode
                  the length, for example, the length of a 124-byte-long string
                  can't be encoded as the sequence 126, 0, 124.  The payload length
                  is the length of the "Extension data" + the length of the
                  "Application data".  The length of the "Extension data" may be
                  zero, in which case the payload length is the length of the
                  "Application data".
             */


            //byte[] payloadBytes = data.getBytes("UTF-8");


            int i;
            if (payloadSize <= 125) {
                //log.debug(getClass(), "write() # payloadSize <= 125");
                i = payloadSize;
                if (mask) {
                    i = i | 128; // set mask bit to 1
                }
                output.write(i); // mask (1) and payload len (7)
            }
            else if (payloadSize <= 65535) {
                //log.debug(getClass(), "write() # payloadSize <= 65535");
                /*
                    The largest 16-bit unsigned integer is 1111111111111111 in binary (sixteen 1s), which is equal
                    to 65535 in decimal.
                 */

                i = 126;
                if (mask) {
                    i = i | 128; // set mask bit to 1
                }
                output.write(i); // mask (1) and payload len (7)

                // 16-bit unsigned integer (2 bytes)
                byte[] bit16 = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt( payloadSize ).array();
                output.write(bit16, 2, 2); // send the two low order bytes
            }
            else {
                //log.debug(getClass(), "write() # payloadSize <= " + Integer.MAX_VALUE);
                i = 127;
                if (mask) {
                    i = i | 128; // set mask bit to 1
                }

                output.write(i); // mask (1) and payload len (7)

                // the actual payload size can be max. Integer.MAX_VALUE bytes because the byte array length is
                // bounded by Integer.MAX_VALUE.
                // but the spec says that payload sizes larger than 65535 bytes must be denoted by
                // a 64-bit unsigned integer (the most significant bit MUST be 0), therefore we describe an 32 bit
                // integer as a 64 bit long
                byte[] bit64 = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong((long) payloadSize).array();
                output.write(bit64);
            }

            /*
             *
             * j                   = i MOD 4
             * transformed-octet-i = original-octet-i XOR masking-key-octet-j
             *
             */

            if (mask) {
                byte[] maskingKey = new byte[] { (byte) getRandomInt(), (byte) getRandomInt(), (byte) getRandomInt(), (byte) getRandomInt() };
                output.write(maskingKey[0]); // *
                output.write(maskingKey[1]); // * masking key (32)
                output.write(maskingKey[2]); // *
                output.write(maskingKey[3]); // *
                payloadBytes = maskPayload(payloadBytes, maskingKey);
            }


            output.write(payloadBytes);
            output.flush();
        }
        catch (IOException ioe) {
            throw new WebSocketException("error while sending data", ioe);
        }
    }

    private synchronized void stream(byte[] data, boolean isFinalChunk, byte initialFrameOpCode) throws WebSocketException {
        if (streaming) {
            // send continuation frame
            write(data, isFinalChunk, (byte) 0x0, true);
            if (isFinalChunk) {
                streaming = false;
            }
        }
        else {
            // send initial chunk
            write(data, isFinalChunk, initialFrameOpCode, true);
            if (!isFinalChunk) {
                streaming = true;
            }
        }
    }

}
