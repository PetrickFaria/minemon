#!/usr/bin/env python3
"""Measure selected meshes in sampled native idle poses; omit extremities from collision bounds."""
import json,math,re
from functools import lru_cache
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
SIN=lambda v:math.sin(math.radians(v))
COS=lambda v:math.cos(math.radians(v))
FUN={'sin':SIN,'cos':COS,'clamp':lambda x,a,b:max(a,min(b,x)),'abs':abs,'min':min,'max':max,'sqrt':math.sqrt,'floor':math.floor,'ceil':math.ceil}
@lru_cache(maxsize=200000)
def scalar(v,t):
 if isinstance(v,(float,int)):return float(v)
 if not isinstance(v,str):return 0
 v=v.lower();v=re.sub(r'\b(q|query)\.(anim_time|life_time)',str(t),v)
 v=re.sub(r'\b(q|query)\.[a-z_]+','0',v);v=v.replace('math.','')
 if re.search(r'[^a-z0-9_.,+*/()% -]',v):return 0
 try:return float(eval(v,{'__builtins__':{}},FUN))
 except (ValueError,NameError,SyntaxError,TypeError,ZeroDivisionError):return 0

def vector(v,t,default):
 if isinstance(v,dict):
  if 'vector' in v:v=v['vector']
  elif 'post' in v:v=v['post']
  elif 'pre' in v:v=v['pre']
  else:
   keys=sorted((float(k),k) for k in v if re.fullmatch(r'[0-9.]+',k))
   if keys:
    k=next((k for time,k in reversed(keys) if time<=t),keys[0][1]);return vector(v[k],t,default)
   return default
 if isinstance(v,list):return [scalar(x,t) for x in v]
 if v is None:return default
 return [scalar(v,t)]*3

def rotate(p,pivot,angles):
 x,y,z=[p[i]-pivot[i] for i in range(3)]
 # GeckoLib 3 GeoBuilder/GeoCube reflect the raw X coordinates and negate
 # X/Y rotations; RenderUtils composes Z,Y,X. In this raw-coordinate frame
 # that is Rz(-z) Ry(y) Rx(-x), including animation-controller rotations.
 angles=[-angles[0],angles[1],-angles[2]]
 for axis,a in enumerate(angles):
  c,s=COS(a),SIN(a)
  if axis==0:y,z=y*c-z*s,y*s+z*c
  elif axis==1:x,z=x*c+z*s,-x*s+z*c
  else:x,y=x*c-y*s,x*s+y*c
 return [x+pivot[0],y+pivot[1],z+pivot[2]]

def measure(geo,clip):
 bones={b['name']:b for g in geo['minecraft:geometry'] for b in g['bones']};tracks=clip.get('bones',{})
 core=[];full=[]
 for step in range(17):
  t=step*.25
  for b in bones.values():
   chain=[];n=b;seen=set()
   while n and n['name'] not in seen:seen.add(n['name']);chain.append(n);n=bones.get(n.get('parent'))
   appendage=any(re.search(r'wing|tail|flame|fire|spine|horn|mane|feather|antenna|whisker|tongue|gas|locator|fin|leaf|leaves|flower|petal|vine|tentacle',n['name'],re.I) for n in chain)
   if any(min(vector(tracks.get(n['name'],{}).get('scale'),t,[1,1,1]))<.05 for n in chain):continue
   for cube in b.get('cubes',[]):
    inflate=cube.get('inflate',b.get('inflate',0)) or 0
    for mask in range(8):
     p=[cube['origin'][i]+(cube['size'][i]+inflate if mask&(1<<i) else -inflate) for i in range(3)]
     p=rotate(p,cube.get('pivot',[0,0,0]),cube.get('rotation',[0,0,0]))
     for n in chain:
      tr=tracks.get(n['name'],{});pivot=n.get('pivot',[0,0,0]);scale=vector(tr.get('scale'),t,[1,1,1])
      p=[pivot[i]+(p[i]-pivot[i])*scale[i] for i in range(3)]
      rotation=vector(tr.get('rotation'),t,[0,0,0]);rotation=[rotation[i]+n.get('rotation',[0,0,0])[i] for i in range(3)]
      p=rotate(p,pivot,rotation);offset=vector(tr.get('position'),t,[0,0,0]);p=[p[i]+offset[i] for i in range(3)]
     p=[v/16 for v in p];full.append(p)
     if not appendage:core.append(p)
 if not core:core=full
 bounds=lambda a:[min(p[i] for p in a) for i in range(3)]+[max(p[i] for p in a) for i in range(3)]
 return bounds(core),bounds(full)

def generate():
 result={}
 for d in json.loads((ROOT/'src/main/resources/data/minemon_gen1/runtime_species.json').read_text()):
  geo=json.loads((ROOT/'src/main/resources'/d['model']).read_text());a=ROOT/'src/main/resources'/d['animation']
  animations=json.loads(a.read_text()).get('animations',{}) if a.exists() else {}
  clip=animations.get(d['idle'],animations.get(d['clips'].get('ground_idle'),{}))
  core,full=measure(geo,clip);w=core[3]-core[0];depth=core[5]-core[2]
  # Vanilla collision boxes are square in X/Z. Long slender articulated bodies
  # use their thickness, rather than a tail-length-sized square on every turn.
  width=min(w,depth)*1.25 if max(w,depth)>2.5*max(.05,min(w,depth)) else max(w,depth)
  if d['slug'] in ('ekans','arbok','onix','gyarados','dratini','dragonair'):
   # A square box around an entire articulated coil obstructs paths. Use the
   # measured largest solid segment cross-section for these serpentine bodies.
   sizes=[c['size'] for g in geo['minecraft:geometry'] for b in g['bones'] if not re.search(r'tail|wing|horn|spine|fin|whisker|eye|pupil|lip|tooth',b['name'],re.I) for c in b.get('cubes',[]) if all(v>0 for v in c['size'])]
   if sizes:width=max(max(v[0],v[2]) for v in sizes)/16
  result[d['slug']]={'width':max(.15,width),'height':max(.15,core[4]-min(0,core[1])),'top':full[4],'bottom':full[1],'core_bounds':core,'source':d['model']}
 out=ROOT/'src/main/resources/data/minemon_gen1/model_metrics.json';out.write_text(json.dumps(result,indent=2)+'\n')
 print('Measured',len(result),'models')
 for slug in ('charmander','gastly','gyarados','onix','abra','porygon'):print(slug,{k:round(v,3) for k,v in result[slug].items() if isinstance(v,(float,int))})

if __name__=="__main__":generate()
