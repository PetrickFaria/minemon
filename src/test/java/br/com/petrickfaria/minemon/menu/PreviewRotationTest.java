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
 @Test public void approvedScaleIsRestoredAndPivotUsesTheBodyCoordinateFrame()throws Exception{
  var root=java.nio.file.Path.of(System.getProperty("minemon.testResources"));
  var bounds=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("data/minemon_gen1/preview_bounds.json"))).getAsJsonObject();
  var pivots=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(root.resolve("data/minemon_gen1/preview_body_pivots.json"))).getAsJsonObject();
  assertEquals(151,pivots.size());
  for(var d:SpeciesDefinitions.ALL.values()){
   var a=bounds.getAsJsonArray(d.slug());double w=a.get(3).getAsDouble()-a.get(0).getAsDouble(),h=a.get(4).getAsDouble()-a.get(1).getAsDouble(),depth=a.get(5).getAsDouble()-a.get(2).getAsDouble();
   float approved=(float)Math.min(100,Math.min(232/Math.max(.2,(w+depth*Math.sin(Math.toRadians(4)))*d.scale()*1.3),88/Math.max(.2,(h*Math.cos(Math.toRadians(6))+depth*Math.sin(Math.toRadians(6)))*d.scale()*1.3)));
   var fit=PreviewBounds.fit(d.slug(),d.scale());assertEquals(d.slug(),approved,fit.scale(),0);
   var c=pivots.getAsJsonObject(d.slug()).getAsJsonArray("center");
   assertEquals(-c.get(0).getAsDouble()*d.scale(),fit.x(),0);assertEquals(c.get(1).getAsDouble()*d.scale(),fit.y(),0);assertEquals(c.get(2).getAsDouble()*d.scale(),fit.z(),0);
  }
 }

}
