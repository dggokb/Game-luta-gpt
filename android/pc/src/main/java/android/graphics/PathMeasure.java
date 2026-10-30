package android.graphics;

import java.util.ArrayList;
import java.util.List;

/** Comprimento e pontos de cada contorno de um {@link Path}, sobre a versão em segmentos dele. */
public class PathMeasure {
    private final List<float[]> contornos = new ArrayList<>();
    private int atual;

    public PathMeasure() {}

    public PathMeasure(Path path, boolean forceClosed) { setPath(path, forceClosed); }

    public void setPath(Path path, boolean forceClosed) {
        contornos.clear();
        atual = 0;
        if (path == null) return;
        for (float[] c : Path.contornos(path.shape, 0.25f, forceClosed)) contornos.add(c);
    }

    public float getLength() {
        if (atual >= contornos.size()) return 0f;
        float[] c = contornos.get(atual);
        float l = 0f;
        for (int i = 2; i < c.length; i += 2) l += (float) Math.hypot(c[i] - c[i - 2], c[i + 1] - c[i - 1]);
        return l;
    }

    public boolean getPosTan(float distancia, float[] pos, float[] tan) {
        if (atual >= contornos.size()) return false;
        float[] c = contornos.get(atual);
        if (c.length < 2) return false;
        float resto = Math.max(0f, distancia);
        for (int i = 2; i < c.length; i += 2) {
            float dx = c[i] - c[i - 2], dy = c[i + 1] - c[i - 1];
            float l = (float) Math.hypot(dx, dy);
            if (resto <= l || i == c.length - 2) {
                float f = l > 0f ? Math.min(1f, resto / l) : 0f;
                if (pos != null) { pos[0] = c[i - 2] + dx * f; pos[1] = c[i - 1] + dy * f; }
                if (tan != null) { tan[0] = l > 0f ? dx / l : 1f; tan[1] = l > 0f ? dy / l : 0f; }
                return true;
            }
            resto -= l;
        }
        if (pos != null) { pos[0] = c[0]; pos[1] = c[1]; }
        if (tan != null) { tan[0] = 1f; tan[1] = 0f; }
        return true;
    }

    public boolean nextContour() { return ++atual < contornos.size(); }

}
