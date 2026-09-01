// URL encoding / parsing — exact port of tiktok-signature/urlencode.go.
//
// Author: S1
// GitHub: github.com/JokerPython3
// Based on: Go port of SignerPy (Python)
// Language: Java
package tiktoksignature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UrlEncode {

    private UrlEncode() {}

    public static String encodeParams(List<Param> pairs) {
        if (pairs == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pairs.size(); i++) {
            if (i > 0) sb.append('&');
            sb.append(quotePlus(pairs.get(i).key));
            sb.append('=');
            sb.append(quotePlus(pairs.get(i).value));
        }
        return sb.toString();
    }

    public static String quotePlus(String s) {
        byte[] bytes = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(bytes.length);
        for (byte b : bytes) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_' || c == '.' || c == '-' || c == '~') {
                sb.append((char) c);
            } else if (c == ' ') {
                sb.append('+');
            } else {
                sb.append('%');
                sb.append(HEX_UPPER_DIGITS[(c >> 4) & 0x0F]);
                sb.append(HEX_UPPER_DIGITS[c & 0x0F]);
            }
        }
        return sb.toString();
    }

    private static final char[] HEX_UPPER_DIGITS = "0123456789ABCDEF".toCharArray();

    public static String unquotePlus(String s) {
        s = s.replace('+', ' ');
        if (!s.contains("%")) return s;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = fromHex64(s.charAt(i + 1));
                int lo = fromHex64(s.charAt(i + 2));
                if (hi >= 0 && lo >= 0) {
                    out.write((hi << 4) | lo);
                    i += 2;
                    continue;
                }
            }
            out.write((byte) c);
        }
        return new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int fromHex64(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }

    public static Map<String, String> parseQueryParams(String qs) {
        Map<String, String> out = new LinkedHashMap<>();
        if (qs == null || qs.isEmpty()) return out;
        for (String nameValue : qs.split("&")) {
            if (nameValue.isEmpty()) continue;
            for (String pair : nameValue.split(";")) {
                if (pair.isEmpty()) continue;
                int eq = pair.indexOf('=');
                if (eq < 0) continue;
                String name = pair.substring(0, eq);
                String value = pair.substring(eq + 1);
                if (value.isEmpty()) continue;
                name = unquotePlus(name);
                value = unquotePlus(value);
                if (!out.containsKey(name)) {
                    out.put(name, value);
                }
            }
        }
        return out;
    }
}
