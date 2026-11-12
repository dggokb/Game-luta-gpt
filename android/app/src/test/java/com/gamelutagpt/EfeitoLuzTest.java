package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Test;

/**
 * O poder luminoso é fonte de luz (docs/vfx-continuar.md, "Poder luminoso é fonte de luz"): o
 * efeito que emite luz declara {@code "luz": {"cor", "alcance", "forca"}} no efeito.json. Um
 * "luz" errado faria o efeito inteiro não carregar (o EfeitosVideo pula o que falha ao ler), sem
 * aviso na luta: aqui todos são lidos pela mesma regra do jogo.
 */
public class EfeitoLuzTest {
    /** A pasta dos efeitos: o teste roda na pasta do app ou na do pc (gradle :pc:testarMotor). */
    private static File pastaEfeitos() {
        for (String p : new String[] {"src/main/assets/efeitos", "../app/src/main/assets/efeitos"}) {
            File f = new File(p);
            if (f.isDirectory()) return f;
        }
        throw new AssertionError("pasta dos efeitos não achada");
    }

    private static EfeitosVideo.Folha folha(String nome) {
        return new EfeitosVideo.Folha(nome, null, 1f, new int[0][], new float[0][], new float[0]);
    }

    @Test
    public void todoLuzDeEfeitoLeEOsPoderesEFaiscasIluminam() throws Exception {
        File[] pastas = pastaEfeitos().listFiles(File::isDirectory);
        assertTrue(pastas != null && pastas.length > 0);
        int comLuz = 0;
        for (File pasta : pastas) {
            File json = new File(pasta, "efeito.json");
            if (!json.isFile()) continue;
            JSONObject dados = new JSONObject(new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8));
            JSONObject luz = dados.optJSONObject("luz");
            if (luz == null) continue;
            EfeitosVideo.Folha f = folha(pasta.getName());
            EfeitosVideo.lerLuz(f, luz, pasta.getName());
            assertTrue(pasta.getName(), f.luzForca > 0f && f.luzAlcance > 0f);
            comLuz++;
        }
        assertTrue("efeitos com luz: " + comLuz, comLuz >= 10);
        // Os que precisam emitir: os poderes do Ryūga e as faíscas de acerto que a luta toca.
        for (String nome : new String[] {"onda_player_base", "super_player_base", "impacto_v06_fraco",
            "impacto_v06_medio_ec", "acerto_forte_05_ec", "impacto_ar_super"}) {
            JSONObject d = new JSONObject(new String(Files.readAllBytes(
                new File(pastaEfeitos(), nome + "/efeito.json").toPath()), StandardCharsets.UTF_8));
            assertTrue(nome + " sem luz", d.has("luz"));
        }
        // Fumaça e poeira não emitem luz.
        for (String nome : new String[] {"fumaca_pouso", "fumaca_pulo", "poeira_corrida", "impacto_chao"}) {
            JSONObject d = new JSONObject(new String(Files.readAllBytes(
                new File(pastaEfeitos(), nome + "/efeito.json").toPath()), StandardCharsets.UTF_8));
            assertTrue(nome + " com luz", !d.has("luz"));
        }
    }

    @Test
    public void luzErradaNoEfeitoJsonERecusada() {
        String[] errados = {
            "{\"alcance\": 1, \"forca\": 0.5}",                      // sem cor
            "{\"cor\": \"laranja\", \"alcance\": 1, \"forca\": 0.5}", // cor sem #RRGGBB
            "{\"cor\": \"#FFAA00\", \"forca\": 0.5}",                 // sem alcance
            "{\"cor\": \"#FFAA00\", \"alcance\": 1, \"forca\": 1.5}", // força acima de 1 (nada soma)
        };
        for (String e : errados) {
            try {
                EfeitosVideo.lerLuz(folha("x"), new JSONObject(e), "x");
                fail(e);
            } catch (IllegalArgumentException esperado) {
                // recusado
            }
        }
    }

    @Test
    public void aLuzDoEfeitoNasceEApagaComEle() {
        assertEquals(0f, EfeitosVideo.vida(0f), 0f);
        assertEquals(1f, EfeitosVideo.vida(0.15f), 1e-6f);
        assertTrue(EfeitosVideo.vida(0.5f) > 0f && EfeitosVideo.vida(0.5f) < 1f);
        assertEquals(0f, EfeitosVideo.vida(1f), 0f);
        // A força registrada é a do efeito vezes a vida: nunca passa da força do efeito.
        EfeitosVideo.Folha f = folha("x");
        EfeitosVideo.lerLuz(f, new JSONObject("{\"cor\": \"#FF8000\", \"alcance\": 1.2, \"forca\": 0.8}"), "x");
        assertEquals(0xFF8000, f.luzCor);
        assertEquals(1.2f, f.luzAlcance, 1e-6f);
        assertEquals(0.8f, f.luzForca, 1e-6f);
        float maior = 0f;
        for (int i = 0; i <= 100; i++) maior = Math.max(maior, f.luzForca * EfeitosVideo.vida(i / 100f));
        assertTrue(maior <= f.luzForca + 1e-6f);
    }
}
