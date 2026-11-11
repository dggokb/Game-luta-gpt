package com.gamelutagpt;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

/**
 * O modo de mistura somada (a luz do efeito soma com o que está embaixo, em vez de cobrir), como os
 * efeitos de luz do Street Fighter 6 (reports/Luz em jogos de luta profissionais.md). O Paint com
 * {@link #SOMA} pede a soma: o Canvas do Android faz sozinho (ADD), o OpenGL ({@link CanvasGl})
 * reconhece esta instância e o desenho do PC também (o modo ADD).
 */
final class Mistura {
    private Mistura() {}

    /** A mesma instância sempre: o CanvasGl compara por ela (o Android não deixa ler o modo). */
    static final PorterDuffXfermode SOMA = new PorterDuffXfermode(PorterDuff.Mode.ADD);
}
