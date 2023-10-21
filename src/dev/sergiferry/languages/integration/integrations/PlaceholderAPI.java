package dev.sergiferry.languages.integration.integrations;

import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
import dev.sergiferry.languages.api.minecraft.MinecraftTranslation;
import dev.sergiferry.languages.integration.IntegrationsManager;
import dev.sergiferry.languages.integration.PluginIntegration;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creado por SergiFerry el 20/08/2022
 */
public class PlaceholderAPI extends PluginIntegration {

    private Extension extension;

    public PlaceholderAPI(Plugin plugin) {
        super(plugin);
        this.extension = new Extension(IntegrationsManager.getPlugin());
    }

    @Override
    public void load() {
        extension.register();
    }

    @Override
    public void unload() {
        extension.unregister();
    }

    public String replace(Player player, String text){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

    public List<String> replace(Player player, List<String> text){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

    public String replace(OfflinePlayer player, String text){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

    public List<String> replace(OfflinePlayer player, List<String> text){
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

    public static class Extension extends PlaceholderExpansion implements Relational {

        private Plugin plugin;

        public Extension(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() {
            return plugin.getName().toLowerCase();
        }

        @Override
        public String getAuthor() {
            return plugin.getDescription().getAuthors().get(0);
        }

        @Override
        public String getVersion() {
            return plugin.getDescription().getVersion();
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public boolean canRegister() {
            return true;
        }

        public String onPlaceholderRequest(Player player, String identifier) {
            identifier = identifier.toLowerCase();
            if (identifier.equals("author")) { return getAuthor(); }
            else if (identifier.equals("version")) { return getVersion(); }
            else if (identifier.startsWith("language_")){
                Language language = LanguagesAPI.getLanguage(player);
                if(identifier.endsWith("_code")) return language.getLocaleCode();
                else if(identifier.endsWith("_name")) return language.getName();
                else if(identifier.endsWith("_englishName")) return language.getEnglishName();
            }
            else if (identifier.startsWith("translation_")){
                String messageID = identifier.replaceFirst("translation_", "");
                if(!messageID.contains("_")) return "Error: Usage %languagesapi_translation_plugin_code[locale]%";
                String[] codeArray = messageID.split("_", 2);
                String pluginName = codeArray[0];
                String code = codeArray[1];
                if(!LanguagesAPI.getPluginNames().contains(pluginName.toLowerCase())) return "Error: " + pluginName + " is not using LanguagesAPI";
                LanguagesAPI.PluginManager pluginManager = LanguagesAPI.getPluginManager(pluginName);
                Language language = LanguagesAPI.getServerLanguage();
                if(player != null) language = LanguagesAPI.getLanguage(player);
                if(code.contains("[") && code.contains("]")){
                    Matcher matcher = Pattern.compile("[\\[](.*?)]").matcher(code);
                    if(matcher.find()){
                        String key = matcher.group(1);
                        Language lang = Language.grabLanguage(key).orElse(null);
                        if(lang != null){
                            language = lang;
                            code = code.replaceAll("\\[" + key + "]", "");
                        }
                    }
                }
                if(!pluginManager.hasTranslation(code)) return "Error: " + code + " translation doesn't exists";
                return pluginManager.getTranslationString(code, language);
            }
            else if (identifier.startsWith("minecraft_")){
                String messageID = identifier.replaceFirst("minecraft_", "");
                if(messageID.length() == 0) return "Error: Usage %languagesapi_minecraft_key[locale]%";
                Language language = LanguagesAPI.getServerLanguage();
                if(player != null) language = LanguagesAPI.getLanguage(player);
                if(messageID.contains("[") && messageID.contains("]")){
                    Matcher matcher = Pattern.compile("[\\[](.*?)]").matcher(messageID);
                    if(matcher.find()){
                        String key = matcher.group(1);
                        Language lang = Language.grabLanguage(key).orElse(null);
                        if(lang != null){
                            language = lang;
                            messageID = messageID.replaceAll("\\[" + key + "]", "");
                        }
                    }
                }
                return MinecraftTranslation.getTranslation(language, messageID);
            }
            return null;
        }

        public String onPlaceholderRequest(Player playerOne, Player playerTwo, String identifier) {
            return onPlaceholderRequest(playerOne, identifier);
        }

    }
}
