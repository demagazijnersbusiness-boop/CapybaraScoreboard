package net.capybarasmp.modpanel;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StaffCommands implements CommandExecutor, TabCompleter {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final StaffManager staff;
    private final StaffActions act;
    private final PanelGui gui;

    public StaffCommands(StaffManager staff, StaffActions act, PanelGui gui) {
        this.staff = staff;
        this.act = act;
        this.gui = gui;
    }

    // ------------------------------------------------------------ helpers

    private Player resolve(CommandSender s, String[] a, int i) {
        if (a.length > i) {
            Player t = Bukkit.getPlayer(a[i]);
            if (t == null) act.msg(s, "<red>That player is not online.");
            return t;
        }
        if (s instanceof Player p) return p;
        act.msg(s, "<red>Specify a player.");
        return null;
    }

    private void notifyTarget(CommandSender by, Player t, String mini) {
        if (!t.equals(by)) act.msg(t, mini);
    }

    private String onOff(boolean b) {
        return b ? "<green>ON" : "<red>OFF";
    }

    // ------------------------------------------------------------ dispatch

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        String name = cmd.getName().toLowerCase();
        if (name.equals("mod")) {
            modCommand(sender, a);
            return true;
        }
        if (!staff.isStaff(sender)) {
            act.msg(sender, "<red>You are not a moderator.");
            return true;
        }

        switch (name) {
            case "modpanel" -> {
                if (sender instanceof Player p) gui.openMain(p);
                else act.msg(sender, "<red>Players only.");
            }
            case "fly" -> {
                Player t = resolve(sender, a, 0);
                if (t == null) return true;
                boolean on = act.toggleFly(t);
                act.msg(sender, "<white><n> <gray>flight: " + onOff(on), Placeholder.unparsed("n", t.getName()));
                notifyTarget(sender, t, "<gray>Flight: " + onOff(on));
            }
            case "god" -> {
                Player t = resolve(sender, a, 0);
                if (t == null) return true;
                boolean on = act.toggleGod(t);
                act.msg(sender, "<white><n> <gray>god mode: " + onOff(on), Placeholder.unparsed("n", t.getName()));
                notifyTarget(sender, t, "<gray>God mode: " + onOff(on));
            }
            case "vanish" -> {
                Player t = resolve(sender, a, 0);
                if (t == null) return true;
                boolean on = act.toggleVanish(t);
                act.msg(sender, "<white><n> <gray>vanish: " + onOff(on), Placeholder.unparsed("n", t.getName()));
                notifyTarget(sender, t, "<gray>Vanish: " + onOff(on));
            }
            case "spectate" -> spectate(sender, a);
            case "setarmor" -> setArmor(sender, a);
            case "skellys" -> skellys(sender, a);
            case "heal" -> {
                Player t = resolve(sender, a, 0);
                if (t == null) return true;
                act.heal(t);
                act.msg(sender, "<green>Healed <white><n><green>.", Placeholder.unparsed("n", t.getName()));
                notifyTarget(sender, t, "<green>You were healed.");
            }
            case "feed" -> {
                Player t = resolve(sender, a, 0);
                if (t == null) return true;
                act.feed(t);
                act.msg(sender, "<green>Fed <white><n><green>.", Placeholder.unparsed("n", t.getName()));
                notifyTarget(sender, t, "<green>You were fed.");
            }
            case "gm" -> gamemode(sender, a);
            case "goto" -> {
                if (!(sender instanceof Player p)) {
                    act.msg(sender, "<red>Players only.");
                    return true;
                }
                Player t = a.length > 0 ? resolve(sender, a, 0) : null;
                if (t == null) {
                    if (a.length == 0) act.msg(sender, "<red>Usage: /goto <player>");
                    return true;
                }
                act.goTo(p, t);
                act.msg(sender, "<green>Teleported to <white><n><green>.", Placeholder.unparsed("n", t.getName()));
            }
            case "bring" -> {
                if (!(sender instanceof Player p)) {
                    act.msg(sender, "<red>Players only.");
                    return true;
                }
                Player t = a.length > 0 ? resolve(sender, a, 0) : null;
                if (t == null) {
                    if (a.length == 0) act.msg(sender, "<red>Usage: /bring <player>");
                    return true;
                }
                act.bring(p, t);
                act.msg(sender, "<green>Brought <white><n><green> to you.", Placeholder.unparsed("n", t.getName()));
            }
            case "freeze" -> {
                Player t = a.length > 0 ? resolve(sender, a, 0) : null;
                if (t == null) {
                    if (a.length == 0) act.msg(sender, "<red>Usage: /freeze <player>");
                    return true;
                }
                boolean on = act.toggleFreeze(t);
                act.msg(sender, "<white><n> <gray>frozen: " + onOff(on), Placeholder.unparsed("n", t.getName()));
                act.msg(t, on ? "<red>You have been frozen by staff. Do not log out!" : "<green>You have been unfrozen.");
            }
            case "mute" -> {
                Player t = a.length > 0 ? resolve(sender, a, 0) : null;
                if (t == null) {
                    if (a.length == 0) act.msg(sender, "<red>Usage: /mute <player>");
                    return true;
                }
                boolean on = act.toggleMute(t);
                act.msg(sender, "<white><n> <gray>muted: " + onOff(on), Placeholder.unparsed("n", t.getName()));
                act.msg(t, on ? "<red>You have been muted." : "<green>You have been unmuted.");
            }
            case "invsee" -> {
                if (!(sender instanceof Player p)) {
                    act.msg(sender, "<red>Players only.");
                    return true;
                }
                Player t = a.length > 0 ? resolve(sender, a, 0) : null;
                if (t == null) {
                    if (a.length == 0) act.msg(sender, "<red>Usage: /invsee <player>");
                    return true;
                }
                p.openInventory(t.getInventory());
            }
            case "speed" -> speed(sender, a);
            case "sc" -> {
                if (a.length == 0) {
                    act.msg(sender, "<red>Usage: /sc <message>");
                    return true;
                }
                String who = sender instanceof Player p ? p.getName() : "Console";
                var msg = MM.deserialize("<dark_aqua>[Staff] <white><n><gray>: <aqua><m>",
                        Placeholder.unparsed("n", who), Placeholder.unparsed("m", String.join(" ", a)));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (staff.isMod(online.getUniqueId())) online.sendMessage(msg);
                }
                Bukkit.getConsoleSender().sendMessage(msg);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------ /mod

    private void modCommand(CommandSender s, String[] a) {
        if (!staff.isOwner(s)) {
            act.msg(s, "<red>Only the owner can manage moderators.");
            return;
        }
        if (a.length == 0) {
            act.msg(s, "<red>Usage: /mod <add|remove|list> [player]");
            return;
        }
        switch (a[0].toLowerCase()) {
            case "list" -> {
                act.msg(s, "<gold>Owners: <white><l>", Placeholder.unparsed("l", names(staff.owners())));
                act.msg(s, "<green>Moderators: <white><l>", Placeholder.unparsed("l", names(staff.mods())));
            }
            case "add", "remove", "owner" -> {
                if (a.length < 2) {
                    act.msg(s, "<red>Usage: /mod " + a[0] + " <player>");
                    return;
                }
                if (a[0].equalsIgnoreCase("owner") && s instanceof Player) {
                    act.msg(s, "<red>/mod owner can only be used from the console.");
                    return;
                }
                OfflinePlayer t = Bukkit.getPlayerExact(a[1]);
                if (t == null) t = Bukkit.getOfflinePlayerIfCached(a[1]);
                if (t == null) {
                    act.msg(s, "<red>That player has never joined.");
                    return;
                }
                UUID id = t.getUniqueId();
                String name = t.getName() == null ? a[1] : t.getName();
                switch (a[0].toLowerCase()) {
                    case "add" -> {
                        if (staff.isMod(id)) {
                            act.msg(s, "<yellow><n> is already staff.", Placeholder.unparsed("n", name));
                            return;
                        }
                        staff.addMod(id, name);
                        act.msg(s, "<green><n> is now a moderator.", Placeholder.unparsed("n", name));
                        if (t.getPlayer() != null) {
                            act.msg(t.getPlayer(), "<green>You are now a <bold>moderator</bold><green>! Use <white>/modpanel<green>.");
                        }
                    }
                    case "remove" -> {
                        if (staff.isOwner(id)) {
                            act.msg(s, "<red>You can't remove an owner.");
                            return;
                        }
                        if (staff.removeMod(id)) {
                            act.msg(s, "<yellow><n> is no longer a moderator.", Placeholder.unparsed("n", name));
                            if (t.getPlayer() != null) {
                                act.cleanup(t.getPlayer());
                                act.refreshVisibility();
                                act.msg(t.getPlayer(), "<red>You are no longer a moderator.");
                            }
                        } else {
                            act.msg(s, "<red><n> is not a moderator.", Placeholder.unparsed("n", name));
                        }
                    }
                    default -> {
                        staff.addOwner(id, name);
                        act.msg(s, "<green><n> is now an OWNER.", Placeholder.unparsed("n", name));
                    }
                }
            }
            default -> act.msg(s, "<red>Usage: /mod <add|remove|list> [player]");
        }
    }

    private String names(Map<UUID, String> m) {
        return m.isEmpty() ? "-" : String.join(", ", m.values());
    }

    // ------------------------------------------------------------ individual commands

    private void spectate(CommandSender s, String[] a) {
        if (!(s instanceof Player p)) {
            act.msg(s, "<red>Players only.");
            return;
        }
        if (a.length == 0 || a[0].equalsIgnoreCase("off")) {
            if (act.stopSpectate(p)) act.msg(p, "<green>Stopped spectating.");
            else act.msg(p, "<red>Usage: /spectate <player>");
            return;
        }
        Player t = Bukkit.getPlayer(a[0]);
        if (t == null) {
            act.msg(p, "<red>That player is not online.");
            return;
        }
        if (t.equals(p)) {
            act.msg(p, "<red>You can't spectate yourself.");
            return;
        }
        act.spectate(p, t);
        act.msg(p, "<green>Spectating <white><n><green>. Use <white>/spectate off<green> to return.",
                Placeholder.unparsed("n", t.getName()));
    }

    private void setArmor(CommandSender s, String[] a) {
        if (a.length == 0) {
            act.msg(s, "<red>Usage: /setarmor [player] <leather|chainmail|iron|gold|diamond|netherite> [max]");
            return;
        }
        Player t;
        String tier;
        boolean max;
        if (StaffActions.isTier(a[0])) {
            if (!(s instanceof Player p)) {
                act.msg(s, "<red>Specify a player.");
                return;
            }
            t = p;
            tier = a[0];
            max = a.length > 1 && a[1].equalsIgnoreCase("max");
        } else {
            t = resolve(s, a, 0);
            if (t == null) return;
            if (a.length < 2) {
                act.msg(s, "<red>Usage: /setarmor [player] <tier> [max]");
                return;
            }
            tier = a[1];
            max = a.length > 2 && a[2].equalsIgnoreCase("max");
        }
        if (!act.setArmor(t, tier, max)) {
            act.msg(s, "<red>Unknown armor tier. Use: leather, chainmail, iron, gold, diamond, netherite.");
            return;
        }
        act.msg(s, "<green>Gave <white><n> <green>a full <tier> set<m>.",
                Placeholder.unparsed("n", t.getName()), Placeholder.unparsed("tier", tier.toLowerCase()),
                Placeholder.unparsed("m", max ? " (max enchants)" : ""));
        notifyTarget(s, t, "<green>You received a full " + tier.toLowerCase() + " armor set.");
    }

    private void skellys(CommandSender s, String[] a) {
        if (a.length == 0) {
            act.msg(s, "<red>Usage: /skellys <player> [amount]");
            return;
        }
        Player t = resolve(s, a, 0);
        if (t == null) return;
        int amount = 1;
        if (a.length > 1) {
            try {
                amount = Integer.parseInt(a[1]);
            } catch (NumberFormatException e) {
                act.msg(s, "<red>Amount must be a number.");
                return;
            }
        }
        if (!act.giveSkelly(t, amount)) {
            act.msg(s, "<red>The CapybaraScoreboard plugin (Skelly Spawners) is not installed.");
            return;
        }
        act.msg(s, "<green>Gave <white><a> <green>Skelly Spawner(s) to <white><n><green>.",
                Placeholder.unparsed("a", String.valueOf(Math.max(1, Math.min(64, amount)))),
                Placeholder.unparsed("n", t.getName()));
    }

    private void gamemode(CommandSender s, String[] a) {
        if (a.length == 0) {
            act.msg(s, "<red>Usage: /gm <survival|creative|adventure|spectator> [player]");
            return;
        }
        GameMode gm = StaffActions.parseGamemode(a[0]);
        if (gm == null) {
            act.msg(s, "<red>Unknown gamemode.");
            return;
        }
        Player t = resolve(s, a, 1);
        if (t == null) return;
        t.setGameMode(gm);
        act.msg(s, "<white><n> <gray>gamemode: <yellow><g>", Placeholder.unparsed("n", t.getName()),
                Placeholder.unparsed("g", gm.name().toLowerCase()));
        notifyTarget(s, t, "<gray>Your gamemode is now <yellow>" + gm.name().toLowerCase());
    }

    private void speed(CommandSender s, String[] a) {
        if (a.length == 0) {
            act.msg(s, "<red>Usage: /speed <1-10|reset> [player]");
            return;
        }
        Player t = resolve(s, a, 1);
        if (t == null) return;
        if (a[0].equalsIgnoreCase("reset")) {
            act.resetSpeed(t);
            act.msg(s, "<green>Speed reset for <white><n><green>.", Placeholder.unparsed("n", t.getName()));
            return;
        }
        try {
            int level = Integer.parseInt(a[0]);
            act.setSpeed(t, level);
            act.msg(s, "<green>Speed <white><l> <green>set for <white><n><green>.",
                    Placeholder.unparsed("l", String.valueOf(Math.max(1, Math.min(10, level)))),
                    Placeholder.unparsed("n", t.getName()));
        } catch (NumberFormatException e) {
            act.msg(s, "<red>Use a number from 1 to 10, or reset.");
        }
    }

    // ------------------------------------------------------------ tab complete

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!staff.isStaff(s)) return out;
        String name = cmd.getName().toLowerCase();
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase();

        if (name.equals("mod")) {
            if (a.length == 1) out.addAll(List.of("add", "remove", "list"));
            else if (a.length == 2) addPlayers(out);
        } else if (name.equals("gm")) {
            if (a.length == 1) out.addAll(List.of("survival", "creative", "adventure", "spectator"));
            else if (a.length == 2) addPlayers(out);
        } else if (name.equals("setarmor")) {
            if (a.length == 1) {
                out.addAll(List.of(StaffActions.TIERS));
                addPlayers(out);
            } else if (a.length == 2) {
                out.addAll(List.of(StaffActions.TIERS));
                out.add("max");
            } else out.add("max");
        } else if (name.equals("speed")) {
            if (a.length == 1) out.addAll(List.of("1", "2", "4", "6", "8", "10", "reset"));
            else addPlayers(out);
        } else if (!name.equals("sc") && !name.equals("modpanel") && a.length == 1) {
            addPlayers(out);
            if (name.equals("spectate")) out.add("off");
        }
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }

    private void addPlayers(List<String> out) {
        for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
    }
}
