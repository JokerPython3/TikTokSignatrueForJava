// X-Ladon encryption — exact port of tiktok-signature/ladon.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.util.Base64;

public final class Ladon {

    private Ladon() {}

    private static long ror64(long v, int n) {
        return (v >>> n) | (v << (64 - n));
    }

    public static String encrypt(long khronos, int licenseID, int aid, byte[] randomBytes) {
        if (randomBytes == null) {
            randomBytes = new byte[4];
            randomBytes[0] = CryptoHelpers.cryptoRandByte();
            randomBytes[1] = CryptoHelpers.cryptoRandByte();
            randomBytes[2] = CryptoHelpers.cryptoRandByte();
            randomBytes[3] = CryptoHelpers.cryptoRandByte();
        }

        String data = Long.toString(khronos) + "-" + Integer.toString(licenseID) + "-" + Integer.toString(aid);

        byte[] keygen = new byte[4 + Integer.toString(aid).length()];
        System.arraycopy(randomBytes, 0, keygen, 0, 4);
        byte[] aidStr = Integer.toString(aid).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        System.arraycopy(aidStr, 0, keygen, 4, aidStr.length);

        String md5Hex = CryptoHelpers.md5hexFromBytes(keygen);

        int size = data.length();
        int newSize = paddingSize(size);

        byte[] output = new byte[newSize + 4];
        System.arraycopy(randomBytes, 0, output, 0, 4);
        byte[] encrypted = encryptLadon(md5Hex.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                                         data.getBytes(java.nio.charset.StandardCharsets.UTF_8), size);
        System.arraycopy(encrypted, 0, output, 4, newSize);

        return Base64.getEncoder().encodeToString(output);
    }

    static int paddingSize(int size) {
        int mod = size % 16;
        if (mod > 0) {
            return size + (16 - mod);
        }
        return size;
    }

    static byte[] encryptLadon(byte[] md5hex, byte[] data, int size) {
        byte[] hashTable = new byte[272 + 16];
        System.arraycopy(md5hex, 0, hashTable, 0, 32);

        long[] temp = new long[4];
        for (int i = 0; i < 4; i++) {
            temp[i] = CryptoHelpers.le64(hashTable, i * 8);
        }

        long bufferB0 = temp[0];
        long bufferB8 = temp[1];

        // Rebuild temp as a queue: initial 2 elements consumed, then 0x22 elements appended
        // temp[0..1] consumed, then we add elements. In Go: temp starts with 4, then shifts 2, appends 0x22.
        long[] queue = new long[2 + 0x22];
        queue[0] = temp[2];
        queue[1] = temp[3];
        int qHead = 0;

        for (int i = 0; i < 0x22; i++) {
            long x9 = bufferB0;
            long x8 = bufferB8;
            x8 = ror64(x8, 8);
            x8 = x8 + x9;
            x8 = x8 ^ (long) i;
            // Append to queue
            queue[qHead + 2 + i] = x8;
            x8 = x8 ^ ror64(x9, 61);
            CryptoHelpers.writeUint64LE(hashTable, i + 1, x8);
            bufferB0 = x8;
            bufferB8 = queue[qHead + i];
        }

        int newSize = paddingSize(size);
        byte[] input = new byte[newSize];
        System.arraycopy(data, 0, input, 0, Math.min(data.length, size));
        input = pkcs7Pad(input, 0, size, 16);

        byte[] output = new byte[newSize];
        for (int i = 0; i < newSize / 16; i++) {
            byte[] blockOut = encryptLadonInput(hashTable, input, i * 16);
            System.arraycopy(blockOut, 0, output, i * 16, 16);
        }
        return output;
    }

    static byte[] pkcs7Pad(byte[] data, int offset, int size, int blockSize) {
        int padLen = blockSize - size % blockSize;
        byte[] p = new byte[size + padLen];
        System.arraycopy(data, offset, p, 0, size);
        for (int i = size; i < p.length; i++) {
            p[i] = (byte) padLen;
        }
        return p;
    }

    static byte[] encryptLadonInput(byte[] hashTable, byte[] block, int blockOff) {
        long data0 = CryptoHelpers.le64(block, blockOff);
        long data1 = CryptoHelpers.le64(block, blockOff + 8);

        for (int i = 0; i < 0x22; i++) {
            long hash = CryptoHelpers.le64(hashTable, i * 8);
            data1 = hash ^ (data0 + ror64(data1, 8));
            data0 = data1 ^ ror64(data0, 0x3D);
        }

        byte[] out = new byte[16];
        CryptoHelpers.writeUint64LE(out, 0, data0);
        CryptoHelpers.writeUint64LE(out, 1, data1);
        return out;
    }

    // For tests only.
    static int paddingSizeForTest(int size) { return paddingSize(size); }
}
