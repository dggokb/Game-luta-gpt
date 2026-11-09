package org.json;
import java.util.*;
/** org.json mínimo e de verdade, só para rodar o jogo no ambiente sem o Maven. */
public class JSONObject {
    public static final Object NULL = new Object() { public String toString() { return "null"; } };
    final LinkedHashMap<String, Object> m = new LinkedHashMap<>();
    public JSONObject() {}
    public JSONObject(String s) {
        P p = new P(s); p.ws();
        Object o = p.value(); 
        if (!(o instanceof JSONObject)) throw new JSONException("não é objeto");
        m.putAll(((JSONObject) o).m);
    }
    public boolean has(String k) { return m.containsKey(k); }
    public boolean isNull(String k) { Object o = m.get(k); return o == null || o == NULL; }
    public int length() { return m.size(); }
    public Iterator<String> keys() { return new ArrayList<>(m.keySet()).iterator(); }
    public Set<String> keySet() { return m.keySet(); }
    public JSONArray names() { if (m.isEmpty()) return null; JSONArray a = new JSONArray(); for (String k : m.keySet()) a.put(k); return a; }
    public Object get(String k) { Object o = m.get(k); if (o == null) throw new JSONException("sem " + k); return o; }
    public Object opt(String k) { return m.get(k); }
    public Object remove(String k) { return m.remove(k); }
    public JSONObject getJSONObject(String k) { Object o = get(k); if (o instanceof JSONObject) return (JSONObject) o; throw new JSONException(k + " não é objeto"); }
    public JSONObject optJSONObject(String k) { Object o = m.get(k); return o instanceof JSONObject ? (JSONObject) o : null; }
    public JSONArray getJSONArray(String k) { Object o = get(k); if (o instanceof JSONArray) return (JSONArray) o; throw new JSONException(k + " não é lista"); }
    public JSONArray optJSONArray(String k) { Object o = m.get(k); return o instanceof JSONArray ? (JSONArray) o : null; }
    public String getString(String k) { Object o = get(k); if (o instanceof String) return (String) o; if (o == NULL) throw new JSONException(k + " nulo"); return String.valueOf(o); }
    public String optString(String k) { return optString(k, ""); }
    public String optString(String k, String d) { Object o = m.get(k); return o == null || o == NULL ? d : String.valueOf(o); }
    static Double num(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o instanceof String) try { return Double.parseDouble((String) o); } catch (NumberFormatException e) { return null; }
        return null;
    }
    static Boolean bool(Object o) {
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof String) { if ("true".equalsIgnoreCase((String) o)) return true; if ("false".equalsIgnoreCase((String) o)) return false; }
        return null;
    }
    public int getInt(String k) { Double d = num(get(k)); if (d == null) throw new JSONException(k + " não é número"); return (int) d.doubleValue(); }
    public int optInt(String k) { return optInt(k, 0); }
    public int optInt(String k, int d) { Double v = num(m.get(k)); return v == null ? d : (int) v.doubleValue(); }
    public long getLong(String k) { Double d = num(get(k)); if (d == null) throw new JSONException(k + " não é número"); return (long) d.doubleValue(); }
    public long optLong(String k) { return optLong(k, 0); }
    public long optLong(String k, long d) { Double v = num(m.get(k)); return v == null ? d : (long) v.doubleValue(); }
    public double getDouble(String k) { Double d = num(get(k)); if (d == null) throw new JSONException(k + " não é número"); return d; }
    public double optDouble(String k) { return optDouble(k, Double.NaN); }
    public double optDouble(String k, double d) { Double v = num(m.get(k)); return v == null ? d : v; }
    public boolean getBoolean(String k) { Boolean b = bool(get(k)); if (b == null) throw new JSONException(k + " não é booleano"); return b; }
    public boolean optBoolean(String k) { return optBoolean(k, false); }
    public boolean optBoolean(String k, boolean d) { Boolean b = bool(m.get(k)); return b == null ? d : b; }
    public JSONObject put(String k, Object v) { if (v == null) m.remove(k); else m.put(k, v); return this; }
    public JSONObject put(String k, int v) { m.put(k, v); return this; }
    public JSONObject put(String k, long v) { m.put(k, v); return this; }
    public JSONObject put(String k, double v) { m.put(k, v); return this; }
    public JSONObject put(String k, boolean v) { m.put(k, v); return this; }
    @Override public String toString() { StringBuilder b = new StringBuilder(); write(b, this, 0, 0); return b.toString(); }
    public String toString(int indent) { StringBuilder b = new StringBuilder(); write(b, this, indent, 0); return b.toString(); }

    static void nl(StringBuilder b, int indent, int depth) { if (indent > 0) { b.append('\n'); for (int i = 0; i < indent * depth; i++) b.append(' '); } }
    static void write(StringBuilder b, Object o, int indent, int depth) {
        if (o instanceof JSONObject) {
            Map<String, Object> m = ((JSONObject) o).m; b.append('{'); boolean first = true;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                if (!first) b.append(','); first = false; nl(b, indent, depth + 1);
                str(b, e.getKey()); b.append(indent > 0 ? ": " : ":"); write(b, e.getValue(), indent, depth + 1);
            }
            if (!m.isEmpty()) nl(b, indent, depth); b.append('}');
        } else if (o instanceof JSONArray) {
            List<Object> l = ((JSONArray) o).l; b.append('['); boolean first = true;
            for (Object v : l) { if (!first) b.append(','); first = false; nl(b, indent, depth + 1); write(b, v, indent, depth + 1); }
            if (!l.isEmpty()) nl(b, indent, depth); b.append(']');
        } else if (o instanceof String) str(b, (String) o);
        else if (o instanceof Double || o instanceof Float) {
            double d = ((Number) o).doubleValue(); if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d); else b.append(d);
        } else b.append(String.valueOf(o));
    }
    static void str(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break; case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break; case '\r': b.append("\\r"); break; case '\t': b.append("\\t"); break;
                default: if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
            }
        }
        b.append('"');
    }

    static final class P {
        final String s; int i;
        P(String s) { this.s = s; }
        void ws() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t' || c == '﻿') i++;
                else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') { while (i < s.length() && s.charAt(i) != '\n') i++; }
                else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') { i = s.indexOf("*/", i + 2) + 2; }
                else break;
            }
        }
        Object value() {
            ws(); if (i >= s.length()) throw new JSONException("fim inesperado");
            char c = s.charAt(i);
            if (c == '{') {
                i++; JSONObject o = new JSONObject(); ws();
                if (s.charAt(i) == '}') { i++; return o; }
                while (true) {
                    ws(); String k = s.charAt(i) == '"' ? string() : bare(); ws();
                    if (s.charAt(i) != ':') throw new JSONException("esperava : em " + i); i++;
                    o.m.put(k, value()); ws();
                    char d = s.charAt(i++);
                    if (d == '}') return o;
                    if (d != ',') throw new JSONException("esperava , ou } em " + i);
                    ws(); if (s.charAt(i) == '}') { i++; return o; }
                }
            }
            if (c == '[') {
                i++; JSONArray a = new JSONArray(); ws();
                if (s.charAt(i) == ']') { i++; return a; }
                while (true) {
                    a.l.add(value()); ws();
                    char d = s.charAt(i++);
                    if (d == ']') return a;
                    if (d != ',') throw new JSONException("esperava , ou ] em " + i);
                    ws(); if (s.charAt(i) == ']') { i++; return a; }
                }
            }
            if (c == '"') return string();
            String t = bare();
            if (t.equals("true")) return Boolean.TRUE;
            if (t.equals("false")) return Boolean.FALSE;
            if (t.equals("null")) return NULL;
            try {
                if (t.indexOf('.') < 0 && t.indexOf('e') < 0 && t.indexOf('E') < 0) {
                    long v = Long.parseLong(t); if (v == (int) v) return (int) v; return v;
                }
                return Double.parseDouble(t);
            } catch (NumberFormatException e) { return t; }
        }
        String bare() { int j = i; while (i < s.length() && ",:]}\n\r\t ".indexOf(s.charAt(i)) < 0) i++; if (i == j) throw new JSONException("valor vazio em " + i); return s.substring(j, i); }
        String string() {
            StringBuilder b = new StringBuilder(); i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': b.append('\n'); break; case 't': b.append('\t'); break; case 'r': b.append('\r'); break;
                        case 'b': b.append('\b'); break; case 'f': b.append('\f'); break;
                        case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: b.append(e);
                    }
                } else b.append(c);
            }
        }
    }
}
