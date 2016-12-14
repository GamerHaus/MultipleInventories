package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandException;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.components.rawtext.RawText;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Set;


@CommandInfo (name = "list", usageParameters = "")
public final class MiListCommand extends Command
{
    @Override
    protected void run() throws CommandException
    {
        Map<String, Set<String>> groups = MultipleInventories.get().getPlayersManager().getWorldsGroups();

        send(new RawText("").then(I.t("World groups")).style(ChatColor.GREEN, ChatColor.BOLD).then(" (" + groups.size() + ")").color(ChatColor.GRAY).build());

        for (Map.Entry<String, Set<String>> entry : groups.entrySet())
        {
            send(new RawText("")
                    .then("- ").color(ChatColor.GRAY)
                    .then(entry.getKey()).color(ChatColor.DARK_GREEN)
                     /// Separator between group name and worlds list in /mi list
                    .then(I.t(": ")).color(ChatColor.GRAY)
                    .then(StringUtils.join(entry.getValue(), ", ")).color(ChatColor.WHITE)
                    .build()
            );
        }
    }

    @Override
    public boolean canExecute(CommandSender sender)
    {
        return Permissions.LIST.grantedTo(sender);
    }
}
