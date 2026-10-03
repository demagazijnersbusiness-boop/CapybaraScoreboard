package net.capybarasmp.modpanel;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

public final class StaffListener implements Listener {

    private final ModPanelPlugin plugin;
    private final StaffManager staff;
    private final StaffActions act;
    private boolean warnedOffline;

    public StaffListener(ModPanelPlugin plugin, StaffManager staff, StaffActions act) {
        this.plugin = plugin;
        this.staff = staff;
        this.act = act;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // auto-owner by name (only when the name can be trusted)
        List<String> owners = plugin.getConfig().getStringList("owners");
        boolean isListed = owners.stream().anyMatch(n -> n.equalsIgnoreCase(p.getName()));
        if (isListed && !staff.isOwner(p.getUniqueId())) {
            boolean trusted = Bukkit.getOnlineMode() || plugin.getConfig().getBoolean("trust-names-offline", false);
            if (trusted) {
                staff.addOwner(p.getUniqueId(), p.getName());
                act.msg(p, "<green>You are the <gold><bold>OWNER</bold> <green>of the Mod Panel. Use <white>/modpanel<green>.");
                plugin.getLogger().info(p.getName() + " was set as OWNER.");
            } else if (!warnedOffline) {
                warnedOffline = true;
                plugin.getLogger().warning("Server is in offline mode: owner auto-claim for '" + p.getName()
                        + "' is disabled for security. Run in the console: mod owner " + p.getName());
            }
        }

        // hide vanished staff from the player who just joined
        act.refreshVisibility();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (act.isVanished(p.getUniqueId())) {
            e.quitMessage(null); // vanished players leave silently
        }
        act.cleanup(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && act.isGod(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && act.isGod(p.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!act.isFrozen(e.getPlayer().getUniqueId())) return;
        if (e.getTo() == null) return;
        if (e.getFrom().getX() != e.getTo().getX()
                || e.getFrom().getY() != e.getTo().getY()
                || e.getFrom().getZ() != e.getTo().getZ()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (act.isMuted(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            act.msg(e.getPlayer(), "<red>You are muted.");
        }
    }
}
