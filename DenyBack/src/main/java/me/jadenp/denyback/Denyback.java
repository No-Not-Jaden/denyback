//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

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
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Denyback extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    public static StateFlag MY_CUSTOM_FLAG;
    public List<String> aliases = new ArrayList<>();
    public File lastLocations = new File(this.getDataFolder() + File.separator + "locations.yml");
    ;
    public Map<String, Location> lastLoc = new HashMap<>();
    public boolean griefPreventionEnabled;
    public boolean denyBackClaims;
    public String deathPermission;
    public String prefix = ChatColor.GRAY + "[" + ChatColor.RED + "DenyBack" + ChatColor.GRAY + "] " + ChatColor.DARK_GRAY + "> ";

    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            StateFlag flag = new StateFlag("deny-back", false);
            registry.register(flag);
            MY_CUSTOM_FLAG = flag;
        } catch (FlagConflictException var4) {
            Flag<?> existing = registry.get("deny-back");
            if (existing instanceof StateFlag) {
                MY_CUSTOM_FLAG = (StateFlag) existing;
            }
        }

    }

    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(this.getCommand("denyback")).setExecutor(this);
        griefPreventionEnabled = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention") != null;
        this.saveDefaultConfig();
        try {
            if (lastLocations.createNewFile()) {
                Bukkit.getLogger().info("Created locations file.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(lastLocations);

        for (int i = 1; configuration.getString(i + ".uuid") != null; i++) {
            String uuid = configuration.getString(i + ".uuid");
            Location loc = configuration.getLocation(i + ".location");
            lastLoc.put(uuid, loc);
        }

        this.loadConfig();
        new BukkitRunnable() {
            public void run() {
                save();

            }
        }.runTaskTimer(this, 36000L, 36000L);
    }

    public void loadConfig() {
        this.reloadConfig();

        if (!this.getConfig().isSet("back-on-death-permission")) {
            this.getConfig().set("back-on-death-permission", "essentials.back.ondeath");
        }
        aliases.clear();
        aliases = this.getConfig().getStringList("deny-commands");
        denyBackClaims = this.getConfig().getBoolean("deny-untrusted-claims");
        deathPermission = this.getConfig().getString("back-on-death-permission");
    }

    public void onDisable() {
        save();
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();
        int i = 1;

        for (Map.Entry<String, Location> entry : lastLoc.entrySet()) {
            configuration.set(i + ".uuid", entry.getKey());
            configuration.set(i + ".location", entry.getValue());
            i++;
        }

        try {
            configuration.save(this.lastLocations);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Couldn't save last locations!");
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().hasPermission("denyback.admin")) {
            String message = event.getMessage();
            if (message.charAt(message.length() - 1) == ' ') {
                message = message.substring(0, message.length() - 1);
            }

            if (aliases.contains(message.toLowerCase(Locale.ROOT))) {
                Player p = event.getPlayer();
                if (lastLoc.containsKey(p.getUniqueId().toString())) {
                    Location testLocation = lastLoc.get(p.getUniqueId().toString());
                    if (getBackFlag(p, testLocation)) {
                        event.setCancelled(true);
                        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Hey! " + ChatColor.GRAY + "Sorry, but you can't return to there.");
                        return;
                    }

                    if (griefPreventionEnabled && denyBackClaims) {
                        if (!playerTrusted(p, testLocation)) {
                            p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Hey! " + ChatColor.GRAY + "Sorry, but you can't return to there.");
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }

    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("denyback")) {
            if (sender.hasPermission("denyback.admin")) {
                if (args.length == 1) {
                    list.add("reload");
                }
            }
        }
        return list;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("denyback")) {
            if (sender.hasPermission("denyback.admin")) {
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        this.loadConfig();
                        sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded DenyBack " + this.getDescription().getVersion() + ".");
                    } else {
                        sender.sendMessage(prefix + ChatColor.GOLD + "Unknown Command!");
                    }
                } else {
                    sender.sendMessage(prefix + ChatColor.GOLD + "Unknown Command!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
        }

        return true;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if ((event.getCause() == TeleportCause.COMMAND || event.getCause() == TeleportCause.PLUGIN) && !event.isCancelled()) {
            if (lastLoc.containsKey(event.getPlayer().getUniqueId().toString())) {
                lastLoc.replace(event.getPlayer().getUniqueId().toString(), event.getFrom());
            } else {
                lastLoc.put(event.getPlayer().getUniqueId().toString(), event.getFrom());
            }
        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (event.getEntity().hasPermission(deathPermission)) {
            if (lastLoc.containsKey(event.getEntity().getUniqueId().toString())) {
                lastLoc.replace(event.getEntity().getUniqueId().toString(), event.getEntity().getLocation());
            } else {
                lastLoc.put(event.getEntity().getUniqueId().toString(), event.getEntity().getLocation());
            }
        }
    }

    public boolean getBackFlag(Player p, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);
        return !WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(p.getWorld())) && set.testState(localPlayer, MY_CUSTOM_FLAG);
    }

    public boolean playerTrusted(Player p, Location location) {
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) {
            return true;
        }
        String perms = claim.allowAccess(p);
        if (perms == null) {
            return true;
        }
        p.sendMessage(ChatColor.RED + perms);
        return false;


    }
}
