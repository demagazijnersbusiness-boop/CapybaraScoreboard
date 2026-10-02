package net.capybarasmp.scoreboard;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public final class SellService {

    public record Result(long items, BigInteger total) {}

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ShopManager shop;
    private final Economy economy;

    public SellService(ShopManager shop, Economy economy) {
        this.shop = shop;
        this.economy = economy;
    }

    /** Removes (sets to null) every sellable stack in the array and returns what it was worth. */
    public Result sell(ItemStack[] stacks) {
        long items = 0;
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < stacks.length; i++) {
            ItemStack s = stacks[i];
            if (s == null || s.getType().isAir() || s.hasItemMeta()) continue;
            BigInteger price = shop.sellPrice(s.getType());
            if (price == null) continue;
            total = total.add(price.multiply(BigInteger.valueOf(s.getAmount())));
            items += s.getAmount();
            stacks[i] = null;
        }
        return new Result(items, total);
    }

    public void payout(Player p, Result r) {
        if (r.items() == 0) {
            p.sendMessage(MM.deserialize("<red>You have nothing sellable."));
            return;
        }
        economy.add(p.getUniqueId(), r.total());
        p.sendMessage(MM.deserialize("<green>Sold <white><n> items <green>for <yellow>$<amt><green>.",
                Placeholder.unparsed("n", String.valueOf(r.items())),
                Placeholder.unparsed("amt", MoneyUtil.format(r.total()))));
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
}
