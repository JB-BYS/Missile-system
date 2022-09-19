package fr.chilli.missilesystem;

import fr.chilli.missilesystem.commands.MessageCommand;
import fr.chilli.missilesystem.commands.MissileCommand;
import fr.chilli.missilesystem.commands.TargetCommand;
import fr.chilli.missilesystem.events.InventoryClickEvents;
import fr.chilli.missilesystem.events.MissileEvents;
import fr.chilli.missilesystem.missile.Missile;
import fr.chilli.missilesystem.placeholder.MissileExpansion;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class Main extends JavaPlugin {
    private static Main instance;
    private static Economy econ = null;
    public static final List<Missile> MISSILES = new ArrayList<>();
    public final File LOG_FILE = new File(getDataFolder(), "log.yml");
    public final FileConfiguration LOG = YamlConfiguration.loadConfiguration(LOG_FILE);
    public final File MESSAGES_FILE = new File(getDataFolder(), "messages.yml");
    public final FileConfiguration MESSAGES = YamlConfiguration.loadConfiguration(MESSAGES_FILE);


    @Override
    public void onEnable() {

        config = getConfig();
        config.options().copyDefaults(true);
        instance = this;
        saveDefaultConfig();
        cfile = new File(getDataFolder(),"config.yml");

        ConfigurationSection missiles = getConfig().getConfigurationSection("missiles");

        if (missiles == null) {
            getLogger().warning("Pas de missile défini");
            return;
        }

        Set<String> missileNames = missiles.getKeys(false);
        missileNames.forEach(name -> {
            int price = missiles.getInt(name + ".price");
            int explosionStrength = missiles.getInt(name + ".explosion_strength");
            int flightTime = missiles.getInt(name + ".flight_time");
            List<String> lore = missiles.getStringList(name + ".lore");
            String displayName = missiles.getString(name + ".display_name");
            int slot = missiles.getInt(name + ".slot");

            MISSILES.add(new Missile(name, displayName, lore, explosionStrength, flightTime, price, slot));
        });


        getCommand("missile").setExecutor(new MissileCommand());
        getCommand("target").setExecutor(new TargetCommand());
        getCommand("setmessage").setExecutor(new MessageCommand());

        getServer()
                .getPluginManager()
                .registerEvents(new InventoryClickEvents(), this);

        getServer()
                .getPluginManager()
                .registerEvents(new MissileEvents(), this);

        if (!new MissileExpansion().register()) {
            getLogger().warning("Could not register PlaceholderAPI expansion!");
        }


        if (!setupEconomy()) {
            getLogger().severe("- Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    FileConfiguration config;
    File cfile;
    public boolean onCommand(CommandSender sender, Command cmd,String commandLabel,String[] args){
        if (cmd.getName().equalsIgnoreCase("missile_reload")){
            config = YamlConfiguration.loadConfiguration(cfile);
            sender.sendMessage(ChatColor.GREEN + "Missile reload !");
        }
        return true;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        econ = rsp.getProvider();
        return true;
    }

    public static Main getInstance() {
        return instance;
    }

    public static Economy getEcon() {
        return econ;
    }

    public static void log(UUID uuid, Missile missile) {
        List<String> stringList = getInstance()
                .LOG
                .getStringList(uuid.toString());

        stringList.add(missile.getIntern());

        getInstance()
                .LOG
                .set(uuid.toString(), stringList);

        try{
            getInstance()
                    .LOG
                    .save(getInstance().LOG_FILE);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static int getUsesOfMissile(UUID uuid, String intern) {
        List<String> stringList = getInstance()
                .LOG
                .getStringList(uuid.toString());

        int uses = 0;
        for (String s : stringList) {
            if (s.equals(intern)) {
                uses++;
            }
        }

        return uses;
    }
}
