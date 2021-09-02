package ru.mrflaxe.hubquests.database.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@DatabaseTable (tableName = "secrets")
public class SecretModel {

    @DatabaseField (id = true)
    private int id;
    @DatabaseField
    private double x;
    @DatabaseField
    private double y;
    @DatabaseField
    private double z;
    @DatabaseField
    private String world;
    @DatabaseField
    private int difficulty;
    
    public SecretModel(int id, Location loc, int difficulty) {
        this.id = id;
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.world = loc.getWorld().getName();
        this.difficulty = difficulty;
    }
    
    public Location getLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }
    
    public void setLocation(Location loc) {
        this.x = loc.getX();
        this.y = loc.getY();
        this.z = loc.getZ();
        this.world = loc.getWorld().toString();
    }
}
