package ru.mrflaxe.hubquests.database.model;

import java.util.Calendar;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
@DatabaseTable (tableName = "players")
public class ProfileModel {

    @DatabaseField (id = true)
    private String name;
    @DatabaseField (dataType = DataType.SERIALIZABLE)
    private Calendar lastCompleteQuest;
    @DatabaseField
    private int lastQuestNumber;
    
    public ProfileModel(String name) {
        this.name = name;
        this.lastCompleteQuest = Calendar.getInstance();
        this.lastQuestNumber = 0;
    }
}
