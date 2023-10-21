package dev.sergiferry.languages.integration.integrations;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import dev.sergiferry.languages.LanguagesPlugin;
import dev.sergiferry.languages.debug.DebugManager;
import dev.sergiferry.languages.integration.PluginIntegration;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

/**
 * Creado por SergiFerry el 18/10/2023
 */
public class ProtocolLib extends PluginIntegration {

    private PacketAdapter listenSettingsPacket;

    public ProtocolLib(Plugin plugin) {
        super(plugin);
        this.listenSettingsPacket = new PacketAdapter(LanguagesPlugin.getInstance(), PacketType.Play.Client.SETTINGS) {
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                PacketContainer packet = event.getPacket();
                String locale = packet.getStrings().read(0);
                if(!LanguagesPlugin.getInstance().getLanguagesAPIInstance().isStillLoadingLanguage(event.getPlayer())) return;
                DebugManager.debug(event.getPlayer(), "Your received protocol locale is: " + locale);
                Bukkit.getScheduler().scheduleSyncDelayedTask(LanguagesPlugin.getInstance(), () -> new PlayerLocaleJoinEvent(event.getPlayer(), locale));
            }
        };
    }

    @Override
    public void load() {
        ProtocolLibrary.getProtocolManager().addPacketListener(listenSettingsPacket);
    }

    @Override
    public void unload() {
        ProtocolLibrary.getProtocolManager().removePacketListener(listenSettingsPacket);
    }

    public class PlayerLocaleJoinEvent extends PlayerEvent {

        private static final HandlerList HANDLERS_LIST = new HandlerList();

        @Nonnull
        private String locale;

        public PlayerLocaleJoinEvent(@Nonnull Player who, @Nonnull String locale) {
            super(who);
            Validate.notNull(who); Validate.notNull(locale);
            this.locale = locale;
            Bukkit.getPluginManager().callEvent(this);
        }

        @Nonnull public String getLocale() { return locale; }

        @Override public HandlerList getHandlers() { return HANDLERS_LIST; }

        public static HandlerList getHandlerList(){
            return HANDLERS_LIST;
        }
    }
}
