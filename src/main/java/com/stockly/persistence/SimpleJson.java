package com.stockly.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A tiny JSON reader and writer so the project needs <strong>no external libraries</strong>.
 * <p>
 * Supported value types: objects ({@code Map<String,Object>}), arrays ({@code List<Object>}), strings,
 * integers ({@code Long}), decimals ({@code Double}), booleans and {@code null}. That is everything the
 * inventory file uses.
 * </p>
 *
 * @author Umama Wahab
 */
public final class SimpleJson {

    private final String text;
    private int position;

    private SimpleJson(String text) {
        this.text = text;
    }

    // ------------------------------------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------------------------------------

    /**
     * Parses JSON text into Java objects.
     *
     * @param json the JSON document
     * @return a {@code Map}, {@code List}, {@code String}, {@code Long}, {@code Double}, {@code Boolean} or {@code null}
     * @throws IllegalArgumentException if the text is not valid JSON
     */
    public static Object parse(String json) {
        SimpleJson parser = new SimpleJson(json);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.position != json.length()) {
            throw parser.error("Unexpected content after the JSON document");
        }
        return value;
    }

    private Object readValue() {
        if (position >= text.length()) {
            throw error("Unexpected end of file");
        }
        char c = text.charAt(position);
        switch (c) {
            case '{':
                return readObject();
            case '[':
                return readArray();
            case '"':
                return readString();
            case 't':
                return readLiteral("true", Boolean.TRUE);
            case 'f':
                return readLiteral("false", Boolean.FALSE);
            case 'n':
                return readLiteral("null", null);
            default:
                return readNumber();
        }
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        position++; // skip '{'
        skipWhitespace();
        if (peek() == '}') {
            position++;
            return map;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return map;
            }
            if (c != ',') {
                throw error("Expected ',' or '}' in object");
            }
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        position++; // skip '['
        skipWhitespace();
        if (peek() == ']') {
            position++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return list;
            }
            if (c != ',') {
                throw error("Expected ',' or ']' in array");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                char escaped = next();
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (position + 4 > text.length()) {
                            throw error("Incomplete unicode escape");
                        }
                        try {
                            sb.append((char) Integer.parseInt(text.substring(position, position + 4), 16));
                        } catch (NumberFormatException e) {
                            throw error("Invalid unicode escape");
                        }
                        position += 4;
                        break;
                    default:
                        throw error("Invalid escape sequence");
                }
            } else if (c < 0x20) {
                throw error("Control character inside string");
            } else {
                sb.append(c);
            }
        }
    }

    private Object readLiteral(String word, Object value) {
        if (!text.startsWith(word, position)) {
            throw error("Unexpected token");
        }
        position += word.length();
        return value;
    }

    private Object readNumber() {
        int start = position;
        while (position < text.length() && "+-0123456789.eE".indexOf(text.charAt(position)) >= 0) {
            position++;
        }
        if (start == position) {
            throw error("Unexpected character '" + text.charAt(position) + "'");
        }
        String number = text.substring(start, position);
        try {
            if (number.contains(".") || number.contains("e") || number.contains("E")) {
                return Double.parseDouble(number);
            }
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            position = start;
            throw error("Invalid number '" + number + "'");
        }
    }

    private void skipWhitespace() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    private char peek() {
        if (position >= text.length()) {
            throw error("Unexpected end of file");
        }
        return text.charAt(position);
    }

    private char next() {
        char c = peek();
        position++;
        return c;
    }

    private void expect(char expected) {
        if (next() != expected) {
            position--;
            throw error("Expected '" + expected + "'");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + position);
    }

    // ------------------------------------------------------------------------------------------------
    // Writing
    // ------------------------------------------------------------------------------------------------

    /**
     * Converts Java objects into indented, human-readable JSON text.
     *
     * @param value a {@code Map}, {@code List}, {@code String}, {@code Number}, {@code Boolean} or {@code null}
     * @return the JSON text
     * @throws IllegalArgumentException if the value contains an unsupported type
     */
    public static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String string) {
            quote(string, sb);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
        } else if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append("{\n");
            int count = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                indent(sb, indent + 1);
                quote(String.valueOf(entry.getKey()), sb);
                sb.append(": ");
                write(entry.getValue(), sb, indent + 1);
                sb.append(++count < map.size() ? ",\n" : "\n");
            }
            indent(sb, indent);
            sb.append('}');
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(sb, indent + 1);
                write(list.get(i), sb, indent + 1);
                sb.append(i < list.size() - 1 ? ",\n" : "\n");
            }
            indent(sb, indent);
            sb.append(']');
        } else {
            throw new IllegalArgumentException("Unsupported JSON type: " + value.getClass().getName());
        }
    }

    private static void indent(StringBuilder sb, int level) {
        sb.append("  ".repeat(level));
    }

    private static void quote(String string, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
