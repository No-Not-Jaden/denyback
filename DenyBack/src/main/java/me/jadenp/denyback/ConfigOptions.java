package me.jadenp.denyback;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class ConfigOptions {
    private boolean registerCommand;
    private boolean lastAvailableLocation;
    private int teleportDelay;
    private int minBackDistance;
    private boolean griefPreventionEnabled;
    private boolean denyBackClaims;
    private String deathPermission;
    private String denyMessage;
    private boolean useGriefPreventionMessage;
    private boolean denyNonMembers;
    private String backMessage;
    private List<String> aliases = new ArrayList<>();

    public void loadConfig(Denyback db) {
        griefPreventionEnabled = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention") != null;

        db.reloadConfig();

        // fill in any default options that aren't present
        if (db.getResource("config.yml") != null) {
            db.getConfig().setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(db.getResource("config.yml")))));
            for (String key : Objects.requireNonNull(db.getConfig().getDefaults()).getKeys(true)) {
                if (!db.getConfig().isSet(key))
                    db.getConfig().set(key, db.getConfig().getDefaults().get(key));
            }
        }

        db.saveConfig();

        aliases.clear();
        aliases = db.getConfig().getStringList("deny-commands");
        denyBackClaims = db.getConfig().getBoolean("deny-untrusted-claims");
        deathPermission = db.getConfig().getString("back-on-death-permission");
        denyMessage = color(db.getConfig().getString("deny-message"));
        useGriefPreventionMessage = db.getConfig().getBoolean("use-grief-prevention-message");
        denyNonMembers = db.getConfig().getBoolean("deny-nonmembers");
        registerCommand = db.getConfig().getBoolean("back-command.register");
        lastAvailableLocation = db.getConfig().getBoolean("back-command.use-last-available-location");
        teleportDelay = db.getConfig().getInt("back-command.teleport-delay");
        backMessage = color(db.getConfig().getString("back-command.message"));
        int autoSaveInterval = db.getConfig().getInt("auto-save-interval");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        if (plugin != null) {
            minBackDistance = plugin.getConfig().getInt("Optimizations.Teleport.BackMinDistance");
            if (db.isDebug())
                Bukkit.getLogger().info(() -> "[DenyBack] CMI registered, BackMinDistance: " + minBackDistance);
        } else {
            minBackDistance = 0;
        }

        db.loadSaveTask(autoSaveInterval);
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

    public List<String> getAliases() {
        return aliases;
    }

    public boolean isRegisterCommand() {
        return registerCommand;
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public boolean notLastAvailableLocation() {
        return !lastAvailableLocation;
    }

    public int getMinBackDistance() {
        return minBackDistance;
    }

    public String getBackMessage() {
        return backMessage;
    }

    public String getDeathPermission() {
        return deathPermission;
    }

    public String getDenyMessage() {
        return denyMessage;
    }

    public boolean isDenyBackClaims() {
        return denyBackClaims;
    }

    public boolean isDenyNonMembers() {
        return denyNonMembers;
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionEnabled;
    }

    public boolean isUseGriefPreventionMessage() {
        return useGriefPreventionMessage;
    }
}
