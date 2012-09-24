package io.cuckoo.websocket.nephila.crypto;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 {

    public static byte[] encode(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		if (text == null) {
            throw new IllegalArgumentException("text is null");
        }

        MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(text.getBytes("ISO-8859-1"), 0, text.length());
        return md.digest();
	}

}
