package ru.mrflaxe.hubquests.listeners;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.events.CollectSecretEvent;
import ru.mrflaxe.hubquests.session.Session;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.lib.configuration.Messages;

public class CustomEventsListener implements Listener {

    private final Messages messages;
    private final Configuration config;
    private final Plugin plugin;
    
    public CustomEventsListener(Messages messages, Configuration config, Plugin plugin) {
        this.messages = messages;
        this.config = config;
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onSecretCollect(CollectSecretEvent event) {
        Player player = event.getPlayer();
        SecretModel secret = event.getSecret();
        Session session = event.getSession();
        
        int collected = session.getFound().size();
        int remained = session.getHiddenSecrets().size();

        if(remained == 0) {
            List<String> congratMessage = messages.getColoredList("collect.all");
            congratMessage = messages.formatList(congratMessage, "%found%", collected, "%total%", collected, "%reward%", session.getReward());
            
            congratMessage.forEach(message -> player.sendMessage(message));
            player.playSound(player.getLocation(), getCollectAllSound(plugin, config), 1, 1);
            return;
        }
        
        List<String> collectMessage = messages.getColoredList("collect.piece");
        collectMessage = messages.formatList(collectMessage, "%found%", collected, "%total%", collected + remained);
        
        collectMessage.forEach(message -> player.sendMessage(message));
        player.playSound(secret.getLocation(), getCollectSound(plugin, config), 1, 1);
    }
    
    private Sound getCollectSound(Plugin plugin, Configuration config) {
        String example = config.getString("sounds.secret-collect");
        Sound sound = Sound.valueOf(example);
        
        if(sound == null) {
            plugin.getLogger().warning("Failed to parse " + example + " into Sound object for section \"sounds.secret-collect\"");
            plugin.getLogger().warning("Will use default sound: ENTITY_EXPERIENCE_ORB_PICKUP");
            sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
        
        return sound;
    }
    
    private Sound getCollectAllSound(Plugin plugin, Configuration config) {
        String example = config.getString("sounds.secret-collect-all");
        Sound sound = Sound.valueOf(example);
        
        if(sound == null) {
            plugin.getLogger().warning("Failed to parse " + example + " into Sound object for section \"sounds.secret-collect-all\"");
            plugin.getLogger().warning("Will use default sound: ENTITY_PLAYER_LEVELUP");
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        }
        
        return sound;
    }
    
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}
