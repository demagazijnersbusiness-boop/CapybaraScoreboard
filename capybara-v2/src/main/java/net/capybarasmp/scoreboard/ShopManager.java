package net.capybarasmp.scoreboard;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopManager {

    public record ShopItem(Material material, BigInteger price, boolean skelly) {}

    public record Category(String id, String name, Material icon, List<ShopItem> items) {}

    private final CapybaraScoreboard plugin;
    private final Map<Material, BigInteger> sellPrices = new HashMap<>();
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public ShopManager(CapybaraScoreboard plugin) {
        this.plugin = plugin;
    }

    public void load() {
        sellPrices.clear();
        categories.clear();

        File f = new File(plugin.getDataFolder(), "shop.yml");
        if (!f.exists()) plugin.saveResource("shop.yml", false);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);

        ConfigurationSection sell = y.getConfigurationSection("sell");
        if (sell != null) {
            for (String key : sell.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                BigInteger price = price(sell.getString(key));
                if (m == null || !m.isItem() || price == null) {
                    plugin.getLogger().warning("shop.yml: invalid sell entry '" + key + "'");
                    continue;
                }
                sellPrices.put(m, price);
            }
        }

        int percent = Math.max(0, y.getInt("sell-percent", 25));

        ConfigurationSection cats = y.getConfigurationSection("categories");
        if (cats != null) {
            for (String id : cats.getKeys(false)) {
                ConfigurationSection cs = cats.getConfigurationSection(id);
                if (cs == null) continue;
                Material icon = Material.matchMaterial(cs.getString("icon", "CHEST"));
                if (icon == null || !icon.isItem()) icon = Material.CHEST;
                List<ShopItem> items = new ArrayList<>();
                ConfigurationSection is = cs.getConfigurationSection("items");
                if (is != null) {
                    for (String key : is.getKeys(false)) {
                        BigInteger price = price(is.getString(key));
                        if (price == null) {
                            plugin.getLogger().warning("shop.yml: invalid price for '" + key + "' in " + id);
                            continue;
                        }
                        if (key.equalsIgnoreCase("SKELLY_SPAWNER")) {
                            items.add(new ShopItem(Material.SPAWNER, price, true));
                            continue;
                        }
                        Material m = Material.matchMaterial(key);
                        if (m == null || !m.isItem()) {
                            plugin.getLogger().warning("shop.yml: invalid shop entry '" + key + "' in " + id);
                            continue;
                        }
                        items.add(new ShopItem(m, price, false));
                        // automatic sell price when none is set
                        if (!sellPrices.containsKey(m) && percent > 0) {
                            BigInteger auto = price.multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100));
                            if (auto.signum() <= 0) auto = BigInteger.ONE;
                            sellPrices.put(m, auto);
                        }
                    }
                }
                categories.put(id, new Category(id, cs.getString("name", id), icon, items));
            }
        }
        plugin.getLogger().info("Loaded " + sellPrices.size() + " sell prices and " + categories.size() + " shop categories.");
    }

    private BigInteger price(String s) {
        if (s == null) return null;
        try {
            BigInteger b = MoneyUtil.parse(s);
            return b.signum() > 0 ? b : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public BigInteger sellPrice(Material m) {
        return sellPrices.get(m);
    }

    public Collection<Category> categories() {
        return categories.values();
    }

    public Category category(String id) {
        return categories.get(id);
    }

    public static String pretty(Material m) {
        String[] parts = m.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
