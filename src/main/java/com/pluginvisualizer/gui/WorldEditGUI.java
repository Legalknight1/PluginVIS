package com.pluginvisualizer.gui;

import com.pluginvisualizer.PluginVisualizer;
import com.pluginvisualizer.util.ChatInputManager;
import com.pluginvisualizer.util.PluginDataLoader;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * WorldEdit visual command GUI.
 * Provides visual buttons for common WorldEdit operations.
 * Text values (like coordinates, sizes) use chat input.
 */
public class WorldEditGUI extends GUIFramework {

    public WorldEditGUI(Player player) {
        super(player, "§8§lWorldEdit §7— Commands", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Title
        setItem(4, new GUIItemBuilder(Material.WOODEN_AXE)
                .name("§a§lWorldEdit")
                .lore("§7Visual command shortcuts")
                .glowing(true)
                .build());

        // ===== WORLD BORDER SECTION =====
        setItem(9, new GUIItemBuilder(Material.STRUCTURE_VOID)
                .name("§6§l━━ World Border ━━")
                .build());

        PluginDataLoader.WorldBorderInfo borderInfo = plugin.getDataLoader().getWorldBorderInfo(player.getWorld());

        // Border size
        setItem(10, new GUIItemBuilder(Material.BARRIER)
                .name("§e§lSet Border Size")
                .lore(
                    "§7Set the world border diameter",
                    "",
                    "§7Current size: §f" + String.format("%.0f", borderInfo.size),
                    "§7World: §f" + player.getWorld().getName(),
                    "",
                    "§eClick to set size"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter world border size (blocks, e.g., 1000):",
                    input -> {
                        try {
                            double size = Double.parseDouble(input);
                            plugin.dispatchCommand("worldborder set " + (int) size);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "World border set to §f" + (int) size + " §ablocks!");
                        } catch (NumberFormatException e) {
                            plugin.playSound(player, "error");
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Invalid number!");
                        }
                        refresh();
                        open();
                    },
                    input -> {
                        try { Double.parseDouble(input); return null; }
                        catch (NumberFormatException e) { return "Please enter a valid number!"; }
                    }
                );
            }
        );

        // Border center
        setItem(11, new GUIItemBuilder(Material.REDSTONE)
                .name("§e§lSet Border Center")
                .lore(
                    "§7Set center of world border",
                    "",
                    "§7Current: §f" + String.format("%.0f, %.0f", borderInfo.center.getX(), borderInfo.center.getZ()),
                    "",
                    "§7Or click §b'Use My Position' §7below",
                    "§eClick to set center manually"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter center coordinates (x z, e.g., 100 200):",
                    input -> {
                        String[] parts = input.split(" ");
                        if (parts.length == 2) {
                            try {
                                double x = Double.parseDouble(parts[0]);
                                double z = Double.parseDouble(parts[1]);
                                plugin.dispatchCommand("worldborder center " + x + " " + z);
                                plugin.playSound(player, "success");
                                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Border center set!");
                            } catch (NumberFormatException e) {
                                plugin.playSound(player, "error");
                                player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Invalid coordinates!");
                            }
                        } else {
                            plugin.playSound(player, "error");
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Format: <x> <z>");
                        }
                        refresh();
                        open();
                    }
                );
            }
        );

        // Use current position as center
        setItem(12, new GUIItemBuilder(Material.COMPASS)
                .name("§b§lCenter = My Position")
                .lore(
                    "§7Set border center to where",
                    "§7you are standing right now",
                    "",
                    "§7Your position: §f" + 
                        String.format("%.0f, %.0f", player.getLocation().getX(), player.getLocation().getZ()),
                    "",
                    "§eClick to set"
                )
                .build(),
            event -> {
                Location loc = player.getLocation();
                plugin.dispatchCommand("worldborder center " + loc.getBlockX() + " " + loc.getBlockZ());
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Border center set to your position!");
                refresh();
                open();
            }
        );

        // Warning distance
        setItem(13, new GUIItemBuilder(Material.YELLOW_WOOL)
                .name("§e§lWarning Distance")
                .lore(
                    "§7Show warning at this distance",
                    "§7from the border",
                    "",
                    "§7Current: §f" + borderInfo.warningDistance + " blocks",
                    "",
                    "§eClick to set"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter warning distance (blocks, e.g., 10):",
                    input -> {
                        try {
                            int dist = Integer.parseInt(input);
                            plugin.dispatchCommand("worldborder warning distance " + dist);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Warning distance set to §f" + dist);
                        } catch (NumberFormatException e) {
                            plugin.playSound(player, "error");
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Invalid number!");
                        }
                        refresh();
                        open();
                    }
                );
            }
        );

        // Damage amount
        setItem(14, new GUIItemBuilder(Material.RED_WOOL)
                .name("§c§lDamage Amount")
                .lore(
                    "§7Damage per second outside border",
                    "",
                    "§7Current: §f" + String.format("%.1f", borderInfo.damageAmount),
                    "",
                    "§eClick to set"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter damage amount per second (e.g., 2.0):",
                    input -> {
                        try {
                            double dmg = Double.parseDouble(input);
                            plugin.dispatchCommand("worldborder damage amount " + dmg);
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Border damage set to §f" + dmg);
                        } catch (NumberFormatException e) {
                            plugin.playSound(player, "error");
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Invalid number!");
                        }
                        refresh();
                        open();
                    }
                );
            }
        );

        // ===== SELECTION SECTION =====
        setItem(18, new GUIItemBuilder(Material.STRUCTURE_VOID)
                .name("§6§l━━ Selection ━━")
                .build());

        // Select wand
        setItem(19, new GUIItemBuilder(Material.WOODEN_AXE)
                .name("§a§lGet Wand")
                .lore("§7Give yourself the", "§7WorldEdit selection wand", "", "§eClick to get")
                .build(),
            event -> {
                plugin.dispatchCommand("wand");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Wand given!");
            }
        );

        // Select pos1
        setItem(20, new GUIItemBuilder(Material.OAK_PLANKS)
                .name("§6§lSelect Pos 1")
                .lore("§7Set position 1 to your", "§7current location", "", "§eClick to set")
                .build(),
            event -> {
                plugin.dispatchCommand("pos1");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Position 1 set!");
            }
        );

        // Select pos2
        setItem(21, new GUIItemBuilder(Material.SPRUCE_PLANKS)
                .name("§6§lSelect Pos 2")
                .lore("§7Set position 2 to your", "§7current location", "", "§eClick to set")
                .build(),
            event -> {
                plugin.dispatchCommand("pos2");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Position 2 set!");
            }
        );

        // Expand selection
        setItem(22, new GUIItemBuilder(Material.ARROW)
                .name("§b§lExpand Selection")
                .lore("§7Expand selection in a direction", "", "§eClick then enter distance")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter expand amount and direction (e.g., '10 up' or '5 north'):",
                    input -> {
                        plugin.dispatchCommand("expand " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Selection expanded!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Contract selection
        setItem(23, new GUIItemBuilder(Material.FIRE_CHARGE)
                .name("§b§lContract Selection")
                .lore("§7Contract selection inward", "", "§eClick then enter distance")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter contract amount and direction (e.g., '5 down'):",
                    input -> {
                        plugin.dispatchCommand("contract " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Selection contracted!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // ===== CLIPBOARD SECTION =====
        setItem(27, new GUIItemBuilder(Material.STRUCTURE_VOID)
                .name("§6§l━━ Clipboard ━━")
                .build());

        // Copy
        setItem(28, new GUIItemBuilder(Material.BOOK)
                .name("§a§lCopy")
                .lore("§7Copy selection to clipboard", "", "§eClick to copy")
                .build(),
            event -> {
                plugin.dispatchCommand("copy");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Selection copied!");
            }
        );

        // Paste
        setItem(29, new GUIItemBuilder(Material.WRITABLE_BOOK)
                .name("§a§lPaste")
                .lore("§7Paste clipboard contents", "", "§eClick to paste")
                .build(),
            event -> {
                plugin.dispatchCommand("paste");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Clipboard pasted!");
            }
        );

        // Rotate
        setItem(30, new GUIItemBuilder(Material.COMPASS)
                .name("§e§lRotate")
                .lore("§7Rotate clipboard (degrees)", "", "§eClick then enter angle")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter rotation angle (90, 180, 270):",
                    input -> {
                        plugin.dispatchCommand("rotate " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Rotated by " + input + "°!");
                        refresh();
                        open();
                    },
                    input -> {
                        try {
                            int angle = Integer.parseInt(input);
                            if (angle % 90 != 0) return "Angle must be multiple of 90!";
                            return null;
                        } catch (NumberFormatException e) {
                            return "Enter a valid number (90, 180, 270)!";
                        }
                    }
                );
            }
        );

        // Flip
        setItem(31, new GUIItemBuilder(Material.ENDER_PEARL)
                .name("§e§lFlip")
                .lore("§7Flip clipboard (up/down/left/right)", "", "§eClick then enter direction")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter flip direction (up, down, north, south, east, west):",
                    input -> {
                        plugin.dispatchCommand("flip " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Flipped " + input + "!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // ===== GENERATION SECTION =====
        setItem(36, new GUIItemBuilder(Material.STRUCTURE_VOID)
                .name("§6§l━━ Generation ━━")
                .build());

        // Sphere
        setItem(37, new GUIItemBuilder(Material.SNOWBALL)
                .name("§d§lSphere")
                .lore("§7Generate a filled sphere", "", "§eClick then enter radius")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter sphere radius and block (e.g., '5 stone' or '3 glass'):",
                    input -> {
                        plugin.dispatchCommand("sphere " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Sphere created!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Hollow sphere
        setItem(38, new GUIItemBuilder(Material.GLASS)
                .name("§d§lHollow Sphere")
                .lore("§7Generate a hollow sphere", "", "§eClick then enter radius")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter hollow sphere radius and block (e.g., '5 stone'):",
                    input -> {
                        plugin.dispatchCommand("hsphere " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Hollow sphere created!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Cylinder
        setItem(39, new GUIItemBuilder(Material.BUCKET)
                .name("§d§lCylinder")
                .lore("§7Generate a cylinder", "", "§eClick then enter params")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter cylinder params (radius height block, e.g., '5 10 stone'):",
                    input -> {
                        plugin.dispatchCommand("cyl " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Cylinder created!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // ===== UTILITY SECTION =====
        setItem(45, new GUIItemBuilder(Material.STRUCTURE_VOID)
                .name("§6§l━━ Utility ━━")
                .build());

        // Undo
        setItem(46, new GUIItemBuilder(Material.ARROW)
                .name("§c§lUndo")
                .lore("§7Undo last action", "", "§eClick to undo")
                .build(),
            event -> {
                plugin.dispatchCommand("undo");
                plugin.playSound(player, "click");
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Undo executed!");
            }
        );

        // Redo
        setItem(47, new GUIItemBuilder(Material.ARROW)
                .name("§a§lRedo")
                .lore("§7Redo last undone action", "", "§eClick to redo")
                .build(),
            event -> {
                plugin.dispatchCommand("redo");
                plugin.playSound(player, "click");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Redo executed!");
            }
        );

        // Replace
        setItem(48, new GUIItemBuilder(Material.CRAFTING_TABLE)
                .name("§e§lReplace")
                .lore("§7Replace blocks in selection", "", "§eClick then enter blocks")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter replace (from to, e.g., 'stone dirt' or 'air water'):",
                    input -> {
                        plugin.dispatchCommand("replace " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Replace executed!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Fill
        setItem(49, new GUIItemBuilder(Material.LAVA_BUCKET)
                .name("§6§lFill Selection")
                .lore("§7Fill selection with a block", "", "§eClick then enter block type")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter block type to fill with (e.g., 'stone', 'glass', 'air'):",
                    input -> {
                        plugin.dispatchCommand("set " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Selection filled with " + input + "!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Drain
        setItem(50, new GUIItemBuilder(Material.SPONGE)
                .name("§b§lDrain")
                .lore("§7Remove water/lava nearby", "", "§eClick then enter radius")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter drain radius (e.g., 10):",
                    input -> {
                        plugin.dispatchCommand("drain " + input);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Drained!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Navigation
        addBackButton(52, () -> new MainMenuGUI(player).open());
        addCloseButton(53);
    }
}
