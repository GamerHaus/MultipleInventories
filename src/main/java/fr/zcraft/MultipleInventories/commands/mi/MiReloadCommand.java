package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.Config;
import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.components.i18n.I;
import org.bukkit.command.CommandSender;


@CommandInfo (name = "reload")
public final class MiReloadCommand extends Command
{
    @Override
    protected void run()
    {
        Config.reload();
        MultipleInventories.get().getPlayersManager().loadWorldsGroups();
        success(I.t("{0} configuration reloaded.", MultipleInventories.get().getDescription().getName()));
    }

    @Override
    public boolean canExecute(CommandSender sender)
    {
        return Permissions.RELOAD.grantedTo(sender);
    }
}
