import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tiny JSON parser for the limited shapes produced by our C++ backend.
// Supports: objects with string/number/bool/array fields, array of objects, array of strings.
public final class JsonMini {
    private JsonMini() {}

    public static Map<String, String> obj(String json) {
        json = json == null ? "" : json.trim();
        Map<String, String> out = new HashMap<>();
        if (!json.startsWith("{") || !json.endsWith("}")) return out;
        int i = 1;
        while (i < json.length() - 1) {
            i = skipWs(json, i);
            if (i >= json.length() - 1) break;
            if (json.charAt(i) == ',') { i++; continue; }
            String key = readString(json, i);
            i = nextIndex;
            i = skipWs(json, i);
            if (i < json.length() && json.charAt(i) == ':') i++;
            i = skipWs(json, i);
            String val = readValue(json, i);
            i = nextIndex;
            out.put(key, val);
            i = skipWs(json, i);
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return out;
    }

    public static List<String> arrStrings(String json) {
        List<String> out = new ArrayList<>();
        json = json == null ? "" : json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return out;
        int i = 1;
        while (i < json.length() - 1) {
            i = skipWs(json, i);
            if (i >= json.length() - 1) break;
            if (json.charAt(i) == ',') { i++; continue; }
            String s = readString(json, i);
            i = nextIndex;
            out.add(s);
            i = skipWs(json, i);
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return out;
    }

    public static List<Map<String, String>> arrObjects(String json) {
        List<Map<String, String>> out = new ArrayList<>();
        json = json == null ? "" : json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return out;
        int i = 1;
        while (i < json.length() - 1) {
            i = skipWs(json, i);
            if (i >= json.length() - 1) break;
            if (json.charAt(i) == ',') { i++; continue; }
            String v = readValue(json, i);
            i = nextIndex;
            Map<String, String> o = obj(v);
            if (!o.isEmpty()) out.add(o);
            i = skipWs(json, i);
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        return out;
    }

    // Parses: [[n,l,r],[n,l,r],...] into List<int[]{n,l,r}>
    public static List<int[]> arrIntTriples(String json) {
        List<int[]> out = new ArrayList<>();
        json = json == null ? "" : json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return out;

        int i = 1; // skip outer [
        while (i < json.length() - 1) {
            i = skipWs(json, i);
            if (i >= json.length() - 1) break;
            char c = json.charAt(i);
            if (c == ',') { i++; continue; }
            if (c != '[') { i++; continue; }

            int end = matchBrace(json, i, '[', ']');
            if (end <= i) break;
            String inner = json.substring(i + 1, end - 1).trim(); // n,l,r
            String[] parts = inner.split(",");
            if (parts.length >= 3) {
                try {
                    int n = Integer.parseInt(parts[0].trim());
                    int l = Integer.parseInt(parts[1].trim());
                    int r = Integer.parseInt(parts[2].trim());
                    out.add(new int[] { n, l, r });
                } catch (Exception ignored) {
                }
            }
            i = end;
        }

        return out;
    }

    public static int asInt(String raw, int def) {
        try {
            return Integer.parseInt(stripQuotes(raw));
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean asBool(String raw) {
        raw = raw == null ? "" : raw.trim();
        return raw.equals("true") || raw.equals("\"true\"");
    }

    public static String asString(String raw) {
        return stripQuotes(raw);
    }

    public static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\\"", "\"");
    }

    // --- internal scanner ---

    private static int nextIndex = 0;

    private static int skipWs(String s, int i) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ' ' && c != '\n' && c != '\r' && c != '\t') break;
            i++;
        }
        return i;
    }

    private static String readString(String s, int i) {
        i = skipWs(s, i);
        if (i >= s.length() || s.charAt(i) != '"') {
            nextIndex = i;
            return "";
        }
        i++;
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '"') {
                i++;
                break;
            }
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (n == '"' || n == '\\' || n == '/') { sb.append(n); i += 2; continue; }
                if (n == 'n') { sb.append('\n'); i += 2; continue; }
                if (n == 'r') { sb.append('\r'); i += 2; continue; }
                if (n == 't') { sb.append('\t'); i += 2; continue; }
            }
            sb.append(c);
            i++;
        }
        nextIndex = i;
        return sb.toString();
    }

    private static String readValue(String s, int i) {
        i = skipWs(s, i);
        if (i >= s.length()) { nextIndex = i; return ""; }
        char c = s.charAt(i);
        if (c == '"') {
            String str = readString(s, i);
            nextIndex = nextIndex;
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        if (c == '{') {
            int end = matchBrace(s, i, '{', '}');
            nextIndex = end;
            return s.substring(i, end);
        }
        if (c == '[') {
            int end = matchBrace(s, i, '[', ']');
            nextIndex = end;
            return s.substring(i, end);
        }

        int j = i;
        while (j < s.length()) {
            char cc = s.charAt(j);
            if (cc == ',' || cc == '}' || cc == ']') break;
            j++;
        }
        nextIndex = j;
        return s.substring(i, j).trim();
    }

    private static int matchBrace(String s, int i, char open, char close) {
        int depth = 0;
        boolean inStr = false;
        for (int j = i; j < s.length(); j++) {
            char c = s.charAt(j);
            if (c == '"' && (j == 0 || s.charAt(j - 1) != '\\')) inStr = !inStr;
            if (inStr) continue;
            if (c == open) depth++;
            if (c == close) {
                depth--;
                if (depth == 0) return j + 1;
            }
        }
        return s.length();
    }
}
