package net.capybarasmp.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Skelly Spawner: looks like a skeleton spawner, but never spawns mobs.
 * It pays its owner money every few seconds. Placed spawners are saved in spawners.yml.
 */
public final class SpawnerManager {

    public record Spawner(UUID owner, String ownerName, String world, int x, int y, int z) {
        public String key() {
            return world + ";" + x + ";" + y + ";" + z;
        }
    }

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final String TAG = "skelly_holo";

    private final CapybaraScoreboard plugin;
    private final Economy economy;
    private final File file;
    private final NamespacedKey itemKey;
    private final Map<String, Spawner> spawners = new HashMap<>();
    private BukkitTask task;

    public SpawnerManager(CapybaraScoreboard plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "spawners.yml");
        this.itemKey = new NamespacedKey(plugin, "skelly_spawner");
    }

    // ------------------------------------------------------------ config values

    private BigInteger amount() {
        try {
            BigInteger b = MoneyUtil.parse(plugin.getConfig().getString("skelly-spawner.amount", "25"));
            return b.signum() > 0 ? b : BigInteger.valueOf(25);
        } catch (NumberFormatException e) {
            return BigInteger.valueOf(25);
        }
    }

    private int interval() {
        return Math.max(1, plugin.getConfig().getInt("skelly-spawner.interval-seconds", 10));
    }

    // ------------------------------------------------------------ item

    public ItemStack createItem(int amount) {
        ItemStack it = new ItemStack(Material.SPAWNER, amount);
        String amt = MoneyUtil.format(amount());
        String sec = String.valueOf(interval());
        it.editMeta(m -> {
            m.displayName(MM.deserialize("<!italic><white><bold>SKELLY SPAWNER"));
            m.lore(List.of(
                    MM.deserialize("<!italic><gray>Place it down to generate money."),
                    MM.deserialize("<!italic><green>+$<amt> <gray>/ <sec> seconds",
                            Placeholder.unparsed("amt", amt), Placeholder.unparsed("sec", sec)),
                    MM.deserialize("<!italic><dark_gray>No mobs spawn from it.")));
            m.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        });
        return it;
    }

    public boolean isSkellyItem(ItemStack it) {
        if (it == null || it.getType() != Material.SPAWNER || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------ registry

    private static String key(Location l) {
        return l.getWorld().getName() + ";" + l.getBlockX() + ";" + l.getBlockY() + ";" + l.getBlockZ();
    }

    public Spawner get(Block b) {
        return spawners.get(key(b.getLocation()));
    }

    public boolean isSkelly(Block b) {
        return spawners.containsKey(key(b.getLocation()));
    }

    public void register(Block b, Player owner) {
        Spawner sp = new Spawner(owner.getUniqueId(), owner.getName(), b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        spawners.put(sp.key(), sp);

        BlockState st = b.getState();
        if (st instanceof CreatureSpawner cs) {
            cs.setSpawnedType(EntityType.SKELETON); // only for the spinning skeleton visual
            cs.setSpawnCount(0);
            cs.setMaxNearbyEntities(0);
            cs.update();
        }
        save();
        ensureHologram(sp);
    }

    public void unregister(Block b) {
        Spawner sp = spawners.remove(key(b.getLocation()));
        if (sp != null) {
            removeHologram(sp);
            save();
        }
    }

    // ------------------------------------------------------------ saving

    public void load() {
        spawners.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("spawners");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            ConfigurationSection c = s.getConfigurationSection(k);
            if (c == null) continue;
            try {
                Spawner sp = new Spawner(UUID.fromString(c.getString("owner", "")), c.getString("name", "Unknown"),
                        c.getString("world", "world"), c.getInt("x"), c.getInt("y"), c.getInt("z"));
                spawners.put(sp.key(), sp);
            } catch (IllegalArgumentException ignored) {
                // skip broken entries
            }
        }
        plugin.getLogger().info("Loaded " + spawners.size() + " Skelly Spawners.");
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        int i = 0;
        for (Spawner sp : spawners.values()) {
            String base = "spawners." + (i++);
            y.set(base + ".owner", sp.owner().toString());
            y.set(base + ".name", sp.ownerName());
            y.set(base + ".world", sp.world());
            y.set(base + ".x", sp.x());
            y.set(base + ".y", sp.y());
            y.set(base + ".z", sp.z());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save spawners.yml: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ payout task

    public void start() {
        if (task != null) task.cancel();
        long ticks = interval() * 20L;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, ticks, ticks);
    }

    private boolean loaded(World w, Spawner sp) {
        return w != null && w.isChunkLoaded(sp.x() >> 4, sp.z() >> 4);
    }

    private void tick() {
        BigInteger amount = amount();
        boolean particles = plugin.getConfig().getBoolean("skelly-spawner.particles", true);
        Map<UUID, BigInteger> pay = new HashMap<>();
        List<Spawner> dead = new ArrayList<>();

        for (Spawner sp : spawners.values()) {
            World w = Bukkit.getWorld(sp.world());
            if (loaded(w, sp)) {
                if (w.getBlockAt(sp.x(), sp.y(), sp.z()).getType() != Material.SPAWNER) {
                    dead.add(sp); // block was removed by something else
                    continue;
                }
                ensureHologram(sp);
                if (particles) {
                    w.spawnParticle(Particle.HAPPY_VILLAGER, new Location(w, sp.x() + 0.5, sp.y() + 1.1, sp.z() + 0.5),
                            6, 0.3, 0.15, 0.3, 0);
                }
            }
            pay.merge(sp.owner(), amount, BigInteger::add);
        }

        for (Spawner sp : dead) {
            removeHologram(sp);
            spawners.remove(sp.key());
        }
        if (!dead.isEmpty()) save();

        for (Map.Entry<UUID, BigInteger> e : pay.entrySet()) {
            economy.add(e.getKey(), e.getValue());
        }
    }

    // ------------------------------------------------------------ hologram

    private Location center(Spawner sp) {
        World w = Bukkit.getWorld(sp.world());
        if (w == null) return null;
        return new Location(w, sp.x() + 0.5, sp.y() + 1.35, sp.z() + 0.5);
    }

    private List<Entity> findHolograms(Location c) {
        List<Entity> out = new ArrayList<>();
        Collection<Entity> near = c.getWorld().getNearbyEntities(c, 0.6, 0.3, 0.6);
        for (Entity e : near) {
            if (e instanceof TextDisplay && e.getScoreboardTags().contains(TAG)) out.add(e);
        }
        return out;
    }

    private Component hologramText(Spawner sp) {
        return MM.deserialize(
                "<white><bold>SKELLY SPAWNER</bold><newline><green>+$<amt> <gray>/ <sec> seconds<newline><gray>Owner: <white><owner>",
                Placeholder.unparsed("amt", MoneyUtil.format(amount())),
                Placeholder.unparsed("sec", String.valueOf(interval())),
                Placeholder.unparsed("owner", sp.ownerName()));
    }

    public void ensureHologram(Spawner sp) {
        if (!plugin.getConfig().getBoolean("skelly-spawner.hologram", true)) return;
        Location c = center(sp);
        if (c == null || !loaded(c.getWorld(), sp)) return;
        if (!findHolograms(c).isEmpty()) return;
        c.getWorld().spawn(c, TextDisplay.class, td -> {
            td.text(hologramText(sp));
            td.setBillboard(Display.Billboard.CENTER);
            td.addScoreboardTag(TAG);
        });
    }

    public void removeHologram(Spawner sp) {
        Location c = center(sp);
        if (c == null || !loaded(c.getWorld(), sp)) return;
        for (Entity e : findHolograms(c)) e.remove();
    }

    /** Re-creates every hologram (used after /csb reload so new config values show up). */
    public void refreshHolograms() {
        for (Spawner sp : new ArrayList<>(spawners.values())) {
            removeHologram(sp);
            ensureHologram(sp);
        }
    }
}
