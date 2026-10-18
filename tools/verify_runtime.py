import json,struct,sys,zipfile,re,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources'
def validate(read,definitions,prepared=False):
 for d in definitions:
  g=json.loads(read(d['model']));a=json.loads(read(d['animation']))['animations'];bone_names={b['name'] for part in g['minecraft:geometry'] for b in part['bones']}
  for name in d.get('excluded_clips',[]):
   if prepared and name in a:raise ValueError('Excluded clip packaged '+name)
   a.pop(name,None)
  for name,v in a.items():
   missing=set(v.get('bones',{}))-bone_names
   if missing:raise ValueError(name+' missing bones '+str(missing))
   for field in ('particle_effects','sound_effects'):
    ev=v.get(field,{})
    if not isinstance(ev,dict) or any(not isinstance(x,dict) for x in ev.values()):raise ValueError(name+' incompatible event '+field)
   if prepared:
    if re.search(r'\bq\.',json.dumps(v)):raise ValueError(name+' unbound q alias')
    if 'animation_length' in v and v['animation_length']<=0:raise ValueError(name+' zero duration')
  generated=lambda n:any(n.endswith('.minemon_'+suffix) for suffix in ('run','sit','sleep','physical_attack'))
  for name in d['clips'].values():
   if name not in a and (prepared or not generated(name)):raise ValueError('Missing referenced clip '+name)
  attack=next((d['clips'][n] for n in ('physical_attack','physical','attack') if n in d['clips']),None)
  if attack and attack in a and d.get('attackTicks')!=math.ceil(a[attack]['animation_length']*20):raise ValueError('Attack duration mismatch '+d['slug'])
  if d['idle'] not in a or d['walk'] not in a:raise ValueError('Missing state clip '+d['slug'])
  if prepared:
   for state,keys in {'run':('ground_run','run'),'sit':('sit',),'sleep':('sleep',),'attack':('physical_attack','physical','attack')}.items():
    if not any(d['clips'].get(k) in a for k in keys):raise ValueError('Missing prepared '+state+' animation '+d['slug'])
  tex=read(d['texture']);dims=struct.unpack('>II',tex[16:24]);desc=g['minecraft:geometry'][0]['description']
  if dims!=(desc['texture_width'],desc['texture_height']):raise ValueError('UV dimensions '+d['slug'])
 print('PASS runtime asset validation:',len(definitions),'species')
if __name__=='__main__':
 if len(sys.argv)>1:
  with zipfile.ZipFile(sys.argv[1]) as z:
   defs=json.loads(z.read('data/minemon/runtime_species.json'));validate(z.read,defs,True)
   allowed={p for d in defs for p in [d['model'],d['texture'],d['animation']]+d['sounds']+d['licenses']}
   roots=('assets/minemon/geo/','assets/minemon/animations/','assets/minemon/textures/pokemon/','assets/minemon/sounds/pokemon/','assets/minemon/licenses/pokemon/')
   unexpected=[n for n in z.namelist() if not n.endswith('/') and n.startswith(roots) and n not in allowed]
   if unexpected:raise ValueError('Unexpected assets '+str(unexpected))
   for d in defs:
    egg=json.loads(z.read(f"assets/minemon/models/item/{d['slug']}_spawn_egg.json"));assert egg['parent']=='minecraft:item/template_spawn_egg'
   for p in ['pack.mcmeta','META-INF/mods.toml','META-INF/ASSET_LICENSES.md','br/com/petrickfaria/minemon/Minemon.class']:assert p in z.namelist()
   print('PASS JAR registrations/resources:',len(defs),'eggs; only selected assets packaged')
 else:
  defs=json.loads((R/'data/minemon/runtime_species.json').read_text());validate(lambda p:(R/p).read_bytes(),defs)
  enabled=next(l.split('=',1)[1].split(',') for l in (ROOT/'gradle.properties').read_text().splitlines() if l.startswith('runtime_species='))
  assert enabled==[d['slug'] for d in defs]
