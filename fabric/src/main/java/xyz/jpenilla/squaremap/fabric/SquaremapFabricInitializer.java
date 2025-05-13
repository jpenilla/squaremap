package xyz.jpenilla.squaremap.fabric;

import net.fabricmc.api.ModInitializer;

public final class SquaremapFabricInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        new SquaremapFabric();
    }
}
