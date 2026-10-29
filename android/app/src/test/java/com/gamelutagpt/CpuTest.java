package com.gamelutagpt;

import java.util.List;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * A CPU nova: níveis 1 a 8 sem reação mais rápida que a humana, defesa, punição, anti-aéreo,
 * caderno de combos e estilo por lutador. As lutas são simuladas no motor de verdade, com
 * sementes fixas.
 */
public class CpuTest {
    private static final CharacterDefinition BASE = GeneratedCharacters.get("player_base");

    /** Jogador roteirizado (lado 0) contra a CPU (lado 1). */
    private static final class Fight {
        final CombatEngine engine;
        final AiController cpu;
        final FighterInput player = new FighterInput(), out = new FighterInput();

        Fight(CharacterDefinition p1, CharacterDefinition p2, float gap, int level, long seed) {
            engine = new CombatEngine(new FighterState(p1, "P1"), new FighterState(p2, "CPU"),
                500f, 500f + gap, new CombatConfig());
            cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(level);
        }

        CombatFighter p() { return engine.fighter(0); }
        CombatFighter c() { return engine.fighter(1); }

        void step() {
            cpu.fill(engine, 1, out);
            engine.step(player, out);
            player.clearPresses();
        }
    }

    // ------------------------------------------------------------ combos da CPU

    /**
     * Um minuto contra um jogador que anda e às vezes ataca, sem defender: dos acertos corpo a
     * corpo da CPU (sem contar o especial e o Super soltos), quantos viram combo de 3+ hits.
     */
    private static double emenda(CharacterDefinition cpuChar, int level, long seeds) {
        int aberturas = 0, emendou = 0;
        for (long seed = 1; seed <= seeds; seed++) {
            CombatConfig cfg = new CombatConfig();
            cfg.justDefenseFrames = -1;
            CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"), new FighterState(cpuChar, "CPU"), 600f, 900f, cfg);
            e.fighter(0).state.life = Integer.MAX_VALUE / 4;
            e.fighter(1).state.life = Integer.MAX_VALUE / 4;
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setLevel(level);
            FighterInput p = new FighterInput(), out = new FighterInput();
            Random r = new Random(seed + 99);
            Object ultima = null;
            String abriu = null;
            for (int fr = 0; fr < 60 * 60; fr++) {
                if (fr % 20 == 0) {
                    int d = r.nextInt(5);
                    p.direction = d == 0 ? 1 : d == 1 ? 5 : 0;
                }
                if (r.nextInt(40) == 0) p.light = true; else if (r.nextInt(60) == 0) p.medium = true;
                cpu.fill(e, 1, out);
                e.step(p, out);
                p.clearPresses();
                for (CombatEngine.HitEvent h : e.events()) {
                    ComboSession s = e.session(0);
                    if (h.attacker == 1 && !h.blocked && s != null && s.hitCount == 1) abriu = h.projectile ? "proj" : h.moveId;
                }
                ComboSession s = e.lastSession(0);
                if (s != null && s != ultima) {
                    ultima = s;
                    if (s.attacker == 1 && abriu != null && !"proj".equals(abriu) && !"S".equals(abriu) && !"SUPER".equals(abriu)) {
                        aberturas++;
                        if (s.hitCount >= 3) emendou++;
                    }
                    abriu = null;
                }
            }
        }
        return aberturas == 0 ? 0 : emendou / (double)aberturas;
    }

    @Test public void aHighLevelCpuTurnsItsHitsIntoCombos() {
        double alto = emenda(BASE, 8, 4), baixo = emenda(BASE, 1, 4);
        assertTrue("nível 8 emenda: " + alto, alto >= 0.4);
        assertTrue("nível 1 emenda menos: " + baixo + " < " + alto, baixo < alto);
    }

    @Test public void everyCharactersCombosRunWithoutBreakingTheCpu() {
        // O próximo golpe que não alcança faz a CPU procurar outro (H, M, especial, Super): nenhum
        // pacote pode quebrar essa busca (golpe sem alcance cadastrado, por exemplo).
        for (String id : GeneratedCharacters.SELECTABLE) emenda(GeneratedCharacters.get(id), 8, 1);
    }

    @Test public void theCpuAlmostNeverStartsAnAttackOutOfReach() {
        // Ela decide com a distância vista (com o atraso da reação): a folga de alcance do chão e
        // a do L de abertura seguram o golpe na beira, que saía no vento por poucas unidades.
        CharacterDefinition.Body target = GeneratedCharacters.defaultCharacter().fighter.body;
        int inicios = 0, fora = 0;
        for (int level : new int[] {5, 8}) {
            for (long seed = 1; seed <= 12; seed++) {
                CombatEngine e = new CombatEngine(new FighterState(GeneratedCharacters.defaultCharacter(), "P1"),
                    new FighterState(GeneratedCharacters.opponentCharacter(), "CPU"), 420f, 590f, new CombatConfig());
                AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
                cpu.setEnabled(true);
                cpu.setFullKit(true);
                cpu.setLevel(level);
                FighterInput p = new FighterInput(), out = new FighterInput();
                for (int i = 0; i < 900; i++) {
                    cpu.fill(e, 1, out);
                    e.step(p, out);
                    CombatFighter c = e.fighter(1);
                    if (c.attacking() && c.attackFrame == 0 && c.move != null) {
                        inicios++;
                        // A régua é o alcance real do golpe (CpuReach), o do desenho; no ar, o do pacote.
                        float[] real = c.grounded ? CpuReach.measured(c.character(), c.attack.id) : null;
                        float max = real != null ? real[0] + target.halfWidth : CombatRules.maxCenterDistance(c.move, target);
                        if (Math.abs(e.fighter(0).x - c.x) > max + 0.5f) fora++;
                    }
                }
            }
        }
        assertTrue("começou golpes", inicios > 100);
        assertTrue("fora do alcance: " + fora + " de " + inicios, fora * 200 <= inicios);
    }

    /**
     * Batalha 2x2 (kit completo) contra uma CPU nível 5: {Supers crus por minuto (saídos do
     * neutro, sem ter acertado nada), barra média em milésimos}.
     */
    private static double[] recursos(int level) {
        long frames = 0, barra = 0;
        int crus = 0;
        // 120 lutas (~2 s): o Super cru é raro (1–2 por minuto), e com poucas lutas a razão
        // entre os níveis variava de 0,4 a 1,0 só com o sorteio. Com 200 ela fica em ~0,5.
        for (long seed = 1; seed <= 120; seed++) {
            FighterState[] t0 = {new FighterState(BASE, "P1"), new FighterState(GeneratedCharacters.get("p02"), "P2")};
            FighterState[] t1 = {new FighterState(GeneratedCharacters.get("p03"), "C1"), new FighterState(GeneratedCharacters.get("p05"), "C2")};
            CombatEngine e = new CombatEngine(t0[0], t1[0], 600f, 900f, new CombatConfig());
            e.setTeam(0, t0);
            e.setTeam(1, t1);
            AiController rival = new AiController(new OpponentAi(new Random(seed + 50)), new Random(seed * 13 + 1));
            rival.setEnabled(true);
            rival.setFullKit(true);
            rival.setLevel(5);
            AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
            cpu.setEnabled(true);
            cpu.setFullKit(true);
            cpu.setLevel(level);
            FighterInput a = new FighterInput(), b = new FighterInput();
            for (int f = 0; f < 90 * 60; f++) {
                rival.fill(e, 0, a);
                cpu.fill(e, 1, b);
                e.step(a, b);
                CombatFighter c = e.fighter(1);
                // Cru: sai com o rival livre (fechando combo, o rival está atordoado).
                if (c.attacking() && c.attackFrame == 0 && c.attack.kind == AttackDefinition.Kind.SUPER &&
                    !e.fighter(0).inHitstun()) crus++;
                barra += c.state.superMeter;
                frames++;
            }
        }
        return new double[] {crus / (frames / 3600.0), barra / (double)frames};
    }

    @Test public void higherLevelsSpendMeterWithMoreSense() {
        // Contra o 5 (que já usa o kit todo; o 2 quase não usa barra nenhuma).
        double[] baixo = recursos(5), alto = recursos(8);
        assertTrue("o nível 8 solta bem menos Super cru: " + baixo[0] + " → " + alto[0], alto[0] < baixo[0] * 0.7);
        assertTrue("e guarda mais barra (para o Ultra): " + baixo[1] + " → " + alto[1], alto[1] > baixo[1]);
    }

    /**
     * Quem fica no canto soltando bola (meia-lua + soco) sem parar: o nível alto pula por cima
     * na hora certa e cai batendo, defende o resto sem recuar e tira bem mais vida do spammer
     * que o nível baixo. Antes ele levava ~3 de cada 4 bolas e quase nunca chegava nele.
     */
    @Test public void aHighLevelCpuBeatsAFireballSpammer() {
        double[] alto = spammer(8), baixo = spammer(3);
        assertTrue("o nível 8 leva bem menos bolas: " + alto[1], alto[1] < 0.5);
        assertTrue("e tira vida do spammer: " + alto[0], alto[0] > 0.6);
        assertTrue("mais que o nível 3: " + baixo[1] + " → " + alto[1], alto[1] < baixo[1] - 0.15);
    }

    /** Média de {vida que o spammer perdeu, fração das bolas que acertaram a CPU}. */
    private static double[] spammer(int level) {
        double perdeu = 0, acertou = 0;
        int lutas = 0;
        for (String ch : new String[] {"p03", "player_two", "p05"}) {
            for (long seed = 1; seed <= 3; seed++) {
                CombatEngine e = new CombatEngine(new FighterState(BASE, "P1"),
                    new FighterState(GeneratedCharacters.get(ch), "CPU"), 250f, 1000f, new CombatConfig());
                AiController cpu = new AiController(new OpponentAi(new Random(seed)), new Random(seed * 31 + 7));
                cpu.setEnabled(true);
                cpu.setLevel(level);
                FighterInput p = new FighterInput(), out = new FighterInput();
                Random r = new Random(seed * 13 + 5);
                int vida = e.fighter(0).state.life, hit = 0, blocked = 0, espera = 0;
                for (int f = 0; f < 60 * 60 && !e.fighter(0).ko() && !e.fighter(1).ko(); f++) {
                    CombatFighter me = e.fighter(0);
                    p.clear();
                    if (me.canAct() && me.grounded && !e.energyProjectileAlive(0) && espera-- <= 0) {
                        p.special = "L";   // a bola de novo, assim que pode
                        espera = 4 + r.nextInt(8);
                    } else {
                        p.direction = me.facing > 0 ? 5 : 1;   // entre uma e outra, segura trás
                    }
                    cpu.fill(e, 1, out);
                    e.step(p, out);
                    for (CombatEngine.HitEvent ev : e.events()) {
                        if (ev.attacker == 0 && ev.projectile) {
                            if (ev.blocked) blocked++;
                            else hit++;
                        }
                    }
                }
                perdeu += 1.0 - e.fighter(0).state.life / (double)vida;
                acertou += hit / (double)Math.max(1, hit + blocked);
                lutas++;
            }
        }
        return new double[] {perdeu / lutas, acertou / lutas};
    }

    @Test public void theNeutralPokePrefersTheComboStarterUpClose() {
        assertTrue(OpponentAi.starterChance(CpuLevel.of(8)) > OpponentAi.starterChance(CpuLevel.of(1)));
        assertTrue(OpponentAi.starterChance(CpuLevel.of(8)) <= 0.5);
    }

    // ------------------------------------------------------------ níveis e percepção

    @Test public void noLevelReactsFasterThanAHuman() {
        for (int n = CpuLevel.MIN; n <= CpuLevel.MAX; n++) {
            CpuLevel level = CpuLevel.of(n);
            assertEquals(n, level.number);
            assertTrue("nível " + n, level.reactionFrames >= CpuLevel.HUMAN_REACTION_FRAMES);
            if (n > 1) assertTrue("mais alto reage mais rápido", level.reactionFrames <= CpuLevel.of(n - 1).reactionFrames);
        }
        assertTrue("13 quadros ≈ 217 ms: o humano rápido", CpuLevel.HUMAN_REACTION_FRAMES * 1000 / CombatConfig.FPS >= 200);
        assertEquals("fora da faixa vira o mais próximo", 8, CpuLevel.of(12).number);
        assertEquals(1, CpuLevel.of(0).number);
    }

    @Test public void perceptionOnlyShowsThePast() {
        Fight f = new Fight(BASE, BASE, 300f, 8, 1);
        CpuPerception p = new CpuPerception();
        assertNull("nada antes de ver", p.seen(0));
        for (int i = 0; i < 40; i++) {
            p.record(f.engine, 1);
            f.engine.step(f.player, f.out);
        }
        CpuPerception.Snapshot s = p.seen(13);
        assertEquals(p.lastFrame() - 13, s.frame);
        assertNull("o que ainda não durou a reação, ela não sabe", p.seen(40));
    }

    /**
     * O jogador pula para cima da CPU num quadro sorteado; o primeiro quadro em que a CPU muda
     * por causa disso (defende em pé ou sobe com o 2H) vem pelo menos 13 quadros depois.
     */
    @Test public void theCpuNeverReactsToAJumpBeforeTheHumanReaction() {
        int reactions = 0;
        for (int level = 1; level <= 8; level++) {
            // 10 sementes: a CPU ataca bastante no neutro e às vezes está ocupada quando ele pula.
            for (long seed = 1; seed <= 10; seed++) {
                Fight f = new Fight(BASE, BASE, 430f, level, seed);
                int jumpAt = 40 + (int)(seed * 7);
                int reaction = -1;
                for (int i = 0; i < jumpAt + 60 && reaction < 0; i++) {
                    if (i == jumpAt) f.player.direction = 8;   // pulo para frente
                    if (i == jumpAt + 1) f.player.direction = 0;
                    f.step();
                    // Segurando para trás em reação (a espera em pé por hábito não é reação ao pulo).
                    boolean guardsHigh = f.out.direction == 1 && f.c().facing < 0 && !f.cpu.guardingByHabit();
                    boolean antiAir = "antiaereo".equals(f.cpu.lastReason) && f.out.heavy;
                    if (i > jumpAt && (guardsHigh || antiAir)) reaction = i;
                }
                if (reaction >= 0) {
                    reactions++;
                    assertTrue("nível " + level + " semente " + seed + ": reagiu em " + (reaction - jumpAt),
                        reaction - jumpAt >= CpuLevel.HUMAN_REACTION_FRAMES);
                }
            }
        }
        assertTrue("ela reagiu aos pulos (o teste mediu algo): " + reactions, reactions >= 10);
    }

    // ------------------------------------------------------------ defesa

    /** O jogador pressiona com L e 2L colado; quanto a CPU defende. */
    private static double blockRate(int level) {
        int blocked = 0, contacts = 0;
        // 32 lutas: com 8 a taxa variava ±10 pontos com qualquer mudança de ritmo do motor.
        for (long seed = 1; seed <= 32; seed++) {
            Fight f = new Fight(BASE, BASE, 130f, level, seed);
            Random script = new Random(seed);
            for (int i = 0; i < 600; i++) {
                if (i % 40 == 20) {
                    boolean low = script.nextBoolean();
                    f.player.direction = low ? 3 : 0;
                    f.player.light = true;
                }
                if (i % 40 == 22) f.player.direction = 0;
                f.step();
                for (CombatEngine.HitEvent e : f.engine.events()) {
                    if (e.attacker != 0) continue;
                    contacts++;
                    if (e.blocked) blocked++;
                }
                // Mantém os dois colados para a pressão continuar.
                if (Math.abs(f.c().x - f.p().x) > 160f && f.c().canAct()) f.p().x = f.c().x - 130f;
            }
        }
        return contacts == 0 ? 0 : blocked / (double)contacts;
    }

    @Test public void higherLevelsBlockMore() {
        double low = blockRate(1), high = blockRate(8);
        assertTrue("nível 1 quase não defende: " + low, low < 0.25);
        // A defesa por hábito é baixa de propósito (o que dá graça é a luta): o nível 8 segura
        // parte dos jabs colados (~25% com a pausa de acerto do fraco; ~31% sem ela), sempre bem
        // mais que o 1.
        assertTrue("nível 8 defende parte: " + high, high > 0.2);
        assertTrue("e bem mais que o nível 1: " + low + " → " + high, high > low + 0.15);
    }

    // ------------------------------------------------------------ punir e combos

    /** Golpe do jogador que nunca alcança e demora para recuperar: um convite para punir. */
    private static final CharacterDefinition WHIFFER = semDesenho(CombatEngineTest.withMoves(BASE,
        new AttackDefinition.Builder("H", AttackDefinition.Kind.NORMAL).damage(100)
            .frames(10, 3, 28).stun(12, 8, 0).windows(null, null, null)
            .pushback(0f, 0f).reach(10f, 78f).build()), "H");

    /**
     * O golpe de teste sem o desenho do golpe de verdade: o braço desenhado do H alcança muito
     * mais que o alcance curto do teste, e o golpe "errado" acertaria pelo desenho. Com o desenho
     * parado (sem membro medido) a caixa do teste decide.
     */
    private static CharacterDefinition semDesenho(CharacterDefinition c, String id) {
        java.util.Map<String, CharacterDefinition.Move> moves = new java.util.LinkedHashMap<>(c.moves);
        CharacterDefinition.Move m = moves.get(id);
        moves.put(id, new CharacterDefinition.Move(id, c.animations.get("IDLE"), m.pose, m.attack));
        return new CharacterDefinition(c.id, c.displayName, c.profile, c.artFacing, c.visualStandHeight,
            c.visualCrouchHeight, c.fighter, c.animations, moves, c.specialAnimations, c.specials);
    }

    /**
     * O jogador erra um golpe lento colado na CPU. Só valem as tentativas em que a CPU não
     * atacou antes do erro aparecer (senão é contra-ataque, não punição). Devolve quantas vezes
     * ela pune antes de ele se recuperar e quanto demora para o golpe dela sair, em quadros desde
     * o começo da recuperação (quem não pune conta a recuperação inteira).
     */
    private static double[] punishStats(int level) {
        int punished = 0, tries = 0, fastest = Integer.MAX_VALUE;
        double delay = 0;
        AttackDefinition h = WHIFFER.attack("H");
        int recoveryStart = h.startupFrames + h.activeFrames;
        // Muitas sementes: calma, a CPU muitas vezes ataca antes do erro (tentativa que não vale).
        for (long seed = 1; seed <= 400; seed++) {
            Fight f = new Fight(WHIFFER, BASE, 110f, level, seed);
            for (int i = 0; i < 30; i++) f.step();   // a CPU passa a ver a luta
            // Espera um momento em que a CPU está livre e parada perto (ela ataca bastante no
            // neutro): o erro do jogador tem que aparecer com ela sem golpe na tela.
            for (int i = 0; i < 600 && !livreParaPunir(f); i++) f.step();
            if (!livreParaPunir(f)) continue;
            f.player.heavy = true;
            boolean valid = true;
            int started = -1;
            boolean hit = false;
            for (int i = 0; i < h.totalFrames + 4 && valid && !hit; i++) {
                f.step();
                if (f.c().attacking() && started < 0) {
                    if (i < recoveryStart) valid = false;   // atacou antes de ver o erro
                    else started = i;
                    if ("punir".equals(f.cpu.lastReason)) fastest = Math.min(fastest, i - recoveryStart);
                }
                for (CombatEngine.HitEvent e : f.engine.events()) hit |= e.attacker == 1 && !e.blocked;
            }
            if (!valid) continue;
            tries++;
            if (hit) punished++;
            delay += hit && started >= 0 ? started - recoveryStart : h.recoveryFrames;
        }
        assertTrue("nível " + level + ": poucas tentativas válidas (" + tries + ")", tries >= 6);
        return new double[] {punished / (double)tries, delay / tries, fastest};
    }

    /** Os dois no chão, livres, a CPU sem golpe na tela e na distância do teste. */
    private static boolean livreParaPunir(Fight f) {
        float gap = Math.abs(f.c().x - f.p().x);
        return f.p().canAct() && f.c().canAct() && f.p().grounded && f.c().grounded && !f.c().attacking() &&
            gap >= 90f && gap <= 130f;
    }

    @Test public void level8PunishesAWhiffButNotFasterThanAHuman() {
        double[] high = punishStats(8);
        assertTrue("nível 8 pune quase sempre: " + high[0], high[0] >= 0.75);
        assertTrue("logo depois da reação: " + high[1], high[1] <= 22);
        // Punição (a decisão de punir, não um golpe do neutro que calhou de sair) só depois da reação.
        assertTrue("e nunca antes dela: " + high[2], high[2] >= CpuLevel.HUMAN_REACTION_FRAMES);
    }

    /** O maior combo que a CPU tira de um jogador parado (que não defende). */
    private static int longestCombo(int level) {
        int longest = 0;
        for (long seed = 1; seed <= 6; seed++) {
            Fight f = new Fight(BASE, BASE, 140f, level, seed);
            for (int i = 0; i < 900; i++) {
                f.step();
                ComboSession s = f.engine.lastSession(0);
                if (s != null) longest = Math.max(longest, s.hitCount);
                ComboSession live = f.engine.session(0);
                if (live != null) longest = Math.max(longest, live.hitCount);
                f.p().state.restoreLife();
            }
        }
        return longest;
    }

    @Test public void higherLevelsRunLongerCombosFromTheBook() {
        int low = longestCombo(1), high = longestCombo(8);
        assertTrue("nível 1: golpe solto: " + low, low <= 2);
        assertTrue("nível 8: combo de verdade: " + high, high >= 4);
    }

    // ------------------------------------------------------------ caderno e estilo

    @Test public void theComboBookFollowsEachStyle() {
        List<String> p01 = ComboBook.route(GeneratedCharacters.get("player_base"));
        assertEquals("L", p01.get(0));
        assertTrue("lançador e super pulo: " + p01, p01.contains("2H") && p01.contains(ComboBook.JUMP));
        assertTrue("pulo no ar: " + p01, p01.contains(ComboBook.AIR_JUMP));
        List<String> p04 = ComboBook.route(GeneratedCharacters.get("p04"));
        assertEquals("pressão: M > M: " + p04, 2, p04.stream().filter("M"::equals).count());
        List<String> p09 = ComboBook.route(GeneratedCharacters.get("p09"));
        assertTrue("tanque: H > H: " + p09, p09.indexOf("H") >= 0 && "H".equals(p09.get(p09.indexOf("H") + 1)));
        List<String> p12 = ComboBook.route(GeneratedCharacters.get("p12"));
        int h = p12.indexOf("H");
        assertTrue("imprevisível volta: H > L: " + p12, h >= 0 && "L".equals(p12.get(h + 1)));
        List<String> p08 = ComboBook.route(GeneratedCharacters.get("p08"));
        String end = p08.get(p08.size() - 1);
        assertTrue("alcance fecha no especial: " + p08, "S".equals(end) || "SUPER".equals(end));
        // Toda rota só usa o que o pacote deixa.
        for (String id : new String[] {"player_base", "p02", "p03", "p04", "p05", "p06", "p07", "p08", "p09", "p10", "p11", "p12"}) {
            CharacterDefinition c = GeneratedCharacters.get(id);
            List<String> r = ComboBook.route(c);
            for (int i = 0; i + 1 < r.size(); i++) {
                String a = r.get(i), b = r.get(i + 1);
                if (ComboBook.JUMP.equals(a) || ComboBook.AIR_JUMP.equals(a)) continue;
                String target = ComboBook.JUMP.equals(b) || ComboBook.AIR_JUMP.equals(b) ? AttackDefinition.JUMP : b;
                assertTrue(id + ": " + a + " > " + b, c.attack(a).cancelsInto(target));
            }
        }
    }

    /** Distância média da CPU ao jogador parado. */
    private static double averageGap(String fighter) {
        double sum = 0;
        int n = 0;
        for (long seed = 1; seed <= 4; seed++) {
            Fight f = new Fight(BASE, GeneratedCharacters.get(fighter), 300f, 5, seed);
            for (int i = 0; i < 900; i++) {
                f.step();
                f.p().state.restoreLife();
                if (i > 120) {
                    sum += Math.abs(f.c().x - f.p().x);
                    n++;
                }
            }
        }
        return sum / n;
    }

    @Test public void zonersKeepFurtherAwayThanRushdowns() {
        double zoner = averageGap("p08"), rush = averageGap("p04");
        assertTrue("alcance " + zoner + " > pressão " + rush, zoner > rush + 40);
    }

    // ------------------------------------------------------------ CPU contra CPU

    @Test public void theHigherLevelWinsMostFights() {
        int wins = 0;
        for (long seed = 1; seed <= 8; seed++) {
            CombatEngine engine = new CombatEngine(new FighterState(BASE, "A"), new FighterState(BASE, "B"),
                500f, 800f, new CombatConfig());
            AiController strong = new AiController(new OpponentAi(new Random(seed)), new Random(seed + 100));
            AiController weak = new AiController(new OpponentAi(new Random(seed + 50)), new Random(seed + 200));
            strong.setEnabled(true);
            weak.setEnabled(true);
            strong.setLevel(8);
            weak.setLevel(2);
            FighterInput a = new FighterInput(), b = new FighterInput();
            for (int i = 0; i < 60 * 60 && !engine.fighter(0).ko() && !engine.fighter(1).ko(); i++) {
                strong.fill(engine, 0, a);
                weak.fill(engine, 1, b);
                engine.step(a, b);
            }
            if (engine.fighter(0).state.life > engine.fighter(1).state.life) wins++;
        }
        assertTrue("nível 8 vence o nível 2: " + wins + "/8", wins >= 6);
    }
}
