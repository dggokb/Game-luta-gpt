package com.gamelutagpt;
import android.graphics.*;
import android.view.MotionEvent;
import java.io.*;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class SpriteIntegrationTest {
    private GameView game;
    @Before public void setup(){game=new GameView(RuntimeEnvironment.getApplication());game.layout(0,0,1280,720);}
    private Object get(String key)throws Exception {Field f=GameView.class.getDeclaredField(key);f.setAccessible(true);return f.get(game);}
    private void set(String key,Object v)throws Exception {Field f=GameView.class.getDeclaredField(key);f.setAccessible(true);f.set(game,v);}
    private void frames(int n)throws Exception {Method m=GameView.class.getDeclaredMethod("advanceSimulation",float.class);m.setAccessible(true);for(int i=0;i<n;i++)m.invoke(game,1f/60);}
    private SpriteMotion motion()throws Exception {return ((SpriteFighterRenderer)get("spriteFighterRenderer")).motion;}
    private void touch(int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,10000,action,x,y,0);game.onTouchEvent(e);e.recycle();}
    private void invoke(String name,Class<?>[] types,Object... args)throws Exception {Method m=GameView.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(game,args);}
    private float meter(Object fighter)throws Exception {Field f=fighter.getClass().getDeclaredField("superMeter");f.setAccessible(true);return f.getFloat(fighter);}
    private Object activeFighter()throws Exception {return ((Object[])get("team"))[0];}
    private CombatFighter player(){return game.engine.fighter(0);}
    private CombatFighter npc(){return game.engine.fighter(1);}
    /** Presses a button through the real pad (the engine decides whether it starts). */
    private void press(PadInput.Button button,int direction){game.pad.setDirection(direction);game.pad.press(button);}
    /** ↓ + TAG through the real pad: the engine's raw tag (exit, enter and pose, 70 frames). */
    private void rawTag()throws Exception {press(PadInput.Button.TAG,3);frames(1);game.pad.setDirection(0);}

    private String rendererCharacterId()throws Exception {
        SpriteFighterRenderer renderer=(SpriteFighterRenderer)get("spriteFighterRenderer");
        Field field=SpriteFighterRenderer.class.getDeclaredField("character");
        field.setAccessible(true);
        return ((CharacterDefinition)field.get(renderer)).id;
    }

    @Test public void productionRosterUsesRealSecondCharacterPack()throws Exception {
        assertArrayEquals(new String[]{"player_base","player_two"},GeneratedCharacters.TEAM);
        assertEquals("monster_npc",GeneratedCharacters.OPPONENT);
        CharacterDefinition npc=GeneratedCharacters.opponentCharacter();
        assertEquals("monster_npc",npc.id);
        assertEquals("Brutamonte",npc.displayName);
        assertEquals("monster_npc_pack",npc.animation("IDLE").atlas.resource);
        CharacterDefinition first=GeneratedCharacters.get("player_base");
        CharacterDefinition second=GeneratedCharacters.get("player_two");
        assertNotSame(first,second);
        assertEquals("Lutador Teste 2",second.displayName);
        assertEquals("player_two",second.profile.id);
        assertEquals(384,second.profile.frameWidth);
        assertEquals(256,second.profile.frameHeight);
        assertEquals(192f,second.profile.rootX,.001f);
        assertEquals(246f,second.profile.rootY,.001f);
        assertTrue(first.animations.keySet().containsAll(second.animations.keySet()));
        assertTrue(first.moves.keySet().containsAll(second.moves.keySet()));
        // Beyond the shared inputs a character may add air ↓ + button versions (j2X).
        java.util.Set<String> extra=new java.util.HashSet<>(first.moves.keySet());extra.removeAll(second.moves.keySet());
        for(String id:extra)assertTrue("Unexpected extra move "+id,id.startsWith("j2"));
        assertNotNull(first.moves.get("2L").animation);
        assertNotNull(first.moves.get("2M").animation);
        // Inputs without dedicated art declare their pose explicitly.
        assertNull(second.moves.get("2L").animation);
        assertEquals(SpriteStates.POSE_CROUCH,second.moves.get("2L").pose);
        assertEquals(SpriteStates.POSE_AIR,second.moves.get("jH").pose);
        assertNotEquals(first.animation("IDLE").atlas.resource,second.animation("IDLE").atlas.resource);
        assertEquals("player_two_idle",second.animation("IDLE").atlas.resource);
        assertEquals("player_two_movement",second.animation("WALK_FORWARD").atlas.resource);
        assertEquals("player_two_jab",second.animation("LIGHT_JAB").atlas.resource);
        assertEquals("player_two_medium_kick",second.animation("MEDIUM_KICK").atlas.resource);
        assertEquals("player_two_heavy_straight",second.animation("HEAVY_STRAIGHT").atlas.resource);
        for(String binding:new String[]{"L","M","H"}) {
            CharacterDefinition.Move a=first.moves.get(binding);
            CharacterDefinition.Move b=second.moves.get(binding);
            assertEquals(a.damage,b.damage);
            assertEquals(a.activeStart,b.activeStart,.0001f);
            assertEquals(a.activeEnd,b.activeEnd,.0001f);
            assertEquals(a.totalTime,b.totalTime,.0001f);
            // Reach is measured in world units and follows each character's art scale.
            assertEquals(a.reach/first.profile.worldScale,b.reach/second.profile.worldScale,1.5f);
            assertEquals(a.animation.id,b.animation.id);
        }
    }

    @Test public void playerBaseUsesDedicated2LThroughCharacterPackEngine()throws Exception {
        CharacterDefinition first=GeneratedCharacters.get("player_base");
        assertTrue(first.animations.containsKey("CROUCH_LIGHT"));
        assertEquals("player_base_crouch_light",
            first.animation("CROUCH_LIGHT").atlas.resource);
        assertTrue(first.moves.containsKey("2L"));
        assertEquals("CROUCH_LIGHT",first.moves.get("2L").animation.id);

        press(PadInput.Button.LIGHT,3);frames(1);
        assertEquals("CROUCH_LIGHT",motion().clip);
        assertEquals(0,motion().frame());
        frames(3);
        assertTrue("2L must advance through authored frames",motion().frame() >= 1);

        assertTrue(first.animations.containsKey("CROUCH_MEDIUM"));
        assertEquals("player_base_crouch_medium",
            first.animation("CROUCH_MEDIUM").atlas.resource);
        assertTrue(first.moves.containsKey("2M"));
        assertEquals("CROUCH_MEDIUM",first.moves.get("2M").animation.id);
        // Shipped atlases are packed (empty border cropped); geometry comes from generation.
        assertEquals(GeneratedSpriteLayouts.CROUCH_MEDIUM_FRAME_WIDTH,first.animation("CROUCH_MEDIUM").atlas.width);
        assertEquals(GeneratedSpriteLayouts.CROUCH_MEDIUM_ROOT_X,first.animation("CROUCH_MEDIUM").atlas.rootX);
        assertTrue("2M keeps room for the extended leg",
            first.animation("CROUCH_MEDIUM").atlas.width>first.animation("CROUCH_LIGHT").atlas.width);

        setup();press(PadInput.Button.MEDIUM,3);frames(1);
        assertEquals("CROUCH_MEDIUM",motion().clip);
        assertEquals(0,motion().frame());
        frames(5);
        assertTrue("2M must advance through authored frames",motion().frame() >= 1);

        setup();press(PadInput.Button.HEAVY,3);frames(1);
        assertEquals("CROUCH_HEAVY",motion().clip);
        assertEquals(0,motion().frame());
        CharacterDefinition.Move launcher=first.moves.get("2H");
        // The uppercut frame is on screen for the whole active window of the launcher.
        for(float t=launcher.activeStart;t<launcher.activeEnd;t+=.01f)
            assertEquals(2,launcher.animation.frame(launcher.animationTime(t),0));
        frames(24);
        assertEquals(SpriteMotion.Clip.CROUCH,motion().clip);

        setup();player().grounded=false;player().y=430f;player().vy=-300f;
        press(PadInput.Button.HEAVY,0);frames(1);
        assertEquals("JUMP_HEAVY",motion().clip);

        setup();player().grounded=false;player().y=430f;player().vy=-300f;
        press(PadInput.Button.LIGHT,0);frames(1);
        assertEquals("JUMP_LIGHT",motion().clip);

        setup();blockstun(CombatFighter.GUARD_LOW);frames(1);
        assertEquals("DEFENSE_CROUCH",motion().clip);

        setup();player().status=CombatFighter.Status.KNOCKDOWN;player().knockdownFrame=0;frames(1);
        assertEquals("KNOCKDOWN",motion().clip);
    }

    private void blockstun(int guard){
        CombatFighter p=player();
        p.status=CombatFighter.Status.BLOCKSTUN;p.lastGuard=guard;p.stunLeft=p.stunTotal=14;p.stunElapsed=0;
    }

    @Test public void opponentIsRenderedByTheGenericSpriteEngine()throws Exception {
        SpriteFighterRenderer opponent=(SpriteFighterRenderer)get("opponentSpriteRenderer");
        Field field=SpriteFighterRenderer.class.getDeclaredField("character");
        field.setAccessible(true);
        assertEquals("the training CPU uses the base character","player_base",((CharacterDefinition)field.get(opponent)).id);
        Field tint=SpriteFighterRenderer.class.getDeclaredField("tint");
        tint.setAccessible(true);
        assertNotNull("drawn washed out",tint.get(opponent));
        frames(2);
        assertEquals(SpriteMotion.Clip.IDLE,opponent.motion.clip);
    }

    @Test public void realTagSwitchLoadsSecondPackAndAllStandingAttacks()throws Exception {
        assertEquals("player_base",rendererCharacterId());
        rawTag();
        frames(72);
        assertEquals(1,game.engine.team(0).point);
        assertEquals("player_two",rendererCharacterId());

        String[] bindings={"L","M","H"};
        PadInput.Button[] buttons={PadInput.Button.LIGHT,PadInput.Button.MEDIUM,PadInput.Button.HEAVY};
        CharacterDefinition second=GeneratedCharacters.get("player_two");
        assertSame("The engine plays the tagged-in pack",second,player().character());
        for(int i=0;i<bindings.length;i++) {
            int total=second.attack(bindings[i]).totalFrames;
            press(buttons[i],0);
            frames(1);
            assertEquals(second.moves.get(bindings[i]).animation.id,motion().clip);
            frames(total);
            assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
        }
    }

    @Test public void actualPadDrivesForwardBackAndStopsAtRelease()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);frames(10);assertEquals(SpriteMotion.Clip.WALK_FORWARD,motion().clip);
        touch(MotionEvent.ACTION_MOVE,85,555);frames(10);assertEquals(SpriteMotion.Clip.WALK_BACK,motion().clip);
        touch(MotionEvent.ACTION_UP,85,555);frames(10);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    @Test public void actualJumpUsesAirFramesThenHeldCrouchAtLanding()throws Exception {
        touch(MotionEvent.ACTION_DOWN,175,465);frames(8);assertEquals(SpriteMotion.Clip.JUMP,motion().clip);
        touch(MotionEvent.ACTION_MOVE,175,645);frames(60);assertEquals(SpriteMotion.Clip.CROUCH,motion().clip);
        // Holding down after landing keeps the last (fully crouched) pose of the crouch clip.
        CharacterDefinition.Animation crouch=GeneratedCharacters.defaultCharacter().animation(SpriteMotion.Clip.CROUCH);
        assertEquals(crouch.frame(crouch.duration+1,0),motion().frame());
    }
    @Test public void facingChangesSelectForwardInBothDirections()throws Exception {
        player().x=1400f;npc().x=1000f;player().facing=-1;npc().facing=1;
        touch(MotionEvent.ACTION_DOWN,85,555);frames(6);assertEquals(SpriteMotion.Clip.WALK_FORWARD,motion().clip);
    }
    @Test public void pauseClearsHeldInputWithoutStartingAnotherLoop()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);frames(5);game.pauseGame();assertEquals(0,game.pad.direction());
        game.resumeGame();assertFalse((Boolean)get("running"));frames(1);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    @Test public void touchIsQueuedAndLatestMoveWinsWithoutBuildingBacklog()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);
        assertEquals("Touch waits for the game thread",0,game.pad.direction());
        for(int i=0;i<80;i++)touch(MotionEvent.ACTION_MOVE,265-i*2.25f,555);
        frames(1);
        assertEquals("Latest move wins",5,game.pad.direction());
        assertTrue("Walked left",player().x<420f);
        touch(MotionEvent.ACTION_UP,85,555);frames(1);
        assertEquals(0,game.pad.direction());
    }
    @Test public void powerGaugeRequiresAConfirmedHitThroughTheRealPad()throws Exception {
        npc().x=1000f;
        press(PadInput.Button.LIGHT,0);frames(40);
        assertEquals("A whiff builds no meter",0,player().state.superMeter);

        npc().x=520f;
        press(PadInput.Button.LIGHT,0);frames(30);
        assertEquals(100,player().state.superMeter);

        // ↓ ↘ → + M: the motion parser turns the button into the energy special.
        game.pad.setDirection(3);frames(1);game.pad.setDirection(2);frames(1);game.pad.setDirection(1);frames(1);
        game.pad.press(PadInput.Button.MEDIUM);frames(1);
        assertEquals("S",player().attack.id);
        frames(30);
        assertEquals(550,player().state.superMeter);
    }

    @Test public void customCharacterAnimationRendersWithSameGenericRenderer() {
        CharacterDefinition base=GeneratedCharacters.defaultCharacter();
        java.util.Map<String,CharacterDefinition.Animation> animations=new java.util.LinkedHashMap<>(base.animations);
        animations.put("CUSTOM_PUNCH",new CharacterDefinition.Animation("CUSTOM_PUNCH",base.animation("LIGHT_JAB").atlas,
            new int[]{0,1,2},new float[]{.04f,.06f,.06f},false,0));
        CharacterDefinition custom=base.withAnimations("custom",animations);
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication(),custom);
        Rect bounds=renderedBounds(renderer,"CUSTOM_PUNCH",.07f,0);
        assertTrue(bounds.width()>60);assertTrue(bounds.height()>150);
    }
    /** An attack plays its frames forward without stepping back, then hands over to Idle. */
    private void assertAttackPlaysForwardThenIdles(PadInput.Button button,String clip,int steps)throws Exception {
        press(button,0);
        frames(1);assertEquals(clip,motion().clip);assertEquals(0,motion().frame());
        int last=0;boolean advanced=false;
        for(int i=0;i<steps && clip.equals(motion().clip);i++) {
            frames(1);
            if(!clip.equals(motion().clip))break;
            int f=motion().frame();
            assertTrue(clip+" never steps back: "+last+" -> "+f,f>=last);
            advanced|=f>last;last=f;
        }
        assertTrue(clip+" animates",advanced);
        frames(5);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    private Bitmap atlasBitmap(CharacterDefinition.Atlas a) {
        android.content.res.Resources r=RuntimeEnvironment.getApplication().getResources();
        int id=r.getIdentifier(a.resource,"drawable",RuntimeEnvironment.getApplication().getPackageName());
        return BitmapFactory.decodeResource(r,id);
    }
    /** Every packed cell of the attack atlas holds one whole pose with a safety margin. */
    private void assertAttackAtlasCellsAreWhole(String animation) {
        CharacterDefinition.Atlas a=GeneratedCharacters.defaultCharacter().animation(animation).atlas;
        Bitmap atlas=atlasBitmap(a);
        assertNotNull(atlas);
        assertEquals(a.columns*a.width,atlas.getWidth());
        assertEquals((a.count+a.columns-1)/a.columns*a.height,atlas.getHeight());
        assertTrue(a.rootX>0 && a.rootX<a.width);
        assertTrue(a.rootY>0 && a.rootY<a.height);
        for(int i=0;i<a.count;i++) {
            int left=(i%a.columns)*a.width,top=(i/a.columns)*a.height;
            int count=0,minX=a.width,minY=a.height,maxX=-1,maxY=-1;
            for(int y=0;y<a.height;y++)for(int x=0;x<a.width;x++) {
                if(Color.alpha(atlas.getPixel(left+x,top+y))>10) {
                    count++;
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
            }
            assertTrue("Empty "+animation+" frame "+i,count>10000);
            assertTrue(animation+" clipped left "+i,minX>=8);
            assertTrue(animation+" clipped right "+i,a.width-1-maxX>=8);
            assertTrue(animation+" clipped top "+i,minY>=8);
            assertTrue(animation+" clipped bottom "+i,a.height-1-maxY>=8);
        }
    }
    /** The production renderer draws every frame of the attack inside its own cell. */
    private void assertRendererKeepsAttackInsideCanvas(String animation,String reviewFile)throws Exception {
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        CharacterDefinition.Animation anim=GeneratedCharacters.defaultCharacter().animation(animation);
        CharacterDefinition.Atlas a=anim.atlas;
        // The cell drawn at the pack's scale (p01 is drawn 7.5% larger than its atlas).
        float scale=GeneratedCharacters.defaultCharacter().profile.worldScale;
        int fw=(int)Math.ceil(a.width*scale),fh=(int)Math.ceil(a.height*scale),samples=a.count,background=Color.rgb(32,36,44);
        Bitmap review=Bitmap.createBitmap(fw*samples,fh,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(review);
        canvas.drawColor(background);
        for(int i=0;i<samples;i++) {
            float t=anim.duration*(i+.5f)/samples;
            renderer.motion.clip=animation;renderer.motion.time=t;
            assertEquals(anim.frame(t,0),renderer.motion.frame());
            canvas.save();canvas.translate(i*fw,0);
            renderer.draw(canvas,a.rootX*scale,a.rootY*scale,1,false,false);
            canvas.restore();
        }
        for(int i=0;i<samples;i++) {
            int minX=fw,minY=fh,maxX=-1,maxY=-1;
            for(int y=0;y<fh;y++)for(int x=0;x<fw;x++) {
                if(review.getPixel(i*fw+x,y)!=background) {
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
            }
            assertTrue("Rendered "+animation+" clipped left "+i,minX>=8);
            assertTrue("Rendered "+animation+" clipped right "+i,fw-1-maxX>=8);
            assertTrue("Rendered "+animation+" clipped top "+i,minY>=8);
            assertTrue("Rendered "+animation+" clipped bottom "+i,fh-1-maxY>=8);
        }
        File dir=new File("build/sprite-review");
        dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,reviewFile))) {
            assertTrue(review.compress(Bitmap.CompressFormat.PNG,100,out));
        }
    }
    @Test public void standingLightAttackPlaysJabStartupActiveRecoveryThenReturnsIdle()throws Exception {
        assertAttackPlaysForwardThenIdles(PadInput.Button.LIGHT,"LIGHT_JAB",6);
    }
    @Test public void standingMediumAttackPlaysForwardAtScaleOne()throws Exception {
        assertAttackPlaysForwardThenIdles(PadInput.Button.MEDIUM,"MEDIUM_KICK",11);
        // The renderer draws at the pack's own scale (p01: the official intro size).
        assertEquals(GeneratedCharacters.defaultCharacter().profile.worldScale,
            ((SpriteFighterRenderer)get("spriteFighterRenderer")).visualProfile().worldScale,.001f);
    }
    @Test public void mediumAttackUsesWideCanvasWithoutShrinkingCharacter() {
        assertAttackAtlasCellsAreWhole("MEDIUM_KICK");
    }
    @Test public void productionRendererKeepsMediumAttackInsideGeneratedCanvas()throws Exception {
        assertRendererKeepsAttackInsideCanvas("MEDIUM_KICK","medium-kick-render.png");
    }

    @Test public void standingHeavyAttackUsesNineFrameStraightAtScaleOne()throws Exception {
        press(PadInput.Button.HEAVY,0);
        frames(1);
        assertEquals("HEAVY_STRAIGHT",motion().clip);
        assertEquals(0,motion().frame());
        frames(25);
        assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
        assertEquals(
            GeneratedCharacters.defaultCharacter().profile.worldScale,
            ((SpriteFighterRenderer)get("spriteFighterRenderer"))
                .visualProfile().worldScale,
            .001f
        );
    }

    @Test public void heavyAttackUsesGeneratedCanvasWithoutClipping() {
        assertAttackAtlasCellsAreWhole("HEAVY_STRAIGHT");
    }

    @Test public void productionRendererKeepsHeavyAttackInsideGeneratedCanvas()throws Exception {
        assertRendererKeepsAttackInsideCanvas("HEAVY_STRAIGHT","heavy-straight-render.png");
    }

    @Test public void normalizedAtlasesUsePlayerBaseCellGeometry() {
        CharacterVisualProfile p=GeneratedCharacters.defaultCharacter().profile;
        assertEquals(256,p.frameWidth);
        assertEquals(256,p.frameHeight);
        assertEquals(128f,p.rootX,.001f);
        assertEquals(238f,p.rootY,.001f);

        Bitmap idle=BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().getResources(),
            R.drawable.player_base_idle
        );
        Bitmap jab=BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().getResources(),
            R.drawable.player_base_jab
        );

        assertNotNull(idle);assertNotNull(jab);
        // The idle is a long video-derived loop: its grid follows the frame count.
        assertEquals(0,idle.getWidth()%GeneratedSpriteLayouts.IDLE_FRAME_WIDTH);
        assertEquals(0,idle.getHeight()%GeneratedSpriteLayouts.IDLE_FRAME_HEIGHT);
        assertTrue(idle.getWidth()/GeneratedSpriteLayouts.IDLE_FRAME_WIDTH
            *(idle.getHeight()/GeneratedSpriteLayouts.IDLE_FRAME_HEIGHT)>=GeneratedSpriteLayouts.IDLE_FRAME_COUNT);
        assertEquals(0,jab.getWidth()%GeneratedSpriteLayouts.JAB_FRAME_WIDTH);
        assertEquals(0,jab.getHeight()%GeneratedSpriteLayouts.JAB_FRAME_HEIGHT);
        // Packing never grows past the canonical 256x256 authoring cell.
        assertTrue(GeneratedSpriteLayouts.IDLE_FRAME_WIDTH<=256 && GeneratedSpriteLayouts.IDLE_FRAME_HEIGHT<=256);
    }

    @Test public void characterProfilesCanRepresentDifferentSizedFighters() {
        CharacterVisualProfile base=GeneratedCharacters.defaultCharacter().profile;
        CharacterVisualProfile large=
            new CharacterVisualProfile("large_test",320,320,160f,300f,1f);
        RectF a=new RectF(),b=new RectF();
        base.place(a,500f,500f);
        large.place(b,500f,500f);
        assertEquals(base.frameHeight*base.worldScale,a.height(),.001f);
        assertEquals(320f,b.height(),.001f);
        assertEquals(500f,a.left+base.rootX*base.worldScale,.001f);
        assertEquals(500f,b.left+large.rootX*large.worldScale,.001f);
    }

    private Rect renderedBounds(
        SpriteFighterRenderer renderer,
        String clip,
        float time,
        float distance
    ) {
        Bitmap out=Bitmap.createBitmap(420,320,Bitmap.Config.ARGB_8888);
        SpriteMotion m=renderer.motion;
        m.clip=clip;m.time=time;m.distance=distance;
        renderer.draw(new Canvas(out),210,270,1,false,false);
        int minX=out.getWidth(),minY=out.getHeight(),maxX=-1,maxY=-1;
        for(int y=0;y<out.getHeight();y++)for(int x=0;x<out.getWidth();x++) {
            if(Color.alpha(out.getPixel(x,y))>10) {
                minX=Math.min(minX,x);minY=Math.min(minY,y);
                maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
            }
        }
        assertTrue("Rendered sprite is empty",maxX>=minX && maxY>=minY);
        return new Rect(minX,minY,maxX+1,maxY+1);
    }

    private int upperBodyWidth(
        SpriteFighterRenderer renderer,
        String clip,
        float time,
        float distance
    ) {
        Bitmap out=Bitmap.createBitmap(420,320,Bitmap.Config.ARGB_8888);
        SpriteMotion m=renderer.motion;
        m.clip=clip;m.time=time;m.distance=distance;
        renderer.draw(new Canvas(out),210,270,1,false,false);

        Rect b=renderedBounds(renderer,clip,time,distance);
        int bandBottom=b.top + Math.max(1,Math.round(b.height()*0.22f));
        int minX=out.getWidth(),maxX=-1;
        for(int y=b.top;y<=bandBottom && y<out.getHeight();y++) {
            for(int x=0;x<out.getWidth();x++) {
                if(Color.alpha(out.getPixel(x,y))>10) {
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                }
            }
        }
        assertTrue("Upper body band empty",maxX>=minX);
        return maxX-minX+1;
    }

    @Test public void normalizedStandingFramesStayOnModelAtScaleOne() {
        SpriteFighterRenderer renderer=
            new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        assertEquals(GeneratedCharacters.defaultCharacter().profile.worldScale,renderer.visualProfile().worldScale,.001f);

        int idleUpper=upperBodyWidth(renderer,SpriteMotion.Clip.IDLE,0f,0f);
        int walkUpper=upperBodyWidth(renderer,SpriteMotion.Clip.WALK_FORWARD,0f,1f);
        int jabUpper=upperBodyWidth(renderer,"LIGHT_JAB",.016f,0f);
        int heavyUpper=upperBodyWidth(renderer,"HEAVY_STRAIGHT",.016f,0f);

        float idleRatio=idleUpper/(float)walkUpper;
        float jabRatio=jabUpper/(float)walkUpper;
        float heavyRatio=heavyUpper/(float)idleUpper;
        assertTrue("Idle off-model: "+idleRatio,idleRatio>=.94f && idleRatio<=1.12f);
        assertTrue("Jab startup off-model: "+jabRatio,jabRatio>=.94f && jabRatio<=1.14f);
        assertTrue("Heavy startup off-model: "+heavyRatio,heavyRatio>=.94f && heavyRatio<=1.05f);
    }
    @Test public void packagedAtlasIsVisibleAndEveryCropContainsOneWholePose()throws Exception {
        // The 4x4 movement grid of the drawn character (player_two); p01 uses one atlas per video clip.
        Bitmap atlas=BitmapFactory.decodeResource(RuntimeEnvironment.getApplication().getResources(),R.drawable.player_two_movement);
        assertNotNull(atlas);assertTrue(atlas.hasAlpha());
        int fw=GeneratedSpriteLayouts.PLAYER_TWO_MOVEMENT_FRAME_WIDTH,fh=GeneratedSpriteLayouts.PLAYER_TWO_MOVEMENT_FRAME_HEIGHT;
        assertEquals(4*fw,atlas.getWidth());assertEquals(4*fh,atlas.getHeight());
        for(int i=0;i<16;i++) {
            int left=(i%4)*fw,top=(i/4)*fh,count=0;
            for(int y=top;y<top+fh;y++)for(int x=left;x<left+fw;x++)if(Color.alpha(atlas.getPixel(x,y))>128)count++;
            assertTrue("Empty sprite "+i,count>4500);assertTrue("Opaque background "+i,count<fw*fh*.70f);
        }
    }
    private int activeLife()throws Exception {Field f=activeFighter().getClass().getDeclaredField("life");f.setAccessible(true);return f.getInt(activeFighter());}

    @Test public void opponentLowAttackPlaysItsOwnClip()throws Exception {
        CharacterDefinition npc=npc().character();
        // The CPU presses buttons like a player: crouching M through its buffer.
        npc().buffer.push(InputBuffer.Button.MEDIUM,null,true);
        frames(1);
        assertEquals("2M",npc().attack.id);
        SpriteFighterRenderer opponent=(SpriteFighterRenderer)get("opponentSpriteRenderer");
        assertEquals(npc.moves.get("2M").animation.id,opponent.motion.clip);
        assertNotEquals(npc.moves.get("M").animation.id,opponent.motion.clip);
    }
    @Test public void cameraAndHudUseMeasuredSpriteHeight()throws Exception {
        CharacterDefinition npc=npc().character();
        Method top=GameView.class.getDeclaredMethod("opponentVisualTop");top.setAccessible(true);
        float visualTop=(Float)top.invoke(game);
        assertEquals(npc().y-npc.visualStandHeight,visualTop,.01f);
        assertTrue("Visual height is the opaque art, not the PNG cell",
            npc.visualStandHeight<npc.profile.rootY*npc.profile.worldScale);
        frames(240);
        assertTrue("Opponent head must stay on screen",((CameraRig)get("camera")).top<=visualTop);
    }
    @Test public void opponentAiOnlyStartsAttacksThatCanReach()throws Exception {
        CharacterDefinition.Body target=GeneratedCharacters.defaultCharacter().fighter.body;
        npc().x=590f;
        invoke("setOpponentAiEnabled",new Class<?>[]{boolean.class},true);
        int started=0;
        for(int i=0;i<900;i++) {
            frames(1);
            CombatFighter cpu=npc();
            if(cpu.attacking() && cpu.attackFrame==0 && cpu.move!=null) {
                started++;
                float distance=Math.abs(player().x-cpu.x);
                assertTrue(cpu.attack.id+" started out of reach at "+distance,
                    distance<=CombatRules.maxCenterDistance(cpu.move,target)+0.5f);
            }
        }
        assertTrue("AI should attack once in range",started>0);
    }
    @Test public void lifeButtonsRefillTheTeamAndTheCpuWithoutRestarting()throws Exception {
        FighterState[] team=(FighterState[])get("team");
        team[0].life=0;team[1].life=1234;npc().state.life=10;
        touch(MotionEvent.ACTION_DOWN,1120,250);touch(MotionEvent.ACTION_UP,1120,250);frames(1);
        assertEquals(team[0].profile.maxLife,team[0].life);
        assertEquals(team[1].profile.maxLife,team[1].life);
        assertFalse("A KO'd player fights again",player().ko());
        assertEquals("CPU untouched",10,npc().state.life);
        touch(MotionEvent.ACTION_DOWN,1210,250);touch(MotionEvent.ACTION_UP,1210,250);frames(1);
        assertEquals(npc().state.profile.maxLife,npc().state.life);
    }
    @Test public void walkingIntoTheOpponentPushesInsteadOfOverlapping()throws Exception {
        CharacterDefinition.Body p=GeneratedCharacters.defaultCharacter().fighter.body;
        CharacterDefinition.Body n=npc().body();
        float gap=p.pushHalfWidth+n.pushHalfWidth;
        npc().x=520f;
        touch(MotionEvent.ACTION_DOWN,265,555);frames(60);
        float px=player().x,dx=npc().x;
        assertTrue("Bodies overlap: "+(dx-px),dx-px>=gap-0.01f);
        assertTrue("Opponent should be pushed",dx>520f);
        assertEquals(1,player().facing);
    }
    @Test public void introPlaysBeforeTheFightAndHoldsTheControls()throws Exception {
        invoke("startIntro",new Class<?>[0]);
        float x=player().x;
        press(PadInput.Button.HEAVY,0);frames(1);
        assertEquals("INTRO",motion().clip);
        assertFalse("No attack during the intro",player().attacking());
        int total=(Integer)get("introTotalFrames");
        frames(total);
        assertNotEquals("INTRO",motion().clip);
        assertEquals(x,player().x,0.01f);
        press(PadInput.Button.HEAVY,0);frames(1);
        assertTrue("Fight is on after the intro",player().attacking());
    }
    @Test public void airBlockstunShowsTheAirGuardImpact()throws Exception {
        player().grounded=false;player().y=420f;player().vy=-200f;
        blockstun(CombatFighter.GUARD_AIR);
        frames(1);
        assertEquals("DEFENSE_AIR",motion().clip);
        CharacterDefinition.Animation guard=GeneratedCharacters.defaultCharacter().animation("DEFENSE_AIR");
        assertEquals("Impact frame while in blockstun",
            guard.frame(guard.guardTime(true,player().stunElapsed/(float)player().stunTotal),0),motion().frame());
        assertNotEquals("Not the held guard frame",guard.frame(guard.guardTime(false,0f),0),motion().frame());
    }
    @Test public void knockdownPlaysTheNewFallSequenceOnTheGround()throws Exception {
        player().status=CombatFighter.Status.KNOCKDOWN;player().knockdownFrame=0;frames(1);
        assertEquals("KNOCKDOWN",motion().clip);
        assertEquals("player_base_fall",GeneratedCharacters.defaultCharacter().animation("KNOCKDOWN").atlas.resource);
        player().knockdownFrame=game.engine.config.knockdownFallFrames;frames(1);
        assertEquals("GROUNDED",motion().clip);
    }
    @Test public void launchPoseFollowsThePhysicsAndHitsUseTheNewSheets()throws Exception {
        CombatFighter p=player();
        p.grounded=false;p.y=380f;p.status=CombatFighter.Status.AIR_HITSTUN;p.launched=true;p.stunLeft=p.stunTotal=40;
        p.vy=-1200f;frames(1);
        assertEquals("HIT_AIR",motion().clip);assertEquals("Thrown up",0,motion().frame());
        p.vy=300f;frames(1);
        assertEquals("Recovery tuck on the way down",3,motion().frame());
        setup();player().status=CombatFighter.Status.HITSTUN;player().stunLeft=player().stunTotal=12;frames(1);
        assertEquals("HIT_STAND",motion().clip);
        assertEquals("player_base_hit_stand",GeneratedCharacters.defaultCharacter().animation("HIT_STAND").atlas.resource);
        // The reaction is spread over the hitstun: impact first, back in guard at the end.
        CharacterDefinition.Animation hit=GeneratedCharacters.defaultCharacter().animation("HIT_STAND");
        for(int total:new int[]{12,30}) {
            assertEquals(hit.frame(0f,0),hit.frame(GameView.hitReactionTime(hit,0,total),0));
            assertEquals(hit.frame(hit.duration-.0001f,0),hit.frame(GameView.hitReactionTime(hit,total-1,total),0));
        }
        setup();player().status=CombatFighter.Status.WAKEUP;player().knockdownFrame=0;frames(1);
        assertEquals("GETUP",motion().clip);
        assertEquals("player_base_getup",GeneratedCharacters.defaultCharacter().animation("GETUP").atlas.resource);
    }
    @Test public void matchRenderersShareOneDecodedAtlasCache()throws Exception {
        SpriteFighterRenderer player=(SpriteFighterRenderer)get("spriteFighterRenderer");
        SpriteFighterRenderer opponent=(SpriteFighterRenderer)get("opponentSpriteRenderer");
        Field field=SpriteFighterRenderer.class.getDeclaredField("atlases");field.setAccessible(true);
        SpriteAtlasCache cache=(SpriteAtlasCache)field.get(player);
        assertSame(cache,field.get(opponent));
        java.util.Set<String> expected=new java.util.HashSet<>();
        for(String id:GeneratedCharacters.TEAM)for(CharacterDefinition.Animation a:GeneratedCharacters.get(id).animations.values())expected.add(a.atlas.resource);
        assertEquals(expected.size(),cache.size());
        rawTag();frames(72);
        assertEquals("Tag must not decode new atlases",expected.size(),cache.size());
    }
    @Test public void rendererMirrorsFromDeclaredArtFacing() {
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        Bitmap right=Bitmap.createBitmap(420,320,Bitmap.Config.ARGB_8888),left=Bitmap.createBitmap(420,320,Bitmap.Config.ARGB_8888);
        renderer.motion.clip="LIGHT_JAB";renderer.motion.time=.07f;
        renderer.draw(new Canvas(right),210,270,1,false,false);
        renderer.draw(new Canvas(left),210,270,-1,false,false);
        // At a fractional scale the filtered edge can land one pixel over: compare with the
        // closest of the mirrored pixel and its neighbours.
        for(int y=0;y<320;y+=4)for(int x=4;x<416;x+=4) {
            int want=Color.alpha(right.getPixel(x,y)),best=255;
            for(int dx=-1;dx<=1;dx++)best=Math.min(best,Math.abs(want-Color.alpha(left.getPixel(419-x+dx,y))));
            assertTrue("Mirror differs at "+x+","+y,best<=2);
        }
    }
    @Test public void renderAllFramesUsingRealCanvasAndPackagedAssets()throws Exception {
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        Bitmap sheet=Bitmap.createBitmap(1200,1040,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(sheet);c.drawColor(Color.rgb(43,52,65));
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTextSize(15);p.setColor(Color.WHITE);
        String[] clips={SpriteMotion.Clip.WALK_FORWARD,SpriteMotion.Clip.WALK_BACK,SpriteMotion.Clip.CROUCH,SpriteMotion.Clip.CROUCH,SpriteMotion.Clip.JUMP,SpriteMotion.Clip.FALL,SpriteMotion.Clip.DASH,SpriteMotion.Clip.DASH,SpriteMotion.Clip.BACKDASH,SpriteMotion.Clip.LAND};
        for(int i=0;i<16;i++) {
            SpriteMotion m=renderer.motion;m.time=0;m.distance=0;
            if(i<8){m.clip=i<4?clips[0]:clips[1];m.distance=(i%4)*(i<4?36:32)+1;}
            else {m.clip=clips[i-6];m.time=(i==9?.12f:i==13?.12f:0);}
            // Each clip owns its atlas; the chosen frame must exist in that atlas.
            CharacterDefinition.Animation a=GeneratedCharacters.defaultCharacter().animation(m.clip);
            assertTrue(m.clip+" frame "+m.frame(),m.frame()>=0 && m.frame()<a.atlas.count);
            c.save();c.translate((i%4)*300,(i/4)*260);
            p.setColor(Color.rgb(90,105,115));c.drawLine(0,235,300,235,p);
            renderer.draw(c,150,235,1,false,false);p.setColor(Color.WHITE);c.drawText(i+" "+m.clip,10,20,p);c.restore();
        }
        File dir=new File("build/sprite-review");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,"movement-frames.png"))){assertTrue(sheet.compress(Bitmap.CompressFormat.PNG,100,out));}
        // Whole gameplay screenshot using production stage, HUD, camera and sprite renderer.
        Bitmap scene=Bitmap.createBitmap(1280,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(scene);
        Method draw=GameView.class.getDeclaredMethod("drawScenario",Canvas.class);draw.setAccessible(true);draw.invoke(game,canvas);
        Method fighter=GameView.class.getDeclaredMethod("drawPlayer",Canvas.class);fighter.setAccessible(true);fighter.invoke(game,canvas);
        Method hud=GameView.class.getDeclaredMethod("drawHud",Canvas.class);hud.setAccessible(true);hud.invoke(game,canvas);
        Method controls=GameView.class.getDeclaredMethod("drawControls",Canvas.class);controls.setAccessible(true);controls.invoke(game,canvas);
        try(FileOutputStream out=new FileOutputStream(new File(dir,"gameplay.png"))){scene.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
}
