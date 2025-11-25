package xyz.jpenilla.squaremap.fabric;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.component.ComponentV3;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;

@DefaultQualifier(NonNull.class)
public class SquaremapComponentInitializer implements EntityComponentInitializer {
    public static final ComponentKey<PlayerComponent> SQUAREMAP_PLAYER_COMPONENT =
        ComponentRegistryV3.INSTANCE.getOrCreate(Identifier.parse("squaremap:player_component"), PlayerComponent.class);

    @Override
    public void registerEntityComponentFactories(final EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(SQUAREMAP_PLAYER_COMPONENT, player -> new PlayerComponentImpl(), RespawnCopyStrategy.ALWAYS_COPY);
    }

    public interface PlayerComponent extends ComponentV3 {
        boolean hidden();

        void hidden(boolean hidden);
    }

    private static final class PlayerComponentImpl implements PlayerComponent {
        private static final String HIDDEN_KEY = "hidden";

        private boolean hidden;

        @Override
        public void readData(final ValueInput output) {
            this.hidden = output.getBooleanOr(HIDDEN_KEY, false);
        }

        @Override
        public void writeData(final ValueOutput input) {
            input.putBoolean(HIDDEN_KEY, this.hidden);
        }

        @Override
        public boolean hidden() {
            return this.hidden;
        }

        @Override
        public void hidden(final boolean hidden) {
            this.hidden = hidden;
        }
    }
}
