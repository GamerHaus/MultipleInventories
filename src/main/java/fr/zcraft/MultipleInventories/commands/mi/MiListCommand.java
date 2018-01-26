package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.Config;
import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.zlib.components.commands.Command;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.components.rawtext.RawText;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.Set;


@CommandInfo (name = "list")
public final class MiListCommand extends Command
{
    @Override
    protected void run()
    {
        final Map<String, Set<String>> groups = MultipleInventories.get().getPlayersManager().getWorldsGroups();

        send(
                new RawText("")
                        .then(I.t("World groups"))
                            .style(ChatColor.GREEN, ChatColor.BOLD)
                        .then(" (" + groups.size() + ")")
                            .color(ChatColor.GRAY)
                .build()
        );

        groups
            .entrySet().stream()
            .map(entry -> new RawText("")
                .then("- ")
                    .color(ChatColor.GRAY)
                .then(entry.getKey())
                    .color(ChatColor.DARK_GREEN)
                 /// Separator between group name and worlds list in /mi list
                .then(I.t(": "))
                    .color(ChatColor.GRAY)
                .then(StringUtils.join(entry.getValue(), ", "))
                    .color(ChatColor.WHITE)
                .build())
            .forEach(this::send);

        info("");

        if (Config.PER_GAMEMODE_INVENTORIES.get())
        {
            info(I.t("Per-gamemode inventories are {green}enabled{gray}."));
        }
        else
        {
            info(I.t("Per-gamemode inventories are {red}disabled{gray}."));
        }
    }

    @Override
    public boolean canExecute(CommandSender sender)
    {
        return Permissions.LIST.grantedTo(sender);
    }
}
