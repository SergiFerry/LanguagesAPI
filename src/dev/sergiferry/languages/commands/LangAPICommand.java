package dev.sergiferry.languages.commands;

import dev.sergiferry.languages.LanguagesPlugin;
import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
import dev.sergiferry.languages.api.Translation;
import dev.sergiferry.languages.api.events.plugin.PluginLanguageLoadEvent;
import dev.sergiferry.languages.api.exceptions.EmptyResultException;
import dev.sergiferry.languages.api.exceptions.NotExternalDatabaseException;
import dev.sergiferry.languages.debug.DebugManager;
import dev.sergiferry.languages.utils.StringUtils;
import dev.sergiferry.spigot.SpigotPlugin;
import dev.sergiferry.spigot.commands.CommandInstance;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class LangAPICommand extends CommandInstance implements TabCompleter {

    private static String COMMAND_LABEL = "langapi";
    private static List<String> COMMAND_ACTIONS = Arrays.asList("reload", "set", "get", "info", "upload", "download");
    private static final String COMMAND_LANG_API = "command.lang_api.";

    public LangAPICommand(SpigotPlugin plugin) {
        super(plugin, COMMAND_LABEL);
        setTabCompleter(this);
    }

    public String getStringTranslation(String key, Language displayLanguage){ return getLanguagesPluginManager().getStringTranslation(COMMAND_LANG_API + key).get(displayLanguage); }

    @Override
    public void onExecute(CommandSender sender, Command command, String label, String[] args) {
        final Player playerSender = (sender instanceof Player) ? (Player) sender : null;
        Language displayLanguage = LanguagesAPI.getLanguage(sender);
        if (playerSender != null && !getPlugin().hasPermission(playerSender, "command")) {
            sender.sendMessage(getLanguagesPluginManager().getTranslationString("player.no_permissions", displayLanguage));
            return;
        }
        if(args.length > 0){
            LanguagesAPI.PluginManager tempPluginManager = null;
            try{ tempPluginManager = LanguagesAPI.getPluginManager(args[0]); }
            catch (Exception e) {
                if(Bukkit.getPluginManager().getPlugin(args[0]) == null){
                    sender.sendMessage(getStringTranslation("error.plugin_not_found", displayLanguage));
                    return;
                }
                sender.sendMessage(getStringTranslation("error.plugin_not_using", displayLanguage));
                return;
            }
            LanguagesAPI.PluginManager pluginManager = tempPluginManager;
            if(args.length > 1){
                String action = args[1];
                if(!COMMAND_ACTIONS.contains(action)){
                    sender.sendMessage(getStringTranslation("error.not_recognized.action", displayLanguage));
                    return;
                }
                if(LanguagesAPI.getDatabaseType().isLocal() && (action.equalsIgnoreCase("download") || action.equalsIgnoreCase("upload"))){
                    sender.sendMessage(getStringTranslation("error.database.not_external", displayLanguage));
                    return;
                }
                if(action.equalsIgnoreCase("info") && args.length == 2){
                    Collection<Translation> translations = pluginManager.getTranslationsValues();
                    int totalTranslations = translations.size();
                    Map<Language, Integer> translatedLanguages = new HashMap<>();
                    LanguagesAPI.getAvailableLanguages().forEach(language -> translatedLanguages.put(language, (int) translations.stream().filter(translation -> translation.isTranslatedIn(language)).count()));
                    sender.sendMessage(getStringTranslation("info.translated_languages", displayLanguage).formatted(pluginManager.getPlugin().getName()));
                    translatedLanguages.forEach((language, integer) -> sender.sendMessage("- " + language.getNameAndRegion() + ": "  + (integer < totalTranslations ? (integer > 0 ? "§e": "§c") : "§a") + integer + "§f/" + totalTranslations));
                    return;
                }
                else if(action.equalsIgnoreCase("upload") && args.length == 2){
                    final Integer total = Math.toIntExact(pluginManager.getTranslationsValues().stream().filter(x -> x.isTranslatedIn(LanguagesAPI.getAvailableLanguages())).count());
                    if(total <= 0){
                        sender.sendMessage(getStringTranslation("error.no_translations_to_upload", displayLanguage));
                        return;
                    }
                    AtomicLong started = new AtomicLong(System.currentTimeMillis());
                    AtomicLong nextTranslation = new AtomicLong(System.currentTimeMillis());
                    AtomicInteger current = new AtomicInteger(0);
                    BossBar bar = Bukkit.createBossBar(getStringTranslation("upload.starting", displayLanguage).formatted(total, pluginManager.getPlugin().getName()), BarColor.GREEN, BarStyle.SOLID);
                    bar.addPlayer(playerSender);
                    bar.setProgress(0.0);
                    try {
                        pluginManager.uploadTranslations(translation -> {
                            if(current.addAndGet(1) > total) return;
                            bar.setTitle(getStringTranslation("upload.progress", displayLanguage).formatted(current.get(), total));
                            bar.setProgress((double) current.get() / (double) total);
                            DebugManager.debug(sender, "Uploaded " + current.get() + "/" + total + " in " + (System.currentTimeMillis() - nextTranslation.getAndSet(System.currentTimeMillis())) + "ms");
                        }, () -> {
                            bar.setTitle(getStringTranslation("upload.completed", displayLanguage));
                            bar.setProgress(1);
                            DebugManager.debug(sender, "Completed upload of " + total + " in " + (System.currentTimeMillis() - started.get()) + "ms");
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> bar.removeAll(), 40);
                        });
                    } catch (NotExternalDatabaseException e) {
                        sender.sendMessage(getStringTranslation("error.database.not_external", displayLanguage));
                    }
                    return;
                }
                else if(action.equalsIgnoreCase("reload") && args.length == 2){
                    for(Language language : LanguagesAPI.getAvailableLanguages()) reloadLanguage(sender, displayLanguage, language, pluginManager);
                    return;
                }
                else if(action.equalsIgnoreCase("download") && args.length == 2){
                    sender.sendMessage(getStringTranslation("download.starting.plugin", displayLanguage).formatted( pluginManager.getPlugin().getName()));
                    try {
                        pluginManager.downloadTranslations().whenComplete((result, error) -> {
                            if(error != null){
                                checkDownloadExceptions(error, sender, displayLanguage);
                                return;
                            }
                            sender.sendMessage(getStringTranslation("download.completed", displayLanguage).formatted(result.size()));
                            try { pluginManager.save(); } catch (IOException e) { DebugManager.print(e); }
                        });
                    } catch (NotExternalDatabaseException e) {
                        sender.sendMessage(getStringTranslation("error.database.not_external", displayLanguage));
                    }
                    return;
                }
                if(args.length > 2){
                    Language language = Language.grabLanguage(args[2]).orElse(null);
                    if(language == null){
                        sender.sendMessage(getStringTranslation("error.not_recognized.language", displayLanguage));
                        return;
                    }
                    if(pluginManager.getUnavailableLanguages().contains(language)){
                        sender.sendMessage(getStringTranslation("error.language_not_supported.server", displayLanguage));
                        return;
                    }
                    if(!LanguagesAPI.getAvailableLanguages().contains(language)){
                        sender.sendMessage(getStringTranslation("error.language_not_supported.plugin", displayLanguage).formatted(pluginManager.getPlugin().getName()));
                        return;
                    }
                    if(action.equalsIgnoreCase("reload")){ reloadLanguage(sender, displayLanguage, language, pluginManager); }
                    else if(action.equalsIgnoreCase("info")){
                        Collection<Translation> translations = pluginManager.getTranslationsValues();
                        int totalTranslations = translations.size();
                        int translatedTranslations = (int) translations.stream().filter(translation -> translation.isTranslatedIn(language)).count();
                        sender.sendMessage(getStringTranslation("info.language_translations", displayLanguage).formatted(language.getNameAndRegion(), pluginManager.getPlugin().getName()));
                        sender.sendMessage(getStringTranslation("info.translations_count", displayLanguage).formatted((translatedTranslations < totalTranslations ? (translatedTranslations > 0 ? "§e": "§c") : "§a") + translatedTranslations, totalTranslations));
                        if(translatedTranslations < totalTranslations && translatedTranslations > 0){
                            List<String> missing = translations.stream().filter(translation -> !translation.isTranslatedIn(language)).map(translation -> translation.getSimpleKey()).collect(Collectors.toList());
                            sender.sendMessage(getStringTranslation("info.missing_translations", displayLanguage).formatted(StringUtils.getStringFromList(missing, StringUtils.StringListSeparator.COMMA)));
                        }
                    }
                    else if(action.equalsIgnoreCase("upload")){
                        final Integer total = Math.toIntExact(pluginManager.getTranslationsValues().stream().filter(x -> x.isTranslatedIn(language)).count());
                        if(total <= 0){
                            sender.sendMessage(getStringTranslation("error.no_translations_to_upload", displayLanguage));
                            return;
                        }
                        AtomicLong started = new AtomicLong(System.currentTimeMillis());
                        AtomicLong nextTranslation = new AtomicLong(System.currentTimeMillis());
                        AtomicInteger current = new AtomicInteger(0);
                        BossBar bar = Bukkit.createBossBar(getStringTranslation("upload.starting", displayLanguage).formatted(total, pluginManager.getPlugin().getName()), BarColor.GREEN, BarStyle.SOLID);
                        bar.addPlayer(playerSender);
                        bar.setProgress(0.0);
                        try {
                            pluginManager.uploadTranslations(language, translation -> {
                                if(current.addAndGet(1) > total) return;
                                bar.setTitle(getStringTranslation("upload.progress", displayLanguage).formatted(current.get(), total));
                                bar.setProgress((double) current.get() / (double) total);
                                DebugManager.debug(sender, "Uploaded " + current.get() + "/" + total + " in " + (System.currentTimeMillis() - nextTranslation.getAndSet(System.currentTimeMillis())) + "ms");
                            }, () -> {
                                bar.setTitle(getStringTranslation("upload.completed", displayLanguage));
                                bar.setProgress(1);
                                DebugManager.debug(sender, "Completed upload of " + total + " in " + (System.currentTimeMillis() - started.get()) + "ms");
                                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> bar.removeAll(), 40);
                            });
                        } catch (NotExternalDatabaseException e) {
                            sender.sendMessage(getStringTranslation("error.database.not_external", displayLanguage));
                        }
                    }
                    else if(action.equalsIgnoreCase("download")){
                        sender.sendMessage(getStringTranslation("download.starting.language", displayLanguage).formatted(language.getNameAndRegion(), pluginManager.getPlugin().getName()));
                        LanguagesAPI.PluginManager finalPluginManager = pluginManager;
                        try {
                            pluginManager.downloadTranslations(language).whenComplete((result, error) -> {
                                if(error != null){
                                    checkDownloadExceptions(error, sender, displayLanguage);
                                    return;
                                }
                                sender.sendMessage(getStringTranslation("download.completed", displayLanguage).formatted(result.size()));
                                try { finalPluginManager.save(language); } catch (IOException e) { DebugManager.print(e); }
                            });
                        } catch (NotExternalDatabaseException e) {
                            sender.sendMessage(getStringTranslation("error.database.not_external", displayLanguage));
                        }
                    }
                    else if(action.equalsIgnoreCase("get")){
                        if(args.length > 3) {
                            String code = args[3].toLowerCase();
                            if (!pluginManager.hasTranslation(code) || pluginManager.grabTranslationType(code).isEmpty()) {
                                sender.sendMessage(getStringTranslation("error.not_recognized.translation", displayLanguage));
                                return;
                            }
                            Translation translation = pluginManager.getTranslation(code, pluginManager.grabTranslationType(code).get());
                            if (!translation.getType().containsText()) {
                                sender.sendMessage(getStringTranslation("error.not_text", displayLanguage));
                                return;
                            }
                            Translation.Type translationType = translation.getType();
                            Translation.Result translationResult = translation.getResult(language);
                            Language languageResult = translationResult.getLanguage();
                            String type = translationType.getName().substring(0, 1).toUpperCase() + translationType.getName().substring(1).toLowerCase();
                            String fallback = " §c(" + getStringTranslation("error.fallback_from", displayLanguage)
                                    .formatted(language.getName()) + ")";
                            sender.sendMessage(getStringTranslation("success.get", displayLanguage)
                                    .formatted(code.toLowerCase(), languageResult.getNameAndRegion() + (!language.equals(languageResult) ? fallback : ""), pluginManager.getPlugin().getName(), type)
                            );
                            if(translationType.equals(Translation.Type.STRING)){
                                Translation.Result<String> stringResult = translationResult;
                                String finalResult = stringResult.getFinalResult();
                                if(ChatColor.stripColor(finalResult).isEmpty()) finalResult = stringResult.getPlainResult();
                                sender.sendMessage(finalResult);
                            }
                            else if(translationType.equals(Translation.Type.LIST)) for(String s : pluginManager.getListTranslation(code).get(language)) sender.sendMessage("- " + s);
                        }
                        else{
                            sender.sendMessage(getStringTranslation("error.not_specified.translation_key", displayLanguage));
                        }
                    }
                    else if(action.equalsIgnoreCase("set")){
                        if(args.length > 3){
                            String simpleKey = args[3].toUpperCase();
                            if(!pluginManager.hasTranslation(simpleKey) || pluginManager.grabTranslationType(simpleKey).isEmpty()){
                                sender.sendMessage(getStringTranslation("error.not_recognized.translation", displayLanguage));
                                return;
                            }
                            Translation translation = pluginManager.getTranslation(simpleKey, pluginManager.grabTranslationType(simpleKey).get());
                            if(!translation.canBeModified()){
                                sender.sendMessage(getStringTranslation("error.unmodifiable", displayLanguage));
                                return;
                            }
                            if(!translation.getType().containsText()){
                                sender.sendMessage(getStringTranslation("error.not_text", displayLanguage));
                                return;
                            }
                            if(args.length > 4){
                                String msg = "";
                                for(int i = 4; i < args.length; i++){
                                    msg = msg + " " + args[i];
                                }
                                msg = msg.replaceFirst(" ", "");
                                if(translation.getType().equals(Translation.Type.STRING)){
                                    pluginManager.setTranslation(language, simpleKey, Translation.Type.STRING, msg);
                                    msg = pluginManager.getTranslationString(simpleKey, language);
                                }
                                else if(translation.getType().equals(Translation.Type.LIST)){
                                    List<String> list = new ArrayList<>();
                                    if(msg.contains("\\n")) for(String s : msg.split("\\\\n")) list.add(s);
                                    else list.add(msg);
                                    pluginManager.setTranslation(language, simpleKey, Translation.Type.LIST, list);
                                    msg = "§r- " + StringUtils.getStringFromList(pluginManager.getTranslationList(simpleKey, language), StringUtils.StringListSeparator.LISTED);
                                }
                                else{
                                    sender.sendMessage(getStringTranslation("error.not_text", displayLanguage));
                                    return;
                                }
                                sender.sendMessage(getStringTranslation("success.set", displayLanguage)
                                        .formatted(simpleKey.toLowerCase(), language.getNameAndRegion(), pluginManager.getPlugin().getName(), translation.getType().getName().toLowerCase()));
                                sender.sendMessage(msg);
                                try { pluginManager.save(language); } catch (IOException e) { DebugManager.print(e); }
                            }
                            else{
                                sender.sendMessage(getStringTranslation("error.not_specified.translation_content", displayLanguage));
                            }
                        }
                        else{
                            sender.sendMessage(getStringTranslation("error.not_specified.translation_key", displayLanguage));
                        }
                    }
                    else{
                        sender.sendMessage(getStringTranslation("error.not_recognized.action", displayLanguage));
                    }
                }
                else{
                    sender.sendMessage(getStringTranslation("error.not_specified.language", displayLanguage));
                }
            }
            else{
                sender.sendMessage(getStringTranslation("error.not_specified.action", displayLanguage));
            }
        }
        else{
            StringBuilder stringBuilder = new StringBuilder("");
            for(String plugins : LanguagesAPI.getPluginNames()) stringBuilder.append("§f, §a" + plugins);
            stringBuilder.replace(0, 4, "");
            playerSender.sendMessage(getStringTranslation("general.title", displayLanguage).formatted(LanguagesPlugin.getInstance().getDescription().getVersion()));
            playerSender.sendMessage(getStringTranslation("general.plugins", displayLanguage).formatted(stringBuilder.toString()));
            playerSender.sendMessage(getStringTranslation("general.use", displayLanguage));

            Map<String, String> arguments = new HashMap<>();
            arguments.put("plugin", getStringTranslation("arguments.plugin", displayLanguage));
            arguments.put("language", getStringTranslation("arguments.language", displayLanguage));
            arguments.put("translation", getStringTranslation("arguments.translation", displayLanguage));
            arguments.put("value", getStringTranslation("arguments.value", displayLanguage));

            playerSender.sendMessage("§7- §e/" + COMMAND_LABEL + " (" + arguments.get("plugin") + ") set (" + arguments.get("language") + ") (" + arguments.get("translation") + ") (" + arguments.get("value") + ")");
            playerSender.sendMessage("§7- §e/" + COMMAND_LABEL + " (" + arguments.get("plugin") + ") get (" + arguments.get("language") + ") (" + arguments.get("translation") + ")");
            playerSender.sendMessage("§7- §e/" + COMMAND_LABEL + " (" + arguments.get("plugin") + ") reload [" + arguments.get("language") + "]");
            if(!LanguagesAPI.getDatabaseType().isLocal()){
                playerSender.sendMessage("§7- §e/" + COMMAND_LABEL + " (" + arguments.get("plugin") + ") upload [" + arguments.get("language") + "]");
                playerSender.sendMessage("§7- §e/" + COMMAND_LABEL + " (" + arguments.get("plugin") + ") download [" + arguments.get("language") + "]");
            }
            playerSender.sendMessage("§7- §e/" + COMMAND_LABEL + " (" + arguments.get("plugin") + ") info [" + arguments.get("language") + "]");
        }
    }

    private void reloadLanguage(CommandSender sender, Language displayLanguage, Language language, LanguagesAPI.PluginManager pluginManager){
        try {
            pluginManager.load(language);
            new PluginLanguageLoadEvent(pluginManager, language, PluginLanguageLoadEvent.Cause.COMMAND_RELOAD);
            sender.sendMessage(getStringTranslation("success.reload", displayLanguage).formatted(language.getNameAndRegion()));
        } catch(FileNotFoundException e){
            try {
                pluginManager.saveDefaults(language);
                sender.sendMessage("§aCreated new translations file in " + language.getNameAndRegion() + " for " + pluginManager.getPlugin().getName() + ".");
            } catch (IOException ex) {
                DebugManager.print(ex);
                sender.sendMessage(getStringTranslation("error.translations_file.creating", displayLanguage).formatted(language.getNameAndRegion(), pluginManager.getPlugin().getName()));
            }
        } catch (InvalidConfigurationException e){
            sender.sendMessage(getStringTranslation("error.translations_file.syntax", displayLanguage).formatted(pluginManager.getPlugin().getDataFolder().getPath() + "\\languages\\" + language.getLocaleCode().toLowerCase() + ".yml"));
        } catch (Exception e) {
            DebugManager.print(e);
            sender.sendMessage(getStringTranslation("error.reloading_language", displayLanguage).formatted(language.getNameAndRegion(), e.getMessage()));
        }
    }

    private void checkDownloadExceptions(Throwable error, CommandSender sender, Language displayLanguage){
        if(error instanceof TimeoutException || error instanceof SQLTimeoutException){
            sender.sendMessage(getStringTranslation("error.database.timeout", displayLanguage));
        } else if(error instanceof EmptyResultException){
            sender.sendMessage(getStringTranslation("error.no_translations_to_download", displayLanguage));
        } else{
            if(error instanceof SQLException && checkSQLExceptions((SQLException) error, sender, displayLanguage)) return;
            sender.sendMessage(getStringTranslation("error.downloading_translations", displayLanguage));
            DebugManager.print(error);
        }
    }

    private boolean checkSQLExceptions(SQLException error, CommandSender sender, Language displayLanguage){
        boolean checked = true;
        if(error instanceof SQLTimeoutException){
            sender.sendMessage(getStringTranslation("error.database.timeout", displayLanguage));
        } else if(error instanceof SQLRecoverableException){
            sender.sendMessage(getStringTranslation("error.database.connection", displayLanguage));
        } else if(error.getSQLState().equals("28000")){
            sender.sendMessage(getStringTranslation("error.database.credentials", displayLanguage));
        } else if(error.getSQLState().startsWith("42000")){
            sender.sendMessage(getStringTranslation("error.database.not_found", displayLanguage).formatted( StringUtils.getStringInside(error.getLocalizedMessage(), '\'')));
        } else if(error.getSQLState().equals("42S02")){
            sender.sendMessage(getStringTranslation("error.database.table_not_found", displayLanguage).formatted(StringUtils.getStringInside(error.getLocalizedMessage(), '\'')));
        } else checked = false;
        DebugManager.debug("SQL Exception " + (!checked ? "- not checked - " : "") + "(" + error.getSQLState() + "): " + error.getLocalizedMessage());
        return checked;
    }

    protected LanguagesAPI.PluginManager getLanguagesPluginManager(){ return LanguagesPlugin.getPluginManager(); }

    @Nullable
    @Override
    public List<String> onTabComplete(@Nonnull CommandSender commandSender, @Nonnull Command command, @Nonnull String label, @Nonnull String[] args) {
        if(!isCommand(label)) return null;
        List<String> suggested = new ArrayList<>();
        if(args.length == 1) LanguagesAPI.getPluginNames().stream().filter(x-> x.toLowerCase().startsWith(args[0].toLowerCase())).forEach(x-> suggested.add(x));
        else if(args.length > 1){
            LanguagesAPI.PluginManager pluginManager = null;
            try{ pluginManager = LanguagesAPI.getPluginManager(args[0]); } catch (Exception e) { return suggested; }
            if(args.length == 2){
                COMMAND_ACTIONS.stream().filter(x-> x.toLowerCase().startsWith(args[1].toLowerCase())).forEach(x-> suggested.add(x.toLowerCase()));
                if(LanguagesAPI.getDatabaseType().isLocal()) Arrays.asList("download", "upload").stream().filter(x -> suggested.contains(x)).forEach(x-> suggested.remove(x));
            }
            else if(args.length > 2){
                if(args.length == 3){
                    LanguagesAPI.getAvailableLanguages().stream().filter(x-> x.getLocaleCode().toLowerCase().startsWith(args[2].toLowerCase())).forEach(x-> suggested.add(x.getLocaleCode().toLowerCase()));
                    if(!pluginManager.getUnavailableLanguages().isEmpty()) {
                        pluginManager.getUnavailableLanguages().stream().filter(x-> x.getLocaleCode().toLowerCase().startsWith(args[2].toLowerCase())).forEach(x-> suggested.add("§c" + x.getLocaleCode().toLowerCase()));
                    }
                }
                else if(args.length > 3){
                    if(args.length == 4 && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("get"))){
                        pluginManager.getTranslationsValues().stream().filter(x-> x.getSimpleKey().toLowerCase().startsWith(args[3].toLowerCase())).forEach(x-> {
                            if(args[1].equalsIgnoreCase("set") && !x.canBeModified()) suggested.add("§c" + x.getSimpleKey().toLowerCase());
                            else suggested.add(x.getSimpleKey().toLowerCase());
                        });
                    }
                    else if(args.length > 4){
                        if(args.length == 5 && args[1].equalsIgnoreCase("set") && args[4].length() == 0){
                            Language language = Language.grabLanguage(args[2].toLowerCase()).orElse(null);
                            if(language == null) return suggested;
                            Translation.Type type = pluginManager.grabTranslationType(args[3].toLowerCase()).orElse(null);
                            if(type == null) return suggested;
                            if(type.equals(Translation.Type.STRING)){
                                Translation<String> translation = pluginManager.getTranslation(args[3].toLowerCase(), Translation.Type.STRING);
                                if(translation.isTranslatedIn(language)) suggested.add(translation.getCache(language));
                                else if(translation.isTranslatedIn(pluginManager.getDefaultLanguage())) suggested.add(translation.getCache(pluginManager.getDefaultLanguage()));
                                return suggested;
                            }
                            if(type.equals(Translation.Type.LIST)){
                                Translation<List<String>> translation = pluginManager.getTranslation(args[3].toLowerCase(), Translation.Type.LIST);
                                if(translation.isTranslatedIn(language)) suggested.add(StringUtils.getStringFromList(translation.getCache(language), StringUtils.StringListSeparator.NEW_LINE_TEXT));
                                else if(translation.isTranslatedIn(pluginManager.getDefaultLanguage())) suggested.add(StringUtils.getStringFromList(translation.getCache(pluginManager.getDefaultLanguage()), StringUtils.StringListSeparator.NEW_LINE_TEXT));
                                return suggested;
                            }
                        }
                    }
                }
            }
        }
        return suggested;
    }
}
