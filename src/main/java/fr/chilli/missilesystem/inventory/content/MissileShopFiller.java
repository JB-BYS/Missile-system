package fr.chilli.missilesystem.inventory.content;

import fr.chilli.missilesystem.Main;
import fr.chilli.missilesystem.inventory.EInventory;
import fr.chilli.missilesystem.missile.Missile;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class MissileShopFiller implements IContentFiller {

    @Override
    public EInventory getEInventory() {
        return EInventory.MISSILE_SHOP;
    }

    @Override
    public Map<ItemStack, Function<Player, Consumer<Inventory>>> insertConsumerItems(Inventory inventory) {
        Map<ItemStack, Function<Player, Consumer<Inventory>>> map = new HashMap<>();

        Main.MISSILES.forEach(missile -> inventory.setItem(missile.getSlot(), registerMissile(map, missile)));


        return map;
    }

    private static ItemStack registerMissile(Map<ItemStack, Function<Player, Consumer<Inventory>>> map, Missile missile) {
        ItemStack itemStack = missile.getItemStack();

        map.put(itemStack, (player) -> (inventory) -> {
            if(!player.hasPermission("missile.buy")){
                player.sendMessage(Main.getInstance().getConfig().getString("messages.no_permission", ""));
                return;
            }

            if (player.getGameMode() == GameMode.CREATIVE) {
                player.sendMessage(
                        Main.getInstance().getConfig().getString("messages.bought_confirm_message", "")
                                .replace("%missile%", missile.getDisplayName()));
                player.getInventory().addItem(itemStack);
                return;
            }

            int price = missile.getPrice();
            if (Main.getEcon().has(player, price)) {
                Main.getEcon().withdrawPlayer(player, price);

                player.sendMessage(
                        Main.getInstance().getConfig().getString("messages.bought_confirm_message", "")
                                .replace("%missile%", missile.getDisplayName()));

                player.getInventory().addItem(itemStack);
            } else {
                player.sendMessage(Main.getInstance().getConfig().getString("messages.not_enough_money", ""));
            }
        });

        return itemStack;
    }

}
