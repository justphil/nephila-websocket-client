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


import io.cuckoo.websocket.nephila.WebSocketException;
import io.cuckoo.websocket.nephila.crypto.Base64;
import io.cuckoo.websocket.nephila.crypto.SHA1;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;


public class WebSocketHandshake {	
	private final String key;
	private final byte[] expectedSecWebSocketAcceptValue;
	
	private final URI url;

	private static final String CRLF = "\r\n";
	private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";


	
	public WebSocketHandshake(URI url) throws WebSocketException {
		this.url = url;
		KeyGenerationResult keyGenerationResult = generateKeys();
        this.key = keyGenerationResult.getKey();
        this.expectedSecWebSocketAcceptValue = keyGenerationResult.getExpectedSecWebSocketAcceptValue();
	}

	public byte[] getHandshakeBytes(String acceptingSubProtocolsCSV) throws WebSocketException {
		String path = url.getPath();
		String host = url.getHost();
		//origin = "http://" + host;
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("GET ").append(path).append(" HTTP/1.1")			.append(CRLF);
		sb.append("Host: ").append(host)							.append(CRLF);
		sb.append("Upgrade: websocket")								.append(CRLF);
		sb.append("Connection: Upgrade")							.append(CRLF);
		sb.append("Sec-WebSocket-Key: ").append(key)				.append(CRLF);
		
		if (acceptingSubProtocolsCSV != null ) {
			sb.append("Sec-WebSocket-Protocol: ").append(acceptingSubProtocolsCSV).append(CRLF);
		}
		
		sb.append("Sec-WebSocket-Version: 13")						.append(CRLF);
		sb.append(CRLF);
				
		try {
			return sb.toString().getBytes("ISO-8859-1");
		}
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("iso-8859-1 is not supported on this platform");
		}
	}
	
	
	public void verifySecWebSocketAccept(String secWebSocketAccept) throws WebSocketException {
		String[] segs = secWebSocketAccept.split( Pattern.quote( ":" ) );
		
		if (segs.length != 2) {
            throw new WebSocketException("invalid server opening handshake: bad 'sec-websocket-accept' header");
		}
		
		String serverKey = segs[1].trim();

        try {
            String expectedSecWebSocketAcceptValueString = new String(expectedSecWebSocketAcceptValue, "ISO-8859-1");
            if (!expectedSecWebSocketAcceptValueString.equals(serverKey)) {
                throw new WebSocketException(
                        "invalid server opening handshake:" +
                                " unexpected 'sec-websocket-accept' header value (got: " + serverKey + " " +
                                "expected: " + expectedSecWebSocketAcceptValueString + ")");
            }
        }
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
        }
	}

    public List<String> negotiateSubProtocols(String[] acceptingSubProtocols, String secWebSocketProtocol) throws WebSocketException {
        List<String> negotiatedSubProtocols = new ArrayList<String>();

        if (secWebSocketProtocol == null) {
            if (acceptingSubProtocols.length > 0) {
                throw new WebSocketException("server cannot talk any of the sub protocols the client has provided: "
                        + Arrays.toString(acceptingSubProtocols));
            }
        }
        else {
            String[] segs = secWebSocketProtocol.split( Pattern.quote( ":" ) );
            if (segs.length != 2) {
                throw new WebSocketException("invalid server opening handshake: bad 'sec-websocket-protocol' header");
            }

            String secWebSocketProtocolValue = segs[1].trim();
            if (!secWebSocketProtocolValue.isEmpty() && acceptingSubProtocols.length == 0) {
                throw new WebSocketException("cannot talk any of the sub protocols the server has provided: "
                        + secWebSocketProtocolValue);
            }
            else if (secWebSocketProtocolValue.isEmpty() && acceptingSubProtocols.length > 0) {
                throw new WebSocketException("server cannot talk any of the sub protocols the client has provided: "
                        + Arrays.toString(acceptingSubProtocols));
            }
            else {
                segs = secWebSocketProtocolValue.split( Pattern.quote( "," ) );
                String trimmedServerSubProtocol;
                for (String serverSubProtocol : segs) {
                    trimmedServerSubProtocol = serverSubProtocol.trim();
                    if (arrayContains(acceptingSubProtocols, trimmedServerSubProtocol)) {
                        negotiatedSubProtocols.add(trimmedServerSubProtocol);
                    }
                }

                if (negotiatedSubProtocols.isEmpty()) {
                    throw new WebSocketException("couldn't find a sub protocol that both parties are speaking");
                }
            }
        }


        return negotiatedSubProtocols;
    }


	
	public void verifyServerStatusLine(String statusLine) throws WebSocketException {
		int statusCode = Integer.valueOf(statusLine.substring(9, 12));
		
		if (statusCode == 407) {
			throw new WebSocketException("connection failed: proxy authentication not supported");
		}
		else if (statusCode == 404) {
			throw new WebSocketException("connection failed: 404 not found");
		}
		else if (statusCode != 101) {
			throw new WebSocketException("connection failed: unknown status code " + statusCode);
		}
	}
	
	
	public void verifyServerHandshakeHeaders(HashMap<String, String> headers) throws WebSocketException {
		
		String upgradeKey;
		if (headers.containsKey("Upgrade"))
			upgradeKey = "Upgrade";
		else if (headers.containsKey("upgrade"))
			upgradeKey = "upgrade";
		else
			throw new WebSocketException("connection failed: missing header field in server opening handshake: Upgrade");
		
		if (!headers.get(upgradeKey).equalsIgnoreCase("websocket")) {
			throw new WebSocketException("connection failed: 'Upgrade' header in server opening handshake does not match 'websocket'");
		}
		
		String connectionKey;
		if (headers.containsKey("Connection"))
			connectionKey = "Connection";
		else if (headers.containsKey("connection"))
			connectionKey = "connection";
		else
			throw new WebSocketException("connection failed: missing header field in server opening handshake: Connection");
		
		if (!headers.get(connectionKey).equalsIgnoreCase("upgrade")) {
			throw new WebSocketException("connection failed: 'Connection' header in server opening handshake does not match 'Upgrade'");
		}
		
		/* Browsers should also check the origin policy! But, we aren't a browser.
		else if (!headers.get("Sec-WebSocket-Origin").equals(origin)) {
			throw new WebSocketException("connection failed: missing header field in server handshake: Sec-WebSocket-Origin");
		}
		*/
	}
	
	private KeyGenerationResult generateKeys() throws WebSocketException {
		try {
            final long time = System.nanoTime();
			final String key = Base64.encodeToString(String.valueOf(time).getBytes("UTF-8"), false);
            final String exp = key + GUID;
            byte[] sha1Hash = SHA1.encode(exp);
            byte[] expectedSecWebSocketAcceptValue = Base64.encodeToByte(sha1Hash, false);

            return new KeyGenerationResult(key, expectedSecWebSocketAcceptValue);
		}
        catch (UnsupportedEncodingException e) {
            throw new WebSocketException("utf-8 is not supported on this platform");
		}
        catch (NoSuchAlgorithmException e) {
            throw new WebSocketException("sha-1 is not supported on this platform");
        }
	}

    private <T> boolean arrayContains(T[] array, T object) {
        for(T elem : array) {
            if (elem.equals(object)) {
                return true;
            }
        }

        return false;
    }

    /* ######################################################################## */
    /* ######################################################################## */
    /* ######################################################################## */

    private class KeyGenerationResult {
        private final String key;
        private final byte[] expectedSecWebSocketAcceptValue;

        private KeyGenerationResult(String key, byte[] expectedSecWebSocketAcceptValue) {
            this.key = key;
            this.expectedSecWebSocketAcceptValue = expectedSecWebSocketAcceptValue;
        }

        public String getKey() {
            return key;
        }

        public byte[] getExpectedSecWebSocketAcceptValue() {
            return expectedSecWebSocketAcceptValue;
        }
    }
}
