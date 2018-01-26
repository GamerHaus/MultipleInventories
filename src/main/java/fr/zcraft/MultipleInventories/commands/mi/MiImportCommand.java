package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.MultipleInventories.importers.ImportProcess;
import fr.zcraft.MultipleInventories.importers.MultiInvImporter;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.components.i18n.I;
import org.bukkit.command.CommandSender;


@CommandInfo (name = "import")
public final class MiImportCommand extends Command
{
    @Override
    protected void run()
    {
        if (args.length == 0 || !args[0].equalsIgnoreCase("confirm"))
        {
            sender.sendMessage(I.t("{gold}{bold}This importer will import data from MultiInv."));
            sender.sendMessage(I.t("{red}{bold}IMPORTANT.{red} Before processing, please check that you configured this plugin (MultipleInventories) like MultiInv, with the same groups names exactly (this importer will NOT copy the configuration and will use the MultiInv groups to import data)."));
            sender.sendMessage(I.t("At the beginning of the process, all players (including you!) will be kicked, and the server will be force-whitelisted during this import."));
            sender.sendMessage(I.t("When the import will be completed, remove MultiInv, as having both MultipleInventories and MultiInv will cause conflicts."));
            sender.sendMessage("");
            sender.sendMessage(I.t("Please also note that this importer will only read the MultiInv data, but never write anything on the MultiInv side."));
            sender.sendMessage("");
            sender.sendMessage(I.t("If you are ready, execute {cc}{0}{reset}.", build("confirm")));
        }
        else
        {
            new ImportProcess(new MultiInvImporter(), sender).begin();
        }
    }

    @Override
    public boolean canExecute(CommandSender sender)
    {
        return Permissions.IMPORT.grantedTo(sender);
    }
}
