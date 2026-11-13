package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.Arrays;
import org.junit.Test;

/**
 * Combos de assinatura do Ryūga (12/11, docs/personagens/combos-assinatura.md): a base de todos
 * mais o soco duplo da casa (M > M), o Chute giratório que joga na parede, o Soco no plexo (→ + H)
 * e a Onda do assist, que segura o rival em pé para o parceiro recomeçar. Cada combo é apertado no
 * motor de verdade ({@link BateriaCombos#executar}) contra o Ryūga parado, e todas as pancadas têm
 * de emendar.
 *
 * <p>Passos: L, M, H, 2L, 6H (→ + H), S (Onda), S2, S3, SUPER, A (assist), SJ (super pulo), jL...,
 * W90 (espera 90 quadros), C160 (corre segurando → até ficar a 160 px do rival).
 */
public class ComboRyugaTest {
    private static final String RYUGA = "player_base", YINLU = "p02";

    private static BateriaCombos.Exec combo(String quem, String parceiro, String rota) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        return BateriaCombos.executar(quem, parceiro, Arrays.asList(rota.split(" ")), cn);
    }

    private static BateriaCombos.Exec ryuga(String rota) { return combo(RYUGA, YINLU, rota); }

    private static void emenda(String rota, BateriaCombos.Exec x, int acertos) {
        assertTrue(rota + ": o combo emenda", x.valida);
        assertEquals(rota + ": acertos", acertos, x.acertos);
    }

    @Test public void fundamento() {
        String rota = "2L 2L M S";
        emenda(rota, ryuga(rota), 4);
    }

    @Test public void socoDuploDaCasa() {
        // M > M só no Ryūga (a Yin-lu tem o dela, de chute); o segundo M entrega para o especial.
        for (String rota : new String[] {"L M M", "L M M S", "2L M M S2"}) {
            emenda(rota, ryuga(rota), rota.split(" ").length);
        }
        // De perto (o S2 é curto): de 90 a 135 px entre os dois, o S2 pega depois dos dois M.
        for (float gap : new float[] {90f, 110f, 125f, 135f}) {
            BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
            cn.gap = gap;
            BateriaCombos.Exec x = BateriaCombos.executar(RYUGA, YINLU, Arrays.asList("L", "M", "M", "S2"), cn);
            emenda("L M M S2 a " + gap + " px", x, 4);
        }
        AttackDefinition m = GeneratedCharacters.get(RYUGA).attack("M");
        assertEquals("M emenda no M uma vez", 2, m.maxUsesPerChain);
        assertTrue(m.cancelsInto("M"));
    }

    @Test public void chuteGiratorioLevaAParede() {
        // L, M, M, os quatro chutes do S3 jogam na parede; ele corre atrás e pega a volta no S2.
        AttackDefinition s3 = GeneratedCharacters.get(RYUGA).attack("S3");
        assertEquals(AttackDefinition.Launch.WALL_BOUNCE, s3.launch);
        String rota = "L M M S3 W90 C200 S2";
        BateriaCombos.Exec x = ryuga(rota);
        emenda(rota, x, 8);
        assertEquals("quicou na parede", 1, x.parede);
    }

    @Test public void chuteGiratorioPegaTodoMundoEmPe() {
        // 13/11 (o dono): o giro passava por cima da Yin-lu. Os chutes desceram (desenho 28 px mais
        // baixo, sem o voo de 30 px, caixa de 140 a 210 px): pega em pé até o mais baixo do elenco.
        for (String alvo : new String[] {"p02", "p05", "p06", "p12", "p13", "koga_8", "ryuga_8"}) {
            CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(RYUGA), GeneratedCharacters.get(alvo), 600f, 720f);
            CombatFighter me = s.f(0);
            me.commandButton = "M";
            me.buffer.push(InputBuffer.Button.SPECIAL, "S3", false);
            int acertos = 0;
            for (int i = 0; i < 110; i++) {
                s.step();
                for (CombatEngine.HitEvent h : s.engine.events()) if (h.attacker == 0 && !h.blocked) acertos++;
            }
            assertEquals("os 4 chutes em " + alvo, 4, acertos);
        }
    }

    @Test public void puniçãoNaVoltaDaParede() {
        // O H joga na parede: correndo atrás, o S2, o lançador com o combo no ar, ou o Super.
        BateriaCombos.Exec s2 = ryuga("L M H C160 S2");
        emenda("L M H C160 S2", s2, 4);
        BateriaCombos.Exec ar = ryuga("L M H C160 2H SJ jL jM j2H");
        emenda("L M H C160 2H SJ jL jM j2H", ar, 7);
        assertEquals("o j2H quica no chão", 1, ar.chao);
        BateriaCombos.Exec sup = ryuga("L M H C SUPER");
        emenda("L M H C SUPER", sup, 8);
    }

    @Test public void socoNoPlexo() {
        // → + H: dois socos que emendam em qualquer especial; o mais forte sem barra.
        for (String rota : new String[] {"2L 6H S2", "L M M 6H S2"}) {
            BateriaCombos.Exec x = ryuga(rota);
            emenda(rota, x, rota.split(" ").length + 1);   // o 6H são dois acertos
        }
        emenda("2L 6H S2 SUPER", ryuga("2L 6H S2 SUPER"), 9);
        AttackDefinition plexo = GeneratedCharacters.get(RYUGA).attack("6H");
        assertEquals("dois socos", 2, plexo.maxHits);
        assertTrue(plexo.cancelsInto("S2") && plexo.cancelsInto("S") && plexo.cancelsInto("S3") && plexo.cancelsInto("SUPER"));
        assertEquals("só cancela depois do segundo soco", plexo.hitbox(1).firstFrame, plexo.hitWindow.start);
    }

    @Test public void maisHComFrenteSoViraPlexoOndeEmenda() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get(RYUGA), GeneratedCharacters.get(RYUGA), 600f, 1400f);
        CombatFighter me = s.f(0);
        // Do neutro, → + H é o Soco no plexo.
        s.in[0].direction = 1;
        s.in[0].heavy = true;
        s.step();
        s.in[0].direction = 0;
        s.until(me::attacking, 10);
        assertEquals("6H", me.attack.id);
        // Sem o golpe no pacote (o Kōga, por enquanto), → + H continua sendo o H.
        CombatEngineTest.Sim y = new CombatEngineTest.Sim(GeneratedCharacters.get("p03"), GeneratedCharacters.get(RYUGA), 600f, 1400f);
        y.in[0].direction = 1;
        y.in[0].heavy = true;
        y.step();
        y.in[0].direction = 0;
        y.until(y.f(0)::attacking, 10);
        assertEquals("H", y.f(0).attack.id);
        // Dentro de um golpe que só emenda no H (o 2M), → + H continua sendo o H.
        AttackDefinition rasteira = GeneratedCharacters.get(RYUGA).attack("2M");
        assertTrue(rasteira.cancelsInto("H") && !rasteira.cancelsInto("6H"));
    }

    /** O executor começando a {@code gap} px do rival. */
    private static BateriaCombos.Exec perto(String rota, float gap) {
        BateriaCombos.Cenario cn = new BateriaCombos.Cenario();
        cn.barras = 3;
        cn.gap = gap;
        return BateriaCombos.executar(RYUGA, YINLU, Arrays.asList(rota.split(" ")), cn);
    }

    @Test public void comboCurtoEmendaEmOutroCombo() {
        // O pedido de 12/11: combo curto, mas dá para emendar outro depois. A parede do S3, o
        // assist no meio e a troca no Super (DHC) recomeçam; e, depois do ↓jH que quica no chão,
        // quem cravou desce rápido e pega o rival ainda quicando (relança ou recomeça no chão).
        emenda("parede do S3 → combo novo", perto("L M M S3 W90 C200 L M M 6H S2", 100f), 12);
        // O assist da Yin-lu é o chute que lança (12/11): ele sobe atrás e continua no ar.
        emenda("assist no meio", perto("L M M A W25 SJ jL jM J jL jM j2H", 100f), 9);
        emenda("Super → DHC", perto("2L 6H S2 SUPER DHC", 100f), 14);
        emenda("↓jH → relança", perto("2L M 2H SJ jL jM j2H 2H SJ jL jM j2H", 100f), 10);
        emenda("↓jH → recomeça no chão", perto("2L M 2H SJ jL jM j2H C140 L M", 100f), 8);
    }

    @Test public void aOndaDoAssistSeguraParaOParceiroRecomecar() {
        // A Yin-lu em campo chama o Ryūga: a Onda dele segura o rival em pé, ela corre e recomeça.
        String rota = "L M A W20 C140 L M M S4";
        emenda(rota, combo(YINLU, RYUGA, rota), 11);   // o S4 são cinco chutes (13/11)
        CharacterDefinition.Fighter f = GeneratedCharacters.get(RYUGA).fighter;
        AttackDefinition segura = f.assistShot;
        assertNotNull("o assist do Ryūga tem a Onda que segura", segura);
        assertTrue("segura mais que a Onda dele", segura.hitstunFrames > f.energy.attack.hitstunFrames + 20);
        // Controle: com a Onda de sempre, o rival se solta antes de ela chegar.
        f.assistShot = null;
        try {
            BateriaCombos.Exec sem = combo(YINLU, RYUGA, rota);
            assertTrue("sem segurar, a corrente nova não emenda: " + sem.acertos, !sem.valida || sem.acertos < 11);
        } finally {
            f.assistShot = segura;
        }
    }
}
