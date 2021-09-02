package ru.mrflaxe.hubquests.gui;

import java.util.Calendar;
import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.ProfileModel;
import ru.mrflaxe.hubquests.session.Session;
import ru.mrflaxe.hubquests.session.SessionContainer;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.lib.configuration.Messages;

public class MenuProvider {

    private final Messages messages;
    private final MenuConfiguration menuConfig;
    private final Configuration config;
    private final DatabaseManager databaseManager;
    private final SessionContainer sessionContainer;
    private final Plugin plugin;
    
    public MenuProvider(Messages messages,
            MenuConfiguration menuConfig,
            Configuration config,
            DatabaseManager databaseManager,
            SessionContainer sessionContainer,
            Plugin plugin
     ) {
        this.messages = messages;
        this.menuConfig = menuConfig;
        this.config = config;
        this.databaseManager = databaseManager;
        this.sessionContainer = sessionContainer;
        this.plugin = plugin;
    }
    
    public void openMenu(Player player) {
        Inventory menu = menuConfig.createMenu();
        String name = player.getName();
        
        ProfileModel profile = databaseManager.getProfile(name).join();
        if(profile == null) {
            plugin.getLogger().severe("Player don't have a profile data. Impossible to create a gui menu.");
            messages.getAndSend(player, "error.no-profile");
            return;
        }
        
        ItemStack background = menuConfig.getItem(menuConfig.getSection("background"));
        
        for(int i = 0; i < 9; i++) {
            menu.setItem(i, background);
        }
        
        Calendar lastComplete = profile.getLastCompleteQuest();
        Calendar today = Calendar.getInstance();
        int lastQuestNumber = profile.getLastQuestNumber();
        

        int currentYear = today.get(Calendar.YEAR);
        int year = lastComplete.get(Calendar.YEAR);
        int currentDay;
        int day;
        
        // This code will prevent errors that may occur when the player completes tasks in the next year
        // Of course, it can work a little crookedly. After all, there are leap years, the duration of which is 366 days,
        // but I will deal with the implementation of such code later
        
        if(currentYear == year) {
            day = lastComplete.get(Calendar.DAY_OF_YEAR);
            currentDay = today.get(Calendar.DAY_OF_YEAR);
        } else {
            int yearsDifference = currentYear - year;
            currentDay = yearsDifference * 365 + today.get(Calendar.DAY_OF_YEAR);
            day = lastComplete.get(Calendar.DAY_OF_YEAR);
        }
        
        int difference = currentDay - day;
        
        if(difference > 1) {
            profile.setLastQuestNumber(0);
            profile.setLastCompleteQuest(today);
            
            databaseManager.saveProfile(profile);
        }
        
        if(difference == 1 && lastQuestNumber == 7) {
            profile.setLastQuestNumber(0);
            profile.setLastCompleteQuest(today);
            
            databaseManager.saveProfile(profile);
            
            lastComplete = profile.getLastCompleteQuest();
            lastQuestNumber = profile.getLastQuestNumber();
        }
        
        for(int i = 1; i <= lastQuestNumber; i++) {
            ItemStack completed = menuConfig.getItem(menuConfig.getSection("quests.buttons.completed"));

            ItemMeta meta = completed.getItemMeta();
            List<String> lore = meta.getLore();
            
            int reward = config.getInt("quests.day_" + i + ".reward");
            int found = getTotal(i);
            
            lore = menuConfig.formatList(lore, "%reward%", reward, "%found%", found, "%total%", found);
            meta.setLore(lore);
            completed.setItemMeta(meta);
            
            menu.setItem(i, completed);
        }
        
        int number = lastQuestNumber + 1;
        
        if(difference == 0 && lastQuestNumber != 0) {
            ItemStack nextUnavailable = menuConfig.getItem(menuConfig.getSection("quests.buttons.unavailable-next"));

            ItemMeta completedMeta = nextUnavailable.getItemMeta();
            List<String> completedLore = completedMeta.getLore();
            
            int completedReward = config.getInt("quests.day_" + number + ".reward");
            int completedTotal = getTotal(number);
            
            completedLore = menuConfig.formatList(completedLore, "%reward%", completedReward, "%find%", 0, "%total%", completedTotal);
            completedMeta.setLore(completedLore);
            nextUnavailable.setItemMeta(completedMeta);
            
            menu.setItem(number, nextUnavailable);
            
            for(int i = lastQuestNumber + 2; i < 8; i ++) {
                ItemStack unavailable = menuConfig.getItem(menuConfig.getSection("quests.buttons.unavailable"));
                
                ItemMeta meta = unavailable.getItemMeta();
                List<String> lore = meta.getLore();
                
                int reward = config.getInt("quests.day_" + i + ".reward");
                int total = getTotal(i);
                
                lore = menuConfig.formatList(lore, "%reward%", reward, "%find%", 0, "%total%", total);
                meta.setLore(lore);
                unavailable.setItemMeta(meta);
                
                menu.setItem(i, unavailable);
            }
        } else {
            if(sessionContainer.hasSession(player)) {
                ItemStack available = menuConfig.getItem(menuConfig.getSection("quests.buttons.in-progress"));
                Session session = sessionContainer.getSession(player);
                
                ItemMeta availableMeta = available.getItemMeta();
                List<String> availableLore = availableMeta.getLore();
                
                int availableReward = config.getInt("quests.day_" + number + ".reward");
                int availableTotal = getTotal(number);
                int found = session.getFound().size();
                
                availableLore = menuConfig.formatList(availableLore, "%reward%", availableReward, "%found%", found, "%total%", availableTotal);
                availableMeta.setLore(availableLore);
                availableMeta.addEnchant(Enchantment.LURE, 1, true);
                availableMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                available.setItemMeta(availableMeta);
                
                menu.setItem(number, available);
            } else {
                ItemStack available = menuConfig.getItem(menuConfig.getSection("quests.buttons.available"));
                
                ItemMeta availableMeta = available.getItemMeta();
                List<String> availableLore = availableMeta.getLore();
                
                int availableReward = config.getInt("quests.day_" + number + ".reward");
                int availableTotal = getTotal(number);
                
                availableLore = menuConfig.formatList(availableLore, "%reward%", availableReward, "%find%", 0, "%total%", availableTotal);
                availableMeta.setLore(availableLore);
                available.setItemMeta(availableMeta);
                
                menu.setItem(number, available);
            }
            
            for(int i = lastQuestNumber + 2; i < 8; i ++) {
                ItemStack unavailable = menuConfig.getItem(menuConfig.getSection("quests.buttons.unavailable"));
                
                ItemMeta meta = unavailable.getItemMeta();
                List<String> lore = meta.getLore();
                
                int reward = config.getInt("quests.number_" + i + ".reward");
                int total = getTotal(i);
                
                lore = menuConfig.formatList(lore, "%reward%", reward, "%find%", 0, "%total%", total);
                meta.setLore(lore);
                unavailable.setItemMeta(meta);
                
                menu.setItem(i, unavailable);
            }
        }
        player.openInventory(menu);
    }
    
    public void openDifficultyMenu(Player player, int entityID) {
        Inventory menu = menuConfig.createDifficultyChooser(entityID);
        
        ItemStack background = menuConfig.getItem(menuConfig.getSection("background"));
        
        for(int i = 0; i < 9; i++) {
            menu.setItem(i, background);
        }
        
        ItemStack firstButton = menuConfig.getItem(menuConfig.getSection("difficulty.buttons.first"));
        ItemStack secondButton = menuConfig.getItem(menuConfig.getSection("difficulty.buttons.second"));
        ItemStack thirdButton = menuConfig.getItem(menuConfig.getSection("difficulty.buttons.third"));
        
        menu.setItem(3, firstButton);
        menu.setItem(4, secondButton);
        menu.setItem(5, thirdButton);
        
        player.openInventory(menu);
    }
    
    private int getTotal(int i) {
        int easyCount = config.getInt("quests.day_" + i + ".to-find.easy");
        int mediumCount = config.getInt("quests.day_" + i + ".to-find.medium");
        int hardCount = config.getInt("quests.day_" + i + ".to-find.hard");
        
        return easyCount + hardCount + mediumCount;
    }
    
}