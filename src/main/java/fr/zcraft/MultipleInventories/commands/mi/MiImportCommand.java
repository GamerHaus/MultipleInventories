package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.MultipleInventories.importers.ImportProcess;
import fr.zcraft.MultipleInventories.importers.MultiInvImporter;
import fr.zcraft.quartzlib.components.commands.Command;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.commands.WithFlags;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.rawtext.RawText;
import org.bukkit.command.CommandSender;


@CommandInfo (name = "import", usageParameters = "[--confirm]")
@WithFlags({"confirm"})
public final class MiImportCommand extends Command
{
    @Override
    protected void run()
    {
        if (!hasFlag("confirm"))
        {
            sender.sendMessage(I.t("{gold}{bold}This importer will import data from MultiInv."));
            sender.sendMessage(I.t("{red}{bold}IMPORTANT.{red} Before processing, please check that you configured this plugin (MultipleInventories) like MultiInv, with the same groups names exactly (this importer will NOT copy the configuration and will use the MultiInv groups to import data)."));
            sender.sendMessage(I.t("At the beginning of the process, all players (including you!) will be kicked, and the server will be force-whitelisted during this import."));
            sender.sendMessage(I.t("When the import will be completed, remove MultiInv, as having both MultipleInventories and MultiInv will cause conflicts."));
            sender.sendMessage("");
            sender.sendMessage(I.t("Please also note that this importer will only read the MultiInv data, but never write anything on the MultiInv side."));
            sender.sendMessage("");
            send(
                    new RawText(I.t("If you are ready, execute {cc}{0}{reset}.", build("--confirm")))
                        .hover(new RawText(I.t("Click here to execute\n{cc}{0}", build("--confirm"))))
                        .command(getClass(), "--confirm")
            );
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
