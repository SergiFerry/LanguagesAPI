package dev.sergiferry.languages.commands;

import dev.sergiferry.languages.LanguagesPlugin;
import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
import dev.sergiferry.spigot.SpigotPlugin;
import dev.sergiferry.spigot.commands.CommandInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class LangCommand extends CommandInstance implements TabCompleter{

    private static String COMMAND_LABEL = "lang";

    public LangCommand(SpigotPlugin plugin) {
        super(plugin, COMMAND_LABEL);
        setAliases("language");
        setTabCompleter(this);
    }

    @Override
    public void onExecute(CommandSender sender, Command command, String label, String[] args) {
        final Player playerSender = (sender instanceof Player) ? (Player) sender : null;
        LanguagesAPI.PluginManager pluginManager1 = LanguagesAPI.getPluginManager(LanguagesPlugin.getInstance());
        Language displayLanguage = LanguagesAPI.getLanguage(sender);
        if (playerSender == null) {
            sender.sendMessage("This command can only be executed by a player.");
            return;
        }
        if(args.length > 0 && LanguagesAPI.isAllowPlayerSelectLanguage()){
            String lang = args[0];
            if(lang.equalsIgnoreCase("auto")){
                LanguagesAPI.resetLanguage(playerSender);
                return;
            }
            Language language = Language.grabLanguage(lang).orElse(null);
            if(language == null){
                sender.sendMessage(pluginManager1.getTranslationString("command.lang_api.error.not-recognized.language", displayLanguage));
                return;
            }
            if(!LanguagesAPI.getAvailableLanguages().contains(language)){
                sender.sendMessage(pluginManager1.getTranslationString("command.lang_api.error.language_not_supported.server", displayLanguage));
                return;
            }
            LanguagesAPI.setLanguage(playerSender, language);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder("");
        for(Language language : LanguagesAPI.getAvailableLanguages()) stringBuilder.append("§7, " + (displayLanguage.equals(language) ? "§a" : displayLanguage.getSimilarLanguages().contains(language) ? "§6§o": "§7") + language.getNameAndRegion());
        stringBuilder.replace(0, 4, "");
        boolean similar = displayLanguage.getSimilarLanguages().stream().filter(x-> LanguagesAPI.getAvailableLanguages().contains(x)).findFirst().isPresent();
        playerSender.sendMessage(pluginManager1.getTranslationString("command.lang.general.your_language", displayLanguage)
                .formatted(displayLanguage != null ? (LanguagesAPI.getAvailableLanguages().contains(displayLanguage) ? "§a" : (similar ? "§6" : "§c")) + displayLanguage.getNameAndRegion() + (playerSender.isOp() ? " §7[" + displayLanguage.getLocaleCode() + "]" : "") + (LanguagesAPI.getAvailableLanguages().contains(displayLanguage) ? "" : " - " + (similar ? pluginManager1.getTranslationString("command.lang.tags.supported_by_similar", playerSender) : pluginManager1.getTranslationString("command.lang.tags.not_supported", playerSender))) : "§c" + playerSender.getLocale()));
        playerSender.sendMessage(pluginManager1.getTranslationString("command.lang.general.server_language", displayLanguage)
                .formatted(displayLanguage.equals(LanguagesAPI.getServerLanguage()) ? "§a" : "§7") + LanguagesAPI.getServerLanguage().getNameAndRegion() + (playerSender.isOp() ? " §7[" + LanguagesAPI.getServerLanguage().getLocaleCode() + "]" : ""));
        playerSender.sendMessage(pluginManager1.getTranslationString("command.lang.general.supported_languages", displayLanguage)
                .formatted ((LanguagesAPI.getAvailableLanguages().size() > 1 ? "\n" : "") + stringBuilder));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@Nonnull CommandSender commandSender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if(!isCommand(label)) return null;
        List<String> suggested = new ArrayList<>();
        if(!(commandSender instanceof Player)) return suggested;
        Player playerSender = (Player) commandSender;
        if(args.length == 1 && LanguagesAPI.isAllowPlayerSelectLanguage()){
            Language actual = LanguagesAPI.getSelectedLanguage(playerSender).orElse(null);
            LanguagesAPI.getAvailableLanguages().stream().filter(x-> x.getLocaleCode().toLowerCase().startsWith(args[0].toLowerCase())).forEach(x-> suggested.add((actual != null && actual.equals(x) ? "§a" : "") + x.getLocaleCode().toLowerCase()));
            suggested.add((actual == null ? "§a" : "") + "auto");
        }
        return suggested;
    }

}
