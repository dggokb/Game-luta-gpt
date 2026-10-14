package com.gamelutagpt;

import java.util.ArrayList;
import java.util.List;

/**
 * Modo tutorial: o treino guiado dos golpes, lição por lição, no dojo. Quem joga é o
 * jogador (o pad dele vai direto para o motor); o boneco de treino só fica parado ou ataca
 * quando a lição é de defesa. Cada lição diz o que fazer, quantas vezes, e o que conta como
 * feito, olhando o próprio motor (golpe que acertou, pulo, dash, defesa...).
 *
 * As lições vêm da lista de golpes que o personagem tem: uma lição de golpe que ele não
 * tem fica de fora. Cada lição também traz um exemplo de entrada que a cumpre, usado pelos
 * testes (TutorialDirectorTest) para garantir que dá para fazer.
 *
 * Java puro sobre o motor, como o {@link DemoDirector}.
 */
final class TutorialDirector {
    /** Quadros do "MUITO BEM!" entre uma lição e a próxima. */
    static final int PRAISE_FRAMES = 50;
    /** Quadros do "TREINO COMPLETO!" no fim. */
    static final int COMPLETE_FRAMES = 120;
    private static final int BAR = CombatConfig.METER_PER_BAR;
    /** Distância entre os corpos nas lições de golpe (o boneco ao alcance). */
    private static final float CLOSE = 24f;
    /** Mais longe que isso, numa lição de golpe, o boneco volta para perto. */
    private static final float TOO_FAR = 260f;
    /** Quadros parados e longe antes de o boneco voltar para perto. */
    private static final int RESET_AFTER = 40;

    /** O que a lição vê e lembra. */
    static final class Ctx {
        CombatEngine engine;
        FighterState[] team;
        /** Quadros desde o começo da lição. */
        int t;
        /** Andou para frente e para trás desde o começo da lição (unidades do mundo). */
        float forward, back;
        /** O golpe do jogador que está saindo agora acertou o boneco (uma vez por golpe). */
        String landed = "";
        private AttackDefinition lastAttack;
        private int lastAttackFrame;

        CombatFighter p1() { return engine.fighter(0); }
        CombatFighter dummy() { return engine.fighter(1); }

        /** Golpe {@code id} do jogador saindo (do primeiro quadro em diante). */
        boolean doing(String id) {
            CombatFighter p = p1();
            return p.attacking() && p.attack != null && id.equals(p.attack.id);
        }

        /** O golpe {@code id} do jogador acertou o boneco (vale uma vez por golpe). */
        boolean landed(String id) { return id.equals(landed); }

        boolean blocking() { return p1().status == CombatFighter.Status.BLOCKSTUN; }

        boolean event(String moveId) {
            for (CombatEngine.HitEvent e : engine.events()) {
                if (e.attacker == 0 && moveId.equals(e.moveId)) return true;
            }
            return false;
        }

        void placeApart(float gap) {
            float halves = engine.fighter(0).body().halfWidth + engine.fighter(1).body().halfWidth;
            engine.restage(homeX, homeX + halves + gap);
        }

        float gap() {
            float halves = p1().body().halfWidth + dummy().body().halfWidth;
            return Math.abs(dummy().x - p1().x) - halves;
        }

        float homeX;

        /** Depois de cada passo do motor: o que andou e o golpe que acertou. */
        void observe() {
            CombatFighter p = p1();
            if (p.grounded && p.status == CombatFighter.Status.NEUTRAL && !p.forwardDashing && p.backdashFrames == 0) {
                float step = p.travel * p.facing;
                if (step > 0f) forward += step;
                else back -= step;
            }
            // Um golpe novo começa: o "acertou" é dele, não do anterior.
            if (p.attacking() && (p.attack != lastAttack || p.attackFrame < lastAttackFrame)) landed = "";
            lastAttack = p.attacking() ? p.attack : null;
            lastAttackFrame = p.attackFrame;
            for (CombatEngine.HitEvent e : engine.events()) {
                if (e.attacker == 0 && !e.projectile && p.attacking() && p.attack != null &&
                    p.attack.id.equals(e.moveId)) {
                    landed = e.moveId;
                }
            }
        }
    }

    interface Setup { void apply(Ctx c); }
    interface Dummy { void drive(Ctx c, FighterInput dummy); }
    interface Check { boolean met(Ctx c); }
    /** Entrada do jogador que cumpre a lição (testes). */
    interface Example { void drive(Ctx c, FighterInput p1); }

    static final class Lesson {
        /** Nome fixo da lição ("golpe-fraco", "especial-s2"...), usado pelos quadros entre as lições. */
        final String id;
        final String title;
        /** O que fazer, com as setas do direcional (virado para a direita). */
        final String how;
        final int times;
        /** O boneco ao alcance: se ficar longe, volta para perto. */
        final boolean close;
        final Setup setup;
        final Dummy dummy;
        /** Conta uma vez a cada vez que passa de falso para verdadeiro. */
        final Check check;
        final Example example;

        Lesson(String title, String how, int times, boolean close, Setup setup, Dummy dummy, Check check,
               Example example) {
            this.id = idOf(title);
            this.title = title;
            this.how = how;
            this.times = times;
            this.close = close;
            this.setup = setup;
            this.dummy = dummy;
            this.check = check;
            this.example = example;
        }
    }

    /** "ESPECIAL DE ENERGIA" → "especial-de-energia" (sem acentos). */
    static String idOf(String title) {
        String plain = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return plain.toLowerCase(java.util.Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-");
    }

    // ------------------------------------------------------------ lições

    /** As lições para o personagem, na ordem do treino. */
    static List<Lesson> lessons(CharacterDefinition character) {
        List<Lesson> out = new ArrayList<>();
        Setup apart = c -> c.placeApart(220f);
        Setup close = c -> c.placeApart(CLOSE);

        // Movimento.
        out.add(new Lesson("ANDAR", "Segure → para andar e ← para recuar", 1, false, apart, null,
            c -> c.forward >= 120f && c.back >= 80f,
            (c, p1) -> p1.direction = c.t < 40 ? 1 : 5));
        out.add(new Lesson("AGACHAR", "Segure ↓ para agachar", 1, false, apart, null,
            c -> c.p1().crouching,
            (c, p1) -> p1.direction = 3));
        out.add(new Lesson("PULAR", "Toque ↑ para pular (↖ e ↗ pulam para os lados)", 2, false, apart, null,
            c -> !c.p1().grounded && !c.p1().superJumping,
            (c, p1) -> p1.direction = c.t % 60 < 4 ? 7 : 0));
        out.add(new Lesson("SUPER PULO", "↓ e logo ↑: pula bem mais alto", 1, false, apart, null,
            c -> c.p1().superJumping,
            (c, p1) -> p1.direction = c.t < 3 ? 3 : c.t < 6 ? 7 : 0));
        out.add(new Lesson("DASH", "→ → rápido: avança correndo", 1, false, c -> c.placeApart(320f), null,
            c -> c.p1().forwardDashing,
            (c, p1) -> p1.direction = c.t < 3 || (c.t >= 6 && c.t < 9) ? 1 : 0));
        out.add(new Lesson("RECUO", "← ← rápido: salta para trás", 1, false, apart, null,
            c -> c.p1().backdashFrames > 0,
            (c, p1) -> p1.direction = c.t < 3 || (c.t >= 6 && c.t < 9) ? 5 : 0));

        // Golpes no chão, agachado e no ar.
        out.add(hit(character, "GOLPE FRACO", "Perto do boneco, aperte L", "L", 2));
        out.add(hit(character, "GOLPE MÉDIO", "Aperte M", "M", 2));
        out.add(hit(character, "GOLPE FORTE", "Aperte H", "H", 1));
        out.add(hit(character, "FRACO AGACHADO", "Segure ↓ e aperte L", "2L", 2));
        out.add(hit(character, "MÉDIO AGACHADO", "Segure ↓ e aperte M", "2M", 1));
        out.add(hit(character, "FORTE AGACHADO", "Segure ↓ e aperte H", "2H", 1));
        if (character.moves.containsKey("jH")) {
            out.add(new Lesson("GOLPE NO AR", "Pule para frente (↗) e aperte H no ar", 1, true,
                c -> c.placeApart(70f), null,
                c -> c.landed("jH"),
                (c, p1) -> jumpIn(c, p1, false)));
        }
        if (character.moves.containsKey("j2H")) {
            out.add(new Lesson("CRAVADA", "No ar, segure ↓ e aperte H: crava o rival no chão", 1, true,
                c -> c.placeApart(70f), null,
                c -> c.landed("j2H"),
                (c, p1) -> jumpIn(c, p1, true)));
        }

        // Combos.
        out.add(new Lesson("COMBO", "L, M e H, um logo depois do outro enquanto acerta", 1, true, close, null,
            c -> c.engine.session(1) != null && c.engine.session(1).hitCount >= 3,
            (c, p1) -> {
                if (c.t == 0) p1.light = true;
                String id = c.p1().attacking() ? c.p1().attack.id : "";
                if (id.equals("L") && c.p1().outcome != CombatFighter.Outcome.NONE) p1.medium = true;
                if (id.equals("M") && c.p1().outcome != CombatFighter.Outcome.NONE) p1.heavy = true;
            }));
        if (character.fighter.autoCombo.length >= 3) {
            out.add(new Lesson("BOTÃO COMBO", "Aperte COMBO várias vezes: os golpes saem em sequência", 1, true,
                close, null,
                c -> c.p1().attacking() && c.p1().autoStep >= 2,
                (c, p1) -> p1.auto = c.t % 8 == 0));
        }

        // Defesa: o boneco ataca.
        out.add(new Lesson("DEFESA", "Segure ← quando o boneco atacar", 2, true, close,
            (c, dummy) -> { if (c.t % 70 == 30) dummy.medium = true; },
            c -> c.blocking() && !c.p1().crouching,
            (c, p1) -> p1.direction = 5));
        out.add(new Lesson("DEFESA BAIXA", "Segure ↙ contra o golpe baixo", 2, true, close,
            (c, dummy) -> {
                if (c.t % 70 >= 26) dummy.direction = 3;
                if (c.t % 70 == 30) dummy.light = true;
            },
            c -> c.blocking() && c.p1().crouching,
            (c, p1) -> p1.direction = 4));

        // Especiais: os comandos vêm do próprio personagem.
        if (character.fighter.energy != null) {
            int[] command = character.fighter.energyCommand;
            out.add(new Lesson("ESPECIAL DE ENERGIA", arrows(command) + " + L, M ou H: lança energia", 1, false,
                c -> c.placeApart(160f), null,
                c -> c.doing("S"),
                (c, p1) -> special(c, p1, command, "M")));
        }
        String[] names = {"S2", "S3", "S4"};
        for (String id : names) {
            CharacterDefinition.Special special = character.specials.get(id);
            if (special == null) continue;
            int[] command = special.command();
            String buttons = special.buttons;
            String button = buttons.substring(0, 1);
            out.add(new Lesson("ESPECIAL " + id, arrows(command) + " + " + orList(buttons), 1, true, close, null,
                c -> c.landed(id),
                (c, p1) -> special(c, p1, command, button)));
        }

        // Agarrão, Super e Ultra.
        out.add(new Lesson("AGARRÃO", "Colado no boneco, aperte L + M juntos (o botão entre eles)", 1, true,
            c -> c.placeApart(8f), null,
            c -> c.event("THROW"),
            (c, p1) -> p1.grab = c.t == 2));
        out.add(new Lesson("SUPER", "Com 1 barra cheia, aperte SUPER", 1, false,
            c -> { c.placeApart(140f); c.team[0].superMeter = Math.max(c.team[0].superMeter, BAR); }, null,
            c -> c.doing("SUPER"),
            (c, p1) -> p1.superAttack = c.t == 2));
        out.add(new Lesson("ULTRA", "Com 3 barras, segure ↓ e aperte SUPER", 1, true,
            c -> { c.placeApart(60f); c.team[0].superMeter = Math.max(c.team[0].superMeter, CombatConfig.ULTRA_COST); },
            null,
            c -> c.p1().status == CombatFighter.Status.ULTRA,
            (c, p1) -> {
                if (c.t < 4) p1.direction = 3;
                p1.ultra = c.t == 2;
            }));
        return out;
    }

    /** Lição de golpe normal: acertar o boneco com {@code id}. O texto diz o que o golpe faz. */
    private static Lesson hit(CharacterDefinition character, String title, String how, String id, int times) {
        CharacterDefinition.Move move = character.moves.get(id);
        String effect = move == null ? null : effect(move.attack.launch);
        return new Lesson(title, effect == null ? how : how + ": " + effect, times, true, c -> c.placeApart(CLOSE), null,
            c -> c.landed(id),
            (c, p1) -> {
                if (id.startsWith("2")) p1.direction = 3;
                if (c.t % 40 == 2) DemoDirector.Ctx.press(p1, id);
            });
    }

    private static String effect(AttackDefinition.Launch launch) {
        switch (launch) {
            case KNOCKDOWN: return "derruba";
            case LAUNCH: return "lança para cima";
            case SLAM: return "joga no chão";
            case WALL_BOUNCE: return "joga o rival na parede";
            case GROUND_BOUNCE: return "faz o rival quicar no chão";
            default: return null;
        }
    }

    /**
     * Pulo para frente e H na descida; a cada tentativa aperta um pouco mais tarde, até
     * achar o ponto em que o golpe aéreo alcança o boneco.
     */
    private static void jumpIn(Ctx c, FighterInput p1, boolean down) {
        int tryIndex = c.t / 70, frame = c.t % 70;
        if (frame < 3) p1.direction = 8;
        int press = 8 + (tryIndex % 6) * 4;
        if (down && frame >= press - 2 && frame <= press + 2) p1.direction = 3;
        if (frame == press) p1.heavy = true;
    }

    /** Faz o comando (direções relativas, 1 = frente) e aperta o botão no fim. */
    private static void special(Ctx c, FighterInput p1, int[] command, String button) {
        int step = c.t / 3;
        if (step < command.length) {
            p1.direction = MotionParser.relative(command[step], c.p1().facing);
        } else if (step == command.length) {
            p1.direction = MotionParser.relative(command[command.length - 1], c.p1().facing);
            DemoDirector.Ctx.press(p1, button);
        }
    }

    /** Direções relativas (1 = frente) em setas, virado para a direita. */
    static String arrows(int[] command) {
        StringBuilder sb = new StringBuilder();
        for (int d : command) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(d >= 1 && d <= 8 ? ControlsLayout.DPAD_LABELS[d - 1] : "?");
        }
        return sb.toString();
    }

    private static String orList(String buttons) {
        if (buttons.length() == 1) return buttons;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buttons.length(); i++) {
            if (i > 0) sb.append(i == buttons.length() - 1 ? " ou " : ", ");
            sb.append(buttons.charAt(i));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ andamento

    private final Ctx ctx = new Ctx();
    private List<Lesson> lessons = new ArrayList<>();
    private int index = -1;
    private int count;
    private boolean wasMet;
    private int praise = -1;
    private int complete = -1;
    private int farFrames;
    private boolean[] done = new boolean[0];
    private boolean finished;
    /** O jogo mostra um quadro antes de cada lição (e no fim); o treino espera por ele. */
    private boolean panels;
    /** O jogo pede para segurar o "treino completo" (a conversa do fim ainda está tocando). */
    private boolean holdEnd;
    /** Quadro que o treino espera agora (id da lição, ou "fim"), ou null. */
    private String panelWanted;
    /** Acontecimentos deste passo para quem assiste: "licao", "impacto", "levou", "fim". */
    private final List<String> reactions = new ArrayList<>();
    private int lastImpact = -999, lastHurt = -999, frame;

    boolean active() { return index >= 0 && !finished; }
    /** Liga os quadros entre as lições (o jogo responde com {@link #panelDone}). */
    void setPanels(boolean on) { panels = on; }
    /** Segura o fim do treino enquanto for verdadeiro (PULAR ainda termina na hora). */
    void setHoldEnd(boolean hold) { holdEnd = hold; }
    /** Id do quadro que o treino espera agora, ou null. */
    String panelWanted() { return active() ? panelWanted : null; }
    /** O quadro acabou: a lição começa (ou o treino termina). */
    void panelDone() { panelWanted = null; }
    /** Acontecimentos do último passo (a lista é reaproveitada). */
    List<String> reactions() { return reactions; }
    /** Acabou (todas as lições, ou pulou a última): o jogo segue para o que vem depois. */
    boolean finished() { return finished; }
    int lessonIndex() { return index; }
    int lessonCount() { return lessons.size(); }
    Lesson lesson() { return active() && index < lessons.size() ? lessons.get(index) : null; }
    int count() { return count; }
    boolean praising() { return praise >= 0; }
    /** Quadros desde o "MUITO BEM!" (a animação do letreiro). */
    int praiseFrame() { return Math.max(0, praise); }
    boolean completing() { return complete >= 0; }
    /** Lições cumpridas (não puladas) do treino atual. */
    boolean[] done() { return done.clone(); }

    /**
     * Começa o treino: {@code team} é o lado do jogador, o boneco é o lutador 1 do motor.
     * {@code homeX} é onde o jogador fica no começo de cada lição.
     */
    void start(CombatEngine engine, FighterState[] team, float homeX) {
        ctx.engine = engine;
        ctx.team = team;
        ctx.homeX = homeX;
        lessons = lessons(engine.fighter(0).character());
        done = new boolean[lessons.size()];
        finished = false;
        complete = -1;
        reactions.clear();
        begin(0);
    }

    void stop() {
        index = -1;
        finished = false;
        praise = complete = -1;
        panelWanted = null;
        reactions.clear();
    }

    private void begin(int i) {
        index = i;
        count = 0;
        wasMet = false;
        praise = -1;
        farFrames = 0;
        ctx.t = 0;
        ctx.forward = ctx.back = 0f;
        ctx.landed = "";
        ctx.engine.setTeam(0, ctx.team);
        for (FighterState f : ctx.team) {
            f.restoreLife();
            f.superMeter = 0;
            f.refreshHudLabels();
        }
        ctx.dummy().state.restoreLife();
        ctx.dummy().state.superMeter = 0;
        lessons.get(i).setup.apply(ctx);
        panelWanted = panels ? lessons.get(i).id : null;
    }

    /** PULAR: vai para a próxima lição (ou termina, na última). */
    void skip() {
        if (!active()) return;
        if (complete >= 0) {
            finished = true;
            return;
        }
        panelWanted = null;
        next();
    }

    private void next() {
        if (index + 1 < lessons.size()) {
            begin(index + 1);
        } else {
            praise = -1;
            complete = 0;
            reactions.add("fim");
            panelWanted = panels ? "fim" : null;
        }
    }

    /** Golpes que fazem quem assiste se espantar. */
    private static final java.util.Set<String> BIG = new java.util.HashSet<>(java.util.Arrays.asList(
        "H", "2H", "jH", "j2H", "S2", "S3", "S4", "SUPER", "ULTRA", "THROW"));
    /** Intervalo mínimo entre duas reações do mesmo tipo (quadros). */
    private static final int REACTION_GAP = 75;

    /** Golpe forte que acertou ("impacto") e golpe do boneco que pegou o jogador ("levou"). */
    private void watch() {
        boolean impact = false, hurt = false;
        for (CombatEngine.HitEvent e : ctx.engine.events()) {
            if (e.attacker == 0 && !e.blocked && BIG.contains(e.moveId)) impact = true;
            if (e.attacker == 1 && !e.blocked) hurt = true;
        }
        for (String cue : ctx.engine.cues()) {
            if ("WALL_BOUNCE".equals(cue) || "GROUND_BOUNCE".equals(cue)) impact = true;
        }
        if (impact && frame - lastImpact >= REACTION_GAP) {
            lastImpact = frame;
            reactions.add("impacto");
        }
        if (hurt && frame - lastHurt >= REACTION_GAP) {
            lastHurt = frame;
            reactions.add("levou");
        }
    }

    /** Entrada do boneco para o próximo passo do motor (o jogador usa o próprio pad). */
    void fillDummy(FighterInput dummy) {
        dummy.clear();
        Lesson l = lesson();
        if (l == null || l.dummy == null || praise >= 0 || complete >= 0 || panelWanted != null) return;
        l.dummy.drive(ctx, dummy);
    }

    /** Exemplo de entrada que cumpre a lição atual (testes e demonstração). */
    void fillExample(FighterInput p1) {
        p1.clear();
        Lesson l = lesson();
        if (l == null || praise >= 0 || complete >= 0 || panelWanted != null) return;
        l.example.drive(ctx, p1);
    }

    /** Depois do passo do motor: confere a lição, segura a vida e anda o treino. */
    void afterStep() {
        reactions.clear();
        if (!active()) return;
        frame++;
        if (panelWanted != null) return;
        // Treino: ninguém cai. A vida volta cheia e o Ultra tem barra para sair.
        for (int i = 0; i < 2; i++) {
            FighterState s = ctx.engine.fighter(i).state;
            if (s.life < s.profile.maxLife) s.restoreLife();
        }
        if (complete >= 0) {
            if (++complete >= COMPLETE_FRAMES && !holdEnd) finished = true;
            return;
        }
        if (praise >= 0) {
            if (++praise >= PRAISE_FRAMES) next();
            return;
        }
        Lesson l = lessons.get(index);
        watch();
        ctx.observe();
        boolean met = l.check.met(ctx);
        if (met && !wasMet) count++;
        wasMet = met;
        ctx.t++;
        if (count >= l.times) {
            done[index] = true;
            praise = 0;
            reactions.add("licao");
            return;
        }
        // O boneco foi parar longe numa lição de golpe: volta para perto quando os dois param.
        CombatFighter p = ctx.p1(), d = ctx.dummy();
        boolean still = p.canAct() && p.grounded && d.canAct() && d.grounded;
        farFrames = l.close && still && ctx.gap() > TOO_FAR ? farFrames + 1 : 0;
        if (farFrames >= RESET_AFTER) {
            farFrames = 0;
            ctx.placeApart(CLOSE);
        }
    }
}
