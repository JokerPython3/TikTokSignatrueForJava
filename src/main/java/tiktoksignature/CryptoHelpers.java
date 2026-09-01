// Cryptographic / random helpers — exact port of tiktok-signature/crypto_helpers.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class CryptoHelpers {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoHelpers() {}

    public static String md5hex(String s) {
        return md5hexFromBytes(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String md5hexFromBytes(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(b);
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] md5Digest(byte[] b) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(b);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] md5Digest(String s) {
        return md5Digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            hex[i * 2] = HEX_DIGITS[(bytes[i] >> 4) & 0x0F];
            hex[i * 2 + 1] = HEX_DIGITS[bytes[i] & 0x0F];
        }
        return new String(hex);
    }

    public static String hexByte(byte b) {
        return new String(new char[]{
            HEX_DIGITS[(b >> 4) & 0x0F],
            HEX_DIGITS[b & 0x0F]
        });
    }

    public static byte[] firstDigestBytes(String digest, int n) {
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            int hi = hexNibble(digest.charAt(2 * i));
            int lo = hexNibble(digest.charAt(2 * i + 1));
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static int hexNibble(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return 0;
    }

    public static byte[] hexDecodeString(String s) {
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((hexNibble(s.charAt(i)) << 4) | hexNibble(s.charAt(i + 1)));
        }
        return out;
    }

    public static byte cryptoRandByte() {
        byte[] b = new byte[1];
        SECURE_RANDOM.nextBytes(b);
        return b[0];
    }

    public static int cryptoRandUint31() {
        byte[] b = new byte[4];
        SECURE_RANDOM.nextBytes(b);
        int v = ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16)
                | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
        return v & 0x7FFFFFFF;
    }

    public static long le64(byte[] b, int off) {
        return (b[off] & 0xFFL)
             | ((b[off + 1] & 0xFFL) << 8)
             | ((b[off + 2] & 0xFFL) << 16)
             | ((b[off + 3] & 0xFFL) << 24)
             | ((b[off + 4] & 0xFFL) << 32)
             | ((b[off + 5] & 0xFFL) << 40)
             | ((b[off + 6] & 0xFFL) << 48)
             | ((b[off + 7] & 0xFFL) << 56);
    }

    public static void writeUint64LE(byte[] dst, int index, long v) {
        int off = index * 8;
        dst[off]     = (byte) v;
        dst[off + 1] = (byte) (v >>> 8);
        dst[off + 2] = (byte) (v >>> 16);
        dst[off + 3] = (byte) (v >>> 24);
        dst[off + 4] = (byte) (v >>> 32);
        dst[off + 5] = (byte) (v >>> 40);
        dst[off + 6] = (byte) (v >>> 48);
        dst[off + 7] = (byte) (v >>> 56);
    }
}
