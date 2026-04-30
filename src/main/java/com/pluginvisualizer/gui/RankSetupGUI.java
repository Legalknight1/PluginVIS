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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 3-in-1 Rank Setup GUI - The SIMPLE way to create ranks that work everywhere.
 * Three things: Create Name, Set Permissions, Toggle Display.
 * Keeps LP, Chat, TAB separate but lets you set up all 3 at once.
 */
public class RankSetupGUI extends GUIFramework {

    public RankSetupGUI(Player player) {
        super(player, "§8§lRank Setup §7— 3-in-1", 45);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Title
        setItem(4, new GUIItemBuilder(Material.NETHER_STAR)
                .name("§d§l3-in-1 Rank Setup")
                .lore("§7Create ranks that work across", "§7LuckPerms, TAB & Chat - easily!", "",
                    "§7Three simple steps:", "§a1.§f Create the rank name & style",
                    "§b2.§f Set permissions", "§e3.§f Choose where it shows")
                .glowing(true).build());

        // Step 1: Create Rank
        setItem(19, new GUIItemBuilder(Material.EMERALD)
                .name("§a§l1. Create Rank")
                .lore("§7Pick a name, color & symbol",
                    "§7Auto-creates in LP, TAB & Chat",
                    "",
                    "§7This is the starting point -",
                    "§7creates the rank everywhere at once!",
                    "", "§eClick to start →")
                .glowing(true)
                .build(), e -> {
                    plugin.playSound(player, "click");
                    new RankCreateGUI(player).open();
                });

        // Step 2: Set Permissions
        setItem(22, new GUIItemBuilder(Material.SHIELD)
                .name("§b§l2. Set Permissions")
                .lore("§7Pick a rank, then set its perms",
                    "§7Organized by plugin category",
                    "§7Just like the MV Rules system!",
                    "",
                    "§eClick to manage →")
                .build(), e -> {
                    plugin.playSound(player, "click");
                    new RankPermsGUI(player).open();
                });

        // Step 3: Display Toggles
        setItem(25, new GUIItemBuilder(Material.COMPARATOR)
                .name("§e§l3. Display Settings")
                .lore("§7Choose WHERE the rank shows",
                    "§7Toggle: Chat, TAB, Nametag, Below",
                    "§7See a live preview of each!",
                    "",
                    "§eClick to configure →")
                .build(), e -> {
                    plugin.playSound(player, "click");
                    new RankDisplayGUI(player).open();
                });

        // Existing ranks quick view
        List<PluginDataLoader.GroupInfo> groups = plugin.getDataLoader().getLuckPermsGroups();
        int[] quickSlots = {28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < Math.min(groups.size(), quickSlots.length); i++) {
            PluginDataLoader.GroupInfo g = groups.get(i);
            String prefix = g.prefix != null ? g.prefix : "§7None";
            setItem(quickSlots[i], new GUIItemBuilder(Material.PAPER)
                    .name("§6§l" + g.name)
                    .lore("§7Prefix: " + prefix, "§7Weight: §f" + g.weight, "§7Perms: §f" + g.permissionCount)
                    .build());
        }

        if (groups.isEmpty()) {
            setItem(31, new GUIItemBuilder(Material.STRUCTURE_VOID)
                    .name("§7No ranks yet")
                    .lore("§7Use §aStep 1 §7to create one!")
                    .build());
        }

        addBackButton(36, () -> new MainMenuGUI(player).open());
        addCloseButton(44);
    }
}

/**
 * Step 1: Create a rank - Name, Color, Symbol, then auto-create in LP + TAB + Chat
 */
class RankCreateGUI extends GUIFramework {

    private String rankName;
    private String selectedColor = "&f";
    private String selectedStyle = "&l[{NAME}]&r ";
    private int weight = 0;

    // Color palette
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

    // Style presets
    private static final String[][] STYLES = {
        {"[Name]", "&l[{NAME}]&r "},
        {"| Name |", "&l| {NAME} |&r "},
        {"{Name}", "&l{{NAME}}&r "},
        {"★ Name", "&l★ &r"},
        {"◆ Name", "&l◆ &r"},
        {"♛ Name", "&l♛ &r"},
        {"Name", "&l{NAME}&r "},
        {"· Name ·", "&l· {NAME} ·&r "},
    };

    public RankCreateGUI(Player player) {
        super(player, "§8§lRank Setup §7— Create", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Title
        setItem(4, new GUIItemBuilder(Material.EMERALD)
                .name("§a§lCreate New Rank")
                .lore("§7Set name, color & style below", "§7then click CREATE to apply everywhere!")
                .glowing(true).build());

        // ===== NAME INPUT =====
        setItem(10, new GUIItemBuilder(Material.PAPER)
                .name("§e§lRank Name")
                .lore(
                    "§7Click to type a name",
                    "",
                    "§7Current: §f" + (rankName != null ? rankName : "§cNot set"),
                    "",
                    "§eClick to set")
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter rank name (no spaces, e.g. Admin, VIP, Mod):",
                        input -> {
                            rankName = input.replace(" ", "_");
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Name set to §f" + rankName);
                            refresh(); open();
                        }, input -> input.contains(" ") ? "No spaces allowed!" : null);
                });

        // ===== COLOR PALETTE =====
        setItem(19, new GUIItemBuilder(Material.PAINTING)
                .name("§e§lPick Color")
                .lore("§7Current: " + ChatColor.translateAlternateColorCodes('&', selectedColor + "This Color"))
                .build());

        for (int i = 0; i < COLOR_WOOLS.length; i++) {
            int slot = 20 + i;
            if (slot > 34) break;
            String cc = COLOR_CODES[i];
            String cn = COLOR_NAMES[i];
            boolean isSelected = cc.equals(selectedColor);

            setItem(slot, new GUIItemBuilder(COLOR_WOOLS[i])
                    .name((isSelected ? "§a§l✔ " : "§f") + cn)
                    .lore("§7Preview: " + ChatColor.translateAlternateColorCodes('&', cc + "&l" + (rankName != null ? rankName : "Rank")),
                        isSelected ? "§a§lSelected!" : "§eClick to select")
                    .build(), e -> {
                        selectedColor = cc;
                        plugin.playSound(player, "click");
                        refresh(); open();
                    });
        }

        // ===== STYLE PRESETS =====
        setItem(37, new GUIItemBuilder(Material.NAME_TAG)
                .name("§d§lPick Style")
                .lore("§7Choose how the prefix looks")
                .build());

        for (int i = 0; i < STYLES.length; i++) {
            int slot = 38 + i;
            if (slot > 44) break;
            String[] style = STYLES[i];
            String label = style[0];
            String template = style[1];
            boolean isSelected = template.equals(selectedStyle);

            String styledName = rankName != null ? rankName : "Rank";
            String prefix = selectedColor + template.replace("{NAME}", styledName);
            String preview = ChatColor.translateAlternateColorCodes('&', prefix + selectedColor + "PlayerName");

            setItem(slot, new GUIItemBuilder(isSelected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                    .name((isSelected ? "§a§l✔ " : "§7") + label.replace("Name", styledName))
                    .lore("§7Preview: " + preview, isSelected ? "§a§lSelected!" : "§eClick to select")
                    .build(), e -> {
                        selectedStyle = template;
                        plugin.playSound(player, "click");
                        refresh(); open();
                    });
        }

        // ===== WEIGHT =====
        setItem(46, new GUIItemBuilder(Material.ANVIL)
                .name("§b§lWeight: §f" + weight)
                .lore("§7Higher = higher priority", "", "§eLeft click +1 §7| §eRight click -1",
                    "§eShift+Left +10 §7| §eShift+Right -10")
                .build(), e -> {
                    if (e.isShiftClick()) {
                        weight += e.isLeftClick() ? 10 : -10;
                    } else {
                        weight += e.isLeftClick() ? 1 : -1;
                    }
                    weight = Math.max(0, weight);
                    plugin.playSound(player, "click");
                    refresh(); open();
                });

        // ===== LIVE PREVIEW =====
        String styledName = rankName != null ? rankName : "Rank";
        String prefix = selectedColor + selectedStyle.replace("{NAME}", styledName);
        String previewChat = ChatColor.translateAlternateColorCodes('&', prefix + selectedColor + "PlayerName§7: §fHello!");
        String previewTab = ChatColor.translateAlternateColorCodes('&', prefix + selectedColor + "PlayerName");

        setItem(48, new GUIItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .name("§a§lLive Preview")
                .lore("§7━━━ Chat ━━━", previewChat,
                    "", "§7━━━ TAB ━━━", previewTab)
                .build());

        // ===== CREATE BUTTON =====
        boolean canCreate = rankName != null && !rankName.isEmpty();
        setItem(50, new GUIItemBuilder(canCreate ? Material.LIME_WOOL : Material.GRAY_WOOL)
                .name(canCreate ? "§a§l✓ CREATE RANK!" : "§7§l✗ Set a name first")
                .lore(
                    canCreate ? "§7This will create the rank in:" : "§7Enter a name first!",
                    canCreate ? "§6  • LuckPerms (group + prefix + weight)" : "",
                    canCreate ? "§b  • TAB (tabprefix + tagprefix + belowname)" : "",
                    canCreate ? "§e  • Chat (group format)" : "",
                    "",
                    canCreate ? "§a§lClick to create!" : "§8Name required")
                .glowing(canCreate)
                .build(), canCreate ? e -> createRank() : null);

        addBackButton(45, () -> new RankSetupGUI(player).open());
        addCloseButton(53);
    }

    private void createRank() {
        String styledName = rankName;
        String prefix = selectedColor + selectedStyle.replace("{NAME}", styledName);
        String cleanPrefix = selectedColor + selectedStyle.replace("{NAME}", styledName);

        // 1. Create in LuckPerms
        plugin.dispatchCommand("lp creategroup " + rankName);
        plugin.dispatchCommand("lp group " + rankName + " meta setprefix 100 \"" + cleanPrefix + "\"");
        if (weight > 0) {
            plugin.dispatchCommand("lp group " + rankName + " setweight " + weight);
        }

        // 2. Create in TAB
        PluginDataLoader.TABGroupInfo tabGroup = new PluginDataLoader.TABGroupInfo();
        tabGroup.name = rankName;
        tabGroup.tabprefix = cleanPrefix;
        tabGroup.tagprefix = cleanPrefix;
        tabGroup.tabsuffix = "";
        tabGroup.tagsuffix = "";
        tabGroup.customtabname = "";
        tabGroup.belowname = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
            selectedColor + styledName));
        plugin.getDataLoader().saveTABGroup(tabGroup);

        // 3. Create Chat format
        String chatFormat = "&7[&r" + cleanPrefix + "&7] &r{DISPLAYNAME}&7:&r {MESSAGE}";
        plugin.getDataLoader().saveEssentialsChatFormat(rankName, chatFormat);

        plugin.playSound(player, "success");
        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Rank §f" + rankName + " §acreated in LuckPerms + TAB + Chat!");
        player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Prefix: " + ChatColor.translateAlternateColorCodes('&', cleanPrefix + styledName));

        // Go to permissions step
        new RankPermsGUI(player).open();
    }
}

/**
 * Step 2: Set Permissions - Pick a rank, then toggle perms by plugin category
 */
class RankPermsGUI extends GUIFramework {

    public RankPermsGUI(Player player) {
        super(player, "§8§lRank Setup §7— Perms", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.SHIELD)
                .name("§b§lSet Permissions")
                .lore("§7Pick a rank, then add permissions", "§7organized by plugin category")
                .glowing(true).build());

        // List all LuckPerms groups
        List<PluginDataLoader.GroupInfo> groups = plugin.getDataLoader().getLuckPermsGroups();
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34};

        Material[] rankMats = {Material.NETHERITE_HELMET, Material.DIAMOND_HELMET, Material.GOLDEN_HELMET,
            Material.IRON_HELMET, Material.CHAINMAIL_HELMET, Material.LEATHER_HELMET};

        for (int i = 0; i < Math.min(groups.size(), slots.length); i++) {
            PluginDataLoader.GroupInfo g = groups.get(i);
            int mi = Math.min(g.weight / 20, rankMats.length - 1);
            Material mat = g.weight > 0 ? rankMats[mi] : Material.LEATHER_HELMET;
            String prefix = g.prefix != null ? g.prefix : "§7None";

            setItem(slots[i], new GUIItemBuilder(mat)
                    .name("§6§l" + g.name)
                    .lore("§7Prefix: " + prefix, "§7Weight: §f" + g.weight,
                        "§7Perms: §f" + g.permissionCount, "", "§eClick to set perms →")
                    .build(), event -> {
                        plugin.playSound(player, "click");
                        new RankPermsByPluginGUI(player, g.name).open();
                    });
        }

        if (groups.isEmpty()) {
            setItem(22, new GUIItemBuilder(Material.STRUCTURE_VOID)
                    .name("§7No ranks found")
                    .lore("§7Create one in §aStep 1 §7first!")
                    .build());
        }

        addBackButton(48, () -> new RankSetupGUI(player).open());
        addCloseButton(50);
    }
}

/**
 * Permissions by Plugin - Like MV Rules system, list plugins then show perms for each
 */
class RankPermsByPluginGUI extends GUIFramework {
    private final String groupName;

    // Permission categories: {name, material, perm1, perm2, ...}
    private static final Object[][] PLUGIN_CATEGORIES = {
        {"§bMovement", Material.IRON_BOOTS, "essentials.fly", "essentials.speed", "essentials.gamemode", "essentials.god", "essentials.feed", "essentials.heal", "essentials.repair"},
        {"§dTeleport", Material.ENDER_PEARL, "essentials.tp", "essentials.tpa", "essentials.home", "essentials.warp", "essentials.sethome", "essentials.setwarp", "essentials.back"},
        {"§cAdmin", Material.DIAMOND_SWORD, "essentials.ban", "essentials.kick", "essentials.mute", "essentials.tempban", "essentials.invsee", "essentials.vanish", "essentials.enderchest"},
        {"§6WorldEdit", Material.WOODEN_AXE, "worldedit.*", "worldedit.selection.*", "worldedit.clipboard.*", "worldedit.region.*", "worldedit.generation.*", "worldedit.history.*", "worldedit.navigation.*"},
        {"§eChat", Material.OAK_SIGN, "essentials.chat.color", "essentials.chat.bold", "essentials.chat.italic", "essentials.msg", "essentials.nick", "essentials.me", "essentials.mail"},
        {"§aEconomy", Material.GOLD_INGOT, "essentials.pay", "essentials.balance", "essentials.sell", "essentials.worth", "essentials.eco", "essentials.give", "essentials.kit"},
    };

    public RankPermsByPluginGUI(Player player, String groupName) {
        super(player, "§8§lPerms §7— §6" + groupName, 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.SHIELD)
                .name("§b§lPermissions §7— §6" + groupName)
                .lore("§7Click a plugin to toggle its perms")
                .glowing(true).build());

        // Get existing perms
        List<PluginDataLoader.PermInfo> existing = plugin.getDataLoader().getLuckPermsPermissions(groupName);
        Set<String> hasPerms = existing.stream().filter(p -> p.value).map(p -> p.permission).collect(Collectors.toSet());

        // Show each plugin category as a clickable item
        int[] catSlots = {19, 21, 23, 25, 28, 30};

        for (int c = 0; c < PLUGIN_CATEGORIES.length && c < catSlots.length; c++) {
            Object[] cat = PLUGIN_CATEGORIES[c];
            String catName = (String) cat[0];
            Material catMat = (Material) cat[1];

            // Count how many perms from this category are enabled
            int enabled = 0;
            int total = cat.length - 2;
            for (int p = 2; p < cat.length; p++) {
                if (hasPerms.contains((String) cat[p])) enabled++;
            }

            final int catIndex = c;
            setItem(catSlots[c], new GUIItemBuilder(catMat)
                    .name(catName + " §7Permissions")
                    .lore("§7" + enabled + "/" + total + " enabled",
                        enabled == total ? "§aAll enabled!" : enabled > 0 ? "§eSome enabled" : "§7None enabled",
                        "", "§eClick to toggle →")
                    .glowing(enabled == total)
                    .build(), e -> {
                        plugin.playSound(player, "click");
                        new RankPermToggleGUI(player, groupName, catIndex).open();
                    });
        }

        // Custom perm
        setItem(33, new GUIItemBuilder(Material.PAPER)
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

        // Current perms count
        setItem(40, new GUIItemBuilder(Material.BOOK)
                .name("§7§lCurrent Perms: §f" + hasPerms.size())
                .lore("§7Click a category above to toggle")
                .build());

        addBackButton(48, () -> new RankPermsGUI(player).open());
        addCloseButton(50);
    }
}

/**
 * Toggle permissions for a specific plugin category
 */
class RankPermToggleGUI extends GUIFramework {
    private final String groupName;
    private final int categoryIndex;

    private static final Object[][] PLUGIN_CATEGORIES = {
        {"§bMovement", Material.IRON_BOOTS, "essentials.fly", "essentials.speed", "essentials.gamemode", "essentials.god", "essentials.feed", "essentials.heal", "essentials.repair"},
        {"§dTeleport", Material.ENDER_PEARL, "essentials.tp", "essentials.tpa", "essentials.home", "essentials.warp", "essentials.sethome", "essentials.setwarp", "essentials.back"},
        {"§cAdmin", Material.DIAMOND_SWORD, "essentials.ban", "essentials.kick", "essentials.mute", "essentials.tempban", "essentials.invsee", "essentials.vanish", "essentials.enderchest"},
        {"§6WorldEdit", Material.WOODEN_AXE, "worldedit.*", "worldedit.selection.*", "worldedit.clipboard.*", "worldedit.region.*", "worldedit.generation.*", "worldedit.history.*", "worldedit.navigation.*"},
        {"§eChat", Material.OAK_SIGN, "essentials.chat.color", "essentials.chat.bold", "essentials.chat.italic", "essentials.msg", "essentials.nick", "essentials.me", "essentials.mail"},
        {"§aEconomy", Material.GOLD_INGOT, "essentials.pay", "essentials.balance", "essentials.sell", "essentials.worth", "essentials.eco", "essentials.give", "essentials.kit"},
    };

    public RankPermToggleGUI(Player player, String groupName, int categoryIndex) {
        super(player, "§8§lPerms §7— Toggle", 45);
        this.groupName = groupName;
        this.categoryIndex = categoryIndex;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        Object[] cat = PLUGIN_CATEGORIES[categoryIndex];
        String catName = (String) cat[0];
        Material catMat = (Material) cat[1];

        setItem(4, new GUIItemBuilder(catMat)
                .name(catName + " §7— §6" + groupName)
                .lore("§7Click to toggle on/off")
                .glowing(true).build());

        // Get existing perms
        List<PluginDataLoader.PermInfo> existing = plugin.getDataLoader().getLuckPermsPermissions(groupName);
        Set<String> hasPerms = existing.stream().filter(p -> p.value).map(p -> p.permission).collect(Collectors.toSet());

        // Toggle all button
        boolean allEnabledCheck = true;
        for (int p = 2; p < cat.length; p++) {
            if (!hasPerms.contains((String) cat[p])) { allEnabledCheck = false; break; }
        }
        final boolean allEnabled = allEnabledCheck;

        setItem(8, new GUIItemBuilder(allEnabled ? Material.LIME_WOOL : Material.RED_WOOL)
                .name(allEnabled ? "§a§lDisable All" : "§c§lEnable All")
                .lore("§7Toggle all permissions in this category", "", "§eClick")
                .build(), e -> {
                    for (int p = 2; p < cat.length; p++) {
                        String permStr = (String) cat[p];
                        if (allEnabled) {
                            plugin.dispatchCommand("lp group " + groupName + " permission unset " + permStr);
                        } else {
                            plugin.dispatchCommand("lp group " + groupName + " permission set " + permStr + " true");
                        }
                    }
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + (allEnabled ? "Removed" : "Added") + " all " + catName + " §aperms!");
                    refresh(); open();
                });

        // Individual perm toggles
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < cat.length - 2 && i < slots.length; i++) {
            String perm = (String) cat[i + 2];
            boolean has = hasPerms.contains(perm);
            String shortName = perm.startsWith("essentials.") ? perm.substring(11) :
                              perm.startsWith("worldedit.") ? perm.substring(10) : perm;

            setItem(slots[i], new GUIItemBuilder(has ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                    .name(has ? "§a§l✔ " + shortName : "§c§l✗ " + shortName)
                    .lore("§7Full: §f" + perm, "§7Status: " + (has ? "§aENABLED" : "§cDISABLED"), "", "§eClick to toggle")
                    .build(), e -> {
                        if (has) {
                            plugin.dispatchCommand("lp group " + groupName + " permission unset " + perm);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Removed §f" + perm);
                        } else {
                            plugin.dispatchCommand("lp group " + groupName + " permission set " + perm + " true");
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Added §f" + perm);
                        }
                        refresh(); open();
                    });
        }

        addBackButton(36, () -> new RankPermsByPluginGUI(player, groupName).open());
        addCloseButton(44);
    }
}

/**
 * Step 3: Display Settings - Toggle where a rank shows with previews
 */
class RankDisplayGUI extends GUIFramework {

    public RankDisplayGUI(Player player) {
        super(player, "§8§lRank Setup §7— Display", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.COMPARATOR)
                .name("§e§lDisplay Settings")
                .lore("§7Pick a rank, then choose where it shows", "§7with live previews!")
                .glowing(true).build());

        // List all groups
        List<PluginDataLoader.GroupInfo> groups = plugin.getDataLoader().getLuckPermsGroups();
        List<PluginDataLoader.TABGroupInfo> tabGroups = plugin.getDataLoader().getTABGroups();

        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25};

        for (int i = 0; i < Math.min(groups.size(), slots.length); i++) {
            PluginDataLoader.GroupInfo g = groups.get(i);
            String prefix = g.prefix != null ? g.prefix : "§7None";

            // Find matching TAB group
            PluginDataLoader.TABGroupInfo tabG = tabGroups.stream()
                .filter(t -> t.name.equalsIgnoreCase(g.name)).findFirst().orElse(null);

            // Count display locations
            int displays = 0;
            if (g.prefix != null && !g.prefix.isEmpty()) displays++; // LP prefix = Chat
            if (tabG != null && tabG.tabprefix != null && !tabG.tabprefix.isEmpty()) displays++; // TAB
            if (tabG != null && tabG.tagprefix != null && !tabG.tagprefix.isEmpty()) displays++; // Nametag
            if (tabG != null && tabG.belowname != null && !tabG.belowname.isEmpty()) displays++; // Below name

            setItem(slots[i], new GUIItemBuilder(Material.PAPER)
                    .name("§6§l" + g.name)
                    .lore("§7Prefix: " + prefix,
                        "§7Showing in: §f" + displays + " places",
                        "", "§eClick to toggle displays →")
                    .build(), e -> {
                        plugin.playSound(player, "click");
                        new RankDisplayToggleGUI(player, g.name).open();
                    });
        }

        if (groups.isEmpty()) {
            setItem(22, new GUIItemBuilder(Material.STRUCTURE_VOID)
                    .name("§7No ranks found")
                    .lore("§7Create one in §aStep 1 §7first!")
                    .build());
        }

        addBackButton(48, () -> new RankSetupGUI(player).open());
        addCloseButton(50);
    }
}

/**
 * Toggle display locations for a specific rank
 */
class RankDisplayToggleGUI extends GUIFramework {
    private final String groupName;

    public RankDisplayToggleGUI(Player player, String groupName) {
        super(player, "§8§lDisplay §7— §6" + groupName, 45);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        PluginDataLoader.GroupInfo lpGroup = null;
        for (PluginDataLoader.GroupInfo g : plugin.getDataLoader().getLuckPermsGroups()) {
            if (g.name.equalsIgnoreCase(groupName)) { lpGroup = g; break; }
        }

        PluginDataLoader.TABGroupInfo tabGroup = null;
        for (PluginDataLoader.TABGroupInfo g : plugin.getDataLoader().getTABGroups()) {
            if (g.name.equalsIgnoreCase(groupName)) { tabGroup = g; break; }
        }

        // Make final for lambda usage
        final PluginDataLoader.GroupInfo lp = lpGroup;
        final PluginDataLoader.TABGroupInfo tab = tabGroup;
        final String prefix = lp != null && lp.prefix != null ? lp.prefix : "§7None";

        setItem(4, new GUIItemBuilder(Material.COMPARATOR)
                .name("§e§lDisplay §7— §6" + groupName)
                .lore("§7Prefix: " + prefix, "§7Toggle WHERE this rank shows")
                .glowing(true).build());

        // ===== 1. Chat Display =====
        final boolean chatOn = lp != null && lp.prefix != null && !lp.prefix.isEmpty();
        String chatPreview = chatOn ? ChatColor.translateAlternateColorCodes('&', prefix + "PlayerName§7: §fHello!") : "§7[Disabled] PlayerName: Hello!";

        setItem(19, new GUIItemBuilder(chatOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name("§e§lChat Display")
                .lore("§7Shows in chat messages", "",
                    "§7Preview: " + chatPreview,
                    "§7Status: " + (chatOn ? "§aON" : "§cOFF"),
                    "", "§eClick to " + (chatOn ? "disable" : "enable"))
                .build(), e -> {
                    if (chatOn) {
                        plugin.dispatchCommand("lp group " + groupName + " meta removeprefix 100");
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Chat display disabled!");
                    } else {
                        String newPrefix = "&7[&f" + groupName + "&7] &f";
                        if (tab != null && tab.tabprefix != null && !tab.tabprefix.isEmpty()) {
                            newPrefix = tab.tabprefix;
                        }
                        plugin.dispatchCommand("lp group " + groupName + " meta setprefix 100 \"" + newPrefix + "\"");
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chat display enabled!");
                    }
                    refresh(); open();
                });

        // ===== 2. TAB List Display =====
        final boolean tabOn = tab != null && tab.tabprefix != null && !tab.tabprefix.isEmpty();
        String tabPreview = tabOn ? ChatColor.translateAlternateColorCodes('&', tab.tabprefix + "PlayerName" + (tab.tabsuffix != null ? tab.tabsuffix : "")) : "§7PlayerName [Disabled]";

        setItem(21, new GUIItemBuilder(tabOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name("§b§lTAB List Display")
                .lore("§7Shows in the player list (Tab key)", "",
                    "§7Preview: " + tabPreview,
                    "§7Status: " + (tabOn ? "§aON" : "§cOFF"),
                    "", "§eClick to " + (tabOn ? "disable" : "enable"))
                .build(), e -> {
                    PluginDataLoader.TABGroupInfo g = loadOrCreateTAB();
                    if (tabOn) {
                        g.tabprefix = "";
                        g.tabsuffix = "";
                    } else {
                        g.tabprefix = lp != null && lp.prefix != null ? lp.prefix : "&7[" + groupName + "] &f";
                    }
                    plugin.getDataLoader().saveTABGroup(g);
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "TAB display " + (tabOn ? "disabled" : "enabled") + "!");
                    refresh(); open();
                });

        // ===== 3. Nametag Display (above head) =====
        final boolean tagOn = tab != null && tab.tagprefix != null && !tab.tagprefix.isEmpty();
        String tagPreview = tagOn ? ChatColor.translateAlternateColorCodes('&', tab.tagprefix + "PlayerName" + (tab.tagsuffix != null ? tab.tagsuffix : "")) : "§7PlayerName [Disabled]";

        setItem(23, new GUIItemBuilder(tagOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name("§d§lNametag Display")
                .lore("§7Shows above player's head", "",
                    "§7Preview: " + tagPreview,
                    "§7Status: " + (tagOn ? "§aON" : "§cOFF"),
                    "", "§eClick to " + (tagOn ? "disable" : "enable"))
                .build(), e -> {
                    PluginDataLoader.TABGroupInfo g = loadOrCreateTAB();
                    if (tagOn) {
                        g.tagprefix = "";
                        g.tagsuffix = "";
                    } else {
                        g.tagprefix = lp != null && lp.prefix != null ? lp.prefix : "&7[" + groupName + "] &f";
                    }
                    plugin.getDataLoader().saveTABGroup(g);
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Nametag display " + (tagOn ? "disabled" : "enabled") + "!");
                    refresh(); open();
                });

        // ===== 4. Below Name Display =====
        final boolean belowOn = tab != null && tab.belowname != null && !tab.belowname.isEmpty();
        String belowPreview = belowOn ? ChatColor.translateAlternateColorCodes('&', tab.belowname) : "§7[Disabled]";

        setItem(25, new GUIItemBuilder(belowOn ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                .name("§c§lBelow Name Display")
                .lore("§7Shows below the player's name", "",
                    "§7Value: " + belowPreview,
                    "§7Status: " + (belowOn ? "§aON" : "§cOFF"),
                    "", "§eClick to " + (belowOn ? "disable" : "enable"))
                .build(), e -> {
                    PluginDataLoader.TABGroupInfo g = loadOrCreateTAB();
                    if (belowOn) {
                        g.belowname = "";
                    } else {
                        g.belowname = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&',
                            lp != null && lp.prefix != null ? lp.prefix : "&7" + groupName));
                    }
                    plugin.getDataLoader().saveTABGroup(g);
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Below name display " + (belowOn ? "disabled" : "enabled") + "!");
                    refresh(); open();
                });

        // ===== COPY PREFIX TO ALL =====
        setItem(31, new GUIItemBuilder(Material.STRUCTURE_BLOCK)
                .name("§6§lCopy Prefix → All")
                .lore("§7Copies the LP prefix to",
                    "§7TAB, Nametag, and Below Name",
                    "", "§eClick to apply")
                .build(), e -> {
                    if (lp != null && lp.prefix != null && !lp.prefix.isEmpty()) {
                        PluginDataLoader.TABGroupInfo g = loadOrCreateTAB();
                        g.tabprefix = lp.prefix;
                        g.tagprefix = lp.prefix;
                        g.belowname = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', lp.prefix));
                        plugin.getDataLoader().saveTABGroup(g);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Prefix copied to all display locations!");
                    } else {
                        plugin.playSound(player, "error");
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Set a prefix first!");
                    }
                    refresh(); open();
                });

        addBackButton(36, () -> new RankDisplayGUI(player).open());
        addCloseButton(44);
    }

    private PluginDataLoader.TABGroupInfo loadOrCreateTAB() {
        for (PluginDataLoader.TABGroupInfo g : plugin.getDataLoader().getTABGroups()) {
            if (g.name.equalsIgnoreCase(groupName)) return g;
        }
        PluginDataLoader.TABGroupInfo g = new PluginDataLoader.TABGroupInfo();
        g.name = groupName;
        g.tabprefix = ""; g.tabsuffix = ""; g.tagprefix = "";
        g.tagsuffix = ""; g.customtabname = ""; g.belowname = "";
        return g;
    }
}
