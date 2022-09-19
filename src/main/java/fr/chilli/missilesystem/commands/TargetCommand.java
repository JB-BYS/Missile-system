package fr.chilli.missilesystem.commands;

import fr.chilli.missilesystem.Main;
import fr.chilli.missilesystem.events.MissileEvents;
import fr.chilli.missilesystem.missile.Missile;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class TargetCommand implements TabExecutor {

    public static final int MAX_DISTANCE = 150;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            boolean open = false;
            if (args.length > 0 && args[0].equalsIgnoreCase("open")) {
                open = true;
                String[] copy = new String[args.length - 1];

                System.arraycopy(args, 1, copy, 0, args.length - 1);

                args = copy;
            }

            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            Missile instance = Missile.getInstance(itemInMainHand);

            if (instance == null) {
                player.sendMessage(Main.getInstance().getConfig().getString("messages.no_missile_in_hand", ""));
                return true;
            }

            Location targetLocation = null;
            if (args.length == 0) {
                Block targetBlockExact = player.getTargetBlockExact(MAX_DISTANCE);
                if (targetBlockExact == null) {
                    player.sendMessage(Main.getInstance().getConfig().getString("messages.not_targeting_block", ""));
                    return true;
                }

                targetLocation = targetBlockExact.getLocation().add(0.5, 0.5, 0.5);
            } else if (args.length == 1 || args.length == 2) {
                player.sendMessage(Main.getInstance().getConfig().getString("messages.look_at_block_or_loc", ""));
                return true;
            } else if (args.length == 3) {
                try{
                    targetLocation = new Location(player.getWorld(),
                                                  args[0].equals("~") ? player.getLocation().getX() : Double.parseDouble(args[0]),
                                                  args[1].equals("~") ? player.getLocation().getY() : Double.parseDouble(args[1]),
                                                  args[2].equals("~") ? player.getLocation().getZ() : Double.parseDouble(args[2]));
                }catch(NumberFormatException e){
                    player.sendMessage(Main.getInstance().getConfig().getString("messages.use_numbers_as_coordinates", ""));
                    return true;
                }
            }

            if (targetLocation == null) {
                player.sendMessage(Main.getInstance().getConfig().getString("messages.look_at_block_or_loc", ""));
                return true;
            }

            player.sendMessage(
                    Main.getInstance().getConfig().getString("messages.target_to", "")
                            .replace("%x%", String.valueOf(targetLocation.getBlockX()))
                            .replace("%y%", String.valueOf(targetLocation.getBlockY()))
                            .replace("%z%", String.valueOf(targetLocation.getBlockZ())));
            instance.setTarget(itemInMainHand, targetLocation, true);
            if (open) {
                MissileEvents.openGui(targetLocation, player, instance);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
            Missile instance = Missile.getInstance(itemInMainHand);

            if (instance == null) {
                return Collections.singletonList(ChatColor.DARK_RED + "Tu dois tenir un missile dans ta main !");
            }

            Block targetBlockExact = player.getTargetBlockExact(MAX_DISTANCE);

            if (args.length > 0 && args[0].equalsIgnoreCase("open")) {
                String[] copy = new String[args.length - 1];
                System.arraycopy(args, 1, copy, 0, args.length - 1);
                args = copy;
            }

            if (targetBlockExact != null) {
                Location location = targetBlockExact.getLocation();

                if (args.length == 1) {
                    return List.of(
                            String.valueOf(location.getX()),
                            location.getX() + " " + location.getY(),
                            location.getX() + " " + location.getY() + location.getZ(),
                            "~",
                            "~ ~",
                            "~ ~ ~"
                    );
                } else if (args.length == 2) {
                    return List.of(
                            String.valueOf(location.getY()),
                            location.getY() + " " + location.getZ(),
                            "~",
                            "~ ~"
                    );
                } else if (args.length == 3) {
                    return List.of(
                            String.valueOf(location.getZ()),
                            "~"
                    );
                }
            }
        }
        return List.of();
    }

}
