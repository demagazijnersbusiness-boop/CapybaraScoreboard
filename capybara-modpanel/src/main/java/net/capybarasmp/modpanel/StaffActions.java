package net.capybarasmp.modpanel;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** All staff features live here; commands and the GUI both call these methods. */
public final class StaffActions {

    public record SavedState(GameMode mode, Location location) {}

    public static final String[] TIERS = {"leather", "chainmail", "iron", "gold", "diamond", "netherite"};

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ModPanelPlugin plugin;
    private final Set<UUID> god = new HashSet<>();
    private final Set<UUID> vanished = new HashSet<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Set<UUID> muted = new HashSet<>();
    private final Map<UUID, SavedState> spectating = new HashMap<>();

    public StaffActions(ModPanelPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ messages

    public void msg(CommandSender to, String mini, TagResolver... r) {
        to.sendMessage(MM.deserialize(plugin.getConfig().getString("prefix", "<red>[Mod] <gray>") + mini, r));
    }

    // ------------------------------------------------------------ state queries

    public boolean isGod(UUID id) { return god.contains(id); }
    public boolean isVanished(UUID id) { return vanished.contains(id); }
    public boolean isFrozen(UUID id) { return frozen.contains(id); }
    public boolean isMuted(UUID id) { return muted.contains(id); }
    public boolean isSpectating(UUID id) { return spectating.containsKey(id); }

    // ------------------------------------------------------------ toggles

    public boolean toggleFly(Player t) {
        boolean on = !t.getAllowFlight();
        t.setAllowFlight(on);
        if (on) t.setFlying(true);
        return on;
    }

    public boolean toggleGod(Player t) {
        boolean on;
        if (god.remove(t.getUniqueId())) {
            on = false;
        } else {
            god.add(t.getUniqueId());
            on = true;
        }
        t.setInvulnerable(on);
        return on;
    }

    public boolean toggleVanish(Player t) {
        boolean on;
        if (vanished.remove(t.getUniqueId())) {
            on = false;
        } else {
            vanished.add(t.getUniqueId());
            on = true;
        }
        refreshVisibility();
        return on;
    }

    public boolean toggleFreeze(Player t) {
        if (frozen.remove(t.getUniqueId())) return false;
        frozen.add(t.getUniqueId());
        return true;
    }

    public boolean toggleMute(Player t) {
        if (muted.remove(t.getUniqueId())) return false;
        muted.add(t.getUniqueId());
        return true;
    }

    /** Vanished players are hidden for everyone who is not staff. */
    public void refreshVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean staff = plugin.staff().isMod(viewer.getUniqueId());
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target.equals(viewer) || !vanished.contains(target.getUniqueId())) continue;
                if (staff) viewer.showPlayer(plugin, target);
                else viewer.hidePlayer(plugin, target);
            }
        }
    }

    public void cleanup(Player p) {
        UUID id = p.getUniqueId();
        god.remove(id);
        vanished.remove(id);
        frozen.remove(id);
        spectating.remove(id);
        p.setInvulnerable(false);
        // "muted" stays until restart on purpose, so leaving and rejoining does not undo a mute
    }

    // ------------------------------------------------------------ spectate

    public void spectate(Player p, Player t) {
        spectating.putIfAbsent(p.getUniqueId(), new SavedState(p.getGameMode(), p.getLocation()));
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(t.getLocation());
        Bukkit.getScheduler().runTask(plugin, () -> p.setSpectatorTarget(t));
    }

    public boolean stopSpectate(Player p) {
        SavedState s = spectating.remove(p.getUniqueId());
        if (s == null) return false;
        p.setSpectatorTarget(null);
        p.setGameMode(s.mode());
        p.teleport(s.location());
        return true;
    }

    // ------------------------------------------------------------ simple actions

    public void heal(Player t) {
        AttributeInstance max = t.getAttribute(Attribute.MAX_HEALTH);
        t.setHealth(max != null ? max.getValue() : 20.0);
        t.setFoodLevel(20);
        t.setSaturation(20f);
        t.setFireTicks(0);
        for (PotionEffect e : t.getActivePotionEffects()) t.removePotionEffect(e.getType());
    }

    public void feed(Player t) {
        t.setFoodLevel(20);
        t.setSaturation(20f);
    }

    public GameMode cycleGamemode(Player t) {
        GameMode[] order = {GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE, GameMode.SPECTATOR};
        int idx = 0;
        for (int i = 0; i < order.length; i++) if (order[i] == t.getGameMode()) idx = i;
        GameMode next = order[(idx + 1) % order.length];
        t.setGameMode(next);
        return next;
    }

    public static GameMode parseGamemode(String s) {
        return switch (s.toLowerCase()) {
            case "0", "s", "survival" -> GameMode.SURVIVAL;
            case "1", "c", "creative" -> GameMode.CREATIVE;
            case "2", "a", "adventure" -> GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    /** level 1-10 (2 = normal speed). */
    public void setSpeed(Player t, int level) {
        level = Math.max(1, Math.min(10, level));
        t.setWalkSpeed(Math.min(1f, 0.1f * level));
        t.setFlySpeed(Math.min(1f, 0.05f * level));
    }

    public void resetSpeed(Player t) {
        t.setWalkSpeed(0.2f);
        t.setFlySpeed(0.1f);
    }

    // ------------------------------------------------------------ armor

    public static boolean isTier(String s) {
        for (String t : TIERS) if (t.equalsIgnoreCase(s) || (s.equalsIgnoreCase("golden") && t.equals("gold"))) return true;
        return false;
    }

    private Enchantment enchant(String key) {
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    private ItemStack piece(String tier, String part, boolean max) {
        String prefix = tier.equalsIgnoreCase("gold") || tier.equalsIgnoreCase("golden") ? "GOLDEN" : tier.toUpperCase();
        Material m = Material.matchMaterial(prefix + "_" + part);
        if (m == null) return null;
        ItemStack it = new ItemStack(m);
        if (max) {
            Enchantment prot = enchant("protection");
            Enchantment unb = enchant("unbreaking");
            Enchantment mend = enchant("mending");
            if (prot != null) it.addUnsafeEnchantment(prot, 4);
            if (unb != null) it.addUnsafeEnchantment(unb, 3);
            if (mend != null) it.addUnsafeEnchantment(mend, 1);
        }
        return it;
    }

    public boolean setArmor(Player t, String tier, boolean max) {
        ItemStack h = piece(tier, "HELMET", max);
        ItemStack c = piece(tier, "CHESTPLATE", max);
        ItemStack l = piece(tier, "LEGGINGS", max);
        ItemStack b = piece(tier, "BOOTS", max);
        if (h == null || c == null || l == null || b == null) return false;
        t.getInventory().setHelmet(h);
        t.getInventory().setChestplate(c);
        t.getInventory().setLeggings(l);
        t.getInventory().setBoots(b);
        return true;
    }

    // ------------------------------------------------------------ skelly spawners (needs CapybaraScoreboard)

    public boolean skellyAvailable() {
        var pl = Bukkit.getPluginManager().getPlugin("CapybaraScoreboard");
        return pl != null && pl.isEnabled();
    }

    public boolean giveSkelly(Player t, int amount) {
        if (!skellyAvailable()) return false;
        amount = Math.max(1, Math.min(64, amount));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "skelly give " + t.getName() + " " + amount);
        return true;
    }

    // ------------------------------------------------------------ teleport

    public void goTo(Player p, Player t) {
        p.teleport(t.getLocation());
    }

    public void bring(Player p, Player t) {
        t.teleport(p.getLocation());
    }
}
