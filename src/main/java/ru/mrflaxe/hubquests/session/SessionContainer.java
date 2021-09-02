package ru.mrflaxe.hubquests.session;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.ProfileModel;
import ru.mrflaxe.hubquests.protocol.PacketManager;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.lib.configuration.Messages;

public class SessionContainer {

    private final Map<String, Session> sessions = new HashMap<>();
    
    private final DatabaseManager databaseManager;

    
    public SessionContainer(
            Configuration config,
            Messages messages,
            DatabaseManager databaseManager,
            PacketManager packetManager,
            Plugin plugin
    ) {
        this.databaseManager = databaseManager;
        runDailyReset(plugin, packetManager, messages, config);
    }
    

    public Session getSession(Player holder) {
        return sessions.get(holder.getName());
    }
    
    public Session openSession(Session session) {
        sessions.put(session.getHolderName(), session);
        return session;
    }
    
    public boolean hasSession(Player holder) {
        return sessions.containsKey(holder.getName());
    }
    
    public void closeSession(Player holder) {
        Session session = sessions.get(holder.getName());
        session.closeRotationTask();
        
        sessions.remove(holder.getName());
    }
    
    public void closeAllSessions(PacketManager packetManager) {
        if(sessions.isEmpty()) return;
        sessions.values().parallelStream()
            .forEach(Session::removePackets);
        
        sessions.clear();
    }
    
    private void runDailyReset(Plugin plugin, PacketManager packetManager, Messages messages, Configuration config) {
        Calendar calendar = Calendar.getInstance();
        int modifire = config.getInt("sessions-nullify-time");
        
        int hours = 24 - calendar.get(Calendar.HOUR_OF_DAY) + modifire;
        int minutes = 60 - calendar.get(Calendar.MINUTE);
        int seconds = 60 - calendar.get(Calendar.SECOND);
        
        long ticks = hours * 60 * 60 * 20 + minutes * 60 * 20 + seconds * 20;
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            sessions.keySet().parallelStream()
                .forEach(name -> {
                    ProfileModel profile = databaseManager.getProfile(name).join();
                    profile.setLastCompleteQuest(Calendar.getInstance());
                    profile.setLastQuestNumber(0);

                    databaseManager.saveProfile(profile);
                    
                    messages.getColoredList("quest-failed")
                        .forEach(message -> {
                            Bukkit.getOnlinePlayers().parallelStream()
                                .filter(this::hasSession)
                                .forEach(player -> {
                                    player.sendMessage(message);
                                    player.playSound(player.getLocation(), getSound(plugin, config), 0.5f, 1);
                                });
                        });
            });
            
            closeAllSessions(packetManager);
            
        }, ticks, 24 * 72000);
    }


    private Sound getSound(Plugin plugin, Configuration config) {
        String example = config.getString("sounds.quest-failed");
        Sound sound = Sound.valueOf(example);
        
        if(sound == null) {
            plugin.getLogger().warning("Failed to parse " + example + " into Sound object for section \"sounds.quest-failed\"");
            plugin.getLogger().warning("Will use default sound: BLOCK_BEACON_DEACTIVATE");
            sound = Sound.BLOCK_BEACON_DEACTIVATE;
        }
        
        return sound;
    }
}
