package net.capybarasmp.scoreboard;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** category == null means the main shop menu. */
public final class ShopHolder implements InventoryHolder {
    private final String category;
    private final int page;
    private final int offset;
    private Inventory inventory;

    public ShopHolder(String category, int page, int offset) {
        this.category = category;
        this.page = page;
        this.offset = offset;
    }

    public String category() { return category; }
    public int page() { return page; }
    public int offset() { return offset; }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
