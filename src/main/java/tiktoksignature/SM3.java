// SM3 hash function — exact port of SignerPy.sm3 / tiktok-signature/sm3.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class SM3 {

    private static final int[] IV = {
        0x7380166F, 0x4914B2B9, 0x172442D7, 0xDA8A0600,
        0xA96F30BC, 0x163138AA, 0xE38DEE4D, 0xB0FB0E4E
    };

    private static final int[] TJ = new int[64];
    static {
        for (int i = 0; i < 16; i++) TJ[i] = 0x79CC4519;
        for (int i = 16; i < 64; i++) TJ[i] = 0x7A879D8A;
    }

    private SM3() {}

    private static int rotl(int a, int k) {
        k %= 32;
        if (k == 0) return a;
        return (a << k) | (a >>> (32 - k));
    }

    private static int ff(int x, int y, int z, int j) {
        if (j < 16) return x ^ y ^ z;
        return (x & y) | (x & z) | (y & z);
    }

    private static int gg(int x, int y, int z, int j) {
        if (j < 16) return x ^ y ^ z;
        return (x & y) | ((~x) & z);
    }

    private static int p0(int x) {
        return x ^ rotl(x, 9) ^ rotl(x, 17);
    }

    private static int p1(int x) {
        return x ^ rotl(x, 15) ^ rotl(x, 23);
    }

    private static int[] cf(int[] v, byte[] b) {
        int[] w = new int[68];
        ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN);
        for (int i = 0; i < 16; i++) {
            w[i] = buf.getInt();
        }
        for (int j = 16; j < 68; j++) {
            w[j] = p1(w[j - 16] ^ w[j - 9] ^ rotl(w[j - 3], 15))
                    ^ rotl(w[j - 13], 7) ^ w[j - 6];
        }
        int[] w1 = new int[64];
        for (int j = 0; j < 64; j++) {
            w1[j] = w[j] ^ w[j + 4];
        }

        int a = v[0], b0 = v[1], c = v[2], d = v[3];
        int e = v[4], f = v[5], g = v[6], h = v[7];

        for (int j = 0; j < 64; j++) {
            int ss1 = rotl(rotl(a, 12) + e + rotl(TJ[j], j), 7);
            int ss2 = ss1 ^ rotl(a, 12);
            int tt1 = ff(a, b0, c, j) + d + ss2 + w1[j];
            int tt2 = gg(e, f, g, j) + h + ss1 + w[j];
            d = c;
            c = rotl(b0, 9);
            b0 = a;
            a = tt1;
            h = g;
            g = rotl(f, 19);
            f = e;
            e = p0(tt2);
        }

        return new int[]{
            a ^ v[0], b0 ^ v[1], c ^ v[2], d ^ v[3],
            e ^ v[4], f ^ v[5], g ^ v[6], h ^ v[7]
        };
    }

    public static byte[] hash(byte[] msg) {
        byte[] m = new byte[msg.length + 128];
        System.arraycopy(msg, 0, m, 0, msg.length);
        int mlen = msg.length;
        m[mlen] = (byte) 0x80;

        int reserve = mlen % 64 + 1;
        int rangeEnd = 56;
        if (reserve > rangeEnd) {
            rangeEnd += 64;
        }
        int padStart = mlen + 1;
        for (int i = padStart; i < padStart + (rangeEnd - reserve); i++) {
            m[i] = 0x00;
        }
        int totalLen = padStart + (rangeEnd - reserve);

        long bitLength = (long) mlen * 8;
        for (int i = 7; i >= 0; i--) {
            m[totalLen++] = (byte) (bitLength >> (i * 8));
        }

        int[] v = IV.clone();
        int blocks = totalLen / 64;
        for (int i = 0; i < blocks; i++) {
            byte[] block = new byte[64];
            System.arraycopy(m, i * 64, block, 0, 64);
            v = cf(v, block);
        }

        byte[] out = new byte[32];
        ByteBuffer buf = ByteBuffer.wrap(out).order(ByteOrder.BIG_ENDIAN);
        for (int word : v) {
            buf.putInt(word);
        }
        return out;
    }

    public static byte[] hash(String msg) {
        return hash(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
