package de.fabmax.pubsub.codec;

/**
 * Created by Max on 24.02.2015.
 */
public class Base64 {

    private static final char[] BASE64_ENC_TAB = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '+', '/'
    };

    private static final int[] BASE64_DEC_TAB = {
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  0,
            0,  0,  0, 62,  0,  0,  0, 63,
            52, 53, 54, 55, 56, 57, 58, 59,
            60, 61,  0,  0,  0, -1,  0,  0,
            0,  0,  1,  2,  3,  4,  5,  6,
            7,  8,  9, 10, 11, 12, 13, 14,
            15, 16, 17, 18, 19, 20, 21, 22,
            23, 24, 25,  0,  0,  0,  0,  0,
            0, 26, 27, 28, 29, 30, 31, 32,
            33, 34, 35, 36, 37, 38, 39, 40,
            41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51,  0,  0,  0,  0,  0
    };

    public static byte[] decode(String base64) {
        int dstLen = base64.length() / 4 * 3;
        if (base64.charAt(base64.length() - 1) == '=') {
            dstLen--;
        }
        if (base64.charAt(base64.length() - 2) == '=') {
            dstLen--;
        }

        byte[] dst = new byte[dstLen];
        for (int i = 0, j = 0; i < base64.length(); i += 4, j += 3) {
            decode3b(base64, i, dst, j);
        }
        return dst;
    }

    public static String encode(byte[] b) {
        char[] buf = new char[4 * ((b.length + 2) / 3)];
        for (int i = 0, bp = 0; i < b.length; i += 3, bp += 4) {
            int len = Math.min(3, b.length - i);
            encode3b(buf, bp, b, i, len);
        }
        return new String(buf);
    }

    private static void decode3b(String src, int srcOff, byte[] dst, int dstOff) {
        int i0 = BASE64_DEC_TAB[src.charAt(srcOff)];
        int i1 = BASE64_DEC_TAB[src.charAt(srcOff + 1)];
        int i2 = BASE64_DEC_TAB[src.charAt(srcOff + 2)];
        int i3 = BASE64_DEC_TAB[src.charAt(srcOff + 3)];

        dst[dstOff] = (byte) ((i0 << 2) | (i1 >> 4));
        if (i2 >= 0) {
            dst[dstOff + 1] = (byte) ((i1 << 4) | (i2 >> 2));
        }
        if (i3 >= 0) {
            dst[dstOff + 2] = (byte) ((i2 << 6) | i3);
        }
    }

    private static void encode3b(char[] buf, int bufPos, byte[] data, int off, int len) {
        int b0 = (data[off] & 0xfc) >> 2;
        int b1 = (data[off] & 0x03) << 4;
        int b2 = -1;
        int b3 = -1;
        if (len > 1) {
            b1 |= (data[off + 1] & 0xf0) >> 4;
            b2  = (data[off + 1] & 0x0f) << 2;
        }
        if (len > 2) {
            b2 |= (data[off + 2] & 0xc0) >> 6;
            b3  = (data[off + 2] & 0x3f);
        }

        buf[bufPos] = BASE64_ENC_TAB[b0];
        buf[bufPos + 1] = BASE64_ENC_TAB[b1];
        buf[bufPos + 2] = b2 >= 0 ? BASE64_ENC_TAB[b2] : '=';
        buf[bufPos + 3] = b3 >= 0 ? BASE64_ENC_TAB[b3] : '=';
    }
}
