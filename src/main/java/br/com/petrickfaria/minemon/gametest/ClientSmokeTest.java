package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.Minemon;
import br.com.petrickfaria.minemon.client.MinemonRenderer;
import br.com.petrickfaria.minemon.entity.*;
import br.com.petrickfaria.minemon.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib3.resource.GeckoLibCache;
import java.nio.file.*;
import java.util.UUID;
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,value=Dist.CLIENT)
public final class ClientSmokeTest {
 private static boolean opened,setup;
 private static int worldTicks,titleTicks,frames;
 private static volatile UUID testEntity;
 private static float minLeg=Float.MAX_VALUE,maxLeg=-Float.MAX_VALUE,minBody=Float.MAX_VALUE,maxBody=-Float.MAX_VALUE;
 private static int direction=1;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
  if(!Boolean.getBoolean("minemon.clientSmokeTest")||event.phase!=TickEvent.Phase.END)return;
  Minecraft mc=Minecraft.getInstance();
  try {
   if(!opened && mc.screen instanceof TitleScreen && ++titleTicks==20) {
    for(var d:SpeciesDefinitions.ALL.values()) {
     var geo=new ResourceLocation("minemon",d.model().substring("assets/minemon/".length()));var anim=new ResourceLocation("minemon",d.animation().substring("assets/minemon/".length()));
     if(!GeckoLibCache.getInstance().getGeoModels().containsKey(geo)||!GeckoLibCache.getInstance().getAnimations().containsKey(anim))throw new IllegalStateException("GeckoLib did not parse "+d.slug());
    }
    Files.writeString(Path.of("client-validation.txt"),"PASS MENU: Minecraft reached TitleScreen; every registered model/animation parsed by GeckoLib.\n");
    opened=true;
    var dataFile=Path.of("saves/minemon-validation/level.dat").toFile();var tag=net.minecraft.nbt.NbtIo.readCompressed(dataFile);tag.getCompound("Data").putBoolean("confirmedExperimentalSettings",true);net.minecraft.nbt.NbtIo.writeCompressed(tag,dataFile);
    Object flows=Minecraft.class.getMethod("createWorldOpenFlows").invoke(mc);boolean invoked=false;
    for(var method:flows.getClass().getMethods())if(method.getName().equals("loadLevel")) {
     if(method.getParameterCount()==2&&method.getParameterTypes()[1]==String.class){method.invoke(flows,mc.screen,"minemon-validation");invoked=true;break;}
     if(method.getParameterCount()==1&&method.getParameterTypes()[0]==String.class){method.invoke(flows,"minemon-validation");invoked=true;break;}
    }
    if(!invoked)throw new IllegalStateException("Could not locate WorldOpenFlows.loadLevel");
    MinemonRenderer.validationObserver=(entity,model)->{
     if(testEntity==null||!entity.getUUID().equals(testEntity)||worldTicks<110||worldTicks>220)return;
     var leg=model.getAnimationProcessor().getBone("leg_front_right");var body=model.getAnimationProcessor().getBone("body");
     minLeg=Math.min(minLeg,leg.getRotationX());maxLeg=Math.max(maxLeg,leg.getRotationX());minBody=Math.min(minBody,body.getPositionY());maxBody=Math.max(maxBody,body.getPositionY());frames++;
    };
   }
   if(mc.level==null||mc.player==null||mc.getSingleplayerServer()==null)return;
   worldTicks++;
   if(!setup) {
    setup=true;mc.getSingleplayerServer().execute(()->{
     var level=mc.getSingleplayerServer().overworld();level.setDayTime(1000);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,mc.getSingleplayerServer());
     for(int x=-8;x<=8;x++)for(int z=-5;z<=12;z++)level.setBlockAndUpdate(new BlockPos(x,100,z),Blocks.STONE.defaultBlockState());
     var player=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);player.setGameMode(GameType.CREATIVE);player.teleportTo(level,0,101,0,0,12);
     var b=ModEntities.BULBASAUR.get().create(level);b.moveTo(0,101,4,180,0);b.tame(player);b.setOrderedToSit(false);b.setNoAi(true);level.addFreshEntity(b);testEntity=b.getUUID();
    });
   }
   if(worldTicks==70||worldTicks==170)Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
   if(worldTicks>=100&&worldTicks<=220)mc.getSingleplayerServer().execute(()->{
    var b=(MinemonCreature)mc.getSingleplayerServer().overworld().getEntity(testEntity);if(b!=null){b.setNoAi(false);if(b.getX()>2.5)direction=-1;if(b.getX()< -2.5)direction=1;b.getNavigation().moveTo(direction*3,101,4,1);}
   });
   if(worldTicks==250){
    if(frames<10||maxLeg-minLeg<0.1F||maxBody-minBody<0.01F)throw new IllegalStateException("Rendered body animation did not advance: frames="+frames+" leg="+(maxLeg-minLeg)+" body="+(maxBody-minBody));
    Files.writeString(Path.of("client-validation.txt"),"PASS WORLD: Bulbasaur rendered in a Minecraft world; frames="+frames+" leg rotation range="+(maxLeg-minLeg)+" body position range="+(maxBody-minBody)+"\n",StandardOpenOption.APPEND);
    mc.stop();
   }
  }catch(Exception e){throw new IllegalStateException("Minemon client validation failed",e);}
 }
}
