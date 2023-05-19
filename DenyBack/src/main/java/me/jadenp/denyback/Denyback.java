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
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public final class Denyback extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    public static StateFlag MY_CUSTOM_FLAG;
    public List<String> aliases = new ArrayList<>();
    public File lastLocations = new File(this.getDataFolder() + File.separator + "locations.yml");

    public Map<String, Location> lastLoc = new HashMap<>();
    public boolean griefPreventionEnabled;
    public boolean denyBackClaims;
    public String deathPermission;
    public String denyMessage;
    public boolean useGriefPreventionMessage;
    public boolean denyNonMembers;
    public boolean debug = false;
    public int minBackDistance;
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

        loadConfig();
        new BukkitRunnable() {
            public void run() {
                save();

            }
        }.runTaskTimer(this, 36000L, 36000L);
    }

    public void loadConfig() {
        this.reloadConfig();

        if (!getConfig().isSet("back-on-death-permission"))
            getConfig().set("back-on-death-permission", "essentials.back.ondeath");
        if (!getConfig().isSet("deny-message"))
            getConfig().set("deny-message", "&c&lHey! &7Sorry, but you can't return to there.");
        if (!getConfig().isSet("use-grief-prevention-message"))
            getConfig().set("use-grief-prevention-message", true);
        if (!getConfig().isSet("deny-nonmembers"))
            getConfig().set("deny-nonmembers", false);

        saveConfig();

        aliases.clear();
        aliases = getConfig().getStringList("deny-commands");
        denyBackClaims = getConfig().getBoolean("deny-untrusted-claims");
        deathPermission = getConfig().getString("back-on-death-permission");
        denyMessage = color(getConfig().getString("deny-message"));
        useGriefPreventionMessage = getConfig().getBoolean("use-grief-prevention-message");
        denyNonMembers = getConfig().getBoolean("deny-nonmembers");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        if (plugin != null){
            minBackDistance = plugin.getConfig().getInt("Optimizations.Teleport.BackMinDistance");
            if (debug)
                Bukkit.getLogger().info("[DenyBack] CMI registered, BackMinDistance: " + minBackDistance);
        } else {
            minBackDistance = 0;
        }
    }

    public static String color(String str) {
        str = net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', str);
        return translateHexColorCodes("&#", "", str);
    }

    public static String translateHexColorCodes(String startTag, String endTag, String message) {
        final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        return matcher.appendTail(buffer).toString();
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

            String message = event.getMessage();
            if (message.charAt(message.length() - 1) == ' ') {
                message = message.substring(0, message.length() - 1);
            }

            if (aliases.contains(message.toLowerCase(Locale.ROOT))) {
                // back command
                if (!event.getPlayer().hasPermission("denyback.admin")) {
                    Player p = event.getPlayer();
                    if (lastLoc.containsKey(p.getUniqueId().toString())) {
                        Location testLocation = lastLoc.get(p.getUniqueId().toString());
                        if (getBackFlag(p, testLocation)) {
                            event.setCancelled(true);
                            p.sendMessage(denyMessage);
                            return;
                        }

                        if (griefPreventionEnabled && denyBackClaims) {
                            if (!playerTrusted(p, testLocation)) {
                                if (!useGriefPreventionMessage)
                                    p.sendMessage(denyMessage);
                                event.setCancelled(true);
                            }
                        }
                    }
                } else {
                    if (debug)
                        Bukkit.getLogger().info("[DenyBack] " + event.getPlayer().getName() + " has admin permissions.");
                }
            }


    }

    @Override
    public List<String> onTabComplete(@Nonnull CommandSender sender, Command command, @Nonnull String alias, @Nonnull String[] args) {
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

    public boolean onCommand(@Nonnull CommandSender sender, Command command, @Nonnull String label, @Nonnull String[] args) {
        if (command.getName().equalsIgnoreCase("denyback")) {
            if (sender.hasPermission("denyback.admin")) {
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("reload")) {
                        this.loadConfig();
                        sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded DenyBack " + this.getDescription().getVersion() + ".");
                    } else if (args[0].equalsIgnoreCase("debug")){
                        debug = !debug;
                        sender.sendMessage(prefix + ChatColor.YELLOW + "Debug mode set to " + debug + ".");
                    }
                    else {
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
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Player teleported: " + event.getPlayer().getName() + " Reason: " + event.getCause() + " Canceled: " + event.isCancelled());
        if ((event.getCause() == TeleportCause.COMMAND || event.getCause() == TeleportCause.PLUGIN) && !event.isCancelled()) {
            int distance = event.getTo() != null && event.getTo().getWorld() != null && event.getFrom().getWorld() != null && event.getTo().getWorld().equals(event.getFrom().getWorld()) ? (int) event.getTo().distance(event.getFrom()) : 9999;
            if (debug)
                Bukkit.getLogger().info("[DenyBack] Teleport Distance: " + distance + "m");
            if (distance  > minBackDistance){
                lastLoc.put(event.getPlayer().getUniqueId().toString(), event.getFrom());
                if (debug)
                    Bukkit.getLogger().info("[DenyBack] Teleport registered!");
            }

        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Player died: " + event.getEntity().getName() + " Has death permission: " + (event.getEntity().hasPermission(deathPermission) || deathPermission.isEmpty()));
        if (event.getEntity().hasPermission(deathPermission) || deathPermission.isEmpty()) {
            lastLoc.put(event.getEntity().getUniqueId().toString(), event.getEntity().getLocation());
            if (debug)
                Bukkit.getLogger().info("[DenyBack] Death registered!");
        }
    }

    public boolean getBackFlag(Player p, Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);
        assert location.getWorld() != null;
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Checking deny-back flag for " + p.getName() + " at " + Math.round(location.getX()) + " " + Math.round(location.getY()) + " " + Math.round(location.getZ()) + " " + location.getWorld().getName());

        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(p.getWorld()))){
            if (debug)
                Bukkit.getLogger().info("[DenyBack] Player bypasses DenyBack flag.");
            return false;
        }
        if (set.testState(localPlayer, MY_CUSTOM_FLAG)) {
            if (debug)
                Bukkit.getLogger().info("[DenyBack] Player cannot return.");
            return true;
        }
        if (denyNonMembers) {
            if (debug)
                Bukkit.getLogger().info("[DenyBack] Member status: " + set.isMemberOfAll(localPlayer) + ".");
            return !set.isMemberOfAll(localPlayer);
        }
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Player has been allowed.");
        return false;
    }

    public boolean playerTrusted(Player p, Location location) {
        assert location.getWorld() != null;
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Checking GriefPrevention Claim for " + p.getName() + " at " + Math.round(location.getX()) + " " + Math.round(location.getY()) + " " + Math.round(location.getZ()) + " " + location.getWorld().getName());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
        if (claim == null) {
            if (debug)
                Bukkit.getLogger().info("[DenyBack] No claim found.");
            return true;
        }
        String perms = claim.allowAccess(p);
        if (perms == null) {
            if (debug)
                Bukkit.getLogger().info("[DenyBack] Player is trusted.");
            return true;
        }
        if (useGriefPreventionMessage)
            p.sendMessage(ChatColor.RED + perms);
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Player is not trusted.");
        return false;


    }
}
