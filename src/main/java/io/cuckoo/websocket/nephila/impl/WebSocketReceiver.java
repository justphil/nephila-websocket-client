package io.cuckoo.websocket.nephila.impl;

import io.cuckoo.websocket.nephila.WebSocketListener;

import java.io.IOException;
import java.io.InputStream;


public class WebSocketReceiver extends Thread {

    private final DefaultWebSocket  ws;
    private final InputStream       is;
	private final WebSocketListener webSocketListener;
	private volatile boolean        stop;
	private volatile boolean        waitingForServerClosingHandshake;
    private boolean                 inStream;
    private byte                    initialFrameOpCode;

    /*
     * inStream and initialFrameOpCode don't have to be 'volatile'
     * because the WebSocketReceiver exclusively accesses this property
     *
     */

	public WebSocketReceiver(DefaultWebSocket ws, InputStream is) {
        super(WebSocketReceiver.class.getSimpleName() + "-Thread");
        this.ws                             = ws;
        this.is                             = is;
		webSocketListener                   = ws.getWebSocketListener();
        stop                                = false;
        waitingForServerClosingHandshake    = false;
        inStream                            = false;
        initialFrameOpCode                  = -1;
	}

	
	
	/*
	 * WebSocket Base Framing Protocol
	 *
	 *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-------+-+-------------+-------------------------------+
     * |F|R|R|R| opcode|M| Payload len |    Extended payload length    |
     * |I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
     * |N|V|V|V|       |S|             |   (if payload len==126/127)   |
     * | |1|2|3|       |K|             |                               |
     * +-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
     * |     Extended payload length continued, if payload len == 127  |
     * + - - - - - - - - - - - - - - - +-------------------------------+
     * |                               |Masking-key, if MASK set to 1  |
     * +-------------------------------+-------------------------------+
     * | Masking-key (continued)       |          Payload Data         |
     * +-------------------------------- - - - - - - - - - - - - - - - +
     * :                     Payload Data continued ...                :
     * + - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
     * |                     Payload Data continued ...                |
     * +---------------------------------------------------------------+
	 *
	 * Reference: http://tools.ietf.org/html/rfc6455#section-5.2
	 * 
	 */
	
	

	public void run() {
		int b;
		while (!stop) {
			try {
				b = is.read();

                //log.debug(getClass(), "!! Read a byte: " + b);
				if (b == -1) {
					handleEndOfStream();
				}
                else if (b >= 128 && b <= 255) {
                    // FIN bit set to 1 -> unfragmented message / last messages of a streaming sequence
                    handleUnfragmentedMessage(b);
                }
                else if (b >= 0 && b < 128) {
                    // FIN bit set to 0 -> fragmented message
                    handleFragmentedMessage(b);
                }
				else {
                    handleProtocolError("server is not compliant to the websocket protocol");
				}
			}
			catch (IOException ioe) {
				handleError(ioe);
			}
		}

        //log.debug(getClass(), getClass().getSimpleName() + " killed!");
	}
	
	
	public void stopIt() {
		stop = true;
	}

    public void enableWaitingForServerClosingHandshake() {
        waitingForServerClosingHandshake = true;
    }

    /*
     public boolean isRunning() {
         return !stop;
     }
     */
	
	
	private void handleError(Throwable throwable) {
		stopIt();
		ws.handleReceiverError(throwable.getMessage());
	}

    private void handleEndOfStream() {
        handleCloseFrame();
    }

    /**
     * This method is called when a received frame has the FIN bit set to 0.
     * @param b an integer containing the first 8 bits [FIN (1), RSV1 (1), RSV2 (1), RSV3 (1), OP_CODE (4)] of the frame
     */
    private void handleFragmentedMessage(int b) {
        if (b == 0x0) {
            // intermediate continuation frame
            if (inStream) {
                handleContinuationFrame(false);
            }
            else {
                handleProtocolError("received a continuation frame without being in a streaming sequence");
            }
        }
        else if (b == 0x1) {
            // text frame initializing a streaming sequence
            if (inStream) {
                handleProtocolError("not allowed to start a streaming sequence while being in another streaming sequence");
            }
            else {
                inStream = true;
                initialFrameOpCode = 0x1;
                handleTextFrame(false);
            }
        }
        else if (b == 0x2) {
            // binary frame initializing a streaming sequence
            if (inStream) {
                handleProtocolError("not allowed to start a streaming sequence while being in another streaming sequence");
            }
            else {
                inStream = true;
                initialFrameOpCode = 0x2;
                handleBinaryFrame(false);
            }
        }
        else {
            handleProtocolError("fragmented message contains an unsupported op code: " + b);
        }
    }

    /**
     * This method is called when a received frame has the FIN bit set to 1, indicating that it is an unfragmented
     * message or the final chunk of a fragmented message.
     * @param b an integer containing the first 8 bits [FIN (1), RSV1 (1), RSV2 (1), RSV3 (1), OP_CODE (4)] of the frame
     */
    private void handleUnfragmentedMessage(int b) {
        if (b == 0x80) {
            if (inStream) {
                // last continuation frame of a streaming sequence received
                handleContinuationFrame(true);

                // leave streaming mode
                inStream = false;
                initialFrameOpCode = -1;
            }
            else {
                handleProtocolError("received a continuation frame without a preceding initializing streaming frame");
            }
        }
        else if (b == 0x81) {
            if (inStream) {
                handleProtocolError("not allowed to receive unfragmented text frames while receiving a streaming sequence");
            }
            else {
                // op code == %x1 -> text frame
                handleTextFrame(true);
            }
        }
        else if (b == 0x82) {
            if (inStream) {
                handleProtocolError("not allowed to receive unfragmented binary frames while receiving a streaming sequence");
            }
            else {
                // op code == %x2 -> binary frame
                handleBinaryFrame(true);
            }
        }
        /*
         * According to the spec, it is perfectly okay to receive control frames (ping, pong, close)
         * while being in a streaming sequence. Therefore it is not necessary to do the 'inStream' check!
         *
         */
        else if (b == 0x88) {
            // op code == %x8 -> connection close
            if (waitingForServerClosingHandshake) {
                ws.onServerClosingHandshake();
                waitingForServerClosingHandshake = false;
            }
            else {
                // server initiating connection close
                handleCloseFrame();
            }
        }
        else if (b == 0x89) {
            // op code == %x9 -> ping frame
            handlePingFrame();
        }
        else if (b == 0x8A) {
            // op code == %xA -> pong frame
            handlePongFrame();
        }
        else {
            handleProtocolError("unsupported op code: " + (b & 0x7F));
        }
    }

    private void handleContinuationFrame(final boolean isFinalChunk) {
        if (initialFrameOpCode == 0x1) {
            handleTextFrame(isFinalChunk);
        }
        else if (initialFrameOpCode == 0x2) {
            handleBinaryFrame(isFinalChunk);
        }
        else {
            throw new IllegalStateException("being in a streaming sequence an initialFrameOpCode must be either 0x1 or 0x2");
        }
    }
	
	private void handleTextFrame(final boolean isFinalChunk) {
		handlePayload(true, isFinalChunk, PayloadOrigin.DATA_FRAME);
	}

    private void handleBinaryFrame(final boolean isFinalChunk) {
        handlePayload(false, isFinalChunk, PayloadOrigin.DATA_FRAME);
    }

    private void handlePayload(final boolean textFrame, final boolean isFinalChunk, final PayloadOrigin payloadOrigin) {
        //log.debug(getClass(), "### ### ### handlePayload()");
        long payloadSize = 0;

        try {
            int m = is.read();

            if (m >= 128) {
                // payload is masked
                handleProtocolError("server has unexpectedly sent masked data");
            }
            else {
                // payload is NOT masked

                if (m <= 125) {
                    // 7 bit payload length
                    payloadSize = m;
                    //log.debug(getClass(), "handlePayload() # payloadSize -> " + payloadSize);
                }
                else if (m == 126) {
                    // following 2 bytes (16 bit) determine the payload length
                    int p1 = is.read();
                    int p2 = is.read();

                    payloadSize = (p1 << 8) | p2;
                    //log.debug(getClass(), "handlePayload() # payloadSize -> " + payloadSize);
                }
                else if (m == 127) {
                    // following 8 bytes (64 bit) determine the payload length
                    int p1 = is.read();
                    int p2 = is.read();
                    int p3 = is.read();
                    int p4 = is.read();
                    int p5 = is.read();
                    int p6 = is.read();
                    int p7 = is.read();
                    int p8 = is.read();

                    payloadSize = (p1 << 8) | p2;
                    payloadSize = (payloadSize << 8) | p3;
                    payloadSize = (payloadSize << 8) | p4;
                    payloadSize = (payloadSize << 8) | p5;
                    payloadSize = (payloadSize << 8) | p6;
                    payloadSize = (payloadSize << 8) | p7;
                    payloadSize = (payloadSize << 8) | p8;

                    //log.debug(getClass(), "handlePayload() # payloadSize -> " + payloadSize);
                }


                if (payloadSize > Integer.MAX_VALUE) {
                    handleProtocolError("data with payload length > " + Integer.MAX_VALUE + " bytes is not supported yet");
                }
                else {
                    byte[] payload = new byte[ (int) payloadSize];
                    int bytesRead       = 0;
                    int totalBytesRead  = 0;
                    while (totalBytesRead < payloadSize) {
                        bytesRead = is.read(payload, totalBytesRead, payload.length-totalBytesRead);
                        //log.debug(getClass(), "### ### ### bytesRead -> " + bytesRead);
                        if (bytesRead == -1) {
                            break;
                        }
                        else {
                            totalBytesRead += bytesRead;
                        }
                    }

                    //log.debug(getClass(), "### ### ### totalBytesRead -> " + totalBytesRead);
                    //log.debug(getClass(), "### ### ### ### payloadOrigin: " + payloadOrigin + " # isFinalChunk: " + isFinalChunk + " # inStream: " + inStream + " # textFrame: " + textFrame);

                    if (bytesRead == -1) {
                        handleEndOfStream();
                    }
                    else {
                        if (payloadOrigin == PayloadOrigin.PING_FRAME) {
                            if (payload.length > 0) {
                                webSocketListener.onPing(payload);
                            }
                            else {
                                webSocketListener.onPing();
                            }
                        }
                        else if (payloadOrigin == PayloadOrigin.PONG_FRAME) {
                            if (payload.length > 0) {
                                webSocketListener.onPong(payload);
                            }
                            else {
                                webSocketListener.onPong();
                            }
                        }
                        else if (payloadOrigin == PayloadOrigin.DATA_FRAME) {
                            if (inStream) {
                                if (textFrame) {
                                    webSocketListener.onMessageChunk(new String(payload, "UTF-8"), isFinalChunk);
                                }
                                else {
                                    webSocketListener.onMessageChunk(payload, isFinalChunk);
                                }
                            }
                            else {
                                if (textFrame) {
                                    webSocketListener.onMessage(new String(payload, "UTF-8"));
                                }
                                else {
                                    webSocketListener.onMessage(payload);
                                }
                            }
                        }
                        else {
                            throw new RuntimeException("invalid payloadOrigin");
                        }
                    }
                }
            }
            //log.debug(getClass(), "handlePayload() # END");
        }
        catch (IOException ioe) {
            handleError(ioe);
        }
    }

    private void handlePingFrame() {
        handlePayload(false, true, PayloadOrigin.PING_FRAME);
    }

    private void handlePongFrame() {
        handlePayload(false, true, PayloadOrigin.PONG_FRAME);
    }
	
	private void handleCloseFrame() {
		ws.closeSilently();
	}

    private void handleProtocolError(String reason) {
        ws.handleReceiverError(reason);
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private enum PayloadOrigin {
        DATA_FRAME, PING_FRAME, PONG_FRAME
    }
}
