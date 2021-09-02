package ru.mrflaxe.hubquests.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import lombok.Getter;
import ru.mrflaxe.hubquests.database.model.SecretModel;
import ru.mrflaxe.hubquests.session.Session;

@Getter
public class CollectSecretEvent extends Event {

    private final Player player;
    private final SecretModel secret;
    private final Session session;
    
    private static final HandlerList handlers = new HandlerList();
    
    public CollectSecretEvent(Player player, SecretModel secret, Session session) {
        this.player = player;
        this.secret = secret;
        this.session = session;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }

}
