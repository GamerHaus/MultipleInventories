package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandException;
import fr.zcraft.zlib.components.commands.CommandInfo;

import java.util.List;


@CommandInfo (name = "import", usageParameters = "")
public final class MiImportCommand extends Command
{
    @Override
    protected void run() throws CommandException
    {
        // TODO implement command /mi import
    }

    @Override
    protected List<String> complete() throws CommandException
    {
        // TODO implement auto-completion for /mi import
        return null;
    }
}
