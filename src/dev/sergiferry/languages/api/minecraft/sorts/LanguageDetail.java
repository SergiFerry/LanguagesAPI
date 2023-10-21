package dev.sergiferry.languages.api.minecraft.sorts;

/**
 * Creado por SergiFerry el 07/09/2022
 */
public enum LanguageDetail implements SortInterface{

    CODE("code"),
    NAME("name"),
    REGION("region"),
    ;

    private String key;

    LanguageDetail(String key) {
        this.key = key;
    }

    @Override
    public String getKey() { return key; }

    @Override public String toString() { return getKey(); }

}
