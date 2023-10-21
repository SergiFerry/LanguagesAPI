package dev.sergiferry.languages.api.events.plugin;

import dev.sergiferry.languages.api.LanguagesAPI;
import org.apache.commons.lang.Validate;
import org.bukkit.event.server.PluginEvent;

import javax.annotation.Nonnull;

/**
 * Creado por SergiFerry el 08/10/2023
 */
public abstract class PluginManagerEvent extends PluginEvent {

    @Nonnull private final LanguagesAPI.PluginManager pluginManager;

    public PluginManagerEvent(@Nonnull LanguagesAPI.PluginManager pluginManager) {
        super(pluginManager.getPlugin());
        Validate.notNull(pluginManager);
        this.pluginManager = pluginManager;
    }

    @Nonnull public LanguagesAPI.PluginManager getPluginManager() {
        return this.pluginManager;
    }
}
