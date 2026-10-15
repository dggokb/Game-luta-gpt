"""Command line entry point; no game dependencies."""
import argparse
import json
from pathlib import Path
from sprite_pipeline.errors import CalibrationError
from sprite_pipeline.pipeline import process
from sprite_pipeline.imaging import similarity


def main():
    parser = argparse.ArgumentParser(description="Padronizador universal de sprites, local e independente")
    parser.add_argument("project", type=Path)
    parser.add_argument("--output", type=Path, default=Path("output"))
    args = parser.parse_args()
    try:
        result = process(args.project, args.output)
    except (CalibrationError, ValueError, KeyError, OSError) as error:
        parser.exit(2, f"Erro: {error}\n")
    print(json.dumps({"status": result["status"], "report": str(args.output / "index.html"),
                      "clips": [{k: c.get(k) for k in ("name", "status", "frame_count", "scale_per_video")} for c in result["clips"]]}, indent=2))
    if any(c["status"] == "rejected" for c in result["clips"]):
        raise SystemExit(2)


if __name__ == "__main__":
    main()
