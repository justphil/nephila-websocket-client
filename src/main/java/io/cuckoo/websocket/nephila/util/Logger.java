package io.cuckoo.websocket.nephila.util;

public interface Logger {
    void trace(Class<?> clazz, String logMsg);
    void debug(Class<?> clazz, String logMsg);
    void info(Class<?> clazz, String logMsg);
    void warn(Class<?> clazz, String logMsg);
    void error(Class<?> clazz, String logMsg);
}
