// X-Gorgon variants — exact port of tiktok-signature/gorgon.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

public final class Gorgon {

    private Gorgon() {}

    private static byte nibbleSwap(byte b) {
        return (byte) (((b >> 4) & 0x0F) | ((b & 0x0F) << 4));
    }

    private static byte bitReverse(byte b) {
        b = (byte) (((b & 0xF0) >> 4) | ((b & 0x0F) << 4));
        b = (byte) (((b & 0xCC) >> 2) | ((b & 0x33) << 2));
        b = (byte) (((b & 0xAA) >> 1) | ((b & 0x55) << 1));
        return b;
    }

    private static String hexOfUnix(long v) {
        String h = Long.toHexString(v);
        if (h.length() > 8) {
            h = h.substring(0, 8);
        } else if (h.length() < 8) {
            StringBuilder sb = new StringBuilder();
            for (int i = h.length(); i < 8; i++) sb.append('0');
            sb.append(h);
            h = sb.toString();
        }
        return h;
    }

    // Returns [reqTicket, khronos, gorgonVal]
    public static String[] v1(String params, String payload, String cookie, long unix, byte b3, byte b7) {
        byte[] gorgon = new byte[20];
        int pos = 0;

        String urlMD5 = CryptoHelpers.md5hex(params);
        byte[] urlBytes = CryptoHelpers.firstDigestBytes(urlMD5, 4);
        System.arraycopy(urlBytes, 0, gorgon, pos, 4);
        pos += 4;

        if (payload != null && !payload.isEmpty()) {
            String dataMD5 = CryptoHelpers.md5hex(payload);
            byte[] dataBytes = CryptoHelpers.firstDigestBytes(dataMD5, 4);
            System.arraycopy(dataBytes, 0, gorgon, pos, 4);
        }
        pos += 4;

        if (cookie != null && !cookie.isEmpty()) {
            String cookieMD5 = CryptoHelpers.md5hex(cookie);
            byte[] cookieBytes = CryptoHelpers.firstDigestBytes(cookieMD5, 4);
            System.arraycopy(cookieBytes, 0, gorgon, pos, 4);
        }
        pos += 4;

        gorgon[pos] = 0x01;
        gorgon[pos + 1] = 0x01;
        gorgon[pos + 2] = 0x02;
        gorgon[pos + 3] = 0x04;
        pos += 4;

        String khronos = hexOfUnix(unix);
        byte[] khBytes = CryptoHelpers.firstDigestBytes(khronos, 4);
        System.arraycopy(khBytes, 0, gorgon, pos, 4);

        byte ce07 = (byte) (b7 & 0xF0);
        byte ce03 = b3;
        byte ce01 = 0x00;
        byte ce06 = 0x00;

        StringBuilder sb = new StringBuilder();
        sb.append("8404");
        sb.append(CryptoHelpers.hexByte(ce07));
        sb.append(CryptoHelpers.hexByte(ce03));
        sb.append(CryptoHelpers.hexByte(ce01));
        sb.append(CryptoHelpers.hexByte(ce06));
        for (byte b : gorgon) {
            sb.append(CryptoHelpers.hexByte(b));
        }

        return new String[]{
            Long.toString(unix * 1000),
            Long.toString(unix),
            sb.toString()
        };
    }

    // Returns [reqTicket, khronos, gorgonVal]
    public static String[] v2(String params, String data, String cookies, long unix) {
        byte[] paramList = new byte[20];
        int pos = 0;

        String paramsMD5 = CryptoHelpers.md5hex(params);
        byte[] pBytes = CryptoHelpers.firstDigestBytes(paramsMD5, 4);
        System.arraycopy(pBytes, 0, paramList, pos, 4);
        pos += 4;

        if (data != null && !data.isEmpty()) {
            String dataMD5 = CryptoHelpers.md5hex(data);
            byte[] dBytes = CryptoHelpers.firstDigestBytes(dataMD5, 4);
            System.arraycopy(dBytes, 0, paramList, pos, 4);
        }
        pos += 4;

        if (cookies != null && !cookies.isEmpty()) {
            String cookieMD5 = CryptoHelpers.md5hex(cookies);
            byte[] cBytes = CryptoHelpers.firstDigestBytes(cookieMD5, 4);
            System.arraycopy(cBytes, 0, paramList, pos, 4);
        }
        pos += 4;

        paramList[pos] = 0x00;
        paramList[pos + 1] = 0x06;
        paramList[pos + 2] = 0x0B;
        paramList[pos + 3] = 0x1C;
        pos += 4;

        long h = unix & 0xFFFFFFFFL;
        paramList[pos]     = (byte) (h >>> 24);
        paramList[pos + 1] = (byte) (h >>> 16);
        paramList[pos + 2] = (byte) (h >>> 8);
        paramList[pos + 3] = (byte) h;

        byte[] key = {
            (byte)0xDF, (byte)0x77, (byte)0xB9, (byte)0x40,
            (byte)0xB9, (byte)0x9B, (byte)0x84, (byte)0x83,
            (byte)0xD1, (byte)0xB9, (byte)0xCB, (byte)0xD1,
            (byte)0xF7, (byte)0xC2, (byte)0xB9, (byte)0x85,
            (byte)0xC3, (byte)0xD0, (byte)0xFB, (byte)0xC3
        };

        int length = 0x14;
        byte[] eor = new byte[length];
        for (int i = 0; i < length; i++) {
            eor[i] = (byte) (paramList[i] ^ key[i]);
        }
        for (int i = 0; i < length; i++) {
            byte c = nibbleSwap(eor[i]);
            byte d = eor[(i + 1) % length];
            byte e = (byte) (c ^ d);
            byte f = bitReverse(e);
            byte hh = (byte) ((f & 0xFF) ^ 0xFF ^ length);
            eor[i] = hh;
        }

        StringBuilder sb = new StringBuilder();
        for (byte p : eor) {
            sb.append(CryptoHelpers.hexByte(p));
        }

        return new String[]{
            Long.toString(unix * 1000),
            Long.toString(unix),
            "840280416000" + sb.toString()
        };
    }

    // Returns [reqTicket, khronos, gorgonVal]
    public static String[] v3(String params, String data, String cookies, long unix) {
        String[] result = v2(params, data, cookies, unix);
        result[2] = "0404b0d30000" + result[2].substring("840280416000".length());
        return result;
    }

    // For tests only.
    static byte nibbleSwapForTest(byte b) { return nibbleSwap(b); }
    static byte bitReverseForTest(byte b) { return bitReverse(b); }
    static String hexOfUnixForTest(long v) { return hexOfUnix(v); }
}
