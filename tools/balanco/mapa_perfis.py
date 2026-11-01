#!/usr/bin/env python3
"""Mapas de calor da BateriaPerfis (docs/balanco/perfis-16.md): lê perfis_{total,luta,fixa}.csv
da pasta dada e grava os PNG agrupados por semelhança.

    python3 tools/balanco/mapa_perfis.py <pasta dos CSV> docs/balanco/perfis
"""
import csv, sys
from PIL import Image, ImageDraw, ImageFont
def ler(p):
    rows=list(csv.reader(open(p,encoding='utf-8')))
    nomes=rows[0][1:]; d=[[float(x) for x in r[1:]] for r in rows[1:]]
    return nomes,d
def ordem(d):
    # agrupamento por média (aglomerativo), devolve a ordem das folhas
    grupos=[[i] for i in range(len(d))]
    def dist(a,b): return sum(d[i][j] for i in a for j in b)/(len(a)*len(b))
    while len(grupos)>1:
        best=None
        for x in range(len(grupos)):
            for y in range(x+1,len(grupos)):
                v=dist(grupos[x],grupos[y])
                if best is None or v<best[0]: best=(v,x,y)
        _,x,y=best; g=grupos[x]+grupos[y]; grupos=[g]+[grupos[k] for k in range(len(grupos)) if k not in (x,y)]
    return grupos[0]
def cor(v):
    # 0 = verde (iguais), 1 = amarelo, 2+ = vermelho escuro
    t=max(0,min(1,v/2.0))
    if t<0.5:
        a=t/0.5; return (int(40+215*a), int(160+75*a), int(90-40*a))
    a=(t-0.5)/0.5; return (int(255-75*a), int(235-195*a), int(50-10*a))
def desenha(p, titulo, saida):
    nomes,d=ler(p); o=ordem(d)
    try: fonte=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',15); fb=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',20)
    except Exception: fonte=fb=ImageFont.load_default()
    c=46; m=110; n=len(o)
    W=m+n*c+20; H=m+n*c+90
    img=Image.new('RGB',(W,H),'white'); dr=ImageDraw.Draw(img)
    dr.text((10,10),titulo,fill='black',font=fb)
    for a,i in enumerate(o):
        dr.text((8,m+a*c+14),nomes[i],fill='black',font=fonte)
        lbl=Image.new('RGBA',(100,20),(255,255,255,0)); ImageDraw.Draw(lbl).text((0,0),nomes[i],fill='black',font=fonte)
        lbl=lbl.rotate(60,expand=True); img.paste(lbl,(m+a*c,m-lbl.size[1]+8),lbl)
        for b,k in enumerate(o):
            x,y=m+b*c,m+a*c
            if i==k: dr.rectangle([x,y,x+c-2,y+c-2],fill=(230,230,230)); continue
            v=d[i][k]; dr.rectangle([x,y,x+c-2,y+c-2],fill=cor(v))
            dr.text((x+7,y+14),f'{v:.2f}'.replace('.',','),fill='black',font=fonte)
    y=m+n*c+15
    dr.text((10,y),'verde = parecidos  ·  amarelo = ~1 (diferença normal no elenco)  ·  vermelho = muito diferentes',fill='black',font=fonte)
    dr.text((10,y+22),'ordem: agrupados por semelhança (os parecidos ficam lado a lado)',fill='black',font=fonte)
    img.save(saida); print(saida, [nomes[i] for i in o])
desenha(sys.argv[1]+'/perfis_total.csv','Semelhança entre os 16 (ficha fixa + luta)',sys.argv[2]+'/semelhanca-total.png')
desenha(sys.argv[1]+'/perfis_luta.csv','Semelhança no jeito de lutar (simulação)',sys.argv[2]+'/semelhanca-luta.png')
desenha(sys.argv[1]+'/perfis_fixa.csv','Semelhança na ficha fixa (pacote)',sys.argv[2]+'/semelhanca-fixa.png')
