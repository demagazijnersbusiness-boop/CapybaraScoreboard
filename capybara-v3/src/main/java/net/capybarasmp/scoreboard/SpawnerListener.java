package net.capybarasmp.scoreboard;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class SpawnerListener implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final SpawnerManager spawners;

    public SpawnerListener(SpawnerManager spawners) {
        this.spawners = spawners;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!spawners.isSkellyItem(e.getItemInHand())) return;
        spawners.register(e.getBlockPlaced(), e.getPlayer());
        e.getPlayer().sendMessage(MM.deserialize("<green>Skelly Spawner placed! It now generates money for you."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        SpawnerManager.Spawner sp = spawners.get(b);
        if (sp == null) return;

        Player p = e.getPlayer();
        if (!p.getUniqueId().equals(sp.owner()) && !CapybaraScoreboard.isAdmin(p)) {
            e.setCancelled(true);
            p.sendMessage(MM.deserialize("<red>This is not your Skelly Spawner."));
            return;
        }

        e.setDropItems(false);
        e.setExpToDrop(0);
        spawners.unregister(b);

        ItemStack item = spawners.createItem(1);
        p.getInventory().addItem(item).values()
                .forEach(rest -> b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), rest));
        p.sendMessage(MM.deserialize("<green>You picked up the Skelly Spawner."));
    }

    // never let a Skelly Spawner spawn a mob
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(SpawnerSpawnEvent e) {
        CreatureSpawner cs = e.getSpawner();
        if (cs != null && spawners.isSkelly(cs.getBlock())) {
            e.setCancelled(true);
        }
    }

    // nobody can change it with a spawn egg
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        ItemStack item = e.getItem();
        if (b == null || item == null || !spawners.isSkelly(b)) return;
        if (item.getType().name().endsWith("SPAWN_EGG")) {
            e.setUseItemInHand(Event.Result.DENY);
            e.setCancelled(true);
        }
    }

    // explosions can't destroy Skelly Spawners
    @EventHandler(ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(spawners::isSkelly);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(spawners::isSkelly);
    }
}
