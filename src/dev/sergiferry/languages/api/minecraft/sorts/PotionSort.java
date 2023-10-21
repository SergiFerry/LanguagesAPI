package dev.sergiferry.languages.api.minecraft.sorts;

/**
 * Creado por SergiFerry el 06/09/2022
 */
public enum PotionSort implements SortInterface{

    POTION("potion"),
    SPLASH_POTION("splash_potion"),
    LINGERING_POTION("lingering_potion"),
    ;

    private String key;

    PotionSort(String key) {
        this.key = key;
    }

    @Override
    public String getKey() { return key; }

    @Override
    public String toString() {
        return getKey();
    }
}