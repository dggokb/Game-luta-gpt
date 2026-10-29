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
        "player_base L 170 150;player_base M 174 152;player_base H 220 198;player_base 2L 184 170;player_base 2M 204 198;player_base 2H 136 122;player_base 6M 172 150;player_base S2 146 114;player_base S3 202 170;player_base S4 306 276;" +
        "p02 L 186 164;p02 M 230 222;p02 H 240 234;p02 2L 150 126;p02 2M 184 176;p02 2H 138 124;p02 6M 230 222;p02 S2 118 86;p02 S3 170 138;p02 S4 340 310;" +
        "p03 L 174 -1;p03 M 168 -1;p03 H 174 -1;p03 2L 198 184;p03 2M 278 270;p03 2H 136 122;p03 6M 168 -1;p03 S2 250 218;p03 S3 418 386;" +
        "p04 L 182 158;p04 M 224 216;p04 H 224 218;p04 2L 190 176;p04 2M 252 246;p04 2H 144 130;p04 6M 224 216;p04 S2 250 218;p04 S3 154 114;p04 S4 324 292;" +
        "p05 L 170 148;p05 M 242 236;p05 H 170 156;p05 2L 184 170;p05 2M 184 176;p05 2H 150 136;p05 6M 242 236;p05 S3 604 604;p05 S4 440 408;" +
        "p06 L 138 116;p06 M 208 186;p06 H 182 160;p06 2L 188 166;p06 2M 236 230;p06 2H 142 128;p06 6M 208 186;p06 S2 138 106;p06 S3 412 380;p06 S4 382 350;" +
        "p07 L 162 -1;p07 M 228 -1;p07 H 192 -1;p07 2L 170 146;p07 2M 158 134;p07 2H 230 208;p07 6M 228 -1;p07 S 226 196;p07 S2 280 238;p07 S3 308 276;p07 S4 372 340;" +
        "p08 L 162 156;p08 M 250 242;p08 H 398 376;p08 2L 174 168;p08 2M 164 158;p08 2H 164 150;p08 6M 250 242;p08 S2 410 378;p08 S3 180 148;p08 S4 354 322;" +
        "p09 L 192 -1;p09 M 240 218;p09 H 160 138;p09 2L 244 220;p09 2M 266 260;p09 2H 146 132;p09 6M 240 218;p09 S2 108 78;p09 S3 142 110;p09 S4 474 444;" +
        "p10 L 164 -1;p10 M 248 226;p10 H 162 148;p10 2L 174 160;p10 2M 216 210;p10 2H 190 176;p10 6M 248 226;p10 S2 168 138;p10 S3 252 222;p10 S4 374 342;" +
        "p11 L 200 -1;p11 M 184 -1;p11 H 200 178;p11 2L 220 206;p11 2M 228 220;p11 2H 144 122;p11 6M 184 -1;p11 S3 154 122;p11 S4 412 382;" +
        "p12 L 236 222;p12 M 228 220;p12 H 126 104;p12 2L 222 214;p12 2M 262 256;p12 2H 134 120;p12 6M 228 220;p12 S 264 232;p12 S2 232 202;p12 S3 216 186;p12 S4 312 280;" +
        "p14 L 206 192;p14 M 220 198;p14 H 288 264;p14 2L 260 252;p14 2M 296 290;p14 2H 138 124;p14 6M 206 182;p14 S2 444 402;p14 S3 438 398;p14 S4 394 354;" +
        "p15 L 162 -1;p15 M 160 -1;p15 H 264 250;p15 2L 172 150;p15 2M 296 288;p15 2H 170 156;p15 6M 278 272;p15 S2 -1 -1;p15 S3 298 268;p15 S4 322 292;" +
        "p16 L 166 146;p16 M 188 166;p16 H 234 220;p16 2L 160 146;p16 2M 258 252;p16 2H 190 176;p16 6M 256 248;p16 S3 390 348;p16 S4 316 286;" +
        "p13 L 204 -1;p13 M 226 -1;p13 H 250 236;p13 2L 270 248;p13 2M 302 296;p13 2H 150 128;p13 6M 220 206;p13 S 604 604;p13 S3 82 82;p13 S4 476 446;" +
        "monster_npc L 152 144;monster_npc M 172 164;monster_npc H 202 194;monster_npc 2L 124 118;monster_npc 2M 156 150;monster_npc 2H 188 182;monster_npc 6M 172 164;";
}
