from pathlib import Path
import zipfile
out=Path('build/diagnostics');out.mkdir(parents=True,exist_ok=True)
want={'net/minecraft/world/entity/animal/Animal.java','net/minecraft/world/entity/TamableAnimal.java','net/minecraft/world/entity/Mob.java','net/minecraft/gametest/framework/GameTestHelper.java','net/minecraft/world/entity/ai/goal/MeleeAttackGoal.java'}
for path in (Path.home()/'.gradle/caches/forge_gradle').rglob('*sources*.jar'):
 try:
  with zipfile.ZipFile(path) as z:
   for name in want.intersection(z.namelist()): (out/Path(name).name).write_bytes(z.read(name))
 except Exception:pass
