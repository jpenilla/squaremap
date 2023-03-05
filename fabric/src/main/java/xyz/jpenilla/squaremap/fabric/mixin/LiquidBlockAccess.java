package xyz.jpenilla.squaremap.fabric.mixin;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LiquidBlock.class)
public interface LiquidBlockAccess {
    @Accessor("fluid")
    FlowingFluid fluid();
}
