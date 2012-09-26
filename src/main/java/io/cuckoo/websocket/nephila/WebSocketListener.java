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
