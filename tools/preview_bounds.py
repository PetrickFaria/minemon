"""Conservative mesh bounds, including bind rotations and every alternate rig."""
import math

def mesh_bounds(geometry):
 bones={b['name']:b for g in geometry.get('minecraft:geometry',[]) for b in g.get('bones',[])}
 def rotate(point,pivot,angles):
  x,y,z=(point[i]-pivot[i] for i in range(3))
  for axis,angle in enumerate(angles):
   c,s=math.cos(math.radians(angle)),math.sin(math.radians(angle))
   if axis==0:y,z=y*c-z*s,y*s+z*c
   elif axis==1:x,z=x*c+z*s,-x*s+z*c
   else:x,y=x*c-y*s,x*s+y*c
  return [x+pivot[0],y+pivot[1],z+pivot[2]]
 points=[]
 for bone in bones.values():
  for cube in bone.get('cubes',[]):
   origin=cube['origin'];size=cube['size'];inflate=cube.get('inflate',0)
   for bits in range(8):
    point=[origin[i]+size[i]+inflate if bits&(1<<i) else origin[i]-inflate for i in range(3)]
    point=rotate(point,cube.get('pivot',[0,0,0]),cube.get('rotation',[0,0,0]))
    parent=bone;seen=set()
    while parent and parent['name'] not in seen:
     seen.add(parent['name']);point=rotate(point,parent.get('pivot',[0,0,0]),parent.get('rotation',[0,0,0]))
     parent=bones.get(parent.get('parent'))
    points.append([v/16 for v in point])
 if not points:raise ValueError('Model has no cubes')
 return [min(p[i] for p in points) for i in range(3)]+[max(p[i] for p in points) for i in range(3)]
