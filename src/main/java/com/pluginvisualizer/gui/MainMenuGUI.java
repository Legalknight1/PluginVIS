package com.pluginvisualizer.gui;

import com.pluginvisualizer.PluginVisualizer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Main menu GUI - Shows all supported plugins as clickable items.
 * Installed plugins are shown in full color, uninstalled ones are grayed out.
 */
public class MainMenuGUI extends GUIFramework {

    public MainMenuGUI(Player player) {
        super(player, "§8§lPluginVisualizer §7— Select Plugin", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Title decoration
        setItem(4, new GUIItemBuilder(Material.NETHER_STAR)
                .name("§9§lPluginVisualizer")
                .lore("§7Click a plugin to configure it", "§7visually using inventory menus!")
                .glowing(true)
                .build());

        // ===== 3-in-1 Rank Setup (PROMINENT) =====
        setItem(13, new GUIItemBuilder(Material.NETHER_STAR)
                .name("§d§l3-in-1 Rank Setup")
                .lore(
                    "§7The EASY way to create ranks",
                    "§7Works across LuckPerms, TAB & Chat",
                    "",
                    "§a1.§f Create name, color & style",
                    "§b2.§f Set permissions by plugin",
                    "§e3.§f Toggle where it shows",
                    "",
                    "§d§lClick to start §d→"
                )
                .glowing(true)
                .build(), event -> {
                    plugin.playSound(player, "click");
                    new RankSetupGUI(player).open();
                });

        // ===== Plugin slots (rows 2-3) =====
        Map<String, Boolean> status = plugin.getPluginStatus();

        // LuckPerms - slot 19
        boolean lpInstalled = status.getOrDefault("LuckPerms", false);
        setItem(19, new GUIItemBuilder(lpInstalled ? Material.GOLDEN_HELMET : Material.LEATHER_HELMET)
                .name(lpInstalled ? "§6§lLuckPerms" : "§7§lLuckPerms §c§l(Not Installed)")
                .lore(
                    lpInstalled ? "§a✓ Installed" : "§c✗ Not Installed",
                    "",
                    "§7• Create & manage ranks",
                    "§7• Set permissions per group",
                    "§7• Configure prefixes & suffixes",
                    "§7• Manage parent groups",
                    "",
                    lpInstalled ? "§eClick to configure §a→" : "§8Install LuckPerms to use"
                )
                .glowing(lpInstalled)
                .build(),
            lpInstalled ? event -> {
                plugin.playSound(player, "click");
                new LuckPermsGUI(player).open();
            } : null
        );

        // TAB - slot 21
        boolean tabInstalled = status.getOrDefault("TAB", false);
        setItem(21, new GUIItemBuilder(tabInstalled ? Material.NAME_TAG : Material.PAPER)
                .name(tabInstalled ? "§b§lTAB" : "§7§lTAB §c§l(Not Installed)")
                .lore(
                    tabInstalled ? "§a✓ Installed" : "§c✗ Not Installed",
                    "",
                    "§7• Configure tab list groups",
                    "§7• Set tab prefixes & suffixes",
                    "§7• Configure nametag display",
                    "§7• Set belowname text",
                    "",
                    tabInstalled ? "§eClick to configure §a→" : "§8Install TAB to use"
                )
                .glowing(tabInstalled)
                .build(),
            tabInstalled ? event -> {
                plugin.playSound(player, "click");
                new TABGUI(player).open();
            } : null
        );

        // EssentialsX Chat - slot 23
        boolean essInstalled = status.getOrDefault("Essentials", false);
        setItem(23, new GUIItemBuilder(essInstalled ? Material.OAK_SIGN : Material.OAK_SIGN)
                .name(essInstalled ? "§e§lEssentialsX Chat" : "§7§lEssentialsX Chat §c§l(Not Installed)")
                .lore(
                    essInstalled ? "§a✓ Installed" : "§c✗ Not Installed",
                    "",
                    "§7• Configure chat format",
                    "§7• Set group-specific formats",
                    "§7• Manage nicknames",
                    "§7• Chat color settings",
                    "",
                    essInstalled ? "§eClick to configure §a→" : "§8Install EssentialsX to use"
                )
                .glowing(essInstalled)
                .build(),
            essInstalled ? event -> {
                plugin.playSound(player, "click");
                new EssentialsXChatGUI(player).open();
            } : null
        );

        // WorldEdit - slot 25
        boolean weInstalled = status.getOrDefault("WorldEdit", false);
        setItem(25, new GUIItemBuilder(weInstalled ? Material.WOODEN_AXE : Material.STICK)
                .name(weInstalled ? "§a§lWorldEdit" : "§7§lWorldEdit §c§l(Not Installed)")
                .lore(
                    weInstalled ? "§a✓ Installed" : "§c✗ Not Installed",
                    "",
                    "§7• World border controls",
                    "§7• Visual selection commands",
                    "§7• Quick utility actions",
                    "§7• Clipboard operations",
                    "",
                    weInstalled ? "§eClick to configure §a→" : "§8Install WorldEdit to use"
                )
                .glowing(weInstalled)
                .build(),
            weInstalled ? event -> {
                plugin.playSound(player, "click");
                new WorldEditGUI(player).open();
            } : null
        );

        // Multiverse-Core - slot 28
        boolean mvInstalled = status.getOrDefault("Multiverse-Core", false);
        setItem(28, new GUIItemBuilder(mvInstalled ? Material.COMPASS : Material.COMPASS)
                .name(mvInstalled ? "§d§lMultiverse-Core" : "§7§lMultiverse-Core §c§l(Not Installed)")
                .lore(
                    mvInstalled ? "§a✓ Installed" : "§c✗ Not Installed",
                    "",
                    "§7• Create & delete worlds",
                    "§7• Set world environments",
                    "§7• Configure world seeds",
                    "§7• Teleport between worlds",
                    "",
                    mvInstalled ? "§eClick to configure §a→" : "§8Install Multiverse-Core to use"
                )
                .glowing(mvInstalled)
                .build(),
            mvInstalled ? event -> {
                plugin.playSound(player, "click");
                new MultiverseGUI(player).open();
            } : null
        );

        // Separator between plugins and bottom row
        ItemStack sep = new GUIItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 36; i < 45; i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                setItem(i, sep);
            }
        }

        // Close button
        addCloseButton(49);

        // Info
        addInfoButton(53,
            "§7PluginVisualizer v2.1",
            "§7Configure plugins visually",
            "§7No commands needed - just click!",
            "",
            "§7Supported: §6LuckPerms§7, §bTAB§7,",
            "§7§eEssentialsX§7, §aWorldEdit§7, §dMultiverse"
        );
    }
}
