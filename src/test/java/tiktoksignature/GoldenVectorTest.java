// Golden-vector test — loads testdata/vector_*.json and compares the six
// signatures produced by the Java implementation against the expected values
// captured from the Go (and Python SignerPy) reference implementation.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GoldenVectorTest {

    @Test
    public void testGoldenVectors() throws Exception {
        Path dir = Paths.get("testdata");
        List<Path> files = new ArrayList<>();
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().startsWith("vector_")
                    && p.getFileName().toString().endsWith(".json"))
             .sorted().forEach(files::add);
        }
        TestUtil.assertTrue("test vectors found", !files.isEmpty());

        int vectorCount = 0;
        List<String> fieldFailures = new ArrayList<>();
        int[] failureFields = new int[]{0,0,0,0,0,0};

        for (Path file : files) {
            vectorCount++;
            byte[] raw = Files.readAllBytes(file);
            Map<String, Object> root = MiniJson.asObject(MiniJson.parse(new String(raw, StandardCharsets.UTF_8)));
            Map<String, Object> input = MiniJson.asObject(root.get("input"));
            Map<String, Object> random = MiniJson.asObject(root.get("random"));
            Map<String, Object> expected = MiniJson.asObject(root.get("expected"));
            String name = (String) root.get("name");

            List<Param> params = parse((List<Object>) input.get("params"));
            List<Param> data = parse((List<Object>) input.get("data"));
            List<Object> cookieRaw = (List<Object>) input.get("cookie");
            List<Param> cookie = cookieRaw == null ? null : parse(cookieRaw);

            SignRequest req = SignRequest.builder().params(params).data(data).cookie(cookie).build();

            Integer aid = MiniJson.optInt(input, "aid");
            Integer lic = MiniJson.optInt(input, "license_id");
            Integer plat = MiniJson.optInt(input, "platform");
            Integer sdkInt = MiniJson.optInt(input, "sdk_version");
            Long ts = MiniJson.optLong(input, "timestamp");
            Object ver = input.get("version");

            SignOptions.Builder ob = SignOptions.builder()
                    .timestamp(ts)
                    .aid(aid == null ? 0 : aid)
                    .licenseID(lic == null ? 0 : lic)
                    .platform(plat == null ? 0 : plat)
                    .sdkVersion(MiniJson.optString(input, "sdk_version_str"))
                    .sdkVersionInt(sdkInt == null ? 0 : sdkInt)
                    .secDeviceID(MiniJson.optString(input, "sec_device_id"))
                    .gorgonByte3((byte) ((Number) random.get("gorgon_byte3")).intValue())
                    .gorgonByte7((byte) ((Number) random.get("gorgon_byte7")).intValue())
                    .argusRand(((Number) random.get("argus_rand")).intValue())
                    .ladonRandom(hex((String) random.get("ladon_random")));
            if (ver != null) {
                ob.version(((Number) ver).intValue());
            } else {
                ob.version(0);
            }

            Signatures sig = Signer.sign(req, ob.build());

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
                if (!got[i].equals(want[i])) {
                    failureFields[i]++;
                    fieldFailures.add(name + " [" + fieldNames[i] + "]");
                }
            }
        }

        StringBuilder report = new StringBuilder();
        boolean anyFailure = false;
        String[] fieldNames = {"X-Argus", "X-Ladon", "X-Gorgon", "X-Khronos", "X-TT-Request-Ticket", "X-SS-Stub"};
        for (int i = 0; i < 6; i++) {
            report.append(String.format("%-22s: %d/%d passed%n", fieldNames[i], vectorCount - failureFields[i], vectorCount));
            if (failureFields[i] > 0) anyFailure = true;
        }
        if (anyFailure) {
            throw new AssertionError("field failures; " + vectorCount + " vectors:\n" + report
                    + "Failures:\n  " + String.join("\n  ", fieldFailures));
        }
        System.out.println("  " + vectorCount + " vectors -> " + report.toString().replace("\n", "\n  "));
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
