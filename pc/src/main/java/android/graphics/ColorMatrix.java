package android.graphics;
public final class ColorMatrix {
  private final float[] m=new float[20];
  public ColorMatrix(){reset();}
  public ColorMatrix(float[] values){set(values);}
  public void set(float[] values) {
    if(values==null||values.length!=20)
      throw new IllegalArgumentException("ColorMatrix requires exactly 20 entries");
    System.arraycopy(values,0,m,0,20);
  }
  public void reset() {
    java.util.Arrays.fill(m,0f);
    m[0]=m[6]=m[12]=m[18]=1f;
  }
  public void setSaturation(float s) {
    reset();
    float ir=0.213f*(1f-s),ig=0.715f*(1f-s),ib=0.072f*(1f-s);
    m[0]=ir+s;m[1]=ig;m[2]=ib;
    m[5]=ir;m[6]=ig+s;m[7]=ib;
    m[10]=ir;m[11]=ig;m[12]=ib+s;
  }
  public void postConcat(ColorMatrix other) {
    float[] a=m.clone(),b=other.m;
    for(int r=0;r<4;r++){
      for(int col=0;col<5;col++){
        float total=(col==4?a[r*5+4]:0f);
        for(int k=0;k<4;k++)total+=a[r*5+k]*b[k*5+col];
        m[r*5+col]=total;
      }
    }
  }
  public float[] getArray(){return m.clone();}
}