package br.com.petrickfaria.minemon.gametest;
import br.com.petrickfaria.minemon.Minemon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib3.resource.GeckoLibCache;
import java.nio.file.Files;
import java.nio.file.Path;
@Mod.EventBusSubscriber(modid=Minemon.MOD_ID,value=Dist.CLIENT)
public final class ClientSmokeTest {
 private static int titleTicks;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
  if (!Boolean.getBoolean("minemon.clientSmokeTest")||event.phase!=TickEvent.Phase.END) return;
  Minecraft mc=Minecraft.getInstance();
  if (!(mc.screen instanceof TitleScreen)) return;
  titleTicks++;
  if(titleTicks==20) {
   try {
    for(var d:br.com.petrickfaria.minemon.entity.SpeciesDefinitions.ALL.values()) {
     ResourceLocation geo=new ResourceLocation("minemon",d.model().substring("assets/minemon/".length()));
     ResourceLocation anim=new ResourceLocation("minemon",d.animation().substring("assets/minemon/".length()));
     if(!GeckoLibCache.getInstance().getGeoModels().containsKey(geo)||!GeckoLibCache.getInstance().getAnimations().containsKey(anim)) throw new IllegalStateException("Resources not parsed by GeckoLib: "+d.slug());
    }
    Files.writeString(Path.of("client-validation.txt"),"PASS: Minecraft reached TitleScreen; GeckoLib parsed every registered species geometry and animations.\n");
    Screenshot.grab(mc.gameDirectory,mc.getMainRenderTarget(),message->{});
   } catch(Exception e) { throw new IllegalStateException("Minemon client smoke test failed",e); }
  }
  if(titleTicks==60) mc.stop();
 }
}
