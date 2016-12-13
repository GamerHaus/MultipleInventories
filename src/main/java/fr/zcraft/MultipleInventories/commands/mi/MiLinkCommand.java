package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.snaphots.PlayerSnapshot;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandException;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.tools.PluginLogger;
import fr.zcraft.zlib.tools.runners.RunTask;
import org.bukkit.entity.Player;

import java.util.List;


@CommandInfo (name = "link", usageParameters = "")
public final class MiLinkCommand extends Command
{
    @Override
    protected void run() throws CommandException
    {
        final Player player = playerSender();
        final PlayerSnapshot snapshot = PlayerSnapshot.snap(player);
        final String jsonDump = snapshot.toJSONString();

        PluginLogger.info(jsonDump);

        player.getInventory().clear();
        player.getEnderChest().clear();

        RunTask.later(new Runnable() {
            @Override
            public void run()
            {
                PlayerSnapshot.fromJSONString(jsonDump).reconstruct(player);
            }
        }, 20l);
    }

    @Override
    protected List<String> complete() throws CommandException
    {
        // TODO implement auto-completion for /mi link
        return null;
    }
}
