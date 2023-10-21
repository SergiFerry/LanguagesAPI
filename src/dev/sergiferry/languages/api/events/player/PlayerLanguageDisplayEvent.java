package dev.sergiferry.languages.api.events.player;

import dev.sergiferry.languages.api.Language;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class PlayerLanguageDisplayEvent extends PlayerEvent {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Nonnull private Language language;
    @Nullable private PlayerEvent causeEvent;

    public PlayerLanguageDisplayEvent(Player who, Language language, @Nullable PlayerEvent causeEvent) {
        super(who);
        Validate.notNull(who); Validate.notNull(language);
        this.language = language;
        this.causeEvent = causeEvent;
        Bukkit.getPluginManager().callEvent(this);
    }

    @Nonnull public Language getLanguage() { return this.language; }

    public Optional<Language> getPreviousLanguage(){
        return switch (getCause()){
            case LOAD -> Optional.of(Language.en_US);
            case CHANGE -> ((PlayerLanguageChangeEvent) causeEvent).getFrom();
            case REFRESH -> Optional.of(getLanguage());
        };
    }

    public Cause getCause() {
        if(causeEvent != null){
            if(causeEvent instanceof PlayerLanguageChangeEvent) return Cause.CHANGE;
            else if(causeEvent instanceof PlayerLanguageLoadEvent) return Cause.LOAD;
        }
        return Cause.REFRESH;
    }

    public enum Cause{ LOAD, CHANGE, REFRESH }

    @Override
    public HandlerList getHandlers() { return HANDLERS_LIST; }

    public static HandlerList getHandlerList(){ return HANDLERS_LIST; }
}
