package com.gamelutagpt;

import static org.junit.Assert.*;

import com.gamelutagpt.historia.TelaComandos;
import java.util.List;
import org.junit.Test;

/**
 * Comandos próprios de cada personagem (tools/sprites/mecanicas.json, bloco "comandos"): a lista
 * da pausa mostra o que a proposta aprovada pede, e cada comando, feito no motor de verdade, solta
 * o golpe certo.
 */
public class ComandosPorPersonagemTest {
    /** Personagem, e de S1 a S4: nome e comando (como a lista mostra, sem os botões). */
    private static final String[][] TABELA = {
        {"player_base", "Onda", "↓ ↘ →", "Punho ascendente", "→ ↓ ↘", "Aparar", "↓ ↓", "Chute giratório", "↓ ↙ ←"},
        {"p02", "Palma", "↓ ↘ →", "Chute da garça", "[↓] ↑", "Contra-golpe", "← ↙ ↓", "Chute rodopiante", "[←] →"},
        {"p03", "Onda curta", "↓ ↘ →", "Punho em sequência", "→ ↓ ↘", "Avanço em três", "↓ ↙ ←", "Mergulho", "No ar: ↓ ↘ →"},
        {"p04", "Soco de impacto", "↓ ↘ →", "Cabeçada", "[←] →", "Esquiva", "↓ ↓", "Ombrada", "← ↙ ↓ ↘ →"},
        {"p05", "Kunai", "↓ ↘ →", "Shuriken", "↓ ↙ ←", "Selo e mudança", "↓ ↓", "Corte de mão", "← ↙ ↓ ↘ →"},
        {"p06", "Sombra rastejante", "↓ ↙ ←", "Eco de sombra", "↓ ↓", "Puxar", "→ ↘ ↓ ↙ ←", "Passo-sombra", "↓ ↘ →"},
        {"p07", "Tapa de luva", "↓ ↘ →", "Chute ascendente", "→ ↓ ↘", "Contra-golpe", "↓ ↙ ←", "Chute giratório baixo", "↓ ↓"},
        {"p08", "Chicote", "← ↙ ↓ ↘ →", "Giro ascendente", "[↓] ↑", "Contra-golpe", "↓ ↙ ←", "Salto-agulha", "↓ ↘ →"},
        {"p09", "Machado", "→ ↘ ↓ ↙ ←", "Cotovelada", "→ ↓ ↘", "Escudo", "↓ ↓", "Ombrada", "[←] →"},
        {"p10", "Machadinho", "↓ ↙ ←", "Jato de vapor", "→ ↓ ↘", "Esquiva acrobática", "↓ ↓", "Voadora em parafuso", "↓ ↘ →"},
        {"p11", "Bisturi", "↓ ↘ →", "Seringa", "↓ ↙ ←", "Bloqueio de nervos", "→ ↘ ↓ ↙ ←", "Passo cirúrgico", "[←] →"},
        {"p12", "Investida", "[←] →", "Salto de garras", "[↓] ↑", "Fúria", "↓ ↓", "Contorção", "↓ ↙ ←"},
    };

    @Test public void aListaMostraOsComandosAprovados() {
        for (String[] row : TABELA) {
            CharacterDefinition c = GeneratedCharacters.get(row[0]);
            List<TelaComandos.Linha> linhas = ListaComandos.ficha(c, Comandos.Esquema.TOQUE).linhas;
            for (int i = 0; i < 4; i++) {
                TelaComandos.Linha l = linhas.get(i);
                String esperado = row[2 + i * 2];
                boolean noAr = esperado.startsWith("No ar: ");
                assertEquals(row[0] + " S" + (i + 1), row[1 + i * 2], l.nome);
                assertEquals(row[0] + " S" + (i + 1), (noAr ? esperado.substring(7) : esperado) + " + L/M/H", l.comando);
                assertEquals(row[0] + " S" + (i + 1) + " no ar", noAr, l.noAr);
            }
            assertEquals(TelaComandos.SUPER, linhas.get(4).nome);
            assertEquals(TelaComandos.ULTRA, linhas.get(5).nome);
            assertEquals("↓ + SUPER", linhas.get(5).comando);
            assertEquals(TelaComandos.AGARRAO, linhas.get(6).nome);
        }
        // No teclado os botões viram as teclas.
        TelaComandos.Linha onda = ListaComandos.ficha(GeneratedCharacters.get("player_base"), Comandos.Esquema.TECLADO).linhas.get(0);
        assertEquals("↓ ↘ → + [J]/[K]/[L]", onda.comando);
    }

    @Test public void aListaComecaNoPersonagemDoJogador() {
        int[] atual = new int[1];
        String[] roster = {"player_base", "p03", "p02"};
        List<TelaComandos.Ficha> fichas = ListaComandos.fichas(roster, GeneratedCharacters.get("p03"), Comandos.Esquema.TOQUE, atual);
        assertEquals(3, fichas.size());
        assertEquals(1, atual[0]);
        fichas = ListaComandos.fichas(roster, GeneratedCharacters.get("p07"), Comandos.Esquema.TOQUE, atual);
        assertEquals("fora da seleção entra na frente", 4, fichas.size());
        assertEquals(0, atual[0]);
        assertEquals("p07", fichas.get(0).personagem);
    }

    // ------------------------------------------------------------ no motor

    /** Um lutador contra o boneco, longe um do outro (nenhum golpe encosta). */
    private static final class Luta {
        final CombatEngine engine;
        final FighterInput in = new FighterInput(), npc = new FighterInput();

        Luta(String id) {
            engine = new CombatEngine(new FighterState(GeneratedCharacters.get(id), "P1"),
                new FighterState(GeneratedCharacters.opponentCharacter(), "P2"), 500f, 1500f, new CombatConfig());
        }

        CombatFighter p() { return engine.fighter(0); }
        CombatConfig config() { return engine.config; }

        void step() {
            engine.step(in, npc);
            in.clearPresses();
        }

        void steps(int n) { for (int i = 0; i < n; i++) step(); }

        void press(String button) {
            if (button.equals("L")) in.light = true;
            if (button.equals("M")) in.medium = true;
            if (button.equals("H")) in.heavy = true;
        }

        /**
         * Faz o comando como o jogador (dois frames por direção; entre duas iguais, solta) e aperta
         * o botão na última.
         */
        void motion(String button, int... directions) {
            int last = -1;
            for (int d : directions) {
                if (d == last) {
                    in.direction = 0;
                    step();
                }
                in.direction = d;
                steps(2);
                last = d;
            }
            press(button);
            step();
        }

        /** Segura a primeira direção até carregar e aperta o botão junto com a segunda. */
        void hold(String button, int first, int second, int frames) {
            in.direction = first;
            steps(frames);
            in.direction = second;
            press(button);
            step();
        }

        /** O golpe que começou (ou "" se nenhum), esperando o buffer. */
        String started() {
            for (int i = 0; i < 6 && !p().attacking(); i++) step();
            return p().attacking() ? p().attack.id : "";
        }
    }

    /** Faz o comando de S1..S4 do personagem e devolve o golpe que saiu. */
    private static String fazer(String id, String key, String button) {
        Luta l = new Luta(id);
        CharacterDefinition c = l.p().character();
        int[] command;
        boolean hold, air = false;
        if (key.equals("S1")) {
            command = c.fighter.energyCommand;
            hold = c.fighter.energyHold;
        } else {
            CharacterDefinition.Special s = c.specials.get(key);
            command = s.command();
            hold = s.hold;
            air = s.air;
        }
        if (air) {
            l.in.direction = 7;
            l.step();
            l.in.direction = 0;
            l.steps(6);
            assertFalse(id + " " + key + ": no ar", l.p().grounded);
        }
        if (hold) l.hold(button, command[0], command[1], l.config().chargeFrames + 2);
        else l.motion(button, command);
        return l.started();
    }

    @Test public void cadaComandoSoltaOProprioGolpe() {
        for (String[] row : TABELA) {
            for (int i = 1; i <= 4; i++) {
                String key = "S" + i;
                String esperado = i == 1 ? "S" : key;
                for (String button : new String[] {"L", "M", "H"}) {
                    assertEquals(row[0] + " " + key + " (" + row[i * 2] + ") + " + button, esperado, fazer(row[0], key, button));
                }
            }
        }
    }

    @Test public void semCargaOSegurarEUmGolpeNormal() {
        Luta l = new Luta("p02");
        l.hold("M", 5, 1, l.config().chargeFrames / 3);
        assertEquals("pouco tempo atrás: → + M é o golpe alto, não o especial", GolpeAlto.BINDING, l.started());
        Luta solto = new Luta("p02");
        solto.in.direction = 5;
        solto.steps(solto.config().chargeFrames + 2);
        solto.in.direction = 0;
        solto.steps(solto.config().chargeKeepFrames + 2);
        solto.in.direction = 1;
        solto.press("M");
        solto.step();
        assertEquals("a carga acaba um pouco depois de soltar", GolpeAlto.BINDING, solto.started());
    }

    @Test public void segurarBaixoECimaNaoPula() {
        Luta l = new Luta("p02");
        l.hold("M", 3, 7, l.config().chargeFrames + 2);
        assertEquals("S2", l.started());
        assertTrue("o ↑ do [↓] ↑ não vira pulo", l.p().grounded);

        Luta pulo = new Luta("p02");
        pulo.in.direction = 3;
        pulo.steps(pulo.config().chargeFrames + 2);
        pulo.in.direction = 7;
        pulo.step();
        assertTrue("o pulo espera um instante pelo botão", pulo.p().grounded);
        pulo.steps(pulo.config().chargeJumpGraceFrames + 2);
        assertFalse("sem botão, pula", pulo.p().grounded);

        Luta semCarga = new Luta("player_base");
        semCarga.in.direction = 3;
        semCarga.steps(40);
        semCarga.in.direction = 7;
        semCarga.step();
        semCarga.step();
        assertFalse("quem não tem [↓] ↑ pula na hora", semCarga.p().grounded);
    }

    @Test public void oMergulhoSoSaiNoArEDesceAteOChao() {
        Luta chao = new Luta("p03");
        chao.motion("M", 3, 2, 1);
        assertEquals("no chão o ↓↘→ do p03 é a Onda curta", "S", chao.started());

        Luta ar = new Luta("p03");
        ar.in.direction = 7;
        ar.step();
        ar.in.direction = 0;
        ar.steps(6);
        ar.motion("H", 3, 2, 1);
        assertEquals("S4", ar.started());
        CombatFighter p = ar.p();
        AttackDefinition s4 = p.attack;
        float y = p.y, x = p.x;
        while (p.attackFrame < s4.startupFrames - 1) ar.step();
        assertEquals("parado no ar na preparação", y, p.y, 0.5f);
        int frames = 0;
        while (!p.grounded && frames++ < 120) ar.step();
        assertTrue("desce até o chão", p.grounded);
        assertTrue("na diagonal, para frente", p.x > x + 20f);
        ar.steps(ar.config().diveLandingFrames + 2);
        assertFalse("depois do pouso sobra pouca recuperação", p.attacking());

        Luta outro = new Luta("player_base");
        outro.in.direction = 7;
        outro.step();
        outro.in.direction = 0;
        outro.steps(6);
        outro.motion("M", 1, 3, 2);
        assertNotEquals("no ar o especial de chão não sai", "S2", outro.started());
    }

    @Test public void oComandoMaisLongoGanha() {
        Luta meiaLua = new Luta("p05");
        meiaLua.motion("M", 5, 4, 3, 2, 1);
        assertEquals("← ↙ ↓ ↘ → é o Corte de mão, não a Kunai", "S4", meiaLua.started());
        Luta kunai = new Luta("p05");
        kunai.motion("M", 3, 2, 1);
        assertEquals("S", kunai.started());

        // Defendendo (segurando ←) e então ↓↘→: um movimento ganha do segurar.
        Luta palma = new Luta("p02");
        palma.in.direction = 5;
        palma.steps(palma.config().chargeFrames + 2);
        palma.motion("M", 4, 3, 2, 1);
        assertEquals("a Palma, não o Chute rodopiante", "S", palma.started());
    }
}
