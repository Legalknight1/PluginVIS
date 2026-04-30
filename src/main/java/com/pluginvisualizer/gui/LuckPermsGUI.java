package com.pluginvisualizer.gui;

import com.pluginvisualizer.PluginVisualizer;
import com.pluginvisualizer.util.ChatInputManager;
import com.pluginvisualizer.util.PluginDataLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LuckPerms Main GUI - Shows groups in middle, Create+Refresh at bottom center.
 * Dead simple: click a group to manage it.
 */
public class LuckPermsGUI extends GUIFramework {

    private int page = 0;
    private static final int SLOTS_PER_PAGE = 21;
    // Middle area: 3 rows of 7 (rows 2-4, columns 2-8)
    private static final int[] GROUP_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public LuckPermsGUI(Player player) {
        super(player, "§8§lLuckPerms §7— Groups", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Header
        setItem(4, new GUIItemBuilder(Material.GOLDEN_HELMET)
                .name("§6§lLuckPerms §7— Groups")
                .lore("§7Click a group to manage it")
                .glowing(true).build());

        // Bottom center: Create Group + Refresh (slots 48, 50)
        setItem(48, new GUIItemBuilder(Material.EMERALD)
                .name("§a§l+ Create Group")
                .lore("§7Create a new rank/group", "", "§eClick →")
                .build(), e -> createGroup());

        setItem(50, new GUIItemBuilder(Material.COMPARATOR)
                .name("§7§l↻ Refresh")
                .lore("§7Reload groups from LuckPerms", "", "§eClick")
                .build(), e -> { plugin.playSound(player, "click"); refresh(); open(); });

        // Load existing groups into the middle area
        List<PluginDataLoader.GroupInfo> groups = plugin.getDataLoader().getLuckPermsGroups();
        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, groups.size());

        Material[] rankMats = {Material.NETHERITE_HELMET, Material.DIAMOND_HELMET, Material.GOLDEN_HELMET,
            Material.IRON_HELMET, Material.CHAINMAIL_HELMET, Material.LEATHER_HELMET};

        for (int i = start; i < end; i++) {
            int si = i - start;
            if (si >= GROUP_SLOTS.length) break;
            PluginDataLoader.GroupInfo g = groups.get(i);
            int mi = Math.min(g.weight / 20, rankMats.length - 1);
            Material mat = g.weight > 0 ? rankMats[mi] : Material.LEATHER_HELMET;

            String prefix = g.prefix != null ? g.prefix : "§7None";
            String parents = g.parents.isEmpty() ? "§7None" : "§f" + String.join("§7, §f", g.parents);

            setItem(GROUP_SLOTS[si], new GUIItemBuilder(mat)
                    .name("§6§l" + g.name)
                    .lore("§7Prefix: " + prefix, "§7Weight: §f" + g.weight,
                        "§7Perms: §f" + g.permissionCount, "§7Parents: " + parents, "", "§eClick to manage →")
                    .build(),
                event -> { plugin.playSound(player, "click"); new LPGroupGUI(player, g.name).open(); });
        }

        // Pagination
        if (page > 0) setItem(45, new GUIItemBuilder(Material.ARROW).name("§c← Prev").build(),
            e -> { page--; refresh(); });
        if (end < groups.size()) setItem(53, new GUIItemBuilder(Material.ARROW).name("§aNext →").build(),
            e -> { page++; refresh(); });

        // Empty state
        if (groups.isEmpty()) {
            setItem(22, new GUIItemBuilder(Material.STRUCTURE_VOID)
                    .name("§7No groups found")
                    .lore("§7Click §a+ Create Group §7to make one!")
                    .build());
        }

        addBackButton(49, () -> new MainMenuGUI(player).open());
    }

    private void createGroup() {
        plugin.getChatInputManager().requestInput(player, "Enter group name:",
            input -> {
                LuckPerms api = Bukkit.getServicesManager().load(LuckPerms.class);
                if (api != null && api.getGroupManager().getLoadedGroups().stream().anyMatch(g -> g.getName().equalsIgnoreCase(input))) {
                    plugin.playSound(player, "error");
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Group '" + input + "' already exists!");
                    new LuckPermsGUI(player).open();
                    return;
                }
                plugin.dispatchCommand("lp creategroup " + input);
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Created §f" + input);
                new LuckPermsGUI(player).open();
            }, input -> input.contains(" ") ? "No spaces allowed!" : null);
    }
}

/**
 * Group Detail GUI - 3 openers: Permissions, Players, Editor
 */
class LPGroupGUI extends GUIFramework {
    private final String groupName;

    public LPGroupGUI(Player player, String groupName) {
        super(player, "§8§lLuckPerms §7— §6" + groupName, 36);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        PluginDataLoader.GroupInfo g = findGroup();
        String prefix = g != null && g.prefix != null ? g.prefix : "§7None";
        int weight = g != null ? g.weight : 0;
        int perms = g != null ? g.permissionCount : 0;

        // Header
        setItem(4, new GUIItemBuilder(Material.GOLDEN_HELMET)
                .name("§6§l" + groupName)
                .lore("§7Prefix: " + prefix, "§7Weight: §f" + weight, "§7Permissions: §f" + perms)
                .glowing(true).build());

        // 3 Openers
        setItem(11, new GUIItemBuilder(Material.SHIELD)
                .name("§b§lPermissions")
                .lore("§7View & manage permissions", "§7Add common perms by clicking", "§7Toggle existing perms on/off", "", "§eClick to open →")
                .build(), e -> { plugin.playSound(player, "click"); new LPPermissionsGUI(player, groupName).open(); });

        setItem(13, new GUIItemBuilder(Material.PLAYER_HEAD)
                .name("§a§lPlayers")
                .lore("§7See who has this rank", "§7Click players to add/remove", "§7Left = In group | Right = Not in group", "", "§eClick to open →")
                .build(), e -> { plugin.playSound(player, "click"); new LPPlayersGUI(player, groupName).open(); });

        setItem(15, new GUIItemBuilder(Material.ANVIL)
                .name("§e§lEditor")
                .lore("§7Change name color & weight", "§7Click colors to apply", "§7Simple visual editing", "", "§eClick to open →")
                .build(), e -> { plugin.playSound(player, "click"); new LPEditorGUI(player, groupName).open(); });

        // Delete
        setItem(29, new GUIItemBuilder(Material.TNT)
                .name("§c§lDelete Group")
                .lore("§7Permanently remove this group", "§c⚠ Cannot be undone!", "", "§eClick to delete")
                .build(), e -> {
                    plugin.dispatchCommand("lp deletegroup " + groupName);
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Deleted §f" + groupName);
                    new LuckPermsGUI(player).open();
                });

        addBackButton(27, () -> new LuckPermsGUI(player).open());
        addCloseButton(35);
    }

    private PluginDataLoader.GroupInfo findGroup() {
        for (PluginDataLoader.GroupInfo g : plugin.getDataLoader().getLuckPermsGroups()) {
            if (g.name.equalsIgnoreCase(groupName)) return g;
        }
        return null;
    }
}

/**
 * Permissions GUI - Shows existing perms + common perm shortcuts
 */
class LPPermissionsGUI extends GUIFramework {
    private final String groupName;
    private int page = 0;

    public LPPermissionsGUI(Player player, String groupName) {
        super(player, "§8§lLuckPerms §7— §bPermissions", 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.SHIELD)
                .name("§b§lPermissions §7— §6" + groupName).glowing(true).build());

        // Add Common Perms button
        setItem(1, new GUIItemBuilder(Material.EMERALD)
                .name("§a§l+ Add Common Perms")
                .lore("§7Click to pick from a list", "§7of popular permissions", "", "§eClick →")
                .build(), e -> { plugin.playSound(player, "click"); new LPAddPermsGUI(player, groupName).open(); });

        // Add Custom Perm
        setItem(2, new GUIItemBuilder(Material.PAPER)
                .name("§e§l+ Custom Permission")
                .lore("§7Type a permission node manually", "", "§eClick →")
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter permission (e.g. essentials.fly):",
                        input -> {
                            plugin.dispatchCommand("lp group " + groupName + " permission set " + input + " true");
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Added §f" + input);
                            refresh(); open();
                        });
                });

        // List existing perms
        List<PluginDataLoader.PermInfo> perms = plugin.getDataLoader().getLuckPermsPermissions(groupName);
        int[] permSlots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        int start = page * permSlots.length;
        int end = Math.min(start + permSlots.length, perms.size());

        for (int i = start; i < end; i++) {
            int si = i - start;
            PluginDataLoader.PermInfo p = perms.get(i);
            Material mat = p.value ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            String name = p.permission.length() > 28 ? p.permission.substring(0,25) + "..." : p.permission;

            setItem(permSlots[si], new GUIItemBuilder(mat)
                    .name(p.value ? "§a" + name : "§c" + name)
                    .lore("§7Full: §f" + p.permission, "§7Status: " + (p.value ? "§aTRUE" : "§cFALSE"),
                        "", "§eClick to toggle", "§eShift+Click to remove")
                    .build(), event -> {
                        if (event.isShiftClick()) {
                            plugin.dispatchCommand("lp group " + groupName + " permission unset " + p.permission);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Removed §f" + p.permission);
                        } else {
                            String action = p.value ? "unset" : "set";
                            plugin.dispatchCommand("lp group " + groupName + " permission " + action + " " + p.permission + (p.value ? "" : " true"));
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Toggled §f" + p.permission);
                        }
                        refresh(); open();
                    });
        }

        int total = (int) Math.ceil((double) perms.size() / permSlots.length);
        if (page > 0) setItem(45, new GUIItemBuilder(Material.ARROW).name("§c← Prev").build(), e -> { page--; refresh(); });
        if (end < perms.size()) setItem(53, new GUIItemBuilder(Material.ARROW).name("§aNext →").build(), e -> { page++; refresh(); });

        addBackButton(48, () -> new LPGroupGUI(player, groupName).open());
        addCloseButton(50);
    }
}

/**
 * Add Common Permissions GUI - Pre-loaded popular permissions by category
 */
class LPAddPermsGUI extends GUIFramework {
    private final String groupName;

    // Common permissions organized by category
    private static final String[][] CATEGORIES = {
        {"§bMovement", Material.IRON_BOOTS.toString(), "essentials.fly", "essentials.speed", "essentials.gamemode", "essentials.god", "essentials.feed", "essentials.heal", "essentials.repair"},
        {"§dTeleport", Material.ENDER_PEARL.toString(), "essentials.tp", "essentials.tpa", "essentials.home", "essentials.warp", "essentials.sethome", "essentials.setwarp", "essentials.back"},
        {"§cAdmin", Material.DIAMOND_SWORD.toString(), "essentials.ban", "essentials.kick", "essentials.mute", "essentials.tempban", "essentials.invsee", "essentials.vanish", "essentials.enderchest"},
        {"§6WorldEdit", Material.WOODEN_AXE.toString(), "worldedit.*", "worldedit.selection.*", "worldedit.clipboard.*", "worldedit.region.*", "worldedit.generation.*", "worldedit.history.*", "worldedit.navigation.*"},
        {"§eChat", Material.OAK_SIGN.toString(), "essentials.chat.color", "essentials.chat.bold", "essentials.chat.italic", "essentials.msg", "essentials.nick", "essentials.me", "essentials.mail"},
        {"§aEconomy", Material.GOLD_INGOT.toString(), "essentials.pay", "essentials.balance", "essentials.sell", "essentials.worth", "essentials.eco", "essentials.give", "essentials.kit"},
    };

    public LPAddPermsGUI(Player player, String groupName) {
        super(player, "§8§lLuckPerms §7— §aAdd Perms", 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.EMERALD)
                .name("§a§lAdd Permissions §7— §6" + groupName).glowing(true).build());

        List<PluginDataLoader.PermInfo> existing = plugin.getDataLoader().getLuckPermsPermissions(groupName);
        Set<String> hasPerms = existing.stream().filter(p -> p.value).map(p -> p.permission).collect(Collectors.toSet());

        for (int c = 0; c < CATEGORIES.length; c++) {
            String[] cat = CATEGORIES[c];
            String catName = cat[0];
            Material catMat = Material.valueOf(cat[1]);

            int baseRow = 1 + c; // rows 1-6

            // Category label
            setItem(baseRow * 9 + 1, new GUIItemBuilder(catMat).name(catName + " §7Permissions").build());

            // Permission items
            for (int p = 0; p < 7 && p + 2 < cat.length; p++) {
                String perm = cat[p + 2];
                boolean has = hasPerms.contains(perm);
                Material mat = has ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
                String shortName = perm.startsWith("essentials.") ? perm.substring(11) : perm.startsWith("worldedit.") ? perm.substring(10) : perm;

                setItem(baseRow * 9 + 2 + p, new GUIItemBuilder(mat)
                        .name(has ? "§a" + shortName : "§7" + shortName)
                        .lore("§7Full: §f" + perm, has ? "§a✓ Already added" : "§eClick to add", "")
                        .build(), has ? null : event -> {
                            plugin.dispatchCommand("lp group " + groupName + " permission set " + perm + " true");
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Added §f" + perm);
                            refresh(); open();
                        });
            }
        }

        addBackButton(48, () -> new LPPermissionsGUI(player, groupName).open());
        addCloseButton(50);
    }
}

/**
 * Players GUI - Two sides: IN group (left) | separator | NOT in group (right)
 */
class LPPlayersGUI extends GUIFramework {
    private final String groupName;

    public LPPlayersGUI(Player player, String groupName) {
        super(player, "§8§lLuckPerms §7— §aPlayers", 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.PLAYER_HEAD)
                .name("§a§lPlayers §7— §6" + groupName).glowing(true).build());

        // Column headers
        setItem(10, new GUIItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name("§a§l✓ IN GROUP").lore("§7Click to §cremove §7from group").build());
        setItem(13, new GUIItemBuilder(Material.BARRIER)
                .name("§8§l│").lore("§7Separator").build());
        setItem(16, new GUIItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("§c§l✗ NOT IN GROUP").lore("§7Click to §aadd §7to group").build());

        // Get online players and split by group membership
        List<Player> inGroup = new ArrayList<>();
        List<Player> notInGroup = new ArrayList<>();
        Map<UUID, List<String>> playerGroups = new HashMap<>();

        LuckPerms api = Bukkit.getServicesManager().load(LuckPerms.class);
        for (Player online : Bukkit.getOnlinePlayers()) {
            List<String> groups = new ArrayList<>();
            if (api != null) {
                User user = api.getUserManager().getUser(online.getUniqueId());
                if (user != null) {
                    groups = user.getNodes().stream()
                        .filter(n -> n instanceof InheritanceNode)
                        .map(n -> ((InheritanceNode) n).getGroupName())
                        .collect(Collectors.toList());
                }
            }
            playerGroups.put(online.getUniqueId(), groups);
            if (groups.stream().anyMatch(g -> g.equalsIgnoreCase(groupName))) {
                inGroup.add(online);
            } else {
                notInGroup.add(online);
            }
        }

        // Left side: players IN group (slots 19-22, 28-31, 37-40)
        int[] leftSlots = {19, 20, 21, 22, 28, 29, 30, 31, 37, 38, 39, 40};
        for (int i = 0; i < Math.min(inGroup.size(), leftSlots.length); i++) {
            Player p = inGroup.get(i);
            List<String> grps = playerGroups.getOrDefault(p.getUniqueId(), Collections.emptyList());
            String otherGroups = grps.stream().filter(g -> !g.equalsIgnoreCase(groupName)).collect(Collectors.joining("§7, §f"));

            setItem(leftSlots[i], new GUIItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .name("§a§l" + p.getName())
                    .lore("§7Other groups: §f" + (otherGroups.isEmpty() ? "§7None" : otherGroups), "", "§cClick to remove from " + groupName)
                    .build(), event -> {
                        plugin.dispatchCommand("lp user " + p.getName() + " group remove " + groupName);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Removed §f" + p.getName() + " §efrom §f" + groupName);
                        refresh(); open();
                    });
        }

        // Right side: players NOT in group (slots 24-27, 33-36, 42-45)
        int[] rightSlots = {24, 25, 26, 27, 33, 34, 35, 36, 42, 43, 44, 45};
        for (int i = 0; i < Math.min(notInGroup.size(), rightSlots.length); i++) {
            Player p = notInGroup.get(i);
            List<String> grps = playerGroups.getOrDefault(p.getUniqueId(), Collections.emptyList());
            String groupList = String.join("§7, §f", grps);

            setItem(rightSlots[i], new GUIItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .name("§c§l" + p.getName())
                    .lore("§7Current groups: §f" + (groupList.isEmpty() ? "§7None" : groupList), "", "§aClick to add to " + groupName)
                    .build(), event -> {
                        plugin.dispatchCommand("lp user " + p.getName() + " group add " + groupName);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Added §f" + p.getName() + " §ato §f" + groupName);
                        refresh(); open();
                    });
        }

        // Separator column (column 4, rows 2-5)
        ItemStack sep = new GUIItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§8│").build();
        for (int r = 2; r <= 5; r++) setItem(r * 9 + 4, sep);

        // No players message
        if (inGroup.isEmpty()) setItem(19, new GUIItemBuilder(Material.STRUCTURE_VOID).name("§7No players in this group").build());
        if (notInGroup.isEmpty()) setItem(24, new GUIItemBuilder(Material.STRUCTURE_VOID).name("§7All players have this group").build());

        addBackButton(48, () -> new LPGroupGUI(player, groupName).open());
        addCloseButton(50);
    }
}

/**
 * Editor GUI - Simple color palette, weight adjuster, name display
 */
class LPEditorGUI extends GUIFramework {
    private final String groupName;

    // Color palette mapping
    private static final Material[] COLOR_WOOLS = {
        Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
        Material.GREEN_WOOL, Material.CYAN_WOOL, Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL,
        Material.PURPLE_WOOL, Material.MAGENTA_WOOL, Material.PINK_WOOL, Material.WHITE_WOOL,
        Material.LIGHT_GRAY_WOOL, Material.GRAY_WOOL, Material.GRAY_WOOL, Material.BLACK_WOOL
    };
    private static final String[] COLOR_CODES = {
        "&c", "&6", "&e", "&a", "&2", "&3", "&b", "&9", "&5", "&d", "&d", "&f", "&7", "&7", "&8", "&0"
    };
    private static final String[] COLOR_NAMES = {
        "Red", "Orange", "Yellow", "Lime", "Green", "Cyan", "Light Blue", "Blue",
        "Purple", "Magenta", "Pink", "White", "Light Gray", "Gray", "Dark Gray", "Black"
    };

    public LPEditorGUI(Player player, String groupName) {
        super(player, "§8§lLuckPerms §7— §eEditor", 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        PluginDataLoader.GroupInfo g = findGroup();
        String currentPrefix = g != null && g.prefix != null ? g.prefix : "§7None";
        int weight = g != null ? g.weight : 0;

        // Header with preview
        setItem(4, new GUIItemBuilder(Material.GOLDEN_HELMET)
                .name("§e§lEditor §7— §6" + groupName)
                .lore("§7Current prefix: " + currentPrefix, "§7Current weight: §f" + weight, "", "§7Pick a color to auto-set prefix!")
                .glowing(true).build());

        // Color palette label
        setItem(19, new GUIItemBuilder(Material.PAINTING)
                .name("§e§lPick a Color").lore("§7Click to set group prefix", "§7Auto-generates: §f[GroupName]").build());

        // Color palette (slots 20-35, 2 rows of 8)
        for (int i = 0; i < COLOR_WOOLS.length; i++) {
            int slot = 20 + i;
            String colorCode = COLOR_CODES[i];
            String colorName = COLOR_NAMES[i];
            // Build preview of what the prefix will look like
            String preview = ChatColor.translateAlternateColorCodes('&',
                colorCode + "&l[" + groupName + "]&r " + colorCode + "PlayerName");

            setItem(slot, new GUIItemBuilder(COLOR_WOOLS[i])
                    .name("§f" + colorName)
                    .lore("§7Preview:", preview, "", "§eClick to apply")
                    .build(), event -> {
                        String prefix = colorCode + "&l[" + groupName + "]&r " + colorCode;
                        plugin.dispatchCommand("lp group " + groupName + " meta setprefix 100 \"" + prefix + "\"");
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Prefix set! Preview: " + preview);
                        refresh(); open();
                    });
        }

        // Weight section
        setItem(38, new GUIItemBuilder(Material.ANVIL)
                .name("§b§lWeight: §f" + weight)
                .lore("§7Higher = higher priority rank", "", "§7Use buttons below to change")
                .build());

        setItem(39, new GUIItemBuilder(Material.RED_CONCRETE).name("§c§l-10").build(),
            e -> changeWeight(-10));
        setItem(40, new GUIItemBuilder(Material.RED_STAINED_GLASS_PANE).name("§c§l-1").build(),
            e -> changeWeight(-1));
        setItem(41, new GUIItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("§7§l" + weight).build());
        setItem(42, new GUIItemBuilder(Material.LIME_STAINED_GLASS_PANE).name("§a§l+1").build(),
            e -> changeWeight(1));
        setItem(43, new GUIItemBuilder(Material.GREEN_CONCRETE).name("§a§l+10").build(),
            e -> changeWeight(10));

        // Style presets (different bracket styles)
        setItem(47, new GUIItemBuilder(Material.NAME_TAG)
                .name("§d§lStyle Presets")
                .lore("§7Click to change bracket style", "", "§eClick →")
                .build(), e -> { plugin.playSound(player, "click"); new LPStyleGUI(player, groupName).open(); });

        addBackButton(48, () -> new LPGroupGUI(player, groupName).open());
        addCloseButton(50);
    }

    private void changeWeight(int delta) {
        PluginDataLoader.GroupInfo g = findGroup();
        int current = g != null ? g.weight : 0;
        int newWeight = Math.max(0, current + delta);
        plugin.dispatchCommand("lp group " + groupName + " setweight " + newWeight);
        plugin.playSound(player, "success");
        refresh(); open();
    }

    private PluginDataLoader.GroupInfo findGroup() {
        for (PluginDataLoader.GroupInfo g : plugin.getDataLoader().getLuckPermsGroups()) {
            if (g.name.equalsIgnoreCase(groupName)) return g;
        }
        return null;
    }
}

/**
 * Style Presets GUI - Different bracket/symbol styles for the group prefix
 */
class LPStyleGUI extends GUIFramework {
    private final String groupName;

    public LPStyleGUI(Player player, String groupName) {
        super(player, "§8§lLuckPerms §7— §dStyles", 27);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.NAME_TAG)
                .name("§d§lStyle Presets §7— §6" + groupName).glowing(true).build());

        // Detect current color from existing prefix
        PluginDataLoader.GroupInfo g = findGroup();
        String currentColor = "&f";
        if (g != null && g.prefix != null) {
            String prefix = g.prefix;
            if (prefix.contains("§4") || prefix.contains("§c")) currentColor = "&c";
            else if (prefix.contains("§6")) currentColor = "&6";
            else if (prefix.contains("§e")) currentColor = "&e";
            else if (prefix.contains("§a") || prefix.contains("§2")) currentColor = "&a";
            else if (prefix.contains("§b") || prefix.contains("§3")) currentColor = "&b";
            else if (prefix.contains("§9") || prefix.contains("§1")) currentColor = "&9";
            else if (prefix.contains("§5") || prefix.contains("§d")) currentColor = "&5";
            else if (prefix.contains("§7") || prefix.contains("§8")) currentColor = "&7";
        }

        // Style presets: displayName, description, prefixValue, material
        Object[][] styles = {
            {"§f[Name]", "Square Brackets", currentColor + "&l[" + groupName + "]&r " + currentColor, Material.OAK_PLANKS},
            {"§f| Name |", "Pipe Style", currentColor + "&l| " + groupName + " |&r " + currentColor, Material.STICK},
            {"§f{Name}", "Curly Brackets", currentColor + "&l{" + groupName + "}&r " + currentColor, Material.IRON_BARS},
            {"§f- Name -", "Dash Style", currentColor + "&l- " + groupName + " -&r " + currentColor, Material.WHITE_STAINED_GLASS_PANE},
            {"§f★ Name", "Star Prefix", currentColor + "&l★ " + currentColor, Material.GOLD_NUGGET},
            {"§f◆ Name", "Diamond Prefix", currentColor + "&l◆ " + currentColor, Material.DIAMOND},
            {"§f♛ Name", "Crown Prefix", currentColor + "&l♛ " + currentColor, Material.GOLDEN_HELMET},
            {"§f⚔ Name", "Sword Prefix", currentColor + "&l⚔ " + currentColor, Material.IRON_SWORD},
            {"§fName", "No Brackets (Clean)", currentColor + "&l" + groupName + "&r " + currentColor, Material.PAPER},
            {"§f· Name ·", "Dot Style", currentColor + "&l· " + groupName + " ·&r " + currentColor, Material.GUNPOWDER},
            {"§f« Name »", "Arrow Style", currentColor + "&l« " + groupName + " »&r " + currentColor, Material.ARROW},
            {"§f[ N ]", "Short Initials", currentColor + "&l[ " + groupName.charAt(0) + " ]&r " + currentColor, Material.LEVER},
        };

        int[] slots = {9,10,11,12,13,14,15,16,17, 18,19,20};
        for (int i = 0; i < styles.length && i < slots.length; i++) {
            Object[] style = styles[i];
            String displayName = (String) style[0];
            String description = (String) style[1];
            String prefixValue = (String) style[2];
            Material mat = (Material) style[3];
            String preview = ChatColor.translateAlternateColorCodes('&', prefixValue + "PlayerName");

            setItem(slots[i], new GUIItemBuilder(mat)
                    .name("§f" + displayName.replace("Name", groupName))
                    .lore("§7Style: §f" + description, "§7Preview:", preview, "", "§eClick to apply")
                    .build(), event -> {
                        plugin.dispatchCommand("lp group " + groupName + " meta setprefix 100 \"" + prefixValue + "\"");
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Style applied!");
                        new LPEditorGUI(player, groupName).open();
                    });
        }

        addBackButton(22, () -> new LPEditorGUI(player, groupName).open());
        addCloseButton(26);
    }

    private PluginDataLoader.GroupInfo findGroup() {
        for (PluginDataLoader.GroupInfo g : plugin.getDataLoader().getLuckPermsGroups()) {
            if (g.name.equalsIgnoreCase(groupName)) return g;
        }
        return null;
    }
}
