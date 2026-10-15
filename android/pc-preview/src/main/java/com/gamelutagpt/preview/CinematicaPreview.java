package com.gamelutagpt.preview;

import com.gamelutagpt.historia.CinematicaLuta;
import com.gamelutagpt.historia.ConversaLuta;
import com.gamelutagpt.historia.Temporada;
import com.gamelutagpt.historia.Torneio;
import com.gamelutagpt.render.RenderCanvas;
import com.gamelutagpt.stage.StagePack;
import com.gamelutagpt.stage.StageScene;
import com.gamelutagpt.stage.StageWorld;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Grava em PNG uma cinemática de luta ({@link CinematicaLuta}) sem abrir o jogo: o cenário da
 * luta, dois bonecos nas cores dos irmãos movidos por uma física simplificada (o pulo, o
 * recuo e a corrida do jogo, sem animação), as faixas, as falas nos quadros da conversa e as
 * onomatopeias. A cena começa meio segundo depois, sem esperar o gatilho. Serve para revisar o
 * ritmo e o desenho; o movimento de verdade sai do motor de luta (gradle :pc:run).
 *
 * <pre>gradle :pc-preview:runCinematica -Pcinematica=ep01/cinematica_treino.json</pre>
 * Saída: build/cinematica/quadro_NNN.png (10 por segundo) e folha.png (resumo em grade).
 * {@code -Pirmao=koga} toca a campanha do Kōga; {@code -Pidioma=en}, outro idioma.
 */
public final class CinematicaPreview {
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final float DT = 1f / 60f;
    private static final float GROUND_Y = 565f;
    private static final float WORLD_WIDTH = 2600f;
    private static final float CAMERA_ZOOM = 1.24f;
    private static final float CAMERA_MIN_ZOOM = 0.78f;
    private static final float GROUND_SCREEN_Y = 552f;
    private static final float VW = StageWorld.VW;
    private static final float VH = StageWorld.VH;
    /** Meia largura do corpo (o vão da corrida conta de corpo a corpo). */
    private static final float MEIO_CORPO = 45f;

    /** Um boneco: posição, para onde olha e o movimento pedido pela cena. */
    private static final class Boneco {
        float x;
        float y = GROUND_Y;
        final int olha;
        final Color cor;
        CinematicaLuta.Movimento movimento;
        float desde;
        float inicioX;

        Boneco(float x, int olha, Color cor) {
            this.x = x;
            this.olha = olha;
            this.cor = cor;
        }
    }

    private final Boneco[] bonecos = new Boneco[2];
    private float time;
    private float zoom = CAMERA_ZOOM;
    private float left;
    private float top;

    private final CinematicaLuta.Palco palco = new CinematicaLuta.Palco() {
        @Override public void travarControle(boolean travar) {
            System.out.printf("%5.2fs controle %s%n", time, travar ? "travado" : "devolvido");
        }

        @Override public void mover(int lado, CinematicaLuta.Movimento m) {
            Boneco b = bonecos[lado];
            b.movimento = m.tipo == CinematicaLuta.Movimento.Tipo.PARAR ? null : m;
            b.desde = time;
            b.inicioX = b.x;
            System.out.printf("%5.2fs %s: %s%n", time, lado == 0 ? "jogador" : "rival", m.tipo);
        }

        @Override public boolean movendo(int lado) {
            return bonecos[lado].movimento != null;
        }

        @Override public void terminarLuta(String vencedor) {
            System.out.printf("%5.2fs fim da luta: %s%n", time, vencedor);
        }
    };

    private void mexer(Boneco b, Boneco outro) {
        CinematicaLuta.Movimento m = b.movimento;
        if (m == null) return;
        float t = time - b.desde;
        boolean acabou = t >= m.segundos;
        switch (m.tipo) {
            case TITULAR:
                // Sem dupla aqui: os bonecos já são os irmãos.
                acabou = true;
                break;
            case PULAR_TRAS: {
                // O pulo do jogo: ~1,1 s no ar, ~155 px para trás (medido no motor de luta).
                float k = Math.min(1f, t / 1.1f);
                b.x = b.inicioX - b.olha * 155f * k;
                b.y = GROUND_Y - 260f * (float)Math.sin(Math.PI * k);
                acabou |= k >= 1f;
                break;
            }
            case RECUAR: {
                float k = Math.min(1f, t / 0.25f);
                b.x = b.inicioX - b.olha * 175f * k;
                acabou |= t >= 0.45f;
                break;
            }
            default: {
                float vel = m.tipo == CinematicaLuta.Movimento.Tipo.CORRER ? 620f : 300f;
                if (m.distancia >= 0f && Math.abs(b.x - outro.x) - 2f * MEIO_CORPO <= m.distancia) {
                    acabou = true;
                } else {
                    b.x += b.olha * vel * DT;
                }
                break;
            }
        }
        b.x = Math.max(120f, Math.min(WORLD_WIDTH - 120f, b.x));
        if (acabou) {
            b.y = GROUND_Y;
            b.movimento = null;
        }
    }

    /** O enquadramento do CameraRig (sem suavização), com o zoom da cena ancorado no chão. */
    private void camera(float extra) {
        float separation = Math.abs(bonecos[0].x - bonecos[1].x);
        float zoomX = VW / Math.max(VW / CAMERA_ZOOM, separation + 520f);
        float base = Math.max(CAMERA_MIN_ZOOM, Math.min(CAMERA_ZOOM, zoomX));
        zoom = base * extra;
        float visible = VW / zoom;
        float center = Math.max(visible / 2f, Math.min(WORLD_WIDTH - visible / 2f, (bonecos[0].x + bonecos[1].x) / 2f));
        left = center - visible / 2f;
        top = GROUND_Y - GROUND_SCREEN_Y / zoom;
    }

    public static void main(String[] args) throws IOException {
        File assetsDir = new File(args.length > 0 ? args[0] : "app/src/main/assets");
        String arquivo = args.length > 1 ? args[1] : "ep01/cinematica_treino.json";
        File outDir = new File(args.length > 2 ? args[2] : "build/cinematica");
        String cenario = args.length > 3 ? args[3] : "templo_lua";
        if (!outDir.isDirectory() && !outDir.mkdirs()) throw new IOException("não consegui criar " + outDir);
        new CinematicaPreview().gravar(assetsDir, arquivo, outDir, cenario);
    }

    private void gravar(File assetsDir, String arquivo, File outDir, String cenario) throws IOException {
        FileRenderAssets assets = new FileRenderAssets(assetsDir);
        Temporada temporada = Temporada.load(assets);
        String idioma = System.getProperty("idioma", "pt");
        if (!temporada.setIdioma(assets, idioma)) System.out.println("sem tradução para " + idioma);
        String irmao = Torneio.KOGA.equals(System.getProperty("irmao")) ? Torneio.KOGA : Torneio.RYUGA;
        String rival = Torneio.KOGA.equals(irmao) ? Torneio.RYUGA : Torneio.KOGA;
        bonecos[0] = new Boneco(1120f, 1, new Color(temporada.personagem(irmao).cor, true));
        bonecos[1] = new Boneco(1420f, -1, new Color(temporada.personagem(rival).cor, true));

        StageScene scene = new StageScene(StagePack.load(new FileRenderAssets(new File(assetsDir, "stages")), cenario),
            new StageWorld(GROUND_Y, WORLD_WIDTH, CAMERA_ZOOM, 700f, GROUND_SCREEN_Y));
        ConversaLuta conversa = new ConversaLuta(assets);
        CinematicaLuta cena = new CinematicaLuta(assets, conversa, palco);
        if (!cena.carregar(temporada, arquivo, irmao)) throw new IOException("cinemática não abriu: " + arquivo);
        System.out.println(arquivo + " (" + irmao + ", gatilho " + cena.roteiro().gatilho.tipo + ")");

        java.util.List<BufferedImage> quadros = new java.util.ArrayList<>();
        int frame = 0;
        int depois = -1;
        while (frame < 60 * 40 && (depois < 0 || frame < depois)) {
            if (frame == 30) cena.comecar();
            mexer(bonecos[0], bonecos[1]);
            mexer(bonecos[1], bonecos[0]);
            cena.update(DT);
            conversa.update(DT);
            time += DT;
            // Depois do fim, mais um segundo para as faixas e a onomatopeia saírem.
            if (cena.acabou() && depois < 0) depois = frame + 60;
            if (frame % 6 == 0) {
                camera(cena.zoom());
                scene.setCamera(zoom, left, top);
                scene.update(DT * 6);
                BufferedImage img = desenhar(scene, cena, conversa);
                ImageIO.write(img, "png", new File(outDir, String.format("quadro_%03d.png", quadros.size())));
                quadros.add(img);
            }
            frame++;
        }
        folha(quadros, new File(outDir, "folha.png"));
        System.out.println(quadros.size() + " quadros em " + outDir.getAbsolutePath());
    }

    private BufferedImage desenhar(StageScene scene, CinematicaLuta cena, ConversaLuta conversa) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(WIDTH / VW, HEIGHT / VH);
        RenderCanvas canvas = new Java2DRenderCanvas(g, WIDTH, HEIGHT);
        scene.drawBackground(canvas);
        java.awt.geom.AffineTransform saved = g.getTransform();
        g.scale(zoom, zoom);
        g.translate(-left, -top);
        for (Boneco b : bonecos) boneco(g, b);
        g.setTransform(saved);
        scene.drawForeground(canvas);
        // A mesma ordem do jogo: faixas, quadros da conversa e onomatopeias por cima.
        cena.renderFaixas(canvas);
        conversa.render(canvas);
        cena.renderEfeitos(canvas);
        g.dispose();
        return image;
    }

    private static void boneco(Graphics2D g, Boneco b) {
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(Math.round(b.x - 45), Math.round(GROUND_Y - 10), 90, 20);
        g.setColor(b.cor);
        g.setStroke(new BasicStroke(22, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int fx = Math.round(b.x), fy = Math.round(b.y);
        g.drawLine(fx, fy - 70, fx, fy - 150);
        g.drawLine(fx, fy - 70, fx - 30, fy);
        g.drawLine(fx, fy - 70, fx + 30, fy);
        g.drawLine(fx, fy - 140, fx + b.olha * 45, fy - 110);
        g.fillOval(fx - 26, fy - 205, 52, 52);
    }

    /** Doze quadros espalhados pela cena, numa grade 3x4 em um quarto do tamanho. */
    private static void folha(java.util.List<BufferedImage> quadros, File file) throws IOException {
        int w = WIDTH / 4, h = HEIGHT / 4;
        BufferedImage folha = new BufferedImage(w * 3, h * 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = folha.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        for (int i = 0; i < 12 && !quadros.isEmpty(); i++) {
            BufferedImage q = quadros.get(Math.round(i * (quadros.size() - 1) / 11f));
            g.drawImage(q, (i % 3) * w, (i / 3) * h, w, h, null);
        }
        g.dispose();
        ImageIO.write(folha, "png", file);
    }
}
