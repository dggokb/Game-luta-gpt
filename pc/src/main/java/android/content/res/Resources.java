package android.content.res;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class Resources {
    private final Map<Integer,String> names = new HashMap<>();

    public int getIdentifier(String name,String type,String packageName) {
        if (!"drawable".equals(type) || name == null || name.isBlank()) return 0;
        int id = name.hashCode();
        if (id == 0) id = 1;
        names.put(id,name);
        return id;
    }

    public InputStream openDrawable(int id) {
        String name = names.get(id);
        if (name == null) return null;
        ClassLoader loader = Resources.class.getClassLoader();
        InputStream stream = loader.getResourceAsStream("drawable/" + name + ".png");
        if (stream == null) stream = loader.getResourceAsStream("drawable/" + name + ".webp");
        return stream;
    }
}
