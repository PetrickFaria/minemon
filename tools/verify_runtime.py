import json,struct,sys,zipfile,re,math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources'
SOURCE_PREFIX='assets/minemon/';RUNTIME_PREFIX='assets/minemon_gen1/'
def runtime_path(path):return path.replace(SOURCE_PREFIX,RUNTIME_PREFIX,1) if path.startswith(SOURCE_PREFIX) else path
def validate(read,definitions,prepared=False,packed=False):
 previous=0
 for d in definitions:
  number=d['nationalPokedexNumber']
  if number<=previous:raise ValueError('Runtime roster is not in National Pokédex order')
  previous=number
  if (number<=10 or d.get('source_cobblemon_version')=='1.8.2') and not all('_cobblemon_1_8_2/' in d[key] for key in ('model','texture','animation')):
   raise ValueError('Batch 1 species is not using all three Cobblemon 1.8.2 assets: '+d['slug'])
  model_path=runtime_path(d['model']) if packed else d['model']; animation_path=runtime_path(d['animation']) if packed else d['animation']; texture_path=runtime_path(d['texture']) if packed else d['texture']
  g=json.loads(read(model_path))
  try:a=json.loads(read(animation_path))['animations']
  except FileNotFoundError:
   if d.get('generated_static_animation') and not prepared and not packed:a={d['idle']:{'animation_length':1.0,'loop':True,'bones':{}}}
   else:raise
  if d.get('generated_static_animation') and not prepared and not packed:
   a.setdefault(d['idle'],{'animation_length':1.0,'loop':True,'bones':{}})
  bone_names={b['name'] for part in g['minecraft:geometry'] for b in part['bones']}
  for name in d.get('excluded_clips',[]):
   if prepared and name in a:raise ValueError('Excluded clip packaged '+name)
   a.pop(name,None)
  for name,v in a.items():
   missing=set(v.get('bones',{}))-bone_names
   approved_to_strip=set(d.get('stripped_bones',{}).get(name,[]))
   if prepared and missing:raise ValueError(name+' missing bones '+str(missing))
   if not prepared and missing!=approved_to_strip:raise ValueError(name+' missing bones '+str(missing)+' (audit allows only '+str(approved_to_strip)+')')
   for field in ('particle_effects','sound_effects'):
    ev=v.get(field,{})
    valid_event_value=lambda x:isinstance(x,dict) or (not prepared and isinstance(x,list) and all(isinstance(item,dict) for item in x))
    if not isinstance(ev,dict) or any(not valid_event_value(x) for x in ev.values()):raise ValueError(name+' incompatible event '+field)
   if prepared:
    if re.search(r'\bq\.',json.dumps(v)):raise ValueError(name+' unbound q alias')
    if 'animation_length' in v and v['animation_length']<=0:raise ValueError(name+' zero duration')
  generated=lambda n:any(n.endswith('.minemon_'+suffix) for suffix in ('run','sit','sleep','physical_attack','tail_flame'))
  for name in d['clips'].values():
   if name not in a and (prepared or not generated(name)):raise ValueError('Missing referenced clip '+name)
  attack=next((d['clips'][n] for n in ('physical_attack','physical','attack') if n in d['clips']),None)
  if attack and attack in a and d.get('attackTicks')!=math.ceil(a[attack]['animation_length']*20):raise ValueError('Attack duration mismatch '+d['slug'])
  if d['idle'] not in a or d['walk'] not in a:raise ValueError('Missing state clip '+d['slug'])
  if number<=10:
   source_runs={3:'animation.venusaur.ground_run',6:'animation.charizard.ground_run',9:'animation.blastoise.ground_run'}
   expected_run=source_runs.get(number)
   if d['clips'].get('ground_run')!=expected_run:raise ValueError('Batch 1 source run reference differs from audited poser evidence: '+d['slug'])
  if prepared:
   for state,keys in {'run':('ground_run','run'),'sit':('sit',),'sleep':('sleep',),'attack':('physical_attack','physical','attack')}.items():
    if state=='run' and not any(d['clips'].get(k) in a for k in keys):continue
    if 101<=number<=140 and state in ('sit','sleep','attack') and not any(d['clips'].get(k) in a for k in keys):
     # Batches 11–14 follow the native Cobblemon action inventory. Do not synthesize
     # a replacement pose solely to satisfy a generic runtime-resource check.
     continue
    if not any(d['clips'].get(k) in a for k in keys):raise ValueError('Missing prepared '+state+' animation '+d['slug'])
   if d['slug'] in ('charmander','charmeleon','charizard'):
    anchor=d.get('flame_anchor');flame=next((b for b in g['minecraft:geometry'][0]['bones'] if b['name']=='tail_flame'),None)
    if not anchor or not flame or flame.get('parent')!=anchor['host']:raise ValueError('Tail flame is not attached to its source flame bone: '+d['slug'])
    cubes=flame.get('cubes',[])
    if len(cubes)!=2 or any(any(n<=0 for n in cube.get('size',[])) for cube in cubes):raise ValueError('Tail flame is missing or has degenerate geometry: '+d['slug'])
    for cube in cubes:
     origin=cube.get('origin',[]);size=cube.get('size',[])
     if len(origin)!=3 or len(size)!=3 or abs(origin[1]-anchor['origin'][1])>0.001 or abs(origin[2]-anchor['origin'][2])>0.001 or abs((origin[1]+size[1])- (anchor['origin'][1]+anchor['size'][1]))>0.001 or abs((origin[2]+size[2])-(anchor['origin'][2]+anchor['size'][2]))>0.001:
      raise ValueError('Tail flame geometry no longer matches the source plane bounds: '+d['slug'])
   if d['slug']=='charmander':
    if d.get('tail_flame_animation'):
     flame_clip=d['clips'].get('tail_flame');clip=a.get(flame_clip,{})
     if not flame_clip or not clip.get('loop') or not {'rotation','scale'}.issubset(clip.get('bones',{}).get('tail_flame',{})):
      raise ValueError('Charmander has no independent looping tail-flame rotation/scale animation')
    texwidth=g['minecraft:geometry'][0]['description']['texture_width']
    for cube in cubes:
     uv=cube.get('uv',[]);u,v=uv if isinstance(uv,list) and len(uv)==2 else (-1,-1)
     sx,sy,sz=cube.get('size',[0,0,0]);box_width=2*sx+2*sz;box_height=sy+sz
     if u<0 or v<0 or u+box_width>texwidth or v+box_height>g['minecraft:geometry'][0]['description']['texture_height']:
      raise ValueError('Tail flame box UV outside texture: '+d['slug'])
    for key,sign,base in [('left',1,{'charmander':35,'charmeleon':42.5,'charizard':55}[d['slug']]),('right',-1,{'charmander':-35,'charmeleon':-42.5,'charizard':-55}[d['slug']])]:
     for clip_key in ('ground_walk','ground_run'):
      if clip_key not in d['clips']:continue
      clip=d['clips'][clip_key];rotation=a[clip]['bones']['arm_'+key]['rotation']
      if len(rotation)!=3 or not str(rotation[2]).startswith(str(base)):
       raise ValueError('Walk/run arm rest pose was not preserved for '+d['slug']+'/'+clip_key+'/'+key)
   if d['slug']=='charmander':
    if 'charmander_cobblemon_1_8_2' not in d['model'] or 'charmander_cobblemon_1_8_2' not in d['texture'] or 'charmander_cobblemon_1_8_2' not in d['animation']:
     raise ValueError('Charmander pilot is not using the Cobblemon 1.8.2 model, texture, and animation assets')
    bones={b['name']:b for b in g['minecraft:geometry'][0]['bones']}
    if d.get('strip_bedrock_locators') and any('locators' in b for b in bones.values()):raise ValueError('Charmander runtime geometry retained unsupported Bedrock locator metadata')
    for name,parent,pivot in [('tail3','tail2',[0,4,13.5]),('fire','tail3',[0,4.25,16.5]),('fire2','fire',[0,4.25,16.5]),('locator_fire','fire2',[0,5,16.5])]:
     b=bones.get(name)
     if b is None or b.get('parent')!=parent or b.get('pivot')!=pivot:raise ValueError('Charmander Cobblemon tail/flame rig changed: '+name)
    if d['idle']!='animation.charmander.ground_idle' or d['walk']!='animation.charmander.ground_walk':raise ValueError('Charmander pilot clips do not use Cobblemon ground idle/walk')
    if not a[d['idle']].get('loop') or not a[d['walk']].get('loop'):raise ValueError('Charmander idle and walk must loop')
    for clip in (d['idle'],d['walk']):
     if not {'leg_left','leg_right','tail','torso'}.issubset(a[clip].get('bones',{})):raise ValueError('Charmander '+clip+' lost its source articulation channels')
  tex=read(texture_path);dims=struct.unpack('>II',tex[16:24]);desc=g['minecraft:geometry'][0]['description']
  if dims!=(desc['texture_width'],desc['texture_height']):raise ValueError('UV dimensions '+d['slug'])
 print('PASS runtime asset validation:',len(definitions),'species; National Pokédex order')
if __name__=='__main__':
 manifest='data/minemon_gen1/runtime_species.json'
 dex='data/minemon_gen1/pokedex.json'
 if len(sys.argv)>1:
  with zipfile.ZipFile(sys.argv[1]) as z:
   defs=json.loads(z.read(manifest));validate(z.read,defs,True,True)
   entries=json.loads(z.read(dex));assert len(entries)==151 and [e['nationalPokedexNumber'] for e in entries]==list(range(1,152))
   active={d['slug'] for d in defs};assert active=={e['slug'] for e in entries if e['registered']}
   allowed={runtime_path(p) for d in defs for p in [d['model'],d['texture'],d['animation']]+d['sounds']+d['licenses']}
   roots=('assets/minemon_gen1/geo/','assets/minemon_gen1/animations/','assets/minemon_gen1/textures/pokemon/','assets/minemon_gen1/sounds/pokemon/','assets/minemon_gen1/licenses/pokemon/')
   unexpected=[n for n in z.namelist() if not n.endswith('/') and n.startswith(roots) and n not in allowed]
   if unexpected:raise ValueError('Unexpected assets '+str(unexpected))
   for d in defs:
    egg=json.loads(z.read(f"assets/minemon_gen1/models/item/{d['slug']}_spawn_egg.json"));assert egg['parent']=='minecraft:item/template_spawn_egg'
   for p in ['pack.mcmeta','META-INF/mods.toml','META-INF/ASSET_LICENSES.md','br/com/petrickfaria/minemon/Minemon.class',manifest,dex]:assert p in z.namelist(),p
   print('PASS JAR registrations/resources:',len(defs),'eggs; only selected assets packaged')
   meta=z.read('META-INF/mods.toml').decode();assert 'modId="minemon_gen1"' in meta and 'displayName="Minemon (1ª Geração)"' in meta
   print('PASS Minemon (1ª Geração) metadata and minemon_gen1 ID')
 else:
  defs=json.loads((R/manifest).read_text());validate(lambda p:(R/p).read_bytes(),defs)
  enabled=next(l.split('=',1)[1].split(',') for l in (ROOT/'gradle.properties').read_text().splitlines() if l.startswith('runtime_species='))
  assert enabled==[d['slug'] for d in defs]
