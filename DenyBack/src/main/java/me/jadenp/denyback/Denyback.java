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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static me.jadenp.denyback.ConfigOptions.*;

/**
 * Back command
 * all aliases work & tab complete
 * use last available location works
 * teleport delay works
 * permissions work
 */

public final class Denyback extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    public static StateFlag MY_CUSTOM_FLAG = null;
    public static List<String> aliases = new ArrayList<>();
    public File lastLocations = new File(this.getDataFolder() + File.separator + "locations.yml");
    public Map<String, Location> lastLoc = new HashMap<>();
    private static Denyback instance;
    public static boolean debug = false;

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
            } else {
                Bukkit.getLogger().warning("Could not register deny-back flag! This usually means another plugin is conflicting.");
            }
        }

    }

    public static Denyback getInstance() {
        return instance;
    }

    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(this.getCommand("denyback")).setExecutor(this);
        griefPreventionEnabled = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention") != null;
        this.saveDefaultConfig();
        try {
            if (lastLocations.createNewFile()) {
                Bukkit.getLogger().info("[DenyBack] Created locations file.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(lastLocations);


        if (configuration.isConfigurationSection("1")) {
            // load old config
            for (int i = 1; configuration.getString(i + ".uuid") != null; i++) {
                String uuid = configuration.getString(i + ".uuid");
                Location loc = configuration.getLocation(i + ".location");
                lastLoc.put(uuid, loc);
            }
        } else {
            // load new config
            for (String uuid : configuration.getKeys(false)) {
                String worldUUID = configuration.getString(uuid + ".location.world");
                double x = configuration.getDouble(uuid + ".location.x");
                double y = configuration.getDouble(uuid + ".location.y");
                double z = configuration.getDouble(uuid + ".location.z");
                double pitch = configuration.getDouble(uuid + ".location.pitch");
                double yaw = configuration.getDouble(uuid + ".location.yaw");

                if (worldUUID == null) {
                    Bukkit.getLogger().warning("[DenyBack] No world was present for last location of " + uuid);
                    continue;
                }
                World world = Bukkit.getWorld(UUID.fromString(worldUUID));
                if (world == null) {
                    Bukkit.getLogger().warning("[DenyBack] Invalid world for last location of " + uuid);
                    continue;
                }
                Location location = new Location(world, x, y, z, (float) pitch, (float) yaw);
                lastLoc.put(uuid, location);
            }
        }


        ConfigOptions.loadConfig();

        if (registerCommand)
            Objects.requireNonNull(getCommand("dback")).setExecutor(this);

        new BukkitRunnable() {
            public void run() {
                save();
            }
        }.runTaskTimer(this, 36000L, 36000L);
    }





    public void onDisable() {
        save();
    }

    public void save() {
        YamlConfiguration configuration = new YamlConfiguration();

        for (Map.Entry<String, Location> entry : lastLoc.entrySet()) {
            String uuid = entry.getKey();
            Location location = entry.getValue();
            // can't save a null world or location
            if (location == null || location.getWorld() == null)
                continue;

            configuration.set(uuid + ".location.world", location.getWorld().getUID().toString());
            configuration.set(uuid + ".location.x", location.getX());
            configuration.set(uuid + ".location.y", location.getY());
            configuration.set(uuid + ".location.z", location.getZ());
            configuration.set(uuid + ".location.yaw", location.getYaw());
            configuration.set(uuid + ".location.pitch", location.getPitch());
        }

        try {
            configuration.save(lastLocations);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Couldn't save last locations!");
            Bukkit.getLogger().warning(e.toString());
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandPreprocessEvent event) {

        String message = event.getMessage();
        if (message.charAt(message.length() - 1) == ' ') {
            message = message.substring(0, message.length() - 1);
        }

        if (aliases.contains(message.toLowerCase(Locale.ROOT))) {
            Player p = event.getPlayer();
            // back command
            if (!event.getPlayer().hasPermission("denyback.admin")) {
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
                            return;
                        }
                    }
                }
            } else {
                if (debug)
                    Bukkit.getLogger().info("[DenyBack] " + event.getPlayer().getName() + " has admin permissions.");
            }
            if (registerCommand && lastLoc.containsKey(p.getUniqueId().toString()) && p.hasPermission("denyback.back")) {
                p.sendMessage(backMessage);
                event.setCancelled(true);
                if (teleportDelay > 0) {
                    if (debug)
                        Bukkit.getLogger().info("[DenyBack] Initializing delayed teleport...");
                    new BukkitRunnable() {
                        final int movingChars = 3;
                        StringBuilder displayMessage;
                        int timer = teleportDelay;
                        final Location teleportLocation = lastLoc.get(p.getUniqueId().toString());
                        final Location startLocation = p.getLocation();

                        @Override
                        public void run() {
                            if (!p.isOnline() || !compareLocations(startLocation, p.getLocation(), 0.5)) {
                                this.cancel();
                                if (p.isOnline())
                                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "(" + ChatColor.RED + "✖" + ChatColor.GRAY + ")"));
                                if (debug)
                                    Bukkit.getLogger().info("[DenyBack] Delayed teleportation canceled for " + p.getName() + ".");
                                return;
                            }
                            if (timer > 0) {
                                displayMessage = new StringBuilder(ChatColor.DARK_PURPLE + "");
                                for (int i = 0; i < ((double) timer / teleportDelay) * movingChars; i++)
                                    displayMessage.append("《");

                                displayMessage.append(" ").append(ChatColor.LIGHT_PURPLE).append(ChatColor.BOLD).append(timer).append(" ").append(ChatColor.DARK_PURPLE);

                                for (int i = 0; i < ((double) timer / teleportDelay) * movingChars; i++)
                                    displayMessage.append("》");

                                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(displayMessage.toString()));
                                timer--;
                            } else {
                                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GRAY + "(" + ChatColor.GREEN + "✓" + ChatColor.GRAY + ")"));
                                p.teleport(teleportLocation);
                                this.cancel();
                                if (debug)
                                    Bukkit.getLogger().info("[DenyBack] Teleporting player " + p.getName() + ".");
                            }

                        }
                    }.runTaskTimer(this, 0L, 20L);
                } else {
                    if (debug)
                        Bukkit.getLogger().info("[DenyBack] Teleporting player...");
                    p.teleport(lastLoc.get(p.getUniqueId().toString()));
                }
            } else {
                if (debug) {
                    Bukkit.getLogger().info("[DenyBack] Command registered: " + registerCommand);
                    Bukkit.getLogger().info("[DenyBack] Has back location: " + lastLoc.containsKey(p.getUniqueId().toString()));
                    Bukkit.getLogger().info("[DenyBack] Has denyback.back permission: " + p.hasPermission("denyback.back"));
                }
            }


        }


    }

    public boolean compareLocations(Location location1, Location location2, double distance) {
        if (location1.getWorld() == null || location2.getWorld() == null)
            return false;
        if (!location1.getWorld().equals(location2.getWorld()))
            return false;
        return location1.distance(location2) < distance;
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
            if (!sender.hasPermission("denyback.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (args.length != 1) {
                sender.sendMessage(prefix + ChatColor.GOLD + "Unknown Command!");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                ConfigOptions.loadConfig();
                sender.sendMessage(prefix + ChatColor.GREEN + "Reloaded DenyBack " + this.getDescription().getVersion() + ".");
            } else if (args[0].equalsIgnoreCase("debug")) {
                debug = !debug;
                sender.sendMessage(prefix + ChatColor.YELLOW + "Debug mode set to " + debug + ".");
            } else {
                sender.sendMessage(prefix + ChatColor.GOLD + "Unknown Command!");
            }
        } else if (command.getName().equalsIgnoreCase("dback")){
            if (!sender.hasPermission("denyback.back")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            if (!registerCommand) {
                sender.sendMessage(ChatColor.RED + "This command is not enabled!");
                return true;
            }
            if (!lastLoc.containsKey(((Player) sender).getUniqueId().toString())){
                sender.sendMessage(prefix + ChatColor.RED + "There is no place to return you to!");
                return true;
            }
            sender.sendMessage(prefix + ChatColor.RED + "This command is not in the config!");
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
            if (distance > minBackDistance) {
                if (!registerCommand || !lastAvailableLocation || !getBackFlag(event.getPlayer(), event.getFrom()) && playerTrusted(event.getPlayer(), event.getFrom())) {
                    lastLoc.put(event.getPlayer().getUniqueId().toString(), event.getFrom());
                    if (debug)
                        Bukkit.getLogger().info("[DenyBack] Teleport registered!");
                } else {
                    if (debug)
                        Bukkit.getLogger().info("[DenyBack] Player is not allowed back to this location. (not recording)");
                }
            }

        }

    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Player died: " + event.getEntity().getName() + " Has death permission: " + (event.getEntity().hasPermission(deathPermission) || deathPermission.isEmpty()));
        if (event.getEntity().hasPermission(deathPermission) || deathPermission.isEmpty()) {
            if (!registerCommand || !lastAvailableLocation || !getBackFlag(event.getEntity(), event.getEntity().getLocation()) && playerTrusted(event.getEntity(), event.getEntity().getLocation())) {
                lastLoc.put(event.getEntity().getUniqueId().toString(), event.getEntity().getLocation());
                if (debug)
                    Bukkit.getLogger().info("[DenyBack] Death registered!");
            } else {
                if (debug)
                    Bukkit.getLogger().info("[DenyBack] Player is not allowed back to this location. (not recording)");
            }
        }
    }

    public boolean getBackFlag(Player p, Location location) {
        if (MY_CUSTOM_FLAG == null){
            if (debug)
                Bukkit.getLogger().info("[DenyBack] deny-back flag has not been loaded! (a server restart is required)");
            return false;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(p);
        assert location.getWorld() != null;
        if (debug)
            Bukkit.getLogger().info("[DenyBack] Checking deny-back flag for " + p.getName() + " at " + Math.round(location.getX()) + " " + Math.round(location.getY()) + " " + Math.round(location.getZ()) + " " + location.getWorld().getName());

        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, BukkitAdapter.adapt(p.getWorld()))) {
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
        if (!denyBackClaims){
            if (debug)
                Bukkit.getLogger().info("[DenyBack] deny-untrusted-claims is set to false, ignoring check");
            return true;
        }
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
