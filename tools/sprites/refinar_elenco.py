#!/usr/bin/env python3
"""Auditoria de sprites em lote, para não inspecionar todo quadro de todo lutador.

Reusa as ferramentas consolidadas (auditoria, ancorar, reescalar e tamanho).
O modo padrão APENAS mede e relata: não retoca PNG, não modifica
character.json, não desfaz uma arte aprovada e não faz merge.

Exemplos:
  python3 tools/sprites/refinar_elenco.py --todos --saida android/app/build/sprite-review/elenco.json
  python3 tools/sprites/refinar_elenco.py p01 p02 p03 --saida /tmp/qa.json

Como selecionar o trabalho:
- erro + quadro de arte quebrado: refazer arte original (nunca consertar via escala);
- ancoragem/escorregamento: sugerir ancorar.py, mas não alterar clipes de
  caminhada, DASH, pulo ou cena que desloca corpo de propósito;
- tamanho: medir cabeça/torso com reescalar.py, não usar bbox de mãos/pernas;
- ritmo: ajustar distribuição de frames, nunca remover frames silenciosamente;
- nenhuma confiança suficiente: declarar revisão visual, não inventar fator.

O relatório diferencia auditado / pendente / sem fonte; não marca personagem
"incompleto" como aprovado por falta de sprites.
"""
import argparse
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CHARACTERS = ROOT / "characters"
MOVING = {
    "WALK_FORWARD", "WALK_BACK", "DASH", "BACKDASH",
    "JUMP", "FALL", "LAND", "KNOCKDOWN", "GROUNDED",
    "GETUP", "THROW_TOSS", "INTRO", "DEFEAT",
}
ISSUE_PRIORITY = {"quebrado": 0, "tamanho": 1, "deslize": 2,
                  "pulo": 2, "ritmo": 3, "ciclo": 4}


def route(issue):
    """Deterministic, conservative review route; does not change assets."""
    kind, state = issue["kind"], issue["state"]
    if kind == "quebrado":
        return "REFAZER_ARTE"
    if kind == "tamanho":
        return "REESCALAR_POR_CABECA"  # reescalar.py --ver; no destructive scaling
    if kind in ("deslize", "pulo"):
        if state in MOVING:
            return "REVISAR_MOVIMENTO_INTENCIONAL"
        return "ANCORAR_PE"
    if kind == "ritmo":
        return "REVER_RITMO_SEM_PULAR_QUADROS"
    if kind == "ciclo":
        return "REVER_EMENDA_DO_LOOP"
    if kind == "receita":
        return "REVER_REFERENCIA_DA_ANIMACAO"
    return "REVISAR_VISUAL"


def summarize(issues):
    relevant = [dict(entry, acao=route(entry)) for entry in issues]
    relevant.sort(key=lambda x: (
        {"erro": 0, "aviso": 1, "info": 2}.get(x["level"], 3),
        ISSUE_PRIORITY.get(x["kind"], 9), x["state"],
    ))
    counts = Counter(x["level"] for x in issues)
    by_action = Counter(x["acao"] for x in relevant if x["level"] != "info")
    return {
        "erros": counts["erro"], "avisos": counts["aviso"],
        "informacoes": counts["info"], "acoes": dict(by_action),
        "pendencias": relevant,
        "aprovado_automaticamente": not (counts["erro"] or counts["aviso"]),
    }


def audit_batch(names, auditor=None):
    if auditor is None:
        import auditoria
        auditor = auditoria.audit
    output = {}
    for name in names:
        path = CHARACTERS / name / "character.json"
        if not path.exists():
            output[name] = {"status": "SEM_MANIFESTO", "erro": str(path)}
            continue
        try:
            pack = json.loads(path.read_text(encoding="utf-8"))
            issues = auditor(name)
            summary = summarize(issues)
            summary["status"] = "AUDITADO"
            summary["quantidade_estados"] = len(pack.get("animations", {}))
            summary["faltantes_no_manifesto"] = [] if summary["quantidade_estados"] else ["TODOS"]
            output[name] = summary
        except (OSError, ValueError, KeyError, IndexError) as exc:
            output[name] = {"status": "PENDENTE_FONTE_OU_LAYOUT", "erro": str(exc)}
        except Exception as exc:
            # Never call an audit successful when an algorithm fails unexpectedly.
            output[name] = {"status": "FALHA_AUDITORIA", "erro": str(exc)}
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("personagens", nargs="*")
    parser.add_argument("--todos", action="store_true")
    parser.add_argument("--saida", type=Path, default=ROOT/"android/app/build/sprite-review/elenco.json")
    args = parser.parse_args()
    names = sorted({p.parent.name for p in CHARACTERS.glob("*/character.json")}) if args.todos else args.personagens
    if not names:
        parser.error("passe --todos ou informe um ou mais IDs dos personagens")
    results = audit_batch(names)
    counts = Counter(item["status"] for item in results.values())
    report = {
        "versao_schema": 1,
        "regra": "medir pelo corpo, nunca pela bbox inteira; revisar só exceções",
        "personagens": results,
        "resumo": dict(counts),
    }
    args.saida.parent.mkdir(parents=True, exist_ok=True)
    args.saida.write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(f"Auditoria automatizada: {len(names)} personagens; " +
          ", ".join(f"{k}={v}" for k,v in sorted(counts.items())))
    for key, result in results.items():
        print(f"  {key}: {result['status']}, " +
              (f"{result.get('erros',0)} erros, {result.get('avisos',0)} avisos"
               if result["status"]=="AUDITADO" else result.get("erro","")))
    print("Relatorio: "+str(args.saida))


if __name__=="__main__":
    main()
