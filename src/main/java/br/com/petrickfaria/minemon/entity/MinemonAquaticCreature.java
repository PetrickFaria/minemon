package br.com.petrickfaria.minemon.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

/** Water and shore navigation without fish-style dry-land damage. */
public final class MinemonAquaticCreature extends MinemonCreature {
    public MinemonAquaticCreature(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.1F, 0.5F, false);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }
}
