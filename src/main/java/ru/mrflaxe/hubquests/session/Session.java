package ru.mrflaxe.hubquests.session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import lombok.Getter;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.protocol.PacketManager;

@Getter
public class Session {

    private final PacketManager packetManager;
    
    private final String holderName;
    private final int questNumber;
    private final int reward;
    private final List<SecretModel> secrets;
    private final List<SecretModel> found;
    
    private int rotate;
    private BukkitTask rotationTask;
    
    public Session(String holderName, int questNumber, int reward, List<SecretModel> secrets, PacketManager packetManager) {
        this.packetManager = packetManager;
        
        this.holderName = holderName;
        this.questNumber = questNumber;
        this.reward = reward;
        this.secrets = secrets;
        this.found = new ArrayList<>();
        
        rotate = 0;
    }
    
    public void sendPackets(Plugin plugin) {
        getHiddenSecrets().parallelStream()
            .forEach(secret -> {
                Location loc = secret.getLocation();
                int entityID = secret.getId();
                packetManager.sendSessionPacket(Bukkit.getPlayer(holderName), loc, entityID);
            });
        runRotation(plugin);
    }
    
    public void removePackets() {
        getHiddenSecrets().parallelStream()
            .forEach(secret -> {
                Player player = Bukkit.getPlayer(holderName);
                int[] array = {secret.getId()};
                closeRotationTask();
                packetManager.removeEntityPacket(player, array);
            });
    }
    
    public List<SecretModel> getHiddenSecrets() {
        return secrets.parallelStream()
                .filter(secret -> !found.contains(secret))
                .collect(Collectors.toList());
    }
    
    public void addToFound(SecretModel secret) {
        found.add(secret);
    }
    
    private void runRotation(Plugin plugin) {
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            rotate = rotate + 2;
            if(rotate >= 359) rotate = 0;
            int[] ids = getHiddenSecrets().parallelStream().mapToInt(SecretModel::getId).toArray();
            packetManager.sendRotationPacket(Bukkit.getPlayer(holderName), ids, rotate);
        }, 0, 1);
    }
    
    public void closeRotationTask() {
        rotationTask.cancel();
    }
}
