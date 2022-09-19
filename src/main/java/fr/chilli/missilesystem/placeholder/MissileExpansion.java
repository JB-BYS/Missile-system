package fr.chilli.missilesystem.placeholder;

import fr.chilli.missilesystem.Main;
import fr.chilli.missilesystem.missile.Missile;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MissileExpansion extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "missilesystem";
    }

    @Override
    public @NotNull String getAuthor() {
        return "chilli_pepper";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        List<String> placeholders = new ArrayList<>();

        placeholders.add("nbrstockedmissile");

        for (Missile missile : Main.MISSILES) {
            placeholders.add("islaunched" + missile.getIntern());

            placeholders.add("nbrusedmissile" + missile.getIntern());
        }

        return placeholders;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline())
            return null;

        return onPlaceholderRequest(player.getPlayer(), params);
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("nbrstockedmissile")) {
            if (player != null) {
                PlayerInventory inventory = player.getInventory();
                int stockedMissile = 0;

                for (ItemStack itemStack : inventory) {
                    if (Missile.getInstance(itemStack) != null) {
                        stockedMissile += itemStack.getAmount();
                    }
                }

                return String.valueOf(stockedMissile);
            }
            return null;
        }

        for (Missile missile : Main.MISSILES) {
            if (params.equalsIgnoreCase("islaunched" + missile.getIntern())) {
                return String.valueOf(Main.getUsesOfMissile(player.getUniqueId(), missile.getIntern()));
            }

            if (params.equalsIgnoreCase("nbrusedmissile" + missile.getIntern())) {
                return String.valueOf(Main.getUsesOfMissile(player.getUniqueId(), missile.getIntern()) != 0);
            }
        }

        return null;
    }

}
