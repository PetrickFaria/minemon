package br.com.petrickfaria.minemon;
import br.com.petrickfaria.minemon.entity.*;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
public class RefinementRulesTest {
 @Test public void rarityBaseAndLevelGrowthUseHeartConversionExactly(){
  String[] tiers={"common","uncommon","rare","legendary","mythic"};double[] hp={10,14,18,22,30},damage={4,6,8,10,14};
  for(int i=0;i<tiers.length;i++)for(int n:new int[]{1,2,5,10,20,100}){
   assertEquals(hp[i]+2*(n-1),PokemonProgression.healthForRarity(tiers[i],n),0);
   assertEquals(damage[i]+n-1,PokemonProgression.damageForRarity(tiers[i],n),0);
  }
 }
 @Test public void dualTypeChoiceHasExactlyTwoEqualBranches(){
  assertEquals("fire",EggAppearance.choose(List.of("fire","flying"),false));
  assertEquals("flying",EggAppearance.choose(List.of("fire","flying"),true));
  assertEquals("water",EggAppearance.choose(List.of("water"),true));
 }
 @Test public void perSpeciesMeasurementsAreFiniteAndKeepMeshScaleIndependent(){
  for(var d:SpeciesDefinitions.ALL.values()){
   assertTrue(d.slug(),ModelMetrics.width(d.slug(),d.scale())>.05);
   assertTrue(d.slug(),ModelMetrics.height(d.slug(),d.scale())>.05);
   assertTrue(d.slug(),Double.isFinite(ModelMetrics.top(d.slug(),d.scale())));
  }
  var gastly=SpeciesDefinitions.get("gastly");
  assertTrue(ModelMetrics.width("gastly",gastly.scale())>gastly.width()*2);
 }
}
