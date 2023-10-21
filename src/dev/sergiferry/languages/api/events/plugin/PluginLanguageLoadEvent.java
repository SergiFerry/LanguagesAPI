package dev.sergiferry.languages.api.events.plugin;

import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class PluginLanguageLoadEvent extends PluginManagerEvent {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Nonnull private Language language;
    @Nonnull private Cause cause;

    public PluginLanguageLoadEvent(@Nonnull LanguagesAPI.PluginManager pluginManager, @Nonnull Language language, @Nonnull Cause cause) {
        super(pluginManager);
        Validate.notNull(language); Validate.notNull(cause);
        this.language = language;
        this.cause = cause;
        Bukkit.getPluginManager().callEvent(this);
    }

    @Nonnull public Language getLanguage() { return language; }

    @Nonnull public Cause getCause() { return cause; }

    @Override public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public enum Cause { SERVER_LOAD, COMMAND_RELOAD }

    public static HandlerList getHandlerList(){
        return HANDLERS_LIST;
    }
}
