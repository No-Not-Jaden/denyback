package me.jadenp.denyback;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class Denyback extends JavaPlugin implements Listener, CommandExecutor {

    public static StateFlag MY_CUSTOM_FLAG;
    public List<String> aliases = new ArrayList<>();
    public File lastLocations = new File(this.getDataFolder() + File.separator + "locations.yml");
    public Map<String, Location> lastLoc = new HashMap<>();
    public boolean griefPreventionEnabled;
    public boolean denyBackClaims;
    /*
    add config
    choose denied commands x
    record last positions x
     */

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("Deny-Back", false);
            registry.register(flag);
            MY_CUSTOM_FLAG = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("Deny-Back");
            if (existing instanceof StateFlag) {
                MY_CUSTOM_FLAG = (StateFlag) existing;
            } else {

            }
        }
    }
    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(this.getCommand("denyback")).setExecutor(this);
        griefPreventionEnabled = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention") != null;
        this.saveDefaultConfig();
        if (!lastLocations.exists()){
            try {
                lastLocations.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(lastLocations);
        int i = 1;
        while (configuration.getString(i + ".uuid") != null){
            String uuid = configuration.getString(i + ".uuid");
            Location loc = configuration.getLocation(i + ".location");
            lastLoc.put(uuid, loc);
            i++;
        }
        loadConfig();

        new BukkitRunnable(){
            @Override
            public void run() {
                YamlConfiguration configuration = new YamlConfiguration();
                int i = 1;
                for (Map.Entry mapElement : lastLoc.entrySet()) {
                    String key = (String) mapElement.getKey();
                    Location loc = (Location) mapElement.getValue();
                    configuration.set(i + ".uuid", key);
                    configuration.set(i + ".location", loc);
                    i++;
                }
                try {
                    configuration.save(lastLocations);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(this, 36000, 36000);
    }

    public void loadConfig(){
        aliases.clear();
        aliases = this.getConfig().getStringList("deny-commands");
        denyBackClaims = this.getConfig().getBoolean("deny-untrusted-claims");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        YamlConfiguration configuration = new YamlConfiguration();
        int i = 1;
        for (Map.Entry mapElement : lastLoc.entrySet()) {
            String key = (String) mapElement.getKey();
            Location loc = (Location) mapElement.getValue();
            configuration.set(i + ".uuid", key);
            configuration.set(i + ".location", loc);
            i++;
        }
        try {
            configuration.save(lastLocations);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandPreprocessEvent event){
            String message = event.getMessage();
            if (message.charAt(message.length()-1) == ' '){
                message = message.substring(0,message.length()-1);
            }
            if (aliases.contains(message.toLowerCase(Locale.ROOT))){
                Player p = event.getPlayer();
                if (lastLoc.containsKey(p.getUniqueId().toString())){
                    Location testLocation = lastLoc.get(p.getUniqueId().toString());
                    if (getBackFlag(p, testLocation)) {
                        event.setCancelled(true);
                        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Hey! " + ChatColor.GRAY + "Sorry, but you can't return to there.");
                        return;
                    }
                    if (griefPreventionEnabled && denyBackClaims){
                        Claims claims = new Claims();
                        if (!claims.playerTrusted(p, testLocation)){
                            event.setCancelled(true);
                        }
                    }
                }
            }

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("denyback")){
            if (sender.hasPermission("denyback.admin")){
                if (args.length == 1){
                    if (args[0].equalsIgnoreCase("reload")){
                        loadConfig();
                        sender.sendMessage(ChatColor.GREEN + "Reloaded WorldGuardDenyBack.");
                    } else {
                        sender.sendMessage(ChatColor.GOLD + "WHat u doin fam, that command doesn't exist.");
                    }
                } else {
                    sender.sendMessage(ChatColor.GOLD + "WHat u doin fam, that command doesn't exist.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
        }
        return true;
    }


    @EventHandler
    public void onTeleport(PlayerTeleportEvent event){
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.COMMAND || event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            if (!event.isCancelled()) {
                if (lastLoc.containsKey(event.getPlayer().getUniqueId().toString())) {
                    lastLoc.replace(event.getPlayer().getUniqueId().toString(), event.getFrom());
                } else {
                    lastLoc.put(event.getPlayer().getUniqueId().toString(), event.getFrom());
                }
            }
        }
    }

    public boolean getBackFlag(Player p, Location location){
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);
        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(p.getWorld()))){
            return false;
        }
        return set.testState(localPlayer, MY_CUSTOM_FLAG);
    }
}
