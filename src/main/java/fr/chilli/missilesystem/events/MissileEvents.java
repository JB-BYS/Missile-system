package fr.chilli.missilesystem.events;

import fr.chilli.missilesystem.Main;
import fr.chilli.missilesystem.missile.Missile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class MissileEvents implements Listener {

    private static final NamespacedKey NAMESPACED_KEY = new NamespacedKey(Main.getInstance(), "last_missile");

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Missile instance = Missile.getInstance(event.getItem());

            Player player = event.getPlayer();

            if (instance == null) {
                return;
            }

            event.setCancelled(true);

            Location target = Missile.getTarget(event.getItem());

            if (target == null) {
                player.sendMessage(Main.getInstance().getConfig().getString("messages.no_target_selected", ""));
                return;
            }

            openGui(target, player, instance);
        }
    }

    public static void openGui(Location target, Player player, Missile instance) {
        target.setWorld(player.getWorld());

        Inventory inventory = Bukkit.createInventory(null, 9, ChatColor.RED + "Menu de lancement");

        double distance = target.distance(player.getLocation());
        inventory.setItem(0, Missile.getCancelItem());
        inventory.setItem(2, instance.getLocationItem(target));
        inventory.setItem(4, instance.getDistanceItem(distance));
        inventory.setItem(6, instance.getPriceItem());
        inventory.setItem(8, instance.getConfirmItem(distance));

        PersistentDataContainer persistentDataContainer = player.getPersistentDataContainer();

        persistentDataContainer.set(NAMESPACED_KEY, PersistentDataType.STRING, instance.getIntern());

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && event.getView().getTitle().equals(ChatColor.RED + "Menu de lancement")) {

            event.setCancelled(true);
            PersistentDataContainer persistentDataContainer = event.getWhoClicked().getPersistentDataContainer();
            String lastMissileInventory = persistentDataContainer.get(NAMESPACED_KEY, PersistentDataType.STRING);
            Missile instance = Missile.getInstanceByIntern(lastMissileInventory);

            if (currentItem.equals(Missile.getCancelItem())) {
                event.getWhoClicked().closeInventory();
                persistentDataContainer.remove(NAMESPACED_KEY);
                return;
            }


            if (currentItem.getType() == Material.MAP || currentItem.getType() == Material.PAPER) {
                if (event.isShiftClick()) {
                    Inventory clickedInventory = event.getClickedInventory();
                    if (instance != null && clickedInventory != null) {
                        Location location = event.getWhoClicked().getLocation();
                        instance.setTarget(clickedInventory.getItem(2), location, false);
                        instance.setTarget(event.getWhoClicked().getInventory().getItemInMainHand(), location, true);

                        event.getWhoClicked().sendMessage(
                                Main.getInstance().getConfig().getString("messages.target_to", "")
                                        .replace("%x%", String.valueOf(location.getBlockX()))
                                        .replace("%y%", String.valueOf(location.getBlockY()))
                                        .replace("%z%", String.valueOf(location.getBlockZ())));
                    }

                    return;
                }
                event.getWhoClicked().closeInventory();
                persistentDataContainer.remove(NAMESPACED_KEY);

                TextComponent textComponent = Component.text(Main.getInstance().getConfig().getString("messages.click_here_to_apply_loc", ""))
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/target open ~ ~ ~"));

                event.getWhoClicked().sendMessage(textComponent);

                return;
            }

            if (currentItem.getType() == Material.EMERALD) {
                if (!event.getWhoClicked().hasPermission("missile.launch")) {
                    event.getWhoClicked().sendMessage(Main.getInstance().getConfig().getString("messages.no_permission", ""));
                    return;
                }
                Location location = Missile.getTarget(event.getInventory().getItem(2));

                if (location == null) {
                    event.getWhoClicked().sendMessage(Main.getInstance().getConfig().getString("messages.no_target_selected", ""));
                    return;
                }

                location.setWorld(event.getWhoClicked().getWorld());

                if (instance == null) {
                    event.getWhoClicked().sendMessage(Main.getInstance().getConfig().getString("messages.missile_not_found_reopen", ""));
                    return;
                }

                int priceForDistance = instance.getPriceForDistance(location.distance(location));
                if (Main.getEcon().has(((Player) event.getWhoClicked()), priceForDistance) || event.getWhoClicked().getGameMode() == GameMode.CREATIVE) {

                    if (instance.launch(
                            (Player) event.getWhoClicked(),
                            location)) {
                        if (event.getWhoClicked().getGameMode() != GameMode.CREATIVE)
                            Main.getEcon().withdrawPlayer((Player) event.getWhoClicked(), priceForDistance);

                        event.getWhoClicked().closeInventory();
                        persistentDataContainer.remove(NAMESPACED_KEY);
                    } else {
                        event.getWhoClicked().sendMessage(ChatColor.RED + "Tu ne peux pas lancer ce missile...");
                    }

                } else {
                    event.getWhoClicked().sendMessage(ChatColor.RED + "Tu n'as pas assez de Pepettes...");
                }
            }
        }
    }
}
