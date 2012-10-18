package io.cuckoo.websocket.nephila.util;

public class ByteArrayUtils {

    public static byte[] intToByteArray(byte[] output, int value) {
        byte one    = (byte) (value >>> 24);
        byte two    = (byte) (value >>> 16);
        byte three  = (byte) (value >>> 8);
        byte four   = (byte) value;

        if (output != null && output.length == 4) {
            output[0] = one;
            output[1] = two;
            output[2] = three;
            output[3] = four;
            return output;
        }
        else {
            return new byte[] {
                    one, two, three, four
            };
        }
    }

    public static byte[] longToByteArray(byte[] output, long value) {
        byte one    = (byte) (value >>> 56);
        byte two    = (byte) (value >>> 48);
        byte three  = (byte) (value >>> 40);
        byte four   = (byte) (value >>> 32);
        byte five   = (byte) (value >>> 24);
        byte six    = (byte) (value >>> 16);
        byte seven  = (byte) (value >>> 8);
        byte eight  = (byte) value;

        if (output != null && output.length == 8) {
            output[0] = one;
            output[1] = two;
            output[2] = three;
            output[3] = four;
            output[4] = five;
            output[5] = six;
            output[6] = seven;
            output[7] = eight;
            return output;
        }
        else {
            return new byte[] {
                    one, two, three, four,
                    five, six, seven, eight
            };
        }
    }

}
