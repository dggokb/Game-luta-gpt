package com.gamelutagpt;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Super da Yin-lu, Espelho do Lago (17/11): Super de contra-golpe. Ela espera na postura da garça;
 * se o rival bate na janela, ela desvia e responde com as palmas e o empurrão das duas palmas (seis
 * acertos, derruba). Sem nada para ler, ela sai da postura sem soltar as palmas (a barra foi gasta).
 */
public class SuperEspelhoDoLagoTest {
    private static CombatEngineTest.Sim perto() {
        CombatEngineTest.Sim s = new CombatEngineTest.Sim(GeneratedCharacters.get("p02"), GeneratedCharacters.get("player_base"), 500f, 640f);
        s.f(0).state.superMeter = 3 * CombatConfig.METER_PER_BAR;
        return s;
    }

    private static void soltaSuper(CombatEngineTest.Sim s) {
        s.in[0].superAttack = true;
        s.step();
        s.in[0].superAttack = false;
        s.until(() -> s.f(0).attacking() && s.f(0).attack.kind == AttackDefinition.Kind.SUPER, 30);
    }

    @Test public void oGolpeDoRivalViraAsPalmas() {
        AttackDefinition sup = GeneratedCharacters.get("p02").fighter.superAttack.attack;
        assertTrue("lê o golpe", sup.mechanics.counter != AttackDefinition.Reads.NONE);
        assertTrue("só bate no que leu", sup.mechanics.parry);
        CombatEngineTest.Sim s = perto();
        soltaSuper(s);
        int vida = s.f(1).state.life;
        // Espera ela entrar na postura e o rival bate.
        s.until(() -> s.f(0).attackFrame >= 10, 60);
        s.in[1].light = true;
        int acertos = 0;
        boolean contra = false;
        for (int t = 0; t < 240; t++) {
            s.step();
            if (s.engine.cues().contains("COUNTER")) contra = true;
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0 && !e.blocked) acertos++;
            if (!s.f(0).attacking() && t > 20) break;
        }
        assertTrue("leu o golpe", contra);
        assertEquals("o desvio, quatro palmas e as duas palmas", 6, acertos);
        assertTrue("tirou vida", s.f(1).state.life < vida);
        assertTrue("o rival caiu: " + s.f(1).status, s.f(1).status == CombatFighter.Status.KNOCKDOWN || !s.f(1).grounded);
    }

    @Test public void semNadaParaLerElaSaiDaPostura() {
        CombatEngineTest.Sim s = perto();
        soltaSuper(s);
        AttackDefinition sup = s.f(0).attack;
        int t = 0, acertos = 0;
        while (s.f(0).attacking() && t++ < 300) {
            s.step();
            for (CombatEngine.HitEvent e : s.engine.events()) if (e.attacker == 0) acertos++;
        }
        assertEquals("no vazio não sai palma", 0, acertos);
        assertTrue("sai antes do fim inteiro do golpe: " + t, t < sup.totalFrames - 20);
        assertTrue("gastou a barra", s.f(0).state.superMeter < 3 * CombatConfig.METER_PER_BAR);
    }
}
