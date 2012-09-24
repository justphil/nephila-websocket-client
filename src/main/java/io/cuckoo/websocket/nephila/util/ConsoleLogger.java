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
