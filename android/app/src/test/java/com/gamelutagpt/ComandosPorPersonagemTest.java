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

}
