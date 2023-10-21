package dev.sergiferry.languages.api.minecraft.sorts;

/**
 * Creado por SergiFerry el 06/09/2022
 */
public enum FilledMap implements SortInterface{

    BURIED_TREASURE("buried_treasure"),
    ID("id"),
    LEVEL("level"),
    LOCKED("locked"),
    MANSION("mansion"),
    MONUMENT("monument"),
    SCALE("scale"),
    UNKNOWN("unknown")
    ;

    private String key;

    FilledMap(String key){ this.key = key; }

    @Override
    public String getKey() { return this.key; }

}
