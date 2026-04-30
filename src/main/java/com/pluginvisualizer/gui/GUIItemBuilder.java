package com.pluginvisualizer.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder utility for creating ItemStack with custom names, lore, and materials.
 * Makes GUI item creation clean and readable.
 */
public class GUIItemBuilder {

    private final Material material;
    private String name;
    private final List<String> lore = new ArrayList<>();
    private int amount = 1;
    private boolean glowing = false;

    public GUIItemBuilder(Material material) {
        this.material = material;
    }

    public GUIItemBuilder name(String name) {
        this.name = ChatColor.translateAlternateColorCodes('&', name);
        return this;
    }

    public GUIItemBuilder lore(String... lines) {
        for (String line : lines) {
            this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return this;
    }

    public GUIItemBuilder lore(List<String> lines) {
        for (String line : lines) {
            this.lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return this;
    }

    public GUIItemBuilder amount(int amount) {
        this.amount = Math.max(1, Math.min(64, amount));
        return this;
    }

    public GUIItemBuilder glowing(boolean glowing) {
        this.glowing = glowing;
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(name);
            }
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            if (glowing) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.LURE, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        
        return item;
    }

    /**
     * Quick utility: Create a simple named item
     */
    public static ItemStack create(Material material, String name, String... lore) {
        return new GUIItemBuilder(material).name(name).lore(lore).build();
    }
}
