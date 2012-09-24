package io.cuckoo.websocket.nephila;


public class WebSocketException extends Exception {
	private static final long serialVersionUID = 939250145018159015L;

	public WebSocketException(String message) {
		super(message);
	}

	public WebSocketException(String message, Throwable t) {
		super(message, t);
	}
}
