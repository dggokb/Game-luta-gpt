package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Protótipo do SHINKAI individual do Ryūga (PERCEPCAO; docs/balanco/shinkai-ryuga.md): dura pelo
 * desgaste, acaba quando ele quer, no limite rompe (Ruptura), a defesa perfeita vira leitura e o
 * backdash esquiva, e o golpe seguinte é o golpe preciso.
 */
public class ShinkaiRyugaTest {
    private static final class Match {
        final FighterState ryuga, rival;
        final CombatEngine engine;
        final FighterInput[] in = {new FighterInput(), new FighterInput()};
        boolean viu;

        Match(String outro, float xa, float xb) {
            ryuga = new FighterState(GeneratedCharacters.get("player_base"), "P1");
            rival = new FighterState(GeneratedCharacters.get(outro), "CPU");
            engine = new CombatEngine(ryuga, rival, xa, xb, new CombatConfig());
        }

        CombatFighter f(int i) { return engine.fighter(i); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in[0], in[1]);
            in[0].clearPresses();
            in[1].clearPresses();
        }

        /** Anda até ver o aviso (true) ou acabar os quadros. */
        boolean ate(String cue, int n) {
            for (int i = 0; i < n; i++) {
                step();
                if (engine.cues().contains(cue)) return true;
            }
            return false;
        }

        void shinkai() {
            ryuga.shinkai = CombatConfig.SHINKAI_MAX;
            in[0].shinkai = true;
            step();
            assertTrue(engine.cues().contains("SHINKAI"));
            for (int i = 0; i < config().shinkaiFlashFrames; i++) step();
        }
    }

    @Test public void oRyugaTemOPrototipoEOsOutrosOSistemaAntigo() {
        assertEquals(Shinkai.Type.PERCEPCAO, Shinkai.typeOf("player_base"));
        assertEquals(Shinkai.Type.POWER, Shinkai.typeOf("p13"));
        Shinkai.prototipoDesligado = true;
        try {
            assertEquals(Shinkai.Type.ENERGY, Shinkai.typeOf("player_base"));
        } finally {
            Shinkai.prototipoDesligado = false;
        }
    }

    @Test public void paradoChegaAoLimiteEmUns12SegundosEAcabaExausto() {
        Match m = new Match("p15", 300f, 1500f);
        m.shinkai();
        assertEquals(Shinkai.Type.PERCEPCAO, m.ryuga.shinkaiType);
        int quadros = 0;
        while (!m.engine.cues().contains("LIMITE") && quadros < 2000) {
            m.step();
            quadros++;
        }
        assertTrue("chegou ao limite em " + quadros, quadros > 600 && quadros < 800);
        assertTrue("no limite ainda está no SHINKAI (a janela da Ruptura)", m.ryuga.inShinkai());
        assertTrue(m.ate("LIMITE_FIM", m.config().ryugaLimiteJanela + 2));
        assertFalse(m.ryuga.inShinkai());
        assertEquals(m.config().ryugaExaustao, m.ryuga.exaustaoQuadros, 2);
        assertTrue("acabou no limite: não liga de novo no round", m.ryuga.shinkaiUsed);
    }

    @Test public void golpesGastamOCorpoMaisRapido() {
        Match parado = new Match("p15", 300f, 1500f), batendo = new Match("p15", 300f, 1500f);
        parado.shinkai();
        batendo.shinkai();
        for (int i = 0; i < 240; i++) {
            if (batendo.f(0).canAct()) batendo.in[0].light = true;
            batendo.step();
            parado.step();
        }
        assertTrue(batendo.ryuga.sobrecarga > parado.ryuga.sobrecarga + 10_000);
    }

    @Test public void desligarAntesDoLimiteNaoCansaEDevolveParteDaBarra() {
        Match m = new Match("p15", 300f, 1500f);
        m.shinkai();
        for (int i = 0; i < 120; i++) m.step();
        int sobra = CombatConfig.RYUGA_SOBRECARGA_MAX - m.ryuga.sobrecarga;
        m.in[0].shinkai = true;
        assertTrue(m.ate("SHINKAI_FIM", 3));
        assertFalse(m.ryuga.inShinkai());
        assertEquals(0, m.ryuga.exaustaoQuadros);
        assertFalse("pode ligar de novo quando a barra encher", m.ryuga.shinkaiUsed);
        int esperado = (int)((long)sobra * CombatConfig.SHINKAI_MAX / CombatConfig.RYUGA_SOBRECARGA_MAX / 2);
        assertEquals(esperado, m.ryuga.shinkai, 20);
        // Liga de novo: o Super grátis foi só da primeira vez.
        m.shinkai();
        assertTrue(m.ryuga.inShinkai());
        assertFalse(m.ryuga.shinkaiFreeSuper);
    }

    @Test public void noLimiteORupturaDoiMasNuncaDerrubaEDepoisCansaMais() {
        Match m = new Match("p15", 300f, 1500f);
        m.shinkai();
        assertTrue(m.ate("LIMITE", 2000));
        m.ryuga.life = 300;   // a dor nunca tira a última vida
        m.in[0].shinkai = true;
        assertTrue(m.ate("RUPTURA", 3));
        assertTrue(m.ryuga.emRuptura());
        assertEquals(Shinkai.COR_RUPTURA, m.ryuga.corShinkai());
        assertTrue(m.ate("RUPTURA_FIM", m.config().ryugaRupturaQuadros + 20));
        assertTrue(m.ryuga.life > 0 && m.ryuga.life <= 300);
        assertFalse(m.ryuga.inShinkai());
        assertTrue(m.ryuga.exaustaoQuadros > m.config().ryugaExaustao);
        assertEquals(1, m.ryuga.rupturas);
    }

    /** O rival aperta M; o Ryūga aperta trás em cima da hora (até 12 quadros antes). */
    private static void leGolpe(Match m, int antes) {
        m.in[1].medium = true;
        boolean apertou = false;
        for (int i = 0; i < 60 && !m.engine.cues().contains("LEITURA"); i++) {
            CombatFighter r = m.f(1);
            if (!apertou && r.attacking() && r.attack.startupFrames - r.attackFrame <= antes) apertou = true;
            if (apertou) m.in[0].direction = 5;   // trás (ele olha para a direita)
            m.step();
        }
    }

    @Test public void aDefesaPerfeitaViraLeituraEOGolpeSeguinteEPreciso() {
        Match m = new Match("p15", 600f, 700f);
        m.shinkai();
        leGolpe(m, 10);   // 10 quadros antes: fora do SHINKAI (5) não seria perfeita
        assertTrue("leu o golpe", m.engine.cues().contains("LEITURA"));
        assertEquals(1, m.ryuga.leituras);
        assertTrue("quem atacou ficou desequilibrado", m.f(1).inHitstun());
        assertTrue(m.ryuga.precisoQuadros > 0);
        m.in[0].direction = 0;
        boolean preciso = false;
        for (int i = 0; i < 40 && !preciso; i++) {
            if (m.f(0).canAct()) m.in[0].light = true;
            m.step();
            preciso = m.engine.cues().contains("PRECISO");
        }
        assertTrue("o golpe seguinte é o golpe preciso", preciso);
        assertEquals(1, m.ryuga.precisos);
    }

    @Test public void noShinkaiADefesaComumTambemLeUmPouco() {
        Match m = new Match("p15", 600f, 700f);
        m.shinkai();
        // ↙ segurado bem antes do golpe (defende agachado sem andar): defesa comum, não perfeita.
        for (int i = 0; i < 30; i++) {
            m.in[0].direction = 4;
            m.step();
        }
        // 2M do rival (baixo: o M em pé passa por cima de quem está agachado).
        m.in[1].medium = true;
        StringBuilder visto = new StringBuilder();
        for (int i = 0; i < 60 && !m.engine.cues().contains("LEITURA"); i++) {
            m.in[0].direction = 4;
            if (i < 4) m.in[1].direction = 4;   // ↓ para o rival (ele olha para a esquerda: ↙ é baixo-frente)
            else m.in[1].direction = 0;
            m.step();
            for (CombatEngine.HitEvent h : m.engine.events()) visto.append(h.moveId).append(h.blocked ? " defendido " : " acertou ");
        }
        assertTrue(visto.toString(), m.engine.cues().contains("LEITURA"));
        assertFalse("não foi a perfeita", m.engine.cues().contains("JUST_DEFENSE"));
        assertTrue("quem atacou ficou desequilibrado", m.f(1).inHitstun());
        assertTrue(m.f(1).stunLeft <= m.config().ryugaLeituraLeveAbre);
    }

    @Test public void foraDoShinkaiAMesmaDefesaNaoEPerfeita() {
        Match m = new Match("p15", 600f, 700f);
        leGolpe(m, 10);
        assertFalse(m.engine.cues().contains("LEITURA"));
        assertEquals(0, m.ryuga.leituras);
    }

    @Test public void paradoEleEsquivaSozinhoComORastroERecarrega() {
        Match m = new Match("p15", 600f, 700f);
        m.shinkai();
        m.in[1].medium = true;
        boolean esquivou = false;
        for (int i = 0; i < 60 && !esquivou; i++) {
            m.step();
            esquivou = m.engine.cues().contains("ESQUIVA");
            for (CombatEngine.HitEvent e : m.engine.events()) assertFalse("não apanhou", e.defender == 0 && !e.blocked);
        }
        assertTrue("o golpe passou direto", esquivou);
        assertTrue("recuou no backdash", m.f(0).backdashFrames > 0);
        assertTrue("rastro dourado", m.ryuga.rastroQuadros > 0);
        assertTrue("golpe preciso pronto", m.ryuga.precisoQuadros > 0);
        assertEquals(m.config().ryugaEsquivaAutoRecarga, m.ryuga.esquivaRecarga, 2);
        // Logo depois, sem recarga, o golpe seguinte entra (os dois de volta ao lugar).
        for (int i = 0; i < 40; i++) m.step();
        assertTrue(m.ryuga.esquivaRecarga > 0);
        m.f(0).x = 600f;
        m.f(1).x = 700f;
        m.in[1].medium = true;
        boolean apanhou = false;
        for (int i = 0; i < 60 && !apanhou; i++) {
            m.step();
            assertFalse(m.engine.cues().contains("ESQUIVA"));
            for (CombatEngine.HitEvent e : m.engine.events()) apanhou |= e.defender == 0;
        }
        assertTrue("sem recarga não esquiva de novo", apanhou);
    }

    @Test public void segurandoADefesaEleNaoEsquivaSozinho() {
        Match m = new Match("p15", 600f, 700f);
        m.shinkai();
        leGolpe(m, 10);
        assertEquals(0, m.ryuga.esquivas);
        assertEquals(1, m.ryuga.leituras);
    }

    @Test public void oBackdashNoShinkaiPassaPeloGolpe() {
        Match m = new Match("p15", 600f, 700f);
        m.shinkai();
        m.in[1].heavy = true;
        boolean esquivou = false;
        for (int i = 0; i < 60 && !esquivou; i++) {
            CombatFighter r = m.f(1);
            if (r.attacking() && r.attack.startupFrames - r.attackFrame == 3) m.in[0].backdash = true;
            m.step();
            esquivou = m.engine.cues().contains("ESQUIVA");
            for (CombatEngine.HitEvent e : m.engine.events()) assertFalse("não apanhou", e.defender == 0 && !e.blocked);
        }
        assertTrue(esquivou);
        assertTrue(m.ryuga.precisoQuadros > 0);
    }
}
