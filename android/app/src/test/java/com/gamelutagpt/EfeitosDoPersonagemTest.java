package com.gamelutagpt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;

/**
 * Os efeitos de vídeo de um personagem (SHINKAI, ondas, palhaço, Ichigeki, garça) só ficam na
 * memória enquanto ele está na luta: todos juntos eram ~300 MB a mais em toda luta.
 */
public class EfeitosDoPersonagemTest {
    private static EfeitosVideo.Folha folha(String nome) {
        Bitmap b = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888);
        return new EfeitosVideo.Folha(nome, b, 1f, new int[][] {{0, 0, 4, 4}}, new float[][] {{0f, 0f}}, new float[] {16f});
    }

    @Test
    public void oNomeDizDeQuemEOEfeito() {
        assertEquals("p02", EfeitosVideo.dono("shinkai_p02"));
        assertEquals("p16", EfeitosVideo.dono("palhaco_tela_p16"));
        assertEquals("player_base", EfeitosVideo.dono("onda_shinkai_player_base"));
        assertNull(EfeitosVideo.dono("impacto_ar_forte"));
        assertNull(EfeitosVideo.dono("dano_forte_anel_ec"));
        assertNull(EfeitosVideo.dono("defesa_brilho_spray_aneis_2"));
    }

    @Test
    public void trocarDeLutaSoltaOsEfeitosDeQuemSaiu() {
        EfeitosVideo e = new EfeitosVideo(null);
        EfeitosVideo.Folha comum = folha("impacto_ar"), daYinlu = folha("shinkai_p02"), doKoga = folha("onda_p03");
        e.adicionar(comum);
        e.adicionar(daYinlu);
        e.adicionar(doKoga);
        e.manter(new HashSet<>(Arrays.asList("p03", "p15")));
        assertTrue("o comum fica para todos", e.tem("impacto_ar"));
        assertTrue("o de quem luta fica", e.tem("onda_p03"));
        assertFalse("o de quem saiu sai", e.tem("shinkai_p02"));
        assertTrue("e o bitmap dele é solto", daYinlu.bitmap.isRecycled());
        assertFalse(doKoga.bitmap.isRecycled());
        assertFalse(comum.bitmap.isRecycled());
    }

    @Test
    public void umEfeitoTocandoDeQuemSaiuParaDeTocar() {
        EfeitosVideo e = new EfeitosVideo(null);
        e.adicionar(folha("shinkai_p02"));
        assertTrue(e.tocar("shinkai_p02", 0f, 0f, 1f, false));
        e.manter(new HashSet<>(Arrays.asList("p03")));
        // Sem a folha na vaga: avançar e desenhar não usa o bitmap solto.
        e.passo();
        assertFalse(e.tocar("shinkai_p02", 0f, 0f, 1f, false));
    }
}
