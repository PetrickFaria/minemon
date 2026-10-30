package br.com.petrickfaria.minemon.registry;
import br.com.petrickfaria.minemon.Minemon;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.*;
public final class ModRecipes {
 public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,Minemon.MOD_ID);
 public static final RegistryObject<RecipeSerializer<br.com.petrickfaria.minemon.item.EmptyBallUpgradeRecipe>> EMPTY_BALL_UPGRADE=SERIALIZERS.register("empty_ball_upgrade",br.com.petrickfaria.minemon.item.EmptyBallUpgradeRecipe.Serializer::new);
 private ModRecipes(){}
}
