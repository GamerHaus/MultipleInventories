package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandException;
import fr.zcraft.zlib.components.commands.CommandInfo;
import org.bukkit.command.CommandSender;


@CommandInfo (name = "import", usageParameters = "")
public final class MiImportCommand extends Command
{
    @Override
    protected void run() throws CommandException
    {
        // TODO implement command /mi import
    }

    @Override
    public boolean canExecute(CommandSender sender)
    {
        return Permissions.IMPORT.grantedTo(sender);
    }
}
