package com.gamelutagpt;

/**
 * Gerado por GeraAlcanceCpu (teste): o alcance real de cada golpe de chão, medido no motor contra o
 * Ryūga parado (o golpe acerta pelo desenho). Cada entrada: lutador, golpe, a ponta contra quem
 * está em pé e contra quem está agachado (px do centro de quem bate até a frente do corpo do
 * rival; -1 = não acerta). Não editar à mão: rodar o GeraAlcanceCpu de novo.
 */
final class CpuReachTable {
    private CpuReachTable() {}

    static final String DATA =
        "player_base L 145 -1;player_base M 219 191;player_base H 255 229;player_base 2L 215 181;player_base 2M 241 205;player_base 2H 127 101;player_base 6M 231 195;player_base S2 217 183;player_base S3 399 375;player_base S4 299 275;" +
        "p02 L 179 155;p02 M 223 189;p02 H 223 113;p02 2L 143 109;p02 2M 177 143;p02 2H 131 105;p02 6M 223 189;p02 S2 111 87;p02 S3 163 137;p02 S4 335 311;" +
        "p03 L 167 -1;p03 M 161 -1;p03 H 169 -1;p03 2L 191 167;p03 2M 271 237;p03 2H 129 105;p03 6M 161 -1;p03 S2 243 217;p03 S3 411 385;" +
        "p04 L 177 -1;p04 M 211 183;p04 H 219 183;p04 2L 183 159;p04 2M 247 211;p04 2H 137 111;p04 6M 211 183;p04 S2 243 217;p04 S3 149 113;p04 S4 317 291;" +
        "p05 L 163 139;p05 M 237 201;p05 H 163 137;p05 2L 177 153;p05 2M 177 143;p05 2H 143 117;p05 6M 237 201;p05 S3 605 605;p05 S4 433 407;" +
        "p06 L 131 107;p06 M 201 117;p06 H 173 135;p06 2L 183 147;p06 2M 299 265;p06 2H 135 109;p06 6M 201 117;p06 S2 129 105;p06 S3 405 379;p06 S4 375 349;" +
        "p07 L 155 -1;p07 M 221 -1;p07 H 125 -1;p07 2L 163 129;p07 2M 151 117;p07 2H 223 197;p07 6M 221 -1;p07 S 221 197;p07 S2 263 239;p07 S3 299 275;p07 S4 365 341;" +
        "p08 L 157 121;p08 M 233 209;p08 H 391 365;p08 2L 169 133;p08 2M 159 123;p08 2H 157 131;p08 6M 233 209;p08 S2 403 377;p08 S3 173 147;p08 S4 347 323;" +
        "p09 L 187 -1;p09 M 231 127;p09 H 153 129;p09 2L 237 203;p09 2M 231 195;p09 2H 139 113;p09 6M 231 127;p09 S2 101 77;p09 S3 135 109;p09 S4 467 443;" +
        "p10 L 159 -1;p10 M 241 215;p10 H 155 131;p10 2L 167 143;p10 2M 211 175;p10 2H 183 159;p10 6M 241 215;p10 S2 163 137;p10 S3 245 221;p10 S4 377 343;" +
        "p11 L 193 -1;p11 M 177 -1;p11 H 193 169;p11 2L 213 187;p11 2M 221 187;p11 2H 137 111;p11 6M 177 -1;p11 S3 147 121;p11 S4 407 383;" +
        "p12 L 229 203;p12 M 221 187;p12 H 119 95;p12 2L 215 181;p12 2M 257 221;p12 2H 127 103;p12 6M 221 187;p12 S 425 391;p12 S2 225 201;p12 S3 211 187;p12 S4 305 281;" +
        "p14 L 199 173;p14 M 213 187;p14 H 271 247;p14 2L 205 169;p14 2M 279 243;p14 2H 153 127;p14 6M 199 165;p14 S2 437 403;p14 S3 433 397;p14 S4 389 353;" +
        "p15 L 155 -1;p15 M 153 -1;p15 H 257 131;p15 2L 165 141;p15 2M 289 255;p15 2H 163 137;p15 6M 273 237;p15 S2 -1 -1;p15 S3 291 267;p15 S4 315 291;" +
        "p16 L 161 -1;p16 M 181 91;p16 H 227 203;p16 2L 153 127;p16 2M 253 217;p16 2H 183 159;p16 6M 249 215;p16 S3 383 349;p16 S4 311 287;" +
        "p13 L 199 -1;p13 M 219 -1;p13 H 243 219;p13 2L 263 237;p13 2M 297 261;p13 2H 143 119;p13 6M 213 187;p13 S 605 605;p13 S3 81 81;p13 S4 471 447;" +
        "monster_npc L 145 111;monster_npc M 165 131;monster_npc H 195 161;monster_npc 2L 119 83;monster_npc 2M 151 115;monster_npc 2H 183 147;monster_npc 6M 165 131;";
}
