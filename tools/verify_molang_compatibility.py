#!/usr/bin/env python3
"""Fast preflight for Cobblemon-only query.r functions before Forge starts."""
from __future__ import annotations

import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
QUERY_R = re.compile(r"\b(?:q|query)\.r\.([A-Za-z_][A-Za-z0-9_]*)\s*\(")


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from strings(child)
    elif isinstance(value, list):
        for child in value:
            yield from strings(child)


def main() -> int:
    manifest = ROOT / "data/minemon_gen1/runtime_species.json"
    definitions = json.loads(manifest.read_text(encoding="utf-8"))
    unsupported = defaultdict(Counter)
    selected_unsupported = []
    checked = 0

    for definition in definitions:
        relative = definition["animation"].removeprefix("assets/minemon/")
        animation_path = ROOT / "assets/minemon" / relative
        if not animation_path.is_file():
            if definition.get("generated_static_animation"):
                continue
            raise SystemExit(f"[MOLANG PREFLIGHT] Missing animation resource: {animation_path}")

        animations = json.loads(animation_path.read_text(encoding="utf-8")).get("animations", {})
        selected = {
            name for name in (
                definition.get("idle"),
                definition.get("walk"),
                definition.get("clips", {}).get("ground_run"),
            ) if name
        }
        for clip_name, clip in animations.items():
            if not isinstance(clip, dict):
                continue
            functions = Counter(
                function
                for expression in strings(clip.get("bones", {}))
                for function in QUERY_R.findall(expression)
            )
            if not functions:
                continue
            checked += sum(functions.values())
            if clip_name in selected:
                selected_unsupported.append((definition, clip_name, functions))
            else:
                for function, count in functions.items():
                    unsupported[(definition["slug"], clip_name)][function] += count

    if selected_unsupported:
        print("[MOLANG PREFLIGHT] FAIL: unsupported Cobblemon query.r functions occur in active idle/walk/run clips.")
        for definition, clip, functions in selected_unsupported:
            print(f"  #{definition['nationalPokedexNumber']:03d} {definition['slug']} {clip}: {dict(functions)}")
        return 1

    print(f"[MOLANG PREFLIGHT] PASS: checked {checked} query.r references; none occur in selected idle/walk/run clips.")
    if unsupported:
        print("[MOLANG PREFLIGHT] Existing GeckoLib 3.1.40 parser errors remain in retained, non-selected clips:")
        for (slug, clip), functions in sorted(unsupported.items()):
            dex = next(d["nationalPokedexNumber"] for d in definitions if d["slug"] == slug)
            print(f"  #{dex:03d} {slug} {clip}: {', '.join(sorted(functions))} ({sum(functions.values())} refs)")
        print("[MOLANG PREFLIGHT] These references are reported, not suppressed or rewritten. Review the runtime logs; no selected locomotion clip uses them.")
    else:
        print("[MOLANG PREFLIGHT] No unsupported query.r references found.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
