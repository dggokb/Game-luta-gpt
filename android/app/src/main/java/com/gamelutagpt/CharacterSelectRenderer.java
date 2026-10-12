package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Screen layout based on the APPROVED 1648x928 concept, mapped to 1280x720.
 * Exact reference positions: symmetric central four-by-four grid; huge P1/P2
 * character art under blue dragon/red tiger emblems; left/right TAG panels;
 * Moon Temple stage preview in a trapezoid; dark controller-command footer.
 * All selectable art comes from the project's OWN art/concepts (not full-body IDLE cells).
 */
final class CharacterSelectRenderer {
    private static final int BLUE=0xFF158AFF, RED=0xFFFF263E, WHITE=0xFFFAFAFF;
    private final Context context;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint bitmapPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path path=new Path();
    private final Map<String,Bitmap> art=new HashMap<>();
    private final Map<String,Bitmap> scenery=new HashMap<>();
    CharacterSelectRenderer(Context context) {this.context=context;}

    void draw(Canvas c,Paint unused,CharacterSelect s) {
        c.save();
        background(c);
        spiritEmblem(c,0);
        spiritEmblem(c,1);
        hero(c,s,0);
        hero(c,s,1);
        playerTitles(c,s);
        grid(c,s);
        teamPanel(c,s,0);
        teamPanel(c,s,1);
        stagePreview(c);
        footer(c,s);
        if (s.settingsOpen()) settings(c);
        c.restore();
    }

    private void background(Canvas c) {
        c.drawColor(0xFF06091B);
        Bitmap sky=stage("ceu.jpg");
        if(sky!=null) c.drawBitmap(sky,null,new RectF(0f,0f,1280f,690f),bitmapPaint);
        shade(c,0f,0f,640f,684f,0xB5031A51,0x2C050816);
        shade(c,640f,0f,1280f,684f,0xA340001A,0x450B0514);
        // Moon behind the temple
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x995BA9FF);c.drawCircle(640f,28f,59f,p);
        p.setColor(0xFFE8F4FF);c.drawCircle(640f,27f,32f,p);
        p.setColor(0x553E65C8);c.drawCircle(625f,22f,9f,p);
        p.setColor(0x663B52A0);c.drawCircle(651f,44f,7f,p);
        // Temple interior: vertical black stone columns and hanging banners
        for(int i=0;i<7;i++) {
            float x=105f+i*178f;
            p.setColor(0xBF090C25);c.drawRect(x,0,x+42f,661f,p);
            p.setColor(0xBB172F55);c.drawRect(x+6f,0,x+12f,661f,p);
            p.setColor(0xAA3C4263);c.drawRect(x+37f,0,x+40f,661f,p);
            p.setColor(0xFF040D25);c.drawRect(x-7f,45f,x+49f,56f,p);
            p.setColor(i%2==0?0xAA15234E:0xAA581B2F);
            c.drawRect(x+56f,58f,x+114f,232f,p);
            p.setColor(i%2==0?0xFF42517B:0xFF775363);
            c.drawCircle(x+85f,119f,23f,p);
            p.setColor(0xFF111329);c.drawCircle(x+85f,119f,18f,p);
            flame(c,x+26f,62f,i%2==0?BLUE:RED,0.72f);
            flame(c,x-15f,353f,0xFFFF8730,0.88f);
        }
        // floor
        shade(c,0f,570f,1280f,690f,0x0021203D,0xEF030614);
        for(int i=0;i<14;i++){
            line(c,0,579+i*i*.58f,1280,579+i*i*.58f,0x154674B5,1f);
        }
        line(c,0f,687f,1280f,687f,0xFF6D8DC7,2.5f);
    }

    private void shade(Canvas c,float l,float t,float r,float b,int a,int z) {
        p.setShader(new LinearGradient(l,t,l,b,a,z,Shader.TileMode.CLAMP));
        p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);p.setShader(null);
    }

    private void spiritEmblem(Canvas c,int side) {
        int color=side==0?BLUE:RED;
        float x=side==0?238f:1042f, y=142f;
        c.save();
        if(side==1)c.scale(-1f,1f,x,0f);
        // Stylized glowing dragon/tiger spirit behind the fighter, not an inert UI card
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        for(int glow=3;glow>=0;glow--) {
            p.setColor(alpha(color,glow==0?225:35));
            p.setStrokeWidth(glow==0?4f:6f+glow*7f);
            path.reset();
            path.moveTo(x-151,y-18);
            path.cubicTo(x-121,y-127,x+2,y-125,x+98,y-59);
            path.cubicTo(x+125,y-31,x+140,y+17,x+84,y+30);
            path.cubicTo(x+42,y+46,x+59,y+10,x+11,y+19);
            path.cubicTo(x-52,y+32,x-93,y+126,x-158,y+141);
            c.drawPath(path,p);
            path.reset();path.moveTo(x+20,y-100);path.quadTo(x+55,y-139,x+95,y-128);
            path.moveTo(x+24,y-96);path.quadTo(x+14,y-159,x-22,y-165);
            path.moveTo(x+82,y+18);path.quadTo(x+148,y+55,x+164,y+5);
            c.drawPath(path,p);
        }
        p.setStrokeWidth(3f);p.setColor(alpha(color,235));
        for(int i=0;i<6;i++){
            float bx=x-113+i*23f,by=y-92+(i%2)*9f;
            path.reset();path.moveTo(bx,by);path.lineTo(bx+15,by-28);path.lineTo(bx+33,by-18);
            c.drawPath(path,p);
        }
        for(int i=0;i<5;i++) {
            float yy=y-34+i*21f;
            path.reset();path.moveTo(x+91,yy);path.lineTo(x+136+i*2,yy+9);c.drawPath(path,p);
        }
        p.setStyle(Paint.Style.FILL);p.setColor(0xFFFFFFFF);c.drawCircle(x+81,y-19,5f,p);
        p.setColor(0xFF0C1626);c.drawCircle(x+82,y-19,2f,p);
        c.restore();
    }

    private void hero(Canvas c,CharacterSelect s,int side) {
        boolean left=side==0;
        float cx=left?179f:1101f;
        int tone=left?BLUE:RED;
        Bitmap b=concept(s.pick(side,0));
        c.save();
        c.clipRect(left?0f:913f,55f,left?365f:1280f,660f);
        if(b!=null){
            float maxW=left?376f:387f,maxH=left?586f:586f;
            float scale=Math.min(maxW/b.getWidth(),maxH/b.getHeight());
            float w=b.getWidth()*scale,h=b.getHeight()*scale;
            bitmapPaint.setColor(Color.WHITE);bitmapPaint.setAlpha(255);
            // Full-body approved 90s anime concept art, enlarged near the outer edge.
            if(!left)c.scale(-1f,1f,cx,0f);
            c.drawBitmap(b,null,new RectF(cx-w/2f,79f,cx+w/2f,79f+h),bitmapPaint);
        }
        // Subtle edge-colored light while keeping the art visible.
        shade(c,left?0f:913f,56f,left?365f:1280f,640f,
                alpha(tone,10),alpha(tone,22));
        c.restore();
    }

    private void playerTitles(Canvas c,CharacterSelect s) {
        labelPanel(c,0f,21f,117f,84f,BLUE,"P1");
        labelPanel(c,1163f,21f,1280f,84f,RED,"P2");
        // The approved concept does not have a game logo at the top.
    }

    private void labelPanel(Canvas c,float l,float t,float r,float b,int color,String text) {
        p.setColor(0xDA081128);c.drawRect(l,t,r,b,p);
        line(c,l,t,r,t,color,3f);line(c,l,b,r,b,color,3f);
        text(c,text,(l+r)/2f,b-14f,48f,WHITE,true,Paint.Align.CENTER);
    }

    private void grid(Canvas c,CharacterSelect s) {
        // Straight left/right symmetry; vanishing-point perspective comes from framing,
        // not rotating one side of the select grid.
        p.setColor(0xF0091128);c.drawRect(376f,128f,904f,542f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.2f);
        p.setColor(0xFF7898D5);c.drawRect(376f,128f,904f,542f,p);
        p.setStyle(Paint.Style.FILL);
        for(int i=0;i<CharacterSelect.ROWS*CharacterSelect.COLUMNS;i++) tile(c,s,i);
    }

    private void tile(Canvas c,CharacterSelect s,int index) {
        float x=s.cardLeft(index),y=s.cardTop(index),w=s.cardWidth(),h=s.cardHeight();
        boolean locked=index>=s.ids.length;
        p.setStyle(Paint.Style.FILL);p.setColor(locked?0xFF14192C:0xFF1D2539);
        c.drawRect(x,y,x+w,y+h,p);
        if(!locked) {
            String id=s.ids[index];
            Bitmap art=concept(id);
            if(art!=null) {
                c.save();c.clipRect(x+1f,y+1f,x+w-1f,y+h-1f);
                float scale=Math.max(w/(art.getWidth()*.72f),h/(art.getHeight()*.61f));
                float aw=art.getWidth()*scale,ah=art.getHeight()*scale;
                bitmapPaint.setAlpha(255);
                c.drawBitmap(art,null,new RectF(x+(w-aw)*.5f,y-0.19f*ah,x+(w+aw)*.5f,y+0.81f*ah),bitmapPaint);
                c.restore();
            }
            shade(c,x,y+h*.72f,x+w,y+h,0x00060B14,0xDD050711);
            String name=GeneratedCharacters.get(id).displayName;
            // Two corner tabs may coexist when both players select the same fighter.
            int n1=s.order(0,id),n2=s.order(1,id);
            if(n1!=0)tab(c,x+2f,y+2f,BLUE,"P1"+(n1==2?" TAG":""));
            if(n2!=0)tab(c,x+w-48f,y+2f,RED,"P2"+(n2==2?" TAG":""));
            text(c,name,x+w/2f,y+h-9f,13f,WHITE,true,Paint.Align.CENTER);
            if(index==s.cursorIndex()) {
                p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.4f);
                p.setColor(s.focusSide()==0?BLUE:RED);
                c.drawRect(x+2f,y+2f,x+w-2f,y+h-2f,p);
                p.setStyle(Paint.Style.FILL);
            }
        } else {
            lock(c,x+w/2f,y+h/2f,0xFF838B9D);
        }
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1f);p.setColor(0xFF9BA8C9);
        c.drawRect(x,y,x+w,y+h,p);p.setStyle(Paint.Style.FILL);
    }

    private void tab(Canvas c,float x,float y,int color,String label) {
        p.setColor(color);c.drawRect(x,y,x+46f,y+23f,p);
        text(c,label,x+23f,y+16f,label.length()>4?9f:15f,WHITE,true,Paint.Align.CENTER);
    }

    private void lock(Canvas c,float x,float y,int color) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4.2f);p.setColor(color);
        c.drawRoundRect(x-10f,y-24f,x+10f,y-1f,9f,9f,p);
        p.setStyle(Paint.Style.FILL);c.drawRoundRect(x-16f,y-7f,x+16f,y+22f,2f,2f,p);
        p.setColor(0xFF202739);c.drawCircle(x,y+6f,3.8f,p);
    }

    private void teamPanel(Canvas c,CharacterSelect s,int side) {
        float l=s.slotLeft(side),top=s.slotTop(0)-34f;
        int color=side==0?BLUE:RED;
        p.setColor(0xEE05091F);c.drawRoundRect(l-8f,top,l+CharacterSelect.TEAM_W+8f,630f,10f,10f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3f);p.setColor(color);
        c.drawRoundRect(l-8f,top,l+CharacterSelect.TEAM_W+8f,630f,10f,10f,p);
        p.setStyle(Paint.Style.FILL);
        text(c,"TEAM",l+CharacterSelect.TEAM_W/2f,top+26f,21f,WHITE,true,Paint.Align.CENTER);
        for(int slot=0;slot<2;slot++) {
            float y=s.slotTop(slot);
            p.setColor(0xF0060E25);c.drawRect(l,y,l+CharacterSelect.TEAM_W,y+CharacterSelect.SLOT_H,p);
            Bitmap b=concept(s.pick(side,slot));
            if(b!=null) {
                c.save();c.clipRect(l+31f,y+3f,l+CharacterSelect.TEAM_W-4f,y+CharacterSelect.SLOT_H-3f);
                float w=144f,h=144f*b.getHeight()/b.getWidth();
                bitmapPaint.setAlpha(255);
                c.drawBitmap(b,null,new RectF(l+47f,y-0.21f*h,l+47f+w,y+0.79f*h),bitmapPaint);
                c.restore();
            }
            shade(c,l+5f,y+75f,l+CharacterSelect.TEAM_W-5f,y+CharacterSelect.SLOT_H-3f,0x00000000,0xDB000718);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s.focusSide()==side && s.focusSlot()==slot?3.5f:1.5f);
            p.setColor(color);
            c.drawRect(l,y,l+CharacterSelect.TEAM_W,y+CharacterSelect.SLOT_H,p);
            p.setStyle(Paint.Style.FILL);
            text(c,""+(slot+1),l+18f,y+43f,25f,WHITE,true,Paint.Align.CENTER);
            text(c,slot==0?(side==0?"P1":"P2"):"TAG",
                    l+CharacterSelect.TEAM_W-31f,y+CharacterSelect.SLOT_H-11f,20f,WHITE,true,Paint.Align.CENTER);
        }
        text(c,"⇄",l+CharacterSelect.TEAM_W/2f,s.slotTop(1)-9f,27f,WHITE,true,Paint.Align.CENTER);
    }

    private void stagePreview(Canvas c) {
        // Low-view trapezoidal preview at exactly the location in the supplied art.
        float top=547f,bottom=684f;
        path.reset();path.moveTo(338f,top);path.lineTo(942f,top);
        path.lineTo(987f,bottom);path.lineTo(293f,bottom);path.close();
        c.save();c.clipPath(path);
        p.setColor(0xFF0E2151);c.drawRect(290f,top,990f,bottom,p);
        Bitmap sky=stage("ceu.jpg"), city=stage("cidade.png"), gate=stage("portal.png");
        if(sky!=null)c.drawBitmap(sky,null,new RectF(297f,547f,987f,685f),bitmapPaint);
        if(city!=null) {
            bitmapPaint.setAlpha(245);
            c.drawBitmap(city,null,new RectF(297f,565f,987f,664f),bitmapPaint);
        }
        if(gate!=null) {
            bitmapPaint.setAlpha(240);
            c.drawBitmap(gate,null,new RectF(548f,563f,734f,665f),bitmapPaint);
        }
        Bitmap pillar=stage("pilar.png");
        if(pillar!=null) {
            bitmapPaint.setAlpha(255);
            c.drawBitmap(pillar,null,new RectF(302f,542f,405f,670f),bitmapPaint);
            c.drawBitmap(pillar,null,new RectF(874f,542f,978f,670f),bitmapPaint);
        }
        p.setColor(0xD9020C22);c.drawRect(306f,650f,970f,678f,p);
        text(c,CharacterSelect.STAGE,640f,672f,21f,WHITE,true,Paint.Align.CENTER);
        // One stage currently exists; muted arrows never pretend to change the fight.
        text(c,"❮",432f,672f,24f,0xFFB2C6EE,true,Paint.Align.CENTER);
        text(c,"❯",845f,672f,24f,0xFFB2C6EE,true,Paint.Align.CENTER);
        c.restore();
        p.setColor(BLUE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.8f);
        c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
    }

    private void footer(Canvas c,CharacterSelect s) {
        p.setColor(0xF8060A16);c.drawRect(0f,687f,1280f,720f,p);
        line(c,0f,688f,1280f,688f,0xFF4786F5,2f);
        command(c,66f,"✣","SELECIONAR",0xFFDCE6FF,175f);
        command(c,318f,"A","CONFIRMAR",0xFF56E882,415f);
        command(c,473f,"B","VOLTAR",0xFFFF4454,531f);
        command(c,622f,"Y","CONFIGURAÇÕES",0xFFEBCC40,708f);
        command(c,813f,"LB / RB","TROCAR TAG",0xFFF9F9FF,928f);
        command(c,1045f,"X","ALEATÓRIO",0xFF258DFF,1137f);
    }
    private void command(Canvas c,float x,String key,String word,int color,float wordX) {
        p.setColor(0xDD090F20);p.setStyle(Paint.Style.FILL);
        c.drawCircle(x,706f,key.length()==1?15f:20f,p);
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(2f);
        c.drawCircle(x,706f,key.length()==1?15f:20f,p);
        p.setStyle(Paint.Style.FILL);
        text(c,key,x,712f,key.length()>1?10f:21f,color,true,Paint.Align.CENTER);
        text(c,word,wordX,711f,14f,WHITE,true,Paint.Align.CENTER);
    }

    private void settings(Canvas c) {
        p.setColor(0xF50A122C);c.drawRoundRect(313f,204f,967f,485f,16f,16f,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3f);p.setColor(BLUE);
        c.drawRoundRect(313f,204f,967f,485f,16f,16f,p);p.setStyle(Paint.Style.FILL);
        text(c,"CONFIGURAÇÕES · SELEÇÃO TAG",640f,250f,28f,WHITE,true,Paint.Align.CENTER);
        text(c,"TOQUE UM SLOT TEAM PARA ESCOLHER O TITULAR OU PARCEIRO",640f,302f,18f,WHITE,false,Paint.Align.CENTER);
        text(c,"TOQUE UM RETRATO OU USE O DIRECIONAL PARA SELECIONAR",640f,344f,18f,WHITE,false,Paint.Align.CENTER);
        text(c,"LB / RB INVERTE TITULAR E TAG DA EQUIPE ATIVA",640f,386f,18f,WHITE,false,Paint.Align.CENTER);
        text(c,"TOQUE PARA FECHAR",640f,455f,17f,0xFF84BBFF,true,Paint.Align.CENTER);
    }

    private void flame(Canvas c,float x,float y,int color,float scale){
        p.setStyle(Paint.Style.FILL);p.setColor(alpha(color,30));
        c.drawCircle(x,y,24f*scale,p);
        path.reset();path.moveTo(x-7f*scale,y+12f*scale);
        path.quadTo(x-9f*scale,y-6f*scale,x+3f*scale,y-22f*scale);
        path.quadTo(x+23f*scale,y+4f*scale,x+7f*scale,y+14f*scale);
        path.close();p.setColor(alpha(color,210));c.drawPath(path,p);
    }

    private void line(Canvas c,float a,float b,float x,float y,int color,float width){
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);
        c.drawLine(a,b,x,y,p);p.setStyle(Paint.Style.FILL);
    }

    private void text(Canvas c,String text,float x,float y,float size,int color,boolean bold,Paint.Align align){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setTextAlign(align);
        p.setTypeface(Typeface.create("sans-serif-condensed",bold?Typeface.BOLD:Typeface.NORMAL));
        p.setColor(color);p.setTextSize(size);c.drawText(text,x,y,p);
    }
    private static int alpha(int color,int opacity){
        return (Math.max(0,Math.min(opacity,255))<<24)|(color&0x00FFFFFF);
    }
    private Bitmap concept(String id) {
        if(art.containsKey(id))return art.get(id);
        String file="player_base".equals(id)?"p01":id;
        Bitmap bitmap=read("selection/portraits/"+file+".png");
        art.put(id,bitmap);
        return bitmap;
    }
    private Bitmap stage(String name) {
        if(scenery.containsKey(name))return scenery.get(name);
        Bitmap bitmap=read("stages/templo_lua/"+name);
        scenery.put(name,bitmap);
        return bitmap;
    }
    private Bitmap read(String path) {
        try(InputStream in=context.getAssets().open(path)) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            options.inSampleSize=2;
            return BitmapFactory.decodeStream(in,null,options);
        } catch(IOException ex) {
            Log.w("CharacterSelect","Could not load "+path,ex);
            return null;
        }
    }
}
