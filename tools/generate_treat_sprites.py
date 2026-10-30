#!/usr/bin/env python3
"""Deterministic inventory pixel art: one bone mask and shared shading for all types."""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/minemon/textures/item'
COLORS={'normal':(184,191,199),'fire':(238,89,37),'aquatic':(46,111,220),
'electric':(244,200,34),'grass':(67,171,66),'frozen':(114,215,222),
'fighting':(169,64,48),'poison':(168,64,185),'ground':(167,107,47),
'flying':(142,181,241),'psychic':(235,64,137),'bug':(135,156,36),
'rock':(139,127,94),'ghost':(89,64,139),'dragon':(94,77,219),
'dark':(54,58,68),'steel':(137,159,169),'fairy':(242,162,199)}
# Unmodified vanilla 1.19.2 pixels, mirrored from InventivetalentDev/minecraft-assets.
# Runtime client validation compares the reference against minecraft:textures/item/bone.png.
reference=Image.open(Path(__file__).parent/'reference/minecraft-1.19.2-bone.png').convert('RGBA')
for kind,base in COLORS.items():
 image=reference.copy()
 for y in range(16):
  for x in range(16):
   r,g,b,a=reference.getpixel((x,y))
   if not a:continue
   # The vanilla luminance and alpha stay structurally identical; only RGB changes.
   light=(r+g+b)/(3*255)
   image.putpixel((x,y),tuple(min(255,round(v*light)) for v in base)+(a,))
 image.save(ROOT/f'{kind}_treat.png')
print('Recolored 18 treats from the exact vanilla 1.19.2 bone sprite.')
