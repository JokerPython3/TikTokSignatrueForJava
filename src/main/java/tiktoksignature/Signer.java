// Main signing entry point — exact port of tiktok-signature/sign.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Signer {

    private Signer() {}

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static Signatures sign(SignRequest req, SignOptions opts) {
        int aid = opts.aid == 0 ? 1233 : opts.aid;
        int licenseID = opts.licenseID == 0 ? 1611921764 : opts.licenseID;
        String sdkVersionStr = (opts.sdkVersion == null || opts.sdkVersion.isEmpty())
                ? "v05.00.06-ov-android" : opts.sdkVersion;
        int sdkVersion = opts.sdkVersionInt == 0 ? 167775296 : opts.sdkVersionInt;

        long unix;
        if (opts.timestamp != null) {
            unix = opts.timestamp;
        } else {
            unix = System.currentTimeMillis() / 1000;
        }

        List<Param> payload = req.payload;
        List<Param> data = req.data;
        if ((data == null || data.isEmpty()) && payload != null && !payload.isEmpty()) {
            data = payload;
        } else if ((payload == null || payload.isEmpty()) && data != null && !data.isEmpty()) {
            payload = data;
        }
        if (data == null) data = new ArrayList<>();
        if (payload == null) payload = new ArrayList<>();

        List<Param> params = req.params;
        if (params == null && req.url != null && !req.url.isEmpty()) {
            params = paramsFromURL(req.url);
        }
        if (params == null) {
            params = new ArrayList<>();
        }

        List<Param> cookie = req.cookie;
        if (cookie == null) cookie = new ArrayList<>();

        String payloadStr = UrlEncode.encodeParams(payload);
        String paramsStr = UrlEncode.encodeParams(params);
        String cookieStr = UrlEncode.encodeParams(cookie);
        String dataStr = UrlEncode.encodeParams(data);
        if (req.rawBody != null && !req.rawBody.isEmpty()) {
            dataStr = req.rawBody;
        }

        String stub = CryptoHelpers.md5hex(dataStr);
        String stubUpper = stub.toUpperCase();

        String reqTicket, khronos, gorgonVal;
        switch (opts.version) {
            case 8404: {
                byte b3 = opts.gorgonByte3 != null ? opts.gorgonByte3 : SECURE_RANDOM_BYTE();
                byte b7 = opts.gorgonByte7 != null ? opts.gorgonByte7 : SECURE_RANDOM_BYTE();
                String[] r = Gorgon.v1(paramsStr, payloadStr, cookieStr, unix, b3, b7);
                reqTicket = r[0]; khronos = r[1]; gorgonVal = r[2];
                break;
            }
            case 8402: {
                String[] r = Gorgon.v2(paramsStr, payloadStr, cookieStr, unix);
                reqTicket = r[0]; khronos = r[1]; gorgonVal = r[2];
                break;
            }
            case 4404:
            case 0: {
                String[] r = Gorgon.v3(paramsStr, payloadStr, cookieStr, unix);
                reqTicket = r[0]; khronos = r[1]; gorgonVal = r[2];
                break;
            }
            default:
                throw new IllegalArgumentException("unsupported gorgon version " + opts.version);
        }

        String secDeviceID = (opts.secDeviceID == null || opts.secDeviceID.isEmpty())
                ? defaultSecDeviceID() : opts.secDeviceID;

        String argusVal = Argus.getSign(paramsStr, stub, unix, aid, licenseID,
                opts.platform, secDeviceID, sdkVersionStr, sdkVersion, opts.argusRand);

        String ladonVal = Ladon.encrypt(unix, licenseID, aid, opts.ladonRandom);

        return new Signatures(argusVal, ladonVal, gorgonVal, khronos, reqTicket, stubUpper);
    }

    private static byte SECURE_RANDOM_BYTE() {
        byte[] b = new byte[1];
        SECURE_RANDOM.nextBytes(b);
        return b[0];
    }

    private static String defaultSecDeviceID() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder("AadCFwpTyztA5j9L");
        byte[] buf = new byte[9];
        SECURE_RANDOM.nextBytes(buf);
        for (byte c : buf) {
            sb.append(chars.charAt((c & 0xFF) % chars.length()));
        }
        return sb.toString();
    }

    static List<Param> paramsFromURL(String url) {
        int qIdx = url.indexOf('?');
        String query = qIdx >= 0 ? url.substring(qIdx + 1) : "";
        List<Param> out = new ArrayList<>();
        if (query.isEmpty()) return out;
        Map<String, Integer> last = new java.util.LinkedHashMap<>();
        for (String p : query.split("&")) {
            int eq = p.indexOf('=');
            String k, v;
            if (eq < 0) {
                k = p;
                v = "";
            } else {
                k = p.substring(0, eq);
                v = p.substring(eq + 1);
            }
            if (last.containsKey(k)) {
                int idx = last.get(k);
                out.set(idx, new Param(k, v));
            } else {
                last.put(k, out.size());
                out.add(new Param(k, v));
            }
        }
        return out;
    }
}
