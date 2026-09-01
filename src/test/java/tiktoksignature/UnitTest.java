// Unit tests for individual primitives (SM3, Simon, ProtoBuf varint, URL
// encoding, Gorgon helpers). Assertions through TestUtil.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class UnitTest {

    @Test
    public void testSM3Vectors() {
        expectHex("empty", SM3.hash(new byte[0]),
                "1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b");
        expectHex("abc", SM3.hash("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
        byte[] zero16 = new byte[16];
        expectHex("16-zero", SM3.hash(zero16),
                "106e34a2b8c7bb13156cfdd0d91379dcc47543dcf9787c68ae5eb582620ae6e8");
        expectHex("len57", SM3.hash(bytesOf((byte)'x', 57)),
                "6ea239cae3a9660ef7db6a72345e04b9bb572d0269bc3c6528faa6af3821397e");
        expectHex("len120", SM3.hash(bytesOf((byte)'y', 120)),
                "8b7b1040722bb5b75d2486ba610d905e8452789f2295870957ad7779c1282631");
    }

    @Test
    public void testSimonEnc() {
        long[] key = {
            0x0001020304050607L, 0x08090a0b0c0d0e0fL,
            0x1011121314151617L, 0x18191a1b1c1d1e1fL
        };
        long[] pt = {0x626b7373656d7369L, 0x656d6361756c6f70L};
        long[] got = Simon.encrypt(pt, key);
        TestUtil.assertEquals("simon[0]", -7893652479812039344L, got[0]);
        TestUtil.assertEquals("simon[1]", 3305761749203649550L, got[1]);
    }

    @Test
    public void testSm3SignKeyConstant() {
        byte[] signKey = Argus.SIGN_KEY_FOR_TEST;
        byte[] suffix = new byte[signKey.length + 4 + signKey.length];
        System.arraycopy(signKey, 0, suffix, 0, signKey.length);
        suffix[signKey.length] = (byte) 0xf2;
        suffix[signKey.length + 1] = (byte) 0x81;
        suffix[signKey.length + 2] = 'a';
        suffix[signKey.length + 3] = 'o';
        System.arraycopy(signKey, 0, suffix, signKey.length + 4, signKey.length);
        String got = CryptoHelpers.bytesToHex(SM3.hash(suffix));
        String want = CryptoHelpers.bytesToHex(Argus.SM3_OUTPUT_FOR_TEST);
        TestUtil.assertEquals("SM3 sign key constant", want, got);
    }

    @Test
    public void testGorgonHelpers() {
        TestUtil.assertEquals("nibbleSwap(0x12)", 0x21, Gorgon.nibbleSwapForTest((byte) 0x12) & 0xFF);
        TestUtil.assertEquals("bitReverse(0x80)", 0x01, Gorgon.bitReverseForTest((byte) 0x80) & 0xFF);
        TestUtil.assertEquals("hexOfUnix(100000000)", "05f5e100", Gorgon.hexOfUnixForTest(100000000L));
        TestUtil.assertEquals("hexOfUnix(1700000000)", "6553f100", Gorgon.hexOfUnixForTest(1700000000L));
    }

    @Test
    public void testQuotePlusNonAscii() {
        TestUtil.assertEquals("empty", "", UrlEncode.quotePlus(""));
        TestUtil.assertEquals("safe", "abc123_.-~", UrlEncode.quotePlus("abc123_.-~"));
        TestUtil.assertEquals("spaces", "a+b+c", UrlEncode.quotePlus("a b c"));
        TestUtil.assertEquals("plus", "a%2Bb", UrlEncode.quotePlus("a+b"));
        TestUtil.assertEquals("reserved", "x%2Fy%3F%26%3D", UrlEncode.quotePlus("x/y?&="));
        TestUtil.assertEquals("eacute", "%C3%A9", UrlEncode.quotePlus("é"));
        TestUtil.assertEquals("chinese", "%E6%B5%8B%E8%AF%95", UrlEncode.quotePlus("测试"));
    }

    @Test
    public void testUnquotePlusRoundTrip() {
        String[] inputs = {
            "WayDroid x86_64 Device",
            "usér+plus@example.com",
            "a b/c?d=e&f%g",
            "测试",
            ""
        };
        for (String in : inputs) {
            String enc = UrlEncode.quotePlus(in);
            TestUtil.assertEquals("round trip: " + in, in, UrlEncode.unquotePlus(enc));
        }
    }

    @Test
    public void testParseQueryParams() {
        java.util.Map<String, String> got = UrlEncode.parseQueryParams("device_id=7401&version_name=41.9.3&empty=&x=a+b%2Fc");
        TestUtil.assertEquals("device_id", "7401", got.get("device_id"));
        TestUtil.assertEquals("version_name", "41.9.3", got.get("version_name"));
        TestUtil.assertTrue("blank value dropped", !got.containsKey("empty"));
        TestUtil.assertEquals("x", "a b/c", got.get("x"));
    }

    @Test
    public void testVarintQuirk() {
        ByteArrayOutputStream w = new ByteArrayOutputStream();
        ProtoBuf.writeVarint(w, 150);
        TestUtil.assertBytes("writeVarint(150)", new byte[]{(byte) 0x96, 0x01}, w.toByteArray());

        ByteArrayOutputStream w2 = new ByteArrayOutputStream();
        ProtoBuf.writeVarint(w2, 0x80);
        TestUtil.assertBytes("writeVarint(0x80) quirk -> [0x00]", new byte[]{0x00}, w2.toByteArray());
    }

    @Test
    public void testLadonPaddingSize() {
        TestUtil.assertEquals("pad 0", 0, Ladon.paddingSizeForTest(0));
        TestUtil.assertEquals("pad 15", 16, Ladon.paddingSizeForTest(15));
        TestUtil.assertEquals("pad 16", 16, Ladon.paddingSizeForTest(16));
        TestUtil.assertEquals("pad 17", 32, Ladon.paddingSizeForTest(17));
    }

    private static void expectHex(String name, byte[] in, String want) {
        TestUtil.assertEquals("SM3(" + name + ")", want, CryptoHelpers.bytesToHex(in));
    }

    private static byte[] bytesOf(byte b, int n) {
        byte[] out = new byte[n];
        Arrays.fill(out, b);
        return out;
    }
}
