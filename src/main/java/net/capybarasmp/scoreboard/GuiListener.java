package net.capybarasmp.scoreboard;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class GuiListener implements Listener {

    private final ShopManager shop;
    private final ShopGui gui;
    private final SellService sell;

    public GuiListener(ShopManager shop, ShopGui gui, SellService sell) {
        this.shop = shop;
        this.gui = gui;
        this.sell = sell;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof ShopHolder holder)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || !e.getClickedInventory().equals(top)) return;

        if (holder.category() == null) {
            gui.handleMenuClick(p, e.getSlot());
        } else {
            ShopManager.Category c = shop.category(holder.category());
            if (c != null) {
                gui.handleCategoryClick(p, c, e.getSlot(), e.isShiftClick(), e.isRightClick());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (!(inv.getHolder() instanceof SellHolder)) return;
        if (!(e.getPlayer() instanceof Player p)) return;

        ItemStack[] contents = inv.getContents();
        SellService.Result result = sell.sell(contents);
        inv.clear();

        // give back everything that could not be sold
        for (ItemStack s : contents) {
            if (s == null || s.getType().isAir()) continue;
            p.getInventory().addItem(s).values()
                    .forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
        }
        sell.payout(p, result);
    }
}
