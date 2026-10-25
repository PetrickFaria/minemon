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
for path in (Path.home()/'.gradle/caches/forge_gradle/minecraft_user_repo').rglob('*mapped_official_1.19.2.jar'):
 if path.name.endswith('-sources.jar'):continue
 for cls in ['net.minecraft.world.entity.animal.Animal','net.minecraft.world.entity.TamableAnimal','net.minecraft.world.entity.Mob','net.minecraft.world.entity.LivingEntity','net.minecraft.server.level.ServerPlayer','net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal','net.minecraft.world.entity.ai.goal.target.TargetGoal','net.minecraft.world.entity.ai.navigation.PathNavigation','net.minecraft.world.entity.ai.navigation.GroundPathNavigation','net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation','net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl','net.minecraft.world.entity.ai.control.LookControl','net.minecraft.world.level.pathfinder.PathFinder','net.minecraft.world.level.pathfinder.AmphibiousNodeEvaluator','net.minecraft.world.level.pathfinder.WalkNodeEvaluator']:
  result=subprocess.run(['javap','-c','-p','-classpath',str(path),cls],text=True,capture_output=True)
  (out/(cls.rsplit('.',1)[-1]+'.bytecode.txt')).write_text(result.stdout)
 break
