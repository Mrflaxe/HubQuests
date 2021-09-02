package ru.mrflaxe.hubquests.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.gui.MenuConfiguration;
import ru.mrflaxe.hubquests.gui.MenuProvider;
import ru.mrflaxe.hubquests.gui.holders.DifficultyChooserHolder;
import ru.mrflaxe.hubquests.gui.holders.MenuHolder;
import ru.mrflaxe.hubquests.protocol.PacketManager;
import ru.mrflaxe.hubquests.session.Session;
import ru.mrflaxe.hubquests.session.SessionContainer;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.lib.configuration.Messages;

public class InventoryListener implements Listener {
    
    private final Messages messages;
    private final Configuration config;
    private final MenuConfiguration menuConfig;
    private final MenuProvider menuProvider;
    private final DatabaseManager databaseManager;
    private final SessionContainer sessionContainer;
    private final PacketManager packetManager;
    private final Plugin plugin;
    
    public InventoryListener(
            Messages messages,
            Configuration config,
            MenuConfiguration menuConfig,
            MenuProvider menuProvider,
            DatabaseManager databaseManager,
            SessionContainer sessionContainer,
            PacketManager packetManager,
            Plugin plugin
    ) {
        this.messages = messages;
        this.config = config;
        this.menuConfig = menuConfig;
        this.menuProvider = menuProvider;
        this.databaseManager = databaseManager;
        this.sessionContainer = sessionContainer;
        this.packetManager = packetManager;
        this.plugin = plugin;
    }

    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        Entity whoClicked = e.getWhoClicked();
        if(!(whoClicked instanceof Player)) return;
        
        Player player = (Player) whoClicked;
        
        if(inv.getHolder() instanceof DifficultyChooserHolder) {
            e.setCancelled(true);
            
            ItemStack clickedItem = e.getCurrentItem();
            if(clickedItem == null) return;
            
            DifficultyChooserHolder holder = (DifficultyChooserHolder) inv.getHolder();
            int secretID = holder.getSecretID();
            
            SecretModel secret = databaseManager.getSecret(secretID).join();
            
            if(clickedItem.equals(menuConfig.getItem(menuConfig.getSection("difficulty.buttons.first"))))
                saveChoice(secret, 1, player);
            
            if(clickedItem.equals(menuConfig.getItem(menuConfig.getSection("difficulty.buttons.second"))))
                saveChoice(secret, 2, player);
            
            if(clickedItem.equals(menuConfig.getItem(menuConfig.getSection("difficulty.buttons.third"))))
                saveChoice(secret, 3, player);
            
            return;
        }
        
        if(inv.getHolder() instanceof MenuHolder) {
            e.setCancelled(true);
            
            ItemStack clickedItem = e.getCurrentItem();
            if(clickedItem == null) return;
            
            ItemStack pattern = menuConfig.getItem(menuConfig.getSection("quests.buttons.available"));
            Material type = pattern.getType();
            
            if(clickedItem.getType() != type) return;
            if(pattern.getItemMeta().hasEnchants()) return;
            
            int slot = e.getSlot();
            
            List<SecretModel> secrets = getSecrets(slot);
            if(secrets == null) {
                messages.getAndSend(whoClicked, "error.no-secrets");
                return;
            }
            
            int reward = config.getInt("quests.day_" + slot + ".reward");
            
            Session session = new Session(whoClicked.getName(), slot, reward , secrets, packetManager);
            session.sendPackets(plugin);
            sessionContainer.openSession(session);
            
            List<String> message = messages.getColoredList("quest-accept");
            message = messages.formatList(message, "%total%", secrets.size(), "%reward%", reward);
            
            message.forEach(m -> whoClicked.sendMessage(m));
            menuProvider.openMenu(player);
            player.playSound(player.getLocation(), getSound(plugin, config), 1, 1);
            
        }
    }
    
    private Sound getSound(Plugin plugin, Configuration config) {
        String example = config.getString("sounds.quest-accept");
        Sound sound = Sound.valueOf(example);
        
        if(sound == null) {
            plugin.getLogger().warning("Failed to parse " + example + " into Sound object for section \"sounds.quest-accept\"");
            plugin.getLogger().warning("Will use default sound: BLOCK_BEACON_POWER_SELECT");
            sound = Sound.BLOCK_BEACON_POWER_SELECT;
        }
        
        return sound;
    }
    
    private List<SecretModel> getSecrets(int slot) {
        List<SecretModel> allSecrets = databaseManager.getAllSecrets().join();
        
        int easyCount = config.getInt("quests.day_" + slot + ".to-find.easy");
        int mediumCount = config.getInt("quests.day_" + slot + ".to-find.medium");
        int hardCount = config.getInt("quests.day_" + slot + ".to-find.hard");
        
        List<SecretModel> easySecrets = collect(easyCount, 1, allSecrets);
        List<SecretModel> mediumSecrets = collect(mediumCount, 2, allSecrets);
        List<SecretModel> hardSecrets = collect(hardCount, 3, allSecrets);
        
        if(easySecrets == null || mediumSecrets == null || hardSecrets == null) return null;
        
        List<SecretModel> result = new ArrayList<>();
        result.addAll(easySecrets);
        result.addAll(mediumSecrets);
        result.addAll(hardSecrets);
        
        return result;
    }
    
    /*
     * This method returns a list of random secrets
     */
    private List<SecretModel> collect(int count, int difficulty, List<SecretModel> list) {
        list = list.parallelStream()
                .filter(secret -> secret.getDifficulty() == difficulty)
                .collect(Collectors.toList());
        
        // If there are not enough secrets of the required complexity in the database, then we return null and notify the admins
        if(list.size() < count) {
            plugin.getLogger().severe("Failed to find enough secrets in database.");
            plugin.getLogger().severe("Please, make sure that you created enough secrets with difficulty level: " + difficulty);
            return null;
        }
        
        List<SecretModel> result = new ArrayList<>();
        Random r = new Random();
        
        // Simple cycle for getting pseudo random list of secrets
        for(int i = 0; i < count; i++) {
            int random = r.nextInt(list.size());
            result.add(list.get(random));
            list.remove(random);
        }
        
        return result;
    }


    private void saveChoice(SecretModel secret, int difficulty, Player player) {
        secret.setDifficulty(difficulty);
        databaseManager.saveSecret(secret);
        player.closeInventory();
        
        packetManager.sendCustomizerPacket(player, secret.getLocation(), secret.getId());
    }
    
    public void register(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
