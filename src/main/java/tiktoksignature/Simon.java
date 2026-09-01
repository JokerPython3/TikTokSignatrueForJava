// Simon 64/256 cipher — exact port of SignerPy.simon / tiktok-signature/simon.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

public final class Simon {

    private Simon() {}

    private static long rol64(long v, int n) {
        return (v << n) | (v >>> (64 - n));
    }

    private static long ror64(long v, int n) {
        return (v >>> n) | (v << (64 - n));
    }

    private static long getBit(long val, int pos) {
        return (val >>> pos) & 1L;
    }

    private static final long Z = 0x3DC94C3A046D678BL;

    private static void keyExpansion(long[] key) {
        for (int i = 4; i < 72; i++) {
            long tmp = ror64(key[i - 1], 3);
            tmp ^= key[i - 3];
            tmp ^= ror64(tmp, 1);
            key[i] = (~key[i - 4]) ^ tmp ^ getBit(Z, (i - 4) % 62) ^ 3L;
        }
    }

    public static long[] encrypt(long[] pt, long[] k) {
        long[] key = new long[72];
        System.arraycopy(k, 0, key, 0, 4);
        keyExpansion(key);

        long x = pt[0];
        long y = pt[1];
        for (int i = 0; i < 72; i++) {
            long tmp = y;
            long f = rol64(y, 1) & rol64(y, 8);
            y = x ^ f ^ rol64(y, 2) ^ key[i];
            x = tmp;
        }
        return new long[]{x, y};
    }
}
