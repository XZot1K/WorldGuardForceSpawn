package dev.zotware.plugins.wgfs;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class WorldGuardForceSpawn extends JavaPlugin implements Listener {

    public static StateFlag FORCE_SPAWN;

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            // create a flag with the name "force-spawn", defaulting to true
            StateFlag flag = new StateFlag("force-spawn", true);
            registry.register(flag);
            FORCE_SPAWN = flag; // only set our field if there was no error
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("force-spawn");
            if (existing instanceof StateFlag) {FORCE_SPAWN = (StateFlag) existing;}
        }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Register the event listener
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("WorldGuardFlagsPlugin has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("WorldGuardFlagsPlugin has been disabled.");
    }

    @EventHandler(priority = EventPriority.LOWEST) // actually highest under MONITOR
    public void onCreatureSpawn(CreatureSpawnEvent event) {

        List<String> allowedSpawnReasons = getConfig().getStringList("allowed-spawn-reasons");
        if (allowedSpawnReasons.isEmpty() || allowedSpawnReasons.contains(event.getSpawnReason().name())) {return;}

        // Check if the spawn location is in a region with the custom flag set to DENY
        Location loc = event.getLocation();
        if (loc.getWorld() == null) {return;}

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(loc.getWorld()));
        if (regionManager != null) {
            ApplicableRegionSet set = regionManager.getApplicableRegions(BukkitAdapter.asBlockVector(loc));
            if (!set.testState(null, FORCE_SPAWN)) {event.setCancelled(true);}
        }
    }
}