package dev.sergiferry.languages.integration;

import dev.sergiferry.languages.LanguagesPlugin;
import dev.sergiferry.languages.integration.integrations.PlaceholderAPI;
import dev.sergiferry.languages.integration.integrations.ProtocolLib;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Creado por SergiFerry el 20/08/2022
 */
public class IntegrationsManager {

    private static List<PluginIntegration> hookedPlugins;

    private static PlaceholderAPI placeholderAPI;
    private static ProtocolLib protocolLib;

    public static void onEnable(){
        check();
        load();
    }

    public static void onDisable(){
        unload();
    }

    private static void check(){
        hookedPlugins = new ArrayList<>();
        getPlugin().getServer().getConsoleSender().sendMessage(getPlugin().getPrefix() + "§7Looking for soft dependencies...");
        //
        checkPlaceHolderAPI();
        checkProtocolLib();
        //
        if(hookedPlugins.isEmpty()) getPlugin().getServer().getConsoleSender().sendMessage(getPlugin().getPrefix() + "§7No soft dependencies were found.");
    }

    private static void load(){
        hookedPlugins.forEach(x-> x.load());
    }

    private static void unload(){
        hookedPlugins.forEach(x-> x.unload());
    }

    private static void checkPlaceHolderAPI(){
        Plugin placeholderAPIPlugin = getPlugin().getServer().getPluginManager().getPlugin("PlaceholderAPI");
        if(placeholderAPIPlugin == null || !placeholderAPIPlugin.isEnabled()) return;
        placeholderAPI = new PlaceholderAPI(placeholderAPIPlugin);
    }

    private static void checkProtocolLib(){
        Plugin protocolLibPlugin = getPlugin().getServer().getPluginManager().getPlugin("ProtocolLib");
        if(protocolLibPlugin == null || !protocolLibPlugin.isEnabled()) return;
        protocolLib = new ProtocolLib(protocolLibPlugin);
    }

    public static PlaceholderAPI getPlaceholderAPI() {
        return placeholderAPI;
    }

    public static boolean isUsingPlaceholderAPI(){
        return placeholderAPI != null;
    }

    public static ProtocolLib getProtocolLib(){ return protocolLib; }

    public static boolean isUsingProtocolLib(){ return protocolLib != null; }

    public static LanguagesPlugin getPlugin(){
        return LanguagesPlugin.getInstance();
    }

    protected static void addHookedPlugin(PluginIntegration pluginIntegration){
        hookedPlugins.add(pluginIntegration);
        getPlugin().getServer().getConsoleSender().sendMessage(getPlugin().getPrefix() + "§7Hooked into §a" + pluginIntegration.getPlugin().getName() + " §7v" + pluginIntegration.getPlugin().getDescription().getVersion() + ".");
    }

    protected static void removeHookedPlugin(PluginIntegration pluginIntegration){
        if(!hookedPlugins.contains(pluginIntegration)) return;
        hookedPlugins.remove(pluginIntegration);
        getPlugin().getServer().getConsoleSender().sendMessage(getPlugin().getPrefix() + "§7Removed integration from §c" + pluginIntegration.getPlugin().getName());
    }

    public static List<PluginIntegration> getHookedPlugins() {
        return hookedPlugins;
    }
}
