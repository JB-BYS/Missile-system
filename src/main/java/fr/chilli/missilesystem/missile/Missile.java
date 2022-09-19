package fr.chilli.missilesystem.missile;

import fr.chilli.missilesystem.Main;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.bukkit.Effect.MOBSPAWNER_FLAMES;


public class Missile {

    private final ItemStack itemStack;
    private final List<String> lore;
    private final int price;
    private final float pricePerBlock = 0.1f;
    private final int slot;
    private final String intern;
    private final String displayName;
    private final int explosionStrength;
    private static final Map<Firework, BukkitTask> MISSILE_TASKS = new HashMap<>();
    private static final Map<Firework, Location> MISSILE_LAST_LOC = new HashMap<>();

    public Missile(String intern, String displayName, List<String> lore, int explosionStrength, int flightTime, int price, int slot) {
        this.intern = intern;
        this.displayName = displayName;
        this.explosionStrength = explosionStrength;
        this.lore = lore;
        this.price = price;
        this.slot = slot;
        this.itemStack = new ItemStack(Material.FIREWORK_ROCKET);

        FireworkMeta fireworkMeta = (FireworkMeta) itemStack.getItemMeta();
        assert fireworkMeta != null;
        fireworkMeta.setDisplayName(displayName);
        fireworkMeta.setLore(lore);
        this.itemStack.setItemMeta(fireworkMeta);
    }

    public int getExplosionStrength() {
        return explosionStrength;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public int getSlot() {
        return slot;
    }

    public String getDisplayName() {
        return displayName + ChatColor.RESET;
    }

    public int getPrice() {
        return price;
    }

    public int getPriceForDistance(double distance) {
        return (int) Math.round(pricePerBlock * distance);
    }

    public String getIntern() {
        return intern;
    }

    public static Missile getInstanceByIntern(String intern) {
        for (Missile missile : Main.MISSILES) {
            if (missile.getIntern().equals(intern)) {
                return missile;
            }
        }
        return null;
    }

    public static Missile getInstance(ItemStack itemStack) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();

        for (Missile value : Main.MISSILES) {
            ItemMeta itemMeta = value.getItemStack().getItemMeta();
            if ((meta == null) != (itemMeta == null)) {
                continue;
            }

            if (value.getItemStack().getType() == itemStack.getType()
                    && meta != null
                    && itemMeta.getDisplayName().equals(meta.getDisplayName())) {
                return value;
            }
        }

        return null;
    }

    public void setTarget(ItemStack itemStack, Location target, boolean all) {
        if (itemStack == null) return;
        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) return;
        List<String> lore = all ? new ArrayList<>(this.lore) : new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cible: " + target.getBlockX() + " " + target.getBlockY() + " " + target.getBlockZ());
        meta.setLore(lore);
        itemStack.setItemMeta(meta);
    }

    public static Location getTarget(ItemStack itemStack) {
        if (itemStack == null) return null;
        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) return null;
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        for (String line : lore) {
            if (line.startsWith(ChatColor.GRAY + "Cible: ")) {
                String[] split = line.split(" ");
                return new Location(null, Double.parseDouble(split[1]), Double.parseDouble(split[2]), Double.parseDouble(split[3]));
            }
        }
        return null;
    }

    public boolean launch(Player player, Location target) {
        for (ItemStack stack : player.getInventory()) {
            if (stack == null) continue;
            if (stack.getType() == Material.FIREWORK_ROCKET) {
                if (Objects.requireNonNull(stack.getItemMeta()).getDisplayName().equals(this.displayName)) {
                    launch(stack, player, target);
                    return true;
                }
            }
        }

        return false;
    }

    public void launch(ItemStack item, Player player, Location target) {
        Location source = player.getEyeLocation();
        FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
        assert fireworkMeta != null;

        fireworkMeta.setPower(127);
        if (player.getGameMode() != GameMode.CREATIVE)
            item.setAmount(item.getAmount() - 1);

        World world = target.getWorld();
        if (world == null) {
            player.sendMessage(Main.getInstance().getConfig().getString("messages.error_world_null", ""));
            return;
        }

        Main.log(player.getUniqueId(), this);

        String on_launch = getMessage(player.getUniqueId(), "launch");
        if (on_launch != null) {
            on_launch = on_launch.replaceAll("%player%", player.getName())
                    .replaceAll("%missile%", displayName);

            if (!on_launch.isEmpty())
                Bukkit.broadcastMessage(on_launch);
        }


        Firework firework = (Firework) world.spawnEntity(source, EntityType.FIREWORK);
        firework.setFireworkMeta(fireworkMeta);
        firework.setGravity(false);

        AtomicReference<Vector> atomicVector = new AtomicReference<>(new Vector());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                Main.getInstance(), () -> countdown(target, firework, world, atomicVector),
                0,
                1);
        Location clone = player.getLocation().clone();
        BukkitTask task1 = Bukkit.getScheduler().runTaskTimer(
                Main.getInstance(), () -> {
                    if (!task.isCancelled())
                        task.cancel();

                    tick(firework, world, player, clone, target);
                },
                200,
                1);

        MISSILE_TASKS.put(firework, task1);
    }

    private void countdown(Location target, Firework firework, World world, AtomicReference<Vector> atomicReference) {
        Vector ZERO = new Vector(0, 0, 0);
        firework.setVelocity(ZERO);
        int ticksLived = firework.getTicksLived();

        Bukkit.getOnlinePlayers().forEach(p1 -> p1.sendTitle(String.valueOf(10 - (ticksLived / 20)), "", 0, 20, 0));

        world.playEffect(
                firework.getLocation(),
                MOBSPAWNER_FLAMES,
                1);

        atomicReference.set(calculateVector(target, firework.getLocation()));
    }

    private void tick(Firework firework, World world, Player player, Location playerLocation, Location target) {
        Location location = firework.getLocation();
        if (!location.getChunk().isLoaded()) {
            location.getChunk().load();
        }

        firework.setTicksLived(205);
        if (firework.isDead() || !firework.isValid()) {
            handleExplosion(world, firework, player);
            return;
        }

        if (MISSILE_LAST_LOC.containsKey(firework)) {
            Location lastLoc = MISSILE_LAST_LOC.get(firework);
            Location fireworkLoc = firework.getLocation();

            if (lastLoc.getX() == fireworkLoc.getX()
                    || lastLoc.getY() == fireworkLoc.getY()
                    || lastLoc.getZ() == fireworkLoc.getZ()
                    || fireworkLoc.getBlock().getType().isSolid()) {

                handleExplosion(world, firework, player);
                return;
            }
        }

        world.playEffect(
                firework.getLocation(),
                MOBSPAWNER_FLAMES,
                1);

        MISSILE_LAST_LOC.put(firework, firework.getLocation());

        Location loc0 = location.clone();
        loc0.setY(0);
        Location player0 = playerLocation.clone();
        player0.setY(0);
        Location target0 = target.clone();
        target0.setY(0);

        //percentage of distance to target
        double p = loc0.distance(player0) / target0.distance(player0);
        double currentY = firework.getVelocity().getY();

        Vector vector;

        if (p <= 0.5) {
            vector = target0
                    .toVector()
                    .subtract(loc0.toVector())
                    .normalize()
                    .setY(firework.getLocation().getY() > (firework.getWorld().getMaxHeight() + 100) ? 0 : currentY - translatePercentageIntoVariable(p));
        } else {
            if (calculateVector(target, location).equals(firework.getVelocity())) {
                vector = firework.getVelocity().multiply(1.2);
            } else {
                vector = calculateVector(target, location);
            }
        }

        firework.setVelocity(vector);
    }

    private Vector calculateVector(Location loc1, Location loc2) {
        return loc1.toVector()
                .subtract(loc2.toVector())
                .normalize();
    }

    private void handleExplosion(World world, Firework firework, Player player) {
            int x;
            int y = firework.getLocation().getBlockY();
            int lastY=y;
            int z;
            double i;
            double r;
            for (double t = explosionStrength; t > 0; t--) {
                y = y-1;
                for (i = 0.0; i < 360.0; i += 0.1) {
                    for (r = t; r > 0; r--) {
                        double angle = i * Math.PI / 180;
                        x = (int) (firework.getLocation().getBlockX() + r * Math.cos(angle));
                        z = (int) (firework.getLocation().getBlockZ() + r * Math.sin(angle));

                        this.getServer().getWorld("world").getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
            y = lastY;
            y = y-1;
            for (double t = explosionStrength; t > 0; t--) {
                y = y+1;
                for (i = 0.0; i < 360.0; i += 0.1) {
                    for (r = t; r > 0; r--) {
                        double angle = i * Math.PI / 180;
                        x = (int) (firework.getLocation().getBlockX() + r * Math.cos(angle));
                        z = (int) (firework.getLocation().getBlockZ() + r * Math.sin(angle));

                        this.getServer().getWorld("world").getBlockAt(x, y, z).setType(Material.FIRE);
                }
            }
        }
        firework.remove();


        MISSILE_TASKS.remove(firework).cancel();
        String impact = getMessage(player.getUniqueId(), "impact");

        if (impact != null) {

            impact = impact
                    .replaceAll("%player%", player.getName())
                    .replaceAll("%missile%", displayName);

            if (!impact.isEmpty())
                Bukkit.broadcastMessage(impact);
        }
    }

    private Bukkit getServer() {
        return null;
    }

    public ItemStack getLocationItem(Location target) {
        ItemStack itemStack = new ItemStack(Material.MAP);

        setTarget(itemStack, target, false);
        ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.GRAY + "Localisation");
        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

    public ItemStack getDistanceItem(double distance) {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GRAY + "Distance");
        meta.setLore(List.of(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + Math.round(distance) + " Blocs"));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public ItemStack getPriceItem() {
        ItemStack itemStack = new ItemStack(Material.PAPER);
        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GRAY + "Prix du Missile");
        meta.setLore(List.of(ChatColor.GRAY + "Prix du Missile: " + ChatColor.WHITE  + getPrice()+ "Pp"));
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static ItemStack getCancelItem() {
        ItemStack itemStack = new ItemStack(Material.BARRIER);

        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.RED + "Annuler");
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    public ItemStack getConfirmItem(double distance) {
        ItemStack itemStack = new ItemStack(Material.EMERALD);

        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;
        meta.setDisplayName(ChatColor.GREEN + "Confirmer");
        meta.setLore(List.of(ChatColor.GRAY + "Prix: " + ChatColor.WHITE + getPriceForDistance(distance)+"Pp"));
        itemStack.setItemMeta(meta);

        return itemStack;
    }

    private static String getMessage(UUID uuid, String type) {
        return Main.getInstance().MESSAGES.getString(uuid + "." + type.toLowerCase());
    }

    /**
     * This function will take a percentage p and return its relative counterpart in the range [-1, 1]
     */
    private static double translatePercentageIntoVariable(double p) {
        if (p < 0)
            p = 0;
        if (p > 1)
            p = 1;

        return 2 * p - 1;
    }

}
