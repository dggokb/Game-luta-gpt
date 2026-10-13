package com.gamelutagpt;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

/** Toda lição do tutorial dá para fazer: a entrada de exemplo dela cumpre, no motor de verdade. */
public class TutorialDirectorTest {
    private static final float HOME = 420f;
    private static final int LESSON_LIMIT = 600;

    private static final class Treino {
        final FighterState[] team;
        final FighterState dummy = new FighterState(GeneratedCharacters.get("p03"), "CPU");
        final CombatEngine engine;
        final FighterInput p1 = new FighterInput(), npc = new FighterInput();
        final TutorialDirector tutorial = new TutorialDirector();

        Treino(String character) {
            team = new FighterState[] {
                new FighterState(GeneratedCharacters.get(character), "PLAYER 1"),
                new FighterState(GeneratedCharacters.get("p02"), "PLAYER 2")
            };
            engine = new CombatEngine(team[0], dummy, HOME, 980f, new CombatConfig());
            engine.setTeam(0, team);
        }

        void step(boolean example) {
            if (example) tutorial.fillExample(p1);
            else p1.clear();
            tutorial.fillDummy(npc);
            engine.step(p1, npc);
            tutorial.afterStep();
        }
    }

    private static void everyLessonCanBeDone(String character) {
        Treino t = new Treino(character);
        t.tutorial.start(t.engine, t.team, HOME);
        List<TutorialDirector.Lesson> lessons = TutorialDirector.lessons(t.engine.fighter(0).character());
        assertEquals(lessons.size(), t.tutorial.lessonCount());
        for (int i = 0; i < lessons.size(); i++) {
            assertEquals(i, t.tutorial.lessonIndex());
            int frames = 0;
            while (!t.tutorial.praising() && frames++ < LESSON_LIMIT) t.step(true);
            assertTrue(character + ": a lição " + lessons.get(i).title + " não foi cumprida pelo exemplo",
                t.tutorial.praising());
            while (t.tutorial.praising()) t.step(false);
        }
        assertTrue(t.tutorial.completing());
        for (int i = 0; i < TutorialDirector.COMPLETE_FRAMES; i++) t.step(false);
        assertTrue(t.tutorial.finished());
        assertFalse(t.tutorial.active());
        for (boolean done : t.tutorial.done()) assertTrue(done);
    }

    @Test public void lutadorBase() { everyLessonCanBeDone("player_base"); }
    @Test public void p02() { everyLessonCanBeDone("p02"); }
    @Test public void p03() { everyLessonCanBeDone("p03"); }

    @Test public void semFazerNadaNenhumaLicaoPassa() {
        Treino t = new Treino("player_base");
        t.tutorial.start(t.engine, t.team, HOME);
        int n = t.tutorial.lessonCount();
        boolean hurt = false;
        for (int i = 0; i < n; i++) {
            for (int f = 0; f < 400; f++) {
                t.step(false);
                hurt |= t.tutorial.reactions().contains("levou");
            }
            assertEquals(i, t.tutorial.lessonIndex());
            assertFalse("a lição " + t.tutorial.lesson().title + " passou sem o jogador fazer nada",
                t.tutorial.praising());
            assertEquals(0, t.tutorial.count());
            t.tutorial.skip();
        }
        assertTrue("parado na defesa, o jogador leva o golpe do boneco", hurt);
    }

    @Test public void pularVaiParaAProximaENaUltimaTermina() {
        Treino t = new Treino("player_base");
        t.tutorial.start(t.engine, t.team, HOME);
        int n = t.tutorial.lessonCount();
        for (int i = 1; i < n; i++) {
            t.tutorial.skip();
            assertEquals(i, t.tutorial.lessonIndex());
        }
        t.tutorial.skip();
        assertTrue(t.tutorial.completing());
        t.tutorial.skip();
        assertTrue(t.tutorial.finished());
        for (boolean done : t.tutorial.done()) assertFalse(done);
    }

    @Test public void ninguemCaiNoTreino() {
        Treino t = new Treino("player_base");
        t.tutorial.start(t.engine, t.team, HOME);
        for (int i = 0; i < 3000 && t.tutorial.active(); i++) {
            t.step(true);
            assertFalse(t.engine.fighter(0).ko());
            assertFalse(t.engine.fighter(1).ko());
        }
    }

    @Test public void comandosEmSetas() {
        assertEquals("↓ →", TutorialDirector.arrows(new int[] {3, 1}));
        assertEquals("→ ↓ ↘", TutorialDirector.arrows(new int[] {1, 3, 2}));
        assertEquals("↓ ←", TutorialDirector.arrows(new int[] {3, 5}));
    }

    @Test public void quadrosEntreAsLicoesEsperamOJogo() {
        Treino t = new Treino("player_base");
        t.tutorial.setPanels(true);
        t.tutorial.start(t.engine, t.team, HOME);
        assertEquals("andar", t.tutorial.panelWanted());
        for (int i = 0; i < 200; i++) t.step(true);
        assertEquals("com o quadro na tela a lição não anda", 0, t.tutorial.count());
        t.tutorial.panelDone();
        List<String> seen = new java.util.ArrayList<>();
        int n = t.tutorial.lessonCount();
        for (int lesson = 0; lesson < n; lesson++) {
            int frames = 0;
            while (t.tutorial.panelWanted() == null && t.tutorial.active() && frames++ < 2000) {
                t.step(true);
                seen.addAll(t.tutorial.reactions());
            }
            String wanted = t.tutorial.panelWanted();
            assertEquals(lesson + 1 < n ? TutorialDirector.lessons(t.engine.fighter(0).character()).get(lesson + 1).id : "fim",
                wanted);
            t.tutorial.panelDone();
        }
        for (int i = 0; i < TutorialDirector.COMPLETE_FRAMES; i++) t.step(false);
        assertTrue(t.tutorial.finished());
        assertTrue(seen.contains("licao"));
        assertTrue(seen.contains("impacto"));
    }

    @Test public void idsDasLicoesSaoUnicosESemAcento() {
        List<TutorialDirector.Lesson> lessons = TutorialDirector.lessons(GeneratedCharacters.get("player_base"));
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (TutorialDirector.Lesson l : lessons) {
            assertTrue(l.id, l.id.matches("[a-z0-9-]+"));
            assertTrue("id repetido: " + l.id, ids.add(l.id));
        }
        assertEquals("golpe-medio", TutorialDirector.idOf("GOLPE MÉDIO"));
        assertEquals("agarrao", TutorialDirector.idOf("AGARRÃO"));
    }
}
