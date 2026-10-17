package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.util.DisplayMetrics;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Single owner of decoded atlases. Every renderer of a match shares one cache, so an
 * atlas used by several fighters (or renderers) is decoded exactly once.
 *
 * <p>Folhas HD ({@link CharacterDefinition.Atlas#pixelScale} 2) são decodificadas no tamanho
 * que a tela do aparelho precisa ({@link #escalaPara}): numa tela 720p em 3/4, em 1080p ou
 * mais inteiras. O desenho compensa pela escala da {@link Folha}; a geometria da folha
 * (células, raiz, caixas) continua em pixels da folha original.
 */
final class SpriteAtlasCache {
    /**
     * Uma folha decodificada. {@code escalaX}/{@code escalaY}: pixels do bitmap por pixel da
     * folha original (1 = inteira; 0.5 = metade), medidos no bitmap que o decodificador entregou.
     */
    static final class Folha {
        final Bitmap bitmap;
        final float escalaX,escalaY;
        Folha(Bitmap bitmap,float escalaX,float escalaY) {this.bitmap=bitmap;this.escalaX=escalaX;this.escalaY=escalaY;}
        boolean inteira() {return escalaX==1f&&escalaY==1f;}
    }

    /**
     * Ampliação máxima (pixels da tela por pixel decodificado) na luta, com o zoom normal da
     * câmera: 1, a folha nunca é esticada fora dos closes. Assim os closes (cinema, ultra,
     * soco de câmera) ficam pelo menos tão definidos quanto a HD inteira numa tela 1080p 16:9,
     * a referência da arte. Numa tela 720p isso dá 3/4 (a metade ficaria 1,33x ampliada na luta
     * e visivelmente mais mole nos closes); em 1080p ou mais, a folha inteira.
     */
    static final float AMPLIACAO_MAXIMA = 1f;
    /** Escalas de decodificação, da menor para a maior. */
    private static final float[] ESCALAS = {0.5f, 0.75f, 1f};

    private final Context context;
    private final Map<String,Folha> sheets=new HashMap<>();
    /** Pixels da tela por unidade da tela virtual 1280x720 (o maior eixo, se esticar). */
    private final float densidadeTela;

    SpriteAtlasCache(Context context) {
        this.context=context;SpriteAutoFix.load(context);
        densidadeTela=densidadeTela(context.getResources().getDisplayMetrics());
    }

    /** A tela em paisagem: o lado maior cobre os 1280 virtuais e o menor os 720. */
    static float densidadeTela(DisplayMetrics tela) {
        return tela==null?Float.MAX_VALUE:densidadeTela(tela.widthPixels,tela.heightPixels);
    }

    static float densidadeTela(int largura,int altura) {
        if(largura<=0||altura<=0)return Float.MAX_VALUE;
        int longo=Math.max(largura,altura),curto=Math.min(largura,altura);
        return Math.max(longo/(float)Arena.VW,curto/(float)Arena.VH);
    }

    /**
     * Escala de decodificação de uma folha do personagem nesta tela: a menor que não amplia
     * mais que {@link #AMPLIACAO_MAXIMA} na luta e nunca abaixo da arte canônica 1x.
     */
    static float escalaPara(CharacterDefinition character,CharacterDefinition.Atlas atlas,float densidadeTela) {
        if(atlas.pixelScale<=1f)return 1f;
        float telaPorPixel=character.profile.worldScale/atlas.pixelScale*CameraRig.CAMERA_ZOOM*densidadeTela;
        for(float escala:ESCALAS) {
            if(escala*atlas.pixelScale<1f-1e-4f)continue;
            if(telaPorPixel/escala<=AMPLIACAO_MAXIMA*1.01f)return escala;
        }
        return 1f;
    }

    /** Decodes every atlas of a character before gameplay; never called per frame. */
    void preload(CharacterDefinition definition) {
        for(CharacterDefinition.Animation animation:definition.animations.values()) load(definition,animation.atlas);
    }

    Folha get(CharacterDefinition.Atlas atlas) {
        Folha folha=sheets.get(atlas.resource);
        if(folha==null)throw new IllegalStateException("Atlas not preloaded: "+atlas.resource);
        return folha;
    }

    int size() { return sheets.size(); }

    /** Bytes dos pixels decodificados de todas as folhas guardadas. */
    long decodedBytes() {
        long total=0;
        for(Folha f:sheets.values()) total+=f.bitmap.getByteCount();
        return total;
    }

    /** Frees every atlas the given characters do not use (team change). */
    void retainOnly(CharacterDefinition... characters) {
        Set<String> used=new HashSet<>();
        for(CharacterDefinition c:characters) for(CharacterDefinition.Animation a:c.animations.values()) used.add(a.atlas.resource);
        for(Iterator<Map.Entry<String,Folha>> it=sheets.entrySet().iterator();it.hasNext();) {
            Map.Entry<String,Folha> e=it.next();
            if(!used.contains(e.getKey())) { e.getValue().bitmap.recycle(); it.remove(); }
        }
    }

    private void load(CharacterDefinition character,CharacterDefinition.Atlas a) {
        Folha folha=sheets.get(a.resource);
        int largura=a.sheetWidth,altura=a.sheetHeight;
        if(folha==null) {
            int id=context.getResources().getIdentifier(a.resource,"drawable",context.getPackageName());
            if(id==0)throw new IllegalStateException("Missing atlas "+a.resource);
            Bitmap bitmap=decodificar(id,escalaPara(character,a,densidadeTela),largura,altura);
            if(bitmap==null)throw new IllegalStateException("Cannot decode "+a.resource);
            bitmap.setDensity(Bitmap.DENSITY_NONE);
            folha=new Folha(bitmap,bitmap.getWidth()/(float)largura,bitmap.getHeight()/(float)altura);
            sheets.put(a.resource,folha);
        }
        // Inteira, a folha tem exatamente o tamanho gerado. Reduzida, tem a escala pedida (ou a
        // metade, no Android antigo) a menos do arredondamento do decodificador, igual nos dois eixos.
        int w=folha.bitmap.getWidth(),h=folha.bitmap.getHeight();
        float pedida=escalaPara(character,a,densidadeTela);
        boolean valida=folha.inteira()?w==largura&&h==altura
            :pedida<1f&&(Math.abs(w-largura*pedida)<=1.5f||Math.abs(w-largura*0.5f)<=1.5f)
             &&Math.abs(folha.escalaX*altura-h)<=2f;
        if(!valida)
            throw new IllegalStateException("Invalid atlas dimensions: "+a.resource);
    }

    /**
     * Decodifica já na escala pedida (sem passar pela folha inteira na memória). O
     * ImageDecoder (Android 9+) reduz o WebP durante a decodificação em qualquer tamanho; antes
     * dele só a metade (inSampleSize 2) é possível, e 3/4 vira a folha inteira.
     */
    private Bitmap decodificar(int id,float escala,int largura,int altura) {
        if(escala<1f&&Build.VERSION.SDK_INT>=Build.VERSION_CODES.P) {
            final int alvoL=Math.round(largura*escala),alvoA=Math.round(altura*escala);
            try {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getResources(),id),
                    (decoder,info,source)->{
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        decoder.setTargetSize(alvoL,alvoA);
                    });
            } catch(IOException|RuntimeException e) {
                android.util.Log.w(MedicaoCarregamento.TAG,"ImageDecoder falhou; decodificando com BitmapFactory",e);
            }
        }
        BitmapFactory.Options options=new BitmapFactory.Options();options.inScaled=false;
        options.inSampleSize=escala<=0.5f?2:1;
        return BitmapFactory.decodeResource(context.getResources(),id,options);
    }
}
