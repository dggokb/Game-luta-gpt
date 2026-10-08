package com.gamelutagpt;

/**
 * Scripted demos of the systems added in v0.80…v0.85 and v0.87. While one plays, this class writes
 * both fighters' input every frame (the pad and the CPU brain are ignored), so the feature
 * plays out on its own. Each demo is a list of steps: the fight is restaged, both stand
 * still for {@link #LEAD_IN} frames under the caption, then the step's script runs.
 * Pure Java over the engine, so the scripts are checked by JVM tests (DemoDirectorTest).
 */
final class DemoDirector {
    /** Version of each demo, in button order. */
    static final String[] VERSIONS = {"80", "81", "82", "83", "84", "85", "87"};
    /** Frames both fighters stand still at the start of a step, while the caption is read. */
    static final int LEAD_IN = 36;

    /** What a step's script sees and remembers. */
    static final class Ctx {
        CombatEngine engine;
        FighterState[] team;
        /** Frames since the script started (after the lead-in). */
        int t;
        /** Free slots for the script: the frame something happened, a value to compare. */
        int mark = -1;
        int value;

        CombatFighter p1() { return engine.fighter(0); }
        CombatFighter cpu() { return engine.fighter(1); }
        CombatConfig config() { return engine.config; }

        /** Restages with this much room between the two bodies. */
        void placeApart(float x, float gap) {
            float halves = engine.fighter(0).body().halfWidth + engine.fighter(1).body().halfWidth;
            engine.restage(x, x + halves + gap);
        }

        boolean cue(String id) { return engine.cues().contains(id); }

        /** A clean hit (not blocked) of {@code moveId} by {@code attacker} in the last step. */
        boolean hit(int attacker, String moveId) {
            for (CombatEngine.HitEvent e : engine.events()) {
                if (e.attacker == attacker && !e.blocked && moveId.equals(e.moveId)) return true;
            }
            return false;
        }

        String attackId(CombatFighter f) { return f.attacking() ? f.attack.id : ""; }

        /** L > M > H, each press while the previous one plays (the engine cancels on contact). */
        void chain(CombatFighter f, FighterInput in) {
            if (t == 0) in.light = true;
            String id = attackId(f);
            if (id.equals("L")) in.medium = true;
            if (id.equals("M")) in.heavy = true;
        }

        /** Presses the button of a move id ("2M" holds ↓ too). */
        static void press(FighterInput in, String move) {
            if (move.startsWith("2")) in.direction = 3;
            char button = move.charAt(move.length() - 1);
            if (button == 'L') in.light = true;
            if (button == 'M') in.medium = true;
            if (button == 'H') in.heavy = true;
        }

        /**
         * Plays {@code moves} in order: the first one now, each next one once the previous
         * hit or was blocked. {@link #value} keeps how far it got.
         */
        void sequence(CombatFighter f, FighterInput in, String... moves) {
            if (value >= moves.length) return;
            String now = attackId(f);
            if (now.equals(moves[value])) {
                value++;
                return;
            }
            if (value == 0 || (now.equals(moves[value - 1]) && f.outcome != CombatFighter.Outcome.NONE)) {
                press(in, moves[value]);
            }
        }

        /** Screen direction of "back" for a fighter (away from where it looks). */
        static int back(CombatFighter f) { return f.facing > 0 ? 5 : 1; }
        static int forward(CombatFighter f) { return f.facing > 0 ? 1 : 5; }
    }

    interface Setup { void apply(Ctx c); }
    interface Script { void drive(Ctx c, FighterInput p1, FighterInput cpu); }
    interface Check { boolean met(Ctx c); }

    static final class Step {
        final String title;
        final String detail;
        /** Frames of script after the lead-in. */
        final int frames;
        final Setup setup;
        final Script script;
        /** What the step shows (checked every frame; the HUD marks it once it happened). */
        final Check check;

        Step(String title, String detail, int frames, Setup setup, Script script, Check check) {
            this.title = title;
            this.detail = detail;
            this.frames = frames;
            this.setup = setup;
            this.script = script;
            this.check = check;
        }
    }

    private static final float X = 760f;
    private static final int BAR = CombatConfig.METER_PER_BAR;

    static Step[] steps(int demo) {
        switch (demo) {
            case 0: return new Step[] {
                new Step("AGARRÃO", "L+M colado no rival: segura, arremessa e derruba", 110,
                    c -> c.placeApart(X, 10f),
                    (c, p1, cpu) -> { if (c.t == 0) p1.grab = true; },
                    c -> c.hit(0, "THROW")),
                new Step("TECH", "o CPU aperta L+M logo depois de ser pego: escapa sem dano", 80,
                    c -> c.placeApart(X, 10f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.grab = true;
                        if (c.cpu().status == CombatFighter.Status.THROWN && c.mark < 0) c.mark = c.t;
                        if (c.mark >= 0 && c.t == c.mark + 5) cpu.grab = true;
                    },
                    c -> c.cue("TECH")),
                new Step("AGARRÃO NO VAZIO", "longe demais: o agarrão erra e o CPU pune com H", 90,
                    c -> c.placeApart(X, 70f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.grab = true;
                        CombatFighter p = c.p1();
                        if (p.status == CombatFighter.Status.THROW && p.throwPhase == CombatFighter.THROW_WHIFF &&
                            c.mark < 0) c.mark = c.t;
                        if (c.mark >= 0 && c.t == c.mark + 2) cpu.heavy = true;
                    },
                    c -> c.hit(1, "H")),
            };
            case 1: return new Step[] {
                new Step("PUSHBLOCK", "defendendo, M+H empurra o CPU para longe (¼ de barra)", 100,
                    c -> { c.placeApart(X, 24f); c.team[0].superMeter = BAR; },
                    (c, p1, cpu) -> {
                        p1.direction = Ctx.back(c.p1());
                        c.chain(c.cpu(), cpu);
                        if (c.p1().status == CombatFighter.Status.BLOCKSTUN && c.mark < 0) c.mark = c.t;
                        if (c.mark >= 0 && c.t == c.mark + 4) p1.pushblock = true;
                    },
                    c -> c.cue("PUSHBLOCK")),
                new Step("GUARD CANCEL", "defendendo, TAG: o parceiro entra golpeando, invencível (1 barra)", 130,
                    c -> { c.placeApart(X, 24f); c.team[0].superMeter = BAR; },
                    (c, p1, cpu) -> {
                        if (c.mark < 0) p1.direction = Ctx.back(c.p1());
                        c.chain(c.cpu(), cpu);
                        if (c.p1().status == CombatFighter.Status.BLOCKSTUN && c.mark < 0) c.mark = c.t;
                        if (c.mark >= 0 && c.t == c.mark + 4) p1.assist = true;
                    },
                    c -> c.cue("GUARD_CANCEL")),
            };
            case 2: return new Step[] {
                new Step("AIR DASH", "pulo e → → no ar: avança reto e emenda um golpe aéreo", 90,
                    c -> c.engine.restage(X, X + 380f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.jump = true;
                        if (c.t == 10) p1.dash = true;
                        if (c.t == 17) p1.medium = true;
                    },
                    c -> c.p1().airDashFrames > 0 && !c.p1().airDashBack),
                new Step("AIR DASH PARA TRÁS", "pulo e ← ← no ar: recua reto, fugindo do CPU", 80,
                    c -> c.engine.restage(X, X + 260f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.jump = true;
                        if (c.t == 10) p1.backdash = true;
                    },
                    c -> c.p1().airDashFrames > 0 && c.p1().airDashBack),
            };
            case 3: return new Step[] {
                new Step("WALL BOUNCE", "L > M > H em combo: o H joga o CPU na parede e ele volta", 150,
                    c -> c.placeApart(X - 200f, 12f),
                    (c, p1, cpu) -> c.chain(c.p1(), p1),
                    c -> c.cue("WALL_BOUNCE")),
                new Step("GROUND BOUNCE", "pulam juntos; jM e ↓+jH: o ↓+jH crava o CPU no chão e ele quica", 150,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) { p1.jump = true; cpu.jump = true; }
                        String id = c.attackId(c.p1());
                        if (c.t == 9) p1.medium = true;
                        if (id.equals("jM") && c.p1().outcome == CombatFighter.Outcome.HIT) {
                            p1.direction = 3;
                            p1.heavy = true;
                        }
                    },
                    c -> c.cue("GROUND_BOUNCE")),
            };
            case 4: return new Step[] {
                new Step("AIR TECH", "jM acerta no ar; o CPU aperta um botão + trás e escapa invencível", 100,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) { p1.jump = true; cpu.jump = true; }
                        if (c.t == 9) p1.medium = true;
                        CombatFighter d = c.cpu();
                        if (d.status == CombatFighter.Status.AIR_HITSTUN && d.stunLeft <= 1) {
                            cpu.light = true;
                            if (c.mark < 0) c.mark = c.t;
                        }
                        if (c.mark >= 0) cpu.direction = Ctx.back(d);
                    },
                    c -> c.cue("AIR_TECH")),
                new Step("LEVANTAR RÁPIDO", "derrubado (2M), segurar ↑ levanta bem mais cedo", 110,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> { sweep(c, p1); if (down(c)) cpu.direction = 7; },
                    c -> c.cpu().wakeup == CombatFighter.WAKE_QUICK),
                new Step("ROLAMENTO PARA TRÁS", "derrubado, segurar para trás: rola para longe, invencível", 120,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> { sweep(c, p1); if (down(c)) cpu.direction = Ctx.back(c.cpu()); },
                    c -> c.cpu().wakeup == CombatFighter.WAKE_BACK_ROLL),
                new Step("ROLAMENTO PARA FRENTE", "segurar para frente: rola e passa para o outro lado", 120,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> {
                        sweep(c, p1);
                        if (down(c)) cpu.direction = Ctx.forward(c.cpu());
                    },
                    c -> c.cpu().wakeup == CombatFighter.WAKE_FORWARD_ROLL),
            };
            case 5: return new Step[] {
                new Step("DHC", "SUPER e, quando ele sai, TAG: o parceiro entra com o SUPER dele (2 barras)", 200,
                    c -> { c.engine.restage(X - 120f, X + 220f); c.team[0].superMeter = 2 * BAR; },
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.superAttack = true;
                        if (c.mark < 0 && c.engine.teams.dhcReady(0, c.p1())) {
                            c.mark = c.t;
                            p1.assist = true;
                        }
                    },
                    c -> c.cue("DHC")),
                new Step("VIDA VERMELHA", "parte do dano fica vermelha; ↓+TAG troca e quem sai recupera", 260,
                    c -> c.placeApart(X, 24f),
                    (c, p1, cpu) -> {
                        if (c.mark < 0) c.chain(c.cpu(), cpu);
                        CombatFighter p = c.p1();
                        if (c.mark < 0 && c.team[0].recoverableLife > 0 && p.canAct() && p.grounded &&
                            c.engine.session(0) == null) {
                            c.mark = c.t;
                            c.value = c.team[0].life;
                        }
                        if (c.mark >= 0 && c.t == c.mark + 12) {
                            p1.direction = 3;
                            p1.tag = true;
                        }
                    },
                    c -> c.mark >= 0 && c.engine.team(0).pointState() != c.team[0] && c.team[0].life > c.value),
            };
            case 6: return new Step[] {
                new Step("OVERDRIVE", "botão OD (ou SUPER+TAG), 1 vez por round: 8 s mais rápido e com mais barra", 150,
                    c -> c.engine.restage(X - 200f, X + 420f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.overdrive = true;
                        if (c.t == 30) p1.dash = true;
                        if (c.t >= 30 && c.t < 70) p1.direction = 1;
                        if (c.t == 70) p1.light = true;
                    },
                    c -> c.cue("OVERDRIVE")),
                new Step("CANCELS LIVRES", "no Overdrive todo golpe que acerta cancela em outro: H > M > H", 150,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> {
                        if (c.t == 0) p1.overdrive = true;
                        if (c.t >= 24) c.sequence(c.p1(), p1, "H", "M", "H");
                    },
                    c -> c.value >= 3 && c.engine.session(1) != null && c.engine.session(1).hitCount >= 3),
                new Step("OVERDRIVE NO MEIO DO COMBO", "L > M acertou: OD congela o CPU e o combo segue com 2L > 2M", 150,
                    c -> c.placeApart(X, 12f),
                    (c, p1, cpu) -> {
                        c.sequence(c.p1(), p1, "L", "M");
                        if (c.value == 2 && c.p1().outcome == CombatFighter.Outcome.HIT && c.mark < 0) {
                            c.mark = c.t;
                            p1.overdrive = true;
                        }
                        // After the flash: a fresh route the M could not cancel into.
                        if (c.mark >= 0 && c.t > c.mark + 2 && c.t < c.mark + 60) {
                            String id = c.attackId(c.p1());
                            if (id.isEmpty() && c.p1().canAct()) { p1.direction = 3; p1.light = true; }
                            if (id.equals("2L") && c.p1().outcome != CombatFighter.Outcome.NONE) Ctx.press(p1, "2M");
                        }
                    },
                    c -> c.mark >= 0 && c.hit(0, "2M")),
                new Step("VIDA VERMELHA EM CAMPO", "no Overdrive a vida vermelha volta até para quem está lutando", 150,
                    c -> {
                        c.placeApart(X, 200f);
                        c.team[0].takeDamage(3000, c.config().recoverableLifePermille);
                        c.value = c.team[0].life;
                    },
                    (c, p1, cpu) -> { if (c.t == 0) p1.overdrive = true; },
                    c -> c.engine.team(0).pointState() == c.team[0] && c.team[0].life > c.value),
            };
            default: throw new IllegalArgumentException("demo " + demo);
        }
    }

    /** Lying down: the held direction picks how it gets up (held earlier it would jump). */
    private static boolean down(Ctx c) {
        return c.cpu().status == CombatFighter.Status.KNOCKDOWN;
    }

    /** 2M (↓ + M) right away: a low that knocks down. */
    private static void sweep(Ctx c, FighterInput p1) {
        if (c.t <= 2) p1.direction = 3;
        if (c.t == 0) p1.medium = true;
    }

    private final Ctx ctx = new Ctx();
    private Step[] steps;
    private int demo = -1;
    private int step;
    private int frame;
    private boolean met;
    private boolean[] results = new boolean[0];
    private float homeFirstX, homeSecondX;
    // What the demo changes and gives back when it ends.
    private FighterState[] saved = new FighterState[0];
    private int[] savedLife = new int[0], savedRecoverable = new int[0], savedMeter = new int[0];
    private int savedPoint;
    private final boolean[] savedOverdriveUsed = new boolean[2];

    boolean active() { return demo >= 0; }
    /** Demo playing (index into {@link #VERSIONS}), or -1. */
    int demo() { return demo; }
    int stepIndex() { return step; }
    /** Per step of the last demo started: did it show what it is about. */
    boolean[] results() { return results.clone(); }

    String title() {
        if (!active()) return "";
        return "DEMO v0." + VERSIONS[demo] + "  •  " + (step + 1) + "/" + steps.length + "  •  " +
            steps[step].title + (met ? "  ✓" : "");
    }

    String caption() { return active() ? steps[step].detail : ""; }

    /**
     * Starts demo {@code index} on {@code engine}; {@code team} is the player's side and
     * {@code cpu} the opponent. Life, meter and the point are given back by {@link #stop}.
     */
    void start(int index, CombatEngine engine, FighterState[] team, FighterState cpu,
               float homeFirstX, float homeSecondX) {
        if (active()) stop();
        ctx.engine = engine;
        ctx.team = team;
        this.homeFirstX = homeFirstX;
        this.homeSecondX = homeSecondX;
        saved = new FighterState[team.length + 1];
        System.arraycopy(team, 0, saved, 0, team.length);
        saved[team.length] = cpu;
        savedLife = new int[saved.length];
        savedRecoverable = new int[saved.length];
        savedMeter = new int[saved.length];
        for (int i = 0; i < saved.length; i++) {
            savedLife[i] = saved[i].life;
            savedRecoverable[i] = saved[i].recoverableLife;
            savedMeter[i] = saved[i].superMeter;
        }
        savedPoint = engine.team(0).point;
        for (int side = 0; side < 2; side++) savedOverdriveUsed[side] = engine.team(side).overdriveUsed;
        demo = index;
        steps = steps(index);
        results = new boolean[steps.length];
        begin(0);
    }

    private void begin(int index) {
        step = index;
        frame = 0;
        met = false;
        ctx.t = 0;
        ctx.mark = -1;
        ctx.value = 0;
        ctx.engine.setTeam(0, ctx.team);
        ctx.engine.refreshOverdrive(0);
        ctx.engine.refreshOverdrive(1);
        for (FighterState f : saved) {
            f.restoreLife();
            f.superMeter = 0;
        }
        ctx.engine.restage(homeFirstX, homeSecondX);
        steps[index].setup.apply(ctx);
        for (FighterState f : saved) f.refreshHudLabels();
    }

    /** Writes both fighters' input for the next engine step. */
    void fill(FighterInput player, FighterInput opponent) {
        player.clear();
        opponent.clear();
        if (!active() || frame < LEAD_IN) return;
        ctx.t = frame - LEAD_IN;
        steps[step].script.drive(ctx, player, opponent);
    }

    /** After the engine step: checks the step, then moves on (and ends after the last). */
    void afterStep() {
        if (!active()) return;
        if (!met && frame >= LEAD_IN && steps[step].check.met(ctx)) {
            met = true;
            results[step] = true;
        }
        frame++;
        if (frame < LEAD_IN + steps[step].frames) return;
        if (step + 1 < steps.length) begin(step + 1);
        else stop();
    }

    /** Ends the demo: fighters back home, life, meter and the point as before. */
    void stop() {
        if (!active()) return;
        CombatEngine engine = ctx.engine;
        for (int i = 0; i < saved.length; i++) {
            saved[i].life = savedLife[i];
            saved[i].recoverableLife = savedRecoverable[i];
            saved[i].superMeter = savedMeter[i];
            saved[i].refreshHudLabels();
        }
        engine.setTeam(0, ctx.team);
        engine.restage(homeFirstX, homeSecondX);
        for (int side = 0; side < 2; side++) engine.team(side).overdriveUsed = savedOverdriveUsed[side];
        if (savedPoint != 0) {
            engine.team(0).point = savedPoint;
            engine.setFighterState(0, ctx.team[savedPoint]);
        }
        demo = -1;
        steps = null;
    }
}
