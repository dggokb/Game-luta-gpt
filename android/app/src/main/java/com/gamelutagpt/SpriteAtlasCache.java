package com.gamelutagpt;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.util.DisplayMetrics;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Single owner of decoded atlases. Every renderer of a match shares one cache, so an
 * atlas used by several fighters (or renderers) is decoded exactly once.
 *
 * <p>Folhas HD ({@link CharacterDefinition.Atlas#pixelScale} 2) são decodificadas no tamanho
 * que a tela do aparelho precisa ({@link #escalaPara}): numa tela 720p em 3/4, em 1080p ou
 * mais inteiras. O desenho compensa pela escala da {@link Folha}; a geometria da folha
 * (células, raiz, caixas) continua em pixels da folha original.
 *
 * <p>Decodificação em paralelo e em segundo plano: {@link #antecipar} começa a decodificar as
 * folhas de quem vai lutar (no menu, na seleção, nas páginas da história) sem bloquear quem
 * chamou; {@link #preload} espera só o que ainda falta, com prioridade sobre as antecipações, e
 * a própria thread que espera ajuda a decodificar. Os trabalhadores ({@link #trabalhadores}) só
 * mexem no estado interno deste cache; quem desenha só lê folhas prontas.
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

    /**
     * Bytes que podem estar sendo decodificados ao mesmo tempo (fora a primeira folha, que
     * sempre pode): segura o pico de memória quando muitos núcleos decodificam juntos.
     */
    static final long EM_VOO_MAXIMO=192L<<20;

    /** Uma folha: decodificada, na fila, decodificando, ou que falhou. Estado guardado pela trava. */
    private static final class Entrada {
        final CharacterDefinition character;
        final CharacterDefinition.Atlas atlas;
        final float escala;
        final long bytes;
        /** Pronta para desenhar (escrita uma vez, lida sem trava por quem desenha). */
        volatile Folha folha;
        Throwable erro;
        boolean naFila,decodificando,descartada;
        /** Só uma antecipação pediu (nenhuma luta a usou ainda): pode sair por outra antecipação. */
        boolean soAntecipada;
        Entrada(CharacterDefinition character,CharacterDefinition.Atlas atlas,float escala) {
            this.character=character;this.atlas=atlas;this.escala=escala;
            bytes=(long)Math.ceil(atlas.sheetWidth*escala)*(long)Math.ceil(atlas.sheetHeight*escala)*4L;
        }
    }

    private final Context context;
    /** Pixels da tela por unidade da tela virtual 1280x720 (o maior eixo, se esticar). */
    private final float densidadeTela;
    /** Quanto as antecipações podem ocupar no total (folhas prontas + em andamento). */
    private final long orcamentoAntecipado;
    private final Object trava=new Object();
    /** Por recurso; a ordem é a de chegada (só para os números do log). */
    private final Map<String,Entrada> folhas=new LinkedHashMap<>();
    /** Fila de decodificação: as que alguém espera na frente, as antecipadas atrás. */
    private final ArrayDeque<Entrada> fila=new ArrayDeque<>();
    private long bytesProntos,bytesEmAndamento,bytesDecodificando;
    private int trabalhadoresVivos;
    /** Threads de decodificação além da que espera no {@link #preload}. */
    private final int trabalhadores;

    SpriteAtlasCache(Context context) {
        this.context=context;SpriteAutoFix.load(context);
        densidadeTela=densidadeTela(context.getResources().getDisplayMetrics());
        orcamentoAntecipado=orcamentoAntecipado(context);
        // Um núcleo fica para o loop do jogo (menu, seleção, tela de carregamento). No PC,
        // -Dgameluta.folhas=N fixa quantos (0: só a thread que espera, como antes).
        Integer fixo=Integer.getInteger("gameluta.folhas");
        trabalhadores=fixo!=null?Math.max(0,fixo):Math.max(1,Math.min(4,Runtime.getRuntime().availableProcessors()-1));
    }

    /**
     * 35% da memória do aparelho, entre 384 MB e 2,5 GB: antecipar nunca tira lugar do que a
     * luta atual usa, e um aparelho com pouca memória antecipa menos (o resto carrega na tela de
     * carregamento, como antes).
     */
    private static long orcamentoAntecipado(Context context) {
        long total=0;
        try {
            android.app.ActivityManager am=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
            if(am!=null) {
                android.app.ActivityManager.MemoryInfo info=new android.app.ActivityManager.MemoryInfo();
                am.getMemoryInfo(info);
                total=info.totalMem;
            }
        } catch(RuntimeException e) {
            total=0;
        }
        if(total<=0)total=2L<<30;
        return Math.max(384L<<20,Math.min(2560L<<20,total*35/100));
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

    /** Decodes every atlas of the characters before gameplay (in parallel); never called per frame. */
    void preload(CharacterDefinition... characters) {
        List<Entrada> esperadas=new ArrayList<>();
        synchronized(trava) {
            for(CharacterDefinition c:characters) for(CharacterDefinition.Animation a:c.animations.values()) {
                Entrada e=entrada(c,a.atlas,true);
                if(e.folha==null&&!esperadas.contains(e))esperadas.add(e);
            }
            garantirTrabalhadores();
        }
        // Quem espera também decodifica: pega da frente da fila enquanto houver o que fazer.
        for(int i=0;i<esperadas.size();i++) {
            while(true) {
                Entrada e=esperadas.get(i);
                Entrada proxima;
                synchronized(trava) {
                    if(e.folha!=null)break;
                    if(e.descartada) {
                        // Outro carregamento soltou esta folha enquanto esperávamos: pede de novo.
                        esperadas.set(i,entrada(e.character,e.atlas,true));
                        garantirTrabalhadores();
                        continue;
                    }
                    if(e.erro!=null) {
                        // Falhou no trabalhador: tenta de novo aqui, para o erro sair em quem pediu.
                        e.erro=null;
                        proxima=e;e.decodificando=true;bytesDecodificando+=e.bytes;
                    } else {
                        proxima=pegarDaFila(true);
                        if(proxima==null) {
                            try { trava.wait(); } catch(InterruptedException ex) {
                                Thread.currentThread().interrupt();throw new IllegalStateException("Carregamento interrompido",ex);
                            }
                            continue;
                        }
                    }
                }
                decodificar(proxima,proxima==e);
            }
        }
    }

    /**
     * Começa a decodificar as folhas dos personagens em segundo plano, sem esperar, enquanto o
     * total couber no orçamento das antecipações. Devolve false se alguma ficou de fora.
     */
    boolean antecipar(CharacterDefinition... characters) {
        boolean todas=true;
        java.util.Set<String> queridas=new java.util.HashSet<>();
        for(CharacterDefinition c:characters) for(CharacterDefinition.Animation a:c.animations.values()) queridas.add(a.atlas.resource);
        synchronized(trava) {
            // A antecipação nova vale mais que as antigas (o jogador já saiu de onde elas valiam):
            // as antigas ainda na fila saem dela, e as prontas cedem lugar se faltar orçamento.
            for(Iterator<Map.Entry<String,Entrada>> it=folhas.entrySet().iterator();it.hasNext();) {
                Entrada e=it.next().getValue();
                if(e.soAntecipada&&e.naFila&&!queridas.contains(e.atlas.resource)) {
                    fila.remove(e);e.naFila=false;e.descartada=true;bytesEmAndamento-=e.bytes;it.remove();
                }
            }
            for(CharacterDefinition c:characters) for(CharacterDefinition.Animation a:c.animations.values()) {
                if(folhas.containsKey(a.atlas.resource))continue;
                float escala=escalaPara(c,a.atlas,densidadeTela);
                Entrada nova=new Entrada(c,a.atlas,escala);
                if(bytesProntos+bytesEmAndamento+nova.bytes>orcamentoAntecipado) liberarAntecipadas(queridas,nova.bytes);
                if(bytesProntos+bytesEmAndamento+nova.bytes>orcamentoAntecipado) { todas=false;continue; }
                nova.soAntecipada=true;
                folhas.put(a.atlas.resource,nova);
                bytesEmAndamento+=nova.bytes;
                nova.naFila=true;fila.addLast(nova);
            }
            garantirTrabalhadores();
            trava.notifyAll();
        }
        return todas;
    }

    /** Solta folhas prontas que só antecipações antigas pediram, até caber {@code bytes}. Com a trava. */
    private void liberarAntecipadas(java.util.Set<String> queridas,long bytes) {
        for(Iterator<Map.Entry<String,Entrada>> it=folhas.entrySet().iterator();it.hasNext();) {
            if(bytesProntos+bytesEmAndamento+bytes<=orcamentoAntecipado)return;
            Entrada e=it.next().getValue();
            if(!e.soAntecipada||e.folha==null||queridas.contains(e.atlas.resource))continue;
            it.remove();e.descartada=true;
            bytesProntos-=e.folha.bitmap.getByteCount();
            e.folha.bitmap.recycle();
        }
    }

    /**
     * A folha pronta para desenhar. Uma folha que não foi carregada antes (erro de quem montou
     * a luta) é decodificada aqui mesmo, com aviso no log: um engasgo em vez de fechar o jogo.
     */
    Folha get(CharacterDefinition character,CharacterDefinition.Atlas atlas) {
        Entrada e;
        synchronized(trava) {
            e=folhas.get(atlas.resource);
            // Desenhada: não pode mais sair por causa de uma antecipação.
            if(e!=null)e.soAntecipada=false;
        }
        Folha folha=e!=null?e.folha:null;
        if(folha!=null)return folha;
        android.util.Log.w(MedicaoCarregamento.TAG,"folha "+atlas.resource+" decodificada na hora de desenhar");
        preload(character);
        synchronized(trava) { e=folhas.get(atlas.resource); }
        if(e==null||e.folha==null)throw new IllegalStateException("Atlas not preloaded: "+atlas.resource);
        return e.folha;
    }

    /** A folha já está pronta para desenhar (sem decodificar na hora). */
    boolean pronta(String resource) {
        synchronized(trava) {
            Entrada e=folhas.get(resource);
            return e!=null&&e.folha!=null;
        }
    }

    /** Folhas prontas. */
    int size() {
        synchronized(trava) {
            int n=0;
            for(Entrada e:folhas.values()) if(e.folha!=null)n++;
            return n;
        }
    }

    /** Bytes dos pixels decodificados de todas as folhas prontas. */
    long decodedBytes() {
        synchronized(trava) { return bytesProntos; }
    }

    /** Folhas ainda na fila ou decodificando. */
    int pendentes() {
        synchronized(trava) { return folhas.size()-size(); }
    }

    /**
     * Frees every atlas the given characters do not use (team change). Uma folha ainda na fila
     * sai dela; uma que está decodificando é jogada fora quando terminar.
     */
    void retainOnly(CharacterDefinition... characters) {
        java.util.Set<String> used=new java.util.HashSet<>();
        for(CharacterDefinition c:characters) for(CharacterDefinition.Animation a:c.animations.values()) used.add(a.atlas.resource);
        synchronized(trava) {
            for(Iterator<Map.Entry<String,Entrada>> it=folhas.entrySet().iterator();it.hasNext();) {
                Entrada e=it.next().getValue();
                if(used.contains(e.atlas.resource))continue;
                it.remove();
                e.descartada=true;
                if(e.folha!=null) {
                    bytesProntos-=e.folha.bitmap.getByteCount();
                    e.folha.bitmap.recycle();
                } else {
                    // Na fila, decodificando ou esperando nova tentativa: sai da conta agora (quem
                    // decodifica vê "descartada" e só recicla o bitmap).
                    if(e.naFila) {fila.remove(e);e.naFila=false;}
                    bytesEmAndamento-=e.bytes;
                }
            }
            trava.notifyAll();
        }
    }

    /** A entrada da folha, criada (e posta na fila) se ainda não existe. Com a trava. */
    private Entrada entrada(CharacterDefinition c,CharacterDefinition.Atlas a,boolean urgente) {
        Entrada e=folhas.get(a.resource);
        if(e==null) {
            e=new Entrada(c,a,escalaPara(c,a,densidadeTela));
            folhas.put(a.resource,e);
            bytesEmAndamento+=e.bytes;
            e.naFila=true;
            if(urgente)fila.addFirst(e); else fila.addLast(e);
        } else if(urgente&&e.naFila) {
            // Antecipada e ainda na fila: passa para a frente.
            fila.remove(e);fila.addFirst(e);
        }
        if(urgente)e.soAntecipada=false;
        return e;
    }

    /**
     * A próxima da fila que cabe no limite de bytes decodificando ao mesmo tempo (a primeira
     * sempre cabe), já marcada como decodificando; null se não há nenhuma. Com a trava.
     */
    private Entrada pegarDaFila(boolean quemEspera) {
        Entrada e=fila.peekFirst();
        if(e==null)return null;
        if(bytesDecodificando>0&&bytesDecodificando+e.bytes>EM_VOO_MAXIMO)return null;
        fila.pollFirst();
        e.naFila=false;e.decodificando=true;bytesDecodificando+=e.bytes;
        return e;
    }

    /** Decodifica fora da trava e publica o resultado. {@code propagar}: o erro sobe para quem chamou. */
    private void decodificar(Entrada e,boolean propagar) {
        Folha folha=null;Throwable erro=null;
        try {
            folha=carregar(e.character,e.atlas,e.escala);
        } catch(RuntimeException|OutOfMemoryError ex) {
            erro=ex;
        }
        synchronized(trava) {
            e.decodificando=false;
            bytesDecodificando-=e.bytes;
            if(!e.descartada)bytesEmAndamento-=e.bytes;
            if(folha!=null) {
                if(e.descartada) folha.bitmap.recycle();
                else { e.folha=folha;bytesProntos+=folha.bitmap.getByteCount(); }
            } else if(!e.descartada) {
                if(propagar) {
                    // Quem chamou recebe o erro; a folha sai do cache (um próximo pedido tenta de novo).
                    folhas.remove(e.atlas.resource);
                } else {
                    e.erro=erro;bytesEmAndamento+=e.bytes;
                }
            }
            trava.notifyAll();
        }
        if(erro!=null&&propagar) {
            if(erro instanceof RuntimeException)throw (RuntimeException)erro;
            throw (Error)erro;
        }
        if(erro!=null)android.util.Log.w(MedicaoCarregamento.TAG,"folha "+e.atlas.resource+" falhou em segundo plano",erro);
    }

    /** Liga os trabalhadores que faltam (eles terminam quando a fila esvazia). Com a trava. */
    private void garantirTrabalhadores() {
        int faltam=Math.min(trabalhadores,fila.size())-trabalhadoresVivos;
        for(int i=0;i<faltam;i++) {
            trabalhadoresVivos++;
            Thread t=new Thread(this::trabalhar,"Folhas");
            t.setDaemon(true);
            // Abaixo do loop do jogo: antecipar não pode engasgar o menu.
            t.setPriority(Math.max(Thread.MIN_PRIORITY,Thread.NORM_PRIORITY-2));
            t.start();
        }
    }

    private void trabalhar() {
        while(true) {
            Entrada e;
            synchronized(trava) {
                e=pegarDaFila(false);
                while(e==null&&!fila.isEmpty()) {
                    // Cheia de folhas decodificando: espera alguma terminar.
                    try { trava.wait(200); } catch(InterruptedException ex) { trabalhadoresVivos--;return; }
                    e=pegarDaFila(false);
                }
                if(e==null) { trabalhadoresVivos--;return; }
            }
            decodificar(e,false);
        }
    }

    private Folha carregar(CharacterDefinition character,CharacterDefinition.Atlas a,float escala) {
        int largura=a.sheetWidth,altura=a.sheetHeight;
        int id=context.getResources().getIdentifier(a.resource,"drawable",context.getPackageName());
        if(id==0)throw new IllegalStateException("Missing atlas "+a.resource);
        Bitmap bitmap=decodificar(id,escala,largura,altura);
        if(bitmap==null)throw new IllegalStateException("Cannot decode "+a.resource);
        bitmap.setDensity(Bitmap.DENSITY_NONE);
        Folha folha=new Folha(bitmap,bitmap.getWidth()/(float)largura,bitmap.getHeight()/(float)altura);
        // Inteira, a folha tem exatamente o tamanho gerado. Reduzida, tem a escala pedida (ou a
        // metade, no Android antigo) a menos do arredondamento do decodificador, igual nos dois eixos.
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        boolean valida=folha.inteira()?w==largura&&h==altura
            :escala<1f&&(Math.abs(w-largura*escala)<=1.5f||Math.abs(w-largura*0.5f)<=1.5f)
             &&Math.abs(folha.escalaX*altura-h)<=2f;
        if(!valida) {
            bitmap.recycle();
            throw new IllegalStateException("Invalid atlas dimensions: "+a.resource);
        }
        return folha;
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
