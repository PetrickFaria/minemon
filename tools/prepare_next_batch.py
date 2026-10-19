#!/usr/bin/env python3
"""Audit and prepare one small, Pokédex-ordered batch of compatible Kanto species."""
import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

from audit_candidates import ROOT, audit

parser = argparse.ArgumentParser()
parser.add_argument("--size", type=int, default=3)
args = parser.parse_args()
if args.size < 1 or args.size > 5:
    raise SystemExit("Batch size must be between 1 and 5")

manifest_path = ROOT / "src/main/resources/data/minemon_gen1/runtime_species.json"
current = json.loads(manifest_path.read_text(encoding="utf-8"))
registered = {entry["slug"] for entry in current}
rows = audit()
audit_path = ROOT / "tools/candidate_audit.json"
audit_path.write_text(json.dumps(rows, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
eligible = [row["slug"] for row in rows if row["accepted"] and row["slug"] not in registered]
selected = eligible[:args.size]
print(f"Kanto audit: {len(rows)} candidates, {len(eligible)} statically compatible and not yet registered")
print("Selected next batch: " + (", ".join(selected) if selected else "(none)"))
if selected:
    subprocess.run(
        [sys.executable, str(ROOT / "tools/enable_batch.py"), *selected],
        cwd=ROOT,
        check=True,
    )
out = ROOT / "build/kanto-batch.txt"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text(",".join(selected) + "\n", encoding="utf-8")
with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
    output.write(f"changed={'true' if selected else 'false'}\n")
    output.write(f"slugs={','.join(selected)}\n")
