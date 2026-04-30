package com.pluginvisualizer;

import com.pluginvisualizer.gui.GUIFramework;
import com.pluginvisualizer.util.ChatInputManager;
import com.pluginvisualizer.util.PluginDataLoader;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PluginVisualizer extends JavaPlugin implements Listener {

    private static PluginVisualizer instance;
    private ChatInputManager chatInputManager;
    private PluginDataLoader dataLoader;
    private final Map<String, Boolean> pluginStatus = new HashMap<>();
    
    // RELIABLE player-based tracking — the ONLY way to guarantee click handling works
    private final ConcurrentHashMap<UUID, GUIFramework> activeGUIs = new ConcurrentHashMap<>();
    
    // Title prefix that ALL our GUIs start with — used as fallback detection
    public static final String GUI_TITLE_PREFIX = "§8§l";

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        chatInputManager = new ChatInputManager(this);
        dataLoader = new PluginDataLoader(this);
        
        // Register events — use HIGH priority so we run before other plugins
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(chatInputManager, this);
        
        detectPlugins();
        
        getLogger().info("PluginVisualizer enabled!");
        getLogger().info("Detected: LuckPerms=" + isPluginInstalled("LuckPerms") 
            + " TAB=" + isPluginInstalled("TAB") 
            + " WorldEdit=" + isPluginInstalled("WorldEdit")
            + " Multiverse-Core=" + isPluginInstalled("Multiverse-Core")
            + " Essentials=" + isPluginInstalled("Essentials"));
    }

    @Override
    public void onDisable() {
        for (UUID uuid : activeGUIs.keySet()) {
            Player player = getServer().getPlayer(uuid);
            if (player != null) player.closeInventory();
        }
        activeGUIs.clear();
    }

    private void detectPlugins() {
        String[] supported = {"LuckPerms", "TAB", "WorldEdit", "Multiverse-Core", "Essentials"};
        for (String name : supported) {
            pluginStatus.put(name, getServer().getPluginManager().getPlugin(name) != null);
        }
    }

    public static PluginVisualizer getInstance() { return instance; }
    public ChatInputManager getChatInputManager() { return chatInputManager; }
    public PluginDataLoader getDataLoader() { return dataLoader; }
    public boolean isPluginInstalled(String name) { return pluginStatus.getOrDefault(name, false); }
    public Map<String, Boolean> getPluginStatus() { return pluginStatus; }
    
    public void registerGUI(Player player, GUIFramework gui) {
        activeGUIs.put(player.getUniqueId(), gui);
    }
    
    public void unregisterGUI(Player player) {
        activeGUIs.remove(player.getUniqueId());
    }
    
    public GUIFramework getActiveGUI(Player player) {
        return activeGUIs.get(player.getUniqueId());
    }
    
    /**
     * Check if an inventory title belongs to one of our GUIs.
     * This is the FALLBACK detection method if holder check fails.
     */
    public boolean isOurInventory(String title) {
        if (title == null) return false;
        return title.startsWith(GUI_TITLE_PREFIX) && (
            title.contains("PluginVisualizer") ||
            title.contains("LuckPerms") ||
            title.contains("TAB") ||
            title.contains("WorldEdit") ||
            title.contains("Multiverse") ||
            title.contains("Chat") ||
            title.contains("Rank Setup") ||
            title.contains("Perms") ||
            title.contains("Display")
        );
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        if (!player.hasPermission("pluginvisualizer.use")) {
            player.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }
        new com.pluginvisualizer.gui.MainMenuGUI(player).open();
        return true;
    }

    /**
     * MAIN CLICK HANDLER — uses HIGHEST priority + triple detection method.
     * 
     * Detection order:
     * 1. Player UUID in activeGUIs map (most reliable)
     * 2. InventoryHolder instanceof GUIFramework (standard method)
     * 3. Inventory title matches our pattern (fallback)
     * 
     * If ANY of these match, we cancel the event and handle the click.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        UUID uuid = player.getUniqueId();
        GUIFramework gui = null;
        
        // METHOD 1: Player tracking map (most reliable)
        gui = activeGUIs.get(uuid);
        
        // METHOD 2: Check InventoryHolder
        if (gui == null && event.getInventory().getHolder() instanceof GUIFramework) {
            gui = (GUIFramework) event.getInventory().getHolder();
        }
        
        // METHOD 3: Title-based detection (fallback)
        if (gui == null) {
            String title = event.getView().getTitle();
            if (isOurInventory(title)) {
                // We know it's our GUI but lost the reference — re-register from holder if possible
                if (event.getInventory().getHolder() instanceof GUIFramework) {
                    gui = (GUIFramework) event.getInventory().getHolder();
                    activeGUIs.put(uuid, gui); // Re-register
                }
            }
        }
        
        // Not our GUI — do nothing
        if (gui == null) return;
        
        // ALWAYS cancel — prevents item pickup, moving, etc.
        event.setCancelled(true);
        
        // Only process clicks in the TOP inventory (our custom one)
        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        
        // Handle the click — works for LEFT, RIGHT, and SHIFT clicks
        gui.handleClick(event);
    }
    
    /**
     * Block ALL dragging in our GUIs to prevent item manipulation
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if any of the dragged slots are in our inventory
        if (activeGUIs.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        
        // Fallback: title check
        if (isOurInventory(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        GUIFramework gui = activeGUIs.remove(player.getUniqueId());
        if (gui != null) {
            gui.handleClose(event);
        }
    }
    
    /**
     * Clean up when players disconnect
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeGUIs.remove(event.getPlayer().getUniqueId());
    }

    public void playSound(Player player, String type) {
        if (!getConfig().getBoolean("settings.sounds.enabled", true)) return;
        try {
            String soundName = getConfig().getString("settings.sounds." + type, "");
            if (soundName.isEmpty()) return;
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 0.7f, 1.0f);
        } catch (IllegalArgumentException ignored) {}
    }

    public String getPrefix() {
        return ChatColor.translateAlternateColorCodes('&', 
            getConfig().getString("settings.prefix", "§8§l[§9PV§8§l] §r"));
    }

    public void dispatchCommand(String command) {
        getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }
}
