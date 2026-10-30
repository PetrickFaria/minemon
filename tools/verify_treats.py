#!/usr/bin/env python3
"""Verify that the 18 Pokémon treat items have complete, unique client assets."""
from __future__ import annotations
import hashlib, json, struct, sys, zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/minemon'
NAMES={
    'aquatic_treat':'Petisco Aquoso','frozen_treat':'Petisco Congelado','dragon_treat':'Petisco Dracônico',
    'electric_treat':'Petisco Elétrico','ghost_treat':'Petisco Fantasmagórico','fairy_treat':'Petisco Feérico',
    'fire_treat':'Petisco Incandescente','bug_treat':'Petisco Insectóide','fighting_treat':'Petisco Marcial',
    'steel_treat':'Petisco Metálico','normal_treat':'Petisco Comum','rock_treat':'Petisco Petrificado',
    'psychic_treat':'Petisco Psicodélico','ground_treat':'Petisco Barroso','poison_treat':'Petisco Venenoso',
    'grass_treat':'Petisco Vegetal','dark_treat':'Petisco Sombrio',
    'flying_treat':'Petisco Voador'}
assert len(NAMES)==18
hashes=set()
for item,name in NAMES.items():
    model=json.loads((ASSETS/'models/item'/f'{item}.json').read_text())
    assert model['parent']=='item/generated' and model['textures']['layer0']==f'minemon:item/{item}', item
    image_path=ASSETS/'textures/item'/f'{item}.png'
    with image_path.open('rb') as image:
        header=image.read(26)
    assert header[:8]==b'\x89PNG\r\n\x1a\n' and header[12:16]==b'IHDR', item
    width,height=struct.unpack('>II',header[16:24])
    assert (width,height)==(16,16), (item,(width,height))
    assert header[25]==6, f'{item} must preserve its alpha channel'
    digest=hashlib.sha256(image_path.read_bytes()).hexdigest()
    assert digest not in hashes, f'duplicate texture for {item}'
    hashes.add(digest)
for lang in ('pt_br','en_us'):
    translations=json.loads((ASSETS/'lang'/f'{lang}.json').read_text())
    for item in NAMES:
        assert translations.get(f'item.minemon.{item}'), f'missing {lang} translation for {item}'
for item in NAMES:
    recipe=json.loads((ROOT/'src/main/resources/data/minemon/recipes'/f'{item}.json').read_text())
    element={'aquatic_treat':'water','frozen_treat':'ice','ghost_treat':'ghost'}.get(item,item.removesuffix('_treat'))
    assert recipe['type']=='minecraft:crafting_shaped'
    assert recipe['pattern']==[' S ','SBS',' S ']
    assert recipe['key']=={'S':{'item':f'minemon_gen1:{element}_soul'},'B':{'item':'minecraft:bone'}}
    assert recipe['result']=={'item':f'minemon:{item}','count':1}
if len(sys.argv)>1:
    jar=Path(sys.argv[1])
    with zipfile.ZipFile(jar) as archive:
        names=set(archive.namelist())
        for item in NAMES:
            for path in (f'assets/minemon/models/item/{item}.json',f'assets/minemon/textures/item/{item}.png'):
                assert path in names, f'missing in JAR: {path}'
            for lang in ('pt_br','en_us'):
                key=f'assets/minemon/lang/{lang}.json'
                assert key in names, f'missing in JAR: {key}'
                translations=json.loads(archive.read(key))
                assert translations.get(f'item.minemon.{item}'), f'missing {lang} JAR translation for {item}'
        for path in ('br/com/petrickfaria/minemon/item/PokemonTreatItem.class',
                     'br/com/petrickfaria/minemon/pokec/PokemonTreatTypes.class'):
            assert path in names, f'missing compiled treat class: {path}'
print('PASS: 18 unique treat models/textures and pt_br/en_us names verified'+(' in JAR' if len(sys.argv)>1 else ''))
