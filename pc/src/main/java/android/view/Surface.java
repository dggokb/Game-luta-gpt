package android.view;

public final class Surface {
    private final SurfaceView view;
    Surface(SurfaceView view){this.view=view;}
    public boolean isValid(){return view.isDisplayable()&&view.getWidth()>0&&view.getHeight()>0;}
}
