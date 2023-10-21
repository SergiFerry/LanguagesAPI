package dev.sergiferry.languages.integration;

import org.bukkit.plugin.Plugin;

/**
 * Creado por SergiFerry el 20/08/2022
 */
public abstract class PluginIntegration {

    private Plugin plugin;

    public PluginIntegration(Plugin plugin){
        this.plugin = plugin;
        addIntegration();
    }

    protected abstract void load();

    protected abstract void unload();

    public void addIntegration(){
        IntegrationsManager.addHookedPlugin(this);
    }

    public void removeIntegration(){
        IntegrationsManager.removeHookedPlugin(this);
    }

    public Plugin getPlugin() { return plugin; }
}
