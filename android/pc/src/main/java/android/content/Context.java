package android.content;

import android.content.res.AssetManager;
import android.content.res.Resources;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * PC: o "aplicativo" é uma pasta de dados (assets e drawables) mais uma pasta de
 * preferências do usuário. Só o que o jogo usa do Context do Android.
 */
public class Context {
    public static final int MODE_PRIVATE = 0;
    public static final String ACTIVITY_SERVICE = "activity";
    private final android.app.ActivityManager activityManager = new android.app.ActivityManager();

    private final AssetManager assets;
    private final Resources resources;
    private final File preferencesDir;
    private final Map<String, FilePreferences> preferences = new HashMap<>();

    public Context(AssetManager assets, Resources resources, File preferencesDir) {
        this.assets = assets;
        this.resources = resources;
        this.preferencesDir = preferencesDir;
    }

    public AssetManager getAssets() { return assets; }

    public Resources getResources() { return resources; }

    public String getPackageName() { return "com.gamelutagpt"; }

    public Object getSystemService(String name) { return ACTIVITY_SERVICE.equals(name) ? activityManager : null; }

    public Context getApplicationContext() { return this; }

    public synchronized SharedPreferences getSharedPreferences(String name, int mode) {
        FilePreferences p = preferences.get(name);
        if (p == null) {
            p = new FilePreferences(preferencesDir == null ? null : new File(preferencesDir, name + ".properties"));
            preferences.put(name, p);
        }
        return p;
    }

    /** Preferências guardadas em texto; sem pasta (testes) ficam só na memória. */
    static final class FilePreferences implements SharedPreferences {
        private final File file;
        private final Properties values = new Properties();

        FilePreferences(File file) {
            this.file = file;
            if (file != null && file.isFile()) {
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    values.load(in);
                } catch (IOException ignored) {
                    // Preferência ilegível: começa do padrão.
                }
            }
        }

        @Override public synchronized Map<String, ?> getAll() {
            Map<String, Object> all = new HashMap<>();
            for (String key : values.stringPropertyNames()) all.put(key, values.getProperty(key));
            return all;
        }
        @Override public synchronized String getString(String key, String defValue) { return values.getProperty(key, defValue); }
        @Override public int getInt(String key, int defValue) {
            try { return Integer.parseInt(getString(key, Integer.toString(defValue))); } catch (NumberFormatException e) { return defValue; }
        }
        @Override public long getLong(String key, long defValue) {
            try { return Long.parseLong(getString(key, Long.toString(defValue))); } catch (NumberFormatException e) { return defValue; }
        }
        @Override public float getFloat(String key, float defValue) {
            try { return Float.parseFloat(getString(key, Float.toString(defValue))); } catch (NumberFormatException e) { return defValue; }
        }
        @Override public boolean getBoolean(String key, boolean defValue) {
            String v = getString(key, null);
            return v == null ? defValue : Boolean.parseBoolean(v);
        }
        @Override public synchronized boolean contains(String key) { return values.containsKey(key); }

        @Override public Editor edit() {
            return new Editor() {
                private final Map<String, String> puts = new HashMap<>();
                private final java.util.Set<String> removes = new java.util.HashSet<>();
                private boolean clear;

                @Override public Editor putString(String key, String value) {
                    if (value == null) return remove(key);
                    puts.put(key, value); removes.remove(key); return this;
                }
                @Override public Editor putInt(String key, int value) { return putString(key, Integer.toString(value)); }
                @Override public Editor putLong(String key, long value) { return putString(key, Long.toString(value)); }
                @Override public Editor putFloat(String key, float value) { return putString(key, Float.toString(value)); }
                @Override public Editor putBoolean(String key, boolean value) { return putString(key, Boolean.toString(value)); }
                @Override public Editor remove(String key) { removes.add(key); puts.remove(key); return this; }
                @Override public Editor clear() { clear = true; return this; }
                @Override public boolean commit() {
                    synchronized (FilePreferences.this) {
                        if (clear) values.clear();
                        for (String key : removes) values.remove(key);
                        values.putAll(puts);
                        return save();
                    }
                }
                @Override public void apply() { commit(); }
            };
        }

        private boolean save() {
            if (file == null) return true;
            try {
                File dir = file.getParentFile();
                if (dir != null) Files.createDirectories(dir.toPath());
                try (OutputStream out = Files.newOutputStream(file.toPath())) {
                    values.store(out, "Game Luta GPT");
                }
                return true;
            } catch (IOException e) {
                android.util.Log.w("Context", "não salvou " + file, e);
                return false;
            }
        }
    }
}
