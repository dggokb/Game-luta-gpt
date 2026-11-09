package com.gamelutagpt;

import java.util.List;
import java.util.Random;

/**
 * Turns {@link OpponentAi} decisions into engine input for one fighter, frame by frame.
 * The CPU presses buttons like a player: its attacks go through the same buffer and
 * state machine, so it cannot attack while busy or stunned. Pure Java.
 *
 * Tudo o que a CPU faz em resposta ao rival (defender, punir, tirar do ar, pular a onda,
 * confirmar um combo) passa pela {@link CpuPerception}: ela só vê a luta de
 * {@link CpuLevel#reactionFrames} quadros atrás, nunca antes da reação humana. O resto é hábito
 * (segurar a defesa colada) e plano (o caderno de combos), como um jogador.
 */
final class AiController {
    /** Pause after the CPU's own attack ends and after it is hit, in frames. */
    static final int ATTACK_COOLDOWN = 9;
    static final int HIT_COOLDOWN = 15;
    static final int ENERGY_COOLDOWN = 24;
    static final int SUPER_COOLDOWN = 27;

    // Batalha (IA ON): o kit inteiro, como um jogador usa. Chances por oportunidade (no nível 6;
    // os outros níveis escalam por CpuLevel.kitScale).
    /** Frames entre as decisões do kit no neutro (assist, troca, Ultra, SHINKAI). */
    static final int KIT_DECISION_FRAMES = 15;
    static final double ASSIST_NEUTRAL_CHANCE = 0.18;
    /** Golpe que acertou: chama o assist para estender o combo. */
    static final double ASSIST_COMBO_CHANCE = 0.45;
    /** O próprio Super saiu: DHC com o Super do parceiro. */
    static final double DHC_CHANCE = 0.55;
    /** Assist em campo: Assist → Tag. */
    static final double CONVERT_CHANCE = 0.30;
    /** Vida baixa e parceiro melhor: troca (↓ + TAG). */
    static final double TAG_CHANCE = 0.40;
    static final float TAG_LIFE_RATIO = 0.40f;
    static final double ULTRA_CHANCE = 0.35;
    /** Distância até onde a investida do Ultra alcança (com folga). */
    static final float ULTRA_REACH = 380f;
    /** Ultra emendado no fim do combo e na punição: chance (vezes a inteligência do nível). */
    static final double ULTRA_LINK_CHANCE = 0.8, ULTRA_PUNISH_CHANCE = 0.7;
    /** Quadros de rival preso a partir dos quais o Ultra pune com certeza. */
    static final int ULTRA_PUNISH_MIN = 10;
    /** O Super mata se o dano dele (com a escala do combo, por baixo) passa da vida que sobra. */
    static final double KILL_SCALE = 0.6;
    /** Quadros que ela lembra que o rival ficou na defesa (para agarrar quem só defende). */
    static final int RIVAL_BLOCK_MEMORY = 90;
    /** SHINKAI (barra cheia) no meio do combo, ou no neutro com a vida mais baixa. */
    static final double SHINKAI_COMBO_CHANCE = 0.15;
    static final double SHINKAI_LOW_LIFE_CHANCE = 0.08;
    /** A cada decisão do neutro com o rival no SHINKAI e a barra dela cheia: responder (o Clash). */
    static final double SHINKAI_ANSWER_CHANCE = 0.25;
    /** Defendendo: Guard Cancel Tag ou pushblock na sequência de bloqueio. */
    static final double GUARD_CANCEL_CHANCE = 0.25;
    static final double PUSHBLOCK_CHANCE = 0.35;
    /** Com a própria guarda quase quebrando ela foge da pressão bem mais (e espera menos defendendo). */
    static final double GUARD_CANCEL_LOW_GUARD_CHANCE = 0.5;
    static final double PUSHBLOCK_LOW_GUARD_CHANCE = 0.8;

    // Especiais S2–S4 (a função de cada um vem dos dados: CpuSpecials). Chances no nível 8; os
    // outros níveis usam menos (pela inteligência e pela reação do nível).
    /** A cada decisão do neutro: chance de trocar o que decidiu por um especial que serve ali. */
    static final double SPECIAL_NEUTRAL_CHANCE = 0.06;
    /** Viu golpe ou bola chegando e tem a postura certa (contra-golpe, esquiva, repel, armadura). */
    static final double SPECIAL_REACT_CHANCE = 0.45;
    /** Tirando do ar: parte das vezes com o especial que sobe, em vez do 2H. */
    static final double SPECIAL_ANTI_AIR_SHARE = 0.5;
    /** Pulando para dentro com um especial de mergulho: chance (uma vez por pulo). */
    static final double SPECIAL_DIVE_CHANCE = 0.35;
    /** No neutro, golpe de especial (sem projétil) só se sair até este quadro. */
    static final int SPECIAL_NEUTRAL_MAX_STARTUP = 16;
    /** O especial de vida (p12) só com a vida acima disto. */
    static final float SPECIAL_LIFE_FLOOR = 0.3f;

    private static final int GUARD_NONE = 0, GUARD_HIGH = 1, GUARD_LOW = 2, GUARD_ANY = 3;
    /** Hábito de esperar agachada quando o rival não está atacando (antes 0,35). */
    static final double CALM_HABIT_SCALE = 0.1;
    /**
     * Na beira do jab do rival: chance (vezes a de defender) de parar em vez de entrar andando,
     * e a folga da beira.
     */
    static final double EDGE_STOP = 0.3;
    static final float EDGE_MARGIN = 20f;
    /** Perto, no respiro entre os golpes dela: chance de segurar a defesa (vezes a de defender). */
    static final double COOLDOWN_GUARD = 0.25;
    /** Quadros desde o último golpe do rival em que ela ainda se sente pressionada. */
    static final int PRESSURE_FRAMES = 30;
    /** Quadros que ela segura a defesa por hábito (de MIN a MIN + SPREAD - 1). */
    static final int HABIT_GUARD_MIN = 8, HABIT_GUARD_SPREAD = 8;
    /**
     * Pressionada: o quanto a defesa por hábito sobe (vezes a de defender reagindo); dobra no
     * nível 8, que respeita mais a pressão.
     */
    static final double PRESSURE_GUARD = 0.05;
    /**
     * Defesa por hábito agachada (segura tudo no chão): sempre contra quem está agachado; contra
     * quem está em pé, esta parte das vezes no nível 1, subindo até quase sempre no 8 (o certo
     * a fazer, mas agachada o tempo todo ela parecia grudada no chão).
     */
    static final double HABIT_LOW = 0.3;
    /**
     * Quadros que ela segura a defesa quando vê um golpe vindo: o bastante para o golpe chegar,
     * mais {@link #REACT_GUARD_EXTRA} no nível 8 (que segura também o próximo jab da pressão).
     * Antes eram 24 em todo nível, e contra quem ataca sem parar ela não largava a defesa.
     */
    static final int REACT_GUARD_FRAMES = 14, REACT_GUARD_EXTRA = 10;
    /** As duas alturas seguram, e ela defende em pé (o projétil do meio). */
    private static final int GUARD_HIGH_ONLY = 4;

    private final OpponentAi ai;
    private final OpponentAi.Situation situation = new OpponentAi.Situation();
    /** Sorteios do kit: separados, para não mexer na sequência das decisões do OpponentAi. */
    private final Random kitRandom;
    /** Sorteios das reações e do combo (defender, punir, confirmar). */
    private final Random reactRandom;
    private final CpuPerception perception = new CpuPerception();
    private CpuLevel level = CpuLevel.of(CpuLevel.DEFAULT);
    private boolean fullKit;
    private int kitTimer;
    /** Golpe para o qual os planos de combo já foram sorteados, e em que frame dele. */
    private AttackDefinition plannedAttack;
    private int plannedAttackFrame;
    private boolean assistPlan, dhcPlan, shinkaiPlan;
    private boolean wasBlocking, guardCancelPlan, pushblockPlan;
    private boolean wasAssistOut, convertPlan;
    private boolean enabled;
    private boolean movingForward;
    private boolean movingBack;
    private boolean dashing;
    private int cooldown;
    private AttackDefinition lastAttack;

    // Quadro (do motor) em que o golpe atual da CPU acertou pela primeira vez, ou -1.
    private int ownHitFrame = -1;

    // Caderno de combos: a rota em andamento e o passo dela.
    private List<String> route;
    private int step;
    private boolean awaitingStart;
    private int routeWait;
    private AttackDefinition lastSeenAttack;
    private int lastSeenAttackFrame;
    private boolean stepDecided, stepGo, commitDecided, commitGo;
    /** O golpe atual da rota acertou e vai ter a recuperação cortada (o próximo passo vira link). */
    private boolean routeCut;

    // Defesa.
    private int guardFrames;
    private boolean guardLow;
    /** A defesa atual é hábito (não viu ameaça): punir e tirar do ar passam na frente dela. */
    private boolean guardHabit;

    /** O quadro que ela acabou de soltar foi defesa por hábito (não reação a nada que viu). */
    private boolean habitGuardFrame;

    /**
     * Defendendo em pé porque o rival vinha pelo ar: quando ele cai perto, ela troca para a defesa
     * baixa (o registro real: jM defendido em pé, a rasteira ao cair entrava sempre).
     */
    private boolean altaPorPulo;
    /** Levantar ou sair do atordoamento defendendo: -1 ainda não decidiu, 0 não, 1 sim. */
    private int saidaPlano = -1;
    private boolean saidaPresa;
    private boolean saidaBaixa;

    /** Está esperando defendendo por hábito (não reagindo a nada que viu). */
    boolean guardingByHabit() { return habitGuardFrame || guardHabit && guardFrames > 0; }
    private int anticipationTimer;
    private int lastGuardSerial = -1, lastJumpSerial = -1, lastProjectileSerial = -1;
    private int lastOpponentAttackSerial = -1, lastOpponentAttackFrame = Integer.MIN_VALUE / 2;
    private CharacterDefinition threatFor;
    private float threatRange;

    // Cartas: punir e anti-aéreo.
    private int lastPunishSerial = -1, lastAntiAirSerial = -1;
    /** Guard break do rival: a última quebra que ela viu e se decidiu castigar. */
    private int lastBreakSerial = -1;
    private boolean breakPlan;
    /** A própria guarda está no aviso neste quadro. */
    private boolean lowGuard;
    private boolean antiAirPlan;
    private boolean jumpOverProjectile;

    // ------------------------------------------------- contra quem fica soltando bola

    /**
     * Quadros até a bola chegar (pela frente dela) em que o pulo para frente passa por cima:
     * medido para todas as bolas do jogo, a janela comum é ~12,8–16,2 quadros. Ela espera a
     * bola chegar a esse tempo e só então pula (pulando assim que via, levava a bola na subida
     * ou na descida).
     */
    static final float JUMP_OVER_LEAD = 14.5f;
    /** Erro de mira do pulo no nível 1 (some no 8), em quadros para cada lado. */
    static final float JUMP_OVER_SLOP = 4f;
    /** O quanto o pulo para frente anda por quadro (fecha a distância junto com a bola). */
    static final float JUMP_FORWARD_STEP = 6.8f;
    /** Pular por cima só vale com o rival a até isto: de mais longe ela cai na frente da próxima. */
    static final float JUMP_OVER_RANGE = 560f;
    /** Bolas do rival nos últimos {@link #ZONING_MEMORY} quadros para ela achar que ele está zoneando. */
    static final int ZONING_SHOTS = 2, ZONING_MEMORY = 240;
    /** Projétil do rival que ela está esperando para pular por cima (serial), ou -1. */
    private int jumpPlanSerial = -1;
    private float jumpPlanLead;
    /** Quadros das últimas bolas do rival que ela viu. */
    private final int[] rivalShots = new int[ZONING_SHOTS];
    private int rivalShotCount;
    /** Levou ou defendeu bola: quando o atordoamento acabar, segura a defesa (como um humano). */
    private boolean projectileStunGuard;
    /** Quadros em que ela ainda segura a direção do pulo que acabou de pedir. */
    private int jumpHold, jumpHoldDirection;
    /**
     * Pulo para dentro (por cima da bola ou antecipando): o aéreo sai logo que ela começa a
     * descer (medido: do quadro ~15 ao ~25 do pulo ele acerta quem está no chão de 160 a 340 de
     * distância). Esperando chegar mais perto do chão, saía tarde e ela caía na bola seguinte.
     */
    private boolean jumpIn;
    static final float JUMP_IN_REACH = 230f;
    /** O rival no ar mais perto que isso pode cruzar: a CPU decide (uma vez por pulo) se lê o lado. */
    static final float CROSS_WATCH = 140f;
    /** Fração do reactGuard do nível com que a CPU lê o crossup (defende para o lado certo). */
    static final double CROSS_READ = 0.5;
    /** Com o rival no ar por cima dela: o lado em que ela o viu (com o atraso da reação) e a leitura. */
    private CpuPerception.Snapshot seenNow;
    private boolean crossRead = true, rivalOverhead;
    /** O rival caído perto: um pulo para cruzar no levantar (uma vez por queda). */
    private boolean crossPlanned;
    /** Pulando para cruzar: desce com o jM. */
    private boolean crossJump;
    /** Sorteios do crossup à parte (não mexem na sequência das outras decisões). */
    private final java.util.Random crossRandom = new java.util.Random(0x5EED);
    static final int JUMP_HOLD_FRAMES = 5;
    /**
     * Quadros antes da bola chegar em que ela começa a segurar a defesa. Antes disso ela segue
     * (avançando): segurar trás com a bola longe era andar para trás, e ela nunca chegava em
     * quem fica soltando bola.
     */
    static final float GUARD_PROJECTILE_LEAD = 7f;
    /** Projétil do rival que ela vai defender (serial), ou -1; e se agachada. */
    private int guardPlanSerial = -1;
    private boolean guardPlanLow;
    /** Quadros até a bola planejada chegar (para não começar golpe que não termina antes). */
    private float guardPlanArrival;
    /** A bola que ela já está defendendo (serial), ou -1. */
    private int guardingSerial = -1;
    /** Andar para frente, por quadro (fecha a distância com a bola). */
    static final float WALK_STEP = 5f;
    /** Com a bola a esta distância (em quadros), ela não começa golpe: só anda até defender. */
    static final float PROJECTILE_BUSY_FRAMES = 30f;

    /** Sorteios dos especiais à parte (não mexem na sequência das outras decisões). */
    private final Random specialRandom = new Random(0x5BEC1A1L);
    /** Golpe e bola do rival para os quais a resposta de especial já foi sorteada. */
    private int specialMeleeSerial = -1, specialShotSerial = -1;
    /** Resposta de especial esperando a hora (o golpe/a bola ainda longe da janela dela). */
    private CpuSpecials.Option meleePlan, shotPlan;
    /** Pulo atual: o mergulho já foi sorteado, e se vai. */
    private boolean diveDecided, divePlan;
    /** Regras de carga do motor da luta atual (os especiais de carga). */
    private CombatConfig chargeConfig;
    /** Este anti-aéreo vai com o especial que sobe (se ele alcançar). */
    private boolean antiAirSpecial;

    /** Por que a CPU apertou o último golpe ("neutro", "punir", "antiaereo"): para os testes. */
    String lastReason = "";
    /** Dragon Rush: o voo dele já sorteado (anti-rush) e o compasso das pancadas na sequência dela. */
    private int lastRushSerial = -1, rushTick;
    private boolean antiRushPlan;

    // Requests of the current decision, delivered once.
    private String attack;
    private String special;
    private boolean superAttack;
    private boolean backdash;
    private boolean jump;
    private boolean superJump;
    private boolean grab;
    /** Frames left before teching the throw it is caught in; -1 no tech, -2 not decided. */
    private int techDelay = -2;
    /** Recovery planned for the current air hitstun / knockdown (relative direction), or -2 undecided. */
    private int airTechPlan = -2;
    private int wakeupPlan = -2;

    private final OpponentAi.Actions actions = new OpponentAi.Actions() {
        @Override public void stop() {
            movingForward = false;
            movingBack = false;
            dashing = false;
        }
        @Override public void attack(String type) { attack = type; }
        @Override public void superAttack() { superAttack = true; }
        @Override public void energy(String strength) { special = strength; }
        @Override public void backdash() { backdash = true; }
        @Override public void moveForward(boolean dash) {
            movingForward = true;
            dashing = dash;
        }
        @Override public void moveBack() { movingBack = true; }
        @Override public void grab() { grab = true; }
        @Override public void jump(boolean superJumpRequest) {
            jump = true;
            superJump = superJumpRequest;
        }
    };

    AiController(OpponentAi ai) {
        this(ai, new Random());
    }

    AiController(OpponentAi ai, Random kitRandom) {
        this.ai = ai;
        this.kitRandom = kitRandom;
        // A semente das reações sai da IA: a mesma semente dá a mesma luta.
        this.reactRandom = new Random(ai.seed());
        this.planoRandom = new Random(ai.seed() ^ 0x9A7E5L);
    }

    /**
     * Batalha: além de andar, bater, soltar energia e Super, o CPU usa o kit inteiro — assist,
     * troca, Assist → Tag, DHC, Guard Cancel, pushblock, SHINKAI e Ultra.
     */
    void setFullKit(boolean value) {
        fullKit = value;
        resetKit();
    }

    boolean fullKit() { return fullKit; }

    /** Nível da CPU, de 1 a 8 (fora disso vira o mais próximo). */
    void setLevel(int number) {
        level = CpuLevel.of(number);
    }

    /**
     * Segundos que faltam no relógio da luta (negativo = sem relógio: treino, cena). Perto do fim
     * ela fica com pressa ({@link OpponentAi#urgency}): ataca mais, recua e espera menos.
     */
    void setSecondsLeft(int seconds) {
        urgency = OpponentAi.urgency(seconds);
    }

    float urgency() { return urgency; }

    private float urgency, hurry;

    int level() { return level.number; }

    // ---------------------------------------------------------------- leitura do rival

    /**
     * O que ela aprendeu do rival e das próprias tentativas ({@link CpuLeitura}). Não zera com
     * {@link #setEnabled}: passa de round para round, e quem cria a luta pode passar a mesma de
     * uma luta para a outra ({@link #setLeitura}).
     */
    private CpuLeitura leitura = new CpuLeitura();
    /** Desligada, ela observa mas não usa (as baterias comparam com e sem). */
    private boolean leituraAtiva = true;

    void setLeitura(CpuLeitura l) { leitura = l != null ? l : new CpuLeitura(); }

    CpuLeitura leitura() { return leitura; }

    void setLeituraAtiva(boolean value) { leituraAtiva = value; }

    /**
     * Certeza mínima para agir pela leitura: abaixo disso o "padrão" é ruído (quem varia dá 0,2 a
     * 0,3), e agir nele piorava a luta contra quem não tem hábito.
     */
    static final double CERTEZA_MINIMA = 0.3;
    /**
     * Resposta arriscada (chegar colado para agarrar quem está levantando: se ele apertar, ela
     * leva): só com o hábito bem claro.
     */
    static final double CERTEZA_ARRISCADA = 0.45;

    /** Chance de agir pelo hábito lido na situação: zero abaixo da certeza mínima. */
    private double agirPelaLeitura(CpuLeitura.Situacao st) {
        double c = leitura.certeza(st);
        return c < CERTEZA_MINIMA ? 0.0 : c * pesoLeitura();
    }

    /** Quanto a leitura pesa: nada no nível 1, inteira no 8; zero desligada. */
    private double pesoLeitura() {
        return leituraAtiva ? smart() : 0.0;
    }

    /** Observação em andamento: a situação, quando começou e de onde ele partiu. */
    private CpuLeitura.Situacao obsSituacao;
    private int obsInicio, obsPulo;
    private float obsX;
    private CombatFighter.Status statusVisto;
    private boolean bolaViva;
    /** Quadros de antiaéreo pronto (ela leu que ele vai pular). */
    private int antiAereoLido;
    /** Quadros parada esperando o que leu (o pulo, o golpe dele). */
    private int esperaLida;
    /** A sequência dela foi defendida (a leitura de "depois de defender" vale no fim dela). */
    private boolean sequenciaDefendida;
    /** Leitura do levantar já decidida para esta queda (a resposta), ou null. */
    private CpuLeitura.Resposta levantarLido;
    private boolean levantarDecidido;
    /** Quadros do plano "chegar e agarrar" (ela leu que ele só defende). */
    private int agarrarLido;
    static final int PLANO_AGARRAR = 70;

    /** A tentativa de chegar que está valendo, e quando começou. */
    private CpuLeitura.Abordagem abordagem;
    private int abordagemInicio;

    static final int JANELA_LEITURA = 24, JANELA_BOLA = 30, JANELA_ABORDAGEM = 50;
    /** Ele andou isto (px) na janela para a frente: avançou. */
    static final float ANDOU = 40f;
    /**
     * Para trás precisa ir longe para ser "recuou" (o backdash): segurar trás é defender, e andar
     * defendendo não é fugir.
     */
    static final float RECUOU = 100f;
    /** A bola dela está a esta distância dele, vindo: começa a ver o que ele faz com ela. */
    static final float BOLA_PERTO = 320f;

    /**
     * Observa o rival pelo que viu: quando ele sai da defesa, quando levanta e quando a bola dela
     * vai nele, o que ele faz logo em seguida. Também fecha a tentativa dela de chegar (deu certo
     * ou foi punida).
     */
    private void observar(CombatEngine engine, int self, CombatFighter me, CpuPerception.Snapshot seen) {
        if (abordagem != null) {
            int passou = engine.frame() - abordagemInicio;
            boolean encostou = false;
            for (CombatEngine.HitEvent ev : engine.events()) if (ev.attacker == self && !ev.blocked) encostou = true;
            if (me.framesSinceHit == 0 && passou > 0) {
                leitura.falhou(abordagem);
                abordagem = null;
            } else if (encostou) {
                leitura.deuCerto(abordagem);
                abordagem = null;
            } else if (passou > JANELA_ABORDAGEM) {
                abordagem = null;
            }
        }
        // Como ele entra nela: golpes de chão (altura, se entraram) e pulos que chegaram.
        CombatFighter rival = engine.fighter(1 - self);
        // Só o golpe que abre (o resto do combo não é "ele entrou de novo").
        ComboSession sessao = engine.session(self);
        boolean abre = sessao == null || !sessao.active || sessao.hitCount <= 1;
        for (CombatEngine.HitEvent ev : engine.events()) {
            if (ev.projectile || ev.moveId == null) continue;
            if (ev.defender == self && ev.attacker != self && (abre || ev.blocked)) {
                AttackDefinition a = rival.character() != null ? rival.character().attack(ev.moveId) : null;
                if (ev.moveId.startsWith("j")) leitura.puloNela();
                else if (a != null && a.kind == AttackDefinition.Kind.NORMAL) leitura.golpeDeChao(a.low, !ev.blocked);
            } else if (ev.attacker == self && !ev.blocked && !rival.grounded && "2H".equals(ev.moveId)) {
                leitura.tirouDoAr();
            }
        }
        if (seen == null) return;
        // A bola dela chegando perto dele (a própria bola ela sabe onde está).
        boolean viva = false;
        for (Projectile p : engine.energyProjectiles) {
            if (p.ownerIndex != self) continue;
            float vem = (seen.x - p.x) * p.direction;
            if (vem > 0f && vem < BOLA_PERTO) viva = true;
        }
        if (obsSituacao == null) {
            if (statusVisto == CombatFighter.Status.BLOCKSTUN && seen.status != CombatFighter.Status.BLOCKSTUN) {
                comecaObservar(CpuLeitura.Situacao.DEPOIS_DE_DEFENDER, seen);
            } else if (statusVisto == CombatFighter.Status.WAKEUP && seen.status != CombatFighter.Status.WAKEUP) {
                comecaObservar(CpuLeitura.Situacao.AO_LEVANTAR, seen);
            } else if (viva && !bolaViva && seen.grounded && seen.status == CombatFighter.Status.NEUTRAL) {
                comecaObservar(CpuLeitura.Situacao.CONTRA_BOLA, seen);
            }
        } else {
            int passou = seen.frame - obsInicio;
            int janela = obsSituacao == CpuLeitura.Situacao.CONTRA_BOLA ? JANELA_BOLA : JANELA_LEITURA;
            float rumo = (seen.x - obsX) * Math.signum(me.x - obsX);
            CpuLeitura.Resposta r = null;
            if (seen.attacking()) r = CpuLeitura.Resposta.ATACA;
            else if (!seen.grounded && seen.jumpSerial != obsPulo && seen.status == CombatFighter.Status.NEUTRAL) r = CpuLeitura.Resposta.PULA;
            else if (rumo > ANDOU) r = CpuLeitura.Resposta.AVANCA;
            else if (rumo < -RECUOU) r = CpuLeitura.Resposta.RECUA;
            else if (seen.status == CombatFighter.Status.BLOCKSTUN || seen.status == CombatFighter.Status.HITSTUN ||
                passou > janela) r = CpuLeitura.Resposta.ESPERA;
            if (r != null) {
                leitura.observa(obsSituacao, r);
                obsSituacao = null;
            }
        }
        statusVisto = seen.status;
        bolaViva = viva;
    }

    private void comecaObservar(CpuLeitura.Situacao s, CpuPerception.Snapshot seen) {
        obsSituacao = s;
        obsInicio = seen.frame;
        obsX = seen.x;
        obsPulo = seen.jumpSerial;
    }

    /** O plano "chegar e agarrar": anda (ou corre, de longe) até o agarrão alcançar e agarra. */
    private boolean agarrar(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out, String porque) {
        float gap = Math.abs(target.x - me.x);
        if (gap <= throwReach(engine, me, target)) {
            agarrarLido = 0;
            out.grab = true;
            lastReason = porque + "agarra quem defende";
            return true;
        }
        out.direction = me.facing > 0 ? 1 : 5;
        out.dash = gap > throwReach(engine, me, target) + 120f;
        lastReason = porque + "chega para agarrar";
        return true;
    }

    // ------------------------------------------- contra a bola: o plano que ela aprende

    /** Quadros que cada tentativa contra a bola tem para mostrar o resultado (ou até a próxima). */
    static final int JANELA_PLANO = 75;
    /** Peso do que ela aprendeu sobre o gosto do estilo (no logit do plano). */
    static final double VALOR_PESO = 2.0;
    /** Curiosidade pelo plano pouco tentado (nos níveis altos). */
    static final double EXPLORA = 0.5;
    /** A corrida de energia começa com a bola a isto (quadros, correndo) dela. */
    static final float CORRIDA_LEAD = 12f;

    /**
     * Uma tentativa em andamento: de onde partiu (vidas e distância). O resultado é a vida que ela
     * tirou menos a que perdeu (em vinte avos da vida, meio ponto cada) mais o quanto chegou perto
     * (só se chegou sem apanhar).
     */
    static final class Tentativa {
        int inicio = -1, vidaEu, vidaEle;
        float gap;

        void comeca(CombatEngine engine, CombatFighter me, CombatFighter target) {
            inicio = engine.frame();
            vidaEu = me.state.life;
            vidaEle = target.state.life;
            gap = Math.abs(target.x - me.x);
        }

        /** O resultado, ou NaN quando não conta (uma luta nova no meio: a vida voltou). */
        double resultado(CombatFighter me, CombatFighter target) {
            int tirou = vidaEle - target.state.life, perdeu = vidaEu - me.state.life;
            if (tirou < 0 || perdeu < 0) return Double.NaN;
            double unidade = Math.max(1.0, me.state.profile.maxLife * 0.05);
            // Chegar perto só vale se chegou sem apanhar: pular e levar o antiaéreo também termina
            // colado, e contava como bom (ela insistia no pulo contra quem tira do ar).
            double perto = perdeu > 0 ? 0.0 : (gap - Math.abs(target.x - me.x)) / 400.0;
            double r = 0.5 * (tirou - perdeu) / unidade + perto;
            return Math.max(-1.5, Math.min(1.5, r));
        }
    }

    private final Random planoRandom;
    private CpuLeitura.PlanoBola planoBola;
    private final Tentativa tentativaBola = new Tentativa();
    /** O plano da bola é a corrida de energia: a bola dele, e em que passo do dash está. */
    private int corridaSerial = -1, corridaArmada;
    /** O especial do kit que serve contra a bola que vem (repel ou esquiva), ou null. */
    private CpuSpecials.Option especialContraBola;
    /** O plano é devolver bola: solta a dela assim que puder. */
    private boolean devolverBola;
    /** O plano é a defesa perfeita: a tentativa sai sempre (com o erro de tempo do nível). */
    private boolean forcarPerfeita;

    /** O que ela aprendeu do plano, no logit: com o peso do nível e a curiosidade pelo pouco tentado. */
    private double aprendido(double valor, double vezes) {
        if (!leituraAtiva) return 0.0;
        double s = smart();
        return (0.3 + 0.7 * s) * VALOR_PESO * valor * Math.min(1.0, vezes / 2.0) + EXPLORA * s / (1.0 + vezes);
    }

    /** Sorteio pelos logits (com mais certeza nos níveis altos) entre os que dá; -1 se nenhum. */
    private int sortear(double[] logit, boolean[] pode) {
        // Temperatura: os níveis baixos sorteiam mais solto; nos altos o que rendeu pesa mais, mas
        // o gosto do estilo ainda aparece (cada um com o seu jeito).
        double temp = 1.0 - 0.4 * smart();
        double[] peso = new double[logit.length];
        double total = 0;
        for (int i = 0; i < logit.length; i++) {
            if (!pode[i]) continue;
            peso[i] = Math.exp(logit[i] / temp);
            total += peso[i];
        }
        if (total <= 0) return -1;
        double r = planoRandom.nextDouble() * total;
        int ultimo = -1;
        for (int i = 0; i < logit.length; i++) {
            if (!pode[i]) continue;
            ultimo = i;
            r -= peso[i];
            if (r <= 0) return i;
        }
        return ultimo;
    }

    /**
     * O gosto do estilo e do nível por cada plano contra a bola (logit): o acrobata pula, quem
     * tem bola devolve, quem é agressivo corre, os níveis altos tentam a defesa perfeita. O que ela
     * aprende soma em cima disso, então cada um continua com o seu jeito.
     */
    private double priorPlano(CpuLeitura.PlanoBola p, CpuStyle style, boolean zoneando) {
        double s = smart();
        switch (p) {
            case DEFENDE: return 0.6;
            case PERFEITA: return -1.4 + 5.0 * level.justDefense();
            case PULA: return -0.8 + 1.6 * level.jumpProjectile * style.jumping + (zoneando ? 0.5 : 0.0);
            case CORRE: return -1.0 + (style.aggression - 0.7) + 0.6 * s + (zoneando ? 0.4 : 0.0);
            case DEVOLVE: return -0.7 + 0.9 * (style.zoning - 1.0) + 0.3 * s;
            // O especial do kit contra a bola era a primeira resposta de quem tem (a identidade).
            case ESPECIAL: return 0.5 + 0.8 * (style.reactSpecial - 1.0) + 0.3 * s;
            default: return 0.0;
        }
    }

    /** Escolhe o plano contra esta bola entre os que dá para fazer agora. */
    private CpuLeitura.PlanoBola escolherPlanoBola(boolean[] pode, CombatFighter me, boolean zoneando) {
        CpuStyle style = CpuStyle.of(me.character());
        CpuLeitura.PlanoBola[] planos = CpuLeitura.PlanoBola.values();
        double[] logit = new double[planos.length];
        for (CpuLeitura.PlanoBola p : planos) {
            logit[p.ordinal()] = priorPlano(p, style, zoneando) + aprendido(leitura.valor(p), leitura.vezes(p));
        }
        int i = sortear(logit, pode);
        return i < 0 ? CpuLeitura.PlanoBola.DEFENDE : planos[i];
    }

    private void comecarPlanoBola(CpuLeitura.PlanoBola p, CombatEngine engine, CombatFighter me, CombatFighter target) {
        fecharTentativa(planoBola, tentativaBola, me, target);
        planoBola = p;
        tentativaBola.comeca(engine, me, target);
    }

    /** Fecha a tentativa do plano (da bola ou da meia distância) e soma o resultado. */
    private void fecharTentativa(Enum<?> plano, Tentativa t, CombatFighter me, CombatFighter target) {
        if (plano == null || t.inicio < 0) return;
        double r = t.resultado(me, target);
        if (!Double.isNaN(r)) {
            if (plano instanceof CpuLeitura.PlanoBola) leitura.resultado((CpuLeitura.PlanoBola)plano, r);
            else if (faixaMeio != null) leitura.resultado(faixaMeio, (CpuLeitura.PlanoMeio)plano, r);
        }
        t.inicio = -1;
        if (plano == planoBola) planoBola = null;
        if (plano == planoMeio) planoMeio = null;
    }

    /** As tentativas em andamento que passaram da janela: fecha. */
    private void acompanharPlanos(CombatEngine engine, CombatFighter me, CombatFighter target) {
        if (planoBola != null && engine.frame() - tentativaBola.inicio >= JANELA_PLANO) {
            fecharTentativa(planoBola, tentativaBola, me, target);
        }
        if (planoMeio != null && engine.frame() - tentativaMeio.inicio >= JANELA_PLANO) {
            fecharTentativa(planoMeio, tentativaMeio, me, target);
        }
    }

    /**
     * Corrida de energia: parada até a bola ficar a {@link #CORRIDA_LEAD} quadros (correndo), então
     * corre e aperta M+H no primeiro quadro da corrida (o quarto de barra): passa através da bola
     * e segue correndo até acabar. Verdadeiro quando o quadro é da corrida.
     */
    private boolean correrContraBola(CombatEngine engine, CombatFighter me, CpuPerception.Snapshot seen,
                                     boolean blockstun, FighterInput out) {
        int frente = me.facing > 0 ? 1 : 5;
        if (blockstun || !me.grounded || me.status != CombatFighter.Status.NEUTRAL) {
            corridaSerial = -1;
            return false;
        }
        if (me.energyRunFrames > 0) {
            // Chegou correndo: emenda a pressão (o 2L que abre o combo), sem parar colado.
            if (seen.projectileGap == Float.MAX_VALUE && pressionar(me, engine.fighter(1 - me.index), out)) {
                corridaSerial = -1;
                return true;
            }
            out.direction = frente;
            out.dash = true;
            dashing = movingForward = true;
            lastReason = "corrida de energia";
            return true;
        }
        if (corridaArmada > 0) {
            // O dash saiu: no primeiro quadro correndo, M+H vira a corrida de energia. Já correu
            // e acabou (ou não saiu em 4 quadros): o plano acabou.
            if (me.forwardDashing && corridaArmada == 1) {
                out.direction = frente;
                out.dash = true;
                out.pushblock = true;
                corridaArmada = 2;
                lastReason = "corrida de energia";
                return true;
            }
            if (corridaArmada >= 2 || ++corridaArmada > 5) {
                corridaSerial = -1;
                return false;
            }
            out.direction = frente;
            out.dash = true;
            return true;
        }
        if (seen.projectileGap == Float.MAX_VALUE || seen.projectileSerial != corridaSerial ||
            me.state.superMeter < engine.config.energyRunCost) {
            corridaSerial = -1;   // a bola sumiu ou a barra não dá mais
            return false;
        }
        float speed = Math.max(1f, seen.projectileSpeed * CombatConfig.DT);
        float corre = engine.dashSpeed(me) * engine.config.energyRunSpeedPermille / 1000f * CombatConfig.DT;
        float lead = seen.projectileGapNow(me.x, engine.frame() - seen.frame) / (speed + corre);
        if (lead <= CORRIDA_LEAD) {
            out.direction = frente;
            out.dash = true;
            corridaArmada = 1;
            lastReason = "corrida de energia";
            return true;
        }
        movingForward = movingBack = dashing = false;
        out.direction = 0;
        return true;
    }

    /** O plano da bola que está valendo (para o depurador e as baterias). */
    CpuLeitura.PlanoBola planoBola() { return planoBola; }

    /** Longe disso, contra quem solta bola, ela avança entre uma bola e outra. */
    static final float AVANCA_CONTRA_BOLA = 320f;
    /**
     * A meia distância de quem solta bola: daqui para dentro a bola dele (de perto) chega antes da
     * reação, então ela decide antes da bola o que fazer ({@link CpuLeitura.PlanoMeio}).
     */
    static final float MEIO_PERTO = 200f, MEIO_LONGE = 480f;
    /** Quadros que um plano da meia distância dura antes de ela decidir de novo. */
    static final int PLANO_MEIO_QUADROS = 45;

    private CpuLeitura.PlanoMeio planoMeio;
    private boolean esperaBaixo;

    /** Ele tem entrado nela com golpe de chão (rasteira) ou de pulo: vale escolher um plano contra. */
    private boolean rivalCutucador() {
        // Os níveis baixos não mudam o jeito (do 4 para cima, mais cedo quanto mais alto).
        if (level.number < 4) return false;
        double limiar = CUTUCADOR * (1.5 - smart());
        return leitura.cutucadas() >= limiar || leitura.pulosNela() >= limiar;
    }

    /** Golpes de chão (ou pulos) dele que entraram, com o esquecimento, para ela mudar o jeito. */
    static final double CUTUCADOR = 4.0;
    private CpuLeitura.Faixa faixaMeio;
    private final Tentativa tentativaMeio = new Tentativa();
    private int meioQuadros;

    /**
     * Contra quem fica soltando bola, entre uma bola e outra: de longe ela avança (corre sem bola na
     * tela, anda com bola); na meia distância, sem bola, escolhe um plano pelo gosto do estilo e
     * pelo que rendeu contra este jogador (andar, esperar defendendo, pular antes, correr com a
     * barra através da próxima). A bola na tela é do plano da bola ({@link #escolherPlanoBola}).
     */
    private boolean avancarContraBola(CombatEngine engine, CombatFighter me, CombatFighter target,
                                      CpuPerception.Snapshot seen, FighterInput out) {
        boolean zoneia = rivalZoning(seen.frame);
        // Quem vive entrando de rasteira ou de pulo nela (pedido do jogador, 02/11): os mesmos
        // planos da meia distância, aprendendo qual rende contra ele (esperar defendendo baixo,
        // pular a rasteira, pressionar antes...).
        boolean cutuca = !zoneia && rivalCutucador();
        if (!(zoneia || cutuca) || !me.grounded || !me.canAct() || guardPlanSerial >= 0 || jumpPlanSerial >= 0 ||
            corridaSerial >= 0 || devolverBola) {
            meioQuadros = 0;
            return false;
        }
        float gap = Math.abs(seen.x - me.x);
        CpuStyle style = CpuStyle.of(me.character());
        if (cutuca && gap > MEIO_LONGE) {
            meioQuadros = 0;
            return false;
        }
        if (gap > MEIO_LONGE) {
            meioQuadros = 0;
            // Quem tem bola que alcança devolve (a decisão do neutro cuida).
            if (style.zoning >= 1.5 && OpponentAi.energyInRange(me.character(), gap, target.body()) &&
                !engine.energyProjectileAlive(me.index)) return false;
            movingForward = true;
            movingBack = false;
            // Corre só sem bola dele na tela (correndo ela não defende); com bola, anda (andando
            // ela ainda defende quando a bola chega).
            dashing = gap > 550f && seen.projectileGap == Float.MAX_VALUE;
            out.direction = me.facing > 0 ? 1 : 5;
            out.dash = dashing;
            lastReason = "avança contra a bola";
            return true;
        }
        if (seen.projectileGap != Float.MAX_VALUE || !seen.grounded || gap < CombatRules.MIN_MELEE_DISTANCE) {
            meioQuadros = 0;
            return false;
        }
        CpuLeitura.Faixa faixa = gap < MEIO_PERTO ? CpuLeitura.Faixa.PERTO : CpuLeitura.Faixa.MEIO;
        if (faixa != faixaMeio) meioQuadros = 0;
        if (meioQuadros <= 0 || planoMeio == null) {
            boolean[] pode = new boolean[CpuLeitura.PlanoMeio.values().length];
            double[] logit = new double[pode.length];
            double s = smart();
            for (CpuLeitura.PlanoMeio p : CpuLeitura.PlanoMeio.values()) {
                pode[p.ordinal()] = p != CpuLeitura.PlanoMeio.CORRE || me.state.superMeter >= engine.config.energyRunCost;
                // Pressionar e agarrar são de perto.
                if (p == CpuLeitura.PlanoMeio.PRESSIONA || p == CpuLeitura.PlanoMeio.AGARRA) pode[p.ordinal()] = faixa == CpuLeitura.Faixa.PERTO;
                double prior;
                switch (p) {
                    case ANDA: prior = 0.2 + 0.5 * (style.aggression - 0.7); break;
                    case ESPERA: prior = 0.2 + 0.5 * (style.patience - 0.8); break;
                    case PULA: prior = -0.7 + 0.7 * style.jumping + 0.5 * level.jumpProjectile; break;
                    case PRESSIONA: prior = 0.3 + 0.5 * (style.aggression - 0.7); break;
                    case AGARRA: prior = -0.4 + 0.3 * (style.throwing - 1.0); break;
                    default: prior = -1.0 + (style.aggression - 0.7) + 0.6 * s; break;
                }
                logit[p.ordinal()] = prior + aprendido(leitura.valor(faixa, p), leitura.vezes(faixa, p));
            }
            int i = sortear(logit, pode);
            fecharTentativa(planoMeio, tentativaMeio, me, target);
            planoMeio = i < 0 ? CpuLeitura.PlanoMeio.ANDA : CpuLeitura.PlanoMeio.values()[i];
            faixaMeio = faixa;
            tentativaMeio.comeca(engine, me, target);
            meioQuadros = PLANO_MEIO_QUADROS;
            // Esperando contra quem dá rasteira: agachada (a altura que ela aprendeu).
            esperaBaixo = cutuca && roll(habitLow());
        }
        meioQuadros--;
        int frente = me.facing > 0 ? 1 : 5, tras = me.facing > 0 ? 5 : 1;
        movingBack = dashing = false;
        movingForward = false;
        switch (planoMeio) {
            case ESPERA:
                out.direction = esperaBaixo ? (me.facing > 0 ? 4 : 2) : tras;
                lastReason = cutuca ? "espera o golpe dele defendendo" : "espera a bola defendendo";
                return true;
            case PULA:
                meioQuadros = 0;
                out.jump = true;
                out.direction = me.facing > 0 ? 8 : 6;
                jumpHold = JUMP_HOLD_FRAMES;
                jumpHoldDirection = out.direction;
                jumpIn = true;
                guardFrames = 0;
                lastReason = cutuca ? "pula na meia distância" : "pula antes da bola";
                return true;
            case PRESSIONA:
                // Colado em quem zoneia: o 2L (rápido e baixo) que abre o combo; fora do alcance, anda.
                if (pressionar(me, target, out)) meioQuadros = 0;
                return true;
            case AGARRA:
                agarrar(engine, me, target, out, "contra quem zoneia: ");
                if (out.grab) meioQuadros = 0;
                return true;
            case CORRE:
                meioQuadros = 0;
                corridaSerial = Integer.MAX_VALUE;   // sem bola: a corrida vai até acabar
                corridaArmada = 1;
                out.direction = frente;
                out.dash = true;
                lastReason = "corrida de energia";
                return true;
            default:
                // Colado, "seguir" é deixar o neutro decidir (golpe, agarrão, a bola dela).
                if (faixa == CpuLeitura.Faixa.PERTO) return false;
                movingForward = true;
                out.direction = frente;
                lastReason = "avança contra a bola";
                return true;
        }
    }

    /**
     * Pressão de perto: o 2L (ou o L) que alcança, começando a rota de combo (o combo segue se
     * acertar); fora do alcance, anda até ele. Verdadeiro quando apertou.
     */
    private boolean pressionar(CombatFighter me, CombatFighter target, FighterInput out) {
        CharacterDefinition c = me.character();
        float gap = Math.abs(target.x - me.x);
        for (String b : new String[] {"2L", "L"}) {
            if (c.move(b, false) != null && CpuReach.reaches(c, b, false, gap, target.body(), target.crouchingBody())) {
                startRoute(c, b);
                press(b, me, out);
                lastReason = "pressiona (" + b + ")";
                return true;
            }
        }
        movingForward = true;
        out.direction = me.facing > 0 ? 1 : 5;
        lastReason = "chega para pressionar";
        return false;
    }

    /** A tentativa de chegar que ela acabou de soltar no neutro. */
    private void marcarAbordagem(CombatEngine engine, FighterInput out, float distance) {
        if (abordagem != null) return;
        CpuLeitura.Abordagem a = null;
        if (out.special != null && isEnergyStrength(out.special)) a = CpuLeitura.Abordagem.BOLA;
        else if (out.jump && distance > 120f) a = CpuLeitura.Abordagem.PULA;
        else if (out.dash && distance > 160f) a = CpuLeitura.Abordagem.CORRE;
        else if (attack != null && distance > 150f) a = CpuLeitura.Abordagem.CUTUCA;
        if (a == null) return;
        abordagem = a;
        abordagemInicio = engine.frame();
    }

    private static boolean isEnergyStrength(String s) {
        return "L".equals(s) || "M".equals(s) || "H".equals(s);
    }

    /** O fator da abordagem para a decisão do neutro: 1 sem leitura, puxado pelo que funcionou. */
    private double fatorAbordagem(CpuLeitura.Abordagem a) {
        return 1.0 + (leitura.fator(a) - 1.0) * pesoLeitura();
    }

    /**
     * A sequência dela acabou de ser defendida: se ela já leu o que ele faz nessa hora, responde
     * com o jeito do lutador dela. Pulou: espera com o antiaéreo pronto. Aperta botão: defende e
     * pune. Fica defendendo: agarra (o zoner volta para a bola). Recua: vai atrás (o zoner
     * solta a bola).
     */
    private boolean lerDepoisDeDefender(CombatEngine engine, CombatFighter me, CombatFighter target,
                                        CpuPerception.Snapshot seen, FighterInput out) {
        CpuLeitura.Situacao st = CpuLeitura.Situacao.DEPOIS_DE_DEFENDER;
        CpuLeitura.Resposta r = leitura.previsto(st);
        if (r == null || !roll(agirPelaLeitura(st))) return false;
        return responder(engine, me, target, seen, r, out, "leu: depois de defender, ");
    }

    /** Ele está caído: na hora em que levanta, responde ao que ele costuma fazer ao levantar. */
    private boolean lerLevantar(CombatEngine engine, CombatFighter me, CombatFighter target,
                                CpuPerception.Snapshot seen, FighterInput out) {
        boolean caido = target.status == CombatFighter.Status.KNOCKDOWN || target.status == CombatFighter.Status.WAKEUP;
        if (!caido && target.status != CombatFighter.Status.NEUTRAL) {
            // Levantou e já saiu fazendo algo (ou apanhou): a leitura desta queda acabou.
            levantarLido = null;
            levantarDecidido = false;
            return false;
        }
        if (caido) {
            if (!levantarDecidido) {
                levantarDecidido = true;
                CpuLeitura.Situacao st = CpuLeitura.Situacao.AO_LEVANTAR;
                CpuLeitura.Resposta r = leitura.previsto(st);
                levantarLido = r != null && roll(agirPelaLeitura(st)) ? r : null;
            }
            if (levantarLido == null) return false;
            float gap = Math.abs(target.x - me.x);
            CpuStyle style = CpuStyle.of(me.character());
            boolean zoner = style.zoning >= 1.5;
            switch (levantarLido) {
                case ESPERA:
                    // Vai colar para agarrar quando ele levantar (o zoner fica na distância da bola).
                    if (leitura.certeza(CpuLeitura.Situacao.AO_LEVANTAR) < CERTEZA_ARRISCADA) return false;
                    if (!zoner && gap > throwReach(engine, me, target) - 10f) {
                        out.direction = me.facing > 0 ? 1 : 5;
                        lastReason = "leu: levantar, chega para agarrar";
                        return true;
                    }
                    return false;
                case ATACA:
                    // Ele aperta botão ao levantar: espera defendendo e pune.
                    guardFrames = Math.max(guardFrames, 20);
                    guardLow = roll(habitLow());
                    guardHabit = false;
                    lastReason = "leu: levantar, espera o botão";
                    return holdGuard(me, out);
                case PULA:
                    antiAereoLido = 45;
                    return false;
                default:
                    return false;
            }
        }
        // Acabou de levantar (o tempo do levantar ela sabe, como quem decorou).
        if (levantarLido == null) {
            levantarDecidido = false;
            return false;
        }
        CpuLeitura.Resposta r = levantarLido;
        levantarLido = null;
        levantarDecidido = false;
        boolean arriscada = r == CpuLeitura.Resposta.ESPERA || r == CpuLeitura.Resposta.RECUA;
        if (arriscada && leitura.certeza(CpuLeitura.Situacao.AO_LEVANTAR) < CERTEZA_ARRISCADA) return false;
        if (arriscada || r == CpuLeitura.Resposta.PULA) {
            return responder(engine, me, target, seen, r, out, "leu: levantar, ");
        }
        return false;
    }

    /**
     * A resposta ao hábito lido, com as ferramentas e o jeito do lutador dela: o zoner responde
     * de longe, o agarrador agarra, quem pressiona vai atrás.
     */
    private boolean responder(CombatEngine engine, CombatFighter me, CombatFighter target, CpuPerception.Snapshot seen,
                              CpuLeitura.Resposta r, FighterInput out, String porque) {
        CpuStyle style = CpuStyle.of(me.character());
        boolean zoner = style.zoning >= 1.5;
        float gap = Math.abs(target.x - me.x);
        float alcanceAgarrao = throwReach(engine, me, target);
        switch (r) {
            case PULA:
                // Leu que ele vai pular: segue jogando, com o antiaéreo de reação garantido (o 2H
                // adiantado, solto antes de ver o pulo, saía no vazio mais do que acertava).
                antiAereoLido = 40;
                return false;
            case ATACA:
            case AVANCA:
                guardFrames = Math.max(guardFrames, 18);
                guardLow = roll(habitLow());
                guardHabit = false;
                lastReason = porque + "defende e pune";
                return holdGuard(me, out);
            case ESPERA:
                if (zoner && gap > 200f && OpponentAi.energyInRange(me.character(), gap, target.body()) &&
                    !engine.energyProjectileAlive(me.index)) {
                    out.special = "M";
                    lastReason = porque + "bola em quem defende";
                    return true;
                }
                // Quem só defende: chega colado e agarra (o plano dura até ele atacar ou acabar).
                agarrarLido = PLANO_AGARRAR;
                return agarrar(engine, me, target, out, porque);
            case RECUA:
                if (zoner && OpponentAi.energyInRange(me.character(), gap + 120f, target.body()) &&
                    !engine.energyProjectileAlive(me.index)) {
                    out.special = "M";
                    lastReason = porque + "bola em quem recua";
                    return true;
                }
                out.direction = me.facing > 0 ? 1 : 5;
                out.dash = true;
                lastReason = porque + "vai atrás";
                return true;
            default:
                return false;
        }
    }

    private void resetKit() {
        kitTimer = 0;
        plannedAttack = null;
        assistPlan = dhcPlan = shinkaiPlan = false;
        wasBlocking = guardCancelPlan = pushblockPlan = false;
        wasAssistOut = convertPlan = false;
    }

    boolean enabled() {
        return enabled;
    }

    void setEnabled(boolean value) {
        enabled = value;
        ai.reset();
        perception.reset();
        movingForward = movingBack = dashing = false;
        cooldown = 0;
        lastAttack = null;
        clearRequests();
        resetKit();
        endRoute();
        guardFrames = 0;
        justDefenseWait = 0;
        breakerWait = 0;
        breakerDecided = false;
        breakerSession = null;
        breakerCombo = false;
        anticipationTimer = 0;
        lastGuardSerial = lastJumpSerial = lastProjectileSerial = -1;
        jumpPlanSerial = guardPlanSerial = guardingSerial = -1;
        corridaSerial = -1;
        corridaArmada = 0;
        devolverBola = forcarPerfeita = false;
        planoBola = null;
        planoMeio = null;
        tentativaBola.inicio = tentativaMeio.inicio = -1;
        meioQuadros = 0;
        jumpHold = 0;
        jumpIn = false;
        rivalShotCount = 0;
        projectileStunGuard = false;
        lastOpponentAttackSerial = -1;
        lastOpponentAttackFrame = Integer.MIN_VALUE / 2;
        lastPunishSerial = lastAntiAirSerial = -1;
        lastBreakSerial = -1;
        breakPlan = false;
        contactFrame = Integer.MIN_VALUE;
        contactPush = 0f;
        antiAirPlan = false;
        ownHitFrame = -1;
        specialMeleeSerial = specialShotSerial = -1;
        meleePlan = shotPlan = null;
        diveDecided = divePlan = false;
    }

    /** Fills the CPU input for the next engine step. */
    void fill(CombatEngine engine, int self, FighterInput out) {
        out.clear();
        habitGuardFrame = false;
        if (!enabled) return;
        perception.record(engine, self);
        chargeConfig = engine.config;
        CombatFighter me = engine.fighter(self);
        CombatFighter target = engine.fighter(1 - self);
        // PROTÓTIPO PERCEPCAO: no SHINKAI do Ryūga ela vê o golpe vindo antes (a percepção sobre-humana).
        reacao = percepcaoReage && me.state.inShinkai(Shinkai.Type.PERCEPCAO)
            ? Math.max(PERCEPCAO_REACAO_MIN, level.reactionFrames * 6 / 10) : Math.round(level.reactionFrames * escalaReacao);
        CpuPerception.Snapshot seen = perception.seen(reacao);
        seenNow = seen;
        observar(engine, self, me, seen);
        acompanharPlanos(engine, me, target);
        if (antiAereoLido > 0) antiAereoLido--;
        boolean overhead = seen != null && !seen.grounded && Math.abs(seen.x - me.x) < CROSS_WATCH;
        if (overhead && !rivalOverhead) crossRead = crossRandom.nextDouble() < level.reactGuard * CROSS_READ;
        rivalOverhead = overhead;
        ShinkaiClash clash = engine.shinkaiClash();
        if (clash != null) {
            pushInClash(clash, self, out);
            return;
        }
        clashHold = 0;
        lowGuard = guardLow(engine, me);
        feelContact(engine, self, me, target);
        if (me.status == CombatFighter.Status.THROWN) {
            if (techDelay == -2) techDelay = ai.techDelay(expectingThrow(engine, me, target, seen) ? level.techGuess : 0);
            if (techDelay >= 0 && techDelay-- == 0) out.grab = true;
            return;
        }
        techDelay = -2;
        if (me.rushSeq > 0) {
            sequenciaDoRush(me, out);
            return;
        }
        // O próprio combo breaker ela sente na hora: o troco vem garantido.
        if (me.comboBroke && engine.cues().contains("COMBO_BREAKER")) breakerCombo = true;
        for (CombatEngine.HitEvent ev : engine.events()) {
            // O próprio atordoamento ela sente na hora (não é reação ao rival).
            if (ev.defender == self && ev.projectile) projectileStunGuard = true;
        }
        if (me.frozen()) return;
        if (me.inHitstun()) betOnComboBreaker(engine, me, seen, out);
        else breakerWait = 0;
        if (me.status == CombatFighter.Status.AIR_HITSTUN) {
            // Air tech once the hitstun is over, sometimes, toward a random side.
            if (airTechPlan == -2) airTechPlan = ai.airTechDirection(level.airTech);
            if (airTechPlan >= 0 && me.stunLeft == 0) {
                out.direction = MotionParser.relative(airTechPlan, me.facing);
                out.light = true;
            }
            endRoute();
            return;
        }
        airTechPlan = -2;
        if (me.status == CombatFighter.Status.KNOCKDOWN) {
            if (wakeupPlan == -2) wakeupPlan = ai.wakeupDirection();
            if (wakeupPlan > 0) out.direction = MotionParser.relative(wakeupPlan, me.facing);
            endRoute();
            return;
        }
        wakeupPlan = -2;

        // Ele estava no ar por conta própria (pulando, não jogado por golpe) no quadro de antes.
        boolean alvoPulava = alvoNoAr;
        alvoNoAr = !target.grounded && (target.status == CombatFighter.Status.NEUTRAL || target.status == CombatFighter.Status.ATTACK);
        if (me.attacking()) {
            // Qualquer golpe de chão dela que pega ele pulando conta como antiaéreo (não só o 2H
            // de reação: a cutucada que acerta quem pula também derruba).
            if (me.outcome == CombatFighter.Outcome.HIT && ownHitFrame < 0 && me.grounded && alvoPulava) marcarAntiAereo(engine);
            if (me.outcome == CombatFighter.Outcome.HIT && engine.frame() - aaQuadro < 45 && !aaAcertou) {
                aaAcertou = true;
                aaJuggleFeito = aaJuggleDecidido = false;
                okiDoAntiAereo = true;
            }
            if (me.outcome == CombatFighter.Outcome.HIT && ownHitFrame < 0) ownHitFrame = engine.frame();
            if (!me.grounded && me.outcome == CombatFighter.Outcome.HIT) pousoEmenda = true;
            if (me.outcome == CombatFighter.Outcome.BLOCK && me.grounded) sequenciaDefendida = true;
            if (me.outcome == CombatFighter.Outcome.HIT) sequenciaDefendida = false;
        } else if (route == null) {
            ownHitFrame = -1;
        }

        if (breakerFollowUp(me, target, out)) return;
        if (leituraFollowUp(me, target, out)) return;
        if (fullKit && !target.ko() && fillKit(engine, me, target, seen, out)) return;

        if (cooldown > 0) cooldown--;
        // Com pressa o respiro entre golpes encurta (até o dobro de rápido).
        hurry += urgency;
        if (hurry >= 1f) {
            hurry -= 1f;
            if (cooldown > 0) cooldown--;
        }
        if (me.framesSinceHit == 0) {
            cooldown = Math.max(cooldown, HIT_COOLDOWN);
            endRoute();
        }
        if (fullKit && ultraLink(engine, me, target, out)) return;
        if (me.attacking()) {
            lastAttack = me.attack;
            runCombo(engine, me, out);
            return;
        }
        if (route != null && runComboFromNeutral(engine, me, target, out)) return;
        if (emendaDoPulo(me, target, out)) return;
        if (seguirAntiAereo(engine, me, target, out)) return;
        if (lastAttack != null) {
            cooldown = Math.max(cooldown,
                lastAttack.kind == AttackDefinition.Kind.SUPER ? SUPER_COOLDOWN
                    : lastAttack.kind == AttackDefinition.Kind.PROJECTILE ? ENERGY_COOLDOWN
                    : Math.round(ATTACK_COOLDOWN * (1f - 0.5f * (float)pressao())));
            lastAttack = null;
        }
        if (target.ko()) {
            movingForward = movingBack = dashing = false;
            return;
        }
        if (saidaDefendendo(me, out)) return;
        boolean wasBlocking = blockedLastFrame;
        blockedLastFrame = me.status == CombatFighter.Status.BLOCKSTUN;
        if (wasBlocking && !blockedLastFrame && punishOnBlock(engine, me, target, seen, out)) return;
        // Defendendo por reação e o golpe dele errou (já está na recuperação): solta a defesa e
        // pune, em vez de segurar a defesa até o fim da janela.
        if (seen != null && !seen.ko && me.canAct() && punish(engine, me, target, seen, out)) return;
        if (seen != null && !seen.ko && specialReaction(engine, me, target, seen, out)) return;
        if (seen != null && !seen.ko && me.canAct() && antiRush(engine, me, target, seen, out)) return;
        if (defend(engine, me, target, seen, out)) return;
        if (!me.canAct()) {
            movingForward = movingBack = dashing = false;
            return;
        }
        if (seen == null || seen.ko) return;   // ainda não viu nada: espera
        if (punishGuardBreak(me, target, seen, out)) return;
        if (punish(engine, me, target, seen, out)) return;
        if (antiAir(engine, me, target, seen, out)) return;
        if (sequenciaDefendida && me.grounded && route == null) {
            sequenciaDefendida = false;
            if (lerDepoisDeDefender(engine, me, target, seen, out)) return;
            if (seguirEmCima(engine, me, target, seen, out)) return;
        }
        if (me.grounded && lerLevantar(engine, me, target, seen, out)) return;
        if (okiPressao(engine, me, target, out)) return;
        if (agarrarLido > 0) {
            agarrarLido--;
            // Ele atacou (ou pulou): o plano acabou, a reação de defesa e punição cuida.
            if (seen.attacking() || !seen.grounded || target.status != CombatFighter.Status.NEUTRAL &&
                target.status != CombatFighter.Status.BLOCKSTUN) agarrarLido = 0;
            else if (me.grounded && agarrar(engine, me, target, out, "leu: só defende, ")) return;
        }
        if (esperaLida > 0) {
            // Leu que ele vai pular (ou apertar): fica parada, pronta para tirar do ar.
            esperaLida--;
            movingForward = movingBack = dashing = false;
            return;
        }
        if (habitGuard(me, target, seen, out)) return;
        if (avancarContraBola(engine, me, target, seen, out)) return;
        if (guardPlanSerial >= 0 && guardingSerial != guardPlanSerial &&
            guardPlanArrival < PROJECTILE_BUSY_FRAMES && me.grounded) {
            // A bola está chegando e ela vai defender: golpe agora não acaba a tempo. Anda até ela.
            movingBack = dashing = false;
            movingForward = Math.abs(seen.x - me.x) > 160f;
            out.direction = movingForward ? (me.facing > 0 ? 1 : 5) : 0;
            return;
        }

        situation.self = me.character();
        situation.target = target.body();
        situation.distance = Math.abs(predictedX(engine, seen) - me.x);
        situation.verticalDistance = Math.abs(seen.y - me.y);
        situation.selfAirborne = !me.grounded;
        situation.targetGrounded = seen.grounded;
        situation.targetCrouching = seen.crouching;
        situation.targetAlive = !seen.ko;
        situation.superReady = me.state.canPaySuper();
        situation.projectileActive = engine.energyProjectileAlive(self);
        // Caído no chão não leva golpe: bater nele é golpe perdido (e de onde ela o viu cair, que
        // já não é onde ele está). Ela chega perto e espera ele levantar.
        situation.attackReady = cooldown <= 0 && seen.status != CombatFighter.Status.KNOCKDOWN;
        situation.throwReach = throwReach(engine, me, target);
        situation.level = level;
        if (seen.status == CombatFighter.Status.BLOCKSTUN) lastRivalBlockFrame = seen.frame;
        situation.rivalBlocked = seen.frame - lastRivalBlockFrame < RIVAL_BLOCK_MEMORY;
        situation.style = CpuStyle.of(me.character());
        situation.urgency = urgency;
        situation.pressao = pressao();
        situation.rivalZoning = rivalZoning(seen.frame);
        situation.fatorCorre = fatorAbordagem(CpuLeitura.Abordagem.CORRE);
        situation.fatorPulo = fatorAbordagem(CpuLeitura.Abordagem.PULA);
        situation.fatorCutuca = fatorAbordagem(CpuLeitura.Abordagem.CUTUCA);
        situation.fatorBola = fatorAbordagem(CpuLeitura.Abordagem.BOLA) * fatorBolaLida();
        clearRequests();
        if (ai.think(CombatConfig.DT, situation, actions)) {
            walkInPlan = !roll(level.reactGuard * EDGE_STOP * (1.0 - pressao()));
            specialNeutral(me, target, seen);
        }
        if (movingForward && !dashing && attack == null && special == null && !superAttack && !grab && !jump &&
            me.grounded && seen.grounded && seen.status == CombatFighter.Status.NEUTRAL && !seen.attacking() &&
            situation.distance < jabRange(me, target) + EDGE_MARGIN && !walkInPlan) {
            // Na beira do golpe rápido do rival: andando para dentro ela levava o jab (a
            // distância que ela vê é a de antes da reação, e os dois estão andando). Para ali
            // e espera defendendo; entra pelo dash, pelo pulo, pela punição ou pelo golpe dela.
            movingForward = false;
            if (roll(level.reactGuard * 0.25 * (1.0 - pressao()))) {
                guardFrames = 8 + reactRandom.nextInt(8);
                guardLow = true;
                guardHabit = true;
                holdGuard(me, out);
                return;
            }
        }

        int forward = me.facing > 0 ? 1 : 5;
        if (movingForward) out.direction = forward;
        else if (movingBack) out.direction = me.facing > 0 ? 5 : 1;
        out.dash = dashing && movingForward;
        out.backdash = backdash;
        out.jump = jump && !superJump;
        out.superJump = jump && superJump;
        out.superAttack = superAttack;
        out.grab = grab;
        if (special != null) out.special = special;
        // O dedo já está no botão: com o rival de fato fora do alcance (a distância que ela viu
        // é a de antes da reação, e nesse meio tempo ele foi empurrado, voou de um golpe ou
        // trocou de lado por baixo dela), ela não solta o golpe no vazio.
        if (attack != null && !CpuReach.reaches(me.character(), attack, !me.grounded,
            Math.abs(target.x - me.x) + PRESS_REACH_MARGIN, target.body(), target.crouchingBody())) {
            attack = null;
        }
        marcarAbordagem(engine, out, situation.distance);
        if (out.special != null && isEnergyStrength(out.special) &&
            leitura.previsto(CpuLeitura.Situacao.CONTRA_BOLA) == CpuLeitura.Resposta.PULA &&
            roll(agirPelaLeitura(CpuLeitura.Situacao.CONTRA_BOLA))) {
            // Ele costuma pular a bola dela: solta e já fica com o antiaéreo pronto.
            antiAereoLido = 50;
        }
        if (attack != null) {
            startRoute(me.character(), attack);
            press(attack, me, out);
            lastReason = "neutro";
        }
    }

    /**
     * A bola contra o que ele costuma fazer com ela: quem pula por cima ganha menos bola (e ela
     * fica pronta para o antiaéreo); quem fica defendendo ganha mais; quem anda por baixo, menos.
     */
    private double fatorBolaLida() {
        CpuLeitura.Situacao st = CpuLeitura.Situacao.CONTRA_BOLA;
        CpuLeitura.Resposta r = leitura.previsto(st);
        if (r == null) return 1.0;
        double c = agirPelaLeitura(st);
        switch (r) {
            case PULA: return 1.0 - 0.45 * c;
            case AVANCA: return 1.0 - 0.3 * c;
            case ESPERA: return 1.0 + 0.4 * c;
            default: return 1.0;
        }
    }

    /** Folga da conferência na hora de apertar: o quadro que o golpe leva para começar, andando. */
    static final float PRESS_REACH_MARGIN = 4f;

    // ---------------------------------------------------------------- defesa

    /**
     * Defesa: reage ao que viu (golpe, pulo, projétil) com a chance e a altura do nível, segura
     * a defesa enquanto apanha nela e, colada no rival, às vezes espera defendendo agachada
     * (hábito, sem reagir a nada). Verdadeiro quando o quadro é da defesa.
     */
    private boolean defend(CombatEngine engine, CombatFighter me, CombatFighter target,
                           CpuPerception.Snapshot seen, FighterInput out) {
        boolean blockstun = me.status == CombatFighter.Status.BLOCKSTUN;
        float ultimoX = arArUltimoX;
        arArUltimoX = me.grounded ? Float.NaN : me.x;
        // Um sorteio por pulo (o primeiro quadro no ar): se vai disputar o ar neste pulo.
        if (me.grounded) arArSorteado = false;
        else if (!arArSorteado) {
            arArSorteado = true;
            arArPlano = roll(level.antiAir);
        }
        if (jumpIn) {
            String arAr = !me.grounded && me.canAct() ? golpeArAr(engine, me, seen, ultimoX) : null;
            if (me.grounded && jumpHold == 0 || !me.canAct() && !me.attacking()) {
                jumpIn = false;
            } else if (arAr != null) {
                // Ele também está no ar e ao alcance: o golpe que sai primeiro e chega (o jM dele,
                // rápido e longo, ganhava do jH dela, lento, curto e só descendo: registro real).
                jumpIn = false;
                crossJump = false;
                press(arAr, me, out);
                lastReason = "ar contra ar";
                return true;
            } else if (!me.grounded && me.canAct() && me.vy >= 0f && seen != null &&
                Math.abs(seen.x - me.x) <= JUMP_IN_REACH) {
                // Descendo: solta o aéreo agora (o mais forte que ela tiver). Passando por cima do
                // rival (o pulo para cruzar, ou caindo em cima dele): o jM, que pega por trás.
                jumpIn = false;
                boolean crossing = crossJump || (target.x - me.x) * me.facing < 0f;
                crossJump = false;
                if (crossing && me.character().move("M", true) != null) {
                    press("M", me, out);
                    lastReason = "crossup";
                    return true;
                }
                for (String b : new String[] {"H", "M", "L"}) {
                    if (me.character().move(b, true) != null) {
                        press(b, me, out);
                        lastReason = "pulo para dentro";
                        return true;
                    }
                }
            }
        }
        if (jumpHold > 0) {
            jumpHold--;
            if (!blockstun && me.status != CombatFighter.Status.HITSTUN &&
                me.status != CombatFighter.Status.AIR_HITSTUN) {
                out.direction = jumpHoldDirection;
                return true;
            }
            jumpHold = 0;
        }
        if (!blockstun && !me.canAct()) {
            guardFrames = 0;
            justDefenseWait = 0;
            return false;
        }
        CpuStyle style = CpuStyle.of(me.character());
        if (projectileStunGuard && !blockstun && me.canAct()) {
            projectileStunGuard = false;
            // Saindo do atordoamento da bola: a próxima pode já estar saindo, perto demais para
            // defender reagindo. Segura trás um instante, como quem joga faz.
            if (me.grounded && roll(level.reactGuard * (0.5 + 0.5 * smart()))) {
                guardHabit = false;
                guardLow = false;
                guardFrames = Math.max(guardFrames, 14 + reactRandom.nextInt(10));
            }
        }
        if (seen != null && !seen.ko) {
            float gap = Math.abs(seen.x - me.x);
            // (a defesa por hábito fica em habitGuard, depois de punir e tirar do ar)
            if (seen.threatening() && seen.attack.melee() && seen.attackSerial != lastGuardSerial &&
                gap <= seen.attack.reach + me.body().halfWidth + 60f && !(antiAirPlan && !seen.grounded && me.grounded)) {
                lastGuardSerial = seen.attackSerial;
                // O golpe alto de chão só se defende em pé.
                int required = !seen.grounded || seen.attack.overhead ? GUARD_HIGH
                    : seen.attack.low ? GUARD_LOW : GUARD_ANY;
                // Com pressa (fim do tempo) ela larga a defesa mais cedo para voltar a atacar.
                int segura = (int)Math.round((REACT_GUARD_FRAMES + REACT_GUARD_EXTRA * smart()) * (1.0 - 0.4 * urgency));
                if (decideGuard(required, segura)) {
                    altaPorPulo = !seen.grounded;
                    // Quadros até o golpe ficar ativo, contando o que passou desde o que ela viu.
                    int elapsed = engine.frame() - seen.frame;
                    planJustDefense(me, seen.attack.startupFrames - seen.attackFrame - elapsed);
                }
            } else if (!seen.grounded && seen.jumpSerial != lastJumpSerial && gap < 340f &&
                seen.status == CombatFighter.Status.NEUTRAL) {
                // Pulo vindo: primeiro decide se tira do ar (antes, a defesa de 40 quadros saía
                // primeiro e o antiaéreo quase nunca chegava a ser tentado); se não, defende em pé.
                lastJumpSerial = seen.jumpSerial;
                if (me.grounded) decidirAntiAereo(me, seen);
                if (!antiAirPlan && decideGuard(GUARD_HIGH, 40)) altaPorPulo = true;
            }
            if (seen.projectileGap != Float.MAX_VALUE && seen.projectileSerial != lastProjectileSerial) {
                float speed = Math.max(1f, seen.projectileSpeed * CombatConfig.DT);
                float now = seen.projectileGap - speed * reacao;
                if (now < 560f) {
                    lastProjectileSerial = seen.projectileSerial;
                    noteRivalShot(seen.frame);
                    float leadNow = seen.projectileGapNow(me.x, engine.frame() - seen.frame) / (speed + JUMP_FORWARD_STEP);
                    boolean zoneando = rivalZoning(seen.frame);
                    float chegaEm = seen.projectileGapNow(me.x, engine.frame() - seen.frame) / speed;
                    boolean livre = !blockstun && me.grounded && me.canAct();
                    // Os planos que dá para fazer com esta bola (defender sempre dá).
                    boolean[] pode = new boolean[CpuLeitura.PlanoBola.values().length];
                    pode[CpuLeitura.PlanoBola.DEFENDE.ordinal()] = true;
                    pode[CpuLeitura.PlanoBola.PERFEITA.ordinal()] = me.grounded && level.justDefense() > 0;
                    pode[CpuLeitura.PlanoBola.PULA.ordinal()] = livre && !seen.projectileSuper &&
                        leadNow > JUMP_OVER_LEAD - 2f && (Math.abs(seen.x - me.x) <= JUMP_OVER_RANGE || zoneando);
                    pode[CpuLeitura.PlanoBola.CORRE.ordinal()] = livre &&
                        me.state.superMeter >= engine.config.energyRunCost &&
                        chegaEm > CORRIDA_LEAD * 0.5f;
                    AttackDefinition s1 = me.character().attack("S");
                    pode[CpuLeitura.PlanoBola.DEVOLVE.ordinal()] = livre && s1 != null && !s1.melee() &&
                        me.character().fighter.energy != null && !engine.energyProjectileAlive(me.index) &&
                        chegaEm > s1.startupFrames + 3;
                    pode[CpuLeitura.PlanoBola.ESPECIAL.ordinal()] = livre && especialContraBola != null &&
                        specialShotSerial == seen.projectileSerial;
                    // Os níveis baixos muitas vezes nem reagem à bola (a chance do nível).
                    CpuLeitura.PlanoBola plano = roll(Math.max(level.reactGuard, level.jumpProjectile))
                        ? escolherPlanoBola(pode, me, zoneando) : null;
                    if (plano != null) comecarPlanoBola(plano, engine, me, target);
                    forcarPerfeita = false;
                    if (plano == CpuLeitura.PlanoBola.PULA) {
                        // Pula por cima, para frente, quando a bola chegar no tempo certo.
                        jumpPlanSerial = seen.projectileSerial;
                        jumpPlanLead = JUMP_OVER_LEAD +
                            (float)((1.0 - smart()) * JUMP_OVER_SLOP * (reactRandom.nextDouble() * 2.0 - 1.0));
                    } else if (plano == CpuLeitura.PlanoBola.CORRE) {
                        // Espera a bola chegar perto e corre através dela com a barra.
                        corridaSerial = seen.projectileSerial;
                        corridaArmada = 0;
                    } else if (plano == CpuLeitura.PlanoBola.DEVOLVE) {
                        devolverBola = true;
                    } else if (plano == CpuLeitura.PlanoBola.ESPECIAL) {
                        // O especial sai na hora certa (a resposta de especial cuida).
                        shotPlan = especialContraBola;
                    } else if (plano != null) {
                        // Defende quando ela chegar (projétil do meio: em pé, parada); na defesa
                        // perfeita ela absorve a bola e sai livre para andar.
                        forcarPerfeita = plano == CpuLeitura.PlanoBola.PERFEITA;
                        guardPlanSerial = seen.projectileSerial;
                        guardPlanLow = seen.projectileLow && roll(level.guardHeight);
                    }
                }
            }
            if (guardPlanSerial >= 0) {
                if (seen.projectileGap == Float.MAX_VALUE || seen.projectileSerial != guardPlanSerial) {
                    guardPlanSerial = guardingSerial = -1;
                } else {
                    float speed = Math.max(1f, seen.projectileSpeed * CombatConfig.DT);
                    // Ela pode estar andando para a bola: conta como se fosse (fecha mais rápido).
                    float arrival = seen.projectileGapNow(me.x, engine.frame() - seen.frame) / (speed + WALK_STEP);
                    guardPlanArrival = arrival;
                    if (arrival <= GUARD_PROJECTILE_LEAD || blockstun || guardingSerial == guardPlanSerial) {
                        if (guardingSerial != guardPlanSerial) planJustDefense(me, Math.round(Math.max(0f, arrival)));
                        // Segura enquanto essa bola existir (recuando ela demora mais a chegar).
                        guardingSerial = guardPlanSerial;
                        guardHabit = false;
                        guardLow = guardPlanLow;
                        guardFrames = Math.max(guardFrames, 6);
                    }
                }
            }
            if (jumpPlanSerial >= 0) {
                if (seen.projectileGap == Float.MAX_VALUE || seen.projectileSerial != jumpPlanSerial || blockstun) {
                    jumpPlanSerial = -1;   // a bola sumiu (bateu em algo) ou ela já está defendendo
                } else {
                    float speed = Math.max(1f, seen.projectileSpeed * CombatConfig.DT);
                    float lead = seen.projectileGapNow(me.x, engine.frame() - seen.frame) / (speed + JUMP_FORWARD_STEP);
                    if (lead <= jumpPlanLead) {
                        jumpPlanSerial = -1;
                        if (me.grounded && me.canAct()) jumpOverProjectile = true;
                    } else if (me.grounded && me.canAct()) {
                        // Esperando a hora: parada (andar para dentro da bola mudava a conta).
                        movingForward = movingBack = dashing = false;
                        out.direction = 0;
                        return true;
                    }
                }
            }
            if (devolverBola) {
                devolverBola = false;
                if (!blockstun && me.grounded && me.canAct() && !engine.energyProjectileAlive(me.index)) {
                    // A bola dela anula a dele no meio do caminho.
                    out.special = "M";
                    lastReason = "devolve a bola";
                    return true;
                }
            }
            if (corridaSerial >= 0 && correrContraBola(engine, me, seen, blockstun, out)) return true;
        }
        // O rival caído perto: às vezes pula para cair por cima dele no levantar (crossup).
        if (seen != null && seen.status != CombatFighter.Status.KNOCKDOWN) crossPlanned = false;
        if (!crossPlanned && !blockstun && me.grounded && me.canAct() && seen != null &&
            seen.status == CombatFighter.Status.KNOCKDOWN && me.character().move("M", true) != null) {
            float gap = Math.abs(seen.x - me.x);
            crossPlanned = true;
            if (gap > 95f && gap < 250f && crossRandom.nextDouble() < level.antiAir * 0.35) {
                out.jump = true;
                out.direction = me.facing > 0 ? 8 : 6;
                jumpHold = JUMP_HOLD_FRAMES;
                jumpHoldDirection = out.direction;
                jumpIn = true;
                crossJump = true;
                guardFrames = 0;
                lastReason = "pulo cruzado";
                return true;
            }
        }
        if (jumpOverProjectile && !blockstun && me.grounded) {
            jumpOverProjectile = false;
            out.jump = true;
            out.direction = me.facing > 0 ? 8 : 6;
            // A direção dos primeiros quadros decide o pulo: segura para frente (a defesa
            // segurando trás logo depois virava o pulo para trás, em cima da bola).
            jumpHold = JUMP_HOLD_FRAMES;
            jumpHoldDirection = out.direction;
            jumpIn = true;
            guardFrames = 0;
            guardPlanSerial = guardingSerial = -1;
            justDefenseWait = 0;
            return true;
        }
        jumpOverProjectile = false;
        if (blockstun) {
            guardFrames = Math.max(guardFrames, 8);
            guardHabit = false;
        }
        if (seen != null && seen.attacking() && seen.attackSerial != lastOpponentAttackSerial) {
            lastOpponentAttackSerial = seen.attackSerial;
            lastOpponentAttackFrame = seen.frame;
        }
        return !guardHabit && holdGuard(me, out);
    }

    /**
     * Hábito: colada no rival, espera defendendo agachada (segura tudo no chão), sem reagir a
     * nada. Forte quando ela acabou de ver o rival atacar (respeita a pressão); fraco contra
     * quem está parado.
     */
    private boolean habitGuard(CombatFighter me, CombatFighter target, CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded) return false;
        if (guardFrames > 0) return holdGuard(me, out);
        if (anticipationTimer > 0) {
            anticipationTimer--;
            return false;
        }
        if (Math.abs(seen.x - me.x) >= threatRange(me, target)) return false;
        // Rival deitado ou atordoado pelo golpe dela não ataca: ela não fica agachada defendendo.
        if (seen.status == CombatFighter.Status.KNOCKDOWN || seen.status == CombatFighter.Status.HITSTUN ||
            seen.status == CombatFighter.Status.AIR_HITSTUN) return false;
        if (cooldown > 0 && roll(level.reactGuard * COOLDOWN_GUARD)) {
            // Perto e ainda sem poder atacar (o respiro entre os golpes dela): parada ali ela
            // levava o jab, rápido demais para defender reagindo. Segura a defesa até poder agir.
            guardFrames = cooldown + 2;
            guardLow = seen.grounded;   // rival no ar: o pulo só se defende em pé
            guardHabit = true;
            return holdGuard(me, out);
        }
        boolean pressured = seen.frame - lastOpponentAttackFrame < PRESSURE_FRAMES;
        double p = level.guardAnticipation * CpuStyle.of(me.character()).patience;
        // Sem pressão (o rival não atacou há pouco) ela quase não fica agachada esperando: era o
        // que mais a deixava parecendo grudada no chão. Pressionada, defende (o jab não dá para
        // defender reagindo), mas em janelas curtas: a pressão acabou, ela volta a atacar.
        p = pressured ? p + PRESSURE_GUARD * (1.0 + smart()) * level.reactGuard : p * CALM_HABIT_SCALE;
        // Aprende: ele tem acertado golpes de chão nela de perto (rápidos demais para defender
        // reagindo, como a rasteira): ela passa a esperar defendendo no alcance dele.
        if (seen.grounded) p += APRENDE_GUARDA * Math.min(1.0, leitura.cutucadas() / APRENDE_CUTUCADAS) * smart();
        // Guarda quase quebrando: ficar parada defendendo é pedir o guard break.
        if (lowGuard) p *= 0.4;
        // Com pressa ela não fica esperando: quem espera deixa o tempo acabar.
        p *= 1.0 - 0.6 * urgency;
        if (roll(Math.min(0.95, p))) {
            guardFrames = HABIT_GUARD_MIN + reactRandom.nextInt(HABIT_GUARD_SPREAD);
            // Rival no ar: o pulo só se defende em pé.
            guardLow = seen.grounded && (seen.crouching || roll(habitLow()));
            guardHabit = true;
            return holdGuard(me, out);
        }
        anticipationTimer = 12;   // decidiu agir: espera um pouco antes de pensar de novo
        return false;
    }

    private boolean holdGuard(CombatFighter me, FighterInput out) {
        if (guardFrames <= 0) return false;
        guardFrames--;
        if (trocaBaixaLigada && altaPorPulo && seenNow != null && (seenNow.grounded || caindoAgora(seenNow))) {
            // Ele está caindo do pulo: o que vem agora é de chão (a rasteira ao cair). Agacha, se
            // for o hábito dela contra ele. (Pelo que ela viu, ele cai daqui a pouco: quem joga
            // vê a queda e já agacha, não espera ver ele no chão.)
            altaPorPulo = false;
            if (!guardLow && Math.abs(seenNow.x - me.x) < SAIDA_PERTO) guardLow = roll(habitLow());
        }
        habitGuardFrame = guardHabit;
        movingForward = movingBack = dashing = false;
        if (justDefenseWait > 0) {
            // Defesa perfeita: solta (parada) e aperta a defesa em cima da hora.
            justDefenseWait--;
            out.direction = 0;
            return true;
        }
        // "Trás" é o contrário do lado em que ela viu o rival: com o rival passando por cima, ela
        // vê o lado antigo por um instante (a reação) e defende errado o crossup, a não ser que
        // tenha lido o pulo.
        int side = me.facing;
        if (!crossRead && seenNow != null && Math.abs(seenNow.x - me.x) > 6f) side = seenNow.x > me.x ? 1 : -1;
        int back = side > 0 ? 5 : 1;
        int downBack = side > 0 ? 4 : 2;
        out.direction = guardLow ? downBack : back;
        return true;
    }

    /**
     * Defesa perfeita, com a chance do nível: fica parada até {@link #JUST_DEFENSE_LEAD} quadros
     * antes do contato e só então aperta a defesa. Só se dá tempo de soltar e apertar de novo.
     */
    private void planJustDefense(CombatFighter me, int framesToContact) {
        justDefenseWait = 0;
        if (!me.grounded || framesToContact <= JUST_DEFENSE_LEAD + 1) return;
        if (forcarPerfeita) {
            // O plano é absorver: tenta sempre, com o erro de tempo do nível (cedo vira defesa
            // comum; tarde, apanha).
            forcarPerfeita = false;
            int erro = (int)Math.round((1.0 - smart()) * (reactRandom.nextDouble() * 5.0 - 3.0));
            justDefenseWait = Math.max(0, framesToContact - JUST_DEFENSE_LEAD + erro);
            guardFrames = Math.max(guardFrames, justDefenseWait + 12);
            return;
        }
        double perfeita = level.justDefense();
        // PROTÓTIPO PERCEPCAO (o Ryūga no SHINKAI): ele vê o golpe vindo e a janela é mais larga.
        if (me.state.inShinkai(Shinkai.Type.PERCEPCAO)) perfeita = Math.min(0.95, perfeita + PERCEPCAO_LEITURA * (0.5 + smart()));
        if (!roll(perfeita)) return;
        justDefenseWait = framesToContact - JUST_DEFENSE_LEAD;
        guardFrames = Math.max(guardFrames, justDefenseWait + 12);
    }

    /**
     * Combo breaker: quando o combo que ela leva fica quebrável (o "!"), decide uma vez se quebra
     * e, se sim, aperta um golpe (L) depois de um tempo de reação.
     */
    private void betOnComboBreaker(CombatEngine engine, CombatFighter me, CpuPerception.Snapshot seen,
                                   FighterInput out) {
        ComboSession session = engine.session(me.index);
        if (session == null || !session.active) {
            breakerWait = 0;
            breakerDecided = false;
            return;
        }
        // Uma decisão por combo: o combo seguinte começa sem quadro livre no meio (a decisão do
        // anterior ficava valendo e ela quase nunca decidia de novo).
        if (session != breakerSession) {
            breakerSession = session;
            breakerWait = 0;
            breakerDecided = false;
        }
        if (!engine.canBreakCombo(me)) return;
        if (!breakerDecided) {
            breakerDecided = true;
            // O aperto cabe no atordoamento que ainda falta: esperar mais que ele deixava o combo
            // acabar antes (decidia quebrar 45% das vezes e quebrava 15%).
            int cabe = Math.max(1, Math.min(BREAKER_GUESS_FRAMES, me.stunLeft - 1));
            if (roll(level.comboBreaker())) breakerWait = 1 + reactRandom.nextInt(cabe);
        }
        if (breakerWait > 0 && --breakerWait == 0) out.light = true;
    }

    /**
     * Depois de quebrar o combo, tenta o troco: o rival fica atordoado um instante e longe (o
     * estalo o joga para trás); ao alcance, abre a rota do caderno com o primeiro golpe que alcança,
     * e sem nenhum ao alcance corre até ele enquanto o atordoamento dura (quase sempre ele se
     * recompõe antes: a luta volta ao neutro).
     */
    private boolean breakerFollowUp(CombatFighter me, CombatFighter target, FighterInput out) {
        if (!breakerCombo) return false;
        if (!target.inHitstun() || target.ko() || !me.grounded) {
            breakerCombo = false;
            return false;
        }
        // O estalo ainda joga o rival para trás: a corrida sozinha leva até ele.
        if (!me.canAct() || target.frozen() || target.pushFramesLeft > 0) return true;
        float gap = Math.abs(target.x - me.x);
        CharacterDefinition c = me.character();
        for (String starter : new String[] {"L", "2L", "M", "2M"}) {
            if (c.attack(starter) == null ||
                !CpuReach.reaches(c, starter, false, gap, target.body(), target.crouchingBody())) continue;
            startRoute(c, starter);
            press(starter, me, out);
            lastReason = "combo breaker";
            breakerCombo = false;
            return true;
        }
        if (me.breakerChase > 0) return true;
        movingBack = false;
        movingForward = dashing = true;
        out.direction = target.x > me.x ? 1 : 5;
        out.dash = true;
        return true;
    }

    private boolean breakerCombo;

    /**
     * PROTÓTIPO PERCEPCAO: leu o golpe (o rival ficou desequilibrado) e o golpe preciso está
     * pronto: castiga com o melhor que chega a tempo; longe, corre até ele.
     */
    private boolean leituraFollowUp(CombatFighter me, CombatFighter target, FighterInput out) {
        if (me.state.precisoQuadros <= 0 || !me.state.inShinkai(Shinkai.Type.PERCEPCAO) || !me.grounded ||
            !target.inHitstun() || target.ko()) return false;
        if (!me.canAct()) return true;
        float gap = Math.abs(target.x - me.x);
        if (punirComOMelhor(me, target, target.stunLeft, gap, target.crouchingBody(), out, "leitura")) return true;
        movingBack = false;
        movingForward = dashing = true;
        out.direction = target.x > me.x ? 1 : 5;
        out.dash = true;
        return true;
    }

    /**
     * SHINKAI Clash: empurra do jeito do tipo dela, mas com a mesma média de empurrão por quadro em
     * todos os tipos ({@link #clashRate}): quem vence é o acaso e o nível, não o tipo (antes a
     * TECHNIQUE da CPU vencia 98% dos confrontos e o POWER 9%; docs/balanco/ajustes.md, etapa 2).
     */
    private void pushInClash(ShinkaiClash c, int self, FighterInput out) {
        if (!c.disputando()) return;
        if (c != clashSorteado) {
            // Cada confronto tem o fôlego dele (±15%): quem vence entre duas CPUs do mesmo nível é
            // o acaso, não a forma de empurrar de cada tipo.
            clashSorteado = c;
            clashFolego = 0.85 + 0.3 * reactRandom.nextDouble();
        }
        double taxa = clashRate(level.number) * clashFolego;
        switch (c.tipo[self]) {
            case POWER:
                // H (58) a cada espera de 12 e um respiro sorteado: 58 / (12 + 1/p) = taxa.
                if (c.espera[self] == 0 && roll(1.0 / Math.max(1.0, 58.0 / taxa - 12.0))) out.heavy = true;
                break;
            case SPEED:
                // L (36) a cada espera de 3 e um respiro: 36 / (3,3 + 1/p) = taxa (medido: ela vê a
                // espera acabar um pouco depois).
                if (c.espera[self] == 0 && roll(1.0 / Math.max(1.0, 36.0 / taxa - 3.3))) out.light = true;
                break;
            case ENERGY: {
                // Segura uma carga C e solta: C² / 16 em C + 1 quadros ≈ taxa com C = 16 taxa + 1, um
                // pouco mais porque a carga que não termina no fim do tempo se perde (sorteada ±10%).
                if (clashHold == 0) {
                    double carga = (16.0 * taxa * 1.12 + 1.0) * (0.9 + 0.2 * reactRandom.nextDouble());
                    clashHold = (int)Math.max(8, Math.min(ShinkaiClash.CARGA_MAX, Math.round(carga)));
                }
                if (c.carga[self] < clashHold) {
                    out.holding = true;
                } else {
                    clashHold = 0;
                }
                break;
            }
            case TECHNIQUE:
            default: {
                // Só no pulso (105 a cada 24 quadros), com duas chances por pulso: acerta o pulso
                // com chance q = taxa · 24 / 105.
                int fase = c.fase();
                boolean pulso = fase == 0 || fase == ShinkaiClash.BATIDA - 1;
                double q = Math.min(1.0, taxa * ShinkaiClash.BATIDA / 105.0);
                double porTentativa = 1.0 - Math.sqrt(1.0 - q);
                if (pulso && !c.batidaUsada[self] && roll(porTentativa)) out.medium = true;
                break;
            }
        }
    }

    /**
     * Empurrão médio por quadro da CPU no SHINKAI Clash, o mesmo em todos os tipos (1,9 no nível
     * 1, 3,8 no 8: o ENERGY chega nisso com a carga cheia de 70 quadros, o teto dele).
     */
    static double clashRate(int level) {
        return 1.9 + 0.27 * (level - 1);
    }

    /** O confronto em que o fôlego foi sorteado, e o fôlego (multiplica a taxa de empurrão). */
    private ShinkaiClash clashSorteado;
    private double clashFolego = 1.0;

    /** SHINKAI Clash (ENERGY): até onde ela segura a carga desta vez. */
    private int clashHold;

    /** Quanto ela espera (no máximo) para apertar depois do "!". */
    static final int BREAKER_GUESS_FRAMES = 18;
    private int breakerWait;
    /** Já decidiu (quebrar ou não) o combo que está levando. */
    private boolean breakerDecided;
    /** O combo em que a decisão foi tomada. */
    private ComboSession breakerSession;


    /** Quantos quadros antes do contato ela aperta a defesa perfeita (dentro da janela). */
    static final int JUST_DEFENSE_LEAD = 2;
    private int justDefenseWait;

    /** Decide se defende o que viu e em que altura (errar a altura também é humano). */
    private boolean decideGuard(int required, int frames) {
        if (!roll(level.reactGuard)) return false;
        guardHabit = false;
        boolean right = roll(level.guardHeight);
        if (required == GUARD_HIGH) guardLow = !right;
        else if (required == GUARD_LOW) guardLow = right;
        else if (required == GUARD_HIGH_ONLY) guardLow = false;
        // Golpe do meio: segura em pé ou agachada (agachada segura também os baixos, mas
        // agachada sempre ela parecia grudada no chão).
        else guardLow = roll(habitLow());
        guardFrames = Math.max(guardFrames, frames);
        return true;
    }

    /**
     * Chance de defender agachada (por hábito, ou o golpe do meio): sobe com o nível. Aprende a
     * altura dele: quem só dá rasteira ela defende baixo; quem só bate alto, em pé.
     */
    private double habitLow() {
        double base = HABIT_LOW + (0.95 - HABIT_LOW) * smart();
        double baixa = leitura.parteBaixa();
        if (baixa < 0.0) return base;
        double w = leitura.pesoBaixo() * smart();
        return Math.max(0.05, Math.min(0.97, base * (1.0 - w) + baixa * w));
    }

    /**
     * Levantar (e sair do atordoamento) defendendo, como quem joga: segura trás (ou trás-baixo) já
     * deitada, e no primeiro quadro livre está defendendo. Sem isso ela levantava parada e só
     * decidia defender depois da reação: a rasteira em cima da hora e o jM no levantar entravam
     * sempre (registro real, 01/11). A chance sobe com o que ele tem feito nela (golpes de perto e
     * pulos que entraram) e com o nível; a altura é a que ela aprendeu dele (rival no ar: em pé).
     */
    private boolean saidaDefendendo(CombatFighter me, FighterInput out) {
        if (!saidaLigada) return false;
        boolean presa = me.grounded && (me.status == CombatFighter.Status.WAKEUP && !me.rolling() ||
            me.status == CombatFighter.Status.HITSTUN);
        CpuPerception.Snapshot seen = seenNow;
        if (presa) {
            saidaPresa = true;
            if (seen == null || seen.ko || Math.abs(seen.x - me.x) >= SAIDA_PERTO || out.superAttack) return false;
            if (saidaPlano < 0) {
                double aprendeu = Math.min(1.0, (leitura.cutucadas() + leitura.pulosNela()) / APRENDE_CUTUCADAS);
                double p = level.reactGuard * (SAIDA_BASE + SAIDA_APRENDE * aprendeu * (0.5 + 0.5 * smart()));
                saidaPlano = roll(p * (1.0 - 0.6 * urgency)) ? 1 : 0;
                saidaBaixa = roll(habitLow());
            }
            if (saidaPlano == 0) return false;
            // Ainda presa: só a direção (segura a defesa para o primeiro quadro livre).
            guardHabit = false;
            altaPorPulo = !seen.grounded;
            guardLow = seen.grounded && saidaBaixa;
            guardFrames = SAIDA_QUADROS + reactRandom.nextInt(SAIDA_SPREAD);
            int side = seen.x > me.x ? 1 : seen.x < me.x ? -1 : me.facing;
            out.direction = guardLow ? (side > 0 ? 4 : 2) : (side > 0 ? 5 : 1);
            lastReason = me.status == CombatFighter.Status.WAKEUP ? "levanta defendendo" : "sai do atordoamento defendendo";
            return true;
        }
        if (saidaPresa) {
            // Livre agora: a defesa segura (guardFrames) continua na defesa comum.
            saidaPresa = false;
            if (saidaPlano != 1) guardFrames = 0;
            saidaPlano = -1;
        }
        return false;
    }

    /**
     * Ar contra ar: com o rival pulando (livre ou golpeando) perto, o golpe no ar (L, M ou H) que
     * sai mais cedo e chega até ele, contando o quanto os dois se aproximam até o golpe ficar
     * ativo. Null: ninguém no ar, ou nenhum golpe alcança ainda.
     */
    private String golpeArAr(CombatEngine engine, CombatFighter me, CpuPerception.Snapshot seen, float ultimoX) {
        if (!arArLigado || !arArPlano || seen == null || seen.ko || seen.grounded ||
            seen.status != CombatFighter.Status.NEUTRAL && seen.status != CombatFighter.Status.ATTACK) return null;
        // A trajetória dele pelo que ela viu (o pulo segue reto no ar): onde ele está agora.
        CpuPerception.Snapshot antes = perception.seen(reacao + MOTION_SAMPLE);
        int amostra = MOTION_SAMPLE;
        if (antes == null || antes.grounded) {
            antes = perception.seen(reacao + 1);
            amostra = 1;
        }
        float vRival = antes == null ? 0f : (seen.x - antes.x) / amostra;
        int passou = Math.max(0, engine.frame() - seen.frame);
        float x = seen.x + vRival * passou;
        float gap = Math.abs(x - me.x);
        if (gap > JUMP_IN_REACH + 80f) return null;
        float lado = Math.signum(x - me.x);
        // O próprio andar no ar ela sabe (o quanto andou do quadro passado para este).
        float vEu = Float.isNaN(ultimoX) ? 0f : me.x - ultimoX;
        float aproxima = Math.max(0f, vEu * lado - vRival * lado);
        String melhor = null;
        int cedo = Integer.MAX_VALUE;
        for (String b : new String[] {"L", "M", "H"}) {
            CharacterDefinition.Move m = me.character().move(b, true);
            if (m == null || m.attack == null) continue;
            AttackDefinition a = m.attack;
            if (a.reach + aproxima * a.startupFrames < gap) continue;
            if (a.startupFrames < cedo) {
                cedo = a.startupFrames;
                melhor = b;
            }
        }
        return melhor;
    }

    /** Baterias: desliga a escolha do golpe no ar contra ar (antes × depois). */
    static boolean arArLigado = true;
    /** Onde ela estava no quadro passado (a velocidade dela no ar; o pulo não usa vx). */
    private float arArUltimoX = Float.NaN;
    /** Neste pulo ela vai disputar o ar (chance do nível, sorteada uma vez por pulo). */
    private boolean arArPlano, arArSorteado;

    /** Baterias: ela pula para dentro agora (o mesmo que o plano do pulo faz). */
    void pularParaDentro(CombatFighter me, FighterInput out) {
        out.clear();
        out.jump = true;
        out.direction = me.facing > 0 ? 8 : 6;
        jumpHold = JUMP_HOLD_FRAMES;
        jumpHoldDirection = out.direction;
        jumpIn = true;
        crossJump = false;
        guardFrames = 0;
        lastReason = "pulo para dentro";
    }

    /**
     * O pulo que acertou e o combo no chão (pedido do jogador, 02/11: "ela não comba"): o golpe no
     * ar do pulo para dentro saía sozinho e, ao cair, ela não emendava nada. Agora, caindo com ele
     * ainda atordoado (ela sente o próprio golpe), emenda o combo do chão pelo primeiro golpe que
     * alcança, com a chance de confirmar do nível.
     */
    private boolean emendaDoPulo(CombatFighter me, CombatFighter target, FighterInput out) {
        if (!pousoEmenda || !me.grounded || !me.canAct()) return false;
        pousoEmenda = false;
        if (!emendaLigada || route != null || !target.inHitstun() || !target.grounded) return false;
        if (!roll(level.confirm * (0.6 + 0.4 * smart()))) return false;
        CharacterDefinition c = me.character();
        float gap = projectedGap(me, target);
        List<String> book = ComboBook.route(c);
        String primeiro = book.isEmpty() ? null : book.get(0);
        for (String abre : new String[] {primeiro, "L", "2L", "M", "2M"}) {
            if (abre == null || abre.startsWith("j") || ComboBook.commits(abre) || !alcanca(c, abre, gap, target)) continue;
            startRoute(c, abre);
            press(abre, me, out);
            lastReason = "pulo que acertou: emenda no chão";
            return true;
        }
        return false;
    }

    /**
     * A bola (S de projétil) sai no contato do golpe, sem esperar ver que acertou: como quem joga
     * cancela o M na bola de olho fechado (defendida, a bola de longe é segura). Antes ela esperava
     * a reação inteira e a janela do cancelamento já tinha fechado.
     */
    private static boolean cancelaNoContato(CharacterDefinition c, String step) {
        if (!contatoLigado || !"S".equals(step)) return false;
        AttackDefinition a = c.attack("S");
        return a != null && a.kind == AttackDefinition.Kind.PROJECTILE;
    }

    /** Baterias: desliga o cancelamento na bola no contato (antes × depois). */
    static boolean contatoLigado = true;

    /** Baterias: a rota de combo atual e o passo (depuração). */
    String rotaAtual() { return route == null ? "-" : route + "@" + step + (awaitingStart ? " esperando" : "") + " decidiu=" + stepDecided + " go=" + stepGo; }

    /** Baterias: o SHINKAI do combo só no golpe que acertou (antes × depois). */
    static boolean shinkaiNoAcerto = true;
    /** O SHINKAI cortou o golpe do combo que acertou: o próximo passo sai do neutro. */
    private boolean cortePeloShinkai;

    /** O golpe solto que já decidiu se segue no combo (quadro em que ele começou). */
    private int soltoDecidido = Integer.MIN_VALUE;
    /** Baterias: desliga o combo depois do golpe solto (antes × depois). */
    static boolean soltoLigado = true;

    /** O golpe no ar acertou: ao cair, emenda no chão (emendaDoPulo). */
    private boolean pousoEmenda;
    /** Baterias: desliga a emenda do pulo (antes × depois). */
    static boolean emendaLigada = true;

    /** Pelo que ela viu (há {@code reacao} quadros), ele já está tocando o chão agora. */
    private boolean caindoAgora(CpuPerception.Snapshot s) {
        if (s.grounded || s.vy <= 0f) return s.grounded;
        // O golpe no ar dele ainda vem (não bateu): fica em pé até ele bater.
        if (s.attacking() && s.outcome == CombatFighter.Outcome.NONE) return false;
        float h = Math.max(0f, Arena.GROUND_Y - s.y);
        float g = chargeConfig != null ? chargeConfig.gravity : 1650f;
        double t = (-s.vy + Math.sqrt((double)s.vy * s.vy + 2.0 * g * h)) / g;
        return t / CombatConfig.DT <= reacao + 1;
    }

    /** Baterias: desliga o levantar defendendo / a troca para baixo quando ele cai do pulo (antes × depois). */
    static boolean saidaLigada = true, trocaBaixaLigada = true;

    /**
     * Pressão dos níveis altos (pedido de 02/11: "ela não pressiona"): golpes mais seguidos no
     * neutro e, com a sequência defendida, segue em cima (outro golpe rápido ou o agarrão) em vez
     * de voltar ao neutro. Por instância (a bateria compara a nova com a antiga na mesma luta).
     */
    /**
     * O quanto ela vai para cima (0 até o nível 6; 0,5 no 7; 0,8 no 8): encurta o respiro entre os
     * golpes, tira o recuo e a parada na beira do jab, segue em cima da sequência defendida e fica
     * em cima de quem caiu.
     */
    private double pressao() {
        if (!pressaoLigada) return 0.0;
        return level.number <= 6 ? 0.0 : level.number == 7 ? PRESSAO_7 : PRESSAO_8;
    }

    static double PRESSAO_7 = 0.5, PRESSAO_8 = 0.8;
    /** Fração do seguir em cima que vai para o agarrão (contra quem fica defendendo). */
    static double PRESSAO_AGARRA = 0.3;
    /** Fração das quedas em que ela vai no agarrão quando ele levanta (o resto é o golpe em cima). */
    static double OKI_AGARRA = 0.25;

    /**
     * A sequência dela acabou de ser defendida e ele ainda está perto: segue em cima (outro golpe
     * rápido logo em seguida, que pega quem aperta botão, ou o agarrão em quem fica defendendo)
     * em vez de voltar ao neutro.
     */
    private boolean seguirEmCima(CombatEngine engine, CombatFighter me, CombatFighter target,
                                 CpuPerception.Snapshot seen, FighterInput out) {
        double p = pressao();
        if (p <= 0.0 || !me.canAct() || !seen.grounded) return false;
        float gap = Math.abs(target.x - me.x);
        if (gap > SEGUE_PERTO || !roll(p)) return false;
        if (roll(PRESSAO_AGARRA)) {
            agarrarLido = PLANO_AGARRAR / 2;
            return agarrar(engine, me, target, out, "pressão: ");
        }
        if (pressionar(me, target, out)) {
            lastReason = "pressão: emenda " + lastReason.substring(lastReason.indexOf('('));
            return true;
        }
        return false;
    }

    /** Distância (centros) até onde ela segue em cima da sequência defendida. */
    static final float SEGUE_PERTO = 230f;

    /** Ela ficou em cima de quem caiu nesta queda (decidido uma vez por queda). */
    private boolean okiDecidido, okiPlano, okiAgarra, okiDoAntiAereo;

    /**
     * Ele caiu: com a pressão do nível (ou depois do antiaéreo dela), chega perto e, quando ele
     * levanta, solta o golpe baixo rápido em cima da hora (o tempo do levantar ela sabe, como quem
     * decorou) ou vai no agarrão. Rolando para longe, deixa.
     */
    private boolean okiPressao(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        boolean caido = target.status == CombatFighter.Status.KNOCKDOWN || target.status == CombatFighter.Status.WAKEUP;
        if (!caido) {
            okiDecidido = false;
            okiDoAntiAereo = false;
            return false;
        }
        if (!okiDecidido) {
            okiDecidido = true;
            double p = Math.max(pressao(), okiDoAntiAereo ? seguirAA() : 0.0);
            okiPlano = p > 0.0 && roll(p);
            okiAgarra = okiPlano && roll(OKI_AGARRA);
        }
        if (!okiPlano || !me.grounded || !me.canAct()) return false;
        if (target.rollFrames > 0 || target.wakeup == CombatFighter.WAKE_BACK_ROLL ||
            target.wakeup == CombatFighter.WAKE_FORWARD_ROLL) {
            okiPlano = false;
            return false;
        }
        CharacterDefinition c = me.character();
        String meaty = c.move("2L", false) != null && c.attack("2L") != null ? "2L" : "L";
        AttackDefinition golpe = c.attack(meaty);
        if (golpe == null) return false;
        float gap = Math.abs(target.x - me.x);
        boolean perto = okiAgarra ? gap <= throwReach(engine, me, target) - 8f
            : CpuReach.reaches(c, meaty, false, gap + OKI_FOLGA, target.body(), target.crouchingBody());
        int livre = livreEm(engine, target);
        if (!perto) {
            out.direction = me.facing > 0 ? 1 : 5;
            out.dash = gap > 300f && livre > 25;
            lastReason = "fica em cima de quem caiu";
            return true;
        }
        if (livre < 0) return false;
        movingForward = movingBack = dashing = false;
        if (okiAgarra) {
            if (livre <= 1) {
                // Levantou: o plano de agarrar pega no primeiro quadro em que ele pode ser agarrado.
                agarrarLido = 10;
                okiPlano = false;
            }
            lastReason = "espera ele levantar para agarrar";
            return true;
        }
        if (livre <= golpe.startupFrames - 1) {
            okiPlano = false;
            startRoute(c, meaty);
            press(meaty, me, out);
            lastReason = "golpe em cima do levantar (" + meaty + ")";
            return true;
        }
        lastReason = "espera ele levantar";
        return true;
    }

    /** Folga do golpe em cima do levantar (ele levanta um pouco mais para lá ou para cá). */
    static final float OKI_FOLGA = 15f;

    /** Quadros até ele poder agir de novo (caindo ou levantando), ou -1 rolando ou ainda no ar. */
    static int livreEm(CombatEngine e, CombatFighter r) {
        CombatConfig c = e.config;
        if (r.status == CombatFighter.Status.WAKEUP) {
            if (r.rollFrames > 0) return -1;
            return Math.max(0, e.wakeupFrames(r) - r.knockdownFrame);
        }
        if (r.status != CombatFighter.Status.KNOCKDOWN || r.knockdownFrame < c.knockdownFallFrames) return -1;
        if (r.wakeup == CombatFighter.WAKE_BACK_ROLL || r.wakeup == CombatFighter.WAKE_FORWARD_ROLL) return -1;
        int down = c.knockdownDownFrames;
        if (r.wakeup == CombatFighter.WAKE_QUICK) down = Math.min(down, c.quickRiseDownFrames);
        if (r.wakeup == CombatFighter.WAKE_DELAY) down += c.delayWakeupFrames;
        return Math.max(0, c.knockdownFallFrames + down - r.knockdownFrame) + e.wakeupFrames(r);
    }

    /** O antiaéreo que ela soltou (quadro), se acertou, e se a emenda no ar já foi tentada. */
    private int aaQuadro = -1000;
    private boolean aaAcertou, aaJuggleFeito, aaJuggleDecidido, aaJugglePlano, alvoNoAr;

    private void marcarAntiAereo(CombatEngine engine) {
        aaQuadro = engine.frame();
        aaAcertou = false;
    }

    /** Chance de ir atrás depois do antiaéreo que acertou (pelo nível). */
    private double seguirAA() {
        return seguirAntiAereoLigado ? level.confirm * (0.5 + 0.5 * smart()) : 0.0;
    }

    /**
     * O antiaéreo acertou e ele está caindo atordoado: emenda um golpe de chão que alcança onde ele
     * vai estar (uma tentativa). Se não der, a queda dele fica com o golpe em cima do levantar
     * ({@link #okiPressao}).
     */
    private boolean seguirAntiAereo(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        if (aaNoAr > 0) {
            // Pulou atrás dele: o golpe aéreo quando ele estiver ao alcance; caiu, acabou.
            if (me.grounded && ++aaNoAr > 12 || target.status != CombatFighter.Status.AIR_HITSTUN) {
                aaNoAr = 0;
                return false;
            }
            if (me.grounded) {
                out.superJump = true;
                out.direction = target.x > me.x ? 8 : 6;
                return true;
            }
            if (!me.canAct()) return false;
            float gap = Math.abs(target.x - me.x);
            if (Math.abs(target.y - me.y) <= 115f) {
                for (String b : new String[] {"H", "M", "L"}) {
                    if (me.character().move(b, true) == null || !CombatRules.reaches(me.character(), b, true, gap, target.body())) continue;
                    aaNoAr = 0;
                    press("j" + b, me, out);
                    lastReason = "antiaéreo: pula atrás e bate (j" + b + ")";
                    return true;
                }
            }
            out.direction = target.x > me.x ? 1 : 5;
            return true;
        }
        if (!aaAcertou || aaJuggleFeito || !juggleLigado) return false;
        if (target.status != CombatFighter.Status.AIR_HITSTUN) {
            if (target.status != CombatFighter.Status.HITSTUN) aaAcertou = false;
            return false;
        }
        if (!me.grounded || !me.canAct() || target.stunLeft <= 0) return false;
        if (!aaJuggleDecidido) {
            aaJuggleDecidido = true;
            aaJugglePlano = roll(seguirAA());
        }
        if (!aaJugglePlano) return false;
        CharacterDefinition c = me.character();
        float g = engine.config.gravity;
        for (int i = 0; i < JUGGLE.length; i++) {
            String b = JUGGLE[i];
            AttackDefinition a = c.attack(b);
            if (a == null || c.move(b, false) == null || target.stunLeft <= a.startupFrames) continue;
            // Onde ele vai estar quando o golpe sair (ele cai e anda com o empurrão): na altura que
            // o golpe pega e ao alcance.
            float t = a.startupFrames * CombatConfig.DT;
            float x = target.x + target.vx * t;
            float y = target.y + target.vy * t + 0.5f * g * t * t;
            float altura = Arena.GROUND_Y - y;
            if (altura < 0f || altura > JUGGLE_ALTURA[i]) continue;
            if (!CombatRules.reaches(c, b, false, Math.abs(x - me.x), target.body())) continue;
            aaJuggleFeito = true;
            startRoute(c, b);
            press(b, me, out);
            lastReason = "antiaéreo: emenda no ar (" + b + ")";
            return true;
        }
        // Ainda alto e atordoado por um bom tempo: super pulo atrás dele.
        float altura = Arena.GROUND_Y - target.y;
        if (puloAtrasLigado && altura > 150f && target.stunLeft >= AA_PULO_STUN && Math.abs(target.x - me.x) < 300f) {
            aaJuggleFeito = true;
            aaNoAr = 1;
            out.superJump = true;
            out.direction = target.x > me.x ? 8 : 6;
            lastReason = "antiaéreo: pula atrás";
            return true;
        }
        // Ainda alto: anda para debaixo dele e espera a hora.
        float gap = Math.abs(target.x - me.x);
        movingForward = gap > 110f;
        out.direction = movingForward ? (me.facing > 0 ? 1 : 5) : 0;
        lastReason = "antiaéreo: espera ele cair";
        return true;
    }

    /** Golpes de chão para emendar no rival que cai do antiaéreo (o mais forte que alcança primeiro). */
    static final String[] JUGGLE = {"2H", "H", "M", "L"};
    /** Até que altura (do chão) cada um pega quem cai. */
    static final float[] JUGGLE_ALTURA = {170f, 110f, 90f, 80f};
    static boolean juggleLigado = true, puloAtrasLigado = true;
    /** Atordoamento que ainda falta para valer o super pulo atrás dele (a subida leva uns 20 quadros). */
    static final int AA_PULO_STUN = 24;
    /** Quadros do super pulo atrás dele depois do antiaéreo (0: não está pulando). */
    private int aaNoAr;

    static boolean pressaoPadrao = true;
    boolean pressaoLigada = pressaoPadrao;
    /** Depois do antiaéreo que acertou: vai atrás (emenda no ar ou fica em cima de quem cai). */
    static boolean seguirAntiAereoPadrao = true;
    boolean seguirAntiAereoLigado = seguirAntiAereoPadrao;

    /** Rival mais perto que isto no levantar: ela levanta defendendo (se decidir). */
    static final float SAIDA_PERTO = 300f;
    /** Chance de levantar defendendo, sem ter aprendido nada dele (× a defesa do nível). */
    static final double SAIDA_BASE = 0.3;
    /** O quanto sobe com o que ele já fez nela de perto e pulando. */
    static final double SAIDA_APRENDE = 0.6;
    static final int SAIDA_QUADROS = 14, SAIDA_SPREAD = 8;

    /** Pulos dele que chegaram nela até a chance de tirar do ar ir ao máximo do nível. */
    static final double APRENDE_PULOS = 3.0;
    /** Golpes de chão dele que entraram nela até a defesa por hábito ir ao máximo. */
    static final double APRENDE_CUTUCADAS = 4.0;
    /** O quanto a defesa por hábito sobe com o que ele tem acertado de perto. */
    static final double APRENDE_GUARDA = 0.45;

    /** Plano da última decisão do neutro: entrar andando no alcance do jab do rival. */
    private boolean walkInPlan = true;
    private CharacterDefinition jabFor;
    private float jabRange;

    /** Até onde chega o golpe rápido do rival (L ou 2L): a beira onde ela para de andar. */
    private float jabRange(CombatFighter me, CombatFighter target) {
        CharacterDefinition c = target.character();
        if (c != jabFor) {
            jabFor = c;
            jabRange = 0f;
            for (String b : new String[] {"L", "2L"}) {
                CharacterDefinition.Move move = c.move(b, false);
                if (move != null) jabRange = Math.max(jabRange, CpuReach.maxCenterDistance(c, b, false, me.body(), false));
            }
        }
        return jabRange;
    }

    /** Até onde o rival alcança com um golpe no chão (mais uma folga): o "colado" da defesa. */
    private float threatRange(CombatFighter me, CombatFighter target) {
        CharacterDefinition c = target.character();
        if (c != threatFor) {
            threatFor = c;
            threatRange = 0f;
            for (String b : OpponentAi.GROUND_ATTACKS) {
                CharacterDefinition.Move move = c.move(b, false);
                if (move != null) threatRange = Math.max(threatRange, CpuReach.maxCenterDistance(c, b, false, me.body(), false));
            }
            threatRange += 40f;
        }
        return threatRange;
    }

    /**
     * O tech é palpite (a janela é menor que a reação humana): a CPU só aperta se já esperava o
     * agarrão, isto é, se o rival estava colado no que ela viu antes de ele agarrar.
     */
    private boolean expectingThrow(CombatEngine engine, CombatFighter me, CombatFighter target,
                                   CpuPerception.Snapshot seen) {
        // No começo da luta ela ainda não tem a reação inteira de história: vale o quadro mais
        // antigo que viu (é de antes do agarrão, e só a distância conta aqui).
        CpuPerception.Snapshot s = seen != null ? seen : perception.oldest();
        return s != null && s.grounded && Math.abs(s.x - me.x) <= throwReach(engine, me, target) + 40f;
    }

    private static float throwReach(CombatEngine engine, CombatFighter me, CombatFighter target) {
        return me.body().halfWidth + target.body().halfWidth + engine.config.throwRange - 4f;
    }

    // ---------------------------------------------------------------- cartas

    /** Quadros entre os dois que ela compara para ver para onde o rival está indo. */
    static final int MOTION_SAMPLE = 6;

    /**
     * Onde o rival deve estar agora pelo que ela viu: a posição vista mais o movimento que ela
     * viu nos quadros anteriores, levado adiante pelo atraso da reação (como um humano que vê o
     * rival recuar do empurrão e não bate no vazio). Só usa o que já viu, nunca o quadro atual.
     */
    private float predictedX(CombatEngine engine, CpuPerception.Snapshot seen) {
        float x = seen.x;
        CpuPerception.Snapshot before = perception.seen(reacao + MOTION_SAMPLE);
        // Só o andar/correr do rival continua no mesmo ritmo: o deslizar de quem apanhou ou caiu
        // freia e para (levar adiante passava do ponto e ela batia de longe).
        if (before != null && seen.grounded && before.grounded &&
            seen.status == CombatFighter.Status.NEUTRAL && before.status == CombatFighter.Status.NEUTRAL) {
            x += (seen.x - before.x) / MOTION_SAMPLE * reacao;
        }
        // O próprio golpe ela sente na hora: sabe onde o rival estava quando encostou e quanto
        // aquele golpe empurra. Até ver o empurrão acontecer (ele pode atrasar num congelamento),
        // conta com o rival lá onde o empurrão leva.
        if (contactPush > 0f && engine.frame() - contactFrame <= CONTACT_MEMORY_FRAMES && seen.grounded) {
            float pushedTo = contactTargetX + contactSide * contactPush;
            x = contactSide > 0 ? Math.max(x, pushedTo) : Math.min(x, pushedTo);
        }
        return x;
    }

    /** Até quanto tempo depois de encostar ela ainda conta com o empurrão do próprio golpe. */
    static final int CONTACT_MEMORY_FRAMES = 90;

    /** O próprio golpe que acabou de encostar (acerto ou defesa): quando, onde o rival estava, quanto empurra e para que lado. */
    private int contactFrame = Integer.MIN_VALUE;
    private float contactPush, contactTargetX;
    private int contactSide;

    /** Anota o encostar do próprio golpe neste passo (o que ela sente, não o que vê). */
    private void feelContact(CombatEngine engine, int self, CombatFighter me, CombatFighter target) {
        for (CombatEngine.HitEvent event : engine.events()) {
            if (event.attacker != self || !me.attacking()) continue;
            AttackDefinition attack = me.attack;
            contactFrame = engine.frame();
            contactPush = event.blocked ? attack.pushbackOnBlock : attack.pushbackOnHit;
            contactSide = target.x >= me.x ? 1 : -1;
            contactTargetX = target.x;
        }
    }

    /** A própria guarda está no aviso (quase quebrando). */
    private static boolean guardLow(CombatEngine engine, CombatFighter me) {
        return me.state.guard * 1000 <= engine.config.guardWarningPermille * CombatConfig.GUARD_MAX;
    }

    /**
     * Viu (com a reação do nível) a guarda do rival quebrar: com a chance de punir do nível,
     * vai para cima dele e abre o combo do caderno enquanto ele está aberto.
     */
    private boolean punishGuardBreak(CombatFighter me, CombatFighter target, CpuPerception.Snapshot seen,
                                     FighterInput out) {
        if (!seen.guardBroken || !me.grounded || target.ko()) return false;
        if (seen.guardBreakSerial != lastBreakSerial) {
            lastBreakSerial = seen.guardBreakSerial;
            breakPlan = roll(level.punish);
        }
        if (!breakPlan) return false;
        float gap = Math.abs(seen.x - me.x);
        if (ultraPunish(me, target, ULTRA_PUNISH_MIN, gap, out)) {
            breakPlan = false;
            return true;
        }
        CharacterDefinition c = me.character();
        for (String starter : new String[] {"M", "L", "2L"}) {
            if (c.attack(starter) == null ||
                !CpuReach.reaches(c, starter, false, gap, target.body(), target.crouchingBody())) continue;
            startRoute(c, starter);
            press(starter, me, out);
            lastReason = "guard break";
            breakPlan = false;
            return true;
        }
        // Longe: corre até ele enquanto a quebra dura.
        movingBack = false;
        movingForward = dashing = true;
        out.direction = seen.x > me.x ? 1 : 5;
        out.dash = true;
        return true;
    }

    /**
     * Viu o rival errar um golpe ao alcance: se a recuperação que ela calcula que sobra dá para o
     * golpe de abertura do caderno, pune com o combo.
     */
    private boolean blockedLastFrame;
    private int lastRivalBlockFrame = Integer.MIN_VALUE / 2;

    /**
     * Saiu da defesa e o golpe que ela defendeu ainda está na recuperação: pune com o golpe mais
     * rápido que entra a tempo. Não é reação (a brecha é menor que a reação humana): é saber que
     * aquele golpe é inseguro, como o jogador que decorou o frame data. Só golpe que ela viu.
     */
    private boolean punishOnBlock(CombatEngine engine, CombatFighter me, CombatFighter target,
                                  CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.canAct() || !me.grounded || !target.grounded || !target.attacking() || seen == null ||
            !seen.attacking() || seen.attack != target.attack) return false;
        if (!roll(level.punish)) return false;
        int left = target.attack.totalFrames - target.attackFrame;
        float gap = Math.abs(target.x - me.x);
        if (ultraPunish(me, target, left, gap, out)) return true;
        return punirComOMelhor(me, target, left, gap, target.crouchingBody(), out, "punir na defesa");
    }

    static final String[] NORMAIS_PUNICAO = {"H", "2H", "2M", "M", GolpeAlto.BINDING, "L", "2L"};
    /** Quadros a mais que o especial de avanço precisa caber na brecha para valer a punição. */
    static final int FOLGA_ESPECIAL = 8;

    /**
     * A punição mais forte que entra a tempo e alcança, como o jogador que conhece o frame data:
     * o Super (se a barra vale), a rota do caderno a partir do primeiro passo que alcança (o L de
     * longe não chega, o H chega, e o resto da rota vem junto), os outros golpes de chão e os
     * especiais que avançam, pelo dano.
     */
    private boolean punirComOMelhor(CombatFighter me, CombatFighter target, int left, float gap, boolean crouching,
                                    FighterInput out, String porque) {
        CharacterDefinition c = me.character();
        AttackDefinition sup = c.attack("SUPER");
        if (sup != null && left >= sup.startupFrames && OpponentAi.superInRange(c, gap) && spendSuper(me, target)) {
            endRoute();
            guardFrames = 0;
            out.superAttack = true;
            lastReason = porque + " (Super)";
            return true;
        }
        for (String b : ComboBook.route(c)) {
            if (b.startsWith("j") || b.startsWith("S") || ComboBook.JUMP.equals(b) || ComboBook.AIR_JUMP.equals(b)) break;
            if (cabe(c, b, left, gap, target, crouching)) {
                startRoute(c, b);
                press(b, me, out);
                guardFrames = 0;
                lastReason = porque + " (" + b + ")";
                return true;
            }
        }
        String melhor = null;
        int dano = -1;
        for (String b : NORMAIS_PUNICAO) {
            CharacterDefinition.Move m = c.move(b, false);
            if (m != null && m.damage > dano && cabe(c, b, left, gap, target, crouching)) {
                melhor = b;
                dano = m.damage;
            }
        }
        CpuSpecials.Option especial = null;
        for (CpuSpecials.Option o : CpuSpecials.of(c)) {
            if (o.role != CpuSpecials.Role.RUSH && o.role != CpuSpecials.Role.POKE && o.role != CpuSpecials.Role.LOW &&
                o.role != CpuSpecials.Role.ANTI_AIR && o.role != CpuSpecials.Role.ARMOR) continue;
            // O especial que avança leva tempo para chegar: só com folga (de longe ele saía no vazio
            // e ela levava o troco), e só se nenhum golpe de chão entra.
            if (melhor != null || o.attack.startupFrames + FOLGA_ESPECIAL > left ||
                CpuSpecials.contato(o, gap, target.body()) + 2 > left ||
                !CpuSpecials.reaches(o, gap, target.body(), crouching) || !lifeAllows(me, o) || o.attack.damage <= dano) continue;
            especial = o;
            dano = o.attack.damage;
        }
        if (especial != null) {
            pressSpecial(especial, out);
            guardFrames = 0;
            lastReason = porque + " (" + especial.id + ")";
            return true;
        }
        if (melhor == null) return false;
        startRoute(c, melhor);
        press(melhor, me, out);
        guardFrames = 0;
        lastReason = porque + " (" + melhor + ")";
        return true;
    }

    /** O golpe de chão {@code b} sai antes de {@code left} quadros e alcança daqui. */
    private static boolean cabe(CharacterDefinition c, String b, int left, float gap, CombatFighter target, boolean crouching) {
        AttackDefinition a = c.attack(b);
        return a != null && c.move(b, false) != null && a.startupFrames <= left &&
            CpuReach.reaches(c, b, false, gap, target.body(), crouching);
    }

    private boolean punish(CombatEngine engine, CombatFighter me, CombatFighter target,
                           CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded || !seen.grounded || !seen.attacking() || seen.threatening() ||
            seen.outcome == CombatFighter.Outcome.HIT || seen.attackSerial == lastPunishSerial) return false;
        lastPunishSerial = seen.attackSerial;
        if (!roll(level.punish)) return false;
        int left = seen.recoveryLeft() - reacao;
        float gap = Math.abs(seen.x - me.x);
        if (ultraPunish(me, target, left, gap, out)) return true;
        return punirComOMelhor(me, target, left, gap, seen.crouching, out, "punir");
    }

    /**
     * O Dragon Rush dela acertou: aperta para as pancadas (mais, quanto maior o nível) e, de
     * costas para a parede no fim, troca de lado (o rival vai para o canto).
     */
    private void sequenciaDoRush(CombatFighter me, FighterInput out) {
        if (++rushTick % 3 == 0 && reactRandom.nextDouble() < 0.4 + 0.6 * level.punish) out.light = true;
        float costas = me.facing > 0 ? me.x - Arena.LEFT_BOUND : Arena.RIGHT_BOUND - me.x;
        if (me.rushSeq <= 3 && costas < 320f) out.direction = MotionParser.relative(5, me.facing);
    }

    /**
     * O Dragon Rush dele vindo: com a chance do anti-aéreo do nível (um sorteio por voo), sobe o
     * 2H quando o voo chega no tempo dele (o 2H vence o voo).
     */
    private boolean antiRush(CombatEngine engine, CombatFighter me, CombatFighter target,
                             CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded || seen.attack == null || !CombatEngine.DRAGON_RUSH.equals(seen.attack.id) ||
            seen.outcome != CombatFighter.Outcome.NONE || seen.attackFrame > seen.attack.lastActiveFrame()) return false;
        CharacterDefinition c = me.character();
        AttackDefinition aa = c.attack("2H");
        if (aa == null || c.move("2H", false) == null) return false;
        if (seen.attackSerial != lastRushSerial) {
            lastRushSerial = seen.attackSerial;
            antiRushPlan = roll(level.antiAir);
        }
        if (!antiRushPlan) return false;
        // Onde ele está agora: o voo é reto e rápido (o que ela viu, mais o que ele andou desde então).
        CombatConfig cfg = engine.config;
        float passo = cfg.dragonRushSpeed * CombatConfig.DT;
        int voando = Math.max(0, reacao - Math.max(0, seen.attack.startupFrames - seen.attackFrame));
        float gap = Math.abs(seen.x - me.x) - passo * voando - me.body().halfWidth - target.body().halfWidth;
        if (gap > cfg.dragonRushReach + passo * (aa.startupFrames + aa.activeFrames / 2f)) return false;
        antiRushPlan = false;
        startRoute(c, "2H");
        press("2H", me, out);
        lastReason = "anti-rush";
        return true;
    }

    /** Um sorteio por pulo dele: vai tirar do ar? (a chance do nível, mais o que ela aprendeu). */
    private void decidirAntiAereo(CombatFighter me, CpuPerception.Snapshot seen) {
        if (seen.jumpSerial == lastAntiAirSerial) return;
        lastAntiAirSerial = seen.jumpSerial;
        if (me.character().attack("2H") == null) {
            antiAirPlan = false;
            return;
        }
        boolean coming = (me.x - seen.x) * seen.vx > 0f || Math.abs(seen.x - me.x) < 200f;
        // Aprende: os pulos dele que chegaram nela puxam a chance para cima (o quanto, pelo nível).
        double aprendido = Math.min(1.0, leitura.pulosNela() / APRENDE_PULOS);
        double chance = level.antiAir + (0.97 - level.antiAir) * aprendido * smart();
        antiAirPlan = coming && roll(antiAereoLido > 0 ? Math.max(chance, 0.95) : chance);
        antiAirSpecial = antiAirPlan && specialRandom.nextDouble() < SPECIAL_ANTI_AIR_SHARE;
    }

    /**
     * Viu o rival pular para cima dela: com a chance do nível, espera ele chegar (pela trajetória
     * que viu) e sobe com o 2H, que lança e começa o combo aéreo.
     */
    private boolean antiAir(CombatEngine engine, CombatFighter me, CombatFighter target,
                            CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.grounded) return false;
        if (seen.grounded || seen.status != CombatFighter.Status.NEUTRAL && !seen.attacking()) {
            antiAirPlan = false;
            return false;
        }
        CharacterDefinition c = me.character();
        if (c.attack("2H") == null) return false;
        decidirAntiAereo(me, seen);
        if (!antiAirPlan) return false;
        // Onde ele está agora pelo que ela viu: a trajetória do pulo é previsível. (Contar também o
        // início do 2H foi testado e piorou: 02/11, docs/balanco/voadora-rasteira.md.)
        float t = reacao * CombatConfig.DT;
        float x = seen.x + seen.vx * t;
        float y = seen.y + seen.vy * t + 0.5f * engine.config.gravity * t * t;
        float gap = Math.abs(x - me.x);
        float height = Arena.GROUND_Y - y;
        CpuSpecials.Option rising = risingSpecial(me, gap, height, target);
        if (rising != null && antiAirSpecial) {
            antiAirPlan = false;
            pressSpecial(rising, out);
            lastReason = "antiaereo (" + rising.id + ")";
            marcarAntiAereo(engine);
            return true;
        }
        if (height > 240f || height < 0f || !CombatRules.reaches(c, "2H", false, gap, target.body())) return false;
        antiAirPlan = false;
        startRoute(c, "2H");
        press("2H", me, out);
        lastReason = "antiaereo";
        marcarAntiAereo(engine);
        return true;
    }

    // ---------------------------------------------------------------- especiais (S2–S4)

    /**
     * As respostas de especial ao que ela viu, antes da defesa: o mergulho no pulo, o
     * contra-golpe, a esquiva ou a armadura contra o golpe que vem, e o repel ou a esquiva
     * contra a bola. Cada golpe e cada bola do rival têm um sorteio só (a chance do nível);
     * quando o tempo ainda não é o da janela ela espera a hora, sem defender por cima.
     */
    private boolean specialReaction(CombatEngine engine, CombatFighter me, CombatFighter target,
                                    CpuPerception.Snapshot seen, FighterInput out) {
        if (!me.canAct()) return false;
        List<CpuSpecials.Option> kit = CpuSpecials.of(me.character());
        if (kit.isEmpty()) return false;
        float gap = Math.abs(seen.x - me.x);
        int elapsed = engine.frame() - seen.frame;
        if (!me.grounded) return dive(me, target, kit, out);
        diveDecided = false;

        // Golpe corpo a corpo do rival vindo.
        if (seen.threatening() && seen.attack.melee() && seen.attackSerial != specialMeleeSerial &&
            gap <= seen.attack.reach + me.body().halfWidth + 60f) {
            specialMeleeSerial = seen.attackSerial;
            meleePlan = null;
            if (specialRandom.nextDouble() < SPECIAL_REACT_CHANCE * level.reactGuard * CpuStyle.of(me.character()).reactSpecial) {
                meleePlan = meleeAnswer(me, target, kit, seen, gap, elapsed, false);
            }
            if (meleePlan != null) lastGuardSerial = seen.attackSerial;   // não defende por cima
        }
        if (meleePlan != null) {
            if (!seen.threatening() || seen.attackSerial != specialMeleeSerial) {
                meleePlan = null;
            } else if (meleeAnswer(me, target, java.util.Collections.singletonList(meleePlan), seen, gap, elapsed, true) != null) {
                CpuSpecials.Option o = meleePlan;
                meleePlan = null;
                pressSpecial(o, out);
                guardFrames = 0;
                lastReason = "resposta (" + o.id + ")";
                return true;
            } else {
                movingForward = movingBack = dashing = false;
                out.direction = 0;
                return true;
            }
        }

        // Bola do rival vindo.
        if (seen.projectileGap != Float.MAX_VALUE && seen.projectileSerial != specialShotSerial) {
            specialShotSerial = seen.projectileSerial;
            shotPlan = null;
            // O especial contra a bola (repel ou esquiva) é um dos planos que ela aprende
            // ({@link #escolherPlanoBola}): aqui só fica guardado o que o kit tem para esta bola.
            especialContraBola = null;
            for (CpuSpecials.Option o : kit) {
                AttackDefinition.Mechanics m = o.m();
                boolean repel = o.role == CpuSpecials.Role.REPEL;
                boolean evade = m.evade == AttackDefinition.Reads.ANY ||
                    m.evade == AttackDefinition.Reads.HIGH && !seen.projectileLow;
                if ((repel || evade) && lifeAllows(me, o)) {
                    especialContraBola = o;
                    break;
                }
            }
        }
        if (shotPlan != null) {
            if (seen.projectileGap == Float.MAX_VALUE || seen.projectileSerial != specialShotSerial) {
                shotPlan = null;
                return false;
            }
            float speed = Math.max(1f, seen.projectileSpeed * CombatConfig.DT);
            float arrival = seen.projectileGapNow(me.x, elapsed) / speed;
            AttackDefinition.Mechanics m = shotPlan.m();
            int start = shotPlan.role == CpuSpecials.Role.REPEL ? m.counterStart : m.evadeStart;
            int end = shotPlan.role == CpuSpecials.Role.REPEL ? m.counterEnd : m.evadeEnd;
            if (CpuSpecials.covers(start, end, arrival, 2)) {
                CpuSpecials.Option o = shotPlan;
                shotPlan = null;
                pressSpecial(o, out);
                guardFrames = 0;
                lastReason = "contra a bola (" + o.id + ")";
                return true;
            }
            if (arrival < start) {
                shotPlan = null;   // perdeu a hora: a defesa normal cuida
                return false;
            }
            movingForward = movingBack = dashing = false;
            out.direction = 0;
            return true;
        }
        return false;
    }

    /**
     * O especial do kit que responde ao golpe que ela viu: contra-golpe que lê aquela altura,
     * esquiva que passa por ele, ou armadura que aguenta e acerta de volta. {@code now}: só se a
     * janela cobre o contato apertando já; senão também os que ainda vão caber (esperando).
     */
    private CpuSpecials.Option meleeAnswer(CombatFighter me, CombatFighter target, List<CpuSpecials.Option> kit,
                                           CpuPerception.Snapshot seen, float gap, int elapsed, boolean now) {
        AttackDefinition a = seen.attack;
        boolean air = !seen.grounded;
        // Quadros até o primeiro quadro ativo do golpe dele, contando o que passou desde o que ela viu.
        float arrival = a.startupFrames - seen.attackFrame - elapsed;
        for (CpuSpecials.Option o : kit) {
            if (!lifeAllows(me, o)) continue;
            AttackDefinition.Mechanics m = o.m();
            int start, end;
            if (o.role == CpuSpecials.Role.COUNTER && CpuSpecials.reads(m.counter, a, air)) {
                start = m.counterStart;
                end = m.counterEnd;
            } else if (m.evade != AttackDefinition.Reads.NONE &&
                (m.evade == AttackDefinition.Reads.ANY || air ||
                    (m.evade != AttackDefinition.Reads.AIR && !a.low))) {
                start = m.evadeStart;
                end = m.evadeEnd;
            } else if (o.role == CpuSpecials.Role.ARMOR && CpuSpecials.reaches(o, gap, target.body(), seen.crouching)) {
                start = m.armorStart;
                end = m.armorEnd;
            } else {
                continue;
            }
            if (CpuSpecials.covers(start, end, arrival, a.activeFrames)) return o;
            if (!now && arrival > start) return o;
        }
        return null;
    }

    /** No ar com especial de mergulho: uma vez por pulo, desce em cima do rival se cair nele. */
    private boolean dive(CombatFighter me, CombatFighter target, List<CpuSpecials.Option> kit, FighterInput out) {
        CpuSpecials.Option dive = null;
        for (CpuSpecials.Option o : kit) if (o.role == CpuSpecials.Role.DIVE) dive = o;
        // Caído ou levantando não leva golpe: mergulhar nele é cair na punição.
        if (dive == null || !target.grounded || target.status == CombatFighter.Status.KNOCKDOWN ||
            target.status == CombatFighter.Status.WAKEUP) return false;
        if (!diveDecided) {
            diveDecided = true;
            divePlan = specialRandom.nextDouble() < SPECIAL_DIVE_CHANCE * level.kitScale / 1.2;
        }
        if (!divePlan) return false;
        AttackDefinition.Mechanics m = dive.m();
        float ahead = (target.x - me.x) * me.facing;
        float height = Arena.GROUND_Y - me.y;
        float reach;
        if (m.dives()) {
            float fall = height / Math.max(1f, m.diveDescent);
            reach = m.diveForward * fall + dive.attack.reach;
        } else {
            reach = dive.reach;
        }
        if (ahead < 40f || ahead > reach + target.body().halfWidth || height < 60f) return false;
        divePlan = false;
        pressSpecial(dive, out);
        lastReason = "mergulho (" + dive.id + ")";
        return true;
    }

    /** O especial que sobe e pega o rival no ar daqui (ou null). */
    private CpuSpecials.Option risingSpecial(CombatFighter me, float gap, float height, CombatFighter target) {
        for (CpuSpecials.Option o : CpuSpecials.of(me.character())) {
            if (o.role != CpuSpecials.Role.ANTI_AIR || !lifeAllows(me, o)) continue;
            if (height < 0f || height > o.attack.hitHeight + 40f) continue;
            if (CpuSpecials.reaches(o, gap, target.body(), false)) return o;
        }
        return null;
    }

    /**
     * Decidiu algo no neutro: às vezes troca pelo especial que serve àquela distância — o
     * projétil e a armadilha de longe, o puxão e o avanço de meia distância, o agarrão de
     * especial colado (mais contra quem só defende), o baixo contra quem está em pé.
     */
    private void specialNeutral(CombatFighter me, CombatFighter target, CpuPerception.Snapshot seen) {
        if (situation.selfAirborne || !situation.attackReady || !situation.targetAlive || !seen.grounded) return;
        // Os níveis baixos quase não trocam a decisão por especial (o estilo multiplica, mas o
        // nível 1 fica num décimo do 8).
        if (specialRandom.nextDouble() >= SPECIAL_NEUTRAL_CHANCE * (0.1 + 0.9 * smart()) * CpuStyle.of(me.character()).neutralSpecial) return;
        float gap = situation.distance;
        CharacterDefinition.Body body = target.body();
        CpuSpecials.Option best = null;
        double total = 0.0;
        // O rival atacou há pouco (a pressão dele): a hora da postura e da armadura.
        boolean pressed = seen.frame - lastOpponentAttackFrame < PRESSURE_FRAMES;
        for (CpuSpecials.Option o : CpuSpecials.of(me.character())) {
            if (!lifeAllows(me, o)) continue;
            // O golpe lento no neutro só leva a punição: ele fica para punir (punish) e para o projétil.
            boolean quick = o.attack.startupFrames <= SPECIAL_NEUTRAL_MAX_STARTUP;
            double w = 0.0;
            switch (o.role) {
                case PROJECTILE:
                    if (!situation.projectileActive && gap >= 200f && gap <= o.range) w = 1.0;
                    break;
                case TRAP:
                    if (!situation.projectileActive && gap >= 140f && gap <= 480f) w = 1.0;
                    break;
                case GRAB:
                    if (gap <= me.body().halfWidth + body.halfWidth + o.m().grabRange) w = situation.rivalBlocked ? 3.0 : 1.5;
                    break;
                case PULL:
                    if (gap >= 160f && CpuSpecials.reaches(o, gap, body, seen.crouching)) w = 1.0;
                    break;
                case ECHO:
                    if (gap >= 100f && gap <= 360f) w = 1.0;
                    break;
                case COUNTER:
                    // Lendo a pressão: colado em quem acabou de atacar, espera o próximo golpe dele.
                    if (pressed && gap <= 200f) w = 1.5;
                    break;
                case RUSH:
                    if (quick && gap >= 140f && CpuSpecials.reaches(o, gap, body, seen.crouching)) w = 1.0;
                    break;
                case LOW:
                    if (quick && !seen.crouching && CpuSpecials.reaches(o, gap, body, seen.crouching)) w = 1.0;
                    break;
                case ARMOR:
                    if (CpuSpecials.reaches(o, gap, body, seen.crouching)) w = pressed ? 1.5 : quick ? 0.6 : 0.0;
                    break;
                case POKE:
                    if (quick && CpuSpecials.reaches(o, gap, body, seen.crouching)) w = 0.6;
                    break;
                default:
                    break;   // esquiva, repel, o que sobe e o mergulho: só reagindo
            }
            if (w <= 0.0) continue;
            total += w;
            if (specialRandom.nextDouble() * total < w) best = o;
        }
        if (best == null) return;
        actions.stop();
        attack = null;
        grab = superAttack = backdash = jump = superJump = false;
        special = best.id.equals("S") ? "M" : best.id;
        lastReason = "neutro (" + best.id + ")";
    }

    /**
     * Pode soltar o especial agora: o que custa vida só com vida sobrando, e o de carga só com a
     * carga feita (trás ou baixo seguro o tempo da carga), como o jogador.
     */
    private boolean lifeAllows(CombatFighter me, CpuSpecials.Option o) {
        if (o.costsLife() && me.state.life <= SPECIAL_LIFE_FLOOR * me.state.profile.maxLife) return false;
        return !o.hold || chargeConfig == null ||
            me.motion.charged(o.holdDown, me.clock, chargeConfig.chargeFrames, chargeConfig.chargeKeepFrames);
    }

    /** Aperta o especial (o S1 de mergulho vai como a energia, com o M). */
    private static void pressSpecial(CpuSpecials.Option o, FighterInput out) {
        out.special = o.id.equals("S") ? "M" : o.id;
        out.light = out.medium = out.heavy = false;
    }

    // ---------------------------------------------------------------- caderno de combos

    /** Começa a rota do caderno no passo {@code first} (ou só ele, se não estiver na rota). */
    private void startRoute(CharacterDefinition c, String first) {
        List<String> book = ComboBook.route(c);
        int index = book.indexOf(first);
        if (index >= 0) {
            route = book;
            step = index;
        } else {
            // Iniciador fora da rota (2L, 2M...): entra nela no primeiro passo que ele cancela.
            route = java.util.Collections.singletonList(first);
            AttackDefinition a = c.attack(first);
            for (int k = 1; a != null && k < book.size(); k++) {
                String b = book.get(k);
                if (b.startsWith("j") || ComboBook.JUMP.equals(b) || ComboBook.AIR_JUMP.equals(b)) break;
                if (a.cancelsInto(b)) {
                    java.util.List<String> joined = new java.util.ArrayList<>();
                    joined.add(first);
                    joined.addAll(book.subList(k, book.size()));
                    route = joined;
                    break;
                }
            }
            step = 0;
        }
        awaitingStart = true;
        routeWait = 0;
        lastSeenAttack = null;
        stepDecided = commitDecided = false;
        ownHitFrame = -1;
    }

    private void endRoute() {
        route = null;
        routeCut = false;
        step = 0;
        awaitingStart = false;
        lastSeenAttack = null;
    }

    /** O golpe na tela é o passo {@code name} do caderno. */
    private static boolean matches(AttackDefinition attack, String name) {
        return attack != null && attack.id.equals(name);
    }

    /**
     * Combo em andamento: no golpe atual, aperta o próximo passo. Golpes comuns seguem às cegas
     * (no acerto ou na defesa, como um jogador encadeia); lançador, especial, Super e o super pulo
     * só depois de ver que acertou, com a reação do nível.
     */
    private void runCombo(CombatEngine engine, CombatFighter me, FighterInput out) {
        runCombo(engine, me, engine.fighter(1 - me.index), out);
    }

    private void runCombo(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        if (route == null) {
            // Golpe fora do caderno (S, Super, kit): só persegue o lançador que viu acertar.
            if (me.attack.launch == AttackDefinition.Launch.LAUNCH && hitSeen(engine) && roll(level.confirm)) {
                startRoute(me.character(), me.attack.id);
                awaitingStart = false;
                lastSeenAttack = me.attack;
                lastSeenAttackFrame = me.attackFrame;
            } else if (soltoLigado && me.grounded && me.attack.kind == AttackDefinition.Kind.NORMAL &&
                me.outcome == CombatFighter.Outcome.HIT) {
                // Golpe de chão solto (cutucada, pressão, reação) que acertou: segue no combo dele,
                // com a chance de confirmar do nível (uma decisão por golpe). Antes só o golpe que
                // já saía como começo de combo seguia: o M e o 2L soltos paravam no primeiro acerto.
                int inicio = engine.frame() - me.attackFrame;
                if (inicio != soltoDecidido) {
                    soltoDecidido = inicio;
                    if (roll(level.confirm)) {
                        int acerto = ownHitFrame;
                        startRoute(me.character(), me.attack.id);
                        ownHitFrame = acerto;
                        awaitingStart = false;
                        lastSeenAttack = me.attack;
                        lastSeenAttackFrame = me.attackFrame;
                    }
                }
            }
            return;
        }
        boolean fresh = me.attack != lastSeenAttack || me.attackFrame < lastSeenAttackFrame;
        lastSeenAttack = me.attack;
        lastSeenAttackFrame = me.attackFrame;
        routeCut = me.hitCutFrame >= 0;
        if (fresh) {
            if (awaitingStart && matches(me.attack, route.get(step))) {
                awaitingStart = false;
            } else if (!awaitingStart && step + 1 < route.size() && matches(me.attack, route.get(step + 1))) {
                step++;
                stepDecided = commitDecided = false;
            } else if (!(awaitingStart || matches(me.attack, route.get(step)) && step == 0)) {
                endRoute();
                return;
            }
        }
        if (awaitingStart) return;
        if (step + 1 >= route.size()) {
            // A rota acabou num golpe comum (um H solto que acertou, o fim de uma rota curta):
            // fecha cancelando no Super ou no especial, se o golpe deixar.
            String ender = ender(engine, me);
            if (ender == null) return;
            java.util.List<String> longer = new java.util.ArrayList<>(route);
            longer.add(ender);
            route = longer;
        }
        String next = route.get(step + 1);
        if (me.outcome == CombatFighter.Outcome.NONE) return;   // ainda não encostou
        if (!stepDecided) {
            stepDecided = true;
            stepGo = step + 1 < level.comboDepth && !roll(level.drop);
        }
        if (!stepGo) return;
        if (ComboBook.commits(next) || ComboBook.AIR_JUMP.equals(next)) {
            if (!hitSeen(engine) && !cancelaNoContato(me.character(), next)) return;
            if (!commitDecided) {
                commitDecided = true;
                commitGo = roll(level.confirm);
                // Com as 3 barras e o combo confirmado no chão: em vez do passo arriscado, deixa o
                // golpe terminar e emenda o Ultra com o rival ainda atordoado (ultraLink).
                if (commitGo && fullKit && me.grounded && !next.startsWith("j") && ultraLinks(engine, me, target) &&
                    me.state.superMeter >= CombatConfig.ULTRA_COST && roll(smart() * ULTRA_LINK_CHANCE)) {
                    ultraLinkSession = engine.session(target.index);
                    ultraLinkPlan = true;
                    commitGo = false;
                }
            }
            if (!commitGo) return;
        }
        if ("SUPER".equals(next) && !spendSuper(me, target)) {
            // Sem barra: fecha com o especial, se o golpe deixar.
            if (me.attack.cancelsInto("S") && me.state.profile.energy != null && !engine.energyProjectileAlive(me.index)) {
                next = "S";
            } else {
                return;
            }
        }
        if (ComboBook.JUMP.equals(next)) return;   // o super pulo sai do neutro, depois do lançador
        if (me.grounded && !next.startsWith("j") && !"S".equals(next) && !"SUPER".equals(next) &&
            !alcanca(me.character(), next, projectedGap(me, target), target)) {
            // O empurrão do golpe tirou o próximo do alcance: fecha com o que ainda alcança.
            next = closer(engine, me, target);
            if (next == null) return;
        }
        if (ComboBook.AIR_JUMP.equals(next)) {
            // ↑ no quadro em que a janela está aberta (a entrada do ↑ é o que conta).
            if (CancelSystem.windowOpen(me)) {
                out.direction = 7;
                step++;
                stepDecided = commitDecided = false;
            }
            return;
        }
        press(next, me, out);
    }

    /**
     * Passos do combo fora de um golpe: o super pulo atrás do lançador e o primeiro golpe aéreo
     * depois dele (ou depois do pulo no ar). Verdadeiro enquanto o combo segue.
     */
    private boolean runComboFromNeutral(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        if (++routeWait > 45) {
            endRoute();
            return false;
        }
        if (awaitingStart) return routeWait <= 8;   // o golpe apertado ainda vai sair
        if (step + 1 >= route.size()) {
            endRoute();
            return false;
        }
        String next = route.get(step + 1);
        String current = route.get(step);
        if (ComboBook.JUMP.equals(next)) {
            // O lançador acabou: super pulo atrás dele, se viu que acertou.
            if (!me.grounded) {
                step++;
                routeWait = 0;
                return true;
            }
            if (me.launcherChase <= 0 || !hitSeen(engine)) {
                endRoute();
                return false;
            }
            out.superJump = true;
            return true;
        }
        if (ComboBook.JUMP.equals(current) || ComboBook.AIR_JUMP.equals(current)) {
            if (me.grounded) {
                endRoute();
                return false;
            }
            // No ar: o primeiro golpe aéreo quando o rival estiver ao alcance.
            float gap = Math.abs(target.x - me.x);
            if (Math.abs(target.y - me.y) <= 115f &&
                CombatRules.reaches(me.character(), next.startsWith("j") ? next.substring(1) : next, true, gap, target.body())) {
                awaitingStart = true;
                step++;
                routeWait = 0;
                press(next, me, out);
            } else if (gap > 80f) {
                out.direction = target.x > me.x ? 1 : 5;   // vai atrás dele no ar
            }
            return true;
        }
        if ((routeCut || cortePeloShinkai) && linkAfterCut(engine, me, target, next, out)) {
            cortePeloShinkai = false;
            return true;
        }
        cortePeloShinkai = false;
        endRoute();
        return false;
    }

    /**
     * O golpe dela acertou e a recuperação foi cortada antes de ela apertar o próximo passo: no
     * chão, com o rival ainda atordoado, ele sai do neutro como link (a vantagem é a mesma que no
     * fim da recuperação inteira). Mesmas decisões do passo dentro do golpe.
     */
    private boolean linkAfterCut(CombatEngine engine, CombatFighter me, CombatFighter target, String next, FighterInput out) {
        if (!me.grounded || !target.inHitstun() || next.startsWith("j") || ComboBook.AIR_JUMP.equals(next)) return false;
        if (!stepDecided) {
            stepDecided = true;
            stepGo = step + 1 < level.comboDepth && !roll(level.drop);
        }
        if (!stepGo) return false;
        if (ComboBook.commits(next)) {
            if (!hitSeen(engine)) return true;   // ainda não viu o acerto: espera
            if (!commitDecided) {
                commitDecided = true;
                commitGo = roll(level.confirm);
            }
            if (!commitGo) return false;
        }
        if ("SUPER".equals(next) && !spendSuper(me, target)) {
            if (me.state.profile.energy == null || engine.energyProjectileAlive(me.index)) return false;
            next = "S";
        }
        if (!"S".equals(next) && !"SUPER".equals(next) &&
            !alcanca(me.character(), next, projectedGap(me, target), target)) return false;
        press(next, me, out);
        return true;
    }

    /**
     * O próximo passo não alcança mais: troca o resto da rota por um fechamento que o golpe atual
     * cancela e que alcança daqui (H, ou o S1, que voa). Null se não houver.
     */
    private String closer(CombatEngine engine, CombatFighter me, CombatFighter target) {
        CharacterDefinition c = me.character();
        float gap = projectedGap(me, target);
        for (String option : CLOSER) {
            if (!me.attack.cancelsInto(option) || option.equals(route.get(step + 1))) continue;
            AttackDefinition a = c.attack(option);
            if (a == null || a.maxUsesPerChain > 0 && me.chain.contains(option)) continue;
            // O que compromete (lançador, especial, Super) só depois de ver que acertou; a bola
            // sai no contato (cancelaNoContato).
            if (ComboBook.commits(option) && !hitSeen(engine) && !cancelaNoContato(c, option)) continue;
            boolean reaches;
            if ("S".equals(option)) {
                reaches = me.state.profile.energy != null && !engine.energyProjectileAlive(me.index) &&
                    (!a.melee() || alcanca(c, option, gap, target));
            } else if ("SUPER".equals(option)) {
                reaches = me.state.canPaySuper() && c.fighter.hasSuperAttack();
            } else {
                reaches = alcanca(c, option, gap, target);
            }
            if (!reaches) continue;
            java.util.List<String> rest = new java.util.ArrayList<>(route.subList(0, step + 1));
            rest.add(option);
            List<String> book = ComboBook.route(c);
            int at = book.indexOf(option);
            if (at >= 0 && !"S".equals(option) && !"SUPER".equals(option)) rest.addAll(book.subList(at + 1, book.size()));
            route = rest;
            return option;
        }
        return null;
    }

    /** O golpe no chão alcança o rival dessa distância (golpe sem alcance no pacote: não). */
    private static boolean alcanca(CharacterDefinition c, String option, float gap, CombatFighter target) {
        return c.move(option, false) != null &&
            CpuReach.reaches(c, option, false, gap, target.body(), target.crouchingBody());
    }

    /** O que tentar quando o próximo passo não alcança: o lançador, os fortes, os médios e o fecho. */
    private static final String[] CLOSER = {"2H", "H", "M", "2M", "SUPER", "S"};

    /**
     * Distância até o rival depois do empurrão que ainda vai acontecer: o golpe seguinte sai
     * com ele já longe (um 2L na ponta empurra o rival para fora do alcance do M).
     */
    static float projectedGap(CombatFighter me, CombatFighter target) {
        return Math.abs(target.x - me.x) + Math.abs(target.pushRemaining) + Math.abs(me.pushRemaining);
    }

    /** Fecho de uma rota que acabou num golpe comum no chão: o Super (com barra) ou o especial. */
    private String ender(CombatEngine engine, CombatFighter me) {
        AttackDefinition a = me.attack;
        if (a == null || !me.grounded || a.kind != AttackDefinition.Kind.NORMAL) return null;
        if (a.cancelsInto("SUPER") && spendSuper(me, engine.fighter(1 - me.index))) return "SUPER";
        if (a.cancelsInto("S") && me.state.profile.energy != null && !engine.energyProjectileAlive(me.index)) return "S";
        return null;
    }

    /** Viu (com a reação do nível) que o golpe atual acertou. */
    private boolean hitSeen(CombatEngine engine) {
        return ownHitFrame >= 0 && engine.frame() - ownHitFrame >= reacao;
    }

    /** Aperta o passo {@code step}: golpe ("2H" agachado, "j2H" ↓ no ar), S1 ou Super. */
    private static void press(String step, CombatFighter me, FighterInput out) {
        if ("SUPER".equals(step)) {
            out.superAttack = true;
            return;
        }
        if ("S".equals(step)) {
            out.special = "M";
            return;
        }
        // Agachado com ↘ (para baixo e para frente): conta como agachado e não fecha comando de
        // especial com a defesa que veio antes (← ↙ ↓ + H vira o S3).
        if (ComboBook.crouching(step)) out.direction = me.facing > 0 ? 2 : 4;
        int forward = me.facing > 0 ? 1 : 5;
        if (GolpeAlto.BINDING.equals(step)) out.direction = forward;   // → + M: o golpe alto
        else if (out.direction == forward) out.direction = 0;          // M andando não vira o 6M
        char button = ComboBook.button(step);
        out.light = button == 'L';
        out.medium = button == 'M';
        out.heavy = button == 'H';
    }

    // ---------------------------------------------------------------- kit da batalha

    /** PROTÓTIPO PERCEPCAO: o quanto a percepção soma na chance da defesa perfeita (vezes 0,5 + smart). */
    static final double PERCEPCAO_LEITURA = 0.35;
    /** PROTÓTIPO PERCEPCAO: a reação no SHINKAI do Ryūga (60% da do nível, nunca abaixo disto). */
    static final int PERCEPCAO_REACAO_MIN = 6;
    /**
     * Para as baterias (BateriaRitmo): a reação em quadros vezes isto. Simula uma pessoa com o
     * mesmo tempo de reação em milissegundos num relógio de luta mais lento (95%: 15 → 14 quadros).
     */
    float escalaReacao = 1f;
    /** Para as baterias: desliga a reação mais rápida (mede a assinatura sozinha). */
    static boolean percepcaoReage = true;
    /** A reação deste quadro (a do nível, ou a da percepção). */
    private int reacao = 13;
    /** No limite, decidiu uma vez se rompe (a janela é curta). */
    private boolean decidiuLimite;

    /**
     * PROTÓTIPO PERCEPCAO (o Ryūga): no limite do corpo escolhe a Ruptura (para fechar a luta, ou
     * perdendo feio) ou deixa acabar; antes do limite, com o corpo quase no fim e nada para
     * aproveitar, os níveis altos desligam para não ficar exaustos.
     */
    private boolean escolhaPercepcao(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        FighterState s = me.state;
        if (!s.inShinkai() || s.shinkaiType != Shinkai.Type.PERCEPCAO && !engine.porReserva(s)) {
            decidiuLimite = false;
            return false;
        }
        if (s.limiteQuadros > 0) {
            // Um tempo de reação antes de decidir.
            if (decidiuLimite || s.limiteQuadros > engine.config.ryugaLimiteJanela - 12) return false;
            decidiuLimite = true;
            float minha = s.life / (float)s.profile.maxLife;
            float dele = target.state.life / (float)target.state.profile.maxLife;
            double p = dele < 0.3f ? 0.45 + 0.4 * smart() : minha < dele - 0.25f ? 0.4 : 0.1;
            if (chance(p)) {
                out.shinkai = true;
                return true;
            }
            return false;
        }
        decidiuLimite = false;
        if (engine.porReserva(s)) return escolhaNaReserva(engine, me, target, out);
        if (s.rupturaQuadros > 0 || !me.canAct() || me.attacking()) return false;
        if (s.sobrecarga < CombatConfig.RYUGA_SOBRECARGA_MAX * 8 / 10 || engine.frame() % KIT_DECISION_FRAMES != 0) return false;
        if (Math.abs(target.x - me.x) < 300f || target.state.life < target.state.profile.maxLife * 3 / 10) return false;
        if (chance(0.3 * smart())) {
            out.shinkai = true;
            return true;
        }
        return false;
    }

    /**
     * SHINKAI por reserva (sem cronômetro), fora do limite: na Ruptura a vida dele paga o poder,
     * então ela desliga quando a vida dela fica baixa e o rival ainda não está no fim, ou quando a
     * Ruptura já durou e não fechou. Com a reserva baixa (antes do limite), longe e sem nada para
     * fechar, os níveis altos desligam para guardar parte dela na barra.
     */
    private boolean escolhaNaReserva(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        FighterState s = me.state;
        if (!me.canAct() || me.attacking() || engine.frame() % KIT_DECISION_FRAMES != 0) return false;
        float minha = s.life / (float)s.profile.maxLife;
        float dele = target.state.life / (float)target.state.profile.maxLife;
        if (s.rupturaQuadros > 0) {
            boolean fechando = dele < 0.2f;
            boolean sangrando = minha < 0.3f || s.rupturaQuadros > RUPTURA_LONGA && dele > minha;
            if (!fechando && sangrando && chance(0.4 + 0.5 * smart())) {
                out.shinkai = true;
                return true;
            }
            return false;
        }
        if (s.reserva > s.reservaMax * 4 / 10) return false;
        if (Math.abs(target.x - me.x) < 300f || dele < 0.3f) return false;
        if (chance(0.2 * smart())) {
            out.shinkai = true;
            return true;
        }
        return false;
    }

    /** Ruptura por reserva que já durou isto (quadros) e não fechou a luta: hora de pensar em sair. */
    static final int RUPTURA_LONGA = 360;

    /**
     * O kit da batalha neste frame. Verdadeiro quando apertou algo que pede o frame só para
     * si (a troca, o Ultra e o SHINKAI no neutro): a decisão normal fica para depois.
     */
    private boolean fillKit(CombatEngine engine, CombatFighter me, CombatFighter target,
                            CpuPerception.Snapshot seen, FighterInput out) {
        int self = me.index;
        TeamSystem teams = engine.teams;
        TeamSystem.Side team = engine.team(self);
        if (escolhaPercepcao(engine, me, target, out)) return true;

        // Assist em campo: às vezes vira o novo titular (Assist → Tag).
        boolean assistOut = team.assistOut();
        if (assistOut && !wasAssistOut) convertPlan = chance(CONVERT_CHANCE);
        wasAssistOut = assistOut;
        if (convertPlan && team.conversionOpen()) {
            out.assist = true;
            convertPlan = false;
        }

        // Defendendo: Guard Cancel Tag (o parceiro entra batendo) ou pushblock.
        boolean blocking = me.status == CombatFighter.Status.BLOCKSTUN;
        if (blocking && !wasBlocking) {
            // Guarda quase quebrando: tenta sair da pressão com mais vontade.
            boolean low = guardLow(engine, me);
            guardCancelPlan = chance(low ? GUARD_CANCEL_LOW_GUARD_CHANCE : GUARD_CANCEL_CHANCE);
            // O pushblock gasta barra: os níveis altos só usam com a guarda perto de quebrar.
            pushblockPlan = chance(low ? PUSHBLOCK_LOW_GUARD_CHANCE : PUSHBLOCK_CHANCE * (1.0 - 0.8 * smart()));
        }
        wasBlocking = blocking;
        if (blocking) {
            if (guardCancelPlan && teams.guardCancelReady(self, me)) {
                out.assist = true;
                guardCancelPlan = false;
            } else if (pushblockPlan && me.state.superMeter >= engine.config.pushblockCost) {
                out.pushblock = true;
                pushblockPlan = false;
            }
            return false;
        }

        // O próprio golpe: um sorteio por golpe novo, usado quando ela vê que ele acertou.
        if (me.attacking()) {
            if (me.attack != plannedAttack || me.attackFrame < plannedAttackFrame) {
                plannedAttack = me.attack;
                assistPlan = chance(ASSIST_COMBO_CHANCE);
                dhcPlan = chance(DHC_CHANCE);
                // Os níveis altos usam o SHINKAI para esticar o combo que acertou.
                shinkaiPlan = chance(SHINKAI_COMBO_CHANCE * (0.5 + 1.5 * smart()));
            }
            plannedAttackFrame = me.attackFrame;
            if (teams.dhcReady(self, me)) {
                if (dhcPlan) out.assist = true;
            } else if (hitSeen(engine) && me.attack.kind != AttackDefinition.Kind.SUPER) {
                // O SHINKAI corta o golpe que acertou (não o próximo, ainda saindo: ele sumia e o
                // combo morria ali) e a rota emenda o passo seguinte (linkAfterCut).
                if (shinkaiPlan && teams.canShinkai(me) && (!shinkaiNoAcerto || me.outcome == CombatFighter.Outcome.HIT)) {
                    out.shinkai = true;
                    shinkaiPlan = false;
                    if (shinkaiNoAcerto && route != null) cortePeloShinkai = true;
                } else if (assistPlan && teams.canAssist(self, me)) {
                    out.assist = true;
                    assistPlan = false;
                }
            }
            return false;
        }
        plannedAttack = null;

        // Neutro: de tempos em tempos olha o que o time e a barra permitem.
        if (!me.canAct() || ++kitTimer < KIT_DECISION_FRAMES) return false;
        kitTimer = 0;
        float distance = Math.abs((seen != null ? seen.x : target.x) - me.x);
        float life = me.state.life / (float)me.state.profile.maxLife;
        FighterState partner = team.partner();
        if (me.grounded && target.grounded && distance <= ULTRA_REACH &&
            me.state.superMeter >= CombatConfig.ULTRA_COST && chance(ULTRA_CHANCE * (1.0 - smart()))) {
            out.ultra = true;
            return true;
        }
        if (team.partnerStanding() && !team.tagging() && team.tagCooldown == 0 && !assistOut &&
            life < TAG_LIFE_RATIO && partner.life / (float)partner.profile.maxLife > life + 0.15f &&
            chance(TAG_CHANCE)) {
            out.tag = true;
            return true;
        }
        if (target.state.inShinkai() && teams.canShinkai(me) && chance(SHINKAI_ANSWER_CHANCE * (0.5 + smart()))) {
            // O rival ligou o SHINKAI: responder com o dela vira o confronto (SHINKAI Clash).
            out.shinkai = true;
            return true;
        }
        if (life < 0.6f && teams.canShinkai(me) &&
            chance(SHINKAI_LOW_LIFE_CHANCE * (1.0 - 0.7 * smart()))) {
            out.shinkai = true;
            return true;
        }
        if (distance >= 150f && distance <= 700f && teams.canAssist(self, me) && chance(ASSIST_NEUTRAL_CHANCE)) {
            // O assist vem junto com o que a decisão normal fizer (andar, energia).
            out.assist = true;
        }
        return false;
    }

    // ------------------------------------------------- contra quem fica soltando bola

    private void noteRivalShot(int frame) {
        rivalShots[rivalShotCount % ZONING_SHOTS] = frame;
        rivalShotCount++;
    }

    /** O rival soltou várias bolas há pouco: está zoneando (ela vai atrás dele). */
    boolean rivalZoning(int frame) {
        if (rivalShotCount < ZONING_SHOTS) return false;
        for (int shot : rivalShots) if (frame - shot > ZONING_MEMORY) return false;
        return true;
    }

    // ---------------------------------------------------------------- recursos com cabeça

    /** Quanto ela usa os recursos com cabeça: 0 no nível 1, 1 no 8 ({@link OpponentAi#smart}). */
    private double smart() {
        return OpponentAi.smart(level);
    }

    /**
     * Gastar a barra no Super que fecha o combo confirmado? Os níveis baixos gastam sempre que
     * têm; os altos só não quebram a reserva das 3 barras do Ultra (a não ser que o Super mate).
     */
    private boolean spendSuper(CombatFighter me, CombatFighter target) {
        int meter = me.state.superMeter;
        if (!me.state.canPaySuper() || !me.character().fighter.hasSuperAttack()) return false;
        // O Super grátis do SHINKAI: não gasta a reserva, sai sempre.
        if (meter < CombatConfig.SUPER_COST) return true;
        if (smart() < 0.5 || !fullKit) return true;
        AttackDefinition sup = me.character().attack("SUPER");
        boolean kills = sup != null && sup.damage * KILL_SCALE >= target.state.life;
        boolean keepsReserve = meter < CombatConfig.ULTRA_COST || meter - CombatConfig.SUPER_COST >= CombatConfig.ULTRA_COST;
        return kills || keepsReserve;
    }

    /**
     * O combo dela acabou e o rival ainda está atordoado (no chão, perto): emenda o Ultra (a
     * preparação congela o rival e o avanço chega com ele ainda preso). Um sorteio por combo.
     */
    private boolean ultraLink(CombatEngine engine, CombatFighter me, CombatFighter target, FighterInput out) {
        ComboSession session = engine.session(target.index);
        if (session == null || session.attacker != me.index || session.hitCount < 2) {
            ultraLinkSession = null;
            return false;
        }
        // No fim da recuperação do golpe dela já aperta (o jogo guarda o comando e o Ultra sai no
        // primeiro quadro livre, com o rival ainda atordoado).
        boolean ending = me.attacking() && me.attack.totalFrames - me.attackFrame < engine.config.bufferFrames;
        if (!(me.canAct() || ending) || !me.grounded || !target.grounded || !target.inHitstun() ||
            me.state.superMeter < CombatConfig.ULTRA_COST || Math.abs(target.x - me.x) > ULTRA_REACH) return false;
        if (session != ultraLinkSession) {
            ultraLinkSession = session;
            ultraLinkPlan = roll(smart() * ULTRA_LINK_CHANCE);
        }
        if (!ultraLinkPlan) return false;
        ultraLinkPlan = false;
        endRoute();
        out.ultra = true;
        lastReason = "ultra no combo";
        return true;
    }

    private ComboSession ultraLinkSession;
    private boolean ultraLinkPlan;

    /**
     * O golpe dela deixa o rival atordoado até ela poder apertar o Ultra (o fim da recuperação,
     * com o comando guardado)? É o frame data do golpe (vantagem no acerto), que o jogador que
     * conhece o personagem sabe.
     */
    private static boolean ultraLinks(CombatEngine engine, CombatFighter me, CombatFighter target) {
        if (!me.attacking() || !target.inHitstun()) return false;
        int free = me.attack.totalFrames - me.attackFrame - engine.config.bufferFrames;
        return target.stunLeft > Math.max(0, free);
    }

    /** Pune com o Ultra (com a chance do nível): o rival preso por pelo menos {@code left} quadros. */
    private boolean ultraPunish(CombatFighter me, CombatFighter target, int left, float gap, FighterInput out) {
        if (!fullKit || left < ULTRA_PUNISH_MIN || me.state.superMeter < CombatConfig.ULTRA_COST || !me.grounded ||
            gap > ULTRA_REACH || !roll(smart() * ULTRA_PUNISH_CHANCE)) return false;
        endRoute();
        guardFrames = 0;
        out.ultra = true;
        lastReason = "punir com ultra";
        return true;
    }

    /** Sorteio do kit, na escala do nível (o nível 1 não usa o kit). */
    private boolean chance(double p) {
        return kitRandom.nextDouble() < p * level.kitScale;
    }

    private boolean roll(double p) {
        return reactRandom.nextDouble() < p;
    }

    private void clearRequests() {
        attack = null;
        special = null;
        superAttack = backdash = jump = superJump = grab = false;
    }
}
