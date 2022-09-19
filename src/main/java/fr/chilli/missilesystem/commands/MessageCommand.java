package fr.chilli.missilesystem.commands;

import fr.chilli.missilesystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MessageCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.getInstance().getConfig().getString("messages.not_player", ""));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Main.getInstance().getConfig().getString("messages.usage_message", ""));
            return true;
        }

        String type = args[0];
        if (!type.equalsIgnoreCase("launch") && !type.equalsIgnoreCase("impact")) {
            sender.sendMessage(Main.getInstance().getConfig().getString("messages.error_unknown_type", ""));
            return true;
        }
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < args.length; i++) {
            builder.append(args[i]);
            builder.append(" ");
        }

        saveMessage(player, type, builder.toString());
        sender.sendMessage(Main.getInstance().getConfig().getString("messages.message_saved", "")
                                   .replaceAll("%message%", builder.toString()));
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> launch = new ArrayList<>(List.of("launch", "impact"));
            launch.removeIf(s -> !s.toLowerCase().startsWith(args[0].toLowerCase()));

            return launch;
        }

        List<String> strings = new ArrayList<>(List.of("%player%", "%missile%"));

        strings.removeIf(s ->
                                 !s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())
                                         || List.of(args).contains(s));

        return strings;
    }

    private static void saveMessage(Player player, String type, String message) {
        Main.getInstance().MESSAGES.set(player.getUniqueId() + "." + type.toLowerCase(), ChatColor.translateAlternateColorCodes('&', message));
        try{
            Main.getInstance().MESSAGES.save(Main.getInstance().MESSAGES_FILE);
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

}
