package org.json;
import java.util.*;
public class JSONArray {
    final ArrayList<Object> l = new ArrayList<>();
    public JSONArray() {}
    public JSONArray(String s) {
        JSONObject.P p = new JSONObject.P(s); Object o = p.value();
        if (!(o instanceof JSONArray)) throw new JSONException("não é lista");
        l.addAll(((JSONArray) o).l);
    }
    public int length() { return l.size(); }
    public Object get(int i) { if (i < 0 || i >= l.size()) throw new JSONException("índice " + i); return l.get(i); }
    public Object opt(int i) { return i < 0 || i >= l.size() ? null : l.get(i); }
    public JSONObject getJSONObject(int i) { Object o = get(i); if (o instanceof JSONObject) return (JSONObject) o; throw new JSONException(i + " não é objeto"); }
    public JSONObject optJSONObject(int i) { Object o = opt(i); return o instanceof JSONObject ? (JSONObject) o : null; }
    public JSONArray getJSONArray(int i) { Object o = get(i); if (o instanceof JSONArray) return (JSONArray) o; throw new JSONException(i + " não é lista"); }
    public JSONArray optJSONArray(int i) { Object o = opt(i); return o instanceof JSONArray ? (JSONArray) o : null; }
    public String getString(int i) { Object o = get(i); if (o == JSONObject.NULL) throw new JSONException(i + " nulo"); return String.valueOf(o); }
    public String optString(int i) { return optString(i, ""); }
    public String optString(int i, String d) { Object o = opt(i); return o == null || o == JSONObject.NULL ? d : String.valueOf(o); }
    public int getInt(int i) { Double d = JSONObject.num(get(i)); if (d == null) throw new JSONException(i + " não é número"); return (int) d.doubleValue(); }
    public int optInt(int i) { return optInt(i, 0); }
    public int optInt(int i, int d) { Double v = JSONObject.num(opt(i)); return v == null ? d : (int) v.doubleValue(); }
    public double getDouble(int i) { Double d = JSONObject.num(get(i)); if (d == null) throw new JSONException(i + " não é número"); return d; }
    public double optDouble(int i) { return optDouble(i, Double.NaN); }
    public double optDouble(int i, double d) { Double v = JSONObject.num(opt(i)); return v == null ? d : v; }
    public boolean getBoolean(int i) { Boolean b = JSONObject.bool(get(i)); if (b == null) throw new JSONException(i + " não é booleano"); return b; }
    public boolean optBoolean(int i) { Boolean b = JSONObject.bool(opt(i)); return b != null && b; }
    public long getLong(int i) { Double d = JSONObject.num(get(i)); if (d == null) throw new JSONException(i + " não é número"); return (long) d.doubleValue(); }
    public JSONArray put(Object o) { l.add(o); return this; }
    public JSONArray put(int v) { l.add(v); return this; }
    public JSONArray put(double v) { l.add(v); return this; }
    public JSONArray put(boolean v) { l.add(v); return this; }
    @Override public String toString() { StringBuilder b = new StringBuilder(); JSONObject.write(b, this, 0, 0); return b.toString(); }
    public String toString(int indent) { StringBuilder b = new StringBuilder(); JSONObject.write(b, this, indent, 0); return b.toString(); }
}
