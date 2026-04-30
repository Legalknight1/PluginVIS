package com.pluginvisualizer.util;

import com.pluginvisualizer.PluginVisualizer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Loads existing data from supported plugins on the server.
 * Used to auto-populate GUIs with current server state.
 */
public class PluginDataLoader {

    private final PluginVisualizer plugin;

    public PluginDataLoader(PluginVisualizer plugin) {
        this.plugin = plugin;
    }

    // ========== LUCKPERMS ==========

    /**
     * Get all LuckPerms groups using the LuckPerms API
     */
    public List<GroupInfo> getLuckPermsGroups() {
        List<GroupInfo> groups = new ArrayList<>();
        
        try {
            net.luckperms.api.LuckPerms api = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);
            if (api == null) return groups;

            for (net.luckperms.api.model.group.Group group : api.getGroupManager().getLoadedGroups()) {
                GroupInfo info = new GroupInfo();
                info.name = group.getName();
                info.weight = group.getWeight().orElse(0);
                
                // Get display name
                try {
                    info.displayName = group.getDisplayName();
                } catch (Exception e) {
                    info.displayName = null;
                }
                
                // Get prefix - use NodeType filter
                Collection<net.luckperms.api.node.types.PrefixNode> prefixNodes = 
                    group.getNodes(net.luckperms.api.node.NodeType.PREFIX);
                if (!prefixNodes.isEmpty()) {
                    net.luckperms.api.node.types.PrefixNode prefixNode = prefixNodes.iterator().next();
                    info.prefix = prefixNode.getMetaValue();
                }
                
                // Get suffix
                Collection<net.luckperms.api.node.types.SuffixNode> suffixNodes = 
                    group.getNodes(net.luckperms.api.node.NodeType.SUFFIX);
                if (!suffixNodes.isEmpty()) {
                    net.luckperms.api.node.types.SuffixNode suffixNode = suffixNodes.iterator().next();
                    info.suffix = suffixNode.getMetaValue();
                }
                
                // Count permissions
                info.permissionCount = (int) group.getNodes().stream()
                    .filter(n -> n instanceof net.luckperms.api.node.types.PermissionNode)
                    .count();
                
                // Get parent groups
                info.parents = group.getNodes().stream()
                    .filter(n -> n instanceof net.luckperms.api.node.types.InheritanceNode)
                    .map(n -> ((net.luckperms.api.node.types.InheritanceNode) n).getGroupName())
                    .collect(Collectors.toList());
                
                groups.add(info);
            }
            
            // Sort by weight descending
            groups.sort((a, b) -> Integer.compare(b.weight, a.weight));
            
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().warning("LuckPerms API not available: " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading LuckPerms groups: " + e.getMessage());
        }
        
        return groups;
    }

    /**
     * Get permissions for a specific LuckPerms group
     */
    public List<PermInfo> getLuckPermsPermissions(String groupName) {
        List<PermInfo> perms = new ArrayList<>();
        
        try {
            net.luckperms.api.LuckPerms api = Bukkit.getServicesManager().load(net.luckperms.api.LuckPerms.class);
            if (api == null) return perms;

            Optional<net.luckperms.api.model.group.Group> groupOpt = api.getGroupManager().getLoadedGroups().stream()
                .filter(g -> g.getName().equalsIgnoreCase(groupName))
                .findFirst();
            
            if (groupOpt.isEmpty()) return perms;
            
            net.luckperms.api.model.group.Group group = groupOpt.get();
            
            for (net.luckperms.api.node.Node node : group.getNodes()) {
                if (node instanceof net.luckperms.api.node.types.PermissionNode permNode) {
                    PermInfo info = new PermInfo();
                    info.permission = permNode.getPermission();
                    info.value = permNode.getValue();
                    info.context = permNode.getContexts().toString();
                    perms.add(info);
                }
            }
            
            perms.sort(Comparator.comparing(p -> p.permission));
            
        } catch (NoClassDefFoundError | Exception e) {
            plugin.getLogger().warning("Error loading LuckPerms permissions: " + e.getMessage());
        }
        
        return perms;
    }

    // ========== TAB ==========

    /**
     * Read TAB groups from its config.yml
     */
    public List<TABGroupInfo> getTABGroups() {
        List<TABGroupInfo> groups = new ArrayList<>();
        
        try {
            File tabFolder = new File(plugin.getDataFolder().getParent(), "TAB");
            File configFile = new File(tabFolder, "config.yml");
            
            if (!configFile.exists()) {
                // Try alternative location
                Plugin tabPlugin = Bukkit.getPluginManager().getPlugin("TAB");
                if (tabPlugin != null) {
                    configFile = new File(tabPlugin.getDataFolder(), "config.yml");
                }
            }
            
            if (!configFile.exists()) return groups;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // TAB stores groups under "Groups" section
            ConfigurationSection groupsSection = config.getConfigurationSection("Groups");
            if (groupsSection == null) return groups;
            
            for (String groupName : groupsSection.getKeys(false)) {
                ConfigurationSection groupSec = groupsSection.getConfigurationSection(groupName);
                
                TABGroupInfo info = new TABGroupInfo();
                info.name = groupName;
                if (groupSec != null) {
                    info.tabprefix = groupSec.getString("tabprefix", "");
                    info.tabsuffix = groupSec.getString("tabsuffix", "");
                    info.tagprefix = groupSec.getString("tagprefix", "");
                    info.tagsuffix = groupSec.getString("tagsuffix", "");
                    info.customtabname = groupSec.getString("customtabname", "");
                    info.belowname = groupSec.getString("belowname", "");
                }
                
                groups.add(info);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading TAB groups: " + e.getMessage());
        }
        
        return groups;
    }

    /**
     * Save a TAB group to config.
     * Always writes all fields (even empty) to ensure the group section is created.
     */
    public boolean saveTABGroup(TABGroupInfo groupInfo) {
        try {
            File tabFolder = new File(plugin.getDataFolder().getParent(), "TAB");
            File configFile = new File(tabFolder, "config.yml");
            
            if (!configFile.exists()) return false;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            String path = "Groups." + groupInfo.name;
            // Always write all fields to ensure the section gets created even with empty values
            config.set(path + ".tabprefix", groupInfo.tabprefix != null ? groupInfo.tabprefix : "");
            config.set(path + ".tabsuffix", groupInfo.tabsuffix != null ? groupInfo.tabsuffix : "");
            config.set(path + ".tagprefix", groupInfo.tagprefix != null ? groupInfo.tagprefix : "");
            config.set(path + ".tagsuffix", groupInfo.tagsuffix != null ? groupInfo.tagsuffix : "");
            config.set(path + ".customtabname", groupInfo.customtabname != null ? groupInfo.customtabname : "");
            config.set(path + ".belowname", groupInfo.belowname != null ? groupInfo.belowname : "");
            
            config.save(configFile);
            
            // Reload TAB
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving TAB group: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a TAB group from config
     */
    public boolean deleteTABGroup(String groupName) {
        try {
            File tabFolder = new File(plugin.getDataFolder().getParent(), "TAB");
            File configFile = new File(tabFolder, "config.yml");
            
            if (!configFile.exists()) return false;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("Groups." + groupName, null);
            config.save(configFile);
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tab reload");
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error deleting TAB group: " + e.getMessage());
            return false;
        }
    }

    // ========== MULTIVERSE ==========

    /**
     * Get all Multiverse worlds using Bukkit's world list + MV properties
     */
    public List<MVWorldInfo> getMultiverseWorlds() {
        List<MVWorldInfo> worlds = new ArrayList<>();
        
        for (World world : Bukkit.getWorlds()) {
            MVWorldInfo info = new MVWorldInfo();
            info.name = world.getName();
            info.environment = world.getEnvironment().name();
            info.seed = world.getSeed();
            info.pvp = world.getPVP();
            info.animals = world.getAllowAnimals();
            info.monsters = world.getAllowMonsters();
            info.difficulty = world.getDifficulty().name();
            info.playerCount = world.getPlayers().size();
            worlds.add(info);
        }
        
        // Also try to get MV-specific properties via command output
        // MV stores additional info like aliases, generators, portals etc.
        try {
            Plugin mvPlugin = Bukkit.getPluginManager().getPlugin("Multiverse-Core");
            if (mvPlugin != null) {
                // Try to access MV world manager via reflection
                // This gets properties not available from Bukkit World
                for (World world : Bukkit.getWorlds()) {
                    for (MVWorldInfo info : worlds) {
                        if (info.name.equals(world.getName())) {
                            // Get MV-specific properties from config
                            File mvFolder = new File(plugin.getDataFolder().getParent(), "Multiverse-Core");
                            File worldsFile = new File(mvFolder, "worlds.yml");
                            if (worldsFile.exists()) {
                                YamlConfiguration worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
                                ConfigurationSection worldSec = worldsConfig.getConfigurationSection("worlds." + info.name);
                                if (worldSec != null) {
                                    info.alias = worldSec.getString("alias", "");
                                    info.generator = worldSec.getString("generator", "default");
                                    info.seedString = worldSec.getString("seed", String.valueOf(info.seed));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fallback: just use Bukkit World properties
        }
        
        return worlds;
    }

    // ========== ESSENTIALSX CHAT ==========

    /**
     * Read EssentialsX chat config
     */
    public ChatConfigInfo getEssentialsChatConfig() {
        ChatConfigInfo info = new ChatConfigInfo();
        
        try {
            File essFolder = new File(plugin.getDataFolder().getParent(), "Essentials");
            File configFile = new File(essFolder, "config.yml");
            
            if (!configFile.exists()) {
                Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essPlugin != null) {
                    configFile = new File(essPlugin.getDataFolder(), "config.yml");
                }
            }
            
            if (!configFile.exists()) return info;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            // Read chat section
            info.defaultFormat = config.getString("chat.format", "");
            
            // Read group-formats
            ConfigurationSection groupFormats = config.getConfigurationSection("chat.group-formats");
            if (groupFormats != null) {
                for (String key : groupFormats.getKeys(false)) {
                    info.groupFormats.put(key, groupFormats.getString(key, ""));
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading EssentialsX chat config: " + e.getMessage());
        }
        
        return info;
    }

    /**
     * Save chat format to EssentialsX config.
     * If group is null, saves the default format.
     * If group is specified, saves a group-specific format.
     */
    public boolean saveEssentialsChatFormat(String group, String format) {
        try {
            File essFolder = new File(plugin.getDataFolder().getParent(), "Essentials");
            File configFile = new File(essFolder, "config.yml");
            
            if (!configFile.exists()) {
                Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essPlugin != null) {
                    configFile = new File(essPlugin.getDataFolder(), "config.yml");
                }
            }
            
            if (!configFile.exists()) return false;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            
            if (group == null || group.isEmpty()) {
                config.set("chat.format", format);
            } else {
                config.set("chat.group-formats." + group, format);
            }
            
            config.save(configFile);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error saving EssentialsX chat format: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove a group-specific chat format
     */
    public boolean removeEssentialsChatFormat(String group) {
        try {
            File essFolder = new File(plugin.getDataFolder().getParent(), "Essentials");
            File configFile = new File(essFolder, "config.yml");
            
            if (!configFile.exists()) {
                Plugin essPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
                if (essPlugin != null) {
                    configFile = new File(essPlugin.getDataFolder(), "config.yml");
                }
            }
            
            if (!configFile.exists()) return false;
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("chat.group-formats." + group, null);
            config.save(configFile);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing EssentialsX chat format: " + e.getMessage());
            return false;
        }
    }

    // ========== WORLDEDIT ==========

    /**
     * Get current world border info for the player's world
     */
    public WorldBorderInfo getWorldBorderInfo(World world) {
        WorldBorderInfo info = new WorldBorderInfo();
        if (world == null) return info;
        
        org.bukkit.WorldBorder border = world.getWorldBorder();
        info.size = border.getSize();
        info.center = border.getCenter();
        info.damageAmount = border.getDamageAmount();
        info.damageBuffer = border.getDamageBuffer();
        info.warningDistance = border.getWarningDistance();
        info.warningTime = border.getWarningTime();
        
        return info;
    }

    // ========== DATA CLASSES ==========

    public static class GroupInfo {
        public String name;
        public String displayName;
        public String prefix;
        public String suffix;
        public int weight;
        public int permissionCount;
        public List<String> parents = new ArrayList<>();
    }

    public static class PermInfo {
        public String permission;
        public boolean value;
        public String context;
    }

    public static class TABGroupInfo {
        public String name;
        public String tabprefix;
        public String tabsuffix;
        public String tagprefix;
        public String tagsuffix;
        public String customtabname;
        public String belowname;
    }

    public static class MVWorldInfo {
        public String name;
        public String environment;
        public long seed;
        public String seedString;
        public String alias;
        public String generator;
        public boolean pvp;
        public boolean animals;
        public boolean monsters;
        public String difficulty;
        public int playerCount;
    }

    public static class WorldBorderInfo {
        public double size;
        public org.bukkit.Location center;
        public double damageAmount;
        public double damageBuffer;
        public int warningDistance;
        public int warningTime;
    }

    public static class ChatConfigInfo {
        public String defaultFormat = "";
        public Map<String, String> groupFormats = new LinkedHashMap<>();
    }
}
