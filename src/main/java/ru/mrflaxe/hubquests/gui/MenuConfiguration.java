package ru.mrflaxe.hubquests.gui;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import ru.mrflaxe.hubquests.exception.ItemStackParsingException;
import ru.mrflaxe.hubquests.exception.UnknownItemTypeException;
import ru.mrflaxe.hubquests.gui.holders.DifficultyChooserHolder;
import ru.mrflaxe.hubquests.gui.holders.MenuHolder;
import ru.soknight.lib.configuration.AbstractConfiguration;
import ru.soknight.lib.configuration.Configuration;

public class MenuConfiguration extends Configuration {
    
    public MenuConfiguration(JavaPlugin plugin) {
        super(plugin, "menu.yml");
    }
    
    public Inventory createMenu() {
        String title = getColoredString("quests.interface.title", "");
        return Bukkit.createInventory(new MenuHolder(), 9, title);
    }
    
    public Inventory createDifficultyChooser(int entityID) {
        String title = getColoredString("difficulty.interface.title", "");
        return Bukkit.createInventory(new DifficultyChooserHolder(entityID), 9, title);
    }
    
    public ItemStack getItem(ConfigurationSection section) {
        ItemStack item;
        if(!section.isString("material"))
            throw new ItemStackParsingException("item type isn't specified");
        
        try {
            Material type = Material.valueOf(section.getString("material").toUpperCase());
            item = new ItemStack(type);
        } catch (EnumConstantNotPresentException ex) {
            throw new UnknownItemTypeException(section.getString("type"));
        }
        
        ItemMeta meta = item.getItemMeta();
        if(meta == null)
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        
        meta.addItemFlags(ItemFlag.values());
        
        if(section.isString("name")) {
            String displayname = colorize(section.getString("name"));
            meta.setDisplayName(displayname);
        }
        
        if(section.isList("lore")) {
            List<String> lore = section.getStringList("lore").stream()
                    .map(AbstractConfiguration::colorize)
                    .collect(Collectors.toList());
            meta.setLore(lore);
        }
        
        if(section.getBoolean("enchanted", false))
            meta.addEnchant(Enchantment.ARROW_INFINITE, 1, true);
        
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
