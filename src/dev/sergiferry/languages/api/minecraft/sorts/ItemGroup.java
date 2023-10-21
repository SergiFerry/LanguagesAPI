package dev.sergiferry.languages.api.minecraft.sorts;

/**
 * Creado por SergiFerry el 06/09/2022
 */
public enum ItemGroup implements SortInterface{

    BUILDING_BLOCKS("buildingBlocks"),
    COLORED_BLOCKS("coloredBlocks"),
    COMBAT("combat"),
    CONSUMABLES("consumables"),
    CRAFTING("crafting"),
    FOOD_AND_DRINK("foodAndDrink"),
    FUNCTIONAL("functional"),
    HOTBAR("hotbar"),
    INGREDIENTS("ingredients"),
    INVENTORY("inventory"),
    NATURAL("natural"),
    OP("op"),
    REDSTONE("redstone"),
    SEARCH("search"),
    SPAWN_EGGS("spawnEggs"),
    TOOLS("tools"),
    ;

    private String key;

    ItemGroup(String key){ this.key = key; }

    @Override
    public String getKey() { return this.key; }

}
