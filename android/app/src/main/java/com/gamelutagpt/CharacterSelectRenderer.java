package com.gamelutagpt;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.os.SystemClock;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Reference-based selection: illustrated backdrop, isolated fighters and actual cropped faces. */
final class CharacterSelectRenderer {
    private static final int BLUE=0xFF24AAFF, RED=0xFFFF3149, WHITE=0xFFF7FAFF;
    private final Context context;
    private final HudConceptPortraits hudConcepts;
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
    private final String[] shownPicks=new String[4];
    private final long[] pickChangedAt=new long[4];
    private final long effectsEpoch=SystemClock.uptimeMillis();
    private long now, fightTransitionAt=-1;
    private float phase;
    private boolean observedPicks;
    private int shownStep=-1;

    // Hero entrance: each side slides in with a white silhouette flash when its preview changes.
    private final String[] shownHero=new String[2];
    private final long[] heroChangedAt=new long[2];
    private final Paint flashPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private static final long HERO_IN_MS=260, LOCK_MS=520;
    private long readyAt=-1;

    private final Bitmap[] futureFaces=new Bitmap[8];
    private final String[] futureIds=new String[8];
    private final Shader[] auras={
        new RadialGradient(170,325,340,new int[]{0xAC168DFF,0x701268F2,0x002168FF},new float[]{0,.45f,1},Shader.TileMode.CLAMP),
        new RadialGradient(1110,325,340,new int[]{0xACFF2948,0x70FF123C,0x00FF2948},new float[]{0,.45f,1},Shader.TileMode.CLAMP)
    };

    CharacterSelectRenderer(Context context) {
        this.context=context;
        hudConcepts=new HudConceptPortraits(context);
        flashPaint.setColorFilter(new PorterDuffColorFilter(0xFFFFFFFF,PorterDuff.Mode.SRC_ATOP));
        backdrop=read("selection/art/backdrop.png",1);
        stageArt=read("selection/art/reference_stage.png",1);
        // P05 already uses the exact upper-right action pose from its concept.
        // P01 and the corrected P02/P03/P04/P08 now have their own isolated assets.
        Bitmap atlas=read("selection/art/heroes_p02_p05.png",1);
        if(atlas!=null) {
            int w=atlas.getWidth()/2,h=atlas.getHeight()/2;
            heroes.put("p05",Bitmap.createBitmap(atlas,w,h,w,h));atlas.recycle();
        }
        // Locked slots show only the P09–P16 concepts that are not playable yet.
        java.util.List<String> playable=java.util.Arrays.asList(GeneratedCharacters.SELECTABLE);
        for(int n=9,i=0;n<=16&&i<8;n++) {
            String id="p"+(n<10?"0":"")+n;
            if(playable.contains(id))continue;
            futureIds[i]=id;futureFaces[i++]=face(id);
        }
        // Decode before the render loop; changing a selection never loads a full model sheet.
        for(String id:GeneratedCharacters.SELECTABLE) { face(id);heroArt(id); }
    }
    /** HUD close-ups stay independent from the selection artwork. */
    Bitmap hudPortrait(CharacterDefinition character) {return hudConcepts.get(character.id);}

    void draw(Canvas c,Paint unused,CharacterSelect s) {
        tick(s);
        c.save();
        c.drawColor(0xFF060B1C);
        imagePaint.setAlpha(255);
        if(backdrop!=null) {
            box.set(0,0,1280,720);c.drawBitmap(backdrop,null,box,imagePaint);
        }
        ambient(c);
        hero(c,s.preview(0),0);
        hero(c,s.preview(1),1);
        namePlate(c,s.preview(0),0);namePlate(c,s.preview(1),1);
        playerTitle(c,0);playerTitle(c,1);
        progress(c,s);
        grid(c,s);
        team(c,s,0);team(c,s,1);
        stage(c);
        if(s.ready()) readyStamps(c,s);
        footer(c,s);
        if(s.settingsOpen()) settings(c);
        c.restore();
    }

    private void hero(Canvas c,String id,int side) {
        Bitmap b=heroArt(id);
        if(b==null)return;
        c.save();
        c.clipRect(side==0?0:906,44,side==0?374:1280,687);
        // Wide action concepts need the same vertical presence as portrait art.
        // Center them behind the side's TEAM panel rather than shrinking the fighter.
        float width=Math.max(435,630f*b.getWidth()/b.getHeight()),top=55;
        float anchor=side==0?192.5f:1087.5f;
        float headX=.55f;
        switch(assetId(id)) {
            case "p01":headX=.50f;break;
            case "p02":headX=.56f;break;
            case "p03":headX=.60f;break;
            case "p04":headX=.43f;break;
            case "p05":headX=.54f;break;
            case "p06":headX=.62f;break;
            case "p08":headX=.78f;break;
        }
        float left=anchor-width*headX;
        float in=Math.min(1f,(now-heroChangedAt[side])/(float)HERO_IN_MS);
        float ease=1f-(1f-in)*(1f-in)*(1f-in);
        // Slide in from the outer edge of the screen (applied before any mirroring).
        c.translate((side==0?-70f:70f)*(1f-ease),0);
        boolean facesRight="player_base".equals(id)||"p02".equals(id)
            ||"p05".equals(id)||"p06".equals(id);
        if((side==0&&!facesRight)||(side==1&&facesRight)) c.scale(-1,1,anchor,0);
        box.set(left,top,left+width,top+width*b.getHeight()/b.getWidth());
        imagePaint.setAlpha(Math.round(255*Math.min(1f,.35f+ease)));c.drawBitmap(b,null,box,imagePaint);
        imagePaint.setAlpha(255);
        if(in<1f) {
            flashPaint.setAlpha(Math.round(230*(1f-in)));
            c.drawBitmap(b,null,box,flashPaint);
        }
        c.restore();
    }

    private static String style(String id) {
        switch(id) {
            case "player_base":return "IRMÃO CALMO";
            case "p02":return "KUNG FU ÁGIL";
            case "p03":return "IRMÃO ENERGÉTICO";
            case "p04":return "LUTADOR DE RUA";
            case "p05":return "NINJA";
            case "p06":return "SOMBRA E ILUSÃO";
            case "p07":return "CAVALHEIRO PESADO";
            case "p08":return "CHICOTE";
            case "p09":return "BOMBEIRO GRAPPLER";
            case "p10":return "BOMBEIRA ÁGIL";
            case "p12":return "CRIATURA FERAL";
            default:return "";
        }
    }

    /** Fighter code and style on a slanted plate above the TEAM panel; slides with the hero. */
    private void namePlate(Canvas c,String id,int side) {
        if(id==null)return;
        float in=Math.min(1f,(now-heroChangedAt[side])/(float)HERO_IN_MS);
        float ease=1f-(1f-in)*(1f-in);
        int color=side==0?BLUE:RED;
        float cx=side==0?192f:1088f, top=292f, w=300f, h=46f;
        float shift=(side==0?-50f:50f)*(1f-ease);
        float l=cx-w/2+shift;
        path.reset();
        if(side==0){path.moveTo(l,top);path.lineTo(l+w,top);path.lineTo(l+w-18,top+h);path.lineTo(l-18,top+h);}
        else {path.moveTo(l+18,top);path.lineTo(l+w+18,top);path.lineTo(l+w,top+h);path.lineTo(l,top+h);}
        path.close();
        fill(c,alphaColor(0xFF040B1B,Math.round(225*ease)));
        stroke(c,alphaColor(color,Math.round(255*ease)),2.5f);
        p.setColor(alphaColor(color,Math.round(255*ease)));
        c.drawRect(side==0?l-12:l+w+6,top+4,side==0?l-6:l+w+12,top+h-4,p);
        String code="player_base".equals(id)?"P01":id.toUpperCase(java.util.Locale.ROOT);
        textAt(c,code,cx+shift-w/2+18,top+34,30,alphaColor(WHITE,Math.round(255*ease)),true,Paint.Align.LEFT);
        textAt(c,style(id),cx+shift+w/2-20,top+30,15,alphaColor(side==0?0xFFA9DDFF:0xFFFFB3BE,Math.round(255*ease)),true,Paint.Align.RIGHT);
    }

    /** Both teams locked: diagonal PRONTO stamps over the TEAM panels. */
    private void readyStamps(Canvas c,CharacterSelect s) {
        if(readyAt<0)readyAt=now;
        float k=Math.min(1f,(now-readyAt)/260f);
        float scale=1.8f-.8f*(1f-(1f-k)*(1f-k));
        for(int side=0;side<2;side++) {
            float cx=s.slotLeft(side)+CharacterSelect.TEAM_W/2, cy=CharacterSelect.TEAM_TOP+CharacterSelect.SLOT_H+17;
            c.save();c.rotate(-8f,cx,cy);c.scale(scale,scale,cx,cy);
            box.set(cx-92,cy-22,cx+92,cy+22);
            p.setStyle(Paint.Style.FILL);p.setColor(alphaColor(0xFF0B2A14,Math.round(235*k)));
            c.drawRoundRect(box,6,6,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
            p.setColor(alphaColor(0xFF57DB78,Math.round(255*k)));c.drawRoundRect(box,6,6,p);
            p.setStyle(Paint.Style.FILL);
            text(c,"PRONTO!",cx,cy+11,30,alphaColor(0xFFE9FFE9,Math.round(255*k)),true);
            c.restore();
        }
    }

    private void progress(Canvas c,CharacterSelect s) {
        int color=s.ready()?0xFF57DB78:s.focusSide()==0?BLUE:RED;
        p.setStyle(Paint.Style.FILL);p.setColor(0xEE040B1B);c.drawRoundRect(375,89,905,123,6,6,p);
        path.reset();path.addRoundRect(375,89,905,123,6,6,Path.Direction.CW);glow(c,color,.8f);
        text(c,s.stepLabel(),640,114,18,WHITE,true);
    }

    private void playerTitle(Canvas c,int side) {
        int color=side==0?BLUE:RED;
        float l=side==0?-22:1166,r=side==0?116:1302;
        path.reset();path.moveTo(l,21);path.lineTo(r-24,21);
        path.lineTo(r+15,84);path.lineTo(l,84);path.close();
        fill(c,0xAF02132A);glow(c,color,1f);stroke(c,color,2.7f);
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
                if(!s.ready()&&i==s.cursorIndex()) {
                    int side=s.focusSide()==0?BLUE:RED;
                    quadPath();glow(c,side,1f);
                    stroke(c,alphaColor(WHITE,210+Math.round(45*pulse())),3f);
                    edgeSpark(c);
                    cursorBrackets(c,side,s.focusSide()==0?(s.focusSlot()==0?"P1":"TAG P1"):(s.focusSlot()==0?"P2":"TAG P2"));
                }
                float flash=selectionFlash(s,id);
                if(flash>0) {
                    quadPath();fill(c,alphaColor(WHITE,Math.round(150*flash)));
                    // Lock-in ring expanding out of the card.
                    float fcx=(corners[0]+corners[2]+corners[4]+corners[6])/4,fcy=(corners[1]+corners[5])/2;
                    c.save();c.scale(1f+.35f*(1f-flash),1f+.35f*(1f-flash),fcx,fcy);
                    quadPath();stroke(c,alphaColor(WHITE,Math.round(255*flash)),4f*flash+1f);
                    c.restore();
                }
            } else {
                int f=i-s.ids.length;
                if(f>=0&&f<8&&futureFaces[f]!=null) {
                    warpedFace(c,futureFaces[f]);
                    quadPath();stroke(c,0xFF647089,1.1f);
                    float x=corners[2]-14,y=corners[3]+18;
                    p.setColor(0xDE061021);c.drawCircle(x,y,12,p);
                    c.save();c.scale(.35f,.35f,x,y);lock(c,x,y,0xFFAFBCD2);c.restore();
                    text(c,futureIds[f].toUpperCase()+"  BLOQUEADO",(corners[6]+corners[4])/2,corners[5]-6,9,0xFFE6ECF7,false);
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
        fill(c,0xEF040B1B);glow(c,color,s.focusSide()==side?1.25f:.85f);
        stroke(c,color,2.5f);
        text(c,"TEAM",(l+r)/2,375,23,WHITE,true);
        for(int slot=0;slot<2;slot++) {
            float t=s.slotTop(slot),b=t+CharacterSelect.SLOT_H;
            p.setColor(0xFF050C1D);p.setStyle(Paint.Style.FILL);c.drawRect(l,t,r,b,p);
            Bitmap face=face(s.pick(side,slot));
            if(face!=null&&s.chosen(side,slot)) {
                box.set(l+23,t+2,r-2,b-2);imagePaint.setAlpha(255);c.drawBitmap(face,null,box,imagePaint);
                long changed=pickChangedAt[side*2+slot];
                float lock=changed>0?Math.max(0f,1f-(now-changed)/(float)LOCK_MS):0f;
                if(lock>0f) {
                    // Lock-in: white burst on the slot and a frame that snaps outward.
                    p.setStyle(Paint.Style.FILL);p.setColor(alphaColor(WHITE,Math.round(200*lock*lock)));
                    c.drawRect(l,t,r,b,p);
                    float grow=14f*(1f-lock);
                    p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3f);p.setColor(alphaColor(color,Math.round(255*lock)));
                    c.drawRect(l-grow,t-grow,r+grow,b+grow,p);
                    p.setStyle(Paint.Style.FILL);
                }
            } else {
                text(c,!s.ready()&&s.focusSide()==side&&s.focusSlot()==slot?"ESCOLHA":"AGUARDANDO",(l+r)/2,t+58,16,
                    !s.ready()&&s.focusSide()==side&&s.focusSlot()==slot?color:0xFF8D99B0,false);
            }
            p.setColor(color);p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(s.focusSide()==side&&s.focusSlot()==slot?3:1.5f);
            c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);
            if(!s.ready()&&s.focusSide()==side&&s.focusSlot()==slot) {
                path.reset();path.addRect(l,t,r,b,Path.Direction.CW);
                glow(c,color,1.25f);
            }
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
        c.restore();glow(c,BLUE,.8f);stroke(c,0x66FFFFFF,6);stroke(c,BLUE,1.8f);
    }

    private void footer(Canvas c,CharacterSelect s) {
        p.setColor(0xF3030711);c.drawRect(0,687,1280,720,p);
        p.setColor(0xFF84A8EB);p.setStrokeWidth(2);c.drawLine(0,689,1280,689,p);
        text(c,"✣",148,715,27,WHITE,false);
        text(c,"SELECIONAR",224,713,14,WHITE,false);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        p.setColor(alphaColor(0xFF57DB78,Math.round(38+40*pulse())));
        c.drawCircle(327,705,16+2*pulse(),p);p.setStyle(Paint.Style.FILL);
        key(c,"A",327,0xFF57DB78);text(c,s.ready()?"CONFIRMAR":"ESCOLHER",385,713,14,WHITE,false);
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
        text(c,"ESCOLHA: P1 → TAG P1 → P2 → TAG P2",640,302,17,WHITE,false);
        text(c,"TOQUE UM RETRATO OU USE O DIRECIONAL",640,344,18,WHITE,false);
        text(c,"B VOLTA UMA ETAPA. CONFIRME APÓS AS QUATRO ESCOLHAS",640,386,18,WHITE,false);
        text(c,"TOQUE PARA FECHAR",640,455,17,BLUE,true);
    }

    // All presentation timing is monotonic wall time; render FPS never changes speed.
    private void tick(CharacterSelect s) {
        now=SystemClock.uptimeMillis();phase=(now-effectsEpoch)/1000f;
        for(int i=0;i<4;i++) {
            String current=s.pick(i/2,i%2);
            if(!current.equals(shownPicks[i])) {
                if(observedPicks) pickChangedAt[i]=now;
                shownPicks[i]=current;
            }
        }
        if(s.step()<shownStep)java.util.Arrays.fill(pickChangedAt,0);
        if(shownStep>=0&&s.step()>shownStep)pickChangedAt[s.step()-1]=now;
        shownStep=s.step();
        for(int side=0;side<2;side++) {
            String hero=s.preview(side);
            if(hero!=null&&!hero.equals(shownHero[side])) {
                // First frame after opening shows the hero settled; later changes animate.
                heroChangedAt[side]=observedPicks?now:0;
                shownHero[side]=hero;
            }
        }
        if(!s.ready())readyAt=-1;
        observedPicks=true;
    }

    private float pulse() {return .5f+.5f*(float)Math.sin(phase*2.4f);}
    private static int alphaColor(int color,int alpha) {
        return (Math.max(0,Math.min(255,alpha))<<24)|(color&0x00FFFFFF);
    }

    private void glow(Canvas c,int color,float strength) {
        float power=strength*(.65f+.35f*pulse());
        stroke(c,alphaColor(color,Math.round(45*power)),24);
        stroke(c,alphaColor(color,Math.round(110*power)),14);
        stroke(c,alphaColor(color,Math.round(235*power)),4);
    }

    private float selectionFlash(CharacterSelect s,String id) {
        float flash=0;
        for(int i=0;i<4;i++) if(s.chosen(i/2,i%2)&&id.equals(s.pick(i/2,i%2))&&pickChangedAt[i]>0) {
            flash=Math.max(flash,Math.max(0,1-(now-pickChangedAt[i])/380f));
        }
        return flash;
    }

    /** Animated corner brackets around the focused card, plus a chip naming the slot being chosen. */
    private void cursorBrackets(Canvas c,int color,String chip) {
        float push=3f+3f*pulse();
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(WHITE);
        float cx=(corners[0]+corners[2]+corners[4]+corners[6])/4,cy=(corners[1]+corners[5])/2;
        for(int k=0;k<4;k++) {
            float x=corners[k*2],y=corners[k*2+1];
            float ox=x<cx?-push:push,oy=y<cy?-push:push;
            float nx=corners[((k+1)%4)*2],ny=corners[((k+1)%4)*2+1];
            float px=corners[((k+3)%4)*2],py=corners[((k+3)%4)*2+1];
            // Two short arms along the card edges from each corner.
            c.drawLine(x+ox,y+oy,x+ox+(nx-x)*.18f,y+oy+(ny-y)*.18f,p);
            c.drawLine(x+ox,y+oy,x+ox+(px-x)*.18f,y+oy+(py-y)*.18f,p);
        }
        p.setStrokeCap(Paint.Cap.BUTT);p.setStyle(Paint.Style.FILL);
        float w=chip.length()*11f+22f, top=corners[1]-push-25;
        box.set(cx-w/2,top,cx+w/2,top+21);
        p.setColor(color);c.drawRoundRect(box,4,4,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);p.setColor(WHITE);c.drawRoundRect(box,4,4,p);
        p.setStyle(Paint.Style.FILL);
        text(c,chip,cx,top+16,15,WHITE,true);
    }

    private void edgeSpark(Canvas c) {
        float t=(phase*.55f)%1f;
        float x=corners[0]+(corners[2]-corners[0])*t,y=corners[1]+2;
        c.save();quadPath();c.clipPath(path);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);
        p.setColor(alphaColor(WHITE,255));c.drawLine(x-24,y,x+5,y,p);
        p.setStyle(Paint.Style.FILL);c.restore();
    }

    private void ambient(Canvas c) {
        // Bright side auras and forty visible motes stay behind the heroes and outside the grid.
        for(int side=0;side<2;side++) {
            c.save();c.clipRect(side==0?0:950,90,side==0?330:1280,680);
            int color=side==0?BLUE:RED;
            p.setStyle(Paint.Style.FILL);p.setShader(auras[side]);p.setAlpha(Math.round(155+85*pulse()));
            c.drawRect(side==0?0:950,90,side==0?330:1280,680,p);p.setShader(null);p.setAlpha(255);
            for(int i=0;i<20;i++) {
                float x=18+(i*47)%310+7*(float)Math.sin(phase*.6f+i);
                if(side==1)x=1280-x;
                float y=680-((phase*(16+i%4*3)+i*57)%590);
                int alpha=115+Math.round(80*(.5f+.5f*(float)Math.sin(phase+i)));
                p.setStyle(Paint.Style.FILL);p.setColor(alphaColor(color,alpha));
                c.drawCircle(x,y,2.5f+i%3*.65f,p);
                p.setStrokeWidth(2);p.setColor(alphaColor(color,alpha*2/3));
                c.drawLine(x,y+24,x,y,p);
            }
            c.restore();
        }
    }

    // Arm the reveal now, but start its clock on the first actual fight frame:
    // loading another character's atlases must not consume the whole fade.
    void beginFightTransition() {fightTransitionAt=-2;}
    void drawFightTransition(Canvas c) {
        if(fightTransitionAt==-2)fightTransitionAt=SystemClock.uptimeMillis();
        if(fightTransitionAt<0)return;
        long age=SystemClock.uptimeMillis()-fightTransitionAt;
        if(age>=360) {fightTransitionAt=-1;return;}
        float k=age/360f;
        c.drawColor(alphaColor(0xFF020611,Math.round(220*(1-k))));
        if(age<120) c.drawColor(alphaColor(WHITE,Math.round(150*(1-age/120f))));
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
                case "p09":l=.012f;t=.751f;r=.242f;bottom=.99f;break;
                case "p10":l=.009f;t=.709f;r=.254f;bottom=.974f;break;
                case "p11":l=.601f;t=.386f;r=.791f;bottom=.688f;break;
                case "p12":l=.807f;t=.014f;r=.991f;bottom=.323f;break;
                case "p13":l=.008f;t=.632f;r=.275f;bottom=.988f;break;
                case "p14":l=.009f;t=.631f;r=.275f;bottom=.99f;break;
                case "p15":l=.625f;t=.472f;r=.803f;bottom=.769f;break;
                case "p16":l=.812f;t=.025f;r=.992f;bottom=.326f;break;
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
