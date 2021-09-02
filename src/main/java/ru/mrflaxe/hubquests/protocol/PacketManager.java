package ru.mrflaxe.hubquests.protocol;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Serializer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import ru.mrflaxe.hubquests.database.DatabaseManager;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.protocol.wrappers.WrapperPlayServerEntityDestroy;
import ru.mrflaxe.hubquests.protocol.wrappers.WrapperPlayServerEntityEquipment;
import ru.mrflaxe.hubquests.protocol.wrappers.WrapperPlayServerEntityLook;
import ru.mrflaxe.hubquests.protocol.wrappers.WrapperPlayServerEntityMetadata;
import ru.mrflaxe.hubquests.protocol.wrappers.WrapperPlayServerSpawnEntity;
import ru.soknight.lib.configuration.Configuration;
import ru.soknight.lib.configuration.Messages;

public class PacketManager {

    private final DatabaseManager databaseManager;
    private final Messages messages;
    private final Configuration config;
    
    public PacketManager(DatabaseManager databaseManager, Messages messages, Configuration config) {
        this.databaseManager = databaseManager;
        this.messages = messages;
        this.config = config;
    }

    private void sendPacket(Player player, Location loc, int entityID) {
        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity();
        
        spawnPacket.setEntityID(entityID);
        spawnPacket.setType(EntityType.ARMOR_STAND);
        spawnPacket.setX(loc.getX());
        spawnPacket.setY(loc.getY());
        spawnPacket.setZ(loc.getZ());
        
        spawnPacket.sendPacket(player);
        
        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata();
        metadataPacket.setEntityID(entityID);
        metadataPacket.setMetadata(getSessionMetadata());
        metadataPacket.sendPacket(player);
        
        String material = config.getString("secret.material");
        Material type = Material.getMaterial(material);
        
        // Config setting of armorstand head material
        ItemStack head;
        if(type == null) head = new ItemStack(Material.GOLD_BLOCK);
        else head = new ItemStack(type);
        
        WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment();
        equipmentPacket.setEntityID(entityID);
        equipmentPacket.setItem(head);
        equipmentPacket.setSlot(ItemSlot.HEAD);
        
        equipmentPacket.sendPacket(player);
        
    }
    
    public void sendCustomizerPacket(Player player, Location loc, int entityID) {
        sendPacket(player, loc, entityID);

        WrapperPlayServerEntityMetadata metadataPacket = new WrapperPlayServerEntityMetadata();
        metadataPacket.setEntityID(entityID);

        String text = messages.getFormatted("secrets.display-name", "%difficulty%", getDifficulty(entityID));
        
        metadataPacket.setMetadata(getCustomizerMetadata(text));
        metadataPacket.sendPacket(player);
    }
    
    private String getDifficulty(int entityID) {
        SecretModel secret = databaseManager.getSecret(entityID).join();
        int difficulty = secret.getDifficulty();
        
        switch (difficulty) {
        case 1:
            return messages.getColoredString("secrets.difficulties.easy");
        case 2:
            return messages.getColoredString("secrets.difficulties.medium");
        case 3:
            return messages.getColoredString("secrets.difficulties.hard");
        }
        
        return null;
    }

    public void sendSessionPacket(Player player, Location loc, int entityID) {
        sendPacket(player, loc, entityID);
        
        
    }
    
    public void sendEntityPackets(Player reciever, int[] entityIds) {
        Arrays.stream(entityIds).forEach(id -> {
                SecretModel secret = databaseManager.getSecret(id).join();
                sendCustomizerPacket(reciever, secret.getLocation(), id);
            });
    }
    
    public void removeEntityPacket(Player reciever, int[] entityIds) {
        WrapperPlayServerEntityDestroy packet = new WrapperPlayServerEntityDestroy();
        packet.setEntityIds(entityIds);
        
        packet.sendPacket(reciever);
    }
    
    public void sendRotationPacket(Player reciever, int[] EntityIds, int rotation) {
        Arrays.stream(EntityIds).forEach(id -> {
            WrapperPlayServerEntityLook rotationPacket = new WrapperPlayServerEntityLook();
            rotationPacket.setEntityID(id);
            rotationPacket.setYaw(rotation);

            rotationPacket.sendPacket(reciever);
        });
        
        
    }
    
    private List<WrappedWatchableObject> getCustomizerMetadata(String message) {
        return Arrays.asList(
                createWatchableObject(Byte.class, 0, (byte) 0x20), // entity bitmask | set invisible
                createOptionalChat(2, message), // custom name
                createWatchableObject(Boolean.class, 3, true) // custom name visible
        );
    }
    
    private List<WrappedWatchableObject> getSessionMetadata() {
        return Arrays.asList(
                createWatchableObject(Byte.class, 0, (byte) 0x20) // entity bitmask | set invisible
        );
    }

    private static WrappedWatchableObject createWatchableObject(Class<?> clazz, int index, Object value) {
        return new WrappedWatchableObject(new WrappedDataWatcherObject(index, Registry.get(clazz)), value);
    }

    private static WrappedWatchableObject createOptionalChat(int index, String text) {
        Serializer serializer = Registry.getChatComponentSerializer(true);

        BaseComponent[] customName = TextComponent.fromLegacyText(text);
        String json = ComponentSerializer.toString(new TextComponent(customName));

        WrappedDataWatcherObject watcherObject = new WrappedDataWatcherObject(index, serializer);

        return new WrappedWatchableObject(
                watcherObject,
                Optional.of(WrappedChatComponent.fromJson(json).getHandle())
        );
    }
}
