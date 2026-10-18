#!/usr/bin/env python3
import json,re,struct
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources'
CAT={s['slug']:s for s in json.loads((R/'data/minemon/species_catalog.json').read_text())}
CANDIDATES='bulbasaur pikachu charmander squirtle jolteon charizard blastoise ivysaur gastly haunter gengar shellder cloyster psyduck hitmonlee mew mewtwo wartortle arbok nidoking growlithe arcanine golem onix scyther electabuzz zapdos'.split()
FLYING={'haunter','gengar','mew','scyther','zapdos'}
EXCLUSIONS={'gastly':'Incompatible arrays of particle events in the original base and shiny animations.','mewtwo':'Multiple animation channels reference bones absent from the available model.','shellder':'Only an idle animation; aquatic navigation and locomotion not validated.','cloyster':'Only an idle animation; aquatic navigation and locomotion not validated.','nidoking':'Available file has idle/blink/cry but no verified locomotion clip.','growlithe':'Available file has experimental test clips; no verified walking clip.','arcanine':'Available file has experimental test clips; no verified walking clip.','golem':'Available file has test/blink/cry; no verified locomotion clip.',**{s:'Available locomotion/flight clips contain only static poses; no animated body or wing motion.' for s in ('haunter','gengar','mew','scyther','zapdos')}}
def audit():
 results=[]
 for slug in CANDIDATES:
  s=CAT[slug];res=s['resources'];issues=[]
  canonical='assets/minemon/geo/'+slug+'/'+slug+'.geo.json'
  if slug in ('pikachu','scyther'):canonical='assets/minemon/geo/'+slug+'/'+slug+'_male.geo.json'
  model=canonical if canonical in res['models'] else res['models'][0]
  animation=next((p for p in res['animations'] if p.endswith('/'+slug+'.animation.json')),res['animations'][0])
  texture=next((p for p in res['textures'] if p.endswith('/'+slug+'.png')),res['textures'][0])
  gd=json.loads((R/model).read_text());ad=json.loads((R/animation).read_text())
  bones={b['name'] for g in gd['minecraft:geometry'] for b in g['bones']}
  parents={b.get('parent') for g in gd['minecraft:geometry'] for b in g['bones'] if b.get('parent')}
  if parents-bones:issues.append('Missing geometry parents: '+str(sorted(parents-bones)))
  excluded_clips=[]
  for n,a in ad['animations'].items():
   missing=set(a.get('bones',{}))-bones
   if missing:
    if slug=='jolteon' and n=='animation.jolteon.cry':excluded_clips.append(n)
    else:issues.append(n+' missing bones '+','.join(sorted(missing)))
   for field in ('particle_effects','sound_effects'):
    if not isinstance(a.get(field,{}),dict) or any(not isinstance(v,dict) for v in a.get(field,{}).values()):issues.append(n+' incompatible '+field)
  png=(R/texture).read_bytes();dims=struct.unpack('>II',png[16:24]);desc=gd['minecraft:geometry'][0]['description']
  if dims!=(desc['texture_width'],desc['texture_height']):issues.append('UV texture dimensions mismatch')
  clips={n.rsplit('.',1)[-1]:n for n in ad['animations'] if n not in excluded_clips}
  mode='flying' if slug in FLYING else 'ground'
  idle=next((clips[n] for n in (('air_idle','ground_idle','idle') if mode=='flying' else ('ground_idle','idle')) if n in clips),None)
  walk=next((clips[n] for n in (('air_fly','ground_walk','air_idle','ground_idle') if mode=='flying' else ('ground_walk','walk')) if n in clips),None)
  if not idle or not walk:issues.append('No validated idle/locomotion mapping')
  elif not re.search(r'(?:q|query)\.anim_time',json.dumps(ad['animations'][walk])) and not any(isinstance(channel,dict) and len(channel)>1 for bone in ad['animations'][walk].get('bones',{}).values() for channel in bone.values()):issues.append('Locomotion clip is a static pose')
  reason=EXCLUSIONS.get(slug)
  if issues and not reason:reason='; '.join(issues)
  d=dict(slug=slug,name=s['name'],model=model,texture=texture,animation=animation,mode=mode,scale=s.get('baseScale',1),width=s['hitbox']['width'],height=s['hitbox']['height'],health=max(20,s['baseStats']['hp']/2),attack=max(3,s['baseStats']['attack']/12),speed=round(min(.35,.20+s['baseStats']['speed']/1000),3),clips=clips,idle=idle,walk=walk,excluded_clips=excluded_clips,sounds=res['sounds'],licenses=res['licenses'])
  results.append(dict(slug=slug,accepted=not bool(reason),reason=reason,issues=issues,definition=d))
 return results
if __name__=='__main__':
 results=audit();out=ROOT/'tools/candidate_audit.json';out.write_text(json.dumps(results,indent=2)+'\n')
 for r in results:print(r['slug'],'READY' if r['accepted'] else 'EXCLUDE',r['reason'] or ('omit optional clips '+str(r['definition']['excluded_clips']) if r['definition']['excluded_clips'] else ''))
