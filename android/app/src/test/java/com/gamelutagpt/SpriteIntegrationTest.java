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
        assertEquals(first.moves.keySet(),second.moves.keySet());
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
            assertEquals(a.reach,b.reach,.0001f);
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

        set("grounded",true);set("crouching",true);
        invoke("startAttack",new Class<?>[]{String.class},"2L");frames(1);
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

        setup();set("grounded",true);set("crouching",true);
        invoke("startAttack",new Class<?>[]{String.class},"2M");frames(1);
        assertEquals("CROUCH_MEDIUM",motion().clip);
        assertEquals(0,motion().frame());
        frames(5);
        assertTrue("2M must advance through authored frames",motion().frame() >= 1);

        setup();set("grounded",true);set("crouching",true);
        invoke("startAttack",new Class<?>[]{String.class},"2H");frames(1);
        assertEquals(SpriteMotion.Clip.CROUCH,motion().clip);

        setup();set("grounded",false);set("playerY",430f);set("velocityY",-300f);
        invoke("startAttack",new Class<?>[]{String.class},"H");frames(1);
        assertEquals(SpriteMotion.Clip.JUMP,motion().clip);

        setup();set("playerBlockstunTimer",.12f);set("playerLastGuardState",2);
        set("playerMovementLocked",true);frames(1);
        assertEquals("DEFENSE_CROUCH",motion().clip);

        setup();set("playerKnockdownState",1);set("playerMovementLocked",true);frames(1);
        assertEquals("KNOCKDOWN",motion().clip);
    }

    @Test public void opponentIsRenderedByTheGenericSpriteEngine()throws Exception {
        SpriteFighterRenderer opponent=(SpriteFighterRenderer)get("opponentSpriteRenderer");
        Field field=SpriteFighterRenderer.class.getDeclaredField("character");
        field.setAccessible(true);
        assertEquals("monster_npc",((CharacterDefinition)field.get(opponent)).id);
        frames(2);
        assertEquals(SpriteMotion.Clip.IDLE,opponent.motion.clip);
    }

    @Test public void realTagSwitchLoadsSecondPackAndAllStandingAttacks()throws Exception {
        assertEquals("player_base",rendererCharacterId());
        invoke("switchFighter",new Class<?>[]{});
        frames(72);
        assertEquals(1,(int)get("activeFighterIndex"));
        assertEquals("player_two",rendererCharacterId());

        String[] bindings={"L","M","H"};
        int[] recoveryFrames={12,18,26};
        CharacterDefinition second=GeneratedCharacters.get("player_two");
        for(int i=0;i<bindings.length;i++) {
            invoke("startAttack",new Class<?>[]{String.class},bindings[i]);
            frames(1);
            assertEquals(second.moves.get(bindings[i]).animation.id,motion().clip);
            frames(recoveryFrames[i]);
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
        touch(MotionEvent.ACTION_MOVE,175,645);frames(60);assertEquals(SpriteMotion.Clip.CROUCH,motion().clip);assertEquals(9,motion().frame());
    }
    @Test public void facingChangesSelectForwardInBothDirections()throws Exception {
        set("playerX",1400f);set("dummyX",1000f);set("facingDirection",-1);
        touch(MotionEvent.ACTION_DOWN,85,555);frames(6);assertEquals(SpriteMotion.Clip.WALK_FORWARD,motion().clip);
    }
    @Test public void pauseClearsHeldInputWithoutStartingAnotherLoop()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);frames(5);game.pauseGame();assertFalse((Boolean)get("movingRight"));
        game.resumeGame();assertFalse((Boolean)get("running"));frames(1);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    @Test public void touchIsQueuedAndLatestMoveWinsWithoutBuildingBacklog()throws Exception {
        touch(MotionEvent.ACTION_DOWN,265,555);
        assertFalse((Boolean)get("movingRight"));
        for(int i=0;i<80;i++)touch(MotionEvent.ACTION_MOVE,265-i*2.25f,555);
        frames(1);
        assertTrue((Boolean)get("movingLeft"));
        assertFalse((Boolean)get("movingRight"));
        touch(MotionEvent.ACTION_UP,85,555);frames(1);
        assertFalse((Boolean)get("movingLeft"));
    }
    @Test public void powerGaugeRequiresAConfirmedHitAndGuardBuildsMeter()throws Exception {
        set("dummyX",1000f);
        invoke("startAttack",new Class<?>[]{String.class},"L");frames(40);
        assertEquals(0f,meter(activeFighter()),.001f);

        set("dummyX",520f);
        invoke("startAttack",new Class<?>[]{String.class},"L");frames(20);
        assertEquals(.10f,meter(activeFighter()),.001f);

        invoke("fireEnergyAttack",new Class<?>[]{String.class},"M");
        assertEquals(.10f,meter(activeFighter()),.001f);
        frames(1);assertEquals(.55f,meter(activeFighter()),.001f);
        frames(30);

        set("dpadDirection",5);
        invoke("applyPlayerHit",new Class<?>[]{int.class,int.class,String.class,boolean.class,boolean.class},300,-1,"L",false,false);
        assertEquals(.60f,meter(activeFighter()),.001f);
        assertEquals(0f,meter(get("opponentFighter")),.001f);

        set("dpadDirection",0);set("playerMovementLocked",false);set("playerBlockstunTimer",0f);
        invoke("applyPlayerHit",new Class<?>[]{int.class,int.class,String.class,boolean.class,boolean.class},500,-1,"M",false,false);
        assertEquals(.15f,meter(get("opponentFighter")),.001f);
    }
    @Test public void standingMoveDamageMatchesActiveWindowOnceInBothDirections()throws Exception {
        for(String binding:new String[]{"L","M","H"})for(int direction:new int[]{1,-1}) {
            setup();set("facingDirection",direction);set("dummyX",420f+100f*direction);
            invoke("startAttack",new Class<?>[]{String.class},binding);
            CharacterDefinition.Move move=GeneratedCharacters.defaultCharacter().moves.get(binding);
            int life=(Integer)get("dummyLife");
            set("attackTimer",move.totalTime-(move.activeStart-.002f));
            invoke("tryApplyMeleeDamage",new Class<?>[]{});
            assertEquals(life,(int)get("dummyLife"));
            set("attackTimer",move.totalTime-(move.activeStart+.002f));
            invoke("tryApplyMeleeDamage",new Class<?>[]{});
            assertEquals(life-move.damage,(int)get("dummyLife"));
            invoke("tryApplyMeleeDamage",new Class<?>[]{});
            assertEquals(life-move.damage,(int)get("dummyLife"));
        }
    }
    @Test public void missedMoveCannotHitWhenOpponentArrivesDuringRecovery()throws Exception {
        for(String binding:new String[]{"L","M","H"}) {
            setup();set("dummyX",1500f);
            invoke("startAttack",new Class<?>[]{String.class},binding);
            CharacterDefinition.Move move=GeneratedCharacters.defaultCharacter().moves.get(binding);
            int life=(Integer)get("dummyLife");
            set("attackTimer",move.totalTime-(move.activeStart+.002f));
            invoke("tryApplyMeleeDamage",new Class<?>[]{});
            set("dummyX",520f);set("attackTimer",move.totalTime-(move.activeEnd+.002f));
            invoke("tryApplyMeleeDamage",new Class<?>[]{});
            assertEquals(life,(int)get("dummyLife"));
        }
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
    @Test public void standingLightAttackPlaysJabStartupActiveRecoveryThenReturnsIdle()throws Exception {
        invoke("startAttack",new Class<?>[]{String.class},"L");
        frames(1);assertEquals("LIGHT_JAB",motion().clip);assertEquals(0,motion().frame());
        frames(2);assertEquals(1,motion().frame());
        frames(4);assertEquals(2,motion().frame());
        frames(5);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
    }
    @Test public void standingMediumAttackUsesThreeFrameKickAtScaleOne()throws Exception {
        invoke("startAttack",new Class<?>[]{String.class},"M");
        frames(1);assertEquals("MEDIUM_KICK",motion().clip);assertEquals(0,motion().frame());
        frames(4);assertEquals(1,motion().frame());
        frames(7);assertEquals(2,motion().frame());
        frames(5);assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
        assertEquals(1f,((SpriteFighterRenderer)get("spriteFighterRenderer")).visualProfile().worldScale,.001f);
    }
    @Test public void mediumKickUsesWideCanvasWithoutShrinkingCharacter() {
        Bitmap kick=BitmapFactory.decodeResource(RuntimeEnvironment.getApplication().getResources(),R.drawable.player_base_medium_kick);
        assertNotNull(kick);
        int fw=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_WIDTH;
        int fh=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_HEIGHT;
        int countFrames=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_COUNT;
        assertEquals(fw*countFrames,kick.getWidth());
        assertEquals(fh,kick.getHeight());
        assertTrue(GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_X>0 && GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_X<fw);
        assertTrue(GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_Y>0 && GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_Y<fh);
        for(int i=0;i<countFrames;i++) {
            int count=0,minX=fw,minY=fh,maxX=-1,maxY=-1;
            for(int y=0;y<fh;y++)for(int x=0;x<fw;x++) {
                if(Color.alpha(kick.getPixel(i*fw+x,y))>10) {
                    count++;
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
            }
            assertTrue("Empty medium kick frame "+i,count>10000);
            assertTrue("Kick clipped left "+i,minX>=8);
            assertTrue("Kick clipped right "+i,fw-1-maxX>=8);
            assertTrue("Kick clipped top "+i,minY>=8);
            assertTrue("Kick clipped bottom "+i,fh-1-maxY>=8);
        }
    }
    @Test public void productionRendererKeepsMediumKickInsideGeneratedCanvas()throws Exception {
        SpriteFighterRenderer renderer=
            new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        int fw=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_WIDTH;
        int fh=GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_HEIGHT;
        int rx=GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_X;
        int ry=GeneratedSpriteLayouts.MEDIUM_KICK_ROOT_Y;

        Bitmap review=Bitmap.createBitmap(
            fw*GeneratedSpriteLayouts.MEDIUM_KICK_FRAME_COUNT,
            fh,
            Bitmap.Config.ARGB_8888
        );
        Canvas canvas=new Canvas(review);
        canvas.drawColor(Color.rgb(32,36,44));
        float[] times={.016f,.090f,.220f};

        for(int i=0;i<times.length;i++) {
            renderer.motion.clip="MEDIUM_KICK";
            renderer.motion.time=times[i];
            assertEquals(i,renderer.motion.frame());
            canvas.save();
            canvas.translate(i*fw,0);
            renderer.draw(canvas,rx,ry,1,false,false);
            canvas.restore();
        }

        for(int i=0;i<times.length;i++) {
            int minX=fw,minY=fh,maxX=-1,maxY=-1;
            for(int y=0;y<fh;y++)for(int x=0;x<fw;x++) {
                int pixel=review.getPixel(i*fw+x,y);
                if(pixel!=Color.rgb(32,36,44)) {
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
            }
            assertTrue("Rendered kick clipped left "+i,minX>=8);
            assertTrue("Rendered kick clipped right "+i,fw-1-maxX>=8);
            assertTrue("Rendered kick clipped top "+i,minY>=8);
            assertTrue("Rendered kick clipped bottom "+i,fh-1-maxY>=8);
        }

        File dir=new File("build/sprite-review");
        dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(
            new File(dir,"medium-kick-render.png")
        )) {
            assertTrue(review.compress(Bitmap.CompressFormat.PNG,100,out));
        }
    }

    @Test public void standingHeavyAttackUsesNineFrameStraightAtScaleOne()throws Exception {
        invoke("startAttack",new Class<?>[]{String.class},"H");
        frames(1);
        assertEquals("HEAVY_STRAIGHT",motion().clip);
        assertEquals(0,motion().frame());
        frames(25);
        assertEquals(SpriteMotion.Clip.IDLE,motion().clip);
        assertEquals(
            1f,
            ((SpriteFighterRenderer)get("spriteFighterRenderer"))
                .visualProfile().worldScale,
            .001f
        );
    }

    @Test public void heavyStraightUsesGeneratedCanvasWithoutClipping() {
        Bitmap heavy=BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().getResources(),
            R.drawable.player_base_heavy_straight
        );
        assertNotNull(heavy);
        int fw=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_WIDTH;
        int fh=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_HEIGHT;
        int countFrames=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_COUNT;
        assertEquals(9,countFrames);
        assertEquals(fw*countFrames,heavy.getWidth());
        assertEquals(fh,heavy.getHeight());

        for(int i=0;i<countFrames;i++) {
            int count=0,minX=fw,minY=fh,maxX=-1,maxY=-1;
            for(int y=0;y<fh;y++)for(int x=0;x<fw;x++) {
                if(Color.alpha(heavy.getPixel(i*fw+x,y))>10) {
                    count++;
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
            }
            assertTrue("Empty heavy straight frame "+i,count>10000);
            assertTrue("Heavy clipped left "+i,minX>=8);
            assertTrue("Heavy clipped right "+i,fw-1-maxX>=8);
            assertTrue("Heavy clipped top "+i,minY>=8);
            assertTrue("Heavy clipped bottom "+i,fh-1-maxY>=8);
        }
    }

    @Test public void productionRendererKeepsHeavyStraightInsideGeneratedCanvas()throws Exception {
        SpriteFighterRenderer renderer=
            new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        int fw=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_WIDTH;
        int fh=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_HEIGHT;
        int rx=GeneratedSpriteLayouts.HEAVY_STRAIGHT_ROOT_X;
        int ry=GeneratedSpriteLayouts.HEAVY_STRAIGHT_ROOT_Y;
        int count=GeneratedSpriteLayouts.HEAVY_STRAIGHT_FRAME_COUNT;

        Bitmap review=Bitmap.createBitmap(fw*count,fh,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(review);
        canvas.drawColor(Color.rgb(32,36,44));
        float[] times={.016f,.050f,.090f,.130f,.170f,.220f,.270f,.320f,.370f};

        for(int i=0;i<count;i++) {
            renderer.motion.clip="HEAVY_STRAIGHT";
            renderer.motion.time=times[i];
            assertEquals(i,renderer.motion.frame());
            canvas.save();
            canvas.translate(i*fw,0);
            renderer.draw(canvas,rx,ry,1,false,false);
            canvas.restore();
        }

        int background=Color.rgb(32,36,44);
        for(int i=0;i<count;i++) {
            int minX=fw,minY=fh,maxX=-1,maxY=-1;
            for(int y=0;y<fh;y++)for(int x=0;x<fw;x++) {
                if(review.getPixel(i*fw+x,y)!=background) {
                    minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                    minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                }
            }
            assertTrue("Rendered heavy clipped left "+i,minX>=8);
            assertTrue("Rendered heavy clipped right "+i,fw-1-maxX>=8);
            assertTrue("Rendered heavy clipped top "+i,minY>=8);
            assertTrue("Rendered heavy clipped bottom "+i,fh-1-maxY>=8);
        }

        File dir=new File("build/sprite-review");
        dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(
            new File(dir,"heavy-straight-render.png")
        )) {
            assertTrue(review.compress(Bitmap.CompressFormat.PNG,100,out));
        }
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
        Bitmap movement=BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().getResources(),
            R.drawable.player_base_movement
        );
        Bitmap jab=BitmapFactory.decodeResource(
            RuntimeEnvironment.getApplication().getResources(),
            R.drawable.player_base_jab
        );

        assertNotNull(idle);assertNotNull(movement);assertNotNull(jab);
        assertEquals(4*GeneratedSpriteLayouts.IDLE_FRAME_WIDTH,idle.getWidth());
        assertEquals(2*GeneratedSpriteLayouts.IDLE_FRAME_HEIGHT,idle.getHeight());
        assertEquals(4*GeneratedSpriteLayouts.MOVEMENT_FRAME_WIDTH,movement.getWidth());
        assertEquals(4*GeneratedSpriteLayouts.MOVEMENT_FRAME_HEIGHT,movement.getHeight());
        assertEquals(3*GeneratedSpriteLayouts.JAB_FRAME_WIDTH,jab.getWidth());
        assertEquals(GeneratedSpriteLayouts.JAB_FRAME_HEIGHT,jab.getHeight());
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
        assertEquals(256f,a.height(),.001f);
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
        assertEquals(1f,renderer.visualProfile().worldScale,.001f);

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
        Bitmap atlas=BitmapFactory.decodeResource(RuntimeEnvironment.getApplication().getResources(),R.drawable.player_base_movement);
        assertNotNull(atlas);assertTrue(atlas.hasAlpha());
        int fw=GeneratedSpriteLayouts.MOVEMENT_FRAME_WIDTH,fh=GeneratedSpriteLayouts.MOVEMENT_FRAME_HEIGHT;
        assertEquals(4*fw,atlas.getWidth());assertEquals(4*fh,atlas.getHeight());
        for(int i=0;i<16;i++) {
            int left=(i%4)*fw,top=(i/4)*fh,count=0;
            for(int y=top;y<top+fh;y++)for(int x=left;x<left+fw;x++)if(Color.alpha(atlas.getPixel(x,y))>128)count++;
            assertTrue("Empty sprite "+i,count>4500);assertTrue("Opaque background "+i,count<fw*fh*.70f);
        }
    }
    private int activeLife()throws Exception {Field f=activeFighter().getClass().getDeclaredField("life");f.setAccessible(true);return f.getInt(activeFighter());}

    @Test public void opponentMoveUsesItsPackActiveWindowAndOwnLowAttackClip()throws Exception {
        CharacterDefinition npc=GeneratedCharacters.opponentCharacter();
        CharacterDefinition.Move move=npc.moves.get("L");
        set("dummyX",520f);set("opponentAiEnabled",true);
        invoke("startOpponentAttack",new Class<?>[]{String.class},"L");
        int life=activeLife();
        set("dummyAttackTimer",move.totalTime-(move.activeStart-.002f));
        invoke("tryApplyOpponentMeleeDamage",new Class<?>[]{});
        assertEquals("Startup must not hit",life,activeLife());
        set("dummyAttackTimer",move.totalTime-(move.activeStart+.002f));
        invoke("tryApplyOpponentMeleeDamage",new Class<?>[]{});
        assertEquals(life-move.damage,activeLife());

        setup();set("dummyX",520f);set("opponentAiEnabled",true);
        invoke("startOpponentAttack",new Class<?>[]{String.class},"2M");
        invoke("updateOpponentSpriteMotion",new Class<?>[]{float.class,float.class},.016f,0f);
        SpriteFighterRenderer opponent=(SpriteFighterRenderer)get("opponentSpriteRenderer");
        assertEquals(npc.moves.get("2M").animation.id,opponent.motion.clip);
        assertNotEquals(npc.moves.get("M").animation.id,opponent.motion.clip);
    }
    @Test public void cameraAndHudUseMeasuredSpriteHeight()throws Exception {
        CharacterDefinition npc=GeneratedCharacters.opponentCharacter();
        Method top=GameView.class.getDeclaredMethod("opponentVisualTop");top.setAccessible(true);
        float visualTop=(Float)top.invoke(game);
        assertEquals((Float)get("dummyY")-npc.visualStandHeight,visualTop,.01f);
        assertTrue("Visual height is the opaque art, not the PNG cell",
            npc.visualStandHeight<npc.profile.rootY*npc.profile.worldScale);
        frames(240);
        assertTrue("Opponent head must stay on screen",((CameraRig)get("camera")).top<=visualTop);
    }
    @Test public void opponentAiOnlyStartsAttacksThatCanReach()throws Exception {
        CharacterDefinition npc=GeneratedCharacters.opponentCharacter();
        CharacterDefinition.Body target=GeneratedCharacters.defaultCharacter().fighter.body;
        set("dummyX",590f);set("opponentAiEnabled",true);
        int started=0;String previous="";
        for(int i=0;i<900;i++) {
            frames(1);
            String type=(String)get("dummyAttackType");
            if(!type.isEmpty() && !type.equals(previous) && !"S".equals(type)) {
                started++;
                CharacterDefinition.Move move=(CharacterDefinition.Move)get("opponentMove");
                float distance=Math.abs((Float)get("playerX")-(Float)get("dummyX"));
                assertTrue(type+" started out of reach at "+distance,
                    distance<=CombatRules.maxCenterDistance(move,target)+0.5f);
            }
            previous=type;
        }
        assertTrue("AI should attack once in range",started>0);
    }
    @Test public void walkingIntoTheOpponentPushesInsteadOfOverlapping()throws Exception {
        CharacterDefinition.Body p=GeneratedCharacters.defaultCharacter().fighter.body;
        CharacterDefinition.Body n=GeneratedCharacters.opponentCharacter().fighter.body;
        float gap=p.pushHalfWidth+n.pushHalfWidth;
        set("dummyX",520f);
        touch(MotionEvent.ACTION_DOWN,265,555);frames(60);
        float px=(Float)get("playerX"),dx=(Float)get("dummyX");
        assertTrue("Bodies overlap: "+(dx-px),dx-px>=gap-0.01f);
        assertTrue("Opponent should be pushed",dx>520f);
        assertEquals(1,(int)get("facingDirection"));
    }
    @Test public void matchRenderersShareOneDecodedAtlasCache()throws Exception {
        SpriteFighterRenderer player=(SpriteFighterRenderer)get("spriteFighterRenderer");
        SpriteFighterRenderer opponent=(SpriteFighterRenderer)get("opponentSpriteRenderer");
        Field field=SpriteFighterRenderer.class.getDeclaredField("atlases");field.setAccessible(true);
        SpriteAtlasCache cache=(SpriteAtlasCache)field.get(player);
        assertSame(cache,field.get(opponent));
        java.util.Set<String> expected=new java.util.HashSet<>();
        for(String id:GeneratedCharacters.TEAM)for(CharacterDefinition.Animation a:GeneratedCharacters.get(id).animations.values())expected.add(a.atlas.resource);
        for(CharacterDefinition.Animation a:GeneratedCharacters.opponentCharacter().animations.values())expected.add(a.atlas.resource);
        assertEquals(expected.size(),cache.size());
        invoke("switchFighter",new Class<?>[]{});frames(72);
        assertEquals("Tag must not decode new atlases",expected.size(),cache.size());
    }
    @Test public void rendererMirrorsFromDeclaredArtFacing() {
        SpriteFighterRenderer renderer=new SpriteFighterRenderer(RuntimeEnvironment.getApplication());
        Bitmap right=Bitmap.createBitmap(420,320,Bitmap.Config.ARGB_8888),left=Bitmap.createBitmap(420,320,Bitmap.Config.ARGB_8888);
        renderer.motion.clip="LIGHT_JAB";renderer.motion.time=.07f;
        renderer.draw(new Canvas(right),210,270,1,false,false);
        renderer.draw(new Canvas(left),210,270,-1,false,false);
        for(int y=0;y<320;y+=4)for(int x=0;x<420;x+=4)
            assertEquals(Color.alpha(right.getPixel(x,y)),Color.alpha(left.getPixel(419-x,y)),2);
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
            assertEquals(i,m.frame());
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
