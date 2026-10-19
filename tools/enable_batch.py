#!/usr/bin/env python3
"""Enable an explicitly audited batch in National Pokédex order."""
import json,sys
from pathlib import Path
from PIL import Image
from audit_candidates import audit,ROOT,R
rows={r['slug']:r for r in audit()}
slugs=sys.argv[1:]
if not slugs or len(set(slugs))!=len(slugs):raise SystemExit('List the complete intended species batch exactly once')
old=json.loads((R/'data/minemon_gen1/runtime_species.json').read_text())
by_slug={d['slug']:d for d in old}
for slug in slugs:
 row=rows.get(slug)
 if not row:raise SystemExit('Not a generation 1 species: '+slug)
 if not row['accepted']:raise SystemExit(slug+': '+row['reason'])
 d=row['definition'];im=Image.open(R/d['texture']).convert('RGBA');colors=im.getcolors(im.width*im.height);colors=sorted(((n,c) for n,c in (colors or []) if c[3]>128),reverse=True)
 chosen=[c for n,c in colors if max(c[:3])-min(c[:3])>20][:2] or ([colors[0][1]]*2 if colors else [(128,128,128,255)]*2)
 d['eggBase']=int.from_bytes(bytes(chosen[0][:3]),'big');d['eggSpots']=int.from_bytes(bytes(chosen[-1][:3]),'big')
 clips=d['clips'];prefix='animation.'+slug+'.minemon_'
 if not any(k in clips for k in ('ground_run','run')):clips['ground_run']=prefix+'run'
 if 'sit' not in clips:clips['sit']=prefix+'sit'
 if 'sleep' not in clips:clips['sleep']=prefix+'sleep'
 if not any(k in clips for k in ('physical_attack','physical','attack')):clips['physical_attack']=prefix+'physical_attack';d['attackTicks']=12
 if slug=='bulbasaur':d.update(scale=1,width=.8,height=.8,health=20,attack=3,speed=.25,eggBase=0x83B75B,eggSpots=0xE4E6C4)
 by_slug[slug]=d
 egg=R/f'assets/minemon/models/item/{slug}_spawn_egg.json';egg.parent.mkdir(parents=True,exist_ok=True);egg.write_text(json.dumps({'parent':'minecraft:item/template_spawn_egg'},indent=2)+'\n')
# Keep all registered species in official Pokédex order.
ordered=sorted(by_slug.values(),key=lambda d:d['nationalPokedexNumber'])
(R/'data/minemon_gen1/runtime_species.json').write_text(json.dumps(ordered,ensure_ascii=False,indent=2)+'\n')
for lang in ('en_us','pt_br'):
 p=R/f'assets/minemon/lang/{lang}.json';data=json.loads(p.read_text())
 for d in ordered:
  data['entity.minemon_gen1.'+d['slug']]=d['name'];data['item.minemon_gen1.'+d['slug']+'_spawn_egg']=('Ovo de invocação de ' if lang=='pt_br' else 'Spawn Egg: ')+d['name']
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
# Structured National Pokédex index remains complete even while some entries are skipped.
catalog=json.loads((R/'data/minemon/species_catalog.json').read_text())
entries=[]
for entry in sorted((s for s in catalog if 1<=s['nationalPokedexNumber']<=151),key=lambda s:s['nationalPokedexNumber']):
 entries.append({'nationalPokedexNumber':entry['nationalPokedexNumber'],'slug':entry['slug'],'name':entry['name'],'registered':entry['slug'] in by_slug})
(R/'data/minemon_gen1/pokedex.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2)+'\n')
props=ROOT/'gradle.properties';lines=props.read_text().splitlines();enabled=','.join(d['slug'] for d in ordered);props.write_text('\n'.join('runtime_species='+enabled if l.startswith('runtime_species=') else l for l in lines)+'\n')
print('Registered in order:',','.join(d['slug'] for d in ordered))
