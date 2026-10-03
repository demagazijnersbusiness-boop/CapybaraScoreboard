package net.capybarasmp.modpanel;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class PanelHolder implements InventoryHolder {

    public enum Type { MAIN, PICKER, TARGET, ARMOR }

    private final Type type;
    private final UUID target;
    private final int page;
    private final boolean spectatePick;
    private Inventory inventory;

    public PanelHolder(Type type, UUID target, int page, boolean spectatePick) {
        this.type = type;
        this.target = target;
        this.page = page;
        this.spectatePick = spectatePick;
    }

    public Type type() { return type; }
    public UUID target() { return target; }
    public int page() { return page; }
    public boolean spectatePick() { return spectatePick; }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
