package ru.mrflaxe.hubquests.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.ProfileModel;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.protocol.PacketManager;
import ru.mrflaxe.hubquests.session.Session;
import ru.mrflaxe.hubquests.session.SessionContainer;

public class PlayerListener implements Listener {

    private final DatabaseManager databaseManager;
    private final PacketManager packetManager;
    private final SessionContainer sessions;
    private final Plugin plugin;
    
    public PlayerListener(DatabaseManager databaseManager, PacketManager packetManager, SessionContainer sessions, Plugin plugin) {
        this.databaseManager = databaseManager;
        this.packetManager = packetManager;
        this.sessions = sessions;
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String name = player.getName();
        
        // Creating profile data for new players
        if(!databaseManager.hasProfile(name).join()) {
            ProfileModel profile = new ProfileModel(name);
            databaseManager.saveProfile(profile);
            return;
        }
        
        // If player has active session sends him packets
        if(sessions.hasSession(player)) {
            Session session = sessions.getSession(player);
            session.sendPackets(plugin);
        }
        
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        
        // stop sending rotation packets to player
        if(sessions.hasSession(player)) {
            Session session = sessions.getSession(player);
            session.closeRotationTask();
        }
    }
    
    @EventHandler
    public void onScrolling(PlayerItemHeldEvent e) {
        Player player = e.getPlayer();
        int newSlot = e.getNewSlot();
        int oldSlot = e.getPreviousSlot();
        ItemStack newItem = player.getInventory().getItem(newSlot);
        ItemStack oldItem = player.getInventory().getItem(oldSlot);
        
        // If item is not customizer
        // Checking it via getPersistentDataContainer
        NamespacedKey key = new NamespacedKey(plugin, "type");
        if(newItem == null && oldItem == null) return;
        
        // If player remove customizer from main slot
        if(oldItem != null && oldItem.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            List<SecretModel> secrets = databaseManager.getAllSecrets().join();
            int[] ids = secrets.parallelStream().mapToInt(SecretModel::getId).toArray();
            packetManager.removeEntityPacket(player, ids);
            
            // If player have session I sending him this packets back
            if(sessions.hasSession(player)) {
                Session session = sessions.getSession(player);
                session.sendPackets(plugin);
            }
        }
        
        // If player take customizer in main slot
        if(newItem != null && newItem.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            // If player have a session I remove packets for a while
            if(sessions.hasSession(player)) {
                Session session = sessions.getSession(player);
                session.removePackets();
            }
            
            List<SecretModel> secrets = databaseManager.getAllSecrets().join();
            int[] ids = secrets.parallelStream().mapToInt(SecretModel::getId).toArray();
            packetManager.sendEntityPackets(player, ids);
            
        }
        
        
    }
    
    @EventHandler
    public void onBlockInterract(PlayerInteractEvent e) {
        if(e.getHand() == EquipmentSlot.OFF_HAND) return;
        
        Player player = e.getPlayer();
        ItemStack item = e.getItem();
        
        if(item == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "type");
        
        if(!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;
        if(!player.hasPermission("command.quest.admin")) return;
        
        Action action = e.getAction();
        
        if(action == Action.RIGHT_CLICK_BLOCK) {
            Block block = e.getClickedBlock();
            Location secretLocation = block.getLocation().add(0.5, 0, 0.5);
            
            // If secret already exist on this location
            if(databaseManager.getSecretByLoc(secretLocation).join() != null) return;
            
            SecretModel newSecret = new SecretModel((int) (Math.random() * Integer.MAX_VALUE), secretLocation, 1);
            databaseManager.saveSecret(newSecret);
            
            int id = newSecret.getId();
            packetManager.sendCustomizerPacket(player, secretLocation, id);
            
            return;
        }
        
    }
    
    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        ItemStack item = e.getItemDrop().getItemStack();
        
        if(item == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "type");
        String type = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        
        if(!type.equals("admin")) return;
        if(!player.hasPermission("command.quest.admin")) return;
        
        List<SecretModel> secrets = databaseManager.getAllSecrets().join();
        int[] ids = secrets.parallelStream().mapToInt(SecretModel::getId).toArray();
        packetManager.removeEntityPacket(player, ids);
    }
    
    @EventHandler
    public void onPickupItem(EntityPickupItemEvent e) {
        if(e.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) e.getEntity();
        
        ItemStack item = e.getItem().getItemStack();
        
        if(item == null) return;
        
        NamespacedKey key = new NamespacedKey(plugin, "type");
        String type = item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
        
        if(!type.equals("admin")) return;
        if(!player.hasPermission("command.quest.admin")) return;
        
        List<SecretModel> secrets = databaseManager.getAllSecrets().join();
        int[] ids = secrets.parallelStream().mapToInt(SecretModel::getId).toArray();
        packetManager.sendEntityPackets(player, ids);
    }
    
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
}
