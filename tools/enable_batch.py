import json,sys
from pathlib import Path
from PIL import Image
from audit_candidates import audit,ROOT,R
rows={r['slug']:r for r in audit()};slugs=sys.argv[1:];defs=[]
if not slugs or slugs[0]!='bulbasaur' or len(set(slugs))!=len(slugs):raise SystemExit('Keep Bulbasaur first and list each species exactly once')
for slug in slugs:
 row=rows[slug]
 if not row['accepted']:raise SystemExit(slug+': '+row['reason'])
 d=row['definition'];im=Image.open(R/d['texture']).convert('RGBA');colors=im.getcolors(im.width*im.height);colors=sorted(((n,c) for n,c in colors if c[3]>128),reverse=True)
 chosen=[c for n,c in colors if max(c[:3])-min(c[:3])>20][:2] or [colors[0][1]]*2
 d['eggBase']=int.from_bytes(bytes(chosen[0][:3]),'big');d['eggSpots']=int.from_bytes(bytes(chosen[-1][:3]),'big')
 # Give the shared renderer explicit six-state aliases. Missing clips are generated
 # in the processed runtime copy from this species' own geometry/idle/walk channels.
 clips=d['clips']; prefix='animation.'+slug+'.minemon_'
 if not any(k in clips for k in ('ground_run','run')): clips['ground_run']=prefix+'run'
 if 'sit' not in clips: clips['sit']=prefix+'sit'
 if 'sleep' not in clips: clips['sleep']=prefix+'sleep'
 if not any(k in clips for k in ('physical_attack','physical','attack')):
  clips['physical_attack']=prefix+'physical_attack';d['attackTicks']=12
 if slug=='bulbasaur':d.update(scale=1,width=.8,height=.8,health=20,attack=3,speed=.25,eggBase=0x83B75B,eggSpots=0xE4E6C4)
 defs.append(d)
 p=R/f'assets/minemon/models/item/{slug}_spawn_egg.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps({'parent':'minecraft:item/template_spawn_egg'},indent=2)+'\n')
for lang in ('en_us','pt_br'):
 p=R/f'assets/minemon/lang/{lang}.json';data=json.loads(p.read_text())
 for d in defs:data['entity.minemon.'+d['slug']]=d['name'];data['item.minemon.'+d['slug']+'_spawn_egg']=('Ovo de invocação de ' if lang=='pt_br' else 'Spawn Egg: ')+d['name']
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
(R/'data/minemon/runtime_species.json').write_text(json.dumps(defs,ensure_ascii=False,indent=2)+'\n')
p=ROOT/'gradle.properties';lines=p.read_text().splitlines();p.write_text('\n'.join('runtime_species='+','.join(slugs) if l.startswith('runtime_species=') else l for l in lines)+'\n')
print('Enabled:',','.join(slugs))
