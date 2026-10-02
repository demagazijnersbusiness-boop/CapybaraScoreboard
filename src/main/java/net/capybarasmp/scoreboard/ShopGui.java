package net.capybarasmp.scoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopGui {

    public static final int[] MENU_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    public static final int BACK_SLOT = 49;

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ShopManager shop;
    private final Economy economy;

    public ShopGui(ShopManager shop, Economy economy) {
        this.shop = shop;
        this.economy = economy;
    }

    private static Component l(String s, TagResolver... r) {
        return MM.deserialize("<!italic>" + s, r);
    }

    private void fill(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        pane.editMeta(m -> m.displayName(Component.text(" ")));
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    public void openMenu(Player p) {
        ShopHolder h = new ShopHolder(null);
        Inventory inv = Bukkit.createInventory(h, 36, MM.deserialize("<dark_gray>Shop"));
        h.setInventory(inv);
        fill(inv);
        int i = 0;
        for (ShopManager.Category c : shop.categories()) {
            if (i >= MENU_SLOTS.length) break;
            ItemStack icon = new ItemStack(c.icon());
            icon.editMeta(m -> {
                m.displayName(l("<yellow><bold>" + c.name()));
                m.lore(List.of(l("<gray>Click to open")));
            });
            inv.setItem(MENU_SLOTS[i++], icon);
        }
        p.openInventory(inv);
    }

    public void openCategory(Player p, ShopManager.Category c) {
        ShopHolder h = new ShopHolder(c.id());
        Inventory inv = Bukkit.createInventory(h, 54, MM.deserialize("<dark_gray>Shop <gray>» <dark_gray>" + c.name()));
        h.setInventory(inv);
        fill(inv);

        for (int i = 0; i < c.items().size() && i < 45; i++) {
            ShopManager.ShopItem si = c.items().get(i);
            ItemStack it = new ItemStack(si.material());
            List<Component> lore = new ArrayList<>();
            lore.add(l("<gray>Price: <green>$<p> <dark_gray>each", Placeholder.unparsed("p", MoneyUtil.format(si.price()))));
            lore.add(Component.empty());
            lore.add(l("<yellow>Left-click <gray>» Buy <white>1 <dark_gray>($<c>)",
                    Placeholder.unparsed("c", MoneyUtil.format(si.price()))));
            if (si.material().getMaxStackSize() > 1) {
                lore.add(l("<yellow>Right-click <gray>» Buy <white>16 <dark_gray>($<c>)",
                        Placeholder.unparsed("c", MoneyUtil.format(si.price().multiply(BigInteger.valueOf(16))))));
                lore.add(l("<yellow>Shift-click <gray>» Buy <white>64 <dark_gray>($<c>)",
                        Placeholder.unparsed("c", MoneyUtil.format(si.price().multiply(BigInteger.valueOf(64))))));
            }
            it.editMeta(m -> m.lore(lore));
            inv.setItem(i, it);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        back.editMeta(m -> m.displayName(l("<red>Back")));
        inv.setItem(BACK_SLOT, back);
        p.openInventory(inv);
    }

    public void handleMenuClick(Player p, int slot) {
        int idx = -1;
        for (int i = 0; i < MENU_SLOTS.length; i++) {
            if (MENU_SLOTS[i] == slot) idx = i;
        }
        if (idx < 0) return;
        List<ShopManager.Category> list = new ArrayList<>(shop.categories());
        if (idx < list.size()) openCategory(p, list.get(idx));
    }

    public void handleCategoryClick(Player p, ShopManager.Category c, int slot, boolean shift, boolean right) {
        if (slot == BACK_SLOT) {
            openMenu(p);
            return;
        }
        if (slot < 0 || slot >= 45 || slot >= c.items().size()) return;
        ShopManager.ShopItem si = c.items().get(slot);
        int qty = shift ? 64 : right ? 16 : 1;
        if (si.material().getMaxStackSize() == 1) qty = 1;
        buy(p, si, qty);
    }

    private void buy(Player p, ShopManager.ShopItem si, int qty) {
        BigInteger cost = si.price().multiply(BigInteger.valueOf(qty));
        if (!economy.take(p.getUniqueId(), cost)) {
            p.sendMessage(MM.deserialize("<red>Not enough money! You need <yellow>$<amt><red>.",
                    Placeholder.unparsed("amt", MoneyUtil.format(cost))));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(si.material(), qty));
        int leftover = 0;
        for (ItemStack s : left.values()) leftover += s.getAmount();
        int bought = qty - leftover;
        BigInteger paid = cost;
        if (leftover > 0) {
            BigInteger refund = si.price().multiply(BigInteger.valueOf(leftover));
            economy.add(p.getUniqueId(), refund);
            paid = cost.subtract(refund);
            p.sendMessage(MM.deserialize("<red>Your inventory is full!"));
        }
        if (bought > 0) {
            p.sendMessage(MM.deserialize("<green>Bought <white><n>x <item> <green>for <yellow>$<amt><green>.",
                    Placeholder.unparsed("n", String.valueOf(bought)),
                    Placeholder.unparsed("item", ShopManager.pretty(si.material())),
                    Placeholder.unparsed("amt", MoneyUtil.format(paid))));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    public void openSell(Player p) {
        SellHolder h = new SellHolder();
        Inventory inv = Bukkit.createInventory(h, 36, MM.deserialize("<dark_gray>Sell <gray>- close to sell"));
        h.setInventory(inv);
        p.openInventory(inv);
    }
}
