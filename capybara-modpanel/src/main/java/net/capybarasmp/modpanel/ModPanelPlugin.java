package net.capybarasmp.modpanel;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class ModPanelPlugin extends JavaPlugin {

    private StaffManager staff;
    private StaffActions actions;

    public StaffManager staff() {
        return staff;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        staff = new StaffManager(this);
        staff.load();
        actions = new StaffActions(this);
        PanelGui gui = new PanelGui(this, staff, actions);

        getServer().getPluginManager().registerEvents(new StaffListener(this, staff, actions), this);
        getServer().getPluginManager().registerEvents(new PanelListener(gui), this);

        StaffCommands commands = new StaffCommands(staff, actions, gui);
        String[] names = {"mod", "modpanel", "fly", "god", "vanish", "spectate", "setarmor", "skellys", "heal", "feed",
                "gm", "goto", "bring", "freeze", "mute", "invsee", "speed", "sc"};
        for (String name : names) {
            getCommand(name).setExecutor(commands);
            getCommand(name).setTabCompleter(commands);
        }

        getLogger().info("Mod Panel ready. " + staff.owners().size() + " owner(s), " + staff.mods().size() + " moderator(s).");
    }

    @Override
    public void onDisable() {
        if (staff != null) staff.save();
        // make sure nobody stays invulnerable or invisible after a reload
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setInvulnerable(false);
            for (Player other : Bukkit.getOnlinePlayers()) {
                p.showPlayer(this, other);
            }
        }
    }
}
