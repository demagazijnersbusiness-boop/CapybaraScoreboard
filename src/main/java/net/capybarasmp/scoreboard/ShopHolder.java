package net.capybarasmp.scoreboard;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** category == null means the main shop menu. */
public final class ShopHolder implements InventoryHolder {
    private final String category;
    private Inventory inventory;

    public ShopHolder(String category) {
        this.category = category;
    }

    public String category() {
        return category;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
