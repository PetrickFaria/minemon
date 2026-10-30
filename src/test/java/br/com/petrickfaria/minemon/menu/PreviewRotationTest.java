package br.com.petrickfaria.minemon.menu;
import org.junit.Test;
import static org.junit.Assert.*;
import br.com.petrickfaria.minemon.entity.SpeciesDefinitions;
public class PreviewRotationTest {
 @Test public void all151PreviewsKeepOneScaleAtEveryAngle(){
  for(var d:SpeciesDefinitions.ALL.values()){
   var fit=PreviewBounds.fit(d.slug(),d.scale());
   assertTrue(d.slug(),fit.scale()>0);
   for(int yaw=0;yaw<360;yaw+=15)for(int pitch=-46;pitch<=34;pitch+=10)
    assertEquals(d.slug(),fit.scale(),PreviewBounds.rotationScale(d.slug(),d.scale(),fit.scale(),yaw,pitch),0);
  }
 }
}
