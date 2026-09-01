// Minimal JSON parser for reading test vector files (no external dependencies).
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

public final class MiniJson {

    private MiniJson() {}

    public static Object parse(String s) {
        Parser p = new Parser(s);
        Object v = p.parseValue();
        p.skipWs();
        if (p.pos < s.length()) throw new RuntimeException("trailing data");
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object o) {
        return (Map<String, Object>) o;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object o) {
        return (List<Object>) o;
    }

    private static final class Parser {
        final String s;
        int pos;

        Parser(String s) { this.s = s; }

        void skipWs() {
            while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        }

        Object parseValue() {
            skipWs();
            if (pos >= s.length()) throw new RuntimeException("unexpected end");
            char c = s.charAt(pos);
            switch (c) {
                case '{': return parseObject();
                case '[': return parseArray();
                case '"': return parseString();
                case 't':
                    expect("true"); return Boolean.TRUE;
                case 'f':
                    expect("false"); return Boolean.FALSE;
                case 'n':
                    expect("null"); return null;
                default:
                    return parseNumber();
            }
        }

        void expect(String lit) {
            if (!s.startsWith(lit, pos)) throw new RuntimeException("expect " + lit);
            pos += lit.length();
        }

        Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<>();
            pos++; // {
            skipWs();
            if (pos < s.length() && s.charAt(pos) == '}') { pos++; return m; }
            while (true) {
                skipWs();
                if (pos >= s.length() || s.charAt(pos) != '"')
                    throw new RuntimeException("expected key");
                String key = parseString();
                skipWs();
                if (pos >= s.length() || s.charAt(pos) != ':')
                    throw new RuntimeException("expected :");
                pos++;
                Object val = parseValue();
                m.put(key, val);
                skipWs();
                if (pos >= s.length()) throw new RuntimeException("unterminated object");
                char c = s.charAt(pos);
                if (c == ',') { pos++; continue; }
                if (c == '}') { pos++; return m; }
                throw new RuntimeException("expected , or }");
            }
        }

        List<Object> parseArray() {
            List<Object> a = new ArrayList<>();
            pos++; // [
            skipWs();
            if (pos < s.length() && s.charAt(pos) == ']') { pos++; return a; }
            while (true) {
                a.add(parseValue());
                skipWs();
                if (pos >= s.length()) throw new RuntimeException("unterminated array");
                char c = s.charAt(pos);
                if (c == ',') { pos++; continue; }
                if (c == ']') { pos++; return a; }
                throw new RuntimeException("expected , or ]");
            }
        }

        String parseString() {
            if (s.charAt(pos) != '"') throw new RuntimeException("expected \"");
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '"') { pos++; return sb.toString(); }
                if (c == '\\') {
                    pos++;
                    if (pos >= s.length()) break;
                    char e = s.charAt(pos);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u': {
                            String hex = s.substring(pos + 1, pos + 5);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            break;
                        }
                        default: throw new RuntimeException("bad escape " + e);
                    }
                    pos++;
                } else {
                    sb.append(c);
                    pos++;
                }
            }
            throw new RuntimeException("unterminated string");
        }

        Object parseNumber() {
            int start = pos;
            while (pos < s.length() && "+-0123456789.eE".indexOf(s.charAt(pos)) >= 0) pos++;
            String num = s.substring(start, pos);
            if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                return Double.parseDouble(num);
            }
            long l = Long.parseLong(num);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
            return l;
        }
    }

    public static String optString(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o == null ? null : String.valueOf(o);
    }

    public static Integer optInt(Map<String, Object> m, String key) {
        Object o = m.get(key);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        return Integer.parseInt(String.valueOf(o));
    }

    public static Long optLong(Map<String, Object> m, String key) {
        Object o = m.get(key);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(String.valueOf(o));
    }
}
