package br.com.petrickfaria.minemon;
import br.com.petrickfaria.minemon.entity.*;
import com.google.gson.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class NightSleepAssetTest {
 @Test public void allSpeciesHaveLoopingSleepWithOriginalBlinkBones() throws Exception {
  var root=Path.of(System.getProperty("minemon.testResources"));
  for(var d:SpeciesDefinitions.ALL.values()){
   var file=root.resolve(d.animation().replace("assets/minemon/","assets/minemon_gen1/"));
   var animations=JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("animations");
   var sleep=animations.getAsJsonObject(d.clips().get("night_sleep"));
   assertNotNull(d.slug(),sleep);
   if(d.slug().equals("growlithe")){
    assertEquals(.1,sleep.getAsJsonObject("bones").getAsJsonObject("eyelid_left").getAsJsonArray("position").get(0).getAsDouble(),.0001);
    assertEquals(-.1,sleep.getAsJsonObject("bones").getAsJsonObject("eyelid_right").getAsJsonArray("position").get(0).getAsDouble(),.0001);
   }
   if(d.slug().equals("haunter"))for(String side:java.util.List.of("left","right")){
    assertEquals(-.15,sleep.getAsJsonObject("bones").getAsJsonObject("closed_eyelid_"+side).getAsJsonArray("position").get(2).getAsDouble(),.0001);
    assertEquals(.2,sleep.getAsJsonObject("bones").getAsJsonObject("pupil_"+side).getAsJsonArray("position").get(2).getAsDouble(),.0001);
   }assertTrue(d.slug(),sleep.get("loop").getAsBoolean());
   assertFalse(d.slug(),sleep.toString().matches("(?s).*\\b(?:q|query)\\.r\\..*"));
   String blinkName=d.clips().get("blink");
   if(blinkName!=null&&animations.has(blinkName)){
    var blink=animations.getAsJsonObject(blinkName).getAsJsonObject("bones");
    if(blink!=null)for(var bone:blink.keySet()){
     assertTrue(d.slug()+" holds native eye bone "+bone,sleep.getAsJsonObject("bones").has(bone));
     for(var channel:sleep.getAsJsonObject("bones").getAsJsonObject(bone).entrySet())
      assertFalse(d.slug()+" closed eye must hold rather than reopen",channel.getValue().isJsonObject());
    }
   }
  }
 }
}
