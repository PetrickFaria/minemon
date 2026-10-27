package br.com.petrickfaria.minemon.pokec;
import net.minecraft.world.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
public final class SoulItem extends Item {
    private final SoulType type;
    public SoulItem(SoulType type,Properties properties){super(properties);this.type=type;}
    public SoulType soulType(){return type;}
    @Override public Component getName(ItemStack stack){
        String name=switch(type){case NORMAL->"Alma Normal";case FIRE->"Alma de Fogo";case WATER->"Alma de Água";case GRASS->"Alma de Planta";case ELECTRIC->"Alma Elétrica";case ICE->"Alma de Gelo";case FIGHTING->"Alma de Lutador";case POISON->"Alma de Veneno";case GROUND->"Alma de Terra";case FLYING->"Alma Voadora";case PSYCHIC->"Alma Psíquica";case BUG->"Alma de Inseto";case ROCK->"Alma de Pedra";case GHOST->"Alma Fantasma";case DRAGON->"Alma de Dragão";};
        return Component.literal(name);
    }
}
