package com.pluginvisualizer.gui;

import com.pluginvisualizer.PluginVisualizer;
import com.pluginvisualizer.util.ChatInputManager;
import com.pluginvisualizer.util.PluginDataLoader;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Multiverse-Core visual configuration GUI.
 * Auto-loads all existing worlds from the server.
 * Create new worlds with visual type selection, seed input, etc.
 */
public class MultiverseGUI extends GUIFramework {

    private int page = 0;
    private static final int WORLDS_PER_PAGE = 21;
    private static final int[] WORLD_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34
    };

    public MultiverseGUI(Player player) {
        super(player, "§8§lMultiverse §7— Worlds", 54);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Title
        setItem(4, new GUIItemBuilder(Material.COMPASS)
                .name("§d§lMultiverse Manager")
                .lore("§7Manage worlds visually")
                .glowing(true)
                .build());

        // Action buttons - row 1
        setItem(1, new GUIItemBuilder(Material.EMERALD)
                .name("§a§l+ Create World")
                .lore("§7Create a new world with", "§7visual setup wizard", "", "§eClick to create §a→")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                new MultiverseCreateGUI(player).open();
            }
        );

        setItem(2, new GUIItemBuilder(Material.RED_DYE)
                .name("§c§l- Delete World")
                .lore("§7Remove a world from Multiverse", "§c⚠ Cannot be undone!", "", "§eClick to delete")
                .build(),
            event -> deleteWorld()
        );

        setItem(3, new GUIItemBuilder(Material.COMPARATOR)
                .name("§7§lImport World")
                .lore("§7Import an existing world", "§7folder into Multiverse", "", "§eClick to import")
                .build(),
            event -> importWorld()
        );

        setItem(5, new GUIItemBuilder(Material.ENDER_PEARL)
                .name("§b§lTeleport to World")
                .lore("§7Teleport to a world's spawn", "", "§eClick then enter world name")
                .build(),
            event -> teleportToWorld()
        );

        setItem(6, new GUIItemBuilder(Material.COMPARATOR)
                .name("§7§lRefresh")
                .lore("§7Reload world list", "", "§eClick to refresh")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                refresh();
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "World list refreshed!");
            }
        );

        // Load existing worlds
        List<PluginDataLoader.MVWorldInfo> worlds = plugin.getDataLoader().getMultiverseWorlds();
        
        int start = page * WORLDS_PER_PAGE;
        int end = Math.min(start + WORLDS_PER_PAGE, worlds.size());

        for (int i = start; i < end; i++) {
            int slotIndex = i - start;
            if (slotIndex >= WORLD_SLOTS.length) break;
            
            PluginDataLoader.MVWorldInfo world = worlds.get(i);
            
            Material worldMat = getWorldMaterial(world.environment);
            
            String aliasDisplay = world.alias != null && !world.alias.isEmpty() ? world.alias : "§7None";
            String genDisplay = world.generator != null && !world.generator.equals("default") ? world.generator : "§7Default";
            
            setItem(WORLD_SLOTS[slotIndex], new GUIItemBuilder(worldMat)
                    .name("§d§l" + world.name)
                    .lore(
                        "§8World: §f" + world.name,
                        "",
                        "§7Environment: §f" + formatEnvironment(world.environment),
                        "§7Alias: §f" + aliasDisplay,
                        "§7Seed: §f" + world.seed,
                        "§7Generator: §f" + genDisplay,
                        "§7Difficulty: §f" + world.difficulty,
                        "§7PvP: " + (world.pvp ? "§aEnabled" : "§cDisabled"),
                        "§7Players: §f" + world.playerCount,
                        "",
                        "§eClick to manage §a→"
                    )
                    .build(),
                event -> {
                    plugin.playSound(player, "click");
                    new MultiverseWorldGUI(player, world.name).open();
                }
            );
        }

        // Page navigation
        int totalPages = (int) Math.ceil((double) worlds.size() / WORLDS_PER_PAGE);
        if (page > 0) {
            setItem(45, new GUIItemBuilder(Material.ARROW)
                    .name("§c§l← Previous Page")
                    .build(),
                event -> { page--; refresh(); }
            );
        }
        if (end < worlds.size()) {
            setItem(53, new GUIItemBuilder(Material.ARROW)
                    .name("§a§lNext Page →")
                    .build(),
                event -> { page++; refresh(); }
            );
        }

        // Bottom
        addBackButton(48, () -> new MainMenuGUI(player).open());
        addCloseButton(50);
        
        setItem(52, new GUIItemBuilder(Material.BOOK)
                .name("§e§lInfo")
                .lore("§7Worlds loaded: §f" + worlds.size(), "§7Page: §f" + (page + 1) + "/" + Math.max(1, totalPages))
                .build()
        );
    }

    private Material getWorldMaterial(String environment) {
        return switch (environment.toUpperCase()) {
            case "NORMAL" -> Material.GRASS_BLOCK;
            case "NETHER" -> Material.NETHERRACK;
            case "THE_END" -> Material.END_STONE;
            default -> Material.STONE;
        };
    }

    private String formatEnvironment(String env) {
        return switch (env.toUpperCase()) {
            case "NORMAL" -> "§aOverworld";
            case "NETHER" -> "§cNether";
            case "THE_END" -> "§5The End";
            default -> "§f" + env;
        };
    }

    private void deleteWorld() {
        plugin.playSound(player, "click");
        plugin.getChatInputManager().requestInput(player,
            "Enter world name to delete (WARNING: permanent!):",
            input -> {
                plugin.dispatchCommand("mv delete " + input);
                plugin.dispatchCommand("mv confirm");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "World §f" + input + " §edeleted!");
                refresh();
                open();
            }
        );
    }

    private void importWorld() {
        plugin.playSound(player, "click");
        plugin.getChatInputManager().requestInput(player,
            "Enter world folder name to import:",
            input -> {
                plugin.dispatchCommand("mv import " + input + " normal");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "World §f" + input + " §aimported!");
                refresh();
                open();
            }
        );
    }

    private void teleportToWorld() {
        plugin.playSound(player, "click");
        plugin.getChatInputManager().requestInput(player,
            "Enter world name to teleport to:",
            input -> {
                plugin.dispatchCommand("mv tp " + input);
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Teleported to §f" + input + "§a!");
            }
        );
    }
}

/**
 * World creation wizard GUI - step by step world creation
 */
class MultiverseCreateGUI extends GUIFramework {

    private String worldName;
    private String environment = "NORMAL";
    private String worldType = "NORMAL";
    private String seed = "";
    private String generator = "";

    public MultiverseCreateGUI(Player player) {
        super(player, "§8§lMultiverse §7— Create World", 27);
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        // Header
        setItem(4, new GUIItemBuilder(Material.EMERALD)
                .name("§a§lCreate New World")
                .lore("§7Set each option then click Create!")
                .glowing(true)
                .build());

        // World Name - slot 10
        setItem(10, new GUIItemBuilder(Material.PAPER)
                .name("§e§lWorld Name")
                .lore(
                    "§7The folder name for this world",
                    "",
                    "§7Current: §f" + (worldName != null ? worldName : "§cNot set"),
                    "",
                    "§eClick to set name"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter world name (no spaces, e.g., survival, creative, skyblock):",
                    input -> {
                        worldName = input.replace(" ", "_");
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "World name set to §f" + worldName);
                        refresh();
                        open();
                    },
                    input -> {
                        if (input.isEmpty()) return "Name cannot be empty!";
                        if (input.length() > 30) return "Name too long!";
                        return null;
                    }
                );
            }
        );

        // Environment - slot 11
        Material envMat = switch (environment) {
            case "NORMAL" -> Material.GRASS_BLOCK;
            case "NETHER" -> Material.NETHERRACK;
            case "THE_END" -> Material.END_STONE;
            default -> Material.STONE;
        };
        String envName = switch (environment) {
            case "NORMAL" -> "§aOverworld";
            case "NETHER" -> "§cNether";
            case "THE_END" -> "§5The End";
            default -> environment;
        };
        
        setItem(11, new GUIItemBuilder(envMat)
                .name("§6§lEnvironment")
                .lore(
                    "§7World dimension type",
                    "",
                    "§7Current: " + envName,
                    "",
                    "§eClick to cycle"
                )
                .build(),
            event -> {
                environment = switch (environment) {
                    case "NORMAL" -> "NETHER";
                    case "NETHER" -> "THE_END";
                    case "THE_END" -> "NORMAL";
                    default -> "NORMAL";
                };
                plugin.playSound(player, "click");
                refresh();
                open();
            }
        );

        // World Type - slot 12
        Material typeMat = switch (worldType) {
            case "NORMAL" -> Material.OAK_LOG;
            case "FLAT" -> Material.SAND;
            case "LARGE_BIOMES" -> Material.JUNGLE_SAPLING;
            case "AMPLIFIED" -> Material.SPRUCE_SAPLING;
            default -> Material.DIRT;
        };
        String typeName = switch (worldType) {
            case "NORMAL" -> "§aNormal";
            case "FLAT" -> "§eFlat";
            case "LARGE_BIOMES" -> "§bLarge Biomes";
            case "AMPLIFIED" -> "§dAmplified";
            default -> worldType;
        };
        
        setItem(12, new GUIItemBuilder(typeMat)
                .name("§b§lWorld Type")
                .lore(
                    "§7Terrain generation type",
                    "",
                    "§7Current: " + typeName,
                    "",
                    "§eClick to cycle"
                )
                .build(),
            event -> {
                worldType = switch (worldType) {
                    case "NORMAL" -> "FLAT";
                    case "FLAT" -> "LARGE_BIOMES";
                    case "LARGE_BIOMES" -> "AMPLIFIED";
                    case "AMPLIFIED" -> "NORMAL";
                    default -> "NORMAL";
                };
                plugin.playSound(player, "click");
                refresh();
                open();
            }
        );

        // Seed - slot 13
        setItem(13, new GUIItemBuilder(Material.WHEAT_SEEDS)
                .name("§d§lSeed")
                .lore(
                    "§7World generation seed",
                    "§7Leave empty for random",
                    "",
                    "§7Current: §f" + (seed.isEmpty() ? "§7Random" : seed),
                    "",
                    "§eClick to set seed"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter seed (or type 'random' for random seed):",
                    input -> {
                        seed = input.equalsIgnoreCase("random") ? "" : input;
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Seed set!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Generator - slot 14
        setItem(14, new GUIItemBuilder(Material.FURNACE)
                .name("§c§lGenerator")
                .lore(
                    "§7Custom chunk generator",
                    "§7Leave empty for default",
                    "",
                    "§7Current: §f" + (generator.isEmpty() ? "§7Default" : generator),
                    "",
                    "§eClick to set generator"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter generator name (or 'default' for vanilla, e.g., TerrainControl, EpicWorldGenerator):",
                    input -> {
                        generator = input.equalsIgnoreCase("default") ? "" : input;
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Generator set!");
                        refresh();
                        open();
                    }
                );
            }
        );

        // Create button - slot 16
        boolean canCreate = worldName != null && !worldName.isEmpty();
        setItem(16, new GUIItemBuilder(canCreate ? Material.LIME_WOOL : Material.GRAY_WOOL)
                .name(canCreate ? "§a§l✓ Create World!" : "§7§l✗ Set a name first")
                .lore(
                    canCreate ? "§7Ready to create!" : "§7You need to set a world name",
                    "",
                    canCreate ? "§7Name: §f" + worldName : "§7Name: §cNot set",
                    "§7Environment: " + envName,
                    "§7Type: " + typeName,
                    "§7Seed: §f" + (seed.isEmpty() ? "Random" : seed),
                    "§7Generator: §f" + (generator.isEmpty() ? "Default" : generator),
                    "",
                    canCreate ? "§a§lClick to create!" : "§8Set a name to continue"
                )
                .glowing(canCreate)
                .build(),
            canCreate ? event -> {
                String cmd = "mv create " + worldName + " " + environment.toLowerCase();
                if (!worldType.equals("NORMAL")) cmd += " -t " + worldType.toLowerCase();
                if (!seed.isEmpty()) cmd += " -s " + seed;
                if (!generator.isEmpty()) cmd += " -g " + generator;
                
                plugin.dispatchCommand(cmd);
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Creating world §f" + worldName + "§a...");
                player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Command: §f/" + cmd);
                new MultiverseGUI(player).open();
            } : null
        );

        // Back
        addBackButton(18, () -> new MultiverseGUI(player).open());
        addCloseButton(26);
    }
}

/**
 * Individual world detail GUI - manage properties of a specific world
 */
class MultiverseWorldGUI extends GUIFramework {

    private final String worldName;

    public MultiverseWorldGUI(Player player, String worldName) {
        super(player, "§8§lMultiverse §7— §d" + worldName, 36);
        this.worldName = worldName;
        build();
    }

    @Override
    protected void build() {
        fillBorder();

        PluginDataLoader loader = plugin.getDataLoader();
        PluginDataLoader.MVWorldInfo worldInfo = null;
        for (PluginDataLoader.MVWorldInfo w : loader.getMultiverseWorlds()) {
            if (w.name.equals(worldName)) {
                worldInfo = w;
                break;
            }
        }

        if (worldInfo == null) {
            setItem(13, new GUIItemBuilder(Material.RED_WOOL)
                    .name("§cWorld not found!")
                    .lore("§7This world may have been deleted")
                    .build());
            addBackButton(27, () -> new MultiverseGUI(player).open());
            return;
        }

        // Capture as final for lambda usage
        final PluginDataLoader.MVWorldInfo wi = worldInfo;
        final boolean pvpEnabled = wi.pvp;
        final boolean animalsEnabled = wi.animals;
        final boolean monstersEnabled = wi.monsters;

        // Header
        Material envMat = switch (worldInfo.environment.toUpperCase()) {
            case "NORMAL" -> Material.GRASS_BLOCK;
            case "NETHER" -> Material.NETHERRACK;
            case "THE_END" -> Material.END_STONE;
            default -> Material.STONE;
        };
        
        setItem(4, new GUIItemBuilder(envMat)
                .name("§d§l" + worldInfo.name)
                .lore(
                    "§7Environment: §f" + worldInfo.environment,
                    "§7Seed: §f" + worldInfo.seed,
                    "§7Difficulty: §f" + worldInfo.difficulty,
                    "§7Players: §f" + worldInfo.playerCount
                )
                .glowing(true)
                .build());

        // Teleport - slot 10
        setItem(10, new GUIItemBuilder(Material.ENDER_PEARL)
                .name("§b§lTeleport")
                .lore("§7Teleport to this world's spawn", "", "§eClick to teleport")
                .build(),
            event -> {
                plugin.dispatchCommand("mv tp " + worldName);
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Teleported to §f" + worldName + "§a!");
            }
        );

        // Set spawn - slot 11
        setItem(11, new GUIItemBuilder(Material.RED_BED)
                .name("§a§lSet Spawn")
                .lore("§7Set this world's spawn point", "§7to your current location", "", "§eClick to set")
                .build(),
            event -> {
                plugin.dispatchCommand("mv set spawn");
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Spawn set to your location!");
            }
        );

        // Toggle PvP - slot 12
        setItem(12, new GUIItemBuilder(pvpEnabled ? Material.IRON_SWORD : Material.WOODEN_SWORD)
                .name("§e§lToggle PvP")
                .lore(
                    "§7Current: " + (pvpEnabled ? "§aEnabled" : "§cDisabled"),
                    "",
                    "§eClick to toggle"
                )
                .build(),
            event -> {
                plugin.dispatchCommand("mv modify set pvp " + !pvpEnabled + " " + worldName);
                plugin.playSound(player, "success");
                player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "PvP " + (!pvpEnabled ? "enabled" : "disabled") + "!");
                refresh();
                open();
            }
        );

        // Set difficulty - slot 13
        setItem(13, new GUIItemBuilder(Material.WITHER_SKELETON_SKULL)
                .name("§c§lSet Difficulty")
                .lore(
                    "§7Current: §f" + worldInfo.difficulty,
                    "",
                    "§eClick to change"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter difficulty (peaceful, easy, normal, hard):",
                    input -> {
                        plugin.dispatchCommand("mv modify set difficulty " + input + " " + worldName);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Difficulty set to §f" + input);
                        refresh();
                        open();
                    },
                    input -> {
                        if (!List.of("peaceful", "easy", "normal", "hard").contains(input.toLowerCase())) {
                            return "Must be: peaceful, easy, normal, or hard!";
                        }
                        return null;
                    }
                );
            }
        );

        // Set alias - slot 14
        String aliasDisplay = worldInfo.alias != null && !worldInfo.alias.isEmpty() ? worldInfo.alias : "§7None";
        setItem(14, new GUIItemBuilder(Material.NAME_TAG)
                .name("§e§lSet Alias")
                .lore(
                    "§7Display name for this world",
                    "",
                    "§7Current: §f" + aliasDisplay,
                    "",
                    "§eClick to set"
                )
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "Enter alias for " + worldName + ":",
                    input -> {
                        plugin.dispatchCommand("mv modify set alias \"" + input + "\" " + worldName);
                        plugin.playSound(player, "success");
                        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Alias set to §f" + input);
                        refresh();
                        open();
                    }
                );
            }
        );

        // Toggle animals - slot 15
        setItem(15, new GUIItemBuilder(animalsEnabled ? Material.PORKCHOP : Material.COOKED_PORKCHOP)
                .name("§a§lToggle Animals")
                .lore(
                    "§7Current: " + (animalsEnabled ? "§aEnabled" : "§cDisabled"),
                    "",
                    "§eClick to toggle"
                )
                .build(),
            event -> {
                plugin.dispatchCommand("mv modify set animals " + !animalsEnabled + " " + worldName);
                plugin.playSound(player, "success");
                refresh();
                open();
            }
        );

        // Toggle monsters - slot 16
        setItem(16, new GUIItemBuilder(monstersEnabled ? Material.ZOMBIE_HEAD : Material.SKELETON_SKULL)
                .name("§c§lToggle Monsters")
                .lore(
                    "§7Current: " + (monstersEnabled ? "§aEnabled" : "§cDisabled"),
                    "",
                    "§eClick to toggle"
                )
                .build(),
            event -> {
                plugin.dispatchCommand("mv modify set monsters " + !monstersEnabled + " " + worldName);
                plugin.playSound(player, "success");
                refresh();
                open();
            }
        );

        // Delete world - slot 22
        setItem(22, new GUIItemBuilder(Material.TNT)
                .name("§4§lDelete World")
                .lore("§7Permanently delete this world", "§c⚠ This cannot be undone!", "", "§cClick to delete")
                .build(),
            event -> {
                plugin.playSound(player, "click");
                plugin.getChatInputManager().requestInput(player,
                    "§cType 'DELETE' to confirm deletion of " + worldName + ":",
                    input -> {
                        if (input.equals("DELETE")) {
                            plugin.dispatchCommand("mv delete " + worldName);
                            plugin.dispatchCommand("mv confirm");
                            plugin.playSound(player, "success");
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "World §f" + worldName + " §cdeleted!");
                            new MultiverseGUI(player).open();
                        } else {
                            plugin.playSound(player, "error");
                            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Deletion cancelled.");
                            refresh();
                            open();
                        }
                    }
                );
            }
        );

        // Navigation
        addBackButton(27, () -> new MultiverseGUI(player).open());
        addCloseButton(35);
    }
}
