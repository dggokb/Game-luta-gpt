package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Reference-based selection: illustrated backdrop, isolated fighters and actual cropped faces. */
final class CharacterSelectRenderer {
    private static final int BLUE=0xFF24AAFF, RED=0xFFFF3149, WHITE=0xFFF7FAFF;
    private final Context context;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path path=new Path();
    private final RectF box=new RectF();
    private final Matrix warp=new Matrix();
    private final float[] corners=new float[8], source=new float[8];
    private final Map<String,Bitmap> faces=new HashMap<>(), heroes=new HashMap<>();
    private final Typeface bold=Typeface.create("sans-serif-condensed",Typeface.BOLD_ITALIC);
    private final Typeface regular=Typeface.create("sans-serif-condensed",Typeface.NORMAL);
    private Bitmap backdrop,stageArt;
    private final Bitmap[] futureFaces=new Bitmap[7];

    CharacterSelectRenderer(Context context) {
        this.context=context;
        backdrop=read("selection/art/backdrop.png",1);
        stageArt=read("selection/art/reference_stage.png",1);
        // The reference's future fighters remain illustrated but locked, never fake roster IDs.
        if(stageArt!=null) {
            int[][] regions={{450,404,608,513},{629,404,766,512},{785,404,929,512},
                {950,404,1088,512},{437,530,597,639},{615,530,776,639},{791,530,950,639}};
            for(int i=0;i<regions.length;i++) {
                int[] q=regions[i];
                int x=q[0]*stageArt.getWidth()/1536,y=q[1]*stageArt.getHeight()/864;
                int w=(q[2]-q[0])*stageArt.getWidth()/1536,h=(q[3]-q[1])*stageArt.getHeight()/864;
                futureFaces[i]=Bitmap.createBitmap(stageArt,x,y,w,h);
            }
        }
        // Decode before the render loop; changing a selection never loads a full model sheet.
        for(String id:GeneratedCharacters.SELECTABLE) { face(id);heroArt(id); }
    }

    void draw(Canvas c,Paint unused,CharacterSelect s) {
        c.save();
        c.drawColor(0xFF060B1C);
        imagePaint.setAlpha(255);
        if(backdrop!=null) {
            box.set(0,0,1280,720);c.drawBitmap(backdrop,null,box,imagePaint);
        }
        hero(c,s.pick(0,0),0);
        hero(c,s.pick(1,0),1);
        playerTitle(c,0);playerTitle(c,1);
        grid(c,s);
        team(c,s,0);team(c,s,1);
        stage(c);
        footer(c);
        if(s.settingsOpen()) settings(c);
        c.restore();
    }

    private void hero(Canvas c,String id,int side) {
        Bitmap b=heroArt(id);
        if(b==null)return;
        c.save();
        c.clipRect(side==0?0:906,44,side==0?374:1280,687);
        float left=side==0?-25:870, width=435, top=55;
        boolean facesRight="player_base".equals(id);
        if((side==0&&!facesRight)||(side==1&&facesRight)) c.scale(-1,1,left+width/2,0);
        box.set(left,top,left+width,top+width*b.getHeight()/b.getWidth());
        imagePaint.setAlpha(255);c.drawBitmap(b,null,box,imagePaint);
        c.restore();
    }

    private void playerTitle(Canvas c,int side) {
        int color=side==0?BLUE:RED;
        float l=side==0?-22:1166,r=side==0?116:1302;
        path.reset();path.moveTo(l,21);path.lineTo(r-24,21);
        path.lineTo(r+15,84);path.lineTo(l,84);path.close();
        fill(c,0xAF02132A);stroke(c,color,2.7f);
        text(c,side==0?"P1":"P2",side==0?62:1218,71,50,WHITE,true);
    }

    private void grid(Canvas c,CharacterSelect s) {
        path.reset();path.moveTo(414,128);path.lineTo(866,128);
        path.lineTo(949,546);path.lineTo(331,546);path.close();
        fill(c,0xEA080C19);stroke(c,0xAA080D21,8);stroke(c,0xFF8AA4D0,1.6f);
        for(int i=0;i<CharacterSelect.COLUMNS*CharacterSelect.ROWS;i++) {
            s.cardCorners(i,corners);quadPath();
            fill(c,0xFF101624);
            if(i<s.ids.length) {
                String id=s.ids[i];
                Bitmap face=face(id);
                if(face!=null) warpedFace(c,face);
                quadPath();
                stroke(c,0xFF9FAABD,1.1f);
                int p1=s.order(0,id),p2=s.order(1,id);
                if(p1>0) {quadPath();stroke(c,0x8822A8FF,7);stroke(c,BLUE,p1==1?2.6f:1.6f);}
                if(p2>0) {quadPath();stroke(c,0x88FF213D,7);stroke(c,RED,p2==1?2.6f:1.6f);}
                float cy=corners[5]-8,rx=corners[4]-8,lx=corners[6]+8;
                if(p1>0) textAt(c,p1==1?"P1":"TAG",p2>0?lx:rx,cy,19,WHITE,true,p2>0?Paint.Align.LEFT:Paint.Align.RIGHT);
                if(p2>0) textAt(c,p2==1?"P2":"TAG",rx,cy,19,WHITE,true,Paint.Align.RIGHT);
                if(i==s.cursorIndex()) {quadPath();stroke(c,0x88FFFFFF,1);}
            } else {
                if(i>=8&&i<15&&futureFaces[i-8]!=null) {
                    warpedFace(c,futureFaces[i-8]);
                    quadPath();stroke(c,0xFF647089,1.1f);
                    float x=corners[2]-14,y=corners[3]+18;
                    p.setColor(0xDE061021);c.drawCircle(x,y,12,p);
                    c.save();c.scale(.35f,.35f,x,y);lock(c,x,y,0xFFAFBCD2);c.restore();
                    text(c,"BLOQUEADO",(corners[6]+corners[4])/2,corners[5]-6,9,0xFFE6ECF7,false);
                } else {
                    quadPath();stroke(c,0xFF647089,1.1f);
                    lock(c,(corners[0]+corners[2]+corners[4]+corners[6])/4,
                        (corners[1]+corners[5])/2,0xFF8892A4);
                }
            }
        }
    }

    private void warpedFace(Canvas c,Bitmap b) {
        source[0]=0;source[1]=0;source[2]=b.getWidth();source[3]=0;
        source[4]=b.getWidth();source[5]=b.getHeight();source[6]=0;source[7]=b.getHeight();
        warp.reset();
        if(warp.setPolyToPoly(source,0,corners,0,4)) {
            c.save();quadPath();c.clipPath(path);
            imagePaint.setAlpha(255);c.drawBitmap(b,warp,imagePaint);c.restore();
        }
    }

    private void quadPath() {
        path.reset();path.moveTo(corners[0],corners[1]);
        path.lineTo(corners[2],corners[3]);path.lineTo(corners[4],corners[5]);
        path.lineTo(corners[6],corners[7]);path.close();
    }

    private void team(Canvas c,CharacterSelect s,int side) {
        float l=s.slotLeft(side),r=l+CharacterSelect.TEAM_W,top=350,bottom=624;
        int color=side==0?BLUE:RED;
        path.reset();path.moveTo(l-11,top+18);path.lineTo(l+4,top);
        path.lineTo(r-5,top);path.lineTo(r+11,top+18);
        path.lineTo(r+11,bottom-9);path.lineTo(r+2,bottom);
        path.lineTo(l-4,bottom);path.lineTo(l-11,bottom-9);path.close();
        fill(c,0xEF040B1B);stroke(c,0x5521A2FF,side==0?8:0.1f);
        stroke(c,color,2.5f);
        text(c,"TEAM",(l+r)/2,375,23,WHITE,true);
        for(int slot=0;slot<2;slot++) {
            float t=s.slotTop(slot),b=t+CharacterSelect.SLOT_H;
            p.setColor(0xFF050C1D);p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);
            Bitmap face=face(s.pick(side,slot));
            if(face!=null) {
                box.set(l+23,t+2,r-2,b-2);imagePaint.setAlpha(255);c.drawBitmap(face,null,box,imagePaint);
            }
            p.setColor(color);p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s.focusSide()==side&&s.focusSlot()==slot?3:1.5f);
            c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);
            text(c,String.valueOf(slot+1),l+12,t+32,25,WHITE,true);
            textAt(c,slot==1?"TAG":side==0?"P1":"P2",r-7,b-9,23,WHITE,true,Paint.Align.RIGHT);
        }
        text(c,"⇄",(l+r)/2,s.slotTop(1)-13,29,WHITE,true);
    }

    private void stage(Canvas c) {
        path.reset();path.moveTo(336,547);path.lineTo(944,547);
        path.lineTo(986,682);path.lineTo(294,682);path.close();
        c.save();c.clipPath(path);
        p.setColor(0xFF081F3F);c.drawRect(294,547,986,682,p);
        if(stageArt!=null) {
            // Only the stage-art region of the supplied reference is rendered.
            // Portraits, selection state and all controls above are real dynamic UI.
            box.set(0,0,1280,720);imagePaint.setAlpha(255);c.drawBitmap(stageArt,null,box,imagePaint);
        }
        p.setColor(0xD8051025);c.drawRect(315,651,965,678,p);
        text(c,CharacterSelect.STAGE,640,673,22,WHITE,false);
        text(c,"❮",434,670,25,WHITE,true);text(c,"❯",846,670,25,WHITE,true);
        c.restore();stroke(c,0x66FFFFFF,6);stroke(c,BLUE,1.8f);
    }

    private void footer(Canvas c) {
        p.setColor(0xF3030711);c.drawRect(0,687,1280,720,p);
        p.setColor(0xFF84A8EB);p.setStrokeWidth(2);c.drawLine(0,689,1280,689,p);
        text(c,"✣",148,715,27,WHITE,false);
        text(c,"SELECIONAR",224,713,14,WHITE,false);
        key(c,"A",327,0xFF57DB78);text(c,"CONFIRMAR",385,713,14,WHITE,false);
        key(c,"B",480,0xFFFF3448);text(c,"VOLTAR",535,713,14,WHITE,false);
        key(c,"Y",615,0xFFFFD62C);text(c,"CONFIGURAÇÕES",710,713,14,WHITE,false);
        text(c,"LB / RB",834,713,13,WHITE,false);text(c,"TROCAR TAG",940,713,14,WHITE,false);
        key(c,"X",1058,BLUE);text(c,"ALEATÓRIO",1145,713,14,WHITE,false);
    }

    private void key(Canvas c,String label,float x,int color) {
        p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(2);c.drawCircle(x,705,12,p);
        p.setStyle(Paint.Style.FILL);text(c,label,x,712,19,color,false);
    }

    private void lock(Canvas c,float x,float y,int color) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(color);
        c.drawRoundRect(x-10,y-24,x+10,y-1,9,9,p);
        p.setStyle(Paint.Style.FILL);c.drawRoundRect(x-16,y-7,x+16,y+22,2,2,p);
        p.setColor(0xFF141C2D);c.drawCircle(x,y+6,3.5f,p);
    }

    private void settings(Canvas c) {
        p.setStyle(Paint.Style.FILL);p.setColor(0xF50A122C);c.drawRoundRect(313,204,967,485,16,16,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(BLUE);c.drawRoundRect(313,204,967,485,16,16,p);
        p.setStyle(Paint.Style.FILL);
        text(c,"SELEÇÃO TAG",640,250,28,WHITE,true);
        text(c,"TOQUE UM SLOT TEAM PARA ESCOLHER TITULAR OU PARCEIRO",640,302,17,WHITE,false);
        text(c,"TOQUE UM RETRATO OU USE O DIRECIONAL",640,344,18,WHITE,false);
        text(c,"LB / RB INVERTE TITULAR E TAG DA EQUIPE ATIVA",640,386,18,WHITE,false);
        text(c,"TOQUE PARA FECHAR",640,455,17,BLUE,true);
    }

    private void fill(Canvas c,int color) {p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(path,p);}
    private void stroke(Canvas c,int color,float width) {
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setColor(color);c.drawPath(path,p);
        p.setStyle(Paint.Style.FILL);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean isBold) {
        textAt(c,value,x,y,size,color,isBold,Paint.Align.CENTER);
    }
    private void textAt(Canvas c,String value,float x,float y,float size,int color,boolean isBold,Paint.Align align) {
        p.setShader(null);p.setTypeface(isBold?bold:regular);p.setTextSize(size);p.setTextAlign(align);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setColor(0xFF030711);c.drawText(value,x,y,p);
        p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawText(value,x,y,p);p.setTextAlign(Paint.Align.LEFT);
    }

    private String assetId(String id) {return "player_base".equals(id)?"p01":id;}
    private Bitmap heroArt(String id) {
        if(heroes.containsKey(id))return heroes.get(id);
        Bitmap b=read("selection/art/hero_"+assetId(id)+".png",2);
        heroes.put(id,b);return b;
    }

    private Bitmap face(String id) {
        if(faces.containsKey(id))return faces.get(id);
        Bitmap sheet=read("selection/portraits/"+assetId(id)+".png",1);
        Bitmap b=null;
        if(sheet!=null) {
            // Exact source ROIs, normalized to the original sheet. Never show the whole sheet.
            float l=.805f,t=.027f,r=.991f,bottom=.312f;
            switch(assetId(id)) {
                case "p02":l=.79f;t=.012f;r=.992f;bottom=.252f;break;
                case "p03":l=.796f;t=.014f;r=.992f;bottom=.311f;break;
                case "p04":l=.809f;t=.023f;r=.991f;bottom=.323f;break;
                case "p05":l=.007f;t=.67f;r=.202f;bottom=.985f;break;
                case "p06":l=.552f;t=.601f;r=.768f;bottom=.992f;break;
                case "p07":l=.011f;t=.668f;r=.231f;bottom=.982f;break;
                case "p08":l=.008f;t=.594f;r=.208f;bottom=.985f;break;
            }
            int x=Math.round(l*sheet.getWidth()),y=Math.round(t*sheet.getHeight());
            int w=Math.round((r-l)*sheet.getWidth()),h=Math.round((bottom-t)*sheet.getHeight());
            b=Bitmap.createBitmap(sheet,x,y,Math.max(1,w),Math.max(1,h));
            if(b!=sheet)sheet.recycle();
        }
        faces.put(id,b);return b;
    }

    private Bitmap read(String asset,int sample) {
        try(InputStream in=context.getAssets().open(asset)) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inSampleSize=sample;
            options.inScaled=false;
            return BitmapFactory.decodeStream(in,null,options);
        } catch(IOException ex) {Log.w("CharacterSelect","Missing selection art: "+asset,ex);return null;}
    }
}
