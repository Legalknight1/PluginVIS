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
 * TAB Main GUI - Same layout as LuckPerms: groups in middle, Create+Refresh at bottom center.
 * Shows all existing TAB groups from config.
 */
public class TABGUI extends GUIFramework {

    private int page = 0;
    private static final int SLOTS_PER_PAGE = 21;
    // Middle area: 3 rows of 7 (rows 2-4, columns 2-8) - same as LuckPerms
    private static final int[] GROUP_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public TABGUI(Player player) {
        super(player, "§8§lTAB §7— Groups", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Header
        setItem(4, new GUIItemBuilder(Material.NAME_TAG)
                .name("§b§lTAB §7— Groups")
                .lore("§7Click a group to configure display")
                .glowing(true).build());

        // Bottom center: Create Group + Reload TAB (slots 48, 50) - same as LuckPerms
        setItem(48, new GUIItemBuilder(Material.EMERALD)
                .name("§a§l+ Create Group")
                .lore("§7Add a new TAB group", "", "§eClick →")
                .build(), e -> createGroup());

        setItem(50, new GUIItemBuilder(Material.COMPARATOR)
                .name("§7§l↻ Reload TAB")
                .lore("§7Reload TAB config & refresh", "", "§eClick")
                .build(), e -> {
                    plugin.dispatchCommand("tab reload");
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "TAB reloaded!");
                    refresh(); open();
                });

        // Load existing TAB groups into the middle area
        List<PluginDataLoader.TABGroupInfo> groups = plugin.getDataLoader().getTABGroups();
        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, groups.size());

        for (int i = start; i < end; i++) {
            int si = i - start;
            if (si >= GROUP_SLOTS.length) break;
            PluginDataLoader.TABGroupInfo g = groups.get(i);

            int configured = 0;
            if (g.tabprefix != null && !g.tabprefix.isEmpty()) configured++;
            if (g.tagprefix != null && !g.tagprefix.isEmpty()) configured++;
            if (g.belowname != null && !g.belowname.isEmpty()) configured++;
            if (g.tabsuffix != null && !g.tabsuffix.isEmpty()) configured++;

            // Material based on how much is configured
            Material mat;
            if (configured >= 3) mat = Material.GOLDEN_APPLE;
            else if (configured >= 1) mat = Material.APPLE;
            else mat = Material.BUCKET;

            // Build a preview of what this group looks like
            String tabPreview = buildPreview(g.tabprefix, g.tabsuffix, g.name);

            setItem(GROUP_SLOTS[si], new GUIItemBuilder(mat)
                    .name("§b§l" + g.name)
                    .lore("§7Preview: " + tabPreview,
                        "§7Configured: §f" + configured + "/4", "", "§eClick to configure →")
                    .build(), event -> { plugin.playSound(player, "click"); new TABGroupGUI(player, g.name).open(); });
        }

        // Pagination
        if (page > 0) setItem(45, new GUIItemBuilder(Material.ARROW).name("§c← Prev").build(),
            e -> { page--; refresh(); });
        if (end < groups.size()) setItem(53, new GUIItemBuilder(Material.ARROW).name("§aNext →").build(),
            e -> { page++; refresh(); });

        // Empty state
        if (groups.isEmpty()) {
            setItem(22, new GUIItemBuilder(Material.STRUCTURE_VOID)
                    .name("§7No TAB groups found")
                    .lore("§7Click §a+ Create Group §7to make one!")
                    .build());
        }

        addBackButton(49, () -> new MainMenuGUI(player).open());
    }

    private String buildPreview(String prefix, String suffix, String name) {
        String p = prefix != null && !prefix.isEmpty() ? ChatColor.translateAlternateColorCodes('&', prefix) : "§7";
        String s = suffix != null && !suffix.isEmpty() ? ChatColor.translateAlternateColorCodes('&', suffix) : "";
        return p + "PlayerName" + s;
    }

    private void createGroup() {
        plugin.getChatInputManager().requestInput(player, "Enter TAB group name:",
            input -> {
                // Check if group already exists
                boolean exists = plugin.getDataLoader().getTABGroups().stream()
                    .anyMatch(g -> g.name.equalsIgnoreCase(input));
                if (exists) {
                    plugin.playSound(player, "error");
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "TAB group '" + input + "' already exists!");
                    new TABGUI(player).open();
                    return;
                }
                
                // Create the group - explicitly set all fields to ensure section is created
                PluginDataLoader.TABGroupInfo newGroup = new PluginDataLoader.TABGroupInfo();
                newGroup.name = input;
                newGroup.tabprefix = "";
                newGroup.tabsuffix = "";
                newGroup.tagprefix = "";
                newGroup.tagsuffix = "";
                newGroup.customtabname = "";
                newGroup.belowname = "";
                
                if (plugin.getDataLoader().saveTABGroup(newGroup)) {
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Created TAB group §f" + input + "§a! Configure it now.");
                    // Open the new group directly so user can configure it
                    new TABGroupGUI(player, input).open();
                } else {
                    plugin.playSound(player, "error");
                    player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to create TAB group. Is TAB installed?");
                    new TABGUI(player).open();
                }
            }, input -> input.contains(" ") ? "No spaces!" : null);
    }
}

/**
 * TAB Group Detail - 3 openers: Prefixes, Players, TEXT Templates
 */
class TABGroupGUI extends GUIFramework {
    private final String groupName;

    public TABGroupGUI(Player player, String groupName) {
        super(player, "§8§lTAB §7— §b" + groupName, 36);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        PluginDataLoader.TABGroupInfo g = loadGroup();
        String tabPreview = "";
        if (g != null) {
            String p = g.tabprefix != null && !g.tabprefix.isEmpty() ? ChatColor.translateAlternateColorCodes('&', g.tabprefix) : "§7";
            String s = g.tabsuffix != null && !g.tabsuffix.isEmpty() ? ChatColor.translateAlternateColorCodes('&', g.tabsuffix) : "";
            tabPreview = p + "PlayerName" + s;
        }

        setItem(4, new GUIItemBuilder(Material.NAME_TAG)
                .name("§b§l" + groupName)
                .lore("§7Preview: " + tabPreview)
                .glowing(true).build());

        // 3 Openers - same layout as LuckPerms LPGroupGUI
        setItem(11, new GUIItemBuilder(Material.JUNGLE_SIGN)
                .name("§d§lPrefixes")
                .lore("§7Choose WHERE this rank shows", "§7Tab list, above head, below name", "", "§eClick to open →")
                .build(), e -> { plugin.playSound(player, "click"); new TABPrefixesGUI(player, groupName).open(); });

        setItem(13, new GUIItemBuilder(Material.PLAYER_HEAD)
                .name("§a§lPlayers")
                .lore("§7See who has this rank", "§7Click players to add/remove", "", "§eClick to open →")
                .build(), e -> { plugin.playSound(player, "click"); new TABPlayersGUI(player, groupName).open(); });

        setItem(15, new GUIItemBuilder(Material.PAINTING)
                .name("§e§lTEXT Templates")
                .lore("§7Pre-made style templates", "§7Click to apply, hover to preview", "§7Auto-generates full config!", "", "§eClick to open →")
                .build(), e -> { plugin.playSound(player, "click"); new TABTextGUI(player, groupName).open(); });

        // Delete
        setItem(29, new GUIItemBuilder(Material.TNT)
                .name("§c§lDelete Group")
                .lore("§7Remove this TAB group", "", "§eClick")
                .build(), e -> {
                    plugin.getDataLoader().deleteTABGroup(groupName);
                    plugin.playSound(player, "success");
                    new TABGUI(player).open();
                });

        addBackButton(27, () -> new TABGUI(player).open());
        addCloseButton(35);
    }

    private PluginDataLoader.TABGroupInfo loadGroup() {
        for (PluginDataLoader.TABGroupInfo g : plugin.getDataLoader().getTABGroups()) {
            if (g.name.equals(groupName)) return g;
        }
        return null;
    }
}

/**
 * Prefixes GUI - Choose WHERE the rank shows (tab, head, below name)
 */
class TABPrefixesGUI extends GUIFramework {
    private final String groupName;

    public TABPrefixesGUI(Player player, String groupName) {
        super(player, "§8§lTAB §7— §dPrefixes", 45);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        PluginDataLoader.TABGroupInfo g = loadGroup();

        setItem(4, new GUIItemBuilder(Material.JUNGLE_SIGN)
                .name("§d§lPrefixes §7— §b" + groupName)
                .lore("§7Toggle WHERE this rank displays")
                .glowing(true).build());

        // Tab List
        boolean tabEnabled = g != null && g.tabprefix != null && !g.tabprefix.isEmpty();
        String tabVal = tabEnabled ? ChatColor.translateAlternateColorCodes('&', g.tabprefix) + "PlayerName" : "§7Not set";
        setItem(19, new GUIItemBuilder(tabEnabled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name("§a§lTab List")
                .lore("§7Shows in the player list (Tab key)", "", "§7Preview: " + tabVal,
                    "§7Status: " + (tabEnabled ? "§aON" : "§cOFF"), "", "§eClick to toggle/edit")
                .build(), e -> togglePrefix("tabprefix"));

        // Nametag (above head)
        boolean tagEnabled = g != null && g.tagprefix != null && !g.tagprefix.isEmpty();
        String tagVal = tagEnabled ? ChatColor.translateAlternateColorCodes('&', g.tagprefix) + "Name" : "§7Not set";
        setItem(21, new GUIItemBuilder(tagEnabled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name("§e§lNametag (Above Head)")
                .lore("§7Shows above the player's head", "", "§7Preview: " + tagVal,
                    "§7Status: " + (tagEnabled ? "§aON" : "§cOFF"), "", "§eClick to toggle/edit")
                .build(), e -> togglePrefix("tagprefix"));

        // Below Name
        boolean belowEnabled = g != null && g.belowname != null && !g.belowname.isEmpty();
        String belowVal = belowEnabled ? ChatColor.translateAlternateColorCodes('&', g.belowname) : "§7Not set";
        setItem(23, new GUIItemBuilder(belowEnabled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name("§c§lBelow Name")
                .lore("§7Shows below the player's name", "", "§7Value: " + belowVal,
                    "§7Status: " + (belowEnabled ? "§aON" : "§cOFF"), "", "§eClick to toggle/edit")
                .build(), e -> togglePrefix("belowname"));

        // Tab Suffix
        boolean sufEnabled = g != null && g.tabsuffix != null && !g.tabsuffix.isEmpty();
        String sufVal = sufEnabled ? ChatColor.translateAlternateColorCodes('&', g.tabsuffix) : "§7Not set";
        setItem(25, new GUIItemBuilder(sufEnabled ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name("§b§lTab Suffix")
                .lore("§7Shows after name in tab list", "", "§7Value: " + sufVal,
                    "§7Status: " + (sufEnabled ? "§aON" : "§cOFF"), "", "§eClick to toggle/edit")
                .build(), e -> togglePrefix("tabsuffix"));

        // Quick set: Apply tabprefix to all
        setItem(31, new GUIItemBuilder(Material.STRUCTURE_BLOCK)
                .name("§6§lCopy Tab → All")
                .lore("§7Copies the Tab prefix to", "§7nametag, below name, and suffix", "", "§eClick to apply")
                .build(), e -> {
                    if (g != null && g.tabprefix != null && !g.tabprefix.isEmpty()) {
                        g.tagprefix = g.tabprefix;
                        g.tagsuffix = g.tabsuffix != null ? g.tabsuffix : "";
                        g.belowname = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', g.tabprefix)).replaceAll("[^a-zA-Z]", "");
                        plugin.getDataLoader().saveTABGroup(g);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Copied to all positions!");
                    } else {
                        plugin.playSound(player, "error");
                        player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Set a Tab prefix first!");
                    }
                    refresh(); open();
                });

        addBackButton(36, () -> new TABGroupGUI(player, groupName).open());
        addCloseButton(44);
    }

    private void togglePrefix(String type) {
        PluginDataLoader.TABGroupInfo g = loadGroup();
        if (g == null) return;
        plugin.getChatInputManager().requestInput(player,
            "Enter " + type + " value (use & for colors, or 'off' to disable):",
            input -> {
                switch (type) {
                    case "tabprefix" -> g.tabprefix = input.equalsIgnoreCase("off") ? "" : input;
                    case "tagprefix" -> g.tagprefix = input.equalsIgnoreCase("off") ? "" : input;
                    case "tabsuffix" -> g.tabsuffix = input.equalsIgnoreCase("off") ? "" : input;
                    case "belowname" -> g.belowname = input.equalsIgnoreCase("off") ? "" : input;
                }
                plugin.getDataLoader().saveTABGroup(g);
                plugin.playSound(player, "success");
                refresh(); open();
            });
    }

    private PluginDataLoader.TABGroupInfo loadGroup() {
        for (PluginDataLoader.TABGroupInfo g : plugin.getDataLoader().getTABGroups()) {
            if (g.name.equals(groupName)) return g;
        }
        return null;
    }
}

/**
 * TAB Players GUI - Same 2-side layout as LuckPerms
 */
class TABPlayersGUI extends GUIFramework {
    private final String groupName;

    public TABPlayersGUI(Player player, String groupName) {
        super(player, "§8§lTAB §7— §aPlayers", 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.PLAYER_HEAD)
                .name("§a§lPlayers §7— §b" + groupName).glowing(true).build());

        // Column headers
        setItem(10, new GUIItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                .name("§a§l✓ IN GROUP").lore("§7Click to §cremove").build());
        setItem(13, new GUIItemBuilder(Material.BARRIER).name("§8§l│").build());
        setItem(16, new GUIItemBuilder(Material.RED_STAINED_GLASS_PANE)
                .name("§c§l✗ NOT IN GROUP").lore("§7Click to §aadd").build());

        // Get online players and split by group membership (uses LuckPerms API)
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
            if (groups.stream().anyMatch(gr -> gr.equalsIgnoreCase(groupName))) {
                inGroup.add(online);
            } else {
                notInGroup.add(online);
            }
        }

        // Left side: players IN group (slots 19-22, 28-31, 37-40) - same layout as LP
        int[] leftSlots = {19, 20, 21, 22, 28, 29, 30, 31, 37, 38, 39, 40};
        for (int i = 0; i < Math.min(inGroup.size(), leftSlots.length); i++) {
            Player p = inGroup.get(i);
            List<String> grps = playerGroups.getOrDefault(p.getUniqueId(), Collections.emptyList());
            String other = grps.stream().filter(gr -> !gr.equalsIgnoreCase(groupName)).collect(Collectors.joining("§7, §f"));

            setItem(leftSlots[i], new GUIItemBuilder(Material.LIME_STAINED_GLASS_PANE)
                    .name("§a§l" + p.getName())
                    .lore("§7Other: §f" + (other.isEmpty() ? "§7None" : other), "", "§cClick to remove")
                    .build(), event -> {
                        plugin.dispatchCommand("lp user " + p.getName() + " group remove " + groupName);
                        plugin.playSound(player, "success");
                        refresh(); open();
                    });
        }

        // Right side: players NOT in group (slots 24-27, 33-36, 42-45) - same layout as LP
        int[] rightSlots = {24, 25, 26, 27, 33, 34, 35, 36, 42, 43, 44, 45};
        for (int i = 0; i < Math.min(notInGroup.size(), rightSlots.length); i++) {
            Player p = notInGroup.get(i);
            List<String> grps = playerGroups.getOrDefault(p.getUniqueId(), Collections.emptyList());
            String groupList = String.join("§7, §f", grps);

            setItem(rightSlots[i], new GUIItemBuilder(Material.RED_STAINED_GLASS_PANE)
                    .name("§c§l" + p.getName())
                    .lore("§7Groups: §f" + (groupList.isEmpty() ? "§7None" : groupList), "", "§aClick to add")
                    .build(), event -> {
                        plugin.dispatchCommand("lp user " + p.getName() + " group add " + groupName);
                        plugin.playSound(player, "success");
                        refresh(); open();
                    });
        }

        // Separator column (column 4, rows 2-5)
        ItemStack sep = new GUIItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name("§8│").build();
        for (int r = 2; r <= 5; r++) setItem(r * 9 + 4, sep);

        // No players message
        if (inGroup.isEmpty()) setItem(19, new GUIItemBuilder(Material.STRUCTURE_VOID).name("§7No players in group").build());
        if (notInGroup.isEmpty()) setItem(24, new GUIItemBuilder(Material.STRUCTURE_VOID).name("§7All players have this group").build());

        addBackButton(48, () -> new TABGroupGUI(player, groupName).open());
        addCloseButton(50);
    }
}

/**
 * TEXT Templates GUI - Pre-made style templates, click to apply!
 * The creative one: hover to preview, click to auto-apply.
 */
class TABTextGUI extends GUIFramework {
    private final String groupName;

    public TABTextGUI(Player player, String groupName) {
        super(player, "§8§lTAB §7— §eTEXT", 54);
        this.groupName = groupName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.PAINTING)
                .name("§e§lTEXT Templates §7— §b" + groupName)
                .lore("§7Click a template to auto-apply!", "§7Hover to see preview")
                .glowing(true).build());

        // Template definitions: {displayName, material, tabprefix, tagprefix, belowname, tabsuffix}
        Object[][] templates = {
            // Red styles
            {"§4§l♛ Owner", Material.RED_WOOL, "&4&l♛ &4", "&4&l♛ &4", "&4Owner", ""},
            {"§c§l[Admin]", Material.RED_CONCRETE, "&c&l[Admin] &c", "&c&l[Admin] &c", "&cAdmin", ""},
            {"§c[Mod]", Material.RED_STAINED_GLASS_PANE, "&c[Mod] &c", "&c[Mod] &c", "&cMod", ""},

            // Gold styles
            {"§6§l[Admin]", Material.ORANGE_WOOL, "&6&l[Admin] &6", "&6&l[Admin] &6", "&6Admin", ""},
            {"§6[Mod]", Material.ORANGE_CONCRETE, "&6[Mod] &6", "&6[Mod] &6", "&6Mod", ""},
            {"§6★ VIP", Material.GOLD_BLOCK, "&6&l★ &6", "&6&l★ &6", "&6VIP", ""},

            // Green styles
            {"§a§l[VIP]", Material.LIME_WOOL, "&a&l[VIP] &a", "&a&l[VIP] &a", "&aVIP", ""},
            {"§a◆ Builder", Material.GREEN_CONCRETE, "&a&l◆ &a", "&a&l◆ &a", "&aBuilder", ""},
            {"§2[Helper]", Material.GREEN_WOOL, "&2[Helper] &2", "&2[Helper] &2", "&2Helper", ""},

            // Blue styles
            {"§9§l[Admin]", Material.BLUE_WOOL, "&9&l[Admin] &9", "&9&l[Admin] &9", "&9Admin", ""},
            {"§9[Dev]", Material.BLUE_CONCRETE, "&9[Dev] &9", "&9[Dev] &9", "&9Dev", ""},
            {"§b✦ Builder", Material.LIGHT_BLUE_WOOL, "&b&l✦ &b", "&b&l✦ &b", "&bBuilder", ""},

            // Purple styles
            {"§5§l[Owner]", Material.PURPLE_WOOL, "&5&l[Owner] &5", "&5&l[Owner] &5", "&5Owner", ""},
            {"§d♡ Mod", Material.PINK_WOOL, "&d&l♡ &d", "&d&l♡ &d", "&dMod", ""},
            {"§5[Dev]", Material.MAGENTA_WOOL, "&5[Dev] &5", "&5[Dev] &5", "&5Dev", ""},

            // Neutral styles
            {"§7[Member]", Material.LIGHT_GRAY_WOOL, "&7[Member] &f", "&7[Member] &f", "&7Member", ""},
            {"§f[Player]", Material.WHITE_WOOL, "&f[Player] &f", "&f[Player] &f", "&fPlayer", ""},
            {"§8[Default]", Material.GRAY_WOOL, "&8[Default] &7", "&8[Default] &7", "&8Default", ""},
        };

        // Show templates in slots 10-43 (4 rows of 7)
        int[] slots = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
        };

        for (int i = 0; i < templates.length && i < slots.length; i++) {
            Object[] t = templates[i];
            String displayName = (String) t[0];
            Material mat = (Material) t[1];
            String tabPre = (String) t[2];
            String tagPre = (String) t[3];
            String below = (String) t[4];
            String tabSuf = (String) t[5];

            // Build preview for lore
            String tabPreview = ChatColor.translateAlternateColorCodes('&', tabPre + "PlayerName" + tabSuf);
            String headPreview = ChatColor.translateAlternateColorCodes('&', tagPre + "PlayerName");
            String belowPreview = ChatColor.translateAlternateColorCodes('&', below);

            setItem(slots[i], new GUIItemBuilder(mat)
                    .name(displayName)
                    .lore(
                        "§7━━ Preview ━━",
                        "§7Tab: " + tabPreview,
                        "§7Head: " + headPreview,
                        "§7Below: " + belowPreview,
                        "",
                        "§aClick to apply this style!"
                    )
                    .build(), event -> applyTemplate(tabPre, tagPre, below, tabSuf));
        }

        // Custom template option
        setItem(47, new GUIItemBuilder(Material.PAPER)
                .name("§e§lCustom Template")
                .lore("§7Type your own prefix", "", "§eClick →")
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter custom tabprefix (use & for colors):",
                        input -> {
                            PluginDataLoader.TABGroupInfo g = loadOrCreateGroup();
                            g.tabprefix = input;
                            g.tagprefix = input;
                            g.tabsuffix = "";
                            g.tagsuffix = "";
                            plugin.getDataLoader().saveTABGroup(g);
                            plugin.playSound(player, "success");
                            refresh(); open();
                        });
                });

        // Clear all
        setItem(51, new GUIItemBuilder(Material.TNT)
                .name("§c§lClear All").lore("§7Remove all TAB formatting", "", "§eClick")
                .build(), e -> {
                    PluginDataLoader.TABGroupInfo g = loadOrCreateGroup();
                    g.tabprefix = ""; g.tagprefix = ""; g.tagsuffix = "";
                    g.tabsuffix = ""; g.belowname = ""; g.customtabname = "";
                    plugin.getDataLoader().saveTABGroup(g);
                    plugin.playSound(player, "success");
                    refresh(); open();
                });

        addBackButton(48, () -> new TABGroupGUI(player, groupName).open());
        addCloseButton(50);
    }

    private void applyTemplate(String tabprefix, String tagprefix, String belowname, String tabsuffix) {
        PluginDataLoader.TABGroupInfo g = loadOrCreateGroup();
        g.tabprefix = tabprefix;
        g.tagprefix = tagprefix;
        g.belowname = belowname;
        g.tabsuffix = tabsuffix;
        g.tagsuffix = "";

        if (plugin.getDataLoader().saveTABGroup(g)) {
            plugin.playSound(player, "success");
            String preview = ChatColor.translateAlternateColorCodes('&', tabprefix + "PlayerName");
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Template applied! Preview: " + preview);
        } else {
            plugin.playSound(player, "error");
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Failed to save. Check console.");
        }
        refresh(); open();
    }

    private PluginDataLoader.TABGroupInfo loadOrCreateGroup() {
        for (PluginDataLoader.TABGroupInfo g : plugin.getDataLoader().getTABGroups()) {
            if (g.name.equals(groupName)) return g;
        }
        PluginDataLoader.TABGroupInfo g = new PluginDataLoader.TABGroupInfo();
        g.name = groupName;
        g.tabprefix = ""; g.tabsuffix = ""; g.tagprefix = "";
        g.tagsuffix = ""; g.customtabname = ""; g.belowname = "";
        return g;
    }
}
