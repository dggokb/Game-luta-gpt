package com.gamelutagpt;

import static org.junit.Assert.*;

import com.gamelutagpt.historia.CinematicaLuta;
import org.junit.Test;

/**
 * Os lutadores na cinemática da luta: os comandos do roteiro passam pelo motor de verdade
 * (troca, pulo, backdash e dash) e cada movimento termina sozinho.
 */
public class CinematicaDirectorTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");
    private static final CharacterDefinition TWO = GeneratedCharacters.get("player_two");

    /** Dupla contra dupla, como o treino do Episódio 1. */
    private static final class Match {
        final FighterState[] jogador = {new FighterState(BASE, "P1"), new FighterState(TWO, "P2")};
        final FighterState[] rival = {new FighterState(BASE, "CPU"), new FighterState(TWO, "CPU2")};
        final CombatEngine engine;
        final CinematicaDirector director = new CinematicaDirector();
        final FighterInput[] in = {new FighterInput(), new FighterInput()};

        Match(float xa, float xb) {
            engine = new CombatEngine(jogador[0], rival[0], xa, xb, new CombatConfig());
            engine.setTeam(0, jogador);
            engine.setTeam(1, rival);
        }

        CombatFighter f(int i) { return engine.fighter(i); }

        void mover(CinematicaLuta.Movimento.Tipo tipo, float distancia) {
            CinematicaLuta.Movimento m = new CinematicaLuta.Movimento(tipo, distancia, 4f);
            director.mover(0, m);
            director.mover(1, m);
        }

        /** Até os dois terminarem (ou o limite de quadros). */
        int rodar(int limite) {
            int n = 0;
            while ((director.movendo(0) || director.movendo(1)) && n++ < limite) {
                director.fill(engine, in[0], in[1]);
                engine.step(in[0], in[1]);
            }
            return n;
        }
    }

    /**
     * O golpe do QTE: quem bate corre até o rival e o golpe pega de verdade (o rival entra em
     * hitstun), com o dano do roteiro uma vez só, mesmo num golpe de várias batidas.
     */
    @Test public void golpeDoQteCorreBateETiraODanoDoRoteiro() {
        for (String golpe : new String[] {"M", "H", "2H", "S2", "S3"}) {
            Match m = new Match(800f, 1400f);
            FighterState alvo = m.f(1).state;
            int dano = alvo.profile.maxLife / 5;
            int antes = alvo.life;
            alvo.cenaDano = dano;
            m.director.golpear(0, new CinematicaLuta.Golpe(golpe, 0.2f, false));
            boolean apanhou = false;
            for (int i = 0; i < 400 && m.director.movendo(0); i++) {
                m.director.fill(m.engine, m.in[0], m.in[1]);
                m.engine.step(m.in[0], m.in[1]);
                apanhou |= m.f(1).inHitstun() || m.f(1).status == CombatFighter.Status.KNOCKDOWN;
            }
            assertFalse(golpe + ": o golpe acaba", m.director.movendo(0));
            assertTrue(golpe + ": o golpe pegou", apanhou);
            assertEquals(golpe + ": o dano do roteiro, uma vez só", antes - dano, alvo.life);
            alvo.cenaSemDano = false;
        }
    }

    /**
     * A troca de golpes entre os QTE, com os lances sorteados: os dois andam, os dois atacam,
     * quem defende segura a defesa e, com a proteção do jogo (o choque é golpe contra golpe),
     * ninguém perde vida. Acaba sozinha.
     */
    @Test public void trocaDeGolpesSemAcertar() {
        for (long semente = 1; semente <= 6; semente++) {
            Match m = new Match(900f, 1300f);
            int vida0 = m.f(0).state.life, vida1 = m.f(1).state.life;
            m.director.trocar(5f, semente);
            boolean[] atacou = new boolean[2];
            boolean defendeu = false, andou = false;
            float x0 = m.f(0).x;
            int quadros = 0;
            while (m.director.trocando() && quadros++ < 60 * 12) {
                for (int i = 0; i < 2; i++) m.f(i).state.cenaSemDano = true;
                m.director.fill(m.engine, m.in[0], m.in[1]);
                m.engine.step(m.in[0], m.in[1]);
                for (int i = 0; i < 2; i++) {
                    atacou[i] |= m.f(i).attacking();
                    defendeu |= m.f(i).status == CombatFighter.Status.BLOCKSTUN;
                }
                andou |= Math.abs(m.f(0).x - x0) > 40f;
            }
            assertFalse(semente + ": a troca acaba sozinha", m.director.trocando());
            assertTrue(semente + ": os dois atacam", atacou[0] && atacou[1]);
            assertTrue(semente + ": alguém defende", defendeu);
            assertTrue(semente + ": eles se movimentam", andou);
            assertEquals(semente + ": nenhum golpe pega", vida0, m.f(0).state.life);
            assertEquals(semente + ": nenhum golpe pega", vida1, m.f(1).state.life);
        }
    }

    /**
     * Cada lance da troca sai de verdade no motor de luta: o salto sai do chão, a esquiva pula
     * para trás com o forte passando, e defesa, vento, esquiva e salto não acertam ninguém
     * (sem a proteção do jogo).
     */
    @Test public void lancesDaTrocaSemAcertar() {
        String[][] roteiros = {{"bloqueio", "vento"}, {"esquiva", "esquiva"}, {"salto", "salto"}};
        for (String[] lances : roteiros) {
            Match m = new Match(900f, 1300f);
            int vida0 = m.f(0).state.life, vida1 = m.f(1).state.life;
            m.director.trocar(4f, 5L, java.util.Arrays.asList(lances));
            boolean pulou = false, defendeu = false, atacou = false;
            for (int q = 0; m.director.trocando() && q < 60 * 10; q++) {
                m.director.fill(m.engine, m.in[0], m.in[1]);
                m.engine.step(m.in[0], m.in[1]);
                for (int i = 0; i < 2; i++) {
                    pulou |= !m.f(i).grounded;
                    defendeu |= m.f(i).status == CombatFighter.Status.BLOCKSTUN;
                    atacou |= m.f(i).attacking();
                }
            }
            String nome = String.join("+", lances);
            assertFalse(nome + ": acaba", m.director.trocando());
            assertTrue(nome + ": alguém ataca", atacou);
            if (!lances[0].equals("bloqueio")) assertTrue(nome + ": alguém sai do chão", pulou);
            if (!lances[0].equals("esquiva")) assertTrue(nome + ": alguém defende", defendeu);
            assertEquals(nome + ": ninguém leva", vida0, m.f(0).state.life);
            assertEquals(nome + ": ninguém leva", vida1, m.f(1).state.life);
        }
    }

    /**
     * O lance do roteiro diz quem ataca e os golpes em ordem ("bloqueio:rival:L,H"), e o "fim"
     * encerra a troca logo depois, sem sortear mais nada (bem antes do tempo da troca).
     */
    @Test public void lanceDoRoteiroComQuemEGolpes() {
        Match m = new Match(900f, 1300f);
        m.director.trocar(8f, 5L, java.util.Arrays.asList("bloqueio:rival:L,H", "fim"));
        java.util.List<String> golpesDoRival = new java.util.ArrayList<>();
        boolean jogadorAtacou = false, defendeu = false;
        String antes = null;
        int q = 0;
        for (; m.director.trocando() && q < 60 * 8; q++) {
            for (int i = 0; i < 2; i++) m.f(i).state.cenaSemDano = true;
            m.director.fill(m.engine, m.in[0], m.in[1]);
            if (m.in[1].light) golpesDoRival.add("L");
            if (m.in[1].heavy) golpesDoRival.add("H");
            m.engine.step(m.in[0], m.in[1]);
            jogadorAtacou |= m.f(0).attacking();
            defendeu |= m.f(0).status == CombatFighter.Status.BLOCKSTUN;
        }
        assertFalse("acaba no fim", m.director.trocando());
        assertTrue("acaba antes do tempo da troca: " + q, q < 60 * 5);
        assertEquals("o rival solta o fraco e o forte, nessa ordem", java.util.Arrays.asList("L", "H"), golpesDoRival);
        assertFalse("o jogador só defende", jogadorAtacou);
        assertTrue("o jogador defende", defendeu);
    }

    /**
     * O choque: os dois soltam o forte e os golpes ficam ativos juntos, então os dois "apanham"
     * no mesmo instante (punho contra punho). Com a proteção do jogo, a vida não muda.
     */
    @Test public void choqueDeGolpes() {
        Match m = new Match(800f, 1400f);
        int vida0 = m.f(0).state.life, vida1 = m.f(1).state.life;
        m.director.trocar(2f, 7L, java.util.Arrays.asList("choque"));
        boolean[] apanhou = new boolean[2];
        for (int q = 0; m.director.trocando() && q < 60 * 8; q++) {
            // A proteção do GameView.protegerTroca.
            for (int i = 0; i < 2; i++) m.f(i).state.cenaSemDano = true;
            m.director.fill(m.engine, m.in[0], m.in[1]);
            m.engine.step(m.in[0], m.in[1]);
            for (int i = 0; i < 2; i++) apanhou[i] |= m.f(i).inHitstun();
        }
        assertTrue("o choque saiu", m.director.choques() >= 1);
        assertTrue("os dois golpes pegaram juntos", apanhou[0] && apanhou[1]);
        assertEquals(vida0, m.f(0).state.life);
        assertEquals(vida1, m.f(1).state.life);
    }

    /** Um golpe pedido no meio da troca interrompe ela. */
    @Test public void golpeInterrompeATroca() {
        Match m = new Match(900f, 1300f);
        m.director.trocar(5f, 3L);
        for (int i = 0; i < 30; i++) {
            m.director.fill(m.engine, m.in[0], m.in[1]);
            m.engine.step(m.in[0], m.in[1]);
        }
        m.director.golpear(0, new CinematicaLuta.Golpe("M", 0.1f, false));
        assertFalse(m.director.trocando());
        assertTrue(m.director.movendo(0));
    }

    /** O golpe final do QTE derruba de vez, com a vida que o rival tiver. */
    @Test public void golpeFinalDerruba() {
        Match m = new Match(800f, 1400f);
        FighterState alvo = m.f(1).state;
        alvo.cenaDano = alvo.life;
        m.director.golpear(0, new CinematicaLuta.Golpe("S2", 0f, true));
        for (int i = 0; i < 400 && m.director.movendo(0); i++) {
            m.director.fill(m.engine, m.in[0], m.in[1]);
            m.engine.step(m.in[0], m.in[1]);
        }
        assertTrue("KO", m.f(1).ko());
    }

    @Test public void pularParaTrasAfastaOsDoisEPousa() {
        Match m = new Match(1100f, 1350f);
        float antes = CinematicaDirector.vao(m.f(0), m.f(1));
        boolean noAr = false;
        m.mover(CinematicaLuta.Movimento.Tipo.PULAR_TRAS, -1f);
        for (int i = 0; i < 200 && (m.director.movendo(0) || m.director.movendo(1)); i++) {
            m.director.fill(m.engine, m.in[0], m.in[1]);
            m.engine.step(m.in[0], m.in[1]);
            noAr |= !m.f(0).grounded && !m.f(1).grounded;
        }
        assertTrue("os dois pularam", noAr);
        assertFalse(m.director.movendo(0));
        assertTrue(m.f(0).grounded && m.f(1).grounded);
        assertTrue("afastaram: " + antes + " → " + CinematicaDirector.vao(m.f(0), m.f(1)),
            CinematicaDirector.vao(m.f(0), m.f(1)) > antes + 250f);
    }

    @Test public void correrParaNaDistancia() {
        Match m = new Match(700f, 1600f);
        m.mover(CinematicaLuta.Movimento.Tipo.CORRER, 110f);
        boolean correndo = false;
        for (int i = 0; i < 240 && (m.director.movendo(0) || m.director.movendo(1)); i++) {
            m.director.fill(m.engine, m.in[0], m.in[1]);
            m.engine.step(m.in[0], m.in[1]);
            correndo |= m.f(0).forwardDashing && m.f(1).forwardDashing;
        }
        assertTrue("o dash do motor, não a caminhada", correndo);
        float vao = CinematicaDirector.vao(m.f(0), m.f(1));
        assertTrue("parou perto: " + vao, vao <= 110f && vao > 20f);
    }

    @Test public void recuarUsaOBackdash() {
        Match m = new Match(1100f, 1350f);
        float x = m.f(0).x;
        m.mover(CinematicaLuta.Movimento.Tipo.RECUAR, -1f);
        assertTrue(m.rodar(120) < 120);
        assertTrue("foi para trás", m.f(0).x < x - 150f);
    }

    /** No 2x2, quem estiver na vez troca com o titular (o irmão) pela troca do motor, sem esperar a recarga. */
    @Test public void titularVoltaParaAVez() {
        Match m = new Match(1000f, 1400f);
        // O rival trocou há pouco: o parceiro está na vez e a troca está recarregando.
        m.in[1].tag = true;
        m.engine.step(m.in[0], m.in[1]);
        m.in[1].clearPresses();
        for (int i = 0; i < 90; i++) m.engine.step(m.in[0], m.in[1]);
        assertSame(m.rival[1], m.f(1).state);
        assertTrue(m.engine.team(1).tagCooldown > 0);

        m.mover(CinematicaLuta.Movimento.Tipo.TITULAR, -1f);
        assertTrue("termina", m.rodar(300) < 300);
        assertSame("o titular voltou", m.rival[0], m.f(1).state);
        assertSame("o do jogador já estava na vez", m.jogador[0], m.f(0).state);
        assertFalse(m.engine.team(1).tagging());
    }

    /** TAG apertado no quadro do gatilho: descartado, o titular não sai da vez. */
    @Test public void tagPendenteNaoTiraOTitular() {
        Match m = new Match(1000f, 1400f);
        m.in[1].tag = true;
        m.engine.teams.readInput(m.f(1), m.in[1]);
        m.in[1].clearPresses();
        assertFalse("pedido pendente: ainda não terminou", m.engine.teams.starterOnPoint(1));
        m.engine.teams.dropRequests();
        m.mover(CinematicaLuta.Movimento.Tipo.TITULAR, -1f);
        m.rodar(10);
        for (int i = 0; i < 90; i++) {
            m.director.fill(m.engine, m.in[0], m.in[1]);
            m.engine.step(m.in[0], m.in[1]);
        }
        assertSame(m.rival[0], m.f(1).state);
        assertFalse(m.engine.team(1).tagging());
    }

    @Test public void pararCancela() {
        Match m = new Match(700f, 1600f);
        m.mover(CinematicaLuta.Movimento.Tipo.CORRER, 50f);
        assertTrue(m.director.movendo(0));
        m.mover(CinematicaLuta.Movimento.Tipo.PARAR, -1f);
        assertFalse(m.director.movendo(0));
        assertFalse(m.director.movendo(1));
    }
}
