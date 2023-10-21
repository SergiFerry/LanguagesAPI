package dev.sergiferry.languages.api.minecraft.sorts;

/**
 * Creado por SergiFerry el 06/09/2022
 */
public enum DeathScreen implements SortInterface{

    QUIT_CONFIRM("quit.confirm"),
    RESPAWN("respawn"),
    SCORE("score"),
    SPECTATE("spectate"),
    YOU_DIED("title"),
    GAME_OVER("title.hardcore"),
    TITLE_SCREEN("titleScreen"),
    ;

    private String key;

    DeathScreen(String key){ this.key = key; }

    @Override
    public String getKey() { return this.key; }

}
