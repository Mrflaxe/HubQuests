package ru.mrflaxe.hubquests.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import lombok.Getter;

@Getter
public class DifficultyChooserHolder implements InventoryHolder {

    private final int secretID;
    
    public DifficultyChooserHolder(int secretID) {
        this.secretID = secretID;
    }
    
    @Override
    public Inventory getInventory() {
        return null;
    }

}
