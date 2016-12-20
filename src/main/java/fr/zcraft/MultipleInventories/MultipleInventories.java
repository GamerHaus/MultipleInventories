package fr.zcraft.MultipleInventories;

import fr.zcraft.MultipleInventories.commands.mi.MiImportCommand;
import fr.zcraft.MultipleInventories.commands.mi.MiListCommand;
import fr.zcraft.MultipleInventories.commands.mi.MiReloadCommand;
import fr.zcraft.MultipleInventories.players.PlayersManager;
import fr.zcraft.MultipleInventories.snaphots.SnapshotsIO;
import fr.zcraft.zlib.components.commands.Commands;
import fr.zcraft.zlib.components.i18n.I18n;
import fr.zcraft.zlib.core.ZPlugin;
import org.bukkit.event.Listener;


public final class MultipleInventories extends ZPlugin implements Listener
{
    private static MultipleInventories instance;

    private PlayersManager playersManager = null;

    @Override
    public void onEnable()
    {
        instance = this;

        saveDefaultConfig();

        loadComponents(Commands.class, Config.class, SnapshotsIO.class, I18n.class);

        playersManager = loadComponent(PlayersManager.class);

        // noinspection unchecked
        Commands.register("mi", MiListCommand.class, MiReloadCommand.class, MiImportCommand.class);
    }

    public static MultipleInventories get()
    {
        return instance;
    }

    public PlayersManager getPlayersManager()
    {
        return playersManager;
    }
}
