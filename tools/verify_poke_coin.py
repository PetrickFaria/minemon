#!/usr/bin/env python3
"""Validate PokéCoin registration, RGBA art, localization, and production JAR entries."""
from __future__ import annotations

import json
import struct
import sys
import zipfile
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/minemon"
COIN = "poke_coin"


def decode_rgba(path: Path):
    data = path.read_bytes()
    assert data.startswith(b"\x89PNG\r\n\x1a\n"), "PokéCoin texture is not PNG"
    offset, compressed, dimensions = 8, bytearray(), None
    while offset < len(data):
        size = struct.unpack(">I", data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8]
        chunk = data[offset + 8:offset + 8 + size]
        offset += size + 12
        if kind == b"IHDR":
            width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", chunk)
            dimensions = (width, height, depth, color, interlace)
        elif kind == b"IDAT":
            compressed.extend(chunk)
        elif kind == b"IEND":
            break
    assert dimensions == (32, 32, 8, 6, 0), f"Expected a 32x32 RGBA PNG, got {dimensions}"
    width, height = dimensions[:2]
    stride, bpp = width * 4, 4
    raw = zlib.decompress(compressed)
    rows, previous, cursor = [], bytearray(stride), 0

    def paeth(a: int, b: int, c: int) -> int:
        estimate = a + b - c
        da, db, dc = abs(estimate - a), abs(estimate - b), abs(estimate - c)
        return a if da <= db and da <= dc else b if db <= dc else c

    for _ in range(height):
        filter_type = raw[cursor]
        scanline = bytearray(raw[cursor + 1:cursor + 1 + stride])
        cursor += stride + 1
        for i in range(stride):
            left = scanline[i - bpp] if i >= bpp else 0
            up = previous[i]
            upper_left = previous[i - bpp] if i >= bpp else 0
            if filter_type == 1:
                scanline[i] = (scanline[i] + left) & 255
            elif filter_type == 2:
                scanline[i] = (scanline[i] + up) & 255
            elif filter_type == 3:
                scanline[i] = (scanline[i] + ((left + up) // 2)) & 255
            elif filter_type == 4:
                scanline[i] = (scanline[i] + paeth(left, up, upper_left)) & 255
            else:
                assert filter_type == 0, f"Unsupported PNG filter {filter_type}"
        rows.append([tuple(scanline[i:i + 4]) for i in range(0, stride, 4)])
        previous = scanline
    assert cursor == len(raw), "Unexpected PNG pixel data length"
    return rows


def verify():
    model_path = ASSETS / "models/item/poke_coin.json"
    model = json.loads(model_path.read_text(encoding="utf-8"))
    assert model.get("parent") == "item/generated"
    assert model.get("textures", {}).get("layer0") == "minemon:item/poke_coin"

    pixels = decode_rgba(ASSETS / "textures/item/poke_coin.png")
    opaque = [pixel for row in pixels for pixel in row if pixel[3] > 200]
    red_upper = [p for y in range(7, 14) for p in pixels[y][6:26] if p[0] > 180 and p[1] < 90 and p[2] < 90 and p[3] > 200]
    white_lower = [p for y in range(19, 27) for p in pixels[y][6:26] if min(p[:3]) > 180 and max(p[:3]) - min(p[:3]) < 45 and p[3] > 200]
    dark_divider = [p for p in pixels[16][5:27] if max(p[:3]) < 70 and p[3] > 200]
    assert 400 < len(opaque) < 950, "Coin must have a crisp round silhouette and transparent corners"
    assert red_upper and white_lower and len(dark_divider) >= 4, "Poké Ball red/white halves and black divider must be visible"
    assert pixels[16][16][3] > 200 and min(pixels[16][16][:3]) > 180, "Poké Ball center button must be white"
    assert pixels[16][12][3] > 200 and max(pixels[16][12][:3]) < 70, "Poké Ball center button must have a black outline"
    assert not any(p[0] > 160 and p[1] > 120 and p[2] < 100 for row in pixels for p in row if p[3] > 200), "Gold/yellow pixels are not allowed"

    for locale in ("pt_br", "en_us"):
        translations = json.loads((ASSETS / "lang" / f"{locale}.json").read_text(encoding="utf-8"))
        assert translations.get("item.minemon.poke_coin") == "PokéCoin", f"Missing {locale} PokéCoin name"

    source = (ROOT / "src/main/java/br/com/petrickfaria/minemon/registry/ModItems.java").read_text(encoding="utf-8")
    assert 'TREAT_ITEMS.register("poke_coin"' in source
    assert "POKE_COIN.get().getDefaultInstance()" in source
    assert "stacksTo(64)" in source
    data_root = ROOT / "src/main/resources/data"
    assert not any("poke_coin" in path.name.lower() for path in data_root.rglob("*.json")), "PokéCoin must not get recipes or drops"

    if len(sys.argv) > 1:
        jar = Path(sys.argv[1])
        with zipfile.ZipFile(jar) as archive:
            names = set(archive.namelist())
            for resource in (
                "assets/minemon/models/item/poke_coin.json",
                "assets/minemon/textures/item/poke_coin.png",
                "assets/minemon/lang/pt_br.json",
            ):
                assert resource in names, f"PokéCoin JAR is missing {resource}"
            assert "br/com/petrickfaria/minemon/registry/ModItems.class" in names, "Compiled item registry is missing from JAR"
            class_bytes = archive.read("br/com/petrickfaria/minemon/registry/ModItems.class")
            assert b"poke_coin" in class_bytes, "Compiled ModItems does not register PokéCoin"
            jar_translations = json.loads(archive.read("assets/minemon/lang/pt_br.json"))
            assert jar_translations.get("item.minemon.poke_coin") == "PokéCoin", "JAR is missing the PokéCoin translation"
    print("PASS: minemon:poke_coin registration, 32x32 RGBA Poké Ball texture, translations, and JAR contents verified")


if __name__ == "__main__":
    verify()
