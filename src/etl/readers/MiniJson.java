package etl.readers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny, dependency-free JSON parser for the demo.
 *
 * <p>Supports the subset of JSON we actually need:
 * objects, arrays, strings, numbers (int / double / BigDecimal-as-string),
 * booleans, null, and standard escape sequences in strings.</p>
 *
 * <p>Returned types:</p>
 * <ul>
 *   <li>object &rarr; {@code Map<String, Object>} (insertion-ordered)</li>
 *   <li>array  &rarr; {@code List<Object>}</li>
 *   <li>string &rarr; {@code String}</li>
 *   <li>number &rarr; {@code String} (caller chooses how to parse, keeps full precision)</li>
 *   <li>true / false &rarr; {@code Boolean}</li>
 *   <li>null   &rarr; {@code null}</li>
 * </ul>
 *
 * <p>Throws {@link IllegalArgumentException} on malformed input.</p>
 */
public final class MiniJson {

    private final String src;
    private int pos;

    private MiniJson(String src) {
        this.src = src;
        this.pos = 0;
    }

    public static Object parse(String json) {
        MiniJson p = new MiniJson(json);
        p.skipWs();
        Object value = p.readValue();
        p.skipWs();
        if (p.pos != p.src.length()) {
            throw p.error("Unexpected trailing data");
        }
        return value;
    }

    private Object readValue() {
        skipWs();
        if (pos >= src.length()) throw error("Unexpected end of input");
        char c = src.charAt(pos);
        return switch (c) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't', 'f' -> readBoolean();
            case 'n' -> readNull();
            default -> {
                if (c == '-' || (c >= '0' && c <= '9')) yield readNumber();
                throw error("Unexpected character: " + c);
            }
        };
    }

    private Map<String, Object> readObject() {
        expect('{');
        skipWs();
        Map<String, Object> map = new LinkedHashMap<>();
        if (peek() == '}') { pos++; return map; }
        while (true) {
            skipWs();
            String key = readString();
            skipWs();
            expect(':');
            Object value = readValue();
            map.put(key, value);
            skipWs();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; return map; }
            throw error("Expected ',' or '}' in object, got: " + c);
        }
    }

    private List<Object> readArray() {
        expect('[');
        skipWs();
        List<Object> list = new ArrayList<>();
        if (peek() == ']') { pos++; return list; }
        while (true) {
            list.add(readValue());
            skipWs();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; return list; }
            throw error("Expected ',' or ']' in array, got: " + c);
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                if (pos >= src.length()) throw error("Bad escape at end of input");
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"', '\\', '/' -> sb.append(esc);
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 > src.length()) throw error("Bad unicode escape");
                        sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> throw error("Unknown escape: \\" + esc);
                }
            } else {
                sb.append(c);
            }
        }
        throw error("Unterminated string");
    }

    private String readNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < src.length() && isNumChar(src.charAt(pos))) pos++;
        return src.substring(start, pos);
    }

    private Boolean readBoolean() {
        if (src.startsWith("true", pos))  { pos += 4; return Boolean.TRUE;  }
        if (src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        throw error("Expected boolean");
    }

    private Object readNull() {
        if (src.startsWith("null", pos)) { pos += 4; return null; }
        throw error("Expected null");
    }

    private boolean isNumChar(char c) {
        return (c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-';
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private char peek() {
        if (pos >= src.length()) throw error("Unexpected end of input");
        return src.charAt(pos);
    }

    private void expect(char c) {
        if (pos >= src.length() || src.charAt(pos) != c) {
            throw error("Expected '" + c + "', got " + (pos >= src.length() ? "EOF" : src.charAt(pos)));
        }
        pos++;
    }

    private IllegalArgumentException error(String msg) {
        return new IllegalArgumentException("JSON parse error at pos " + pos + ": " + msg);
    }
}
