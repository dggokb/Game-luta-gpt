package com.gamelutagpt;

import android.opengl.Matrix;
import java.io.File;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class Fighter3DTest {
    @Test public void idleBreathesWithoutWalking() {
        Fighter3DState s=new Fighter3DState(); FighterPrototype01Rig r=new FighterPrototype01Rig();
        FighterPrototype01Animator a=new FighterPrototype01Animator();
        a.apply(s,r); float y=r.pose.ty[1]; s.animationTime=.5f; a.apply(s,r);
        assertTrue(Math.abs(r.pose.ty[1]-y)>.5f);
    }
    @Test public void crouchAttackDoesNotDoubleLowerThePelvis() {
        Fighter3DState s=new Fighter3DState(); FighterPrototype01Rig r=new FighterPrototype01Rig();
        s.crouching=true; s.attackType="2L"; s.attackPhase=1;
        new FighterPrototype01Animator().apply(s,r); assertEquals(28,r.pose.ty[1],.01);
    }
    @Test public void snapshotCopyIsIndependent() {
        Fighter3DState a=new Fighter3DState(),b=new Fighter3DState();
        a.playerX=12; a.attackType="H"; a.copyTo(b); a.playerX=55;
        assertEquals(12,b.playerX,0); assertEquals("H",b.attackType);
    }
    private static JSONArray array(float[] values) throws Exception {
        JSONArray out=new JSONArray(); for(float v:values) { assertTrue(Float.isFinite(v)); out.put(v); } return out;
    }
    private static float[] data(FighterPrototype01Model.Mesh mesh,String name) throws Exception {
        Field f=mesh.getClass().getDeclaredField(name);f.setAccessible(true);
        FloatBuffer b=((FloatBuffer)f.get(mesh)).duplicate();b.position(0);
        float[] out=new float[b.remaining()];b.get(out);return out;
    }
    @Test public void exportAndValidateActualProductionMeshesAndPoses() throws Exception {
        JSONObject output=new JSONObject();JSONArray meshes=new JSONArray(),poses=new JSONArray();
        IdentityHashMap<FighterPrototype01Model.Mesh,Integer> ids=new IdentityHashMap<>();
        FighterPrototype01Model model=new FighterPrototype01Model();
        FighterPrototype01Rig rig=new FighterPrototype01Rig();FighterPrototype01Animator animator=new FighterPrototype01Animator();
        String[] names={"GUARDA","PASSO","AGACHADO","PULO","L","M","H","2L","2M","2H","S","DEFESA","ESQUERDA","COSTAS"};
        for(int i=0;i<names.length;i++) {
            Fighter3DState s=new Fighter3DState();s.animationTime=.4f;
            if(i==1)s.walkTime=1.1f;if(i==2)s.crouching=true;if(i==3)s.airborne=true;
            if(i>=4&&i<=10){s.attackType=names[i];s.attackPhase=1;s.crouching=i>=7&&i<=9;}
            if(i==11)s.guardPose=Fighter3DState.GUARD_HIGH;
            animator.apply(s,rig);
            float[] root=new float[16],vp=new float[16];Matrix.setIdentityM(root,0);
            if(i==12)Matrix.scaleM(root,0,-1,1,1);
            Matrix.rotateM(root,0,i==13?160:25,0,1,0);
            Matrix.orthoM(vp,0,-140,140,35,-245,-1000,1000);
            JSONArray parts=new JSONArray();
            model.draw(new FighterPrototype01Model.DrawTarget(){
                public void begin(){} public void end(){}
                public void draw(FighterPrototype01Model.Mesh mesh,float[] matrix,float[] projection,float r,float g,float b,float alpha){
                    try {
                        if(!ids.containsKey(mesh)) {
                            float[] p=data(mesh,"positions"),n=data(mesh,"normals");
                            assertEquals(p.length,n.length);assertEquals(0,p.length%9);
                            for(int j=0;j<p.length;j+=9) {
                                float ux=p[j+3]-p[j],uy=p[j+4]-p[j+1],uz=p[j+5]-p[j+2];
                                float vx=p[j+6]-p[j],vy=p[j+7]-p[j+1],vz=p[j+8]-p[j+2];
                                float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
                                assertTrue("outward winding",nx*(n[j]+n[j+3]+n[j+6])+ny*(n[j+1]+n[j+4]+n[j+7])+nz*(n[j+2]+n[j+5]+n[j+8])>0);
                            }
                            for(int j=0;j<n.length;j+=3) assertEquals(1,n[j]*n[j]+n[j+1]*n[j+1]+n[j+2]*n[j+2],.001);
                            ids.put(mesh,meshes.length());meshes.put(new JSONObject().put("p",array(p)).put("n",array(n)));
                        }
                        parts.put(new JSONObject().put("mesh",ids.get(mesh)).put("matrix",array(matrix)).put("color",array(new float[]{r,g,b,alpha})));
                    }catch(Exception e){throw new RuntimeException(e);}
                }
            },rig,root,vp,false);
            poses.put(new JSONObject().put("name",names[i]).put("parts",parts).put("vp",array(vp)).put("mirrored",i==12));
        }
        output.put("meshes",meshes).put("poses",poses).put("vertex",Toon3DProgram.VERTEX).put("fragment",Toon3DProgram.FRAGMENT);
        File file=new File("build/astra-previews/mesh-review.json");file.getParentFile().mkdirs();
        Files.write(file.toPath(),output.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertTrue(meshes.length()>12); assertTrue(file.length()>10000);
    }
}
