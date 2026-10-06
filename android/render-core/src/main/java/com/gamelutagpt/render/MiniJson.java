package com.gamelutagpt.render;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser JSON mínimo e sem dependências, para o motor rodar igual no
 * Android e no PC. Objetos viram Map, arrays viram List, números viram
 * Double.
 */
public final class MiniJson {
    private final String text;
    private int pos;

    private MiniJson(String text) {
        this.text = text;
    }

    public static Object parse(String text) {
        MiniJson parser = new MiniJson(text);
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("conteúdo extra depois do JSON");
        }
        return value;
    }

    private Object readValue() {
        if (pos >= text.length()) throw error("fim inesperado");
        char c = text.charAt(pos);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (c == 't') return readLiteral("true", Boolean.TRUE);
        if (c == 'f') return readLiteral("false", Boolean.FALSE);
        if (c == 'n') return readLiteral("null", null);
        if (c == '-' || (c >= '0' && c <= '9')) return readNumber();
        throw error("caractere inesperado '" + c + "'");
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return map;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') throw error("esperava nome de campo");
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            map.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') return map;
            if (c != ',') throw error("esperava ',' ou '}'");
        }
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return list;
        }
        while (true) {
            skipWhitespace();
            list.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') return list;
            if (c != ',') throw error("esperava ',' ou ']'");
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') return sb.toString();
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char e = next();
            switch (e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if (pos + 4 > text.length()) throw error("escape \\u incompleto");
                    sb.append((char)Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                    break;
                default:
                    throw error("escape inválido '\\" + e + "'");
            }
        }
    }

    private Double readNumber() {
        int start = pos;
        if (peek() == '-') pos++;
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                pos++;
            } else {
                break;
            }
        }
        try {
            return Double.valueOf(text.substring(start, pos));
        } catch (NumberFormatException ex) {
            throw error("número inválido");
        }
    }

    private Object readLiteral(String literal, Object value) {
        if (!text.startsWith(literal, pos)) throw error("esperava " + literal);
        pos += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private char next() {
        if (pos >= text.length()) throw error("fim inesperado");
        return text.charAt(pos++);
    }

    private void expect(char c) {
        if (next() != c) throw error("esperava '" + c + "'");
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON inválido na posição " + pos + ": " + message);
    }
}
