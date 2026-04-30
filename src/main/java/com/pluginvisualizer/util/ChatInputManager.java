package com.pluginvisualizer.util;

import com.pluginvisualizer.PluginVisualizer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chat-based input for values that can't be set via GUI clicks.
 * 
 * Flow:
 * 1. Player clicks an item (e.g., "Create Group")
 * 2. GUI closes, player gets a chat prompt
 * 3. Player types the value in chat
 * 4. Input is captured, callback executes, GUI reopens
 * 
 * Players can type "cancel" to abort.
 */
public class ChatInputManager implements Listener {

    private final PluginVisualizer plugin;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputManager(PluginVisualizer plugin) {
        this.plugin = plugin;
    }

    /**
     * Request chat input from a player.
     * 
     * @param player The player to request input from
     * @param prompt The message to show the player
     * @param callback What to do with the input (runs on main thread)
     */
    public void requestInput(Player player, String prompt, InputCallback callback) {
        UUID uuid = player.getUniqueId();
        
        pendingInputs.put(uuid, new PendingInput(callback, System.currentTimeMillis()));
        
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "§m                                              ");
        player.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "  " + prompt);
        player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "  Type your input in chat, or §c'cancel' §7to abort");
        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "§m                                              ");
        player.sendMessage("");
    }

    /**
     * Request input with a specific validation rule
     */
    public void requestInput(Player player, String prompt, InputCallback callback, InputValidator validator) {
        UUID uuid = player.getUniqueId();
        
        pendingInputs.put(uuid, new PendingInput(callback, validator, System.currentTimeMillis()));
        
        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "§m                                              ");
        player.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "  " + prompt);
        player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "  Type your input in chat, or §c'cancel' §7to abort");
        player.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "§m                                              ");
        player.sendMessage("");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        PendingInput pending = pendingInputs.get(uuid);
        if (pending == null) return;
        
        // Cancel the chat message from showing publicly
        event.setCancelled(true);
        
        String input = event.getMessage().trim();
        
        // Check for cancel
        if (input.equalsIgnoreCase("cancel")) {
            pendingInputs.remove(uuid);
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Input cancelled.");
            return;
        }
        
        // Validate input
        if (pending.validator != null) {
            String error = pending.validator.validate(input);
            if (error != null) {
                player.sendMessage(plugin.getPrefix() + ChatColor.RED + error);
                player.sendMessage(plugin.getPrefix() + ChatColor.GRAY + "Try again or type §c'cancel'");
                return;
            }
        }
        
        // Check timeout
        int timeout = plugin.getConfig().getInt("settings.chat-input-timeout", 30);
        long elapsed = (System.currentTimeMillis() - pending.timestamp) / 1000;
        if (elapsed > timeout) {
            pendingInputs.remove(uuid);
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Input timed out. Please try again.");
            return;
        }
        
        // Remove pending and execute callback on main thread
        pendingInputs.remove(uuid);
        final String finalInput = input;
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            pending.callback.onInput(finalInput);
        });
    }

    public boolean hasPendingInput(Player player) {
        return pendingInputs.containsKey(player.getUniqueId());
    }

    public void cancelPendingInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    @FunctionalInterface
    public interface InputCallback {
        void onInput(String input);
    }

    @FunctionalInterface
    public interface InputValidator {
        /**
         * @return error message if invalid, null if valid
         */
        String validate(String input);
    }

    private static class PendingInput {
        final InputCallback callback;
        final InputValidator validator;
        final long timestamp;

        PendingInput(InputCallback callback, long timestamp) {
            this(callback, null, timestamp);
        }

        PendingInput(InputCallback callback, InputValidator validator, long timestamp) {
            this.callback = callback;
            this.validator = validator;
            this.timestamp = timestamp;
        }
    }
}
