package fr.zcraft.MultipleInventories.commands.mi;

import fr.zcraft.MultipleInventories.Config;
import fr.zcraft.MultipleInventories.MultipleInventories;
import fr.zcraft.MultipleInventories.Permissions;
import fr.zcraft.MultipleInventories.quartzlib.components.commands.Command;
import fr.zcraft.MultipleInventories.quartzlib.components.commands.CommandInfo;
import fr.zcraft.MultipleInventories.quartzlib.components.i18n.I;
import fr.zcraft.MultipleInventories.quartzlib.components.rawtext.RawText;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


@CommandInfo(name = "list")
public final class MiListCommand extends Command
{
    @Override
    protected void run()
    {
        final Map<String, Set<String>> groups = new TreeMap<>((s1, s2) ->
        {
            if (s1.equals(s2))
                return 0;
            else if (s1.equals("default"))
                return 1;
            else if (s2.equals("default"))
                return -1;
            else return s1.compareTo(s2);
        });

        groups.putAll(MultipleInventories.get().getPlayersManager().getWorldsGroups());

        if (sender instanceof Player) info("");

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
                .then(StringUtils.join(entry.getValue(), ChatColor.GRAY + ", " + ChatColor.RESET))
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
