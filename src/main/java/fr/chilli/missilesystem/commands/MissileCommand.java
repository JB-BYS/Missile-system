package fr.chilli.missilesystem.commands;

import fr.chilli.missilesystem.Main;
import fr.chilli.missilesystem.inventory.EInventory;
import fr.chilli.missilesystem.missile.Missile;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MissileCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (give(sender, args)
                || take(sender, args)
                || reset(sender, args)) {
            return true;
        }

        if (sender instanceof Player player)
            player.openInventory(EInventory.MISSILE_SHOP.getInventory());
        return true;
    }

    private static boolean give(CommandSender player, String[] args) {
        if (player.isOp() && args.length > 1) {
            if (args[0].equalsIgnoreCase("give")) {
                if (args.length > 2) {
                    Player target = player.getServer().getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(Main.getInstance().getConfig().getString("messages.player_not_found", ""));
                        return true;
                    }
                    String missileName = args[2];
                    Missile missile = Missile.getInstanceByIntern(missileName);
                    if (missile == null) {
                        player.sendMessage(Main.getInstance().getConfig().getString("messages.missile_not_found", ""));
                        return true;
                    }
                    int amount = 1;
                    if (args.length > 3) {
                        try{
                            amount = Integer.parseInt(args[3]);
                        }catch(NumberFormatException e){
                            player.sendMessage(Main.getInstance().getConfig().getString("messages.amount_not_valid", ""));
                            return true;
                        }
                    }
                    ItemStack itemStack = missile.getItemStack();
                    itemStack.setAmount(amount);
                    target.getInventory().addItem(itemStack);
                    player.sendMessage(Main.getInstance().getConfig().getString("messages.given_confirm_message", "")
                                               .replaceAll("%amount%", String.valueOf(amount))
                                               .replaceAll("%missile%", missileName)
                                               .replaceAll("%player%", target.getName()));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean take(CommandSender player, String[] args) {
        if (player.isOp() && args.length > 1) {
            if (args[0].equalsIgnoreCase("take")) {
                if (args.length > 2) {
                    Player target = player.getServer().getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage(Main.getInstance().getConfig().getString("messages.player_not_found", ""));
                        return true;
                    }
                    String missileName = args[2];
                    Missile missile = Missile.getInstanceByIntern(missileName);
                    if (missile == null) {
                        player.sendMessage(Main.getInstance().getConfig().getString("messages.missile_not_found", ""));
                        return true;
                    }
                    PlayerInventory inventory = target.getInventory();
                    List<ItemStack> toRemove = new ArrayList<>();

                    int amount = 0;

                    for (ItemStack itemStack : inventory.getContents()) {
                        if (Missile.getInstance(itemStack) == missile) {
                            toRemove.add(itemStack);
                            amount += itemStack.getAmount();
                        }
                    }
                    toRemove.forEach(inventory::remove);

                    player.sendMessage(
                            Main.getInstance().getConfig().getString("messages.taken_confirm_message", "")
                                    .replaceAll("%amount%", String.valueOf(amount))
                                    .replaceAll("%player%", player.getName()));
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean reset(CommandSender player, String[] args) {
        if (player.isOp() && args.length > 1) {
            if (args[0].equalsIgnoreCase("reset")) {
                Player target = player.getServer().getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(Main.getInstance().getConfig().getString("messages.player_not_found", ""));
                    return true;
                }

                PlayerInventory inventory = target.getInventory();
                List<ItemStack> toRemove = new ArrayList<>();

                int amount = 0;

                for (ItemStack itemStack : inventory.getContents()) {
                    if (Missile.getInstance(itemStack) != null) {
                        toRemove.add(itemStack);
                        amount += itemStack.getAmount();
                    }
                }

                toRemove.forEach(inventory::remove);

                player.sendMessage(
                        Main.getInstance().getConfig().getString("messages.taken_confirm_message", "")
                                .replaceAll("%amount%", String.valueOf(amount))
                                .replaceAll("%player%", target.getName()));
                //ChatColor.GREEN + "Taken " + amount + " Missiles from " + target.getName());
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp())
            return Collections.emptyList();

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("give")) {
                if (args.length == 2) {
                    return getPlayers(sender, args[1]);
                } else if (args.length == 3) {
                    return getMissiles(args[2]);
                } else {
                    return Collections.emptyList();
                }
            } else if (args[0].equalsIgnoreCase("take")) {
                if (args.length == 2) {
                    return getPlayers(sender, args[1]);
                } else if (args.length == 3) {
                    return getMissiles(args[2]);
                } else {
                    return Collections.emptyList();
                }
            } else if (args[0].equalsIgnoreCase("reset")) {
                if (args.length == 2) {
                    return getPlayers(sender, args[1]);
                } else {
                    return Collections.emptyList();
                }
            }

            if (args.length == 1) {
                return List.of("give", "take", "reset");
            }

        }
        return Collections.emptyList();
    }

    private List<String> getMissiles(String arg) {
        List<String> missiles = new ArrayList<>();
        for (Missile missile : Main.MISSILES) {
            if (missile.getDisplayName().toLowerCase().startsWith(arg.toLowerCase())) {
                missiles.add(missile.getIntern());
            }
        }
        return missiles;
    }

    private List<String> getPlayers(CommandSender sender, String arg) {
        List<String> players = new ArrayList<>();
        for (Player player : sender.getServer().getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(arg.toLowerCase())) {
                players.add(player.getName());
            }
        }
        return players;
    }

}
