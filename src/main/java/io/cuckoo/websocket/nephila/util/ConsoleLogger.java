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

package io.cuckoo.websocket.nephila.util;

import io.cuckoo.websocket.nephila.WebSocketConfig;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ConsoleLogger implements Logger {

    // SimpleDateFormat is not thread safe, so create a new object for every thread to avoid synchronization
    private final ThreadLocal<SimpleDateFormat> simpleDateFormat;

    private final WebSocketConfig config;

    public ConsoleLogger(WebSocketConfig config) {
        this.config = config;

        simpleDateFormat = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat(ConsoleLogger.this.config.getDateTimeFormat());
            }
        };
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    @Override
    public void trace(Class<?> clazz, String logMsg) {
        log(Level.TRACE, clazz, logMsg);
    }

    @Override
    public void debug(Class<?> clazz, String logMsg) {
        log(Level.DEBUG, clazz, logMsg);
    }

    @Override
    public void info(Class<?> clazz, String logMsg) {
        log(Level.INFO, clazz, logMsg);
    }

    @Override
    public void warn(Class<?> clazz, String logMsg) {
        log(Level.WARN, clazz, logMsg);
    }

    @Override
    public void error(Class<?> clazz, String logMsg) {
        log(Level.ERROR, clazz, logMsg);
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private void log(Level level, Class<?> clazz, String logMsg) {
        if (config.isDebug()) {
            StringBuilder sb = new StringBuilder(192);
            sb.append("[").append(simpleDateFormat.get().format(new Date())).append("] ");
            sb.append("[").append(Thread.currentThread().getName()).append("] ");
            sb.append("# ").append(level).append(" ");
            sb.append("# ").append(clazz.getSimpleName()).append(" # ");
            sb.append(logMsg);
            if (level.equals(Level.ERROR) || level.equals(Level.WARN)) {
                System.err.println(sb.toString());
            }
            else {
                System.out.println(sb.toString());
            }
        }
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private enum Level {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
