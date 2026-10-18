from pathlib import Path
import zipfile
out=Path('build/diagnostics');out.mkdir(parents=True,exist_ok=True)
want={'net/minecraft/world/entity/animal/Animal.java','net/minecraft/world/entity/TamableAnimal.java','net/minecraft/world/entity/Mob.java','net/minecraft/gametest/framework/GameTestHelper.java','net/minecraft/world/entity/ai/goal/MeleeAttackGoal.java'}
for path in (Path.home()/'.gradle/caches/forge_gradle').rglob('*sources*.jar'):
 try:
  with zipfile.ZipFile(path) as z:
   for name in want.intersection(z.namelist()): (out/Path(name).name).write_bytes(z.read(name))
 except Exception:pass

import subprocess
for path in (Path.home()/'.gradle/caches/forge_gradle').rglob('*.jar'):
 try:
  with zipfile.ZipFile(path) as z:
   if 'net/minecraft/world/entity/animal/Animal.class' not in z.namelist():continue
  for cls in ['net.minecraft.world.entity.animal.Animal','net.minecraft.world.entity.TamableAnimal','net.minecraft.world.entity.Mob']:
   result=subprocess.run(['javap','-c','-p','-classpath',str(path),cls],text=True,capture_output=True)
   (out/(cls.rsplit('.',1)[-1]+'.bytecode.txt')).write_text(result.stdout)
  break
 except Exception: pass
