package com.pluginvisualizer.gui;

import com.pluginvisualizer.PluginVisualizer;
import com.pluginvisualizer.util.ChatInputManager;
import com.pluginvisualizer.util.PluginDataLoader;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

/**
 * EssentialsX Chat GUI - Simple chat format configuration.
 * Focus: How names/prefixes look IN CHAT.
 * Extras (nicks, etc.) on the side walls.
 */
public class EssentialsXChatGUI extends GUIFramework {

    public EssentialsXChatGUI(Player player) {
        super(player, "§8§lChat §7— Format", 45);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Title
        setItem(4, new GUIItemBuilder(Material.OAK_SIGN)
                .name("§e§lEssentialsX Chat")
                .lore("§7Configure how chat looks", "§7Prefixes, formats & nicks")
                .glowing(true).build());

        // ===== MAIN SECTION: Chat Format =====
        PluginDataLoader.ChatConfigInfo chatInfo = plugin.getDataLoader().getEssentialsChatConfig();

        // Default Format
        String defaultFormat = chatInfo.defaultFormat != null ? chatInfo.defaultFormat : "§7Not set";
        setItem(10, new GUIItemBuilder(Material.PAPER)
                .name("§b§lChat Format")
                .lore(
                    "§7The default chat format",
                    "§7Placeholders: {PREFIX}, {DISPLAYNAME},",
                    "§7  {SUFFIX}, {MESSAGE}, {GROUP}",
                    "",
                    "§7Current: §f" + defaultFormat,
                    "",
                    "§eClick to edit"
                )
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter chat format (use {PREFIX} {DISPLAYNAME} {SUFFIX}: {MESSAGE}):",
                        input -> {
                            plugin.getDataLoader().saveEssentialsChatFormat(null, input);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chat format updated!");
                            refresh(); open();
                        });
                });

        // Group Formats list
        setItem(12, new GUIItemBuilder(Material.WRITABLE_BOOK)
                .name("§a§lGroup Formats")
                .lore(
                    "§7Per-group chat format overrides",
                    "§7Like: Owner sees red, Admin sees gold",
                    "",
                    "§7Groups: §f" + chatInfo.groupFormats.size(),
                    "",
                    "§eClick to manage →"
                )
                .build(), e -> {
                    plugin.playSound(player, "click");
                    new ChatGroupFormatsGUI(player).open();
                });

        // ===== QUICK FORMAT PRESETS =====
        setItem(14, new GUIItemBuilder(Material.PAINTING)
                .name("§d§lQuick Presets")
                .lore("§7Pre-made chat formats", "§7Click to auto-apply", "", "§eClick →")
                .build(), e -> {
                    plugin.playSound(player, "click");
                    new ChatPresetsGUI(player).open();
                });

        // ===== SIDE WALL: Extras (Nicks, etc.) =====
        setItem(28, new GUIItemBuilder(Material.NAME_TAG)
                .name("§6§lSet Nick")
                .lore("§7Set your display nickname", "§7Overrides your name in chat", "", "§eClick to set")
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter nickname (or 'off' to remove):",
                        input -> {
                            if (input.equalsIgnoreCase("off")) {
                                plugin.dispatchCommand("nick off");
                                plugin.playSound(player, "success");
                                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Nickname removed!");
                            } else {
                                plugin.dispatchCommand("nick " + player.getName() + " " + input);
                                plugin.playSound(player, "success");
                                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Nickname set to §f" + input);
                            }
                            refresh(); open();
                        });
                });

        setItem(30, new GUIItemBuilder(Material.PLAYER_HEAD)
                .name("§b§lSet Player Nick")
                .lore("§7Set another player's nickname", "", "§eClick to set")
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter player name:",
                        name -> {
                            plugin.getChatInputManager().requestInput(player,
                                "Enter nickname for " + name + ":",
                                nick -> {
                                    plugin.dispatchCommand("nick " + name + " " + nick);
                                    plugin.playSound(player, "success");
                                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Set §f" + name + "§a's nick to §f" + nick);
                                    refresh(); open();
                                });
                        });
                });

        setItem(32, new GUIItemBuilder(Material.BARRIER)
                .name("§c§lRemove Nick")
                .lore("§7Remove your nickname", "", "§eClick")
                .build(), e -> {
                    plugin.dispatchCommand("nick " + player.getName() + " off");
                    plugin.playSound(player, "success");
                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Nickname removed!");
                    refresh(); open();
                });

        // ===== PREVIEW =====
        String preview = buildPreview(chatInfo.defaultFormat);
        setItem(16, new GUIItemBuilder(Material.GREEN_STAINED_GLASS_PANE)
                .name("§a§lChat Preview")
                .lore("§7How chat will look:", "", preview)
                .build());

        // Navigation
        addBackButton(36, () -> new MainMenuGUI(player).open());
        addCloseButton(44);
    }

    private String buildPreview(String format) {
        if (format == null || format.isEmpty()) {
            return "§7[Owner] §fPlayerName§7: §fHello!";
        }
        String preview = ChatColor.translateAlternateColorCodes('&', format
            .replace("{PREFIX}", "§6[Owner] ")
            .replace("{DISPLAYNAME}", "§fPlayerName")
            .replace("{SUFFIX}", "")
            .replace("{GROUP}", "Owner")
            .replace("{MESSAGE}", "Hello!"));
        return preview;
    }
}

/**
 * Group Formats GUI - Manage per-group chat format overrides
 */
class ChatGroupFormatsGUI extends GUIFramework {
    private int page = 0;
    private static final int[] SLOTS = {
        10,11,12,13,14,15,16,
        19,20,21,22,23,24,25,
        28,29,30,31,32,33,34
    };

    public ChatGroupFormatsGUI(Player player) {
        super(player, "§8§lChat §7— Group Formats", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.WRITABLE_BOOK)
                .name("§a§lGroup Formats")
                .lore("§7Per-group chat format overrides")
                .glowing(true).build());

        // Add new group format
        setItem(1, new GUIItemBuilder(Material.EMERALD)
                .name("§a§l+ Add Group Format")
                .lore("§7Add a chat format for a group", "", "§eClick →")
                .build(), e -> {
                    plugin.getChatInputManager().requestInput(player,
                        "Enter group name for this format:",
                        input -> {
                            plugin.getChatInputManager().requestInput(player,
                                "Enter chat format for " + input + ":",
                                format -> {
                                    plugin.getDataLoader().saveEssentialsChatFormat(input, format);
                                    plugin.playSound(player, "success");
                                    player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Format set for §f" + input);
                                    refresh(); open();
                                });
                        });
                });

        // List existing group formats
        PluginDataLoader.ChatConfigInfo chatInfo = plugin.getDataLoader().getEssentialsChatConfig();
        Map<String, String> formats = chatInfo.groupFormats;

        int i = 0;
        for (Map.Entry<String, String> entry : formats.entrySet()) {
            if (i >= SLOTS.length) break;
            String group = entry.getKey();
            String format = entry.getValue();
            String preview = ChatColor.translateAlternateColorCodes('&', format
                .replace("{PREFIX}", "§6[" + group + "] ")
                .replace("{DISPLAYNAME}", "§fPlayer")
                .replace("{SUFFIX}", "")
                .replace("{GROUP}", group)
                .replace("{MESSAGE}", "Hello!"));

            setItem(SLOTS[i], new GUIItemBuilder(Material.PAPER)
                    .name("§b§l" + group)
                    .lore("§7Format: §f" + (format.length() > 30 ? format.substring(0, 30) + "..." : format),
                        "§7Preview: " + preview,
                        "",
                        "§eClick to edit", "§cShift+Click to remove")
                    .build(), event -> {
                        if (event.isShiftClick()) {
                            plugin.getDataLoader().removeEssentialsChatFormat(group);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Removed format for §f" + group);
                            refresh(); open();
                        } else {
                            plugin.getChatInputManager().requestInput(player,
                                "Enter new format for " + group + ":",
                                input -> {
                                    plugin.getDataLoader().saveEssentialsChatFormat(group, input);
                                    plugin.playSound(player, "success");
                                    refresh(); open();
                                });
                        }
                    });
            i++;
        }

        if (formats.isEmpty()) {
            setItem(22, new GUIItemBuilder(Material.STRUCTURE_VOID)
                    .name("§7No group formats set")
                    .lore("§7Click §a+ Add Group Format §7to create one")
                    .build());
        }

        addBackButton(48, () -> new EssentialsXChatGUI(player).open());
        addCloseButton(50);
    }
}

/**
 * Quick Presets GUI - Pre-made chat format templates
 */
class ChatPresetsGUI extends GUIFramework {

    public ChatPresetsGUI(Player player) {
        super(player, "§8§lChat §7— Presets", 45);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        setItem(4, new GUIItemBuilder(Material.PAINTING)
                .name("§d§lChat Presets")
                .lore("§7Click to apply a format")
                .glowing(true).build());

        // Preset definitions: {displayName, format, material}
        Object[][] presets = {
            {"§6§lRanked", "&7[{GROUP}] &r{PREFIX}{DISPLAYNAME}{SUFFIX}&7:&r {MESSAGE}", Material.GOLDEN_HELMET},
            {"§c§lColored Groups", "&7[&r{PREFIX}&7] &r{DISPLAYNAME}&7:&r {MESSAGE}", Material.RED_WOOL},
            {"§a§lClean", "{PREFIX}{DISPLAYNAME}&7:&r {MESSAGE}", Material.LIME_WOOL},
            {"§b§lMinimal", "{DISPLAYNAME}&7:&r {MESSAGE}", Material.WHITE_WOOL},
            {"§d§lFancy", "&7« &r{PREFIX}{DISPLAYNAME}&7 » &r{MESSAGE}", Material.PURPLE_WOOL},
            {"§e§lBracketed", "&7[&r{PREFIX}&7] &r{DISPLAYNAME}&7:&r {MESSAGE}", Material.YELLOW_WOOL},
            {"§9§lStaff", "&8[&b{GROUP}&8] &r{PREFIX}{DISPLAYNAME}&8:&r {MESSAGE}", Material.LAPIS_BLOCK},
            {"§7§lSimple", "{DISPLAYNAME}: {MESSAGE}", Material.GRAY_WOOL},
        };

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19};

        for (int i = 0; i < presets.length && i < slots.length; i++) {
            Object[] p = presets[i];
            String name = (String) p[0];
            String format = (String) p[1];
            Material mat = (Material) p[2];

            String preview = ChatColor.translateAlternateColorCodes('&', format
                .replace("{PREFIX}", "§6[Owner] ")
                .replace("{DISPLAYNAME}", "§fPlayerName")
                .replace("{SUFFIX}", "")
                .replace("{GROUP}", "Owner")
                .replace("{MESSAGE}", "Hello!"));

            setItem(slots[i], new GUIItemBuilder(mat)
                    .name(name)
                    .lore("§7Preview:", preview, "", "§eClick to apply as default")
                    .build(), e -> {
                        plugin.getDataLoader().saveEssentialsChatFormat(null, format);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Chat preset applied!");
                        refresh(); open();
                    });
        }

        addBackButton(36, () -> new EssentialsXChatGUI(player).open());
        addCloseButton(44);
    }
}
