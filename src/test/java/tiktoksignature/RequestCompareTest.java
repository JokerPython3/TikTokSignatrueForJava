// Request-construction comparison — rebuilds the frozen Example.py inputs
// (testdata/vector_001.json) and proves the query string and body bytes are
// byte-identical to the Python reference (testdata/python_request.json), and
// that all six signatures match the golden values.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RequestCompareTest {

    @Test
    public void testRequestConstructionMatchesPython() throws Exception {
        String raw = new String(Files.readAllBytes(Paths.get("testdata", "python_request.json")), StandardCharsets.UTF_8);
        Map<String, Object> pr = MiniJson.asObject(MiniJson.parse(raw));
        String pyQuery = (String) pr.get("query");
        String pyBody = (String) pr.get("body");
        int pyBodyLen = ((Number) pr.get("body_len")).intValue();
        String pyContentType = (String) pr.get("content_type");

        TestUtil.assertEquals("method", "POST", String.valueOf(pr.get("method")));

        String vectorRaw = new String(Files.readAllBytes(Paths.get("testdata", "vector_001.json")), StandardCharsets.UTF_8);
        Map<String, Object> root = MiniJson.asObject(MiniJson.parse(vectorRaw));
        Map<String, Object> input = MiniJson.asObject(root.get("input"));
        Map<String, Object> random = MiniJson.asObject(root.get("random"));
        Map<String, Object> expected = MiniJson.asObject(root.get("expected"));

        List<Param> params = parse((List<Object>) input.get("params"));
        List<Param> data = parse((List<Object>) input.get("data"));
        List<Object> cookieRaw = (List<Object>) input.get("cookie");
        List<Param> cookie = cookieRaw == null ? null : parse(cookieRaw);

        String gotQuery = UrlEncode.encodeParams(params);
        String gotBody = UrlEncode.encodeParams(data);
        TestUtil.assertEquals("query", pyQuery, gotQuery);
        TestUtil.assertEquals("body", pyBody, gotBody);
        TestUtil.assertEquals("body_len", pyBodyLen, gotBody.length());
        TestUtil.assertEquals("content-type",
                "application/x-www-form-urlencoded; charset=UTF-8", pyContentType);

        SignRequest req = SignRequest.builder().params(params).data(data).cookie(cookie).build();
        Integer aid = MiniJson.optInt(input, "aid");
        Integer lic = MiniJson.optInt(input, "license_id");
        Integer plat = MiniJson.optInt(input, "platform");
        Integer sdkInt = MiniJson.optInt(input, "sdk_version");

        Signatures sig = Signer.sign(req, SignOptions.builder()
                .timestamp(MiniJson.optLong(input, "timestamp"))
                .aid(aid == null ? 0 : aid)
                .licenseID(lic == null ? 0 : lic)
                .platform(plat == null ? 0 : plat)
                .sdkVersion(MiniJson.optString(input, "sdk_version_str"))
                .sdkVersionInt(sdkInt == null ? 0 : sdkInt)
                .secDeviceID(MiniJson.optString(input, "sec_device_id"))
                .gorgonByte3((byte) ((Number) random.get("gorgon_byte3")).intValue())
                .gorgonByte7((byte) ((Number) random.get("gorgon_byte7")).intValue())
                .argusRand(((Number) random.get("argus_rand")).intValue())
                .ladonRandom(hex((String) random.get("ladon_random")))
                .version(((Number) input.get("version")).intValue())
                .build());

        String[] fieldNames = {"X-Argus", "X-Ladon", "X-Gorgon", "X-Khronos", "X-TT-Request-Ticket", "X-SS-Stub"};
        String[] got = {sig.xArgus, sig.xLadon, sig.xGorgon, sig.xKhronos, sig.xTTRequestTicket, sig.xSSStub};
        String[] want = {
            (String) expected.get("x_argus"),
            (String) expected.get("x_ladon"),
            (String) expected.get("x_gorgon"),
            (String) expected.get("x_khronos"),
            (String) expected.get("x_tt_request_ticket"),
            (String) expected.get("x_ss_stub")
        };
        for (int i = 0; i < fieldNames.length; i++) {
            TestUtil.assertEquals(fieldNames[i], want[i], got[i]);
        }
        System.out.println("  request matches Python: method=POST query=" + pyQuery.length()
                + "B body=" + pyBody.length() + "B signatures OK");
    }

    private static List<Param> parse(List<Object> arr) {
        if (arr == null) return null;
        List<Param> out = new ArrayList<>();
        for (Object o : arr) {
            List<Object> kv = (List<Object>) o;
            out.add(new Param((String) kv.get(0), (String) kv.get(1)));
        }
        return out;
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
    }
}
