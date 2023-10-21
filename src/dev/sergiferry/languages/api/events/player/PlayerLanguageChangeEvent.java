package dev.sergiferry.languages.api.events.player;

import dev.sergiferry.languages.LanguagesPlugin;
import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
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
public class PlayerLanguageChangeEvent extends PlayerEvent {

    private static final HandlerList HANDLERS_LIST = new HandlerList();

    @Nullable private Language from;
    @Nonnull private Language to;

    public PlayerLanguageChangeEvent(@Nonnull Player who, @Nullable Language from, @Nonnull Language to, @Nonnull Cause cause) {
        super(who);
        Validate.notNull(who); Validate.notNull(to); Validate.notNull(cause);
        this.from = from; this.to = to;
        Bukkit.getPluginManager().callEvent(this);
        if(LanguagesAPI.isAnnouncePlayerLanguageChange() || cause.equals(Cause.SERVER)){
            boolean similarTo = to != null && to.getSimilarLanguages().stream().filter(x-> LanguagesAPI.getAvailableLanguages().contains(x)).findFirst().isPresent();
            who.sendMessage(LanguagesPlugin.getPluginManager().getTranslationString("player.language_change", to)
                    .formatted((to.isAvailable() ? "§a" : (similarTo ? "§6" : "§c")) + to.getNameAndRegion() + (who.isOp() ? " §7[" + to.getLocaleCode() + "]" : ""))
            );
        }
        new PlayerLanguageDisplayEvent(who, to, this);
    }

    public Optional<Language> getFrom() { return Optional.ofNullable(from); }

    @Nonnull public Language getTo() { return to; }

    @Override public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public enum Cause{ CLIENT, SERVER }

    public static HandlerList getHandlerList(){
        return HANDLERS_LIST;
    }
}
