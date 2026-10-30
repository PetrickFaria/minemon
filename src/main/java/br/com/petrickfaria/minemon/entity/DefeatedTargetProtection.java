package br.com.petrickfaria.minemon.entity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
@Mod.EventBusSubscriber(modid=br.com.petrickfaria.minemon.Minemon.MOD_ID)
public final class DefeatedTargetProtection {
 @SubscribeEvent public static void target(LivingChangeTargetEvent event){if(event.getNewTarget() instanceof MinemonCreature p && p.isDefeated())event.setNewTarget(null);}
 public static void clear(MinemonCreature p){
  if(p.level instanceof net.minecraft.server.level.ServerLevel server)for(var entity:server.getAllEntities()){
   if(entity instanceof net.minecraft.world.entity.Mob mob && mob.getTarget()==p)mob.setTarget(null);
   if(entity instanceof net.minecraft.world.entity.LivingEntity living && living.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
       && living.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET).orElse(null)==p)
    living.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
  }
 }
 private DefeatedTargetProtection(){}
}
