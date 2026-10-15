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
