# Três conceitos de HUD compacto (21/11)

Pedido do dono: "o HUD está cobrindo muito a tela" (diminuir por igual foi "gambiarra") e "faz três
conceitos pra escolhermos". Hoje a barra de cima (`hud-raios.png`) tem três andares e desce até
~26% da altura da tela; no celular a metade de baixo é dos controles.

- **A · Faixa única (13%)**: rosto menor na ponta, vida, e embaixo dela numa linha só o TAG (rosto
  pequeno do parceiro e a barrinha), o SHINKAI e o Super; relógio menor no centro e o MENU vira ícone.
- **B · Medidores embaixo (10% em cima)**: em cima só rosto, vida e TAG; o Super e o SHINKAI dos dois
  numa placa no rodapé, no meio, entre o direcional e os botões.
- **C · Nos cantos (14%)**: o bloco de cada um só no canto (36% da largura cada), o meio do topo
  livre; os medidores dentro do bloco.

São maquetes de layout (o tamanho e o lugar de cada coisa), desenhadas por código só para escolher;
a arte final segue o estilo da barra de raios. Os círculos claros são os controles do celular.
`hud.html` desenha (`?c=A|B|C&ctl=1`), `hud_render.js` tira as fotos; o fundo é uma foto da luta
sem o HUD.
