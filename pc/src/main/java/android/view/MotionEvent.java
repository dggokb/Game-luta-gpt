package android.view;

public final class MotionEvent {
    public static final int ACTION_DOWN=0;
    public static final int ACTION_UP=1;
    public static final int ACTION_MOVE=2;
    public static final int ACTION_CANCEL=3;
    public static final int ACTION_POINTER_DOWN=5;
    public static final int ACTION_POINTER_UP=6;

    private final int action,pointerId;
    private final float x,y;
    private final long eventTime;

    private MotionEvent(int action,int pointerId,float x,float y,long eventTime){
        this.action=action;this.pointerId=pointerId;this.x=x;this.y=y;this.eventTime=eventTime;
    }

    public static MotionEvent desktop(int action,int pointerId,float x,float y){
        return new MotionEvent(action,pointerId,x,y,System.nanoTime()/1_000_000L);
    }

    public static MotionEvent obtain(MotionEvent other){
        return new MotionEvent(other.action,other.pointerId,other.x,other.y,other.eventTime);
    }

    public int getActionMasked(){return action;}
    public int getActionIndex(){return 0;}
    public int getPointerId(int index){return pointerId;}
    public float getX(int index){return x;}
    public float getY(int index){return y;}
    public long getEventTime(){return eventTime;}
    public int findPointerIndex(int id){return id==pointerId?0:-1;}
    public void recycle(){}
}
