package com.gamelutagpt.ultra;

/** Eventos que a cinemática manda de volta para o jogo. */
public interface UltraListener {
    /**
     * Um golpe da cinemática acertou.
     *
     * @param hitIndex 0, 1 ou 2 (o último é a detonação final)
     * @param damageFraction fração do dano total do ultra, já com o bônus do toque
     */
    void onUltraHit(int hitIndex, float damageFraction);

    /** Pedido para tocar um efeito sonoro; a plataforma ignora se não tiver o arquivo. */
    void onUltraSound(String soundId);

    /** A página quebrou e o jogo deve voltar a rodar. */
    void onUltraFinished(UltraGrade grade);
}
