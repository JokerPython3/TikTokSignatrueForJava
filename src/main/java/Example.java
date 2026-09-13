// Example.java is a Java port of Example.go (itself a port of Example.py): it
// builds a signed TikTok request and prints the six generated signing headers.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
import tiktoksignature.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Example {

    static final String TARGET_URL = "https://api16-normal-c-alisg.tiktokv.com/passport/email/send_code/";

    static boolean debugEnabled() {
        String d = System.getenv("DEBUG");
        if (d == null) return false;
        d = d.toLowerCase();
        return d.equals("1") || d.equals("true") || d.equals("yes") || d.equals("on");
    }

    static void debugf(String format, Object... a) {
        if (debugEnabled()) System.out.printf(format, a);
    }

    static String xor(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            sb.append(String.format("%x", s.charAt(i) ^ 5));
        }
        return sb.toString();
    }

    static long randInt64Range(long min, long max) {
        SecureRandom rnd = new SecureRandom();
        long v = rnd.nextLong();
        if (v < 0) v = -v;
        return min + v % (max - min + 1);
    }

    static String randomHexBytes(int n) {
        SecureRandom rnd = new SecureRandom();
        byte[] b = new byte[n];
        rnd.nextBytes(b);
        StringBuilder sb = new StringBuilder();
        for (byte by : b) sb.append(String.format("%02x", by));
        return sb.toString();
    }

    static String hexUUID4() {
        SecureRandom rnd = new SecureRandom();
        byte[] b = new byte[16];
        rnd.nextBytes(b);
        b[6] = (byte) ((b[6] & 0x0f) | 0x40);
        b[8] = (byte) ((b[8] & 0x3f) | 0x80);
        StringBuilder h = new StringBuilder();
        for (byte by : b) h.append(String.format("%02x", by));
        return h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16)
                + "-" + h.substring(16, 20) + "-" + h.substring(20, 32);
    }

    static String redact(String name, String value) {
        String lname = name.toLowerCase();
        for (String marker : new String[]{"token", "auth", "cookie", "password", "secret", "credential"}) {
            if (lname.contains(marker)) return "<REDACTED>";
        }
        return value;
    }

    public static void main(String[] args) throws Exception {
        debugf("[1] Building request\n");

        long nowUnix = System.currentTimeMillis() / 1000;
        long nowUnixMilli = System.currentTimeMillis();

        List<Param> params = new ArrayList<>();


        addParam(params, "passport-sdk-version", "6031490");
        addParam(params, "request_tag_from", "h5");
        addParam(params, "fixed_mix_mode", "1");
        addParam(params, "mix_mode", "1");
        addParam(params, "account_param", "");
        addParam(params, "scene", "1");
        addParam(params, "device_platform", "android");
        addParam(params, "os", "android");
        addParam(params, "ssmix", "a");
        addParam(params, "type", "3736");

        addParam(params, "iid",
                String.valueOf(randInt64Range(
                        7500000000000000000L,
                        7699999999999999999L
                )));

        addParam(params, "device_id",
                String.valueOf(randInt64Range(
                        7500000000000000000L,
                        7699999999999999999L
                )));

        addParam(params, "ac", "MOBILE");
        addParam(params, "channel", "googleplay");
        addParam(params, "app_name", "musical_ly");
        addParam(params, "version_code", "370004");
        addParam(params, "version_name", "37.0.4");
        addParam(params, "ab_version", "37.0.4");
        addParam(params, "device_type", "WayDroid x86_64 Device");
        addParam(params, "device_brand", "waydroid");
        addParam(params, "language", "en");
        addParam(params, "os_api", "33");
        addParam(params, "os_version", "13");
        addParam(params, "openudid", "95a2f1db117b750c");
        addParam(params, "manifest_version_code", "2023700040");
        addParam(params, "resolution", "1920*985");
        addParam(params, "dpi", "180");
        addParam(params, "update_version_code", "2023700040");

        addParam(params, "_rticket", String.valueOf(nowUnixMilli));

        addParam(params, "is_pad", "1");
        addParam(params, "app_type", "normal");
        addParam(params, "sys_region", "US");

// Python:
// str(round(int(time.time()-len("atro"))))
// len("atro") = 4
        addParam(params, "last_install_time", String.valueOf(nowUnix - 4));

        addParam(params, "timezone_name", "GMT");
        addParam(params, "app_language", "en");
        addParam(params, "ac2", "unknown");
        addParam(params, "uoo", "1");
        addParam(params, "op_region", "US");
        addParam(params, "timezone_offset", "0");
        addParam(params, "build_number", "37.0.4");
        addParam(params, "host_abi", "arm64-v8a");
        addParam(params, "locale", "en");
        addParam(params, "region", "US");
        addParam(params, "ts", String.valueOf(nowUnix));

        addParam(params, "cdid", hexUUID4());

        addParam(params, "support_webview", "1");
        addParam(params, "reg_store_region", "nl");
        addParam(params, "okhttp_version", "4.2.195.9-tiktok");
        addParam(params, "use_store_region_cookie", "1");

        System.out.print("Enter email to send code -> ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String email = reader.readLine();

        List<Param> data = new ArrayList<>();
        addParam(data, "account_sdk_source", "app");
        addParam(data, "rule_strategies", "2");
        addParam(data, "mix_mode", "1");
        addParam(data, "multi_login", "1");
        addParam(data, "type", "3736");
        addParam(data, "email", xor(email));
        addParam(data, "email_theme", "2");
        addParam(data, "use_passport_ticket", "1");
        addParam(data, "scene", "1");

        Signatures sig = Signer.sign(
                SignRequest.builder().params(params).data(data).build(),
                SignOptions.builder().timestamp(nowUnix).aid(1233).version(8404).build());

        debugf("[2] Request built\n");

        System.out.printf("X-Argus: %s%n", sig.xArgus);
        System.out.printf("X-Ladon: %s%n", sig.xLadon);
        System.out.printf("X-Gorgon: %s%n", sig.xGorgon);
        System.out.printf("X-Khronos: %s%n", sig.xKhronos);
        System.out.printf("X-TT-Request-Ticket: %s%n", sig.xTTRequestTicket);
        System.out.printf("X-SS-Stub: %s%n", sig.xSSStub);

        String query = UrlEncode.encodeParams(params);
        String bodyStr = UrlEncode.encodeParams(data);

        String fullURL = TARGET_URL;
        if (!query.isEmpty()) fullURL += "?" + query;

        URL url = new URL(fullURL);
        HttpURLConnection conn;
        if (url.getProtocol().equalsIgnoreCase("https")) {
            javax.net.ssl.HttpsURLConnection hc = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            hc.setHostnameVerifier((h, s) -> true);
            conn = hc;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }
        conn.setRequestMethod("POST");

        conn.setRequestProperty("Host", "api16-normal-c-alisg.tiktokv.com");
        conn.setRequestProperty("Sdk-Version", "2");
        conn.setRequestProperty("Tt-Ticket-Guard-Iteration-Version", "0");
        conn.setRequestProperty("X-Tt-Dm-Status", "login=0;ct=1;rt=6");
        conn.setRequestProperty("Tt-Ticket-Guard-Version", "3");
        conn.setRequestProperty("Passport-Sdk-Settings", "x-tt-token");
        conn.setRequestProperty("Passport-Sdk-Sign", "x-tt-token");
        conn.setRequestProperty("Passport-Sdk-Version", "-1");
        conn.setRequestProperty("X-Tt-Bypass-Dp", "1");
        conn.setRequestProperty("X-Vc-Bdturing-Sdk-Version", "2.3.17.i18n");
        conn.setRequestProperty("Tt-Device-Guard-Iteration-Version", "1");
        conn.setRequestProperty("User-Agent", "com.zhiliaoapp.musically/2024109030 (Linux; U; Android 13; en; WayDroid x86_64 Device; Build/TQ3A.230901.001;tt-ok/3.12.13.21)");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("Accept", "*/*");

        conn.setRequestProperty("X-Argus", sig.xArgus);
        conn.setRequestProperty("X-Ladon", sig.xLadon);
        conn.setRequestProperty("X-Gorgon", sig.xGorgon);
        conn.setRequestProperty("X-Khronos", sig.xKhronos);
        conn.setRequestProperty("X-TT-Request-Ticket", sig.xTTRequestTicket);
        conn.setRequestProperty("X-SS-Stub", sig.xSSStub);

        conn.setDoOutput(true);
        conn.setDoInput(true);
        byte[] body = bodyStr.getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(body.length);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        System.out.println("\n==================== HTTP RESPONSE ====================");
        System.out.println("Status Code: " + conn.getResponseCode());
        System.out.println("Headers:");
        conn.getHeaderFields().forEach((k, vs) -> {
            if (k != null) {
                for (String v : vs) System.out.printf("%s: %s%n", k, redact(k, v));
            }
        });

        System.out.println("Body:");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream()));
        String line;
        StringBuilder bodyRead = new StringBuilder();
        while ((line = br.readLine()) != null) {
            bodyRead.append(line).append('\n');
        }
        String respBody = bodyRead.toString();
        System.out.println("Body Length: " + respBody.length());
        if (respBody.trim().isEmpty()) {
            System.out.println("<EMPTY>");
        } else {
            System.out.println(respBody);
        }
    }

    private static void addParam(List<Param> list, String k, String v) {
        list.add(new Param(k, v));
    }
}
