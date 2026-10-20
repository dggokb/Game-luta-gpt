package com.gamelutagpt;

import java.util.HashMap;
import java.util.Map;

/**
 * Olha a luta a cada quadro e diz ao narrador ({@link com.gamelutagpt.historia.Narracao}) o que
 * aconteceu: o começo (GO), o primeiro golpe, os combos, Super e Ultra, guard break, combo
 * breaker, defesa perfeita, agarrão e punição, a troca de lutador, quem está em apuros, quem
 * domina e a virada, os 10 segundos finais, o K.O. (e o K.O. com o Ultra), a vitória, o
 * perfeito, o tempo esgotado e o empate. Quem escolhe a fala e decide se ela entra é o
 * narrador (prioridade e silêncio); aqui só se detecta o evento. Java puro.
 */
final class NarradorLuta {
    /** Quem recebe os eventos (o narrador no jogo; uma lista nos testes). */
    interface Voz {
        void narrar(String evento, Map<String, String> vars);
    }

    /** Combo que vale comentário e combo grande (acertos). */
    static final int COMBO = 5, COMBO_GRANDE = 10;
    /** Vida (por mil) abaixo da qual o titular está em apuros. */
    static final int APUROS = 250;
    /** Diferença de vida (por mil) que conta como dominar e a que confirma uma liderança. */
    static final int DOMINA = 400, LIDERA = 150;

    /**
     * Nome de quem luta com cada pack (o do elenco da história, quando tem); sem ele, o nome
     * do pack.
     */
    private java.util.function.Function<CharacterDefinition, String> nomes = c -> c.displayName;

    void setNomes(java.util.function.Function<CharacterDefinition, String> nomes) {
        this.nomes = nomes != null ? nomes : c -> c.displayName;
    }

    private BattleFlow.Phase fase;
    private boolean comecou, primeiroGolpe, tempoAvisado, dominouAvisado, acabou;
    private final Object[] ultimaSessao = new Object[2];
    private final AttackDefinition[] ultimoGolpe = new AttackDefinition[2];
    private final boolean[] emUltra = new boolean[2];
    private final boolean[] caido = new boolean[2];
    private final FighterState[] titular = new FighterState[2];
    private final java.util.Set<FighterState> emApuros = new java.util.HashSet<>();
    /** Quem lidera por vida (-1: ninguém com folga ainda). */
    private int lider = -1;

    /** Luta nova: tudo de novo. */
    void reset() {
        fase = null;
        comecou = primeiroGolpe = tempoAvisado = dominouAvisado = acabou = false;
        for (int i = 0; i < 2; i++) {
            ultimaSessao[i] = null;
            ultimoGolpe[i] = null;
            emUltra[i] = false;
            caido[i] = false;
            titular[i] = null;
        }
        emApuros.clear();
        lider = -1;
    }

    /** Um quadro, depois do passo do motor e do {@link BattleFlow#update}. */
    void update(CombatEngine engine, BattleFlow flow, Voz voz) {
        BattleFlow.Phase agora = flow.phase();
        if (agora != fase) {
            if (agora == BattleFlow.Phase.GO && !comecou) {
                comecou = true;
                voz.narrar("inicio", vars("p1", nome(engine, 0), "p2", nome(engine, 1)));
            }
            if (agora == BattleFlow.Phase.OVER && !acabou) {
                acabou = true;
                fim(engine, flow, voz);
            }
            fase = agora;
        }
        // Sem batalha (treino) o "GO" não acontece: a luta conta como começada.
        if (!flow.battle()) comecou = true;
        if (!comecou || acabou) {
            lembrar(engine);
            return;
        }

        // Golpes que entraram neste quadro.
        for (CombatEngine.HitEvent e : engine.events()) {
            if (e.blocked) continue;
            if (!primeiroGolpe) {
                primeiroGolpe = true;
                voz.narrar("primeiro_golpe", vars("atacante", nome(engine, e.attacker)));
            }
            if ("THROW".equals(e.moveId)) voz.narrar("agarrao", vars("atacante", nome(engine, e.attacker)));
        }
        for (String cue : engine.cues()) {
            switch (cue) {
                case "GUARD_BREAK":
                    for (int s = 0; s < 2; s++) {
                        if (engine.fighter(s).guardBroken) voz.narrar("guard_break", vars("defensor", nome(engine, s)));
                    }
                    break;
                case "COMBO_BREAKER":
                    for (int s = 0; s < 2; s++) {
                        if (engine.fighter(s).comboBroke && engine.fighter(1 - s).inHitstun()) {
                            voz.narrar("combo_breaker", vars("atacante", nome(engine, s)));
                        }
                    }
                    break;
                case "JUST_DEFENSE":
                    for (int s = 0; s < 2; s++) {
                        CombatFighter f = engine.fighter(s);
                        if (f.justDefended && f.status == CombatFighter.Status.BLOCKSTUN && f.stunElapsed == 0) {
                            voz.narrar("defesa_perfeita", vars("defensor", nome(engine, s)));
                        }
                    }
                    break;
                case "PUNISH":
                    for (CombatEngine.HitEvent e : engine.events()) {
                        if (!e.blocked) {
                            voz.narrar("punicao", vars("atacante", nome(engine, e.attacker)));
                            break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        for (int s = 0; s < 2; s++) {
            CombatFighter f = engine.fighter(s);
            // Super e Ultra no instante em que começam.
            if (f.attacking() && f.attack != ultimoGolpe[s] && f.attack.strength == AttackDefinition.Strength.SUPER &&
                f.status != CombatFighter.Status.ULTRA) {
                voz.narrar("super", vars("atacante", nome(engine, s)));
            }
            boolean ultra = f.status == CombatFighter.Status.ULTRA;
            if (ultra && !emUltra[s]) voz.narrar("ultra", vars("atacante", nome(engine, s)));
            // Combo que acabou (a sessão de quem apanhou fechou): comenta o tamanho.
            ComboSession sessao = engine.lastSession(s);
            if (sessao != null && sessao != ultimaSessao[s]) {
                ultimaSessao[s] = sessao;
                if (sessao.hitCount >= COMBO) {
                    voz.narrar(sessao.hitCount >= COMBO_GRANDE ? "combo_grande" : "combo",
                        vars("atacante", nome(engine, sessao.attacker), "n", String.valueOf(sessao.hitCount)));
                }
            }
            // Troca de lutador (tag ou o parceiro que entra depois de um K.O.).
            FighterState ponto = engine.team(s).pointState();
            if (titular[s] != null && ponto != titular[s] && !ponto.equals(titular[s])) {
                voz.narrar("troca", vars("entra", nomes.apply(ponto.character)));
            }
            // K.O. de um lutador (no 2x2 a luta continua com o parceiro).
            if (f.ko() && !caido[s]) {
                boolean comUltra = emUltra[1 - s] || engine.fighter(1 - s).status == CombatFighter.Status.ULTRA;
                voz.narrar(comUltra ? "ko_ultra" : "ko",
                    vars("vencedor", nome(engine, 1 - s), "perdedor", nome(engine, s)));
            }
            // Em apuros: uma vez por lutador.
            int vida = permille(ponto);
            if (!f.ko() && vida > 0 && vida <= APUROS && emApuros.add(ponto)) {
                voz.narrar("apuros", vars("nome", nomes.apply(ponto.character)));
            }
        }

        // Quem está ganhando: domina (uma vez) e a virada.
        // Pela vida da dupla inteira (a troca de titular não vira "virada").
        int diferenca = equipe(engine, 0) - equipe(engine, 1);
        int frente = diferenca >= LIDERA ? 0 : diferenca <= -LIDERA ? 1 : -1;
        if (frente >= 0) {
            if (lider >= 0 && frente != lider) {
                voz.narrar("virada", vars("nome", nome(engine, frente)));
            } else if (!dominouAvisado && Math.abs(diferenca) >= DOMINA) {
                dominouAvisado = true;
                voz.narrar("dominando", vars("nome", nome(engine, frente)));
            }
            lider = frente;
        }
        if (!tempoAvisado && flow.clockUrgent()) {
            tempoAvisado = true;
            voz.narrar("tempo", vars());
        }
        lembrar(engine);
    }

    private void fim(CombatEngine engine, BattleFlow flow, Voz voz) {
        int vencedor = flow.winner();
        if (vencedor == BattleFlow.DRAW || vencedor == BattleFlow.NONE) {
            voz.narrar("empate", vars());
            return;
        }
        String nome = nome(engine, vencedor);
        if (flow.timeOver()) {
            voz.narrar("tempo_esgotado", vars("vencedor", nome));
            return;
        }
        // O K.O. que fechou a luta é narrado aqui (o lutador caiu neste mesmo quadro).
        CombatFighter derrotado = engine.fighter(1 - vencedor);
        if (derrotado.ko() && !caido[1 - vencedor]) {
            boolean comUltra = emUltra[vencedor] || engine.fighter(vencedor).status == CombatFighter.Status.ULTRA;
            voz.narrar(comUltra ? "ko_ultra" : "ko", vars("vencedor", nome, "perdedor", nome(engine, 1 - vencedor)));
            caido[1 - vencedor] = true;
        }
        boolean perfeito = true;
        for (FighterState m : engine.team(vencedor).members) {
            if (m.life < m.profile.maxLife) perfeito = false;
        }
        voz.narrar(perfeito ? "perfeito" : "vitoria", vars("vencedor", nome));
    }

    /** O que vale no próximo quadro para ver o que mudou. */
    private void lembrar(CombatEngine engine) {
        for (int s = 0; s < 2; s++) {
            CombatFighter f = engine.fighter(s);
            ultimoGolpe[s] = f.attacking() ? f.attack : null;
            emUltra[s] = f.status == CombatFighter.Status.ULTRA;
            caido[s] = f.ko();
            titular[s] = engine.team(s).pointState();
            if (ultimaSessao[s] == null) ultimaSessao[s] = engine.lastSession(s);
        }
    }

    private static int permille(FighterState s) {
        return s.profile.maxLife <= 0 ? 0 : (int)(s.life * 1000L / s.profile.maxLife);
    }

    /** Vida da dupla (por mil do total). */
    private static int equipe(CombatEngine engine, int side) {
        long vida = 0, max = 0;
        for (FighterState m : engine.team(side).members) {
            vida += Math.max(0, m.life);
            max += m.profile.maxLife;
        }
        return max <= 0 ? 0 : (int)(vida * 1000L / max);
    }

    private String nome(CombatEngine engine, int side) {
        return nomes.apply(engine.team(side).pointState().character);
    }

    private static Map<String, String> vars(String... pares) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) m.put(pares[i], pares[i + 1]);
        return m;
    }
}
