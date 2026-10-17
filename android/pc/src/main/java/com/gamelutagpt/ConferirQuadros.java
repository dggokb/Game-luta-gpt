package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.gamelutagpt.pc.DadosDoJogo;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;

/**
 * Desenha cada quadro de cada folha pelo SpriteFighterRenderer (o mesmo caminho da luta) e
 * guarda uma assinatura dos pixels: duas rodadas (antes/depois de mudar as folhas ou a
 * decodificação) mostram exatamente quais quadros mudaram.
 *
 * <pre>
 * gradle :pc:conferirQuadros -Psaida=antes.txt [-Pquem=p02,p03] [-Pzoom=1.24] [-Ppng=pasta]
 * </pre>
 * Com {@code png}, também salva cada quadro (para comparar a olho).
 */
public final class ConferirQuadros {
    private ConferirQuadros() {}

    public static void main(String[] args) throws Exception {
        String saida = args[0];
        String quem = args.length > 1 && !args[1].isEmpty() ? args[1] : null;
        float zoom = args.length > 2 ? Float.parseFloat(args[2]) : 1f;
        File png = args.length > 3 && !args[3].isEmpty() ? new File(args[3]) : null;
        if (png != null) png.mkdirs();
        Context contexto = DadosDoJogo.localizar().contexto(null);
        List<String> ids = new ArrayList<>();
        if (quem != null) {
            for (String id : quem.split(",")) ids.add(id.trim());
        } else {
            for (String id : GeneratedCharacters.SELECTABLE) ids.add(id);
            ids.add(GeneratedCharacters.OPPONENT);
        }
        SpriteAtlasCache cache = new SpriteAtlasCache(contexto);
        long quadros = 0;
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(new File(saida).toPath()))) {
            for (String id : ids) {
                CharacterDefinition c = GeneratedCharacters.get(id);
                SpriteFighterRenderer r = new SpriteFighterRenderer(cache, c);
                Map<String, CharacterDefinition.Atlas> folhas = new LinkedHashMap<>();
                for (CharacterDefinition.Animation a : c.animations.values()) folhas.putIfAbsent(a.atlas.resource, a.atlas);
                for (CharacterDefinition.Atlas a : folhas.values()) {
                    float s = c.profile.worldScale / a.pixelScale * zoom;
                    int w = (int) Math.ceil(a.width * s) + 16, h = (int) Math.ceil(a.height * s) + 16;
                    for (int f = 0; f < a.count; f++) {
                        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(b);
                        canvas.scale(zoom, zoom);
                        r.drawAtlasFrame(canvas, a, f, (a.rootX * s + 8) / zoom, (a.rootY * s + 8) / zoom);
                        int[] px = new int[w * h];
                        b.getPixels(px, 0, w, 0, 0, w, h);
                        CRC32 crc = new CRC32();
                        for (int p : px) {
                            crc.update(p >>> 24); crc.update(p >>> 16); crc.update(p >>> 8); crc.update(p);
                        }
                        out.printf(Locale.ROOT, "%s %d %dx%d %08x%n", a.resource, f, w, h, crc.getValue());
                        if (png != null) ImageIO.write(b.image(), "png", new File(png, a.resource + "_" + f + ".png"));
                        b.recycle();
                        quadros++;
                    }
                }
                cache.retainOnly();
                System.out.printf(Locale.ROOT, "%s: %d folhas%n", id, folhas.size());
            }
        }
        System.out.println(quadros + " quadros em " + saida);
        System.exit(0);
    }
}
