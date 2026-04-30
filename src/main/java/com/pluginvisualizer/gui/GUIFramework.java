package com.pluginvisualizer.gui;

import com.pluginvisualizer.PluginVisualizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for all PluginVisualizer GUIs.
 * 
 * Click handling works through 3 detection methods (see PluginVisualizer.onInventoryClick).
 * All GUI titles MUST start with §8§l so the title-based fallback detection works.
 * Both LEFT and RIGHT clicks are handled.
 */
public abstract class GUIFramework implements InventoryHolder {

    protected final Player player;
    protected final PluginVisualizer plugin;
    protected Inventory inventory;
    protected final Map<Integer, ClickAction> clickActions = new HashMap<>();
    protected String title;

    @FunctionalInterface
    public interface ClickAction {
        void onClick(InventoryClickEvent event);
    }

    public GUIFramework(Player player, String title, int size) {
        this.player = player;
        this.plugin = PluginVisualizer.getInstance();
        this.title = title;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    /**
     * Opens this GUI for the player.
     * MUST register with the plugin's tracker BEFORE opening inventory.
     */
    public void open() {
        plugin.registerGUI(player, this);
        player.openInventory(inventory);
    }

    /**
     * Handle a click event. Both LEFT and RIGHT clicks reach this method.
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ClickAction action = clickActions.get(slot);
        if (action != null) {
            action.onClick(event);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        // Override in subclasses if needed
    }

    /**
     * Set an item with a click action. Fires on BOTH left and right clicks.
     */
    public void setItem(int slot, ItemStack item, ClickAction action) {
        inventory.setItem(slot, item);
        if (action != null) {
            clickActions.put(slot, action);
        }
    }

    /**
     * Set an item without a click action (decoration only).
     */
    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    protected void fillBorder(org.bukkit.Material borderMaterial) {
        ItemStack border = new GUIItemBuilder(borderMaterial).name(" ").build();
        int size = inventory.getSize();
        int columns = 9;
        int rows = size / columns;
        for (int i = 0; i < size; i++) {
            int row = i / columns;
            int col = i % columns;
            if (row == 0 || row == rows - 1 || col == 0 || col == columns - 1) {
                if (inventory.getItem(i) == null || inventory.getItem(i).getType() == org.bukkit.Material.AIR) {
                    setItem(i, border);
                }
            }
        }
    }

    protected void fillBorder() {
        fillBorder(org.bukkit.Material.GRAY_STAINED_GLASS_PANE);
    }

    protected void fillEmpty(org.bukkit.Material material) {
        ItemStack filler = new GUIItemBuilder(material).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == org.bukkit.Material.AIR) {
                setItem(i, filler);
            }
        }
    }

    protected void addBackButton(int slot, Runnable backAction) {
        ItemStack backItem = new GUIItemBuilder(org.bukkit.Material.ARROW)
                .name("§c§l← Back").lore("§7Click to go back").build();
        setItem(slot, backItem, event -> {
            plugin.playSound(player, "click");
            backAction.run();
        });
    }

    protected void addCloseButton(int slot) {
        ItemStack closeItem = new GUIItemBuilder(org.bukkit.Material.BARRIER)
                .name("§c§l✕ Close").lore("§7Click to close").build();
        setItem(slot, closeItem, event -> {
            plugin.playSound(player, "click");
            player.closeInventory();
        });
    }

    protected void addInfoButton(int slot, String... lines) {
        ItemStack infoItem = new GUIItemBuilder(org.bukkit.Material.BOOK)
                .name("§e§lInfo").lore(lines).build();
        setItem(slot, infoItem);
    }

    public void refresh() {
        clickActions.clear();
        inventory.clear();
        build();
    }

    protected abstract void build();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() { return player; }
    public String getTitle() { return title; }
}
