#!/usr/bin/env python3
"""Audit the first 151 National Pokédex entries using the extracted source assets."""
import json, re, struct, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
CAT={s['slug']:s for s in json.loads((R/'data/minemon/species_catalog.json').read_text())}
CANDIDATES=[s['slug'] for s in sorted(CAT.values(),key=lambda x:x['nationalPokedexNumber']) if 1<=s['nationalPokedexNumber']<=151]
# These were already confirmed as requiring nontrivial flight behavior in the prior build.
FLYING={'haunter','gengar','mew','scyther','zapdos'}

def audit():
 results=[]
 for slug in CANDIDATES:
  s=CAT[slug];res=s['resources'];issues=[]
  models=res['models'];animations=res['animations'];textures=res['textures']
  model=next((p for p in models if p.endswith('/'+slug+'.geo.json')),models[0] if models else None)
  if slug in ('pikachu','scyther'):
   model=next((p for p in models if p.endswith('/'+slug+'_male.geo.json')),model)
  animation=next((p for p in animations if p.endswith('/'+slug+'.animation.json')),animations[0] if animations else None)
  texture=next((p for p in textures if p.endswith('/'+slug+'.png') and not p.endswith('_shiny.png')),textures[0] if textures else None)
  if not model or not (R/model).is_file():issues.append('Missing model file')
  if not animation or not (R/animation).is_file():issues.append('Missing animation file')
  if not texture or not (R/texture).is_file():issues.append('Missing texture file')
  if issues:
   results.append(dict(slug=slug,accepted=False,reason='; '.join(issues),issues=issues,definition=None));continue
  try:
   gd=json.loads((R/model).read_text());ad=json.loads((R/animation).read_text()).get('animations',{})
   bones={b['name'] for g in gd['minecraft:geometry'] for b in g['bones']}
   parents={b.get('parent') for g in gd['minecraft:geometry'] for b in g['bones'] if b.get('parent')}
   if parents-bones:issues.append('Missing geometry parents: '+str(sorted(parents-bones)))
   dims=struct.unpack('>II',(R/texture).read_bytes()[16:24]);desc=gd['minecraft:geometry'][0]['description']
   if dims!=(desc['texture_width'],desc['texture_height']):issues.append('Texture and model UV dimensions differ')
   clips={n.rsplit('.',1)[-1]:n for n in ad}
   mode='flying' if slug in FLYING or ('air_idle' in clips and 'air_fly' in clips) else 'ground'
   idle=next((clips[n] for n in (('air_idle','ground_idle','idle') if mode=='flying' else ('ground_idle','idle','air_idle')) if n in clips),None)
   walk=next((clips[n] for n in (('air_fly','ground_walk','walk','ground_run','run') if mode=='flying' else ('ground_walk','walk','ground_run','run')) if n in clips),None)
   if not idle:issues.append('No usable idle clip')
   if not walk:issues.append('No usable locomotion clip')
   excluded=[];stripped_bones={}
   # Keep useful source clips when only some channels target bones absent from
   # this model. The original files stay untouched; runtime preparation strips
   # precisely these channels and the client smoke test validates the result.
   for name,a in ad.items():
    bad_bones=set(a.get('bones',{}))-bones
    if bad_bones:
     if name in (idle,walk):
      remaining={bone:channels for bone,channels in a.get('bones',{}).items() if bone in bones}
      if not remaining:issues.append(name+' has no channels for bones in the model')
      else:stripped_bones[name]=sorted(bad_bones)
     else:excluded.append(name)
   if walk:
    clip=ad[walk]
    valid_bones={bone:channels for bone,channels in clip.get('bones',{}).items() if bone in bones}
    payload=json.dumps({'bones':valid_bones})
    animated=bool(re.search(r'(?:q|query)\.anim_time',payload)) or any(isinstance(ch,dict) and len(ch)>1 for bone in valid_bones.values() for ch in bone.values())
    if not animated:issues.append('Locomotion clip is a static pose')
   attack=next((clips[n] for n in ('physical_attack','physical','attack') if n in clips),None)
   attack_ticks=math.ceil(ad[attack].get('animation_length',0)*20) if attack else 0
   if attack and attack_ticks<=0:issues.append('Physical attack has no finite duration')
  except Exception as exc:
   issues.append('Resource parse failed: '+str(exc))
   idle=walk=None;mode='ground';excluded=[];attack_ticks=0;clips={}
  d=dict(nationalPokedexNumber=s['nationalPokedexNumber'],slug=slug,name=s['name'],model=model,texture=texture,animation=animation,mode=mode,scale=s.get('baseScale',1),width=s['hitbox']['width'],height=s['hitbox']['height'],health=max(20,s['baseStats']['hp']/2),attack=max(3,s['baseStats']['attack']/12),speed=round(min(.35,.20+s['baseStats']['speed']/1000),3),clips=clips,idle=idle,walk=walk,attackTicks=attack_ticks,excluded_clips=excluded,stripped_bones=stripped_bones,sounds=res['sounds'],licenses=res['licenses'])
  accepted=not bool(issues)
  results.append(dict(slug=slug,accepted=accepted,reason='; '.join(issues) if issues else None,issues=issues,definition=d))
 return results

if __name__=='__main__':
 rows=audit();(ROOT/'tools/candidate_audit.json').write_text(json.dumps(rows,indent=2,ensure_ascii=False)+'\n')
 for row in rows:print(f"{row['definition']['nationalPokedexNumber'] if row['definition'] else CAT[row['slug']]['nationalPokedexNumber']:03} {row['slug']}: {'READY' if row['accepted'] else 'SKIP'} {row['reason'] or ''}")
