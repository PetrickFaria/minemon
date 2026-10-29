#!/usr/bin/env python3
"""Verify the Pokébola registry entry, client assets, localization, and built JAR."""
from __future__ import annotations

import json
import struct
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/minemon"
MODEL = "assets/minemon/models/item/pokeball.json"
TEXTURE = "assets/minemon/textures/item/pokeball.png"
MASTER_MODEL = "assets/minemon/models/item/master_ball.json"
MASTER_TEXTURE = "assets/minemon/textures/item/master_ball.png"

model = json.loads((ASSETS / "models/item/pokeball.json").read_text(encoding="utf-8"))
assert model.get("parent") == "minecraft:item/generated", "Pokébola must use its own generated item model"
assert model.get("textures", {}).get("layer0") == "minemon:item/pokeball", "Pokébola model points outside its namespace"
def validate_texture(path: Path, label: str) -> bytes:
    raw = path.read_bytes()
    header = raw[:26]
    assert header[:8] == b"\x89PNG\r\n\x1a\n" and header[12:16] == b"IHDR", f"{label} texture is not a PNG"
    width, height = struct.unpack(">II", header[16:24])
    assert (width, height) == (16, 16) and header[25] == 6, f"{label} texture must preserve the supplied 16x16 RGBA sprite"
    return raw

pokeball_png = validate_texture(ASSETS / "textures/item/pokeball.png", "Pokébola")
master_model = json.loads((ASSETS / "models/item/master_ball.json").read_text(encoding="utf-8"))
assert master_model.get("parent") == "minecraft:item/generated", "Master Ball must use its own generated item model"
assert master_model.get("textures", {}).get("layer0") == "minemon:item/master_ball", "Master Ball model points outside its namespace"
master_ball_png = validate_texture(ASSETS / "textures/item/master_ball.png", "Master Ball")
for locale in ("pt_br", "en_us"):
    translations = json.loads((ASSETS / "lang" / f"{locale}.json").read_text(encoding="utf-8"))
    assert translations.get("item.minemon.pokeball"), f"Missing {locale} Pokébola name"
    assert translations.get("item.minemon.master_ball"), f"Missing {locale} Master Ball name"
source = (ROOT / "src/main/java/br/com/petrickfaria/minemon/registry/ModItems.java").read_text(encoding="utf-8")
assert 'TREAT_ITEMS.register("pokeball"' in source and "POKEBALL.get().getDefaultInstance()" in source
assert 'TREAT_ITEMS.register("master_ball"' in source and "MASTER_BALL.get().getDefaultInstance()" in source
assert "new PokeballItem" in source and 'ForgeRegistries.ITEMS,"minemon"' in source

if len(sys.argv) > 1:
    jar = Path(sys.argv[1])
    with zipfile.ZipFile(jar) as archive:
        names = set(archive.namelist())
        for path in (MODEL, TEXTURE, MASTER_MODEL, MASTER_TEXTURE, "assets/minemon/lang/pt_br.json", "assets/minemon/lang/en_us.json"):
            assert path in names, f"Poké Ball JAR is missing {path}"
        assert archive.read(TEXTURE) == pokeball_png, "JAR Pokébola sprite differs from the exact source PNG"
        assert archive.read(MASTER_TEXTURE) == master_ball_png, "JAR Master Ball sprite differs from the exact source PNG"
        for path in (
            "br/com/petrickfaria/minemon/registry/ModItems.class",
            "br/com/petrickfaria/minemon/item/PokeballItem.class",
            "br/com/petrickfaria/minemon/item/PokemonBallLedger.class",
            "br/com/petrickfaria/minemon/pokec/PokemonSoulConversion.class",
            "br/com/petrickfaria/minemon/pokec/PokecTransactions.class",
            "br/com/petrickfaria/minemon/menu/PokecScreen.class",
        ):
            assert path in names, f"Pokébola compiled class missing from JAR: {path}"
        registry = archive.read("br/com/petrickfaria/minemon/registry/ModItems.class")
        assert b"pokeball" in registry, "Compiled item registry does not contain minemon:pokeball"
        translations = json.loads(archive.read("assets/minemon/lang/pt_br.json"))
        assert translations.get("item.minemon.pokeball") == "Pokébola", "JAR has no Portuguese Pokébola name"

print("PASS: Pokébola and Master Ball exact 16x16 RGBA sprites, models, registration, translations" + (" and JAR contents verified" if len(sys.argv) > 1 else " verified"))
