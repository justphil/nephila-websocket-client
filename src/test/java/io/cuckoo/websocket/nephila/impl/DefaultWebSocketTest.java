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

import io.cuckoo.websocket.nephila.WebSocket;
import io.cuckoo.websocket.nephila.WebSocketConfig;
import io.cuckoo.websocket.nephila.WebSocketException;
import io.cuckoo.websocket.nephila.WebSocketListener;
import io.cuckoo.websocket.nephila.utils.ReceivingStreamWebSocketApplication;
import io.cuckoo.websocket.nephila.utils.SendDataOnConnectWebSocketApplication;
import io.cuckoo.websocket.nephila.utils.WebSocketServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;

public class DefaultWebSocketTest {

    private static final URI ECHO_URI           = URI.create("ws://localhost:8888/echo");
    private static final URI SUB_PROTOCOL_URI   = URI.create("ws://localhost:8888/subprotocoltest");
    private static final String ALPHABET        = "abcdefghijklmnopqrstuvwxyz";
    private static final int STREAMING_SEQUENCE_LENGTH = 10;

    // These properties are overwritten by multiple threads, therefore marked as 'volatile'
    private volatile String receivedStringMessage;
    private volatile byte[] receivedByteMessage;

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @BeforeClass
    public static void setUpClass() throws Exception {
        WebSocketServer.getInstance().start();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        WebSocketServer.getInstance().stop();
        Thread.sleep(3000);
    }

    @Before
    public void setUp() throws Exception {
        receivedStringMessage   = null;
        receivedByteMessage     = null;
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Test
    public void testGetWebSocketListener() throws Exception {
        WebSocketListener webSocketListener = new DummyListener();
        WebSocket ws = new DefaultWebSocket(webSocketListener);

        assertSame("ws.getWebSocketListener() must return the same (identity!) object that has been passed to the constructor",
                webSocketListener, ws.getWebSocketListener());
    }

    @Test
    public void testGetWebSocketConfig() throws Exception {
        WebSocketListener webSocketListener = new DummyListener();
        WebSocketConfig webSocketConfig = new DefaultWebSocketConfig();
        WebSocket ws = new DefaultWebSocket(webSocketListener, webSocketConfig);

        assertSame("ws.getWebSocketConfig() must return the same (identity!) object that has been passed to the constructor",
                webSocketConfig, ws.getWebSocketConfig());
    }

    @Test
    public void testConnectAndClose() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        assertFalse("not connected yet, so ws.isConnected() must return false", ws.isConnected());
        ws.connect(ECHO_URI);
        assertTrue("connected, so ws.isConnected() must return true", ws.isConnected());
        assertTrue("connected, so receivingDataListener.isConnected() must return true", receivingDataListener.isConnected());
        ws.close();
        Thread.sleep(500);
        assertFalse("disconnected, so ws.isConnected() must return false", ws.isConnected());
        assertFalse("disconnected, so receivingDataListener.isConnected() must return false", receivingDataListener.isConnected());
    }

    @Test
    public void testConnectAndCloseWithString() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        assertFalse("not connected yet, so ws.isConnected() must return false", ws.isConnected());
        ws.connect(ECHO_URI);
        assertTrue("connected, so ws.isConnected() must return true", ws.isConnected());
        assertTrue("connected, so receivingDataListener.isConnected() must return true", receivingDataListener.isConnected());
        ws.close("NORMAL CONNECTION CLOSURE");
        Thread.sleep(500);
        assertFalse("disconnected, so ws.isConnected() must return false", ws.isConnected());
        assertFalse("disconnected, so receivingDataListener.isConnected() must return false", receivingDataListener.isConnected());
    }

    @Test(expected = WebSocketException.class)
    public void testConnectWithSubProtocolThatServerCannotSpeak() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener, new String[] {"chat"});
        ws.connect(SUB_PROTOCOL_URI);
        ws.close();
    }

    @Test
    public void testConnectWithSubProtocolThatServerCanSpeak() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener, new String[] {"chat.cuckoo.io"});
        ws.connect(SUB_PROTOCOL_URI);
        assertTrue("connection must be established because the server can speak the specified subprotocol", ws.isConnected());
        ws.close();
    }

    @Test(expected=IllegalStateException.class)
    public void testSendWithoutBeingConnected() throws Exception {
        WebSocket ws = new DefaultWebSocket(new DummyListener());
        ws.send("test");
        ws.close();
    }

    @Test(expected=IllegalStateException.class)
    public void testMultipleConnections() throws Exception {
        WebSocket ws = new DefaultWebSocket(new DummyListener());
        ws.connect(ECHO_URI);
        ws.connect(ECHO_URI);
    }

    @Test(expected=IllegalStateException.class)
    public void testDisconnectWithoutBeingConnected() throws Exception {
        WebSocket ws = new DefaultWebSocket(new DummyListener());
        ws.close();
    }

    @Test
    public void testServerInitiatingConnectionClose() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);

        assertTrue("ws.isConnected() must be true", ws.isConnected());
        assertTrue("receivingDataListener.isConnected() must be true", receivingDataListener.isConnected());

        // this will trigger the server to disconnect us
        ws.send("disconnect");

        Thread.sleep(500);

        assertFalse("ws.isConnected() must be false", ws.isConnected());
        assertFalse("receivingDataListener.isConnected() must be false", receivingDataListener.isConnected());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testInvalidUri() throws Exception {
        WebSocket ws = new DefaultWebSocket(new DummyListener());
        ws.connect("abc");
        ws.close();
    }

    @Test(expected=WebSocketException.class)
    public void testInvalidUriScheme() throws Exception {
        WebSocket ws = new DefaultWebSocket(new DummyListener());
        ws.connect("http://localhost");
        ws.close();
    }

    @Test
    public void testSendString() throws Exception {
        WebSocket ws = new DefaultWebSocket(new WebSocketListener() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onClose() {
            }

            @Override
            public void onMessage(String message) {
                receivedStringMessage = message;
            }

            @Override
            public void onMessage(byte[] message) {
            }

            @Override
            public void onMessageChunk(String messageChunk, boolean isFinalChunk) {
            }

            @Override
            public void onMessageChunk(byte[] messageChunk, boolean isFinalChunk) {
            }

            @Override
            public void onPing() {
            }

            @Override
            public void onPing(byte[] data) {
            }

            @Override
            public void onPong() {
            }

            @Override
            public void onPong(byte[] data) {
            }
        });

        ws.connect(ECHO_URI);
        ws.send("TEST");
        Thread.sleep(1000); // wait in order to ensure that the echoed data has been received (receiving happens in another thread!)

        assertTrue("Sent message must be equal to received message.", "TEST".equals(receivedStringMessage));

        ws.close();
    }

    @Test
    public void testSendStringGreaterThan125Bytes() throws Exception {
        // utf-8 encoded string with 126 chars needs min. 126 bytes
        StringBuilder sb = new StringBuilder(126);
        Random r = new Random(System.nanoTime());
        while (sb.length() < sb.capacity()) {
            sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        }

        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        ws.send(sb.toString());

        Thread.sleep(500);

        assertNotNull("receivingDataListener.getText() cannot be null", receivingDataListener.getText());
        assertEquals("sent text must be equal to received text", sb.toString(), receivingDataListener.getText());

        ws.close();
    }

    @Test
    public void testSendStringGreaterThan65535Bytes() throws Exception {
        // utf-8 encoded string with 65535*2 chars needs min. 65535*2 bytes
        StringBuilder sb = new StringBuilder(65535*2);
        Random r = new Random(System.nanoTime());
        while (sb.length() < sb.capacity()) {
            sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        }

        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        ws.send(sb.toString());

        Thread.sleep(3000);

        assertNotNull("receivingDataListener.getText() cannot be null", receivingDataListener.getText());
        assertEquals("sent text must be equal to received text", sb.toString(), receivingDataListener.getText());

        ws.close();
    }

    @Test
    public void testSendBytes() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);
        byte[] data = new byte[] {0x1,0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8};
        byte[] expected = data.clone();
        ws.send(data);
        Thread.sleep(1000);

        assertNotNull("receivingDataListener.getReceivedBytes() shall not be null after having waited for the echo",
                receivingDataListener.getReceivedBytes());

        receivedByteMessage = receivingDataListener.getReceivedBytes();

        for(int i = 0; i < data.length; i++) {
            assertEquals("the received byte array must be equal to the sent byte array", expected[i], receivedByteMessage[i]);
        }

        ws.close();
    }

    @Test
    public void testSendMoreThan125Bytes() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);

        Random r = new Random(System.nanoTime());
        byte[] data = new byte[126];
        r.nextBytes(data);

        byte[] expected = data.clone();

        ws.send(data);
        Thread.sleep(1000);

        assertNotNull("receivingDataListener.getReceivedBytes() shall not be null after having waited for the echo",
                receivingDataListener.getReceivedBytes());

        receivedByteMessage = receivingDataListener.getReceivedBytes();

        assertTrue("the received byte array must be equal to the sent byte array", Arrays.equals(expected, receivedByteMessage));

        ws.close();
    }

    @Test
    public void testSendMoreThan65535Bytes() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);

        Random r = new Random(System.nanoTime());
        byte[] data = new byte[65536];
        r.nextBytes(data);

        byte[] expected = data.clone();

        ws.send(data);
        Thread.sleep(1000);

        assertNotNull("receivingDataListener.getReceivedBytes() shall not be null after having waited for the echo",
                receivingDataListener.getReceivedBytes());

        receivedByteMessage = receivingDataListener.getReceivedBytes();

        assertTrue("the received byte array must be equal to the sent byte array", Arrays.equals(expected, receivedByteMessage));

        ws.close();
    }

    @Test
    public void testStreamEmptyStringChunks() throws Exception {
        streamStringChunks(0);
    }

    @Test
    public void testStreamOneCharStringChunks() throws Exception {
        streamStringChunks(1);
    }

    @Test
    public void testStreamStringChunksOf125Bytes() throws Exception {
        streamStringChunks(125);
    }

    @Test
    public void testStreamStringChunksGreaterThan125Bytes() throws Exception {
        streamStringChunks(126);
    }

    @Test
    public void testStreamStringChunksOf65535Bytes() throws Exception {
        streamStringChunks(65535);
    }

    @Test
    public void testStreamStringChunksGreaterThan65535Bytes() throws Exception {
        streamStringChunks(65536);
    }

    @Test
    public void testStreamZeroByteChunks() throws Exception {
        streamByteChunks(0);
    }

    @Test
    public void testStreamSmallByteChunks() throws Exception {
        streamByteChunks(1);
    }

    @Test
    public void testStreamByteChunksOf125Bytes() throws Exception {
        streamByteChunks(125);
    }

    @Test
    public void testStreamByteChunksGreaterThan125Bytes() throws Exception {
        streamByteChunks(126);
    }

    @Test
    public void testStreamByteChunksOf65535Bytes() throws Exception {
        streamByteChunks(65535);
    }

    @Test
    public void testStreamByteChunksGreaterThan65535Bytes() throws Exception {
        streamByteChunks(65536);
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Test
    public void testReceiveDataAfterHavingConnected() throws Exception {
        WebSocket ws = new DefaultWebSocket();
        ws.setWebSocketListener(new WebSocketListener() {
            @Override
            public void onConnect() {
            }

            @Override
            public void onClose() {
            }

            @Override
            public void onMessage(String message) {
                receivedStringMessage = message;
            }

            @Override
            public void onMessage(byte[] message) {
            }

            @Override
            public void onMessageChunk(String messageChunk, boolean isFinalChunk) {
            }

            @Override
            public void onMessageChunk(byte[] messageChunk, boolean isFinalChunk) {
            }

            @Override
            public void onPing() {
            }

            @Override
            public void onPing(byte[] data) {
            }

            @Override
            public void onPong() {
            }

            @Override
            public void onPong(byte[] data) {
            }
        });

        ws.connect("ws://127.0.0.1:8888/" + SendDataOnConnectWebSocketApplication.PATH);

        Thread.sleep(1000);

        assertNotNull("after having connected to the server it should have sent a text message -> receivedStringMessage cannot be null",
                receivedStringMessage);

        long timestamp = Long.parseLong(receivedStringMessage);

        assertTrue("timestamp should be quite recent", timestamp > System.currentTimeMillis()-10000);
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Test(expected = IllegalStateException.class)
    public void testCannotSendUnfragmentedStringPayloadDataWhileInStringStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream("abc", false);
        // now send an unfragmented string while being still in the streaming sequence (final chunk not sent yet!)
        ws.send("test");
        ws.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotSendUnfragmentedStringPayloadDataWhileInByteStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream(new byte[] {0xA}, false);
        // now send an unfragmented string while being still in the streaming sequence (final chunk not sent yet!)
        ws.send("test");
        ws.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotSendUnfragmentedBytePayloadDataWhileInStringStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream("abc", false);
        // now send an unfragmented byte while being still in the streaming sequence (final chunk not sent yet!)
        ws.send(new byte[] {0xA});
        ws.close();
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotSendUnfragmentedBytePayloadDataWhileInByteStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream(new byte[] {0xA}, false);
        // now send an unfragmented byte while being still in the streaming sequence (final chunk not sent yet!)
        ws.send(new byte[] {0xA});
        ws.close();
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Test
    public void testSendUnfragmentedStringPayloadDataAfterBeingInStringStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream("abc", false);
        // end streaming
        ws.stream("def", true);
        // now send an unfragmented string after having been in the streaming sequence (final chunk has been sent!)
        ws.send("test");
        ws.close();
    }

    @Test
    public void testSendUnfragmentedStringPayloadDataAfterBeingInByteStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream(new byte[] {0xA}, false);
        // end streaming
        ws.stream(new byte[] {0xB}, true);
        // now send an unfragmented string after having been in the streaming sequence (final chunk has been sent!)
        ws.send("test");
        ws.close();
    }

    @Test
    public void testSendUnfragmentedBytePayloadDataAfterBeingInStringStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream("abc", false);
        // end streaming
        ws.stream("def", true);
        // now send an unfragmented byte after having been in the streaming sequence (final chunk has been sent!)
        ws.send(new byte[] {0xA});
        ws.close();
    }

    @Test
    public void testSendUnfragmentedBytePayloadDataAfterBeingInByteStreamingSequence() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);
        ws.connect(ECHO_URI);
        // start streaming
        ws.stream(new byte[] {0xA}, false);
        // end streaming
        ws.stream(new byte[] {0xB}, true);
        // now send an unfragmented byte after having been in the streaming sequence (final chunk has been sent!)
        ws.send(new byte[] {0xA});
        ws.close();
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Test
    public void testPing() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);
        ws.ping();

        Thread.sleep(500);

        assertTrue("receivingDataListener.isPongReceived() must be true", receivingDataListener.isPongReceived());

        ws.close();
    }

    @Test
    public void testPingWithString() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);

        final String pingText = "ALIVE?";

        ws.ping(pingText);

        Thread.sleep(500);

        assertTrue("receivingDataListener.isPongReceived() must be true", receivingDataListener.isPongReceived());
        assertNotNull("receivingDataListener.getPongData() cannot be null", receivingDataListener.getPongData());

        assertEquals("sent ping text must be equal to received pong text", pingText, new String(receivingDataListener.getPongData(), "UTF-8"));

        ws.close();
    }

    @Test
    public void testPingWithBytes() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);

        final byte[] pingBytes = {0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8};

        final byte[] expected = pingBytes.clone();

        ws.ping(pingBytes);

        Thread.sleep(500);

        assertTrue("receivingDataListener.isPongReceived() must be true", receivingDataListener.isPongReceived());
        assertNotNull("receivingDataListener.getPongData() cannot be null", receivingDataListener.getPongData());

        receivedByteMessage = receivingDataListener.getPongData();
        for(int i = 0; i < pingBytes.length; i++) {
            assertEquals("sent ping bytes must be equal to received pong bytes", expected[i], receivedByteMessage[i]);
        }

        ws.close();
    }

    @Test
    public void testPong() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);
        ws.pong();

        Thread.sleep(500);

        // for test purposes the echo application responds to a pong with a ping
        assertTrue("receivingDataListener.isPingReceived() must be true", receivingDataListener.isPingReceived());

        ws.close();
    }

    @Test
    public void testPongWithString() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);

        final String pongText = "ALIVE!";

        ws.pong(pongText);

        Thread.sleep(500);

        // for test purposes the echo application responds to a pong with a ping
        assertTrue("receivingDataListener.isPingReceived() must be true", receivingDataListener.isPingReceived());
        assertNotNull("receivingDataListener.getPingData() cannot be null", receivingDataListener.getPingData());

        assertEquals("sent pong text must be equal to received ping text", pongText, new String(receivingDataListener.getPingData(), "UTF-8"));

        ws.close();
    }

    @Test
    public void testPongWithBytes() throws Exception {
        ReceivingDataListener receivingDataListener = new ReceivingDataListener();
        WebSocket ws = new DefaultWebSocket(receivingDataListener);

        ws.connect(ECHO_URI);

        final byte[] pongBytes = {0x1, 0x2, 0x3, 0x4, 0x5, 0x6, 0x7, 0x8};
        final byte[] expected = pongBytes.clone();

        ws.pong(pongBytes);

        Thread.sleep(500);

        // for test purposes the echo application responds to a pong with a ping
        assertTrue("receivingDataListener.isPingReceived() must be true", receivingDataListener.isPingReceived());
        assertNotNull("receivingDataListener.getPingData() cannot be null", receivingDataListener.getPingData());

        receivedByteMessage = receivingDataListener.getPingData();
        for(int i = 0; i < pongBytes.length; i++) {
            assertEquals("sent pong bytes must be equal to received ping bytes", expected[i], receivedByteMessage[i]);
        }

        ws.close();
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private void streamStringChunks(int chunkSize) throws Exception {
        ConnectionIdListener connectionIdListener = new ConnectionIdListener();
        WebSocket ws = new DefaultWebSocket(connectionIdListener);
        ws.connect("ws://localhost:8888/" + ReceivingStreamWebSocketApplication.PATH);

        ws.send(ReceivingStreamWebSocketApplication.NEW_TEXT_STREAMING_SEQUENCE_INDICATOR);

        // wait for connectionId
        Thread.sleep(1000);

        // utf-8 encoded string with chunkSize chars needs min. chunkSize bytes
        StringBuilder sb = new StringBuilder(chunkSize);
        Random r = new Random(System.nanoTime());
        while (sb.length() < sb.capacity()) {
            sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
        }

        StringBuilder expectedSb = new StringBuilder(STREAMING_SEQUENCE_LENGTH * sb.length());
        final String toSend = sb.toString();
        for (int i = 0; i < STREAMING_SEQUENCE_LENGTH; i++) {
            expectedSb.append(toSend);
            ws.stream(toSend, (i == STREAMING_SEQUENCE_LENGTH - 1));
        }

        Thread.sleep(2000);

        String textServerHasReceived = ReceivingStreamWebSocketApplication.getInstance().getTextDataByConnectionId(connectionIdListener.getConnectionId());

        assertNotNull("textServerHasReceived cannot be null", textServerHasReceived);

        assertEquals("the sent text must be equal to the text the server has received", expectedSb.toString(), textServerHasReceived);

        ws.close();
    }

    private void streamByteChunks(int chunkSize) throws Exception {
        WebSocket ws = new DefaultWebSocket();
        ConnectionIdListener connectionIdListener = new ConnectionIdListener();
        ws.setWebSocketListener(connectionIdListener);
        ws.connect("ws://localhost:8888/" + ReceivingStreamWebSocketApplication.PATH);

        ws.send(ReceivingStreamWebSocketApplication.NEW_BINARY_STREAMING_SEQUENCE_INDICATOR);

        // wait for connectionId
        Thread.sleep(1000);

        Random r = new Random(System.nanoTime());
        byte[] data = new byte[chunkSize * STREAMING_SEQUENCE_LENGTH];
        r.nextBytes(data);


        byte[] tmp;
        int x;
        for(int i = 0; i < STREAMING_SEQUENCE_LENGTH; i++) {
            tmp = new byte[data.length/STREAMING_SEQUENCE_LENGTH];
            for(int j = 0; j < tmp.length; j++) {
                x = j + (i * tmp.length);
                tmp[j] = data[x];
            }
            ws.stream(tmp, (i == STREAMING_SEQUENCE_LENGTH-1));
        }

        Thread.sleep(2000);

        byte[] binaryData = ReceivingStreamWebSocketApplication.getInstance().getBinaryDataByConnectionId(connectionIdListener.getConnectionId());

        assertNotNull("binaryData cannot be null", binaryData);

        assertTrue("the sent binary data must be equal the binary data the server has received", Arrays.equals(data, binaryData));

        ws.close();
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private class ReceivingDataListener implements WebSocketListener {
        //private final List<Byte> data            = new ArrayList<Byte>();
        //private volatile boolean finished       = false;
        private volatile String text            = "";
        private volatile byte[] receivedBytes   = null;
        private volatile boolean pingReceived   = false;
        private volatile byte[] pingData        = null;
        private volatile boolean pongReceived   = false;
        private volatile byte[] pongData        = null;
        private volatile boolean connected      = false;


        /*public byte[] getData() {
            byte[] byteArray = new byte[data.size()];
            int i = 0;
            for(byte b : data) {
                byteArray[i] = b;
                i++;
            }

            return byteArray;
        }*/

        public String getText() {
            return text;
        }

        public byte[] getReceivedBytes() {
            return receivedBytes;
        }

        /*public boolean isFinished() {
            return finished;
        }*/

        public boolean isPingReceived() {
            return pingReceived;
        }

        public byte[] getPingData() {
            return pingData;
        }

        public boolean isPongReceived() {
            return pongReceived;
        }

        public byte[] getPongData() {
            return pongData;
        }

        public boolean isConnected() {
            return connected;
        }

        @Override
        public void onConnect() {
            connected = true;
        }

        @Override
        public void onClose() {
            connected = false;
        }

        @Override
        public void onMessage(String message) {
            text = message;
        }

        @Override
        public void onMessage(byte[] message) {
            receivedBytes = message;
            onMessageChunk(message, true);
        }

        @Override
        public void onMessageChunk(String messageChunk, boolean isFinalChunk) {
            text += messageChunk;

            /*if (isFinalChunk) {
                finished = true;
            }*/
        }

        @Override
        public void onMessageChunk(byte[] messageChunk, boolean isFinalChunk) {
            //log.debug(getClass(), "onMessageChunk() # length -> " + messageChunk.length + " # last -> " + isFinalChunk);
            /*
            synchronized (data) {
                for(byte b : messageChunk) {
                    data.add(b);
                }
            }

            if (isFinalChunk) {
                finished = true;
            }*/
        }

        @Override
        public void onPing() {
            pingReceived = true;
        }

        @Override
        public void onPing(byte[] data) {
            pingReceived = true;
            pingData = data;
        }

        @Override
        public void onPong() {
            pongReceived = true;
        }

        @Override
        public void onPong(byte[] data) {
            pongReceived = true;
            pongData = data;
        }
    }

    private class ConnectionIdListener implements WebSocketListener {
        private volatile int connectionId;

        /* ######################################################################## */
        /* ######################################################################## */
        /* ######################################################################## */

        public int getConnectionId() {
            return connectionId;
        }

        /* ######################################################################## */
        /* ######################################################################## */
        /* ######################################################################## */

        @Override
        public void onConnect() {}

        @Override
        public void onClose() {}

        @Override
        public void onMessage(String message) {
            connectionId = Integer.parseInt(message);
        }

        @Override
        public void onMessage(byte[] message) {}

        @Override
        public void onMessageChunk(String messageChunk, boolean isFinalChunk) {}

        @Override
        public void onMessageChunk(byte[] messageChunk, boolean isFinalChunk) {}

        @Override
        public void onPing() {}

        @Override
        public void onPing(byte[] data) {}

        @Override
        public void onPong() {}

        @Override
        public void onPong(byte[] data) {}
    }

    private class DummyListener implements WebSocketListener {
        @Override
        public void onConnect() {}

        @Override
        public void onClose() {}

        @Override
        public void onMessage(String message) {}

        @Override
        public void onMessage(byte[] message) {}

        @Override
        public void onMessageChunk(String messageChunk, boolean isFinalChunk) {}

        @Override
        public void onMessageChunk(byte[] messageChunk, boolean isFinalChunk) {}

        @Override
        public void onPing() {}

        @Override
        public void onPing(byte[] data) {}

        @Override
        public void onPong() {}

        @Override
        public void onPong(byte[] data) {}
    }
}
