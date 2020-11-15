package fr.zcraft.MultipleInventories;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.zcraft.MultipleInventories.commands.mi.MiImportCommand;
import fr.zcraft.MultipleInventories.commands.mi.MiListCommand;
import fr.zcraft.MultipleInventories.commands.mi.MiReloadCommand;
import fr.zcraft.MultipleInventories.players.PlayersManager;
import fr.zcraft.MultipleInventories.snaphots.SnapshotsIO;
import fr.zcraft.quartzlib.core.QuartzPlugin;
import fr.zcraft.quartzlib.components.commands.Commands;
import fr.zcraft.quartzlib.components.i18n.I18n;
import org.bukkit.event.Listener;


public final class MultipleInventories extends QuartzPlugin implements Listener
{
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private static MultipleInventories instance;

    private PlayersManager playersManager = null;

    @Override
    public void onEnable()
    {
        instance = this;

        saveDefaultConfig();

        loadComponents(Commands.class, Config.class, SnapshotsIO.class, I18n.class);

        I18n.setPrimaryLocale(Config.LOCALE.get());

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
