package fr.chilli.missilesystem.inventory;

import fr.chilli.missilesystem.inventory.content.IContentFiller;
import fr.chilli.missilesystem.inventory.content.MissileShopFiller;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public enum EInventory {
    MISSILE_SHOP;

    private static final Map<EInventory, Inventory> INVENTORIES = new HashMap<>();
    private static final Map<ItemStack, Function<Player, Inventory>> ITEM_INVENTORY_MAP = new HashMap<>();
    private static final Map<ItemStack, Function<Player, EInventory>> ITEM_E_INVENTORY_MAP = new HashMap<>();
    private static final Map<ItemStack, Function<Player, Consumer<Inventory>>> ITEM_CONSUMER_MAP = new HashMap<>();

    static {
        INVENTORIES.put(MISSILE_SHOP, Bukkit.createInventory(null, 9 * 3, ChatColor.GOLD + "Menu de Shopping"));

        getIContentFillers().forEach(iContentFiller -> {
            Inventory inventory = INVENTORIES.get(iContentFiller.getEInventory());

            ITEM_INVENTORY_MAP.putAll(iContentFiller.insertInventoryItems(inventory));
            ITEM_E_INVENTORY_MAP.putAll(iContentFiller.insertEInventoryItems(inventory));
            ITEM_CONSUMER_MAP.putAll(iContentFiller.insertConsumerItems(inventory));
        });
    }

    public static Inventory getInventory(ItemStack clickedItemStack, Player player) {
        return ITEM_INVENTORY_MAP.containsKey(clickedItemStack)
                ? ITEM_INVENTORY_MAP.get(clickedItemStack).apply(player)
                : null;
    }

    public static Consumer<Inventory> getConsumer(ItemStack itemStack, Player whoClicked) {
        return ITEM_CONSUMER_MAP.containsKey(itemStack)
                ? ITEM_CONSUMER_MAP.get(itemStack).apply(whoClicked)
                : null;
    }

    public static boolean isValidInventory(Inventory inventory) {
        return INVENTORIES.containsValue(inventory);
    }

    private static List<IContentFiller> getIContentFillers() {
        return List.of(new MissileShopFiller());
    }

    public Inventory getInventory() {
        return INVENTORIES.get(this);
    }
}
