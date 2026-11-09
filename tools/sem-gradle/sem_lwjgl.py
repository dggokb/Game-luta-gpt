#!/usr/bin/env python3
"""Gera o que o modo sem Gradle precisa no lugar das dependências do Maven (tools/sem-gradle):

- GLES30, GLUtils e DesenhoGl sem LWJGL (mesma API, corpo vazio): a luta sai pelo Java2D;
- BitmapFactory lendo WEBP pela ponte do Pillow (WebpPonte), no lugar do plugin do TwelveMonkeys.

    python3 sem_lwjgl.py <repo>/android <pasta de saída>
"""
import re, sys
from pathlib import Path
A = Path(sys.argv[1]); OUT = Path(sys.argv[2])

def strip_bodies(src):
    src = re.sub(r'^import org\.lwjgl[^\n]*\n', '', src, flags=re.M).replace('GLCapabilities ', 'Object ')
    out, i, stack, n = [], 0, ['file'], len(src)
    while i < n:
        c = src[i]
        if src.startswith('//', i):
            j = src.find('\n', i); out.append(src[i:j]); i = j; continue
        if src.startswith('/*', i):
            j = src.find('*/', i) + 2; out.append(src[i:j]); i = j; continue
        if c == '"':
            j = i + 1
            while src[j] != '"' or src[j-1] == '\\': j += 1
            out.append(src[i:j+1]); i = j + 1; continue
        if c == "'":
            j = src.find("'", i + 2 if src[i+1] == '\\' else i + 1); out.append(src[i:j+1]); i = j + 1; continue
        if c == '{':
            kind = 'other'
            if True:
                # corpo de método/construtor/inicializador (não de classe aninhada)
                head = ''.join(out)
                k = max(head.rfind(';'), head.rfind('}'), head.rfind('{'))
                decl = head[k+1:]
                is_type = re.search(r'\b(class|interface|enum)\b', decl) is not None
                if is_type: kind = 'type'
                is_field = '=' in decl and '(' not in decl.split('=')[0]
                if stack[-1] == 'type' and not is_type and not is_field and '(' in decl:
                    d, j = 1, i + 1
                    while d:
                        ch = src[j]
                        if ch == '"':
                            j += 1
                            while src[j] != '"' or src[j-1] == '\\': j += 1
                        elif ch == '{': d += 1
                        elif ch == '}': d -= 1
                        j += 1
                    out.append('{ throw new UnsupportedOperationException("stub"); }')
                    i = j; continue
            stack.append(kind)
        elif c == '}':
            stack.pop()
        out.append(c); i += 1
    return ''.join(out)

for rel in ('pc/src/main/java/android/opengl/GLES30.java', 'pc/src/main/java/android/opengl/GLUtils.java',
            'pc/src/substituicoes/java/com/gamelutagpt/DesenhoGl.java'):
    src = (A / rel).read_text(encoding='utf-8')
    pkg = re.search(r'package ([\w.]+);', src).group(1)
    dst = OUT / pkg.replace('.', '/') / Path(rel).name
    dst.parent.mkdir(parents=True, exist_ok=True)
    texto = strip_bodies(src)
    if dst.name == 'DesenhoGl.java':
        # O que a luta pergunta mesmo sem OpenGL: "não há desenho em OpenGL aberto".
        vazio = '{ throw new UnsupportedOperationException("stub"); }'
        for assinatura, corpo in (
                ('static DesenhoGl noQuadro() ', '{ return null; }'),
                ('static void soltarDepois(Bitmap folha) ', '{ }'),
                ('static RenderCanvas comoRenderCanvas(Canvas c) ',
                 '{ return c instanceof CanvasGl ? ((CanvasGl)c).comoRenderCanvas() : null; }')):
            if assinatura + vazio not in texto:
                sys.exit('DesenhoGl mudou: não achei ' + assinatura)
            texto = texto.replace(assinatura + vazio, assinatura + corpo)
    dst.write_text(texto, encoding='utf-8')

fabrica = (A / 'pc/src/main/java/android/graphics/BitmapFactory.java').read_text(encoding='utf-8')
leitura = '            byte[] data = readAll(is);\n'
if fabrica.count(leitura) != 1:
    sys.exit('BitmapFactory mudou: não achei a leitura dos bytes')
dst = OUT / 'android/graphics/BitmapFactory.java'
dst.parent.mkdir(parents=True, exist_ok=True)
dst.write_text(fabrica.replace(leitura, leitura + '            data = WebpPonte.png(data);\n'), encoding='utf-8')
