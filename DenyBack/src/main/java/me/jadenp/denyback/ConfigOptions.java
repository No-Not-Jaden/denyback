package me.jadenp.denyback;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.md_5.bungee.api.ChatColor.COLOR_CHAR;

public class ConfigOptions {
    public static boolean registerCommand;
    public static boolean lastAvailableLocation;
    public static int teleportDelay;
    public static int minBackDistance;
    public static boolean griefPreventionEnabled;
    public static boolean denyBackClaims;
    public static String deathPermission;
    public static String denyMessage;
    public static boolean useGriefPreventionMessage;
    public static boolean denyNonMembers;

    public static void loadConfig() {
        Denyback db = Denyback.getInstance();

        if (!db.getConfig().isSet("back-on-death-permission"))
            db.getConfig().set("back-on-death-permission", "essentials.back.ondeath");
        if (!db.getConfig().isSet("deny-message"))
            db.getConfig().set("deny-message", "&c&lHey! &7Sorry, but you can't return to there.");
        if (!db.getConfig().isSet("use-grief-prevention-message"))
            db.getConfig().set("use-grief-prevention-message", true);
        if (!db.getConfig().isSet("deny-nonmembers"))
            db.getConfig().set("deny-nonmembers", false);
        if (!db.getConfig().isSet("back-command.register"))
            db.getConfig().set("back-command.register", false);
        if (!db.getConfig().isSet("back-command.use-last-available-location"))
            db.getConfig().set("back-command.use-last-available-location", true);
        if (!db.getConfig().isSet("back-command.teleport-delay"))
            db.getConfig().set("back-command.teleport-delay", 0);

        db.saveConfig();

        Denyback.aliases.clear();
        Denyback.aliases = db.getConfig().getStringList("deny-commands");
        denyBackClaims = db.getConfig().getBoolean("deny-untrusted-claims");
        deathPermission = db.getConfig().getString("back-on-death-permission");
        denyMessage = color(db.getConfig().getString("deny-message"));
        useGriefPreventionMessage = db.getConfig().getBoolean("use-grief-prevention-message");
        denyNonMembers = db.getConfig().getBoolean("deny-nonmembers");
        registerCommand = db.getConfig().getBoolean("back-command.register");
        lastAvailableLocation = db.getConfig().getBoolean("back-command.use-last-available-location");
        teleportDelay = db.getConfig().getInt("back-command.teleport-delay");

        Plugin plugin = Bukkit.getPluginManager().getPlugin("CMI");
        if (plugin != null) {
            minBackDistance = plugin.getConfig().getInt("Optimizations.Teleport.BackMinDistance");
            if (Denyback.debug)
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
}
