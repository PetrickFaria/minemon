#!/usr/bin/env python3
"""Adapt copied runtime animations to GeckoLib 3; original assets remain intact."""
import json,re,sys,struct,zlib
from pathlib import Path
root=Path(sys.argv[1])
count=0
manifest=root/'data/minemon_gen1/runtime_species.json'
definitions={d['animation'].replace('assets/minemon/','assets/minemon_gen1/'):d for d in json.loads(manifest.read_text())} if manifest.exists() else {}
def add_pose_animation(data, geo, d, name, kind):
 animations=data.setdefault('animations',{})
 if name in animations:return
 clips=d.get('clips',{})
 idle_name=d.get('idle') or clips.get('ground_idle') or clips.get('air_idle')
 source=animations.get(idle_name)
 if not source:return
 animation=json.loads(json.dumps(source));animation['loop']=kind!='physical_attack';animation['animation_length']=0.6 if kind=='physical_attack' else 4.0
 bones={b['name'] for g in geo.get('minecraft:geometry',[]) for b in g.get('bones',[])}
 channels=animation.setdefault('bones',{})
 if kind=='sit':
  root=next((n for n in ('body','torso','torso2') if n in bones),None)
  if root:
   channels.setdefault(root,{})['position']=[0,-1.0,0]
   channels[root]['rotation']=[8,0,0]
  for n in ('leg_left','leg_right','leg_front_left','leg_front_right','leg_back_left','leg_back_right'):
   if n in bones:channels.setdefault(n,{})['rotation']=[24,0,0]
  for n in ('leg_left2','leg_right2','knee_left','knee_right'):
   if n in bones:channels.setdefault(n,{})['rotation']=[-32,0,0]
 elif kind=='sleep':
  root=next((n for n in ('body','torso','torso2') if n in bones),None)
  if root:
   channels.setdefault(root,{})['position']=[0,-1.4,0]
   channels[root]['rotation']=[18,0,0]
  for n in ('eyelid_left','eyelid_right','eyelid','eye_lid'):
   if n in bones:channels.setdefault(n,{})['position']=[0,-2,0]
 elif kind=='physical_attack':
  root=next((n for n in ('torso','body','head','head_ai') if n in bones),None)
  attack_curve={'0.0':{'vector':[0,0,0]},'0.15':{'vector':[-20,0,0]},'0.32':{'vector':[12,0,0]},'0.6':{'vector':[0,0,0]}}
  if root:channels.setdefault(root,{})['rotation']=attack_curve
  for n in ('arm_right','arm_left','forearm_right','forearm_left','leg_right','leg_left','tail'):
   if n in bones:
    angle=-34 if 'right' in n or 'tail' in n else 24
    channels.setdefault(n,{})['rotation']={'0.0':{'vector':[0,0,0]},'0.15':{'vector':[angle,0,0]},'0.32':{'vector':[-angle*0.45,0,0]},'0.6':{'vector':[0,0,0]}}
 animations[name]=animation

def build_runtime_clips(data,geo,d):
 animations=data.setdefault('animations',{});clips=d.get('clips',{})
 walk_name=d.get('walk')
 run_name=clips.get('ground_run','')
 if run_name.endswith('.minemon_run') and walk_name in animations:
  run=json.loads(json.dumps(animations[walk_name]));run['loop']=True
  def speed(v):
   if isinstance(v,str):return re.sub(r'\b(query|q)\.anim_time\s*\*\s*(\d+(?:\.\d+)?)',lambda m:f'{m.group(1)}.anim_time*{float(m.group(2))*1.35:.6g}',v)
   if isinstance(v,list):return [speed(x) for x in v]
   if isinstance(v,dict):return {k:speed(x) for k,x in v.items()}
   return v
  animations[run_name]=speed(run)
 for key,kind in [('sit','sit'),('sleep','sleep'),('physical_attack','physical_attack')]:
  name=clips.get(key,'')
  if name.endswith('.minemon_'+kind):add_pose_animation(data,geo,d,name,kind)

 # The three fire starters have arm bones posed outward in their bind pose.
 # Their source walk clips animate those bones but flatten the Z rotation to
 # roughly zero, unlike the inward rotations in each species' idle clip.
 # Keep the original shoulder swing channels and restore the model-specific
 # inward pose with a restrained opposite-phase gait swing.
 if d.get('slug') in ('charmander','charmeleon','charizard') and walk_name in animations:
  rest={'charmander':35.0,'charmeleon':42.5,'charizard':55.0}[d['slug']]
  swing={'charmander':5.0,'charmeleon':4.0,'charizard':5.0}[d['slug']]
  frequency={'charmander':'90*6','charmeleon':'90*4.5','charizard':'90*3'}[d['slug']]
  for clip_name in (walk_name,run_name):
   if not clip_name or clip_name not in animations: continue
   walk_bones=animations[clip_name].setdefault('bones',{})
   for side,sign in (('left',1),('right',-1)):
    name='arm_'+side
    channel=walk_bones.get(name)
    if not isinstance(channel,dict): continue
    rotation=channel.get('rotation')
    if not isinstance(rotation,list) or len(rotation)!=3: continue
    channel['rotation'][2]=f'{sign*rest:g}{"+" if sign>0 else "-"}{swing:g}*math.sin(query.anim_time*{frequency})'

def png_chunks(data):
 if data[:8]!=b'\x89PNG\r\n\x1a\n': raise ValueError('Not a PNG texture')
 pos=8;chunks=[]
 while pos<len(data):
  n=struct.unpack('>I',data[pos:pos+4])[0];kind=data[pos+4:pos+8];payload=data[pos+8:pos+8+n];chunks.append((kind,payload));pos+=12+n
  if kind==b'IEND':break
 return chunks

def decode_png_rgba(data):
 chunks=png_chunks(data);items={kind:payload for kind,payload in chunks}
 w,h,depth,color,compression,filter_method,interlace=struct.unpack('>IIBBBBB',items[b'IHDR'])
 if depth!=8 or color not in (2,3,6) or interlace!=0:raise ValueError('Unsupported source PNG format')
 channels={2:3,3:1,6:4}[color];stride=w*channels
 packed=zlib.decompress(b''.join(payload for kind,payload in chunks if kind==b'IDAT'))
 prev=bytearray(stride);rows=[];offset=0
 for _ in range(h):
  f=packed[offset];offset+=1;scan=bytearray(packed[offset:offset+stride]);offset+=stride
  for i in range(stride):
   a=scan[i-channels] if i>=channels else 0;b=prev[i];c=prev[i-channels] if i>=channels else 0
   if f==1:scan[i]=(scan[i]+a)&255
   elif f==2:scan[i]=(scan[i]+b)&255
   elif f==3:scan[i]=(scan[i]+((a+b)//2))&255
   elif f==4:
    p=a+b-c;pa=abs(p-a);pb=abs(p-b);pc=abs(p-c);scan[i]=(scan[i]+(a if pa<=pb and pa<=pc else b if pb<=pc else c))&255
   elif f!=0:raise ValueError('Unsupported PNG filter')
  rows.append(scan);prev=scan
 palette=items.get(b'PLTE',b'');alpha=items.get(b'tRNS',b'');pixels=[]
 for row in rows:
  out=bytearray()
  if color==6: out.extend(row)
  elif color==2:
   for i in range(0,len(row),3):out.extend((row[i],row[i+1],row[i+2],255))
  else:
   for index in row:
    j=index*3
    if j+2>=len(palette):raise ValueError('PNG palette index outside palette')
    out.extend((palette[j],palette[j+1],palette[j+2],alpha[index] if index<len(alpha) else 255))
  pixels.append(out)
 return w,h,pixels

def encode_png_rgba(w,h,rows):
 def chunk(kind,payload):return struct.pack('>I',len(payload))+kind+payload+struct.pack('>I',zlib.crc32(kind+payload)&0xffffffff)
 raw=b''.join(b'\0'+bytes(row) for row in rows)
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')

def prepare_tail_flame(root,d):
 slug=d.get('slug')
 if slug not in ('charmander','charmeleon','charizard'):return
 model=root/'assets/minemon_gen1'/d['model'].removeprefix('assets/minemon/')
 texture=root/'assets/minemon_gen1'/d['texture'].removeprefix('assets/minemon/')
 geo=json.loads(model.read_text());texture_data=texture.read_bytes();w,h,rows=decode_png_rgba(texture_data)
 sprite_sizes={'charmander':(5,8),'charmeleon':(6,10),'charizard':(8,12)}
 fw,fh=sprite_sizes[slug];pad=2;new_w=w+24;x=w+pad;y=2
 # A compact pixel flame is drawn only into the generated runtime atlas.
 # The extracted/original art and its license-bearing source files stay intact.
 for yy in range(fh):
  for xx in range(fw):
   # Layered pointed silhouette, from a narrow yellow core to orange/red edges.
   center=fw/2-0.5;distance=abs(xx-center);progress=yy/max(1,fh-1)
   half=(0.7+progress*(fw/2-0.7))
   if distance>half: continue
   if progress<0.22 and distance>max(0,half-1.2): rgba=(255,226,72,255)
   elif distance<=max(0.6,half*0.36): rgba=(255,244,142,255)
   elif distance<=max(1.0,half*0.7): rgba=(255,174,38,255)
   else: rgba=(232,65,24,255)
   i=(y+yy)*new_w*4+(x+xx)*4;row=rows[y+yy]
   # Extend the row once when moving from the original atlas to the new atlas.
   if len(row)<new_w*4: row.extend(b'\0'*(new_w*4-len(row)))
   row[(x+xx)*4:(x+xx+1)*4]=bytes(rgba)
 for row in rows:
  if len(row)<new_w*4:row.extend(b'\0'*(new_w*4-len(row)))
 description=geo['minecraft:geometry'][0]['description'];description['texture_width']=new_w
 bones=geo['minecraft:geometry'][0]['bones'];fire=next((b for b in bones if b['name']=='fire'),None)
 if fire is None:raise ValueError(slug+' model has no animated fire bone')
 # Replace the original zero-width / invalid-UV planes with two crossed thin
 # sprites. Both remain children of the animated tail-tip fire bone.
 y0,z0={'charmander':(5.7,15.5),'charmeleon':(8.25,18.0),'charizard':(7.75,37.5)}[slug]
 def plane(origin,size,faces):
  return {'origin':origin,'size':size,'uv':{face:{'uv':[x,y],'uv_size':[fw,fh]} for face in faces}}
 all_sides=('north','south','east','west')
 fire['cubes']=[plane([-fw/2,y0,z0-1],[fw,fh,2],all_sides+('up','down')),
                plane([-1,y0,z0-fw/2],[2,fh,fw],all_sides+('up','down'))]
 model.write_text(json.dumps(geo,ensure_ascii=False,indent=2)+'\n')
 texture.write_bytes(encode_png_rgba(new_w,h,rows))
for p in (root/'assets/minemon_gen1/animations').rglob('*.animation.json'):
 data=json.loads(p.read_text())
 def normalize(v):
  if isinstance(v,str): return re.sub(r'\bq\.', 'query.',v)
  if isinstance(v,list): return [normalize(x) for x in v]
  if isinstance(v,dict): return {k:normalize(x) for k,x in v.items()}
  return v
 data=normalize(data)
 definition=definitions.get(str(p.relative_to(root)),{})
 for name in definition.get('excluded_clips',[]): data.get('animations',{}).pop(name,None)
 if definition:
  # Files in the processed resource tree keep the normal assets/minemon root.
  model=root/'assets/minemon_gen1'/definition['model'].removeprefix('assets/minemon/')
  if model.exists():build_runtime_clips(data,json.loads(model.read_text()),definition)
  prepare_tail_flame(root,definition)
 # GeckoLib 3.1.40 dereferences this field while parsing every clip, including
 # expression-only looping clips. Keep the source untouched and add a runtime fallback.
 for animation in data.get('animations',{}).values():
  for field in ('particle_effects','sound_effects'):
   events=animation.get(field)
   if not isinstance(events,dict) or any(not isinstance(value,dict) for value in events.values()): animation.pop(field,None)
  if 'animation_length' not in animation:animation['animation_length']=4.0
 p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
 count+=1
print('GeckoLib 3 runtime animations prepared:',count)
