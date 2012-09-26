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

import io.cuckoo.websocket.nephila.impl.DefaultWebSocketConfig;
import io.cuckoo.websocket.nephila.util.ConsoleLogger;
import io.cuckoo.websocket.nephila.util.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.websockets.WebSocketAddOn;
import org.glassfish.grizzly.websockets.WebSocketEngine;

import java.io.IOException;

public class WebSocketServer {

    private final static WebSocketServer INSTANCE = new WebSocketServer(8888);

    private final HttpServer server;
    private final Logger log;
    private volatile boolean stop;

    private WebSocketServer(int port) {
        server = HttpServer.createSimpleServer("./", port);
        server.getListener("grizzly").registerAddOn(new WebSocketAddOn());
        EchoWebSocketApplication echoApp                = new EchoWebSocketApplication();
        SubProtocolWebSocketApplication subProtocolApp  = new SubProtocolWebSocketApplication();
        WebSocketEngine.getEngine().register(echoApp);
        WebSocketEngine.getEngine().register(subProtocolApp);
        log = new ConsoleLogger(new DefaultWebSocketConfig());
        stop = false;
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    public static WebSocketServer getInstance() {
        return INSTANCE;
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    public void start() {
        if (server != null) {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        server.start();
                        log.debug(
                                WebSocketServer.this.getClass(),
                                WebSocketServer.this.getClass().getSimpleName() + " is RUNNING!");

                        while(!stop) {
                            Thread.sleep(1000);
                        }

                        server.stop();
                        log.debug(
                                WebSocketServer.this.getClass(),
                                WebSocketServer.this.getClass().getSimpleName() + " is STOPPED!");
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    catch (InterruptedException e) {
                        // ignore
                    }
                }
            };

            Thread runner = new Thread(r, getClass().getSimpleName() + "-Runner");
            runner.start();
        }
    }

    public void stop() {
        stop = true;
    }
}
