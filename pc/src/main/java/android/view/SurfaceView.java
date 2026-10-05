package android.view;

import android.content.Context;
import java.awt.Canvas;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashSet;
import java.util.Set;

public class SurfaceView extends Canvas {
    private static final float VW=1280f;
    private static final float VH=720f;
    private static final float DPAD_X=175f;
    private static final float DPAD_Y=555f;
    private static final float DPAD_OFFSET=88f;

    private final SurfaceHolder holder=new SurfaceHolder(this);
    private final Set<Integer> pressed=new HashSet<>();
    private boolean dpadActive;
    private final int mousePointerId=20;

    public SurfaceView(Context context){
        setIgnoreRepaint(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        installKeyboard();
        installMouse();
    }

    public SurfaceHolder getHolder(){return holder;}
    public void setKeepScreenOn(boolean keepScreenOn){}
    public boolean onTouchEvent(MotionEvent event){return false;}

    @Override public void addNotify(){
        super.addNotify();
        requestFocus();
        holder.created();
    }

    @Override public void removeNotify(){
        holder.destroyed();
        super.removeNotify();
    }

    private void installKeyboard(){
        addKeyListener(new KeyAdapter(){
            @Override public void keyPressed(KeyEvent event){
                int code=event.getKeyCode();
                if(!pressed.add(code))return;
                if(isDirectionKey(code)){updateKeyboardDpad();return;}
                switch(code){
                    case KeyEvent.VK_J -> pressButton(2,1005f,598f);
                    case KeyEvent.VK_K -> pressButton(3,1100f,515f);
                    case KeyEvent.VK_L -> pressButton(4,1195f,598f);
                    case KeyEvent.VK_U -> pressButton(5,1100f,650f);
                    case KeyEvent.VK_I -> pressButton(6,930f,505f);
                    case KeyEvent.VK_O -> pressButton(7,905f,620f);
                    case KeyEvent.VK_P -> pressButton(8,1165f,148f);
                    default -> {}
                }
            }

            @Override public void keyReleased(KeyEvent event){
                int code=event.getKeyCode();
                if(!pressed.remove(code))return;
                if(isDirectionKey(code)){updateKeyboardDpad();return;}
                switch(code){
                    case KeyEvent.VK_J -> releaseButton(2,1005f,598f);
                    case KeyEvent.VK_K -> releaseButton(3,1100f,515f);
                    case KeyEvent.VK_L -> releaseButton(4,1195f,598f);
                    case KeyEvent.VK_U -> releaseButton(5,1100f,650f);
                    case KeyEvent.VK_I -> releaseButton(6,930f,505f);
                    case KeyEvent.VK_O -> releaseButton(7,905f,620f);
                    case KeyEvent.VK_P -> releaseButton(8,1165f,148f);
                    default -> {}
                }
            }
        });
    }

    private void installMouse(){
        addMouseListener(new MouseAdapter(){
            @Override public void mousePressed(MouseEvent event){
                requestFocus();
                onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_DOWN,mousePointerId,event.getX(),event.getY()));
            }

            @Override public void mouseReleased(MouseEvent event){
                onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_UP,mousePointerId,event.getX(),event.getY()));
            }

            @Override public void mouseExited(MouseEvent event){
                if((event.getModifiersEx()&MouseEvent.BUTTON1_DOWN_MASK)!=0){
                    onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_CANCEL,mousePointerId,event.getX(),event.getY()));
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent event){
                onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_MOVE,mousePointerId,event.getX(),event.getY()));
            }
        });
    }

    private void updateKeyboardDpad(){
        int horizontal=isPressed(KeyEvent.VK_A,KeyEvent.VK_LEFT)?-1:
            isPressed(KeyEvent.VK_D,KeyEvent.VK_RIGHT)?1:0;
        int vertical=isPressed(KeyEvent.VK_W,KeyEvent.VK_UP)?-1:
            isPressed(KeyEvent.VK_S,KeyEvent.VK_DOWN)?1:0;

        if(horizontal==0&&vertical==0){
            if(dpadActive){
                onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_UP,1,toPixelX(DPAD_X),toPixelY(DPAD_Y)));
                dpadActive=false;
            }
            return;
        }

        float worldX=DPAD_X+horizontal*DPAD_OFFSET;
        float worldY=DPAD_Y+vertical*DPAD_OFFSET;
        int action=dpadActive?MotionEvent.ACTION_MOVE:MotionEvent.ACTION_DOWN;
        onTouchEvent(MotionEvent.desktop(action,1,toPixelX(worldX),toPixelY(worldY)));
        dpadActive=true;
    }

    private boolean isPressed(int first,int second){return pressed.contains(first)||pressed.contains(second);}

    private static boolean isDirectionKey(int code){
        return code==KeyEvent.VK_A||code==KeyEvent.VK_D||code==KeyEvent.VK_W||code==KeyEvent.VK_S
            ||code==KeyEvent.VK_LEFT||code==KeyEvent.VK_RIGHT||code==KeyEvent.VK_UP||code==KeyEvent.VK_DOWN;
    }

    private void pressButton(int pointer,float worldX,float worldY){
        onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_POINTER_DOWN,pointer,toPixelX(worldX),toPixelY(worldY)));
    }

    private void releaseButton(int pointer,float worldX,float worldY){
        onTouchEvent(MotionEvent.desktop(MotionEvent.ACTION_POINTER_UP,pointer,toPixelX(worldX),toPixelY(worldY)));
    }

    private float toPixelX(float worldX){return worldX*Math.max(1,getWidth())/VW;}
    private float toPixelY(float worldY){return worldY*Math.max(1,getHeight())/VH;}
}
