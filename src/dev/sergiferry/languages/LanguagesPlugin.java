package dev.sergiferry.languages;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.sergiferry.languages.api.LanguagesAPI;
import dev.sergiferry.languages.commands.LangAPICommand;
import dev.sergiferry.languages.commands.LangCommand;
import dev.sergiferry.languages.debug.DebugManager;
import dev.sergiferry.languages.integration.IntegrationsManager;
import dev.sergiferry.spigot.SpigotPlugin;
import dev.sergiferry.spigot.server.ServerVersion;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class LanguagesPlugin extends SpigotPlugin implements Listener {

    /* TODO
    - Countries
    - Flags or heads
    - LangAPI command (load, save, info)
    - Lang command (set player custom language)
    - Signs (create through text or command), and update automatically to players.
    - Save custom language per player on local or mysql
     */

    private static LanguagesPlugin instance;

    public LanguagesPlugin() {
        super(0,
                ServerVersion.VERSION_1_17,
                ServerVersion.VERSION_1_17_1,
                ServerVersion.VERSION_1_18,
                ServerVersion.VERSION_1_18_1,
                ServerVersion.VERSION_1_18_2,
                ServerVersion.VERSION_1_19,
                ServerVersion.VERSION_1_19_1,
                ServerVersion.VERSION_1_19_2,
                ServerVersion.VERSION_1_19_3,
                ServerVersion.VERSION_1_19_4,
                ServerVersion.VERSION_1_20,
                ServerVersion.VERSION_1_20_1,
                ServerVersion.VERSION_1_20_2
        );
        instance = this;
    }

    public static LanguagesPlugin getInstance() {
        return instance;
    }

    @Override
    public void enable() {
        setPrefix("§b§lLanguagesAPI §8| §7");
        setupMetrics(19827);
        IntegrationsManager.onEnable();
        getLanguagesAPIInstance().enable(this);
        new LangAPICommand(this);
        new LangCommand(this);
        //
        LanguagesAPI.PluginManager pluginManager = LanguagesAPI.getPluginManager(this);
        Bukkit.getConsoleSender().sendMessage(pluginManager.getStringTranslation("console.enable.server_language").getInServerDefaultLanguage().formatted(LanguagesAPI.getServerLanguage().getConsoleName()));
        Bukkit.getConsoleSender().sendMessage(pluginManager.getStringTranslation("console.enable.languages_count").getInServerDefaultLanguage().formatted(pluginManager.getTranslatedLanguages().size()));
    }

    @Override
    public void disable() {
        getLanguagesAPIInstance().disable(this);
        IntegrationsManager.onDisable();
    }

    public static LanguagesAPI.PluginManager getPluginManager(){
         return LanguagesAPI.getPluginManager(getInstance());
    }

    public LanguagesAPI getLanguagesAPIInstance(){
        try{
            Field field = LanguagesAPI.class.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            LanguagesAPI instance = (LanguagesAPI) field.get(null);
            Validate.notNull(instance, "LanguagesAPI instance is not initialized yet.");
            field.setAccessible(false);
            return instance;
        }
        catch (Exception e){ e.printStackTrace(); return null; }
    }

}
