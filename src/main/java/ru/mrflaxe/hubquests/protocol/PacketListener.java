package ru.mrflaxe.hubquests.protocol;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.EntityUseAction;

import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.ProfileModel;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.events.CollectSecretEvent;
import ru.mrflaxe.hubquests.gui.MenuProvider;
import ru.mrflaxe.hubquests.protocol.wrappers.WrapperPlayClientUseEntity;
import ru.mrflaxe.hubquests.session.Session;
import ru.mrflaxe.hubquests.session.SessionContainer;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.peconomy.api.PEconomyAPI;
import ru.soknight.peconomy.database.model.WalletModel;

public class PacketListener {

    private final ProtocolManager protocolManager;
    private final Configuration config;
    private final DatabaseManager databaseManager;
    private final PacketManager packetManager;
    private final MenuProvider menuProvider;
    private final SessionContainer sessions;
    private final PEconomyAPI api;
    private final Plugin plugin;
    
    public PacketListener(
            Configuration config,
            DatabaseManager databaseManager,
            PacketManager packetManager,
            MenuProvider menuProvider,
            SessionContainer sessions,
            PEconomyAPI api,
            Plugin plugin
    ) {
        protocolManager = ProtocolLibrary.getProtocolManager();
        this.config = config;
        this.databaseManager = databaseManager;
        this.packetManager = packetManager;
        this.menuProvider = menuProvider;
        this.sessions = sessions;
        this.api = api;
        this.plugin = plugin;
        
        initializeListeners();
    }
    
    private void initializeListeners() {
        protocolManager.addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY) {
            
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if(event.getPacketType() != PacketType.Play.Client.USE_ENTITY) return;
                
                Player player = event.getPlayer();
                ItemStack item = player.getInventory().getItemInMainHand();
                
                NamespacedKey key = new NamespacedKey(plugin, "type");
                
                if(item == null || !item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING) ) {
                    WrapperPlayClientUseEntity packet = new WrapperPlayClientUseEntity(event.getPacket());
                    EntityUseAction action = packet.getType();
                    
                    if(action != EntityUseAction.INTERACT_AT) return;
                    
                    int targetID = packet.getTargetID();
                    if(!databaseManager.hasSecret(targetID).join()) return;
                    
                    if(!sessions.hasSession(player)) return;
                    
                    Session session = sessions.getSession(player);
                    List<SecretModel> hidden = session.getHiddenSecrets();
                    
                    SecretModel secret = databaseManager.getSecret(targetID).join();
                    
                    int size = hidden.parallelStream()
                        .filter(h -> h.equals(secret))
                        .collect(Collectors.toList())
                        .size();
                    
                    if(size == 0) return;
                        
                    session.addToFound(secret);
                    CollectSecretEvent customEvent = new CollectSecretEvent(player, secret, session);
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(customEvent));
                    
                    
                    int[] array = {targetID};
                    packetManager.removeEntityPacket(player, array);
                    
                    if(session.getHiddenSecrets().size() == 0) {
                        String name = player.getName();
                        
                        ProfileModel profile = databaseManager.getProfile(name).join();
                        profile.setLastCompleteQuest(Calendar.getInstance());
                        profile.setLastQuestNumber(session.getQuestNumber());
                        
                        String currency = config.getString("currency");
                        WalletModel wallet = api.addAmount(name, currency, session.getReward());
                        api.updateWallet(wallet);
                        
                        databaseManager.saveProfile(profile);
                        sessions.closeSession(player);
                        return;
                    }
                    
                    return;
                }
                
                if(!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;
                
                WrapperPlayClientUseEntity packet = new WrapperPlayClientUseEntity(event.getPacket());
                EntityUseAction action = packet.getType();
                
                if(action == EntityUseAction.INTERACT_AT) {
                    int id = packet.getTargetID();
                    if(!databaseManager.hasSecret(id).join()) return;
                    
                    Bukkit.getScheduler().runTaskLater(plugin, () -> menuProvider.openDifficultyMenu(player, id), 0);
                    return;
                }
                
                if(action == EntityUseAction.ATTACK) {
                    int id = packet.getTargetID();
                    if(!databaseManager.hasSecret(id).join()) return;
                    
                    SecretModel secret = databaseManager.getSecret(id).join();
                    databaseManager.removeSecret(secret);
                    
                    packetManager.removeEntityPacket(player, new int[] {id});
                    return;
                }
                
            }
        });
    }
}
