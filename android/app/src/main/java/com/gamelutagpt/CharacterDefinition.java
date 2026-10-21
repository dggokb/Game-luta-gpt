package com.gamelutagpt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable runtime data compiled from characters/<id>/character.json. */
final class CharacterDefinition {
    static final class Atlas {
        final String resource;
        /** Logical cell (the generator's packed cell): size, root and grid of frames. */
        final int width, height, rootX, rootY, columns, count;
        /** Texture pixels per canonical art pixel; gameplay/profile units stay unchanged. */
        final float pixelScale;
        /**
         * Size of the shipped image. Trimmed sheets keep only the box around each frame's
         * drawing (tools/sprites/folhas.py); without a frame table the image is the grid.
         */
        final int sheetWidth, sheetHeight;
        /** Per frame: sheetX, sheetY, width, height, cellX, cellY (width 0 = empty), or null. */
        private final int[] frames;
        Atlas(String resource,int width,int height,int rootX,int rootY,int columns,int count) {
            this(resource,width,height,rootX,rootY,columns,count,1f);
        }
        Atlas(String resource,int width,int height,int rootX,int rootY,int columns,int count,float pixelScale) {
            this(resource,width,height,rootX,rootY,columns,count,pixelScale,
                columns*width,((count+columns-1)/columns)*height,null);
        }
        Atlas(String resource,int width,int height,int rootX,int rootY,int columns,int count,float pixelScale,
              int sheetWidth,int sheetHeight,String frames) {
            if(!Float.isFinite(pixelScale)||pixelScale<=0f)throw new IllegalArgumentException("Invalid atlas pixelScale");
            this.resource=resource;this.width=width;this.height=height;
            this.rootX=rootX;this.rootY=rootY;this.columns=columns;this.count=count;
            this.pixelScale=pixelScale;this.sheetWidth=sheetWidth;this.sheetHeight=sheetHeight;
            this.frames=frames==null?null:parseFrames(resource,frames,count,sheetWidth,sheetHeight);
        }
        private static int[] parseFrames(String resource,String text,int count,int sheetWidth,int sheetHeight) {
            String[] parts=text.split(",");
            if(parts.length!=count*6)throw new IllegalArgumentException("Invalid frame table: "+resource);
            int[] out=new int[parts.length];
            for(int i=0;i<parts.length;i++)out[i]=Integer.parseInt(parts[i]);
            for(int i=0;i<count;i++) {
                int x=out[i*6],y=out[i*6+1],w=out[i*6+2],h=out[i*6+3];
                if(w<0||h<0||x<0||y<0||x+w>sheetWidth||y+h>sheetHeight)
                    throw new IllegalArgumentException("Frame outside the sheet: "+resource+" #"+i);
            }
            return out;
        }
        /**
         * Where frame {@code frame} is: {@code out} = {sheetX, sheetY, width, height, cellX,
         * cellY}, the last two inside the logical cell. Returns false for an empty frame.
         */
        boolean frame(int frame,int[] out) {
            if(frames==null) {
                out[0]=frame%columns*width;out[1]=frame/columns*height;out[2]=width;out[3]=height;out[4]=0;out[5]=0;
                return true;
            }
            System.arraycopy(frames,frame*6,out,0,6);
            return out[2]>0&&out[3]>0;
        }
    }
    static final class Animation {
        final String id;
        final Atlas atlas;
        private final int[] frames;
        private final float[] durations;
        final boolean loop;
        final float distancePerFrame, duration;
        Animation(String id,Atlas atlas,int[] frames,float[] durations,boolean loop,float distancePerFrame) {
            this.id=id;this.atlas=atlas;this.frames=frames.clone();this.durations=durations.clone();
            this.loop=loop;this.distancePerFrame=distancePerFrame;
            float sum=0;for(float d:durations)sum+=d;this.duration=sum;
        }
        int frame(float time,float distance) {
            if(distancePerFrame>0) return frames[(int)(Math.max(0,distance)/distancePerFrame)%frames.length];
            float t=Math.max(0,time);
            if(loop)t%=duration;
            for(int i=0;i<frames.length-1;i++) { if(t<durations[i])return frames[i];t-=durations[i]; }
            return frames[frames.length-1];
        }
        /**
         * Guard clips: the first frame is the held guard; the remaining frames are the
         * block reaction, stretched over the blockstun. progress is 0..1 of the blockstun.
         */
        float guardTime(boolean blocking,float progress) {
            if(!blocking||durations.length<2)return 0f;
            float hold=durations[0];
            return hold+Math.max(0f,Math.min(1f,progress))*(duration-hold-0.0001f);
        }
        /** Clip time that shows the given frame of the clip (its middle). */
        float timeOfFrame(int index) {
            int i=Math.max(0,Math.min(durations.length-1,index));
            float t=durations[i]*0.5f;
            for(int k=0;k<i;k++)t+=durations[k];
            return t;
        }
        /** Maps a gameplay clock of the given length onto this clip, keeping frame proportions. */
        float timeFor(float elapsed,float gameplayDuration) {
            if(gameplayDuration<=0f)return elapsed;
            return Math.max(0f,elapsed)*duration/gameplayDuration;
        }
    }
    /**
     * One input: its combat definition plus what the body shows. Gameplay timing comes
     * from the attack's frames; the animation, when present, is stretched to fit it. A move
     * without art keeps a declared body pose instead.
     */
    static final class Move {
        final String binding;
        final Animation animation;
        final String pose;
        final AttackDefinition attack;
        final int damage;
        /** Seconds, derived from the attack frames (animation stretch, AI, legacy checks). */
        final float totalTime,activeStart,activeEnd,reach;
        /**
         * Tempo do clipe por trechos (quadros do golpe → segundos do clipe, crescentes), ou null:
         * o clipe esticado inteiro. O golpe alto usa para segurar a pose de preparo em vez de
         * passar o M inteiro em câmera lenta.
         */
        private final float[] knotFrames,knotTimes;
        Move(String binding,Animation animation,String pose,AttackDefinition attack) {
            this(binding,animation,pose,attack,null,null);
        }
        Move(String binding,Animation animation,String pose,AttackDefinition attack,float[] knotFrames,float[] knotTimes) {
            if(knotFrames!=null && (knotTimes==null || knotTimes.length!=knotFrames.length || knotFrames.length<2))
                throw new IllegalArgumentException(binding+": trechos do clipe inválidos");
            this.knotFrames=knotFrames;this.knotTimes=knotTimes;
            this.binding=binding;this.animation=animation;this.pose=pose;this.attack=attack;
            this.damage=attack.damage;
            this.totalTime=attack.totalFrames/(float)CombatConfig.FPS;
            this.activeStart=attack.startupFrames/(float)CombatConfig.FPS;
            this.activeEnd=(attack.startupFrames+attack.activeFrames)/(float)CombatConfig.FPS;
            this.reach=attack.reach;
        }
        boolean active(float elapsed) { return elapsed+0.000001f>=activeStart && elapsed<activeEnd; }
        /** Window where a defender sees the strike coming (used by anticipated guard). */
        boolean threatening(float elapsed) {
            float lead=0.16f*totalTime;
            return elapsed>=activeStart-lead && elapsed<=activeEnd+lead;
        }
        float animationTime(float elapsed) {
            if(animation==null)return 0f;
            if(knotFrames==null)return animation.timeFor(elapsed,totalTime);
            float f=Math.max(0f,elapsed)*CombatConfig.FPS;
            int n=knotFrames.length;
            if(f<=knotFrames[0])return knotTimes[0];
            for(int i=1;i<n;i++){
                if(f<=knotFrames[i]){
                    float k=(f-knotFrames[i-1])/Math.max(0.0001f,knotFrames[i]-knotFrames[i-1]);
                    return knotTimes[i-1]+(knotTimes[i]-knotTimes[i-1])*k;
                }
            }
            return knotTimes[n-1];
        }
    }
    /**
     * Command special (S2, S3...): a ground move started by a motion plus one of its
     * buttons. Relative directions, as the energy command: 1 forward, 3 down, 5 back.
     */
    static final class Special {
        final Move move;
        private final int[] command;
        private final int[] steps;
        /** Buttons that fire it ("LMH" = any). */
        final String buttons;
        /** Nome do golpe na lista de comandos (null: sem nome). */
        final String name;
        /** Comando de segurar: [5, 1] = [←] →, [3, 7] = [↓] ↑. */
        final boolean hold;
        /** Só sai no ar (e não no chão). */
        final boolean air;
        Special(Move move,int[] command,String buttons) {
            this(move,command,buttons,null,false,false);
        }
        Special(Move move,int[] command,String buttons,String name,boolean hold,boolean air) {
            this.move=move;this.command=command.clone();this.buttons=buttons;
            this.name=name;this.hold=hold;this.air=air;
            this.steps=MotionParser.requiredSteps(command);
        }
        /** Projétil que o especial lança no fim da preparação (null: golpe corpo a corpo). */
        Projectile projectile;
        /** Efeito desenhado durante o golpe ("VAPOR"); null: nenhum. */
        String effect;
        /**
         * O especial lança um projétil: desenho (ProjetilVisual.Tipo), alcance e velocidade na
         * horizontal, saída (frente, altura) e velocidade vertical (negativa sobe, positiva desce).
         */
        Special withProjectile(String visual,float range,float speed,float spawnX,float spawnY,float vertical) {
            projectile=new Projectile(move.attack,range,speed,spawnX,spawnY,spawnY,spawnY);
            projectile.visual=visual;projectile.vertical=vertical;
            return this;
        }
        /** Ao cair no chão, o projétil vira uma poça de veneno: quadros, meia largura e o veneno de quem pisa. */
        Special withPuddle(int frames,float halfWidth,int damage,int poisonFrames) {
            projectile.puddleFrames=frames;projectile.puddleHalfWidth=halfWidth;
            projectile.puddleDamage=damage;projectile.puddlePoisonFrames=poisonFrames;
            return this;
        }
        Special withEffect(String name) { effect=name; return this; }
        int commandLength() { return command.length; }
        /** The motion, in relative directions (tutorial text and examples). */
        int[] command() { return command.clone(); }
        boolean acceptsButton(String button) { return buttons.contains(button); }
        boolean matches(MotionParser motion,int clock,int motionWindow,int pressWindow) {
            return motion.matchCommand(command,clock,motionWindow,pressWindow);
        }
    }
    static final class Projectile {
        final int damage;
        final float range,speed;
        /** Launch point: forward offset from the root and heights above the ground. */
        final float spawnX,spawnY,crouchSpawnY,airSpawnY;
        /** The S/SUPER move that throws it; its stun data applies when the projectile hits. */
        final AttackDefinition attack;
        Projectile(AttackDefinition attack,float range,float speed,float spawnX,float spawnY,float crouchSpawnY,float airSpawnY) {
            this.attack=attack;this.damage=attack.damage;this.range=range;this.speed=speed;
            this.spawnX=spawnX;this.spawnY=spawnY;this.crouchSpawnY=crouchSpawnY;this.airSpawnY=airSpawnY;
        }
        /** Desenho e velocidade vertical (projétil de especial; o S1 usa Fighter.energyVisual). */
        String visual;
        float vertical;
        /** Acertos (vários: para no rival entre eles) e o intervalo, em quadros. */
        int hits=1,hitInterval;
        /** Clones de sombra que repetem o Super em volta do rival (p06); só desenho. */
        int clones;
        Projectile withVisual(String name) { visual=name; return this; }
        Projectile withHits(int count,int interval) {
            if(count<1||count>1&&interval<1)throw new IllegalArgumentException("projectile hits need an interval");
            hits=count;hitInterval=interval;return this;
        }
        Projectile withClones(int count) { clones=count; return this; }
        /** Poça que fica onde o projétil cai (0 quadros: some no chão). */
        int puddleFrames,puddleDamage,puddlePoisonFrames;
        float puddleHalfWidth;
        float spawnHeight(boolean crouching,boolean airborne) {
            return crouching ? crouchSpawnY : airborne ? airSpawnY : spawnY;
        }
    }
    /** Gameplay hurtbox in world units, independent of the PNG canvas. */
    /**
     * Ultra que segue o vídeo (tools/sprites/ultras.py): a ativação toca a animação até
     * {@code startupFrame}; a investida segura essa pose até encostar; daí a sequência anda no
     * relógio da animação e cada golpe acerta no quadro dele. Tempos em quadros do jogo.
     */
    static final class UltraSequence {
        /** Tempo da animação em que a ativação termina (a pose da investida). */
        final int startupFrame;
        /** Quadros, contados do começo da sequência, em que cada golpe acerta; o último lança. */
        final int[] hits;
        /** Quadros da sequência até o fim da animação. */
        final int length;
        /** Dano total (sem escalonamento), dividido igualmente entre os golpes. */
        final int damage;
        /** Posição do rival presa à pose: {quadro, frente, altura}, interpolada (agarrão); vazio = fica onde encostou. */
        final float[][] hold;
        UltraSequence(int startupFrame,int[] hits,int length,int damage,float[][] hold) {
            if(startupFrame<0||hits.length==0||damage<=0)throw new IllegalArgumentException("ultra sequence: startup, hits and damage");
            for(int i=0;i<hits.length;i++)
                if(hits[i]<1||hits[i]>=length||(i>0&&hits[i]<=hits[i-1]))throw new IllegalArgumentException("ultra sequence: hits must grow inside the length");
            this.startupFrame=startupFrame;this.hits=hits.clone();this.length=length;this.damage=damage;
            this.hold=hold==null?new float[0][]:hold;
        }
        /** Dano do golpe i: partes iguais, o resto no último. */
        int hitDamage(int i) {
            int each=damage/hits.length;
            return i==hits.length-1?damage-each*(hits.length-1):each;
        }
        /** Frente (x) e altura (y) do rival no quadro dado da sequência, ou null sem agarrão. */
        float[] holdAt(float frame) {
            if(hold.length==0)return null;
            if(frame<=hold[0][0])return new float[]{hold[0][1],hold[0][2]};
            for(int i=1;i<hold.length;i++){
                if(frame<=hold[i][0]){
                    float t=(frame-hold[i-1][0])/Math.max(1f,hold[i][0]-hold[i-1][0]);
                    return new float[]{hold[i-1][1]+(hold[i][1]-hold[i-1][1])*t,hold[i-1][2]+(hold[i][2]-hold[i-1][2])*t};
                }
            }
            float[] last=hold[hold.length-1];
            return new float[]{last[1],last[2]};
        }
    }

    static final class Body {
        final float halfWidth,standHeight,crouchHeight;
        /** Pushbox: keeps two bodies from overlapping; low enough to be jumped over. */
        final float pushHalfWidth,pushHeight;
        Body(float halfWidth,float standHeight,float crouchHeight,float pushHalfWidth,float pushHeight) {
            this.halfWidth=halfWidth;this.standHeight=standHeight;this.crouchHeight=crouchHeight;
            this.pushHalfWidth=pushHalfWidth;this.pushHeight=pushHeight;
        }
        float height(boolean crouching) { return crouching?crouchHeight:standHeight; }
    }
    /** Fighter rules: life, auto-combo, projectiles, body and input priority. */
    static final class Fighter {
        final int color,maxLife;
        final String[] autoCombo;
        final Projectile energy,superAttack;
        final int[] energyCommand;
        final Body body;
        /** Which buffered press wins when several are valid (most important first). */
        final AttackDefinition.Strength[] inputPriority;
        /** Move this fighter performs when its partner calls it with TAG ("S" or a ground normal). */
        final String assistMove;
        Fighter(int color,int maxLife,String[] autoCombo,Projectile energy,
                int[] energyCommand,Projectile superAttack,Body body,AttackDefinition.Strength[] inputPriority) {
            this(color,maxLife,autoCombo,energy,energyCommand,superAttack,body,inputPriority,null);
        }
        Fighter(int color,int maxLife,String[] autoCombo,Projectile energy,
                int[] energyCommand,Projectile superAttack,Body body,AttackDefinition.Strength[] inputPriority,
                String assistMove) {
            // Without a declared assist: the projectile when there is one, else the heavy normal.
            this.assistMove=assistMove!=null?assistMove:energy!=null?"S":"H";
            this.color=color;this.maxLife=scaledLife(maxLife);
            this.autoCombo=autoCombo.clone();this.energy=energy;this.energyCommand=energyCommand.clone();
            this.superAttack=superAttack;this.body=body;
            this.inputPriority=inputPriority==null?CombatConfig.DEFAULT_PRIORITY.clone():inputPriority.clone();
        }
        boolean hasSuperAttack() { return superAttack!=null; }
        /**
         * Vida de todo lutador vezes {@link CombatConfig#LIFE_SCALE_PERMILLE}: a luta dura mais sem
         * mexer no dano de cada golpe (o mesmo que tirar ~29% do dano de tudo).
         */
        static int scaledLife(int packLife) {
            return (int)((long)packLife * CombatConfig.LIFE_SCALE_PERMILLE / 1000);
        }
        /** Frames to stand up after a knockdown, timed to this fighter's GETUP art (0 = engine default). */
        int wakeupFrames;
        Fighter withWakeup(int frames) { wakeupFrames=frames; return this; }
        /** Andar no chão (px/s) no ritmo da arte de andar do lutador (0 = o do motor). */
        float walkSpeed, walkBackSpeed;
        Fighter withWalk(float forward,float back) { walkSpeed=forward; walkBackSpeed=back; return this; }
        /** Estilo de combo (mecanicas.json): a CPU joga conforme ele (CpuStyle). */
        String comboStyle="equilibrado";
        Fighter withComboStyle(String style) { comboStyle=style; return this; }
        /** Nome da energia (S1) na lista de comandos, e se o comando dela é de segurar. */
        String energyName;
        boolean energyHold;
        Fighter withEnergyCommand(String name,boolean hold) { energyName=name; energyHold=hold; return this; }
        /** Desenho do projétil do S1 (ProjetilVisual.Tipo); null = a bola de energia padrão. */
        String energyVisual;
        Fighter withEnergyVisual(String visual) { energyVisual=visual; return this; }
        /** Fury: extra damage, in permille, when the life is all gone (it grows as life is lost). */
        int furyPermille;
        /** Air dashes per jump (the engine default is one). */
        int airDashes=1;
        /** Own throw: damage (0 = the engine's) and extra reach in world units. */
        int throwDamage;
        float throwRangeBonus;
        Fighter withMechanics(int fury,int dashes,int grabDamage,float grabRange) {
            furyPermille=fury;airDashes=dashes;throwDamage=grabDamage;throwRangeBonus=grabRange;return this;
        }
        /** Jumps off the arena's walls (once per jump). */
        boolean wallJump;
        Fighter withWallJump(boolean value) { wallJump=value; return this; }
        /** Henchmen the Super calls in instead of its wave (0 = the wave). */
        int henchmen;
        Fighter withHenchmen(int count) { henchmen=count; return this; }
        /** Ultra que segue a animação do vídeo (null: investida + cinemática + raio). */
        UltraSequence ultraSequence;
        Fighter withUltraSequence(UltraSequence value) { ultraSequence=value; return this; }
        /** Damage multiplier (permille) of this fighter's hits at the given life. */
        int furyScale(int life) {
            if(furyPermille<=0||maxLife<=0)return 1000;
            int lost=Math.max(0,maxLife-life);
            return 1000+(int)((long)furyPermille*lost/maxLife);
        }
    }
    final String id,displayName;
    final CharacterVisualProfile profile;
    /** +1 when the art faces right, -1 when it faces left. */
    final int artFacing;
    /** Opaque height above the root in world units, measured by the importer. */
    final float visualStandHeight,visualCrouchHeight;
    final Fighter fighter;
    final Map<String,Animation> animations;
    final Map<String,Move> moves;
    final Map<String,Animation> specialAnimations;
    /** Command specials by binding (S2, S3...), longest command first. */
    final Map<String,Special> specials;
    CharacterDefinition(String id,String displayName,CharacterVisualProfile profile,int artFacing,
                        float visualStandHeight,float visualCrouchHeight,Fighter fighter,
                        Map<String,Animation> animations,Map<String,Move> moves,
                        Map<String,Animation> specialAnimations,Map<String,Special> specials) {
        this.id=id;this.displayName=displayName;this.profile=profile;this.artFacing=artFacing;
        this.visualStandHeight=visualStandHeight;this.visualCrouchHeight=visualCrouchHeight;this.fighter=fighter;
        this.animations=Collections.unmodifiableMap(new LinkedHashMap<>(animations));
        Map<String,Move> allMoves=new LinkedHashMap<>(moves);
        // Golpe alto de chão (→ + M) de todos: o do pack, ou montado do M até ter animação própria.
        if(!allMoves.containsKey(GolpeAlto.BINDING)&&allMoves.containsKey("M")) {
            allMoves.put(GolpeAlto.BINDING,GolpeAlto.from(allMoves.get("M"),allMoves.get("H")));
        }
        this.moves=Collections.unmodifiableMap(allMoves);
        this.specialAnimations=Collections.unmodifiableMap(new LinkedHashMap<>(specialAnimations));
        // Longest command first, so →↓↘ wins over a shorter motion it contains.
        java.util.List<Map.Entry<String,Special>> sorted=new java.util.ArrayList<>(specials.entrySet());
        java.util.Collections.sort(sorted,(x,y)->y.getValue().commandLength()-x.getValue().commandLength());
        Map<String,Special> ordered=new LinkedHashMap<>();
        for(Map.Entry<String,Special> e:sorted)ordered.put(e.getKey(),e.getValue());
        this.specials=Collections.unmodifiableMap(ordered);
        validateCombat();
    }
    /** Copy with different animations; used by tests and tooling. */
    CharacterDefinition withAnimations(String id,Map<String,Animation> animations) {
        return new CharacterDefinition(id,id,profile,artFacing,visualStandHeight,visualCrouchHeight,
            fighter,animations,moves,specialAnimations,specials);
    }
    /** Combat definition of a binding, S or SUPER; null when the character lacks it. */
    AttackDefinition attack(String id) {
        if("S".equals(id))return fighter.energy==null?null:fighter.energy.attack;
        if("SUPER".equals(id))return fighter.superAttack==null?null:fighter.superAttack.attack;
        Move move=moveFor(id);
        return move==null?null:move.attack;
    }
    /** Move with art and hitboxes for a normal or command-special binding; null otherwise. */
    Move moveFor(String id) {
        Move move=moves.get(id);
        if(move!=null)return move;
        Special special=specials.get(id);
        return special==null?null:special.move;
    }
    /** The command special this button press completes, consuming the motion; null when none. */
    String matchSpecial(MotionParser motion,String button,int clock,int motionWindow,int pressWindow) {
        for(Map.Entry<String,Special> e:specials.entrySet()) {
            Special s=e.getValue();
            if(s.acceptsButton(button)&&s.matches(motion,clock,motionWindow,pressWindow))return e.getKey();
        }
        return null;
    }

    /**
     * O especial que o botão solta agora: "S2".."S9", "S" (a energia, S1) ou null (golpe normal).
     * No chão só os especiais de chão, no ar só os de ar (e a energia, que sai nos dois). Se mais
     * de um comando bate, ganha o de mais passos (um movimento ganha de um segurar); empatado, o
     * que terminou por último; e então os especiais antes da energia, na ordem da ficha. O comando
     * escolhido consome a história e as cargas.
     */
    String matchCommand(MotionParser motion,String button,boolean grounded,int clock,CombatConfig config) {
        String best=null;
        int bestLength=-1,bestEnd=Integer.MAX_VALUE;
        for(Map.Entry<String,Special> e:specials.entrySet()) {
            Special s=e.getValue();
            if(!s.acceptsButton(button)||s.air==grounded)continue;
            int end=commandEnd(motion,s.command,s.steps,s.hold,clock,config);
            int length=weight(s.steps,s.hold);
            if(end>=0&&(length>bestLength||(length==bestLength&&end<bestEnd))) {
                best=e.getKey();bestLength=length;bestEnd=end;
            }
        }
        if(fighter.energy!=null&&fighter.energyCommand.length>0) {
            int[] steps=MotionParser.requiredSteps(fighter.energyCommand);
            int end=commandEnd(motion,fighter.energyCommand,steps,fighter.energyHold,clock,config);
            int length=weight(steps,fighter.energyHold);
            if(end>=0&&(length>bestLength||(length==bestLength&&end<bestEnd)))best="S";
        }
        if(best!=null)motion.consume(clock);
        return best;
    }

    /** Peso no desempate: cada passo vale 2; segurar vale 3 (perde de qualquer movimento). */
    private static int weight(int[] steps,boolean hold) { return hold?3:steps.length*2; }

    private static int commandEnd(MotionParser motion,int[] command,int[] steps,boolean hold,int clock,CombatConfig config) {
        if(hold)return motion.holdCommand(command,clock,config.chargeFrames,config.chargeKeepFrames)?0:-1;
        // Comandos longos (meia-lua) ganham mais tempo para ser feitos.
        int window=config.motionWindowFrames+config.motionWindowPerStep*Math.max(0,steps.length-2);
        return motion.commandEnd(steps,clock,window,config.motionPressFrames);
    }
    /**
     * Load-time validation of the combat data: every cancel route points to an existing
     * move of the same height (ground/air) and the auto-combo is a declared route.
     */
    private void validateCombat() {
        java.util.List<String> ids=new java.util.ArrayList<>(moves.keySet());
        ids.addAll(specials.keySet());
        if(fighter.energy!=null)ids.add("S");
        if(fighter.superAttack!=null)ids.add("SUPER");
        for(String id:ids) {
            AttackDefinition a=attack(id);
            if(!a.id.equals(id))throw new IllegalArgumentException(this.id+"/"+id+": attack id mismatch "+a.id);
            boolean air=id.startsWith("j");
            for(String target:a.cancelInto()) {
                // No chão: pulo do cancelamento. No ar: o cancelamento de pulo (uma vez por voo).
                if(AttackDefinition.JUMP.equals(target))continue;
                if(attack(target)==null)
                    throw new IllegalArgumentException(this.id+"/"+id+": cancelInto "+target+" does not exist");
                boolean special="S".equals(target)||"SUPER".equals(target)||specials.containsKey(target);
                if(!special && target.startsWith("j")!=air)
                    throw new IllegalArgumentException(this.id+"/"+id+": cancelInto "+target+" changes ground/air");
            }
        }
        for(int i=0;i+1<fighter.autoCombo.length;i++) {
            if(!attack(fighter.autoCombo[i]).cancelsInto(fighter.autoCombo[i+1]))
                throw new IllegalArgumentException(this.id+": autoCombo "+fighter.autoCombo[i]+" -> "
                    +fighter.autoCombo[i+1]+" is not a declared cancel route");
        }
    }
    Animation animation(String id) {
        Animation value=animations.get(id);
        if(value==null)throw new IllegalArgumentException("Unknown animation "+this.id+"/"+id);
        return value;
    }
    /**
     * Resolves the move for an input. Airborne attacks use the jL/jM/jH bindings; every
     * binding is mandatory in the pack, so this never falls back silently.
     */
    Move move(String type,boolean airborne) {
        String binding=airborne ? "j"+(type.startsWith("2")?type.substring(1):type) : type;
        return moves.get(binding);
    }
}
