package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Messages;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;

@DefaultQualifier(NonNull.class)
public final class LinkCommand extends SquaremapCommand {
    @Inject
    private LinkCommand(final Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("link")
                .commandDescription(richDescription(Messages.LINK_COMMAND_DESCRIPTION))
                .permission("squaremap.command.link")
                .handler(this::showLink));
    }

    private void showLink(final CommandContext<Commander> ctx) {
        final Map<String, String> params = new HashMap<>();
        if (ctx.sender() instanceof PlayerCommander player) {
            params.put("uuid", player.get(Identity.UUID).orElseThrow().toString().replace("-", ""));
        }
        final StringBuilder link = new StringBuilder(Config.WEB_ADDRESS);
        boolean first = link.toString().indexOf('?') == -1;
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                link.append('?');
                first = false;
            } else {
                link.append('&');
            }
            link.append(entry.getKey());
            link.append('=');
            link.append(entry.getValue());
        }
        ctx.sender().sendMessage(Messages.LINK_COMMAND_LINK_FORMAT.withPlaceholders(Placeholder.parsed("link", link.toString())));
    }
}
