package com.gamelutagpt.historia;

import com.gamelutagpt.render.MiniJson;
import com.gamelutagpt.render.RenderAssets;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.render.RenderImage;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Um quadro de mangá avulso por cima da luta: entre as lições do tutorial, o Takatoshi e o
 * Dafu comentam o que vem. O quadro bate na tela como os painéis da história, os balões entram
 * digitando e o toque avança; depois do último balão o quadro sai e a luta continua.
 *
 * Os quadros ficam em {@code historia/tutorial/quadros.json}, um por id (o id da lição, ou
 * "fim"), no mesmo formato dos painéis dos episódios ({@code imagem}, {@code cena},
 * {@code personagens}, {@code baloes}, {@code efeito}). As imagens ficam na mesma pasta; sem a
 * imagem, a arte provisória da história (cenário e silhuetas do elenco).
 */
public final class QuadroTreino {
    public static final String PASTA = Temporada.PASTA + "/tutorial";
    public static final String ARQUIVO = PASTA + "/quadros.json";

    private static final float VW = PaginaHistoria.VW;
    private static final float VH = PaginaHistoria.VH;
    /** Caixa do quadro na tela. */
    static final float LEFT = 200f, TOP = 70f, RIGHT = 1080f, BOTTOM = 560f;
    private static final float SLAM = 0.16f;
    private static final float BALLOON_DELAY = 0.30f;
    private static final float POP = PaginaHistoria.BALLOON_POP;
    private static final float TYPE_SPEED = PaginaHistoria.TYPE_SPEED;
    /** Balão já escrito inteiro: passa sozinho depois desse tempo. */
    private static final float AUTO_NEXT = 3.2f;
    private static final float EXIT = 0.22f;
    private static final int INK = Baloes.INK;
    private static final int WHITE = Baloes.WHITE;

    private final RenderAssets assets;
    private final ArteHistoria arte = new ArteHistoria();
    private final Baloes baloes = new Baloes();
    private Map<String, Episodio.Painel> quadros = Collections.emptyMap();

    private Temporada temporada;
    private Episodio.Painel painel;
    private RenderImage image;
    private float time;
    /** Balão em foco (os anteriores ficam), -1 antes do primeiro. */
    private int balloon;
    private float balloonAt;
    private float exitAt = -1f;
    private long frame;

    public QuadroTreino(RenderAssets assets) {
        this.assets = assets;
    }

    /** Lê os quadros; sem o arquivo, não há quadros (o tutorial segue direto). */
    public void load() {
        try {
            quadros = fromJson(assets.readText(ARQUIVO));
        } catch (IOException ex) {
            quadros = Collections.emptyMap();
        }
    }

    public static Map<String, Episodio.Painel> fromJson(String json) {
        Map<String, Episodio.Painel> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : Episodio.asMap(MiniJson.parse(json)).entrySet()) {
            if (e.getKey().startsWith("_")) continue; // comentários
            out.put(e.getKey(), Episodio.painelDe(Episodio.asMap(e.getValue())));
        }
        return out;
    }

    public boolean has(String id) {
        return id != null && quadros.containsKey(id);
    }

    /** Mostra o quadro {@code id}; falso (e nada na tela) se ele não existe. */
    public boolean start(Temporada temporada, String id) {
        Episodio.Painel p = id == null ? null : quadros.get(id);
        if (p == null || temporada == null) return false;
        this.temporada = temporada;
        painel = p;
        image = null;
        if (p.imagem != null) {
            try {
                image = assets.loadImage(PASTA + "/" + p.imagem);
            } catch (IOException | RuntimeException ex) {
                image = null; // arte provisória
            }
        }
        time = 0f;
        balloon = -1;
        balloonAt = 0f;
        exitAt = -1f;
        return true;
    }

    public boolean active() {
        return painel != null;
    }

    public void stop() {
        painel = null;
        image = null;
    }

    public void update(float dt) {
        if (painel == null) return;
        frame++;
        time += dt;
        if (exitAt >= 0f) {
            if (time - exitAt >= EXIT) stop();
            return;
        }
        if (balloon < 0) {
            if (time >= SLAM + BALLOON_DELAY) nextBalloon();
            return;
        }
        if (typed() && time - balloonAt - POP - textLength() / TYPE_SPEED >= AUTO_NEXT) nextBalloon();
    }

    /** Toque: mostra o texto inteiro, passa o balão ou fecha o quadro. */
    public void tap() {
        if (painel == null || exitAt >= 0f || time < SLAM) return;
        if (balloon >= 0 && !typed()) {
            // Termina a digitação na hora.
            balloonAt = time - POP - textLength() / TYPE_SPEED;
            return;
        }
        nextBalloon();
    }

    private void nextBalloon() {
        if (balloon + 1 < painel.baloes.size()) {
            balloon++;
            balloonAt = time;
        } else {
            exitAt = time;
        }
    }

    private int textLength() {
        return balloon < 0 ? 0 : temporada.t(painel.baloes.get(balloon).texto).length();
    }

    private boolean typed() {
        return balloon >= 0 && (time - balloonAt - POP) * TYPE_SPEED >= textLength();
    }

    public void render(RenderCanvas c) {
        if (painel == null) return;
        float out = exitAt >= 0f ? Tinta.clamp01((time - exitAt) / EXIT) : 0f;
        float in = Tinta.clamp01(time / SLAM);
        c.fillRect(-40f, -40f, VW + 40f, VH + 40f, Tinta.withAlpha(0xFF000000, (int)(150f * in * (1f - out))));

        float cx = (LEFT + RIGHT) * 0.5f, cy = (TOP + BOTTOM) * 0.5f;
        float u = 1f - in;
        c.save();
        c.translate(0f, out * 760f);
        c.scale(1f + 0.25f * u * u, 1f + 0.25f * u * u, cx, cy);
        c.rotate(-3f * u * u + 1.2f, cx, cy);
        float[] poly = {LEFT, TOP, RIGHT, TOP + 10f, RIGHT - 8f, BOTTOM, LEFT + 6f, BOTTOM - 8f};
        float[] shadow = new float[poly.length];
        for (int i = 0; i < poly.length; i += 2) {
            shadow[i] = poly[i] + 10f;
            shadow[i + 1] = poly[i + 1] + 12f;
        }
        c.fillPolygon(shadow, 0x77000000);
        c.fillPolygon(poly, WHITE);
        c.save();
        c.clipPolygon(poly);
        float zoom = 1.02f + 0.05f * Tinta.clamp01(time / 4f);
        c.save();
        c.scale(zoom, zoom, cx, cy);
        if (image != null) {
            float scale = Math.max((RIGHT - LEFT) / image.width(), (BOTTOM - TOP) / image.height());
            float w = image.width() * scale, h = image.height() * scale;
            float x = LEFT + (RIGHT - LEFT - w) * 0.5f;
            float y = TOP + (BOTTOM - TOP - h) * Tinta.clamp01(painel.foco);
            c.drawImage(image, x, y, x + w, y + h, 255);
        } else {
            arte.drawScene(c, painel.cena, LEFT, TOP, RIGHT, BOTTOM);
            arte.drawCharacters(c, temporada, painel, LEFT, TOP, RIGHT, BOTTOM);
        }
        arte.drawEffect(c, painel.efeito, LEFT, TOP, RIGHT, BOTTOM, time, frame / 3);
        c.restore();
        c.restore();
        c.strokePolygon(poly, 8f, INK);
        if (image == null) arte.drawFileLabel(c, painel.imagem, LEFT, BOTTOM);
        float land = time - SLAM;
        if (land >= 0f && land < 0.12f) c.fillPolygon(poly, Tinta.withAlpha(WHITE, (int)(170f * (1f - land / 0.12f))));

        for (int j = 0; j <= balloon && j < painel.baloes.size(); j++) {
            Episodio.Balao b = painel.baloes.get(j);
            float age = j == balloon ? time - balloonAt : 99f;
            String texto = temporada.t(b.texto);
            int visible = (int)Math.max(0f, (age - POP) * TYPE_SPEED);
            if (age >= POP) visible = Math.max(visible, 1);
            Temporada.Personagem speaker = b.quem != null ? temporada.personagem(b.quem) : null;
            baloes.draw(c, b, texto, speaker, LEFT, TOP, RIGHT, BOTTOM, Math.min(visible, texto.length()),
                Tinta.clamp01(age / POP));
        }
        c.restore();
        if (balloon >= 0 && typed() && exitAt < 0f && (frame / 20) % 2 == 0) {
            c.drawText(temporada.t("toque para continuar") + " ▶", cx, BOTTOM + 52f, 22f, WHITE, INK, 4f, true);
        }
    }
}
