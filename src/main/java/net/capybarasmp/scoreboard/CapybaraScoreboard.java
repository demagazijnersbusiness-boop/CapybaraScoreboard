package net.capybarasmp.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CapybaraScoreboard extends JavaPlugin {

    private DataManager data;
    private BoardManager boards;
    private ShopManager shop;

    public ShopManager getShopManager() {
        return shop;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        data = new DataManager(this);
        data.load();
        boards = new BoardManager(this, data);
        Economy economy = new Economy(data, boards);
        shop = new ShopManager(this);
        shop.load();
        SellService sellService = new SellService(shop, economy);
        ShopGui gui = new ShopGui(shop, economy);

        getServer().getPluginManager().registerEvents(new StatsListener(this, data, boards), this);
        getServer().getPluginManager().registerEvents(new GuiListener(shop, gui, sellService), this);

        CommandHandler handler = new CommandHandler(this, data, boards);
        getCommand("lives").setExecutor(handler);
        getCommand("lives").setTabCompleter(handler);
        getCommand("capybarascoreboard").setExecutor(handler);
        getCommand("capybarascoreboard").setTabCompleter(handler);

        EconomyCommands ec = new EconomyCommands(data, economy, gui, sellService);
        for (String name : new String[]{"money", "pay", "baltop", "eco", "sell", "shop"}) {
            getCommand(name).setExecutor(ec);
        }

        // Playtime: +1 second per second for every online player
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                data.get(p.getUniqueId()).playtimeSeconds++;
                boards.update(p);
            }
        }, 20L, 20L);

        // Autosave every 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> data.save(), 6000L, 6000L);

        for (Player p : Bukkit.getOnlinePlayers()) {
            boards.show(p);
        }
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
    }
}
