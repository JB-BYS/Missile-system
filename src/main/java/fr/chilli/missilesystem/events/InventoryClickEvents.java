package fr.chilli.missilesystem.events;

import fr.chilli.missilesystem.inventory.EInventory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

public class InventoryClickEvents implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (!EInventory.isValidInventory(clickedInventory)) {
            return;
        }

        event.setCancelled(true);
        Inventory inventory = EInventory.getInventory(event.getCurrentItem(), ((Player) event.getWhoClicked()));
        if (inventory != null) {
            event.getWhoClicked().openInventory(inventory);
            return;
        }

        Consumer<Inventory> runnable = EInventory.getConsumer(event.getCurrentItem(), ((Player) event.getWhoClicked()));
        if (runnable != null)
            runnable.accept(clickedInventory);
    }
}
