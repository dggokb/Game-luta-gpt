package com.gamelutagpt;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GameViewTest {
    private GameView game;
    private long time;

    @Before public void setup() {
        game = new GameView(RuntimeEnvironment.getApplication());
        game.layout(0, 0, 1280, 720);
        time = 10000;
    }

    private Object get(String name) throws Exception {
        Field field = GameView.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }
    private float number(String name) throws Exception { return ((Number)get(name)).floatValue(); }
    private void set(String name, Object value) throws Exception {
        Field field = GameView.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
    private void call(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = GameView.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(game, args);
    }
    private void frames(int count, float seconds) throws Exception {
        for (int i = 0; i < count; i++) {
            call("advanceSimulation", new Class<?>[]{float.class}, seconds);
            time += Math.round(seconds * 1000);
        }
    }
    private void touch(int action, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = xy[i * 2]; coords[i].y = xy[i * 2 + 1];
            coords[i].pressure = 1; coords[i].size = 1;
        }
        MotionEvent e = MotionEvent.obtain(10000, time, action, ids.length, properties, coords,
            0, 0, 1, 1, 0, 0, 0, 0);
        game.onTouchEvent(e);
        e.recycle();
        time += 10;
    }
    private void down(float x, float y) { touch(MotionEvent.ACTION_DOWN, new int[]{0}, x, y); }
    private void move(float x, float y) { touch(MotionEvent.ACTION_MOVE, new int[]{0}, x, y); }
    private void up(float x, float y) { touch(MotionEvent.ACTION_UP, new int[]{0}, x, y); }
    private void tap(float x, float y) { down(x, y); up(x, y); }
    private void button(float dx, float dy, float x, float y) {
        touch(MotionEvent.ACTION_POINTER_DOWN | (1 << 8), new int[]{0, 1}, dx, dy, x, y);
        touch(MotionEvent.ACTION_POINTER_UP | (1 << 8), new int[]{0, 1}, dx, dy, x, y);
    }
    private int projectiles() throws Exception { return ((List<?>)get("energyProjectiles")).size(); }
    private void energyCommand() { down(175, 645); move(255, 635); move(265, 555); }

    @Test public void walkingAndJumpAreStableAcrossRenderRates() throws Exception {
        float[] x = new float[3]; float[] y = new float[3]; float[] vy = new float[3];
        int[] rates = {20, 60, 120};
        for (int i = 0; i < rates.length; i++) {
            setup(); down(245, 485);
            frames(rates[i] / 2, 1f / rates[i]);
            x[i] = number("playerX"); y[i] = number("playerY"); vy[i] = number("velocityY");
        }
        assertEquals(570f, x[0], .1f);
        for (int i = 1; i < 3; i++) {
            assertEquals(x[0], x[i], .1f); assertEquals(y[0], y[i], .1f); assertEquals(vy[0], vy[i], .1f);
        }
    }
    @Test public void heldCrouchThenUpStillSuperJumps() throws Exception {
        down(175, 645); frames(90, 1f / 60f); move(175, 465);
        assertTrue((Boolean)get("superJumping")); assertEquals(-1450, number("velocityY"), .01f);
    }
    @Test public void dashStopsOnReleaseAndBackdashKeepsOnlyItsShortImpulse() throws Exception {
        tap(265, 555); down(265, 555); frames(6, 1f/60);
        assertEquals(482, number("playerX"), .2f);
        up(265, 555); frames(12, 1f/60); assertEquals(482, number("playerX"), .2f);
        tap(85, 555); tap(85, 555); frames(30, 1f/60);
        assertEquals(330, number("playerX"), .2f);
    }
    @Test public void groundAttackCannotBeCutByMashOrJump() throws Exception {
        down(265, 555); button(265, 555, 1195, 598); // H while walking
        float x = number("playerX");
        button(265, 555, 1005, 598); move(245, 485);
        frames(12, 1f/60);
        assertEquals("H", get("attackType")); assertEquals(x, number("playerX"), .01f);
        assertTrue((Boolean)get("grounded"));
    }
    @Test public void recoveryBufferExecutesOnceAndManualInputResetsCombo() throws Exception {
        tap(1195, 598); frames(20, 1f/60); tap(1005, 598); tap(1100, 515);
        assertEquals("H", get("attackType"));
        frames(5, 1f/60); assertEquals("L", get("attackType"));
        frames(12, 1f/60); assertEquals("", get("attackType"));
        tap(1100, 650); assertEquals(1, ((Number)get("autoComboIndex")).intValue());
        frames(12, 1f/60); tap(1100, 515); assertEquals(0, ((Number)get("autoComboIndex")).intValue());
    }
    @Test public void autoComboUsesBothProfilesAndCrouchingNormals() throws Exception {
        String[][] expected = {{"L", "M", "H"}, {"L", "L", "H", "M"}};
        for (int fighter = 0; fighter < 2; fighter++) {
            down(175, 645);
            for (String type : expected[fighter]) {
                button(175, 645, 1100, 650); assertEquals("2" + type, get("attackType"));
                frames(25, 1f/60);
            }
            up(175, 645); tap(930, 505);
        }
    }
    @Test public void crouchAttackKeepsPoseWhenThumbReleased() throws Exception {
        down(175, 645); button(175, 645, 1100, 515); up(175, 645); frames(8, 1f/60);
        assertEquals("2M", get("attackType")); assertTrue((Boolean)get("attackCrouched"));
        assertTrue(number("crouchBlend") > .95f);
    }
    @Test public void normalAirAttackPreservesSteeringAndGravity() throws Exception {
        down(245, 485); button(245, 485, 1195, 598);
        frames(12, 1f/60); assertEquals(480, number("playerX"), .1f);
        assertEquals(-330, number("velocityY"), .1f); assertFalse((Boolean)get("grounded"));
    }
    @Test public void landingReevaluatesHeldDownWithoutAnotherMoveEvent() throws Exception {
        down(245, 485); frames(6, 1f/60); move(255, 635);
        frames(60, 1f/60); assertTrue((Boolean)get("grounded")); assertTrue((Boolean)get("crouching"));
        float x = number("playerX"); frames(30, 1f/60); assertEquals(x, number("playerX"), .01f);
    }
    @Test public void airEnergyLocksXButThumbCanChangeDuringRecovery() throws Exception {
        down(175, 645); move(245, 485); frames(6, 1f/60);
        move(175, 645); move(255, 635); move(265, 555); button(265, 555, 1005, 598);
        assertEquals("S", get("attackType")); assertEquals(1, projectiles());
        float x = number("playerX"), vy = number("velocityY");
        move(85, 555); frames(12, 1f/60);
        assertEquals(x, number("playerX"), .01f); assertEquals(vy + 39.6f, number("velocityY"), .1f);
        frames(12, 1f/60); assertTrue(number("playerX") < x - 20f);
    }
    @Test public void energyAllowsOnlyOneProjectilePerOwnerAndCommandExpires() throws Exception {
        energyCommand(); button(265, 555, 1005, 598); up(265, 555);
        frames(20, 1f/60); energyCommand(); button(265, 555, 1195, 598);
        assertEquals(1, projectiles()); assertEquals("H", get("attackType")); up(265, 555);
        frames(120, 1f/60); assertEquals(0, projectiles());
        energyCommand(); time += 600; button(265, 555, 1005, 598);
        assertEquals("L", get("attackType")); assertEquals(0, projectiles());
    }
    @Test public void tagPreservesAirTrajectoryLifeAndCancelsBufferedAttack() throws Exception {
        call("applyDamage", new Class<?>[]{int.class}, 1400);
        down(245, 485); button(245, 485, 1195, 598); frames(20, 1f/60);
        button(245, 485, 1005, 598);
        float x = number("playerX"), y = number("playerY"), vy = number("velocityY");
        button(245, 485, 930, 505);
        assertEquals(x, number("playerX"), 0); assertEquals(y, number("playerY"), 0); assertEquals(vy, number("velocityY"), 0);
        assertEquals("", get("attackType")); assertNull(get("bufferedAttack"));
        button(245, 485, 930, 505);
        Object fighter = ((Object[])get("team"))[0];
        Field life = fighter.getClass().getDeclaredField("life"); life.setAccessible(true);
        assertEquals(8600, life.getInt(fighter));
    }
    @Test public void cancelAndPauseClearPointersAndPendingCommands() throws Exception {
        energyCommand(); touch(MotionEvent.ACTION_CANCEL, new int[]{0}, 265, 555);
        assertEquals(-1, ((Number)get("dpadPointer")).intValue());
        tap(1005, 598); assertEquals("L", get("attackType"));
        down(265, 555); game.pauseGame();
        assertFalse((Boolean)get("movingRight")); assertNull(get("bufferedAttack"));
        assertEquals(-1, ((Number)get("lightPointer")).intValue());
        game.resumeGame(); // No surface: must not accidentally spawn a loop.
        assertFalse((Boolean)get("running"));
    }
    @Test public void boundsAndCameraRemainInsideStage() throws Exception {
        down(265, 555); frames(600, 1f/60); assertEquals(2510, number("playerX"), .01f);
        move(85, 555); frames(1000, 1f/60); assertEquals(90, number("playerX"), .01f);
        assertTrue(number("cameraX") >= 1280f/1.12f/2f - .01f);
        assertTrue(number("cameraTop") >= -520f && number("cameraTop") <= 72f);
    }
    @Test public void multitouchReleaseDoesNotReleaseTheOtherControl() throws Exception {
        down(265, 555);
        touch(MotionEvent.ACTION_POINTER_DOWN | (1 << 8), new int[]{0, 7}, 265, 555, 1005, 598);
        touch(MotionEvent.ACTION_POINTER_UP, new int[]{0, 7}, 265, 555, 1005, 598);
        assertFalse((Boolean)get("movingRight")); assertEquals(7, ((Number)get("lightPointer")).intValue());
        touch(MotionEvent.ACTION_UP, new int[]{7}, 1005, 598);
        assertEquals(-1, ((Number)get("lightPointer")).intValue());
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void renderAllExistingMovesForVisualReview() throws Exception {
        String[] names = {"PARADO", "ANDANDO", "DASH", "SUBINDO", "CAINDO", "AGACHADO", "L", "M", "H", "2L", "2M", "2H", "S", "S NO AR"};
        Bitmap sheet = Bitmap.createBitmap(1820, 820, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(sheet); canvas.drawColor(Color.rgb(27, 39, 59));
        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG); label.setColor(Color.WHITE); label.setTextSize(17);
        for (int i = 0; i < names.length; i++) {
            setup(); set("playerX", 100f); set("playerY", 195f);
            if (i == 1 || i == 2) { set("movingRight", true); set("locomotionBlend", 1f); set("walkTime", 1.1f); }
            if (i == 2) set("forwardDashing", true);
            if (i == 3 || i == 4 || i == 13) { set("grounded", false); set("velocityY", i == 4 ? 300f : -400f); }
            if (i == 5 || (i >= 9 && i <= 11)) set("crouchBlend", 1f);
            if (i >= 6) {
                String attack = i >= 12 ? "S" : names[i];
                call("startAttack", new Class<?>[]{String.class}, attack);
                set("attackTimer", number("attackDuration") * .58f);
            }
            canvas.save(); canvas.translate((i % 7) * 260, (i / 7) * 410);
            canvas.drawText(names[i], 12, 28, label);
            call("drawPlayer", new Class<?>[]{Canvas.class}, canvas);
            // Recovery/anticipation pose below the extension pose.
            canvas.translate(0, 190); set("playerY", 195f);
            if (i >= 6) set("attackTimer", number("attackDuration") * .9f);
            call("drawPlayer", new Class<?>[]{Canvas.class}, canvas);
            canvas.restore();
        }
        File output = new File("build/astra-previews/moves.png"); output.getParentFile().mkdirs();
        try (FileOutputStream stream = new FileOutputStream(output)) { sheet.compress(Bitmap.CompressFormat.PNG, 100, stream); }
        assertTrue(output.length() > 10000);
    }
}
