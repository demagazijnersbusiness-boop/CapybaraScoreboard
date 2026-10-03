package net.capybarasmp.modpanel;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Owners and moderators, saved in staff.yml (survives restarts). */
public final class StaffManager {

    private final ModPanelPlugin plugin;
    private final File file;
    private final Map<UUID, String> owners = new LinkedHashMap<>();
    private final Map<UUID, String> mods = new LinkedHashMap<>();

    public StaffManager(ModPanelPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "staff.yml");
    }

    public void load() {
        owners.clear();
        mods.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        read(y.getConfigurationSection("owners"), owners);
        read(y.getConfigurationSection("mods"), mods);
    }

    private void read(ConfigurationSection s, Map<UUID, String> into) {
        if (s == null) return;
        for (String key : s.getKeys(false)) {
            try {
                into.put(UUID.fromString(key), s.getString(key, "Unknown"));
            } catch (IllegalArgumentException ignored) {
                // skip broken entries
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        owners.forEach((id, name) -> y.set("owners." + id, name));
        mods.forEach((id, name) -> y.set("mods." + id, name));
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save staff.yml: " + e.getMessage());
        }
    }

    public boolean isOwner(UUID id) {
        return owners.containsKey(id);
    }

    /** Owners are also moderators. */
    public boolean isMod(UUID id) {
        return owners.containsKey(id) || mods.containsKey(id);
    }

    /** Console counts as staff. */
    public boolean isStaff(CommandSender s) {
        return !(s instanceof Player p) || isMod(p.getUniqueId());
    }

    public boolean isOwner(CommandSender s) {
        return !(s instanceof Player p) || isOwner(p.getUniqueId());
    }

    public void addOwner(UUID id, String name) {
        owners.put(id, name);
        mods.remove(id);
        save();
    }

    public void addMod(UUID id, String name) {
        if (owners.containsKey(id)) return;
        mods.put(id, name);
        save();
    }

    public boolean removeMod(UUID id) {
        boolean removed = mods.remove(id) != null;
        if (removed) save();
        return removed;
    }

    public Map<UUID, String> owners() {
        return Collections.unmodifiableMap(owners);
    }

    public Map<UUID, String> mods() {
        return Collections.unmodifiableMap(mods);
    }
}
