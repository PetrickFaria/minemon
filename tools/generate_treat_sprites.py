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
# Horizontal dog-biscuit bone, two distinct rounded lobes at each broad end.
ROWS=['................','................','..###......###..','.#####....#####.','.#####....#####.','..############..','...##########...','...##########...','..############..','.#####....#####.','.#####....#####.','..###......###..','................','................','................','................']
MASK={(x,y) for y,row in enumerate(ROWS) for x,c in enumerate(row) if c=='#'}
for kind,base in COLORS.items():
 image=Image.new('RGBA',(16,16))
 for x,y in MASK:
  edge=any((x+dx,y+dy) not in MASK for dx,dy in ((1,0),(-1,0),(0,1),(0,-1)))
  factor=.48 if edge else 1.18 if (x,y-1) not in MASK or (x-1,y-1) not in MASK else .83 if (x+1,y+1) not in MASK else 1
  image.putpixel((x,y),tuple(min(255,round(v*factor)) for v in base)+(255,))
 image.save(ROOT/f'{kind}_treat.png')
print('Generated 18 palette variants with identical alpha masks.')
