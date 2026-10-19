package com.gamelutagpt;

import com.gamelutagpt.historia.TelaComandos;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * As fichas da lista de COMANDOS (pausa) a partir dos dados de cada personagem: S1 (a energia),
 * S2 a S4, Super, Ultra, agarrão e o que vale para todos (golpe alto, defesa perfeita e combo breaker), com os
 * botões no esquema do jogador (toque, teclado ou controle).
 */
final class ListaComandos {
    private ListaComandos() {}

    /** Botões dos especiais: qualquer um dos três (o da energia escolhe a força). */
    private static final String BOTOES = "{L}/{M}/{H}";

    static TelaComandos.Ficha ficha(CharacterDefinition c, Comandos.Esquema esquema) {
        List<TelaComandos.Linha> linhas = new ArrayList<>();
        CharacterDefinition.Fighter f = c.fighter;
        if (f.energy != null && f.energyCommand.length > 0) {
            String nome = f.energyName != null ? f.energyName : "S1";
            linhas.add(linha(nome, TutorialDirector.commandText(f.energyCommand, f.energyHold, false), false, esquema));
        }
        for (Map.Entry<String, CharacterDefinition.Special> e : new TreeMap<>(c.specials).entrySet()) {
            CharacterDefinition.Special s = e.getValue();
            String nome = s.name != null ? s.name : e.getKey();
            linhas.add(linha(nome, TutorialDirector.commandText(s.command(), s.hold, false), s.air, esquema));
        }
        if (f.superAttack != null) {
            linhas.add(new TelaComandos.Linha(TelaComandos.SUPER, Comandos.texto("{SUPER}", esquema), false));
        }
        linhas.add(new TelaComandos.Linha(TelaComandos.ULTRA, Comandos.texto("↓ + {SUPER}", esquema), false));
        linhas.add(new TelaComandos.Linha(TelaComandos.AGARRAO, Comandos.agarraoCurto(esquema), false));
        if (c.moves.containsKey(GolpeAlto.BINDING)) {
            linhas.add(new TelaComandos.Linha(TelaComandos.GOLPE_ALTO, Comandos.texto("→ + {M}", esquema), false));
        }
        linhas.add(new TelaComandos.Linha(TelaComandos.DEFESA_PERFEITA, "←", false));
        linhas.add(new TelaComandos.Linha(TelaComandos.COMBO_BREAKER, Comandos.texto("{M} + {H}", esquema), false));
        return new TelaComandos.Ficha(c.displayName, linhas);
    }

    private static TelaComandos.Linha linha(String nome, String setas, boolean noAr, Comandos.Esquema esquema) {
        return new TelaComandos.Linha(nome, setas + " + " + Comandos.texto(BOTOES, esquema), noAr);
    }

    /**
     * As fichas na ordem da seleção; o personagem do jogador entra na lista (na frente, se não for
     * da seleção). {@code atual} recebe o índice dele.
     */
    static List<TelaComandos.Ficha> fichas(String[] roster, CharacterDefinition jogador, Comandos.Esquema esquema,
                                           int[] atual) {
        List<TelaComandos.Ficha> out = new ArrayList<>();
        atual[0] = -1;
        for (String id : roster) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            if (c == null) continue;
            if (c.id.equals(jogador.id)) atual[0] = out.size();
            out.add(ficha(c, esquema));
        }
        if (atual[0] < 0) {
            out.add(0, ficha(jogador, esquema));
            atual[0] = 0;
        }
        return out;
    }
}
