package me.jadenp.denyback;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Claims {
    public Claims(){

    }

    public boolean playerTrusted(Player p, Location location){
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location,false,null);
        if (claim == null)
            return true;
        String perms = claim.allowAccess(p);
        if (perms == null)
            return true;
        p.sendMessage(ChatColor.RED + perms);



        return false;
    }
}
