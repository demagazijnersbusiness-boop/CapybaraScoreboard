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
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ShopGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final int PER_PAGE = 45;

    private final ShopManager shop;
    private final Economy economy;
    private final SpawnerManager spawners;

    public ShopGui(ShopManager shop, Economy economy, SpawnerManager spawners) {
        this.shop = shop;
        this.economy = economy;
        this.spawners = spawners;
    }

    private static Component l(String s, TagResolver... r) {
        return MM.deserialize("<!italic>" + s, r);
    }

    private ItemStack named(Material m, String mini) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> meta.displayName(l(mini)));
        return it;
    }

    // ---------------------------------------------------------------- main menu

    public void openMenu(Player p) {
        List<ShopManager.Category> list = new ArrayList<>(shop.categories());
        int rows = Math.max(1, Math.min(6, (list.size() + 1 + 8) / 9));
        int size = rows * 9;
        ShopHolder h = new ShopHolder(null, 0, 0);
        Inventory inv = Bukkit.createInventory(h, size, MM.deserialize("<dark_gray>Shop"));
        h.setInventory(inv);

        for (int i = 0; i < list.size() && i < size - 1; i++) {
            ShopManager.Category c = list.get(i);
            ItemStack icon = new ItemStack(c.icon());
            icon.editMeta(m -> {
                m.displayName(l("<yellow><bold>" + c.name()));
                m.lore(List.of(l("<gray>Click to open")));
            });
            inv.setItem(i, icon);
        }

        ItemStack bal = named(Material.GOLD_INGOT, "<green><bold>Balance");
        String amount = MoneyUtil.format(economy.balance(p.getUniqueId()));
        bal.editMeta(m -> m.lore(List.of(l("<white>$<amt>", Placeholder.unparsed("amt", amount)))));
        inv.setItem(size - 1, bal);
        p.openInventory(inv);
    }

    public void handleMenuClick(Player p, int slot, int size) {
        List<ShopManager.Category> list = new ArrayList<>(shop.categories());
        if (slot < 0 || slot >= size - 1 || slot >= list.size()) return;
        openCategory(p, list.get(slot), 0);
    }

    // ---------------------------------------------------------------- category page

    private int pages(ShopManager.Category c) {
        return Math.max(1, (c.items().size() + PER_PAGE - 1) / PER_PAGE);
    }

    public void openCategory(Player p, ShopManager.Category c, int page) {
        List<ShopManager.ShopItem> items = c.items();
        int pages = pages(c);
        page = Math.max(0, Math.min(page, pages - 1));
        int from = page * PER_PAGE;
        int count = Math.max(0, Math.min(PER_PAGE, items.size() - from));

        int size;
        int offset;
        if (pages == 1 && items.size() <= 9) {
            size = 27;   // 3 rows: items in the middle row, back button bottom-left
            offset = 9;
        } else {
            int rows = Math.max(2, Math.min(6, (count + 8) / 9 + 1));
            size = rows * 9;
            offset = 0;
        }

        String title = "<dark_gray>Shop - " + c.name() + (pages > 1 ? " <gray>(" + (page + 1) + "/" + pages + ")" : "");
        ShopHolder h = new ShopHolder(c.id(), page, offset);
        Inventory inv = Bukkit.createInventory(h, size, MM.deserialize(title));
        h.setInventory(inv);

        for (int i = 0; i < count; i++) {
            inv.setItem(offset + i, display(items.get(from + i)));
        }

        int back = size - 9;
        inv.setItem(back, named(Material.RED_STAINED_GLASS_PANE, "<red><bold>Back"));
        if (page > 0) inv.setItem(back + 3, named(Material.ARROW, "<yellow>Previous page"));
        if (page < pages - 1) inv.setItem(back + 5, named(Material.ARROW, "<yellow>Next page"));
        p.openInventory(inv);
    }

    private ItemStack display(ShopManager.ShopItem si) {
        ItemStack it = si.skelly() ? spawners.createItem(1) : new ItemStack(si.material());
        List<Component> lore = new ArrayList<>();
        if (si.skelly()) {
            ItemMeta existing = it.getItemMeta();
            if (existing != null && existing.lore() != null) {
                lore.addAll(existing.lore());
                lore.add(Component.empty());
            }
        }
        String each = MoneyUtil.format(si.price());
        lore.add(l("<gray>Price: <green>$<p> <dark_gray>each", Placeholder.unparsed("p", each)));
        lore.add(Component.empty());
        lore.add(l("<yellow>Left-click <gray>» Buy <white>1 <dark_gray>($<c>)", Placeholder.unparsed("c", each)));
        if (!si.skelly() && si.material().getMaxStackSize() > 1) {
            lore.add(l("<yellow>Right-click <gray>» Buy <white>16 <dark_gray>($<c>)",
                    Placeholder.unparsed("c", MoneyUtil.format(si.price().multiply(BigInteger.valueOf(16))))));
            lore.add(l("<yellow>Shift-click <gray>» Buy <white>64 <dark_gray>($<c>)",
                    Placeholder.unparsed("c", MoneyUtil.format(si.price().multiply(BigInteger.valueOf(64))))));
        }
        it.editMeta(m -> m.lore(lore));
        return it;
    }

    public void handleCategoryClick(Player p, ShopManager.Category c, ShopHolder h, int slot, boolean shift, boolean right) {
        int size = h.getInventory().getSize();
        int back = size - 9;
        int pages = pages(c);

        if (slot == back) {
            openMenu(p);
            return;
        }
        if (slot == back + 3 && h.page() > 0) {
            openCategory(p, c, h.page() - 1);
            return;
        }
        if (slot == back + 5 && h.page() < pages - 1) {
            openCategory(p, c, h.page() + 1);
            return;
        }
        if (slot >= back) return;

        int onPage = slot - h.offset();
        if (onPage < 0 || onPage >= PER_PAGE) return;
        int idx = h.page() * PER_PAGE + onPage;
        if (idx >= c.items().size()) return;

        ShopManager.ShopItem si = c.items().get(idx);
        int qty = shift ? 64 : right ? 16 : 1;
        if (si.skelly() || si.material().getMaxStackSize() == 1) qty = 1;
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
        ItemStack give = si.skelly() ? spawners.createItem(qty) : new ItemStack(si.material(), qty);
        Map<Integer, ItemStack> left = p.getInventory().addItem(give);
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
            String name = si.skelly() ? "Skelly Spawner" : ShopManager.pretty(si.material());
            p.sendMessage(MM.deserialize("<green>Bought <white><n>x <item> <green>for <yellow>$<amt><green>.",
                    Placeholder.unparsed("n", String.valueOf(bought)),
                    Placeholder.unparsed("item", name),
                    Placeholder.unparsed("amt", MoneyUtil.format(paid))));
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        }
    }

    // ---------------------------------------------------------------- sell window

    public void openSell(Player p) {
        SellHolder h = new SellHolder();
        Inventory inv = Bukkit.createInventory(h, 36, MM.deserialize("<dark_gray>Sell <gray>- close to sell"));
        h.setInventory(inv);
        p.openInventory(inv);
    }
}
