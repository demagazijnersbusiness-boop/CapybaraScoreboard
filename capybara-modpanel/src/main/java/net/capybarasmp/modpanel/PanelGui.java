package net.capybarasmp.modpanel;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** The /modpanel chest menus: main panel, player picker, per-player actions and armor sets. */
public final class PanelGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    // main panel slots
    private static final int FLY = 10, GOD = 11, VANISH = 12, GAMEMODE = 13, HEAL = 14, FEED = 15, SPEED = 16;
    private static final int SPECTATE = 19, PLAYERS = 20, ARMOR = 21, SKELLY = 22, STOP_SPEC = 23, STAFF = 24, CLOSE = 31;

    // target menu slots
    private static final int T_SPEC = 10, T_GOTO = 11, T_BRING = 12, T_FREEZE = 13, T_MUTE = 14, T_HEAL = 15, T_FEED = 16;
    private static final int T_INV = 19, T_FLY = 20, T_GOD = 21, T_GM = 22, T_ARMOR = 23, T_SKELLY = 24, T_BACK = 31;

    private static final int[] ARMOR_SLOTS = {10, 11, 12, 13, 14, 15};
    private static final Material[] ARMOR_ICONS = {
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE};

    private final ModPanelPlugin plugin;
    private final StaffManager staff;
    private final StaffActions act;

    public PanelGui(ModPanelPlugin plugin, StaffManager staff, StaffActions act) {
        this.plugin = plugin;
        this.staff = staff;
        this.act = act;
    }

    // ------------------------------------------------------------ item helpers

    private static Component l(String s) {
        return MM.deserialize("<!italic>" + s);
    }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> {
            meta.displayName(l(name));
            if (lore.length > 0) {
                List<Component> lines = new ArrayList<>();
                for (String s : lore) lines.add(l(s));
                meta.lore(lines);
            }
        });
        return it;
    }

    private ItemStack toggle(Material m, String name, boolean on, String... extra) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Status: " + (on ? "<green><bold>ON" : "<red><bold>OFF"));
        for (String s : extra) lore.add(s);
        return item(m, name, lore.toArray(new String[0]));
    }

    // ------------------------------------------------------------ main panel

    public void openMain(Player p) {
        PanelHolder h = new PanelHolder(PanelHolder.Type.MAIN, null, 0, false);
        Inventory inv = Bukkit.createInventory(h, 36, MM.deserialize("<dark_red>Mod Panel"));
        h.setInventory(inv);
        fillMain(inv, p);
        p.openInventory(inv);
    }

    private void fillMain(Inventory inv, Player p) {
        inv.clear();
        UUID id = p.getUniqueId();
        inv.setItem(FLY, toggle(Material.FEATHER, "<aqua><bold>Fly", p.getAllowFlight(), "<dark_gray>Click to toggle"));
        inv.setItem(GOD, toggle(Material.TOTEM_OF_UNDYING, "<gold><bold>God Mode", act.isGod(id), "<dark_gray>Click to toggle"));
        inv.setItem(VANISH, toggle(Material.GLASS, "<white><bold>Vanish", act.isVanished(id), "<dark_gray>Click to toggle"));
        inv.setItem(GAMEMODE, item(Material.GRASS_BLOCK, "<green><bold>Gamemode",
                "<gray>Now: <yellow>" + p.getGameMode().name().toLowerCase(), "<dark_gray>Click to cycle"));
        inv.setItem(HEAL, item(Material.GOLDEN_APPLE, "<red><bold>Heal", "<gray>Full health, food and clear effects"));
        inv.setItem(FEED, item(Material.COOKED_BEEF, "<gold><bold>Feed", "<gray>Fill your hunger bar"));
        inv.setItem(SPEED, item(Material.SUGAR, "<yellow><bold>Speed",
                "<yellow>Left-click <gray>» max speed", "<yellow>Right-click <gray>» reset"));

        inv.setItem(SPECTATE, item(Material.ENDER_EYE, "<light_purple><bold>Spectate", "<gray>Pick a player to spectate"));
        inv.setItem(PLAYERS, item(Material.PLAYER_HEAD, "<aqua><bold>Players", "<gray>Pick a player for more actions"));
        inv.setItem(ARMOR, item(Material.NETHERITE_CHESTPLATE, "<dark_purple><bold>Armor Sets", "<gray>Give yourself a full set"));
        inv.setItem(SKELLY, item(Material.SPAWNER, "<white><bold>Skelly Spawner", "<gray>Give yourself 1 Skelly Spawner"));
        inv.setItem(STOP_SPEC, item(Material.BARRIER, "<red><bold>Stop Spectating",
                "<gray>Return to where you were"));
        inv.setItem(STAFF, item(Material.BOOK, "<yellow><bold>Staff List", "<gray>Show owners and moderators in chat"));
        inv.setItem(CLOSE, item(Material.RED_STAINED_GLASS_PANE, "<red><bold>Close"));
    }

    // ------------------------------------------------------------ player picker

    public void openPicker(Player p, int page, boolean spectatePick) {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int pages = Math.max(1, (players.size() + 44) / 45);
        page = Math.max(0, Math.min(page, pages - 1));

        PanelHolder h = new PanelHolder(PanelHolder.Type.PICKER, null, page, spectatePick);
        String title = spectatePick ? "<dark_red>Spectate - pick a player" : "<dark_red>Players";
        Inventory inv = Bukkit.createInventory(h, 54, MM.deserialize(title));
        h.setInventory(inv);

        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= players.size()) break;
            Player t = players.get(idx);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            head.editMeta(SkullMeta.class, m -> {
                m.setOwningPlayer(t);
                m.displayName(l("<white><bold>" + t.getName()));
                m.lore(List.of(l(spectatePick ? "<gray>Click to spectate" : "<gray>Click for actions")));
            });
            inv.setItem(i, head);
        }
        if (page > 0) inv.setItem(45, item(Material.ARROW, "<yellow><bold>Previous Page"));
        inv.setItem(49, item(Material.RED_STAINED_GLASS_PANE, "<red><bold>Back"));
        if (page < pages - 1) inv.setItem(53, item(Material.ARROW, "<yellow><bold>Next Page"));
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ target actions

    public void openTarget(Player p, Player t) {
        PanelHolder h = new PanelHolder(PanelHolder.Type.TARGET, t.getUniqueId(), 0, false);
        Inventory inv = Bukkit.createInventory(h, 36,
                MM.deserialize("<dark_red>Mod Panel - <n>", Placeholder.unparsed("n", t.getName())));
        h.setInventory(inv);
        fillTarget(inv, t);
        p.openInventory(inv);
    }

    private void fillTarget(Inventory inv, Player t) {
        inv.clear();
        UUID id = t.getUniqueId();
        inv.setItem(T_SPEC, item(Material.ENDER_EYE, "<light_purple><bold>Spectate"));
        inv.setItem(T_GOTO, item(Material.ENDER_PEARL, "<aqua><bold>Go to player", "<gray>Teleport to them"));
        inv.setItem(T_BRING, item(Material.LEAD, "<aqua><bold>Bring player", "<gray>Teleport them to you"));
        inv.setItem(T_FREEZE, toggle(Material.PACKED_ICE, "<aqua><bold>Freeze", act.isFrozen(id)));
        inv.setItem(T_MUTE, toggle(Material.PAPER, "<red><bold>Mute", act.isMuted(id)));
        inv.setItem(T_HEAL, item(Material.GOLDEN_APPLE, "<red><bold>Heal"));
        inv.setItem(T_FEED, item(Material.COOKED_BEEF, "<gold><bold>Feed"));
        inv.setItem(T_INV, item(Material.CHEST, "<yellow><bold>Open Inventory", "<gray>View and edit their items"));
        inv.setItem(T_FLY, toggle(Material.FEATHER, "<aqua><bold>Fly", t.getAllowFlight()));
        inv.setItem(T_GOD, toggle(Material.TOTEM_OF_UNDYING, "<gold><bold>God Mode", act.isGod(id)));
        inv.setItem(T_GM, item(Material.GRASS_BLOCK, "<green><bold>Gamemode",
                "<gray>Now: <yellow>" + t.getGameMode().name().toLowerCase(), "<dark_gray>Click to cycle"));
        inv.setItem(T_ARMOR, item(Material.NETHERITE_CHESTPLATE, "<dark_purple><bold>Armor Sets"));
        inv.setItem(T_SKELLY, item(Material.SPAWNER, "<white><bold>Give Skelly Spawner", "<gray>Gives 1 spawner"));
        inv.setItem(T_BACK, item(Material.RED_STAINED_GLASS_PANE, "<red><bold>Back"));
    }

    // ------------------------------------------------------------ armor sets

    public void openArmor(Player p, UUID target) {
        PanelHolder h = new PanelHolder(PanelHolder.Type.ARMOR, target, 0, false);
        Inventory inv = Bukkit.createInventory(h, 27, MM.deserialize("<dark_red>Armor Sets"));
        h.setInventory(inv);
        for (int i = 0; i < StaffActions.TIERS.length; i++) {
            String tier = StaffActions.TIERS[i];
            inv.setItem(ARMOR_SLOTS[i], item(ARMOR_ICONS[i], "<yellow><bold>" + tier.substring(0, 1).toUpperCase() + tier.substring(1),
                    "<yellow>Left-click <gray>» full set", "<yellow>Shift-click <gray>» full set, max enchants"));
        }
        inv.setItem(22, item(Material.RED_STAINED_GLASS_PANE, "<red><bold>Back"));
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ click handling

    public void handle(Player p, PanelHolder h, int slot, boolean shift, boolean right) {
        if (!staff.isMod(p.getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, p::closeInventory);
            return;
        }
        switch (h.type()) {
            case MAIN -> handleMain(p, h, slot, right);
            case PICKER -> handlePicker(p, h, slot);
            case TARGET -> handleTarget(p, h, slot);
            case ARMOR -> handleArmor(p, h, slot, shift);
        }
    }

    private void close(Player p) {
        Bukkit.getScheduler().runTask(plugin, p::closeInventory);
    }

    private void handleMain(Player p, PanelHolder h, int slot, boolean right) {
        Inventory inv = h.getInventory();
        switch (slot) {
            case FLY -> act.toggleFly(p);
            case GOD -> act.toggleGod(p);
            case VANISH -> act.toggleVanish(p);
            case GAMEMODE -> act.cycleGamemode(p);
            case HEAL -> act.heal(p);
            case FEED -> act.feed(p);
            case SPEED -> {
                if (right) act.resetSpeed(p);
                else act.setSpeed(p, 10);
            }
            case SPECTATE -> {
                openPicker(p, 0, true);
                return;
            }
            case PLAYERS -> {
                openPicker(p, 0, false);
                return;
            }
            case ARMOR -> {
                openArmor(p, p.getUniqueId());
                return;
            }
            case SKELLY -> {
                if (!act.giveSkelly(p, 1)) act.msg(p, "<red>Skelly Spawners are not installed on this server.");
            }
            case STOP_SPEC -> {
                if (act.stopSpectate(p)) act.msg(p, "<green>Stopped spectating.");
                else act.msg(p, "<red>You are not spectating anyone.");
                close(p);
                return;
            }
            case STAFF -> {
                act.msg(p, "<gold>Owners: <white><o>", Placeholder.unparsed("o", String.join(", ", staff.owners().values())));
                act.msg(p, "<green>Moderators: <white><m>", Placeholder.unparsed("m",
                        staff.mods().isEmpty() ? "-" : String.join(", ", staff.mods().values())));
                return;
            }
            case CLOSE -> {
                close(p);
                return;
            }
            default -> {
                return;
            }
        }
        fillMain(inv, p); // refresh the toggles
    }

    private void handlePicker(Player p, PanelHolder h, int slot) {
        if (slot == 49) {
            openMain(p);
            return;
        }
        if (slot == 45 && h.page() > 0) {
            openPicker(p, h.page() - 1, h.spectatePick());
            return;
        }
        if (slot == 53) {
            openPicker(p, h.page() + 1, h.spectatePick());
            return;
        }
        if (slot < 0 || slot >= 45) return;

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        int idx = h.page() * 45 + slot;
        if (idx >= players.size()) return;
        Player t = players.get(idx);

        if (h.spectatePick()) {
            if (t.equals(p)) {
                act.msg(p, "<red>You can't spectate yourself.");
                return;
            }
            close(p);
            Bukkit.getScheduler().runTask(plugin, () -> {
                act.spectate(p, t);
                act.msg(p, "<green>Spectating <white><n><green>. Use <white>/spectate off<green> to return.",
                        Placeholder.unparsed("n", t.getName()));
            });
        } else {
            openTarget(p, t);
        }
    }

    private void handleTarget(Player p, PanelHolder h, int slot) {
        Player t = Bukkit.getPlayer(h.target());
        if (t == null) {
            act.msg(p, "<red>That player went offline.");
            openPicker(p, 0, false);
            return;
        }
        Inventory inv = h.getInventory();
        switch (slot) {
            case T_SPEC -> {
                if (t.equals(p)) {
                    act.msg(p, "<red>You can't spectate yourself.");
                    return;
                }
                close(p);
                Bukkit.getScheduler().runTask(plugin, () -> act.spectate(p, t));
                return;
            }
            case T_GOTO -> {
                act.goTo(p, t);
                close(p);
                return;
            }
            case T_BRING -> {
                act.bring(p, t);
                act.msg(p, "<green>Brought <white><n><green> to you.", Placeholder.unparsed("n", t.getName()));
                return;
            }
            case T_FREEZE -> {
                boolean on = act.toggleFreeze(t);
                act.msg(t, on ? "<red>You have been frozen by staff. Do not log out!" : "<green>You have been unfrozen.");
            }
            case T_MUTE -> {
                boolean on = act.toggleMute(t);
                act.msg(t, on ? "<red>You have been muted." : "<green>You have been unmuted.");
            }
            case T_HEAL -> {
                act.heal(t);
                act.msg(p, "<green>Healed <white><n><green>.", Placeholder.unparsed("n", t.getName()));
                return;
            }
            case T_FEED -> {
                act.feed(t);
                act.msg(p, "<green>Fed <white><n><green>.", Placeholder.unparsed("n", t.getName()));
                return;
            }
            case T_INV -> {
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(t.getInventory()));
                return;
            }
            case T_FLY -> act.toggleFly(t);
            case T_GOD -> act.toggleGod(t);
            case T_GM -> act.cycleGamemode(t);
            case T_ARMOR -> {
                openArmor(p, t.getUniqueId());
                return;
            }
            case T_SKELLY -> {
                if (act.giveSkelly(t, 1)) {
                    act.msg(p, "<green>Gave 1 Skelly Spawner to <white><n><green>.", Placeholder.unparsed("n", t.getName()));
                } else {
                    act.msg(p, "<red>Skelly Spawners are not installed on this server.");
                }
                return;
            }
            case T_BACK -> {
                openPicker(p, 0, false);
                return;
            }
            default -> {
                return;
            }
        }
        fillTarget(inv, t);
    }

    private void handleArmor(Player p, PanelHolder h, int slot, boolean shift) {
        if (slot == 22) {
            if (h.target() != null && !h.target().equals(p.getUniqueId())) {
                Player t = Bukkit.getPlayer(h.target());
                if (t != null) {
                    openTarget(p, t);
                    return;
                }
            }
            openMain(p);
            return;
        }
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            if (slot != ARMOR_SLOTS[i]) continue;
            Player t = Bukkit.getPlayer(h.target());
            if (t == null) {
                act.msg(p, "<red>That player went offline.");
                return;
            }
            String tier = StaffActions.TIERS[i];
            if (act.setArmor(t, tier, shift)) {
                act.msg(p, "<green>Gave <white><n> <green>a full <tier> set<m>.",
                        Placeholder.unparsed("n", t.getName()), Placeholder.unparsed("tier", tier),
                        Placeholder.unparsed("m", shift ? " (max enchants)" : ""));
            }
            return;
        }
    }
}
