package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
final class NineBehaviorChecks {
 static void shore(GameTestHelper h){shore(h,0);}
 private static void shore(GameTestHelper h,int index){
  if(index==2){h.succeed();return;}
  var level=h.getLevel();level.setDayTime(1000);
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<10;y++){
   boolean bank=x>=8;
   h.setBlock(new BlockPos(x,y,z),y==0||bank&&y<=4?Blocks.STONE:!bank&&y<=3?Blocks.WATER:Blocks.AIR);
  }
  var owner=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShoreTrainer"),null);
  owner.connection=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShoreConnection")).connection;
  var target=h.absolutePos(new BlockPos(14,5,7));owner.moveTo(target.getX()+.5,target.getY(),target.getZ()+.5,0,0);level.addNewPlayer(owner);
  var mob=h.spawn(ModEntities.SPECIES.get(index==0?"squirtle":"blastoise").get(),new BlockPos(4,3,7));mob.tame(owner);mob.setOrderedToSit(false);
  awaitShore(h,mob,owner,index,false,400);
 }
 private static void awaitShore(GameTestHelper h,MinemonCreature mob,net.minecraft.server.level.ServerPlayer owner,int index,boolean entering,int remaining){
  double localX=mob.getX()-h.absolutePos(BlockPos.ZERO).getX();
  boolean reached=entering?mob.isInWater()&&localX<7.5:!mob.isInWater()&&mob.isOnGround()&&localX>=8.5;
  if(reached){
   if(!entering){var at=h.absolutePos(new BlockPos(2,3,7));owner.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);awaitShore(h,mob,owner,index,true,400);}
   else{mob.discard();owner.discard();shore(h,index+1);}return;
  }
  if(remaining<=0){String state=mob.getSpeciesSlug()+" pos="+mob.position()+" water="+mob.isInWater()+" horizontalCollision="+mob.horizontalCollision+" path="+mob.getNavigation().getTargetPos();mob.discard();owner.discard();h.fail("One-block shoreline traversal did not finish: entering="+entering+" "+state);return;}
  h.runAfterDelay(5,()->awaitShore(h,mob,owner,index,entering,remaining-5));
 }
 static void defeatedFloaters(GameTestHelper h){
  var mobs=new ArrayList<MinemonCreature>();var species=List.of("gastly","haunter","gengar","abra","porygon","geodude");
  for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<=9;y++)h.setBlock(new BlockPos(x,y,z),y==0?Blocks.STONE:Blocks.AIR);
  for(int i=0;i<species.size();i++){
   var mob=h.spawn(ModEntities.SPECIES.get(species.get(i)).get(),new BlockPos(2+(i%3)*5,6,3+(i/3)*7));
   mob.setTame(true);mob.setOwnerUUID(UUID.randomUUID());mob.rangedCharges().consume();for(int t=0;t<50;t++)mob.rangedCharges().tick(true);
   mob.die(DamageSource.GENERIC);mobs.add(mob);
  }
  var zombie=h.spawn(net.minecraft.world.entity.EntityType.ZOMBIE,new BlockPos(8,1,7));zombie.setNoAi(true);zombie.setTarget(mobs.get(0));
  if(zombie.getTarget()!=null)h.fail("Vanilla hostile acquired defeated companion");
  h.runAfterDelay(180,()->{
   for(var mob:mobs){
    if(!mob.isDefeated()||mob.isRemoved()||!mob.isOnGround()||mob.getY()-h.absolutePos(BlockPos.ZERO).getY()>1.1)h.fail("Defeated floater did not settle on the floor: "+mob.getSpeciesSlug()+" "+mob.position());
    if(mob.rangedCharges().charges()!=2||mob.rangedCharges().elapsed()!=50)h.fail("Defeated companion recharged");
    var n=mob.saveWithoutId(new CompoundTag());var copy=ModEntities.SPECIES.get(mob.getSpeciesSlug()).get().create(h.getLevel());copy.load(n);
    if(copy.isAttackable()||!copy.isDefeated()||copy.rangedCharges().elapsed()!=50)h.fail("Defeated target/recharge state did not survive reload");
    mob.discard();
   }zombie.discard();h.succeed();
  });
 }
}
