package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.snaphots.PlayerSnapshot;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandException;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.tools.PluginLogger;

import java.util.List;


@CommandInfo (name = "link", usageParameters = "")
public final class MiLinkCommand extends Command
{
    @Override
    protected void run() throws CommandException
    {
        PluginLogger.info(PlayerSnapshot.snap(playerSender()).toJSONString());
    }

    @Override
    protected List<String> complete() throws CommandException
    {
        // TODO implement auto-completion for /mi link
        return null;
    }
}
