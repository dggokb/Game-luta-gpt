package com.gamelutagpt;

import android.content.Context;
import android.content.Intent;

/** Abre o TESTE GL do Modo Teste (no PC há uma versão que não faz nada). */
final class AbrirTesteGl {
    private AbrirTesteGl() {}

    static void abrir(Context contexto, String cenario, String[] lutadores) {
        Intent intent = new Intent(contexto, TesteGlActivity.class);
        intent.putExtra(TesteGlActivity.CENARIO, cenario);
        intent.putExtra(TesteGlActivity.LUTADORES, lutadores);
        contexto.startActivity(intent);
    }
}
