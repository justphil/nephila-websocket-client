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

package io.cuckoo.websocket.nephila;

import java.net.URI;
import java.util.List;

public interface WebSocket {
    WebSocketListener getWebSocketListener();
    void setWebSocketListener(WebSocketListener webSocketListener);
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
    void close(String reason) throws WebSocketException;
}
