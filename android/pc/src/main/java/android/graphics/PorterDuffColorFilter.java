package android.graphics;

/** Mistura uma cor fixa (fonte) em cada pixel (destino), como no Android. */
public class PorterDuffColorFilter extends ColorFilter {
    private final int color;
    private final PorterDuff.Mode mode;

    public PorterDuffColorFilter(int color, PorterDuff.Mode mode) {
        this.color = color;
        this.mode = mode;
    }

    @Override int filter(int dst) {
        float sa = Color.alpha(color) / 255f, da = Color.alpha(dst) / 255f;
        float sr = Color.red(color) / 255f, sg = Color.green(color) / 255f, sb = Color.blue(color) / 255f;
        float dr = Color.red(dst) / 255f, dg = Color.green(dst) / 255f, db = Color.blue(dst) / 255f;
        // Pré-multiplicado para as fórmulas de Porter-Duff.
        float psr = sr * sa, psg = sg * sa, psb = sb * sa, pdr = dr * da, pdg = dg * da, pdb = db * da;
        float ra, rr, rg, rb;
        switch (mode) {
            case SRC_ATOP:
                ra = da;
                rr = psr * da + pdr * (1 - sa); rg = psg * da + pdg * (1 - sa); rb = psb * da + pdb * (1 - sa);
                break;
            case SRC_IN:
                ra = sa * da; rr = psr * da; rg = psg * da; rb = psb * da;
                break;
            case DST_IN:
                ra = da * sa; rr = pdr * sa; rg = pdg * sa; rb = pdb * sa;
                break;
            case MULTIPLY:
                ra = sa * da; rr = psr * pdr; rg = psg * pdg; rb = psb * pdb;
                break;
            case SRC:
                ra = sa; rr = psr; rg = psg; rb = psb;
                break;
            case SRC_OVER:
                ra = sa + da * (1 - sa);
                rr = psr + pdr * (1 - sa); rg = psg + pdg * (1 - sa); rb = psb + pdb * (1 - sa);
                break;
            case SCREEN:
                ra = sa + da - sa * da;
                rr = psr + pdr - psr * pdr; rg = psg + pdg - psg * pdg; rb = psb + pdb - psb * pdb;
                break;
            case CLEAR:
                return 0;
            default:
                return dst;
        }
        if (ra <= 0f) return 0;
        return Color.argb(Math.round(ra * 255f), clamp(rr / ra), clamp(rg / ra), clamp(rb / ra));
    }

    private static int clamp(float v) { return Math.max(0, Math.min(255, Math.round(v * 255f))); }

    @Override public boolean equals(Object o) {
        return o instanceof PorterDuffColorFilter && ((PorterDuffColorFilter) o).color == color && ((PorterDuffColorFilter) o).mode == mode;
    }

    @Override public int hashCode() { return color * 31 + mode.hashCode(); }
}
