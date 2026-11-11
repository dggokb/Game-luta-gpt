"""Normal map dos lutadores, tirado do próprio desenho (motor 4 de luz).

Os quadros vêm de vídeo: ninguém desenhou normal map. Aqui um modelo que estima a normal de uma
imagem (Metric3D v2, ViT-small, em ONNX, na CPU) olha cada quadro da folha e devolve para onde
cada ponto do corpo aponta. O que sai dele ainda não serve direto, e a ferramenta arruma:

  1. no tempo: quadros vizinhos da mesma animação são misturados onde o desenho não mudou (o
     modelo olha um quadro por vez e a normal "treme" de um para o outro);
  2. só as formas grandes: o detalhe fino é borrado (detalhe demais vira ruído e "cara de
     metal", como avisa o artigo do Sandfox em reports/Luz em jogos de luta profissionais.md);
  3. a borda: perto do contorno vale a normal da silhueta (a do motor 3), que nunca erra o lado;
  4. tamanho: o mapa sai a 1/4 da folha (1/16 dos pixels), com dois números por ponto (x e y;
     o z o shader refaz). Na placa de vídeo isso é 1/32 da memória da folha.

Saída: `<folha>_n.webp` ao lado da folha: uma imagem cinza com duas metades, a da esquerda com o
x da normal (para a direita da imagem) e a da direita com o y (para BAIXO da imagem, como o uv do
shader); 128 é zero. Cada metade tem o desenho de caixas da folha dividido por 4 (as caixas são
múltiplas de 4: tools/sprites/folhas.py). Cinza porque o WebP com perda guarda bem a luz e mal a
cor: com x e y em vermelho e verde o erro passava de 25 graus; assim fica perto de 1 grau com um
arquivo de ~7% do da folha (sem perda seriam 17%).

Uso:
    python3 tools/sprites/normal_map.py player_base            # todas as folhas do personagem
    python3 tools/sprites/normal_map.py player_base_jab p02_idle   # folhas soltas
    python3 tools/sprites/normal_map.py player_base --saida /tmp/normais --previa /tmp/previa
    python3 tools/sprites/normal_map.py player_base --metodo inflar   # sem modelo (só a silhueta estufada)

Precisa de numpy, opencv-python, onnxruntime e Pillow. O modelo (144 MB) é baixado na primeira
vez para ~/.cache/sokon/metric3d_vit_small.onnx (ou aponte SOKON_NORMAL para o arquivo). Leva
perto de 0,7 s por quadro em 4 núcleos; o elenco inteiro são algumas horas.

Medidas e comparação com os outros jeitos de tirar a normal: docs/vfx-continuar.md, "Motor de
luz 4".
"""
import argparse
import json
import os
import sys
import time
import urllib.request
from pathlib import Path

import cv2
import numpy as np
from PIL import Image

RAIZ = Path(__file__).resolve().parents[2]
RELATORIOS = RAIZ / 'tools/sprites/reports'
FOLHAS = RAIZ / 'android/app/src/main/res/drawable-nodpi'
MODELO_URL = 'https://huggingface.co/onnx-community/metric3d-vit-small/resolve/main/onnx/model.onnx'

# O mapa tem 1/FATOR do lado da folha (as caixas da folha são múltiplas de 4).
FATOR = 4
# Lado maior da imagem que o modelo vê (múltiplo de 14). Mais que isso não muda o mapa reduzido.
LADO = 518
# Formas grandes: borrão da normal, em fração da altura do quadro.
FORMAS = 0.012
# Borda: até esta distância do contorno (fração da altura) a normal da silhueta entra.
BORDA = 0.03
# No tempo: quantos quadros para cada lado entram, o peso deles e quanto a cor pode mudar.
TEMPO_RAIO, TEMPO_SIGMA, TEMPO_COR = 2, 1.2, 0.08
# Qualidade do WebP do mapa (100 = sem perda).
QUALIDADE = 90


def normalizar(n):
    return n / np.maximum(np.linalg.norm(n, axis=2, keepdims=True), 1e-6)


def silhueta(rgba):
    """A normal do motor 3: o alfa borrado cai para fora do corpo."""
    a = np.ascontiguousarray(rgba[..., 3])
    H = a.shape[0]
    b = cv2.GaussianBlur(a, (0, 0), 0.045 * H)
    gx = cv2.Sobel(b, cv2.CV_32F, 1, 0, ksize=3) / 8.0
    gy = cv2.Sobel(b, cv2.CV_32F, 0, 1, ksize=3) / 8.0
    k = 0.9 * 16 * 0.07 * H / 2.2
    return normalizar(np.dstack([-gx * k, -gy * k, np.ones_like(a)]))


def inflar(rgba):
    """Sem modelo: a silhueta estufada (lap(h) = -1 por dentro, raiz), como no Lumo de Johnston."""
    a = rgba[..., 3] > 0.5
    H = a.shape[0]
    niveis = [a.astype(np.float32)]
    while min(niveis[-1].shape) > 24:
        m = niveis[-1]
        niveis.append(cv2.resize(m, (max(1, m.shape[1] // 2), max(1, m.shape[0] // 2)), interpolation=cv2.INTER_AREA))
    h = np.zeros_like(niveis[-1])
    nucleo = np.array([[0, 1, 0], [1, 0, 1], [0, 1, 0]], np.float32)
    for m in reversed(niveis):
        if h.shape != m.shape:
            h = cv2.resize(h, (m.shape[1], m.shape[0]), interpolation=cv2.INTER_LINEAR) * 4.0
        for _ in range(60):
            h = np.where(m > 0.5, (cv2.filter2D(h, -1, nucleo, borderType=cv2.BORDER_CONSTANT) + 1.0) / 4.0, 0.0).astype(np.float32)
    h = cv2.GaussianBlur(np.sqrt(np.maximum(h, 0.0)), (0, 0), 0.004 * H)
    gx = cv2.Sobel(h, cv2.CV_32F, 1, 0, ksize=3) / 8.0
    gy = cv2.Sobel(h, cv2.CV_32F, 0, 1, ksize=3) / 8.0
    return normalizar(np.dstack([-gx, -gy, np.ones_like(h)]))


_sessao = None


def modelo():
    global _sessao
    if _sessao is None:
        import onnxruntime as ort
        caminho = Path(os.environ.get('SOKON_NORMAL', Path.home() / '.cache/sokon/metric3d_vit_small.onnx'))
        if not caminho.exists():
            caminho.parent.mkdir(parents=True, exist_ok=True)
            print(f'baixando o modelo para {caminho} (144 MB)...', file=sys.stderr)
            urllib.request.urlretrieve(MODELO_URL, caminho)
        opcoes = ort.SessionOptions()
        opcoes.intra_op_num_threads = os.cpu_count() or 4
        _sessao = ort.InferenceSession(str(caminho), opcoes, providers=['CPUExecutionProvider'])
    return _sessao


def do_modelo(rgba, lado=LADO):
    """A normal que o Metric3D vê no quadro (sobre cinza, com folga em volta)."""
    a = rgba[..., 3:4]
    H, W = a.shape[:2]
    folga = int(0.08 * H)
    tela = np.full((H + 2 * folga, W + 2 * folga, 3), 0.5, np.float32)
    tela[folga:folga + H, folga:folga + W] = rgba[..., :3] * a + 0.5 * (1 - a)
    k = lado / max(tela.shape[:2])
    h = max(14, int(round(tela.shape[0] * k / 14)) * 14)
    w = max(14, int(round(tela.shape[1] * k / 14)) * 14)
    x = cv2.resize(tela, (w, h), interpolation=cv2.INTER_AREA) * 255.0
    _, n, _ = modelo().run(None, {'pixel_values': x.transpose(2, 0, 1)[None].astype(np.float32)})
    n = cv2.resize(n[0].transpose(1, 2, 0), (W + 2 * folga, H + 2 * folga), interpolation=cv2.INTER_LINEAR)
    n = n[folga:folga + H, folga:folga + W].copy()
    # O modelo fala na câmera (z para dentro da tela); aqui z é para quem olha.
    n[..., 2] = -n[..., 2]
    return normalizar(n)


def estender(v, dentro):
    """Leva os valores de dentro do corpo para fora (o ponto do corpo mais perto)."""
    m = dentro.astype(np.uint8)
    if not m.any():
        return v
    _, rotulo = cv2.distanceTransformWithLabels(1 - m, cv2.DIST_L2, 5, labelType=cv2.DIST_LABEL_PIXEL)
    ys, xs = np.nonzero(m)
    tabela = np.zeros(rotulo.max() + 1, np.int64)
    tabela[rotulo[ys, xs]] = np.arange(len(ys))
    return v[ys, xs][tabela[rotulo]]


def formas_grandes(n, rgba, sigma=FORMAS):
    a = (rgba[..., 3] > 0.5).astype(np.float32)
    s = max(0.8, sigma * a.shape[0])
    num = cv2.GaussianBlur(n * a[..., None], (0, 0), s)
    den = cv2.GaussianBlur(a, (0, 0), s)[..., None]
    return normalizar(np.where(den > 1e-4, num / np.maximum(den, 1e-4), n))


def com_borda(n, rgba, largura=BORDA):
    a = rgba[..., 3] > 0.5
    dist = cv2.distanceTransform(a.astype(np.uint8), cv2.DIST_L2, 5)
    w = np.clip(1.0 - dist / (largura * a.shape[0]), 0, 1)[..., None]
    return normalizar(n * (1 - w) + silhueta(rgba) * w)


def na_celula(pacote, i, v, fundo):
    x, y, w, h, cx, cy = pacote['frames'][i]
    c = np.empty((pacote['frameHeight'], pacote['frameWidth'], v.shape[2]), np.float32)
    c[...] = fundo
    c[cy:cy + h, cx:cx + w] = v
    return c


def alisar_no_tempo(pacote, quadros, normais, volta):
    """Mistura cada normal com as dos quadros vizinhos onde o desenho é o mesmo (a cor não mudou)."""
    vivos = [i for i, q in enumerate(quadros) if q is not None]
    C = {i: na_celula(pacote, i, quadros[i], 0) for i in vivos}
    N = {i: na_celula(pacote, i, normais[i], (0, 0, 1)) for i in vivos}
    T = len(quadros)
    saida = list(normais)
    for t in vivos:
        soma = np.zeros_like(N[t])
        for k in range(-TEMPO_RAIO, TEMPO_RAIO + 1):
            j = t + k
            if volta:
                j %= T
            if j not in C:
                continue
            dc = np.abs(C[t][..., :3] - C[j][..., :3]).max(axis=2)
            w = np.exp(-k * k / (2 * TEMPO_SIGMA ** 2)) * np.exp(-(dc / TEMPO_COR) ** 2) * (C[j][..., 3] > 0.5)
            soma += N[j] * w[..., None]
        x, y, wq, hq, cx, cy = pacote['frames'][t]
        s = soma[cy:cy + hq, cx:cx + wq]
        saida[t] = np.where(np.linalg.norm(s, axis=2, keepdims=True) > 1e-5, normalizar(s), normais[t])
    return saida


def em_loop(folha):
    """A folha serve a alguma animação em loop (o último quadro é vizinho do primeiro)?"""
    for arquivo in (RAIZ / 'characters').glob('*/character.json'):
        if not folha.startswith(arquivo.parent.name):
            continue
        for a in json.loads(arquivo.read_text())['animations'].values():
            if a.get('atlas') == folha and a.get('loop'):
                return True
    return False


def reduzir(n, rgba, fator=FATOR):
    """A normal do quadro a 1/fator, já com o corpo estendido para fora (a borda não mistura com vazio)."""
    dentro = rgba[..., 3] > 0.5
    H, W = dentro.shape
    cheia = estender(n.astype(np.float32), dentro).astype(np.float32)
    return normalizar(cv2.resize(cheia, (W // fator, H // fator), interpolation=cv2.INTER_AREA))


def folha_normal(folha, metodo='modelo', lado=LADO, fator=FATOR):
    """(mapa uint8 RGB a 1/fator da folha, quadros RGBA, normais por quadro em tamanho cheio)."""
    relatorio = json.loads((RELATORIOS / f'{folha}.report.json').read_text())
    pacote = relatorio['packed']
    imagem = Image.open(FOLHAS / f'{folha}.webp').convert('RGBA')
    W, H = imagem.size
    quadros, normais = [], []
    for x, y, w, h, _, _ in pacote['frames']:
        if w == 0:
            quadros.append(None)
            normais.append(None)
            continue
        q = np.asarray(imagem.crop((x, y, x + w, y + h)), np.float32) / 255.0
        quadros.append(q)
        normais.append(do_modelo(q, lado) if metodo == 'modelo' else inflar(q))
    if metodo == 'modelo':
        normais = alisar_no_tempo(pacote, quadros, normais, em_loop(folha))
    mapa = np.zeros((-(-H // fator), -(-W // fator), 3), np.float32)
    mapa[..., 2] = 1.0
    for i, (x, y, w, h, _, _) in enumerate(pacote['frames']):
        if quadros[i] is None:
            continue
        n = normais[i]
        if metodo == 'modelo':
            n = com_borda(formas_grandes(n, quadros[i]), quadros[i])
        normais[i] = n
        mapa[y // fator:(y + h) // fator, x // fator:(x + w) // fator] = reduzir(n, quadros[i], fator)
    return np.round((mapa * 0.5 + 0.5) * 255.0).astype(np.uint8), quadros, normais


def gravar(mapa, destino, qualidade=QUALIDADE):
    """O mapa (uint8, x e y nos dois primeiros canais) como imagem cinza x|y lado a lado."""
    lado = Image.fromarray(np.hstack([mapa[..., 0], mapa[..., 1]])).convert('RGB')
    if qualidade >= 100:
        lado.save(destino, 'WEBP', lossless=True, quality=100, method=6)
    else:
        lado.save(destino, 'WEBP', quality=qualidade, method=6)


def ler(arquivo):
    """O mapa gravado, de volta com x e y nos dois primeiros canais (como o jogo lê)."""
    lado = np.asarray(Image.open(arquivo).convert('L'))
    w = lado.shape[1] // 2
    return np.dstack([lado[:, :w], lado[:, w:], np.full_like(lado[:, :w], 255)])


def acender(rgba, n, luz, cor=(1.0, 0.82, 0.55), raio=1.2, forca=1.4):
    """A conta do shader do jogo, para a prévia. luz: (x, y) em frações do quadro."""
    H, W = rgba.shape[:2]
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    dl = np.dstack([luz[0] * W - xs, luz[1] * H - ys]) / (raio * H)
    dist = np.linalg.norm(dl, axis=2)
    cai = np.clip(1 - dist, 0, 1)
    l3 = normalizar(np.dstack([dl, np.full_like(dist, 0.35)]))
    k = (np.clip(np.sum(n * l3, axis=2), 0, 1) * cai * cai * forca)[..., None]
    base = rgba[..., :3]
    a = rgba[..., 3:4]
    return np.clip(base + np.array(cor, np.float32) * k * (0.35 + 0.65 * base), 0, 1) * a + 0.18 * (1 - a)


def do_mapa(mapa, caixa, forma, fator=FATOR):
    """A normal de um quadro como o shader lê do mapa (bilinear, z refeito)."""
    x, y, w, h = caixa
    xy = mapa[y // fator:(y + h) // fator, x // fator:(x + w) // fator, :2].astype(np.float32) / 255.0 * 2 - 1
    xy = cv2.resize(xy, (forma[1], forma[0]), interpolation=cv2.INTER_LINEAR)
    return np.dstack([xy, np.sqrt(np.clip(1 - np.sum(xy * xy, axis=2), 0, 1))])


def previa(folha, mapa, quadros, pasta, fator=FATOR):
    """Uma imagem por folha: cada quadro, a normal dele e ele aceso da frente, por cima e por trás."""
    pacote = json.loads((RELATORIOS / f'{folha}.report.json').read_text())['packed']
    linhas = []
    passo = max(1, len(quadros) // 6)
    for i in range(0, len(quadros), passo):
        q = quadros[i]
        if q is None:
            continue
        n = do_mapa(mapa, pacote['frames'][i][:4], q.shape[:2], fator)
        a = q[..., 3:4]
        cor = n * 0.5 + 0.5
        partes = [q[..., :3] * a + 0.18 * (1 - a), cor * a + 0.18 * (1 - a),
                  acender(q, n, (1.2, 0.35)), acender(q, n, (0.5, -0.3), cor=(0.6, 0.8, 1.0)), acender(q, n, (-0.2, 0.4))]
        alto = max(p.shape[0] for p in partes)
        partes = [np.pad(p, ((0, alto - p.shape[0]), (0, 0), (0, 0)), constant_values=0.18) for p in partes]
        linhas.append(np.hstack(partes))
    if not linhas:
        return
    largo = max(l.shape[1] for l in linhas)
    linhas = [np.pad(l, ((0, 0), (0, largo - l.shape[1]), (0, 0)), constant_values=0.18) for l in linhas]
    Path(pasta).mkdir(parents=True, exist_ok=True)
    Image.fromarray((np.vstack(linhas) * 255).astype(np.uint8)).save(Path(pasta) / f'{folha}_n_previa.png')


def folhas_de(nome):
    if (RELATORIOS / f'{nome}.report.json').exists():
        return [nome]
    achadas = sorted(p.name[:-len('.report.json')] for p in RELATORIOS.glob(f'{nome}_*.report.json'))
    # p01 mora em player_base; "p1" não pode pegar "p10": só o prefixo inteiro até o "_".
    return [f for f in achadas if (FOLHAS / f'{f}.webp').exists()]


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('nomes', nargs='+', help='personagem (todas as folhas dele) ou folha')
    p.add_argument('--saida', default=str(FOLHAS), help='pasta dos <folha>_n.webp (padrão: ao lado das folhas)')
    p.add_argument('--metodo', choices=['modelo', 'inflar'], default='modelo')
    p.add_argument('--lado', type=int, default=LADO)
    p.add_argument('--fator', type=int, default=FATOR, choices=[4, 8])
    p.add_argument('--qualidade', type=int, default=QUALIDADE, help='do WebP (100 = sem perda)')
    p.add_argument('--previa', help='pasta para uma imagem de conferência por folha')
    p.add_argument('--refazer', action='store_true', help='refaz as que já existem')
    a = p.parse_args()
    saida = Path(a.saida)
    saida.mkdir(parents=True, exist_ok=True)
    folhas = [f for nome in a.nomes for f in folhas_de(nome)]
    if not folhas:
        sys.exit('nenhuma folha achada')
    total_folha = total_mapa = quadros_feitos = 0
    inicio = time.time()
    for k, folha in enumerate(folhas):
        destino = saida / f'{folha}_n.webp'
        if destino.exists() and not a.refazer:
            continue
        mapa, quadros, _ = folha_normal(folha, a.metodo, a.lado, a.fator)
        gravar(mapa, destino, a.qualidade)
        if a.previa:
            previa(folha, ler(destino), quadros, a.previa, a.fator)
        quadros_feitos += sum(q is not None for q in quadros)
        total_folha += (FOLHAS / f'{folha}.webp').stat().st_size
        total_mapa += destino.stat().st_size
        print(f'[{k + 1}/{len(folhas)}] {folha}: {mapa.shape[1]}x{mapa.shape[0]}, {destino.stat().st_size // 1024} KB '
              f'({time.time() - inicio:.0f} s)', flush=True)
    if quadros_feitos:
        print(f'{quadros_feitos} quadros em {time.time() - inicio:.0f} s; arquivos: folhas {total_folha / 2**20:.1f} MB, '
              f'normais {total_mapa / 2**20:.1f} MB ({100 * total_mapa / max(total_folha, 1):.1f}%)')


if __name__ == '__main__':
    main()
