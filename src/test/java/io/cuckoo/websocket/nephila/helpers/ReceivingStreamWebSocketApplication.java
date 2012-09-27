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

package io.cuckoo.websocket.nephila.helpers;

import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketApplication;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ReceivingStreamWebSocketApplication extends WebSocketApplication {

    public static final String NEW_TEXT_STREAMING_SEQUENCE_INDICATOR    = "[NEW TEXT STREAMING SEQUENCE]";
    public static final String NEW_BINARY_STREAMING_SEQUENCE_INDICATOR  = "[NEW BINARY STREAMING SEQUENCE]";
    public static final String PATH                                     = "receivingstream";

    private static final ReceivingStreamWebSocketApplication INSTANCE = new ReceivingStreamWebSocketApplication();
    private ReceivingStreamWebSocketApplication() {}

    public static ReceivingStreamWebSocketApplication getInstance() {
        return INSTANCE;
    }

    private final AtomicInteger connectionIdGenerator           = new AtomicInteger(0);
    private final ConcurrentMap<WebSocket, Integer> connections = new ConcurrentHashMap<WebSocket, Integer>();
    private final ConcurrentMap<Integer, String> textData       = new ConcurrentHashMap<Integer, String>();
    private final ConcurrentMap<Integer, byte[]> binaryData     = new ConcurrentHashMap<Integer, byte[]>();


    public String getTextDataByConnectionId(int connectionId) {
        return textData.get(connectionId);
    }

    public byte[] getBinaryDataByConnectionId(int connectionId) {
        return binaryData.get(connectionId);
    }

    @Override
    public boolean isApplicationRequest(HttpRequestPacket httpRequestPacket) {
        String path = "/" + PATH;
        return path.equals(httpRequestPacket.getRequestURI());
    }

    @Override
    public void onConnect(WebSocket socket) {
        if (!connections.containsKey(socket)) {
            int connectionId = connectionIdGenerator.incrementAndGet();
            connections.put(socket, connectionId);
        }
    }

    @Override
    public void onClose(WebSocket socket, DataFrame frame) {
        if (connections.containsKey(socket)) {
            connections.remove(socket);
        }
    }

    @Override
    public void onMessage(WebSocket socket, String text) {
        if (connections.containsKey(socket)) {
            int connectionId = connections.get(socket);
            if (text.startsWith(NEW_TEXT_STREAMING_SEQUENCE_INDICATOR)) {
                textData.remove(connectionId);
                socket.send(String.valueOf(connectionId));
            }
            else if (text.startsWith(NEW_BINARY_STREAMING_SEQUENCE_INDICATOR)) {
                binaryData.remove(connectionId);
                socket.send(String.valueOf(connectionId));
            }
        }
    }

    @Override
    public void onFragment(WebSocket socket, String fragment, boolean last) {
        synchronized (textData) {
            if (connections.containsKey(socket)) {
                int connectionId = connections.get(socket);
                if (textData.containsKey(connectionId)) {
                    String stored = textData.get(connectionId);
                    stored += fragment;
                    textData.replace(connectionId, stored);
                }
                else {
                    textData.put(connectionId, fragment);
                }
            }
        }
    }

    @Override
    public void onFragment(WebSocket socket, byte[] fragment, boolean last) {
        synchronized (binaryData) {
            if (connections.containsKey(socket)) {
                //System.out.print(Arrays.toString(fragment) + " ");
                int connectionId = connections.get(socket);
                if (binaryData.containsKey(connectionId)) {
                    byte[] stored = binaryData.get(connectionId);
                    byte[] newBytes = Arrays.copyOf(stored, stored.length + fragment.length);
                    System.arraycopy(fragment, 0, newBytes, stored.length, fragment.length);
                    binaryData.replace(connectionId, newBytes);
                }
                else {
                    binaryData.put(connectionId, fragment);
                }
            }
        }
    }
}
