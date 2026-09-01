// X-Argus encryption — exact port of tiktok-signature/argus.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Argus {

    private Argus() {}

    private static final byte[] SIGN_KEY = {
        (byte)0xac, (byte)0x1a, (byte)0xda, (byte)0xae,
        (byte)0x95, (byte)0xa7, (byte)0xaf, (byte)0x94,
        (byte)0xa5, (byte)0x11, (byte)0x4a, (byte)0xb3,
        (byte)0xb3, (byte)0xa9, (byte)0x7d, (byte)0xd8,
        (byte)0x00, (byte)0x50, (byte)0xaa, (byte)0x0a,
        (byte)0x39, (byte)0x31, (byte)0x4c, (byte)0x40,
        (byte)0x52, (byte)0x8c, (byte)0xae, (byte)0xc9,
        (byte)0x52, (byte)0x56, (byte)0xc2, (byte)0x8c
    };

    private static final byte[] SM3_OUTPUT = {
        (byte)0xfc, (byte)0x78, (byte)0xe0, (byte)0xa9,
        (byte)0x65, (byte)0x7a, (byte)0x0c, (byte)0x74,
        (byte)0x8c, (byte)0xe5, (byte)0x15, (byte)0x59,
        (byte)0x90, (byte)0x3c, (byte)0xcf, (byte)0x03,
        (byte)0x51, (byte)0x0e, (byte)0x51, (byte)0xd3,
        (byte)0xcf, (byte)0xf2, (byte)0x32, (byte)0xd7,
        (byte)0x13, (byte)0x43, (byte)0xe8, (byte)0x8a,
        (byte)0x32, (byte)0x1c, (byte)0x53, (byte)0x04
    };

    static byte[] bodyHash(String stub) {
        if (stub == null || stub.isEmpty()) {
            byte[] zero = new byte[16];
            byte[] h = SM3.hash(zero);
            byte[] out = new byte[6];
            System.arraycopy(h, 0, out, 0, 6);
            return out;
        }
        byte[] b;
        try {
            b = CryptoHelpers.hexDecodeString(stub);
        } catch (Exception e) {
            b = new byte[0];
        }
        byte[] h = SM3.hash(b);
        byte[] out = new byte[6];
        System.arraycopy(h, 0, out, 0, 6);
        return out;
    }

    static byte[] queryHash(String query) {
        if (query == null || query.isEmpty()) {
            byte[] zero = new byte[16];
            byte[] h = SM3.hash(zero);
            byte[] out = new byte[6];
            System.arraycopy(h, 0, out, 0, 6);
            return out;
        }
        byte[] h = SM3.hash(query);
        byte[] out = new byte[6];
        System.arraycopy(h, 0, out, 0, 6);
        return out;
    }

    static byte[] encryptEncPb(byte[] data, int l) {
        byte[] d = data.clone();
        byte[] xorArray = new byte[8];
        System.arraycopy(d, 0, xorArray, 0, 8);
        for (int i = 8; i < l; i++) {
            d[i] ^= xorArray[i % 8];
        }
        for (int i = 0, j = d.length - 1; i < j; i++, j--) {
            byte tmp = d[i];
            d[i] = d[j];
            d[j] = tmp;
        }
        return d;
    }

    static byte[] pkcs7Pad(byte[] data, int blockSize) {
        int padLen = blockSize - data.length % blockSize;
        byte[] p = new byte[data.length + padLen];
        System.arraycopy(data, 0, p, 0, data.length);
        for (int i = data.length; i < p.length; i++) {
            p[i] = (byte) padLen;
        }
        return p;
    }

    static String encrypt(byte[] bean) {
        try {
            byte[] protobuf = pkcs7Pad(bean, 16);
            int newLen = protobuf.length;

            byte[] key = new byte[32];
            System.arraycopy(SM3_OUTPUT, 0, key, 0, 32);
            long[] keyList = new long[4];
            for (int i = 0; i < 4; i++) {
                keyList[i] = CryptoHelpers.le64(key, i * 8);
            }

            byte[] encPB = new byte[newLen];
            int encPBPos = 0;
            for (int i = 0; i < newLen / 16; i++) {
                byte[] block = new byte[16];
                System.arraycopy(protobuf, i * 16, block, 0, 16);
                long[] pt = {CryptoHelpers.le64(block, 0), CryptoHelpers.le64(block, 8)};
                long[] ct = Simon.encrypt(pt, keyList);
                byte[] ctBytes = new byte[16];
                CryptoHelpers.writeUint64LE(ctBytes, 0, ct[0]);
                CryptoHelpers.writeUint64LE(ctBytes, 1, ct[1]);
                System.arraycopy(ctBytes, 0, encPB, encPBPos, 16);
                encPBPos += 16;
            }

            byte[] prefix1 = {(byte)0xf2, (byte)0xf7, (byte)0xfc, (byte)0xff,
                              (byte)0xf2, (byte)0xf7, (byte)0xfc, (byte)0xff};
            byte[] buffer = new byte[8 + newLen];
            System.arraycopy(prefix1, 0, buffer, 0, 8);
            System.arraycopy(encPB, 0, buffer, 8, newLen);
            buffer = encryptEncPb(buffer, newLen + 8);

            byte[] prefix2 = {(byte)0xa6, (byte)0x6e, (byte)0xad, (byte)0x9f,
                              (byte)0x77, (byte)0x01, (byte)0xd0, (byte)0x0c, (byte)0x18};
            byte[] withPrefix = new byte[prefix2.length + buffer.length];
            System.arraycopy(prefix2, 0, withPrefix, 0, prefix2.length);
            System.arraycopy(buffer, 0, withPrefix, prefix2.length, buffer.length);

            byte[] withSuffix = new byte[withPrefix.length + 2];
            System.arraycopy(withPrefix, 0, withSuffix, 0, withPrefix.length);
            withSuffix[withPrefix.length] = 'a';
            withSuffix[withPrefix.length + 1] = 'o';

            byte[] keyMD5 = CryptoHelpers.md5Digest(java.util.Arrays.copyOfRange(SIGN_KEY, 0, 16));
            byte[] ivMD5 = CryptoHelpers.md5Digest(java.util.Arrays.copyOfRange(SIGN_KEY, 16, 32));

            SecretKeySpec secretKey = new SecretKeySpec(keyMD5, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivMD5);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
            byte[] ct = cipher.doFinal(withSuffix);

            byte[] result = new byte[2 + ct.length];
            result[0] = (byte) 0xf2;
            result[1] = (byte) 0x81;
            System.arraycopy(ct, 0, result, 2, ct.length);
            return java.util.Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getSign(String queryhash, String stub, long timestamp,
                                  int aid, int licenseID, int platform,
                                  String secDeviceID, String sdkVersion,
                                  int sdkVersionInt, Integer randVal) {
        Map<String, String> qdict = UrlEncode.parseQueryParams(queryhash);
        String deviceID = qdict.getOrDefault("device_id", "");
        String versionName = qdict.getOrDefault("version_name", "");

        int randInt;
        if (randVal != null) {
            randInt = randVal;
        } else {
            randInt = CryptoHelpers.cryptoRandUint31();
        }

        List<ProtoBuf.Field> bean = new ArrayList<>();
        bean.add(ProtoBuf.Field.varint(1, 0x20200929L << 1));
        bean.add(ProtoBuf.Field.varint(2, 2));
        bean.add(ProtoBuf.Field.varint(3, (long) randInt & 0x7FFFFFFFL));
        bean.add(ProtoBuf.Field.string(4, intToStr(aid).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.string(5, deviceID.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.string(6, intToStr(licenseID).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.string(7, versionName.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.string(8, sdkVersion.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.varint(9, (long) sdkVersionInt));
        bean.add(ProtoBuf.Field.string(10, new byte[8]));
        bean.add(ProtoBuf.Field.varint(11, (long) platform));
        bean.add(ProtoBuf.Field.varint(12, timestamp << 1));
        bean.add(ProtoBuf.Field.string(13, bodyHash(stub)));
        bean.add(ProtoBuf.Field.string(14, queryHash(queryhash)));

        List<ProtoBuf.Field> inner15 = new ArrayList<>();
        inner15.add(ProtoBuf.Field.varint(1, 1));
        inner15.add(ProtoBuf.Field.varint(2, 1));
        inner15.add(ProtoBuf.Field.varint(3, 1));
        inner15.add(ProtoBuf.Field.varint(7, 3348294860L));
        bean.add(ProtoBuf.Field.string(15, ProtoBuf.serialize(inner15)));

        bean.add(ProtoBuf.Field.string(16, secDeviceID.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.string(20, "none".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        bean.add(ProtoBuf.Field.varint(21, 738));

        List<ProtoBuf.Field> inner23 = new ArrayList<>();
        inner23.add(ProtoBuf.Field.string(1, "NX551J".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        inner23.add(ProtoBuf.Field.varint(2, 8196));
        inner23.add(ProtoBuf.Field.varint(4, 2162219008L));
        bean.add(ProtoBuf.Field.string(23, ProtoBuf.serialize(inner23)));

        bean.add(ProtoBuf.Field.varint(25, 2));

        return encrypt(ProtoBuf.serialize(bean));
    }

    private static String intToStr(int v) {
        return Integer.toString(v);
    }

    // For tests only.
    static final byte[] SIGN_KEY_FOR_TEST = SIGN_KEY;
    static final byte[] SM3_OUTPUT_FOR_TEST = SM3_OUTPUT;
}
