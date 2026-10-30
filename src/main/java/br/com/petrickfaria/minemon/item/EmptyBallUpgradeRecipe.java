package br.com.petrickfaria.minemon.item;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.*;
/** Vanilla shaped matching plus an exact, tag-free empty Pokébola constraint. */
public final class EmptyBallUpgradeRecipe extends ShapedRecipe {
 public EmptyBallUpgradeRecipe(ShapedRecipe r){super(r.getId(),r.getGroup(),r.getWidth(),r.getHeight(),r.getIngredients(),r.getResultItem());}
 @Override public boolean matches(CraftingContainer grid,net.minecraft.world.level.Level level){
  if(!super.matches(grid,level))return false;
  for(int i=0;i<grid.getContainerSize();i++){
   var stack=grid.getItem(i);
   if(stack.is(br.com.petrickfaria.minemon.registry.ModItems.POKEBALL.get()) && (PokeballItem.isOccupied(stack)||stack.hasTag()))return false;
  }return true;
 }
 @Override public RecipeSerializer<?> getSerializer(){return br.com.petrickfaria.minemon.registry.ModRecipes.EMPTY_BALL_UPGRADE.get();}
 public static final class Serializer implements RecipeSerializer<EmptyBallUpgradeRecipe>{
  private final ShapedRecipe.Serializer vanilla=new ShapedRecipe.Serializer();
  @Override public EmptyBallUpgradeRecipe fromJson(ResourceLocation id,com.google.gson.JsonObject json){return new EmptyBallUpgradeRecipe(vanilla.fromJson(id,json));}
  @Override public EmptyBallUpgradeRecipe fromNetwork(ResourceLocation id,net.minecraft.network.FriendlyByteBuf data){return new EmptyBallUpgradeRecipe(vanilla.fromNetwork(id,data));}
  @Override public void toNetwork(net.minecraft.network.FriendlyByteBuf data,EmptyBallUpgradeRecipe recipe){vanilla.toNetwork(data,recipe);}
 }
}
