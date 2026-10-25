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
   assertNotNull(d.slug(),sleep);assertTrue(d.slug(),sleep.get("loop").getAsBoolean());
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
