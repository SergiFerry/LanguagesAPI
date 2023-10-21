package dev.sergiferry.languages.api.events.player;

import dev.sergiferry.languages.api.Language;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nonnull;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class PlayerLanguageLoadEvent extends PlayerEvent {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Nonnull private Language language;
    @Nonnull private Type type;

    public PlayerLanguageLoadEvent(@Nonnull Player who, @Nonnull Language language, @Nonnull Type type) {
        super(who);
        Validate.notNull(who); Validate.notNull(language); Validate.notNull(type);
        this.language = language;
        this.type = type;
        Bukkit.getPluginManager().callEvent(this);
        new PlayerLanguageDisplayEvent(who, language, this);
    }

    @Nonnull public Language getLanguage() { return language; }

    @Nonnull public Type getLoadedFrom() { return type; }

    @Override public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public enum Type { CLIENT_LANGUAGE, SELECTED_LANGUAGE, SERVER_DEFAULT }

    public static HandlerList getHandlerList(){
        return HANDLERS_LIST;
    }
}
