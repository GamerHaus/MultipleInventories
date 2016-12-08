package fr.zcraft.MultipleInventories;

import fr.zcraft.zlib.core.ZPlugin;
import fr.zcraft.zlib.components.commands.Commands;
import fr.zcraft.MultipleInventories.commands.mi.MiListCommand;
import fr.zcraft.MultipleInventories.commands.mi.MiLinkCommand;
import fr.zcraft.MultipleInventories.commands.mi.MiImportCommand;


public final class MultipleInventories extends ZPlugin
{
    private static MultipleInventories instance;

    @Override
    public void onEnable()
    {
        instance = this;

        loadComponents(Commands.class);
        
        Commands.register("mi", MiListCommand.class, MiLinkCommand.class, MiImportCommand.class);
    }

    public static MultipleInventories get()
    {
        return instance;
    }
}
