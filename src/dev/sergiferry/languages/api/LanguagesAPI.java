package dev.sergiferry.languages.api;

import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import dev.sergiferry.languages.LanguagesPlugin;
import dev.sergiferry.languages.api.events.player.PlayerLanguageChangeEvent;
import dev.sergiferry.languages.api.events.player.PlayerLanguageLoadEvent;
import dev.sergiferry.languages.api.events.plugin.PluginLanguageLoadEvent;
import dev.sergiferry.languages.api.exceptions.EmptyResultException;
import dev.sergiferry.languages.api.exceptions.NotExternalDatabaseException;
import dev.sergiferry.languages.debug.DebugManager;
import dev.sergiferry.languages.integration.IntegrationsManager;
import dev.sergiferry.languages.integration.integrations.ProtocolLib;
import dev.sergiferry.languages.storage.Database;
import dev.sergiferry.languages.storage.MySQL;
import dev.sergiferry.languages.storage.SQLite;
import dev.sergiferry.spigot.SpigotPlugin;
import dev.sergiferry.spigot.server.ServerVersion;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Creado por SergiFerry el 23/08/2022
 */
public class LanguagesAPI implements Listener{

    private static final LanguagesAPI INSTANCE;

    static{
        INSTANCE = new LanguagesAPI();
    }

    private FileConfiguration configuration;
    private Map<String, PluginManager> pluginManagerMap;
    private final Map<UUID, Language> playerLanguage;
    protected Map<Player, PlayerLoadLanguageTask> playerLoadLanguageTaskMap;
    private Language serverLanguage;
    private String apiVersion;
    private Database database;

    private boolean announcePlayerLanguageChange;
    private boolean suggestPlayerAutoLanguage;
    private boolean allowPlayerSelectLanguage;
    private boolean commentDefaultTranslations;
    private boolean readTranslationsFromDatabase;
    private boolean readServerLanguagesFromDatabase;

    private LanguagesAPI(){
        pluginManagerMap = new HashMap<>();
        playerLanguage = new HashMap<>();
        playerLoadLanguageTaskMap = new HashMap<>();
        configuration = null;
        serverLanguage = Language.en_US;
        announcePlayerLanguageChange = true;
        database = null;
    }

    private String getConfigFilePath(){ return LanguagesPlugin.getInstance().getDataFolder().getPath() + "/config.yml"; }

    public void enable(LanguagesPlugin languagesPlugin){
        apiVersion = languagesPlugin.getDescription().getVersion();
        File configFile = new File(getConfigFilePath());
        if(!configFile.exists())  try { configFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        configuration = YamlConfiguration.loadConfiguration(configFile);
        //
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("debug", false);
        defaults.put("server.language", Language.en_US.getLocaleCode().toLowerCase());
        defaults.put("server.availableLanguages", Arrays.asList(
                Language.en_US.getLocaleCode().toLowerCase(),
                Language.es_ES.getLocaleCode().toLowerCase(),
                Language.de_DE.getLocaleCode().toLowerCase(),
                Language.it_IT.getLocaleCode().toLowerCase(),
                Language.fr_FR.getLocaleCode().toLowerCase(),
                Language.pt_PT.getLocaleCode().toLowerCase()
        ));
        defaults.put("server.readFromDatabase", false);
        defaults.put("server.uuid", UUID.randomUUID().toString());
        defaults.put("player.announceLanguageChange", true);
        defaults.put("player.suggestAutoLanguage", true);
        defaults.put("player.allowSelectLanguage", true);
        defaults.put("translations.commentDefault", true);
        defaults.put("translations.readFromDatabase", false);
        defaults.put("database.mysql", false);
        defaults.put("database.host", "localhost");
        defaults.put("database.port", 3306);
        defaults.put("database.user", "root");
        defaults.put("database.password", "");
        //
        boolean m = false;
        for(String s : defaults.keySet()){
            if(configuration.contains(s)) continue;
            configuration.set(s, defaults.get(s));
            m = true;
        }
        if(SpigotPlugin.getServerVersion().isNewerThanOrEqual(ServerVersion.VERSION_1_18)){
            configuration.setComments("player.announceLanguageChange", Arrays.asList("Player will be notified in-game when the server detects that their client's language has changed."));
            configuration.setComments("server.availableLanguages", Arrays.asList("These are the languages that players will be able to select", "You can use '*' to add all languages."));
        }
        if(m) { try { configuration.save(configFile); } catch (IOException e) { e.printStackTrace(); } }
        //
        //  Save languages list file
        try {
            StringBuilder stringBuilder = new StringBuilder("All languages list:");
            for(Language language : Language.getAllLanguages()){ stringBuilder.append("\n- " + language.getConsoleName()); }
            File newTextFile = new File(LanguagesPlugin.getInstance().getDataFolder().getPath() + "/languagesList.txt");
            FileWriter fw = new FileWriter(newTextFile);
            fw.write(stringBuilder.toString());
            fw.close();
        } catch (IOException iox) { iox.printStackTrace(); }
        //
        DebugManager.DEBUG =  configuration.getBoolean("debug");
        serverLanguage = Language.grabLanguage(configuration.getString("server.language")).orElse(Language.en_US);
        announcePlayerLanguageChange = configuration.getBoolean("player.announceLanguageChange");
        suggestPlayerAutoLanguage = configuration.getBoolean("player.suggestAutoLanguage");
        allowPlayerSelectLanguage = configuration.getBoolean("player.allowSelectLanguage");
        readServerLanguagesFromDatabase = configuration.getBoolean("server.readFromDatabase");
        readTranslationsFromDatabase = configuration.getBoolean("translations.readFromDatabase");
        commentDefaultTranslations = configuration.getBoolean("translations.commentDefault");

        List<Language> available = new ArrayList<>();
        for(String locales : configuration.getStringList("server.availableLanguages")){
            if(locales.equals("*")){
                available.clear();
                available.addAll(Language.getAllLanguages());
                break;
            }
            Language.grabLanguage(locales).ifPresentOrElse(lang -> { if(!available.contains(lang)) available.add(lang); }
                    , () -> Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + "Language '§c" + locales + "§7' does not exists."));
        }

        if(configuration.getBoolean("database.mysql")){
            database = new MySQL(
                    configuration.getString("database.host"),
                    configuration.getInt("database.port"),
                    languagesPlugin.getName(),
                    configuration.getString("database.user"),
                    configuration.getString("database.password"));
        }
        else database = new SQLite(languagesPlugin.getName());
        try {
            database.connect();
            database.createTables();
        }
        catch (ClassNotFoundException e) { Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + getDatabaseType().name() + " database type is not supported. Please change it."); }
        catch (SQLException e){
            Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + "§cThere's a problem connecting to database (" + e.getSQLState() + "): " + e.getLocalizedMessage());
            DebugManager.print(e);
        }
        catch (Exception e) { DebugManager.print(e); }
        //
        hook(languagesPlugin, serverLanguage, available);
        languagesPlugin.getServer().getPluginManager().registerEvents(this, languagesPlugin);
        Bukkit.getOnlinePlayers().forEach(x-> new PlayerLoadLanguageTask(x));
    }

    public void disable(LanguagesPlugin languagesPlugin){
        HandlerList.unregisterAll(this);
        try { getDatabase().disconnect(); } catch (SQLException e) { DebugManager.print(e); }
    }

    @Nonnull
    public static PluginManager hook(Plugin plugin, Language defaultLanguage, File defaultLanguageFile, Collection<Language> additionalLanguages){
        Validate.notNull(plugin, "Plugin cannot be null");
        Validate.isTrue(!INSTANCE.pluginManagerMap.containsKey(plugin.getName().toLowerCase()), "This plugin is hooked yet to the Languages API");
        Validate.notNull(defaultLanguage, "Default language cannot be null");
        PluginManager pluginManager = new PluginManager(INSTANCE, plugin, defaultLanguage, defaultLanguageFile, additionalLanguages);
        INSTANCE.pluginManagerMap.put(plugin.getName().toLowerCase(), pluginManager);
        try { pluginManager.load(); } catch (IOException e) { DebugManager.print(e); }
        return pluginManager;
    }

    @Nonnull
    public static PluginManager hook(Plugin plugin, Language defaultLanguage, Collection<Language> additionalLanguages){
        return hook(plugin, defaultLanguage, null, additionalLanguages);
    }

    @Nonnull
    public static PluginManager hook(@Nonnull Plugin plugin, Language defaultLanguage, Language... additionalLanguages){
        return hook(plugin, defaultLanguage, null, Arrays.stream(additionalLanguages).toList());
    }

    @Nonnull
    public static PluginManager hook(@Nonnull Plugin plugin, Language defaultLanguage, File defaultLanguageFile, Language... additionalLanguages){
        return hook(plugin, defaultLanguage, defaultLanguageFile, Arrays.stream(additionalLanguages).toList());
    }

    @Nonnull
    public static PluginManager hook(@Nonnull Plugin plugin, Language defaultLanguage){
        return hook(plugin, defaultLanguage, null, (Collection<Language>) null);
    }

    @Nonnull
    public static PluginManager hook(@Nonnull Plugin plugin, Language defaultLanguage, File defaultLanguageFile){
        return hook(plugin, defaultLanguage, defaultLanguageFile, (Collection<Language>) null);
    }

    @Nonnull
    public static PluginManager getPluginManager(@Nonnull Plugin plugin){
        return getPluginManager(plugin.getName());
    }

    public static PluginManager getPluginManager(@Nonnull String pluginName){
        Validate.notNull(pluginName, "Plugin name cannot be null");
        Validate.isTrue(INSTANCE.pluginManagerMap.containsKey(pluginName.toLowerCase()),  pluginName + " have not hooked Languages API yet.");
        return INSTANCE.pluginManagerMap.get(pluginName.toLowerCase());
    }

    public static Set<String> getPluginNames(){
        return INSTANCE.pluginManagerMap.keySet();
    }

    public static List<Language> getAvailableLanguages(){
        return LanguagesPlugin.getPluginManager().getTranslatedLanguages();
    }

    @Nonnull
    public static Language getLanguage(@Nonnull Player player){
        Validate.notNull(player, "Player cannot be null");
        if(!INSTANCE.isStillLoadingLanguage(player)){
            if(INSTANCE.playerLanguage.containsKey(player.getUniqueId())) return INSTANCE.playerLanguage.get(player.getUniqueId());
            else if(Language.grabClientLanguage(player).isPresent()) return Language.grabClientLanguage(player).orElse(INSTANCE.serverLanguage);
        }
        return INSTANCE.serverLanguage;
    }

    @Nonnull
    public static Language getLanguage(@Nullable CommandSender commandSender){
        if(commandSender != null && commandSender instanceof Player) return getLanguage((Player) commandSender);
        else return getServerLanguage();
    }

    public static void setLanguage(@Nonnull Player player, @Nonnull Language language){
        Validate.notNull(player, "Player cannot be null");
        Validate.notNull(language, "Language cannot be null");
        new PlayerLanguageChangeEvent(player, INSTANCE.playerLanguage.put(player.getUniqueId(), language), language, PlayerLanguageChangeEvent.Cause.SERVER);
        Bukkit.getScheduler().runTaskAsynchronously(LanguagesPlugin.getInstance(), () -> { try { getDatabase().getPlayerLanguagesTable().set(player.getUniqueId(), language, player.getLocale()); } catch (SQLException e) { DebugManager.print(e); }});
    }

    public static void resetLanguage(@Nonnull Player player){
        Validate.notNull(player, "Player cannot be null");
        new PlayerLanguageChangeEvent(player, INSTANCE.playerLanguage.remove(player.getUniqueId()), getLanguage(player), PlayerLanguageChangeEvent.Cause.SERVER);
        Bukkit.getScheduler().runTaskAsynchronously(LanguagesPlugin.getInstance(), () -> { try { getDatabase().getPlayerLanguagesTable().set(player.getUniqueId(), null, player.getLocale()); } catch (SQLException e) { DebugManager.print(e); }});
    }

    @Nullable
    public static String getAPIVersion(){ return INSTANCE.apiVersion; }

    public static boolean isAnnouncePlayerLanguageChange() { return INSTANCE.announcePlayerLanguageChange; }

    public static boolean isSuggestPlayerAutoLanguage() { return INSTANCE.suggestPlayerAutoLanguage; }

    public static boolean isAllowPlayerSelectLanguage() { return INSTANCE.allowPlayerSelectLanguage; }

    public static boolean isCommentDefaultTranslations() { return INSTANCE.commentDefaultTranslations; }

    public static boolean isReadTranslationsFromDatabase() { return INSTANCE.readTranslationsFromDatabase; }

    public static boolean isReadServerLanguagesFromDatabase() { return INSTANCE.readServerLanguagesFromDatabase; }

    protected static Database getDatabase() { return INSTANCE.database; }

    public static Database.Type getDatabaseType(){ return INSTANCE.database.getType(); }

    @Nonnull public static Language getServerLanguage() { return INSTANCE.serverLanguage; }

    public boolean isStillLoadingLanguage(Player player){ return playerLoadLanguageTaskMap.containsKey(player); }

    public static boolean hasSelectedLanguage(Player player){ return INSTANCE.playerLanguage.containsKey(player.getUniqueId()); }

    public static boolean hasAutoDetectedLanguage(Player player){ return !hasSelectedLanguage(player); }

    public static Optional<Language> getSelectedLanguage(Player player){ return Optional.ofNullable(INSTANCE.playerLanguage.getOrDefault(player.getUniqueId(), null)); }

    @EventHandler
    private void onLocaleChange(PlayerLocaleChangeEvent event){
        if(isStillLoadingLanguage(event.getPlayer())) return;
        Optional<Long> lastJoin = event.getPlayer().getMetadata("lastJoin").stream().filter(x-> x.getOwningPlugin().equals(LanguagesPlugin.getInstance())).findFirst().map(x-> x.asLong());
        if(lastJoin.isPresent() && (System.currentTimeMillis() - lastJoin.get()) / 1000 < 5) return;
        final String toLocale = event.getLocale();
        Bukkit.getScheduler().runTaskAsynchronously(LanguagesPlugin.getInstance(), () -> {
            try { getDatabase().getPlayerLanguagesTable().set(event.getPlayer().getUniqueId(), toLocale); }
            catch (SQLException e) {}
        });
        Language to = Language.grabLanguage(toLocale).orElse(null);
        if(to == null) return;
        if(hasSelectedLanguage(event.getPlayer())){
            if(!isSuggestPlayerAutoLanguage()) return;
            if(!to.isAvailable() || !to.hasSimilarAvailable()) return;
            String lang = (to.isAvailable() ? "§a" : (to.hasSimilarAvailable() ? "§6" : "§c")) + to.getNameAndRegion() + (event.getPlayer().isOp() ? " §7[" + to.getLocaleCode() + "]" : "");
            event.getPlayer().sendMessage(LanguagesPlugin.getPluginManager().getStringTranslation("player.detected_client_language_change").get(to).formatted(lang, "/lang auto"));
            return;
        }
        final String fromLocale = event.getPlayer().getLocale();
        Language from = Language.grabLanguage(fromLocale).orElse(null);
        new PlayerLanguageChangeEvent(event.getPlayer(), from, to, PlayerLanguageChangeEvent.Cause.CLIENT);
    }

    @EventHandler
    public void onLocaleJoin(ProtocolLib.PlayerLocaleJoinEvent event){
        Player player = event.getPlayer();
        if(!isStillLoadingLanguage(event.getPlayer())) return;
        playerLoadLanguageTaskMap.get(player).completeLocale(event.getLocale());
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        new PlayerLoadLanguageTask(player);
        player.setMetadata("lastJoin", new FixedMetadataValue(LanguagesPlugin.getInstance(), System.currentTimeMillis()));
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event){
        Player player = event.getPlayer();
        playerLanguage.remove(player.getUniqueId());
    }

    protected static class PlayerLoadLanguageTask extends CompletableFuture<PlayerLoadLanguageTask.Result>{

        private static final String DEFAULT_CLIENT_LOCALE = "en_us";

        private Player player;

        private PlayerLoadLanguageTask(Player player) {
            super();
            this.player = player;
            thenAccept(result -> {
                DebugManager.debug(player, "Your language is: " + result.language.getName() + " ("  + result.type.name() + ")");
                INSTANCE.playerLoadLanguageTaskMap.remove(player);
                Bukkit.getScheduler().scheduleSyncDelayedTask(LanguagesPlugin.getInstance(), () -> new PlayerLanguageLoadEvent(player, result.language, result.type));
            });
            INSTANCE.playerLoadLanguageTaskMap.put(player, this);
            Bukkit.getScheduler().runTaskAsynchronously(LanguagesPlugin.getInstance(), () -> {
                String lastLocale = null;
                try {
                    Pair<Optional<Language>, Optional<String>> data = getDatabase().getPlayerLanguagesTable().get(player.getUniqueId());
                    lastLocale = data.getSecond().orElse(null);
                    Language language = data.getFirst().orElseThrow();
                    INSTANCE.playerLanguage.put(player.getUniqueId(), language);
                    complete(new PlayerLoadLanguageTask.Result(language, PlayerLanguageLoadEvent.Type.SELECTED_LANGUAGE));
                } catch (Exception e) {
                    DebugManager.debug(player, "Your language was not found in database.");
                    if(isDone() || isCancelled()) return;
                    if(IntegrationsManager.isUsingProtocolLib()) return;
                    Optional<Long> lastJoin = player.getMetadata("lastJoin").stream().filter(x-> x.getOwningPlugin().equals(LanguagesPlugin.getInstance())).findFirst().map(x-> x.asLong());
                    boolean isDefault = player.getLocale().equals(DEFAULT_CLIENT_LOCALE) && lastLocale != null && lastLocale.equals(DEFAULT_CLIENT_LOCALE);
                    DebugManager.debug(player, "Your last locale: " + lastLocale);
                    DebugManager.debug(player, "Using default client language (en_us): " + isDefault);
                    if(isDefault) completeLocale();
                    else if(lastJoin.isPresent() && (System.currentTimeMillis() - lastJoin.get()) < 1000){
                        DebugManager.debug(player, "Waiting a little to receive client's locale");
                        String finalLastLocale = lastLocale;
                        new BukkitRunnable(){

                            AtomicInteger ticks = new AtomicInteger(0);

                            @Override
                            public void run() {
                                String actualLocale = player.getLocale();
                                if(!actualLocale.equals(DEFAULT_CLIENT_LOCALE)){
                                    DebugManager.debug(player, "Received your locale " + actualLocale + " in " + ticks.get() + " ticks");
                                    finish();
                                    return;
                                }
                                if(ticks.incrementAndGet() == (finalLastLocale.equals(DEFAULT_CLIENT_LOCALE) ? 10 : 30)) finish();
                            }

                            public void finish(){
                                completeLocale();
                                this.cancel();
                            }

                        }.runTaskTimerAsynchronously(LanguagesPlugin.getInstance(), 1, 1);
                    } else completeLocale();
                }
            });
        }

        private void completeLocale(){ completeLocale(player.getLocale()); }

        private void completeLocale(String locale){
            DebugManager.debug(player, "Local return: " + locale);
            Language language = Language.grabLanguage(locale).orElse(null);
            if(language != null) complete(new PlayerLoadLanguageTask.Result(language, PlayerLanguageLoadEvent.Type.CLIENT_LANGUAGE));
            else complete(new PlayerLoadLanguageTask.Result(LanguagesAPI.getServerLanguage(), PlayerLanguageLoadEvent.Type.SERVER_DEFAULT));
        }

        public Player getPlayer() { return player; }

        protected record Result(Language language, PlayerLanguageLoadEvent.Type type){}

    }

    public static class PluginManager implements Listener {

        private final Plugin plugin;
        private final LanguagesAPI languagesAPI;
        private final Map<String, Translation> translations;
        private final Map<Language, YamlConfiguration> languagesYAML;
        private final Language defaultLanguage;
        private final List<Language> translatedLanguages;
        private final List<Language> unavailableLanguages;
        private File defaultLanguageFile;
        private boolean useFormattedPlaceholders;
        private boolean fallbackToSimilar;
        private boolean fallbackToServerLanguage;
        private boolean fallbackToDefault;
        private boolean autoColorTranslations;
        private boolean saveOnDisable;

        private PluginManager(@Nonnull LanguagesAPI languagesAPI, @Nonnull Plugin plugin, Language defaultLanguage, @Nullable File defaultLanguageFile, Collection<Language> translatedLanguages){
            boolean isLanguagesPlugin = LanguagesPlugin.getInstance().equals(plugin);
            if(!isLanguagesPlugin) Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getPluginManager().getTranslation("console.enable.plugin_registered", Translation.Type.STRING).getInServerDefaultLanguage().formatted(plugin.getName()));
            this.plugin = plugin;
            this.languagesAPI = languagesAPI;
            this.fallbackToSimilar = true;
            this.fallbackToServerLanguage = true;
            this.fallbackToDefault = true;
            this.autoColorTranslations = true;
            this.useFormattedPlaceholders = LanguagesPlugin.getInstance().equals(plugin);
            this.translations = new HashMap<>();
            this.languagesYAML = new HashMap<>();
            this.saveOnDisable = false;
            this.defaultLanguageFile = defaultLanguageFile;
            this.defaultLanguage = defaultLanguage;
            this.translatedLanguages = new ArrayList<>();
            this.unavailableLanguages = new ArrayList<>();
            this.translatedLanguages.add(defaultLanguage);
            if(translatedLanguages != null && !translatedLanguages.isEmpty()) translatedLanguages.stream().filter(x-> !x.equals(defaultLanguage)).forEach(x-> this.translatedLanguages.add(x));
            getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());
        }

        public void disable(){
            if(saveOnDisable) LanguagesAPI.getAvailableLanguages().forEach(x-> {
                try { save(x); } catch (IOException e) { DebugManager.print(e); }
            });
            HandlerList.unregisterAll(this);
            LanguagesAPI.INSTANCE.pluginManagerMap.remove(plugin.getName(), this);
        }

        @EventHandler
        public void onDisable(PluginDisableEvent event){
            if(!saveOnDisable) return;
            if(!event.getPlugin().equals(plugin)) return;
            disable();
        }

        public Plugin getPlugin() { return plugin; }

        public LanguagesAPI getLanguagesAPI() { return languagesAPI; }

        public Language getDefaultLanguage() { return defaultLanguage; }

        public File getDefaultLanguageFile() { return defaultLanguageFile; }

        public boolean hasDefaultLanguageFile() { return defaultLanguageFile != null; }

        protected void load() throws IOException {
            boolean isLanguagesPlugin = LanguagesPlugin.getInstance().equals(plugin);
            List<Language> globalLanguages = isLanguagesPlugin ? translatedLanguages : LanguagesAPI.getAvailableLanguages();
            for(Language language : Language.getAllLanguages()){
                if(!translatedLanguages.contains(language) && !globalLanguages.contains(language)) continue;
                if(translatedLanguages.contains(language) && !globalLanguages.contains(language)){
                    if(isLanguagesPlugin) continue;
                    unavailableLanguages.add(language);
                    Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getPluginManager().getStringTranslation("console.enable.language.not_available").getInServerDefaultLanguage().formatted(plugin.getName(), language.getConsoleName()));
                    continue;
                } else if(!translatedLanguages.contains(language)){
                    File file = new File(language.getTranslationsFilePath(this));
                    if(!file.exists()) {
                        Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getPluginManager().getStringTranslation("console.enable.language.not_translated").getInServerDefaultLanguage().formatted(plugin.getName(), language.getConsoleName()));
                        try { save(language); } catch (IOException e) { DebugManager.print(e); }
                    }
                    continue;
                }
                try{
                    load(language);
                    new PluginLanguageLoadEvent(this, language, PluginLanguageLoadEvent.Cause.SERVER_LOAD);
                }
                catch (FileNotFoundException e){}
                catch (InvalidConfigurationException e){
                    Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + "§c" +  language.getConsoleName() + " §7translations file for §e" + plugin.getName() + " §7is corrupted and cannot be read. Fix it, or delete it in order to create a new one.");
                    continue;
                }
                catch (Exception e) {
                    DebugManager.print(e);
                    if(isLanguagesPlugin) Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + "There was an error loading §c" + language.getConsoleName() + " §7for §e" + plugin.getName());
                    else Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getPluginManager().getStringTranslation("console.enable.language.loading_error").getInServerDefaultLanguage().formatted(language.getConsoleName(), plugin.getName()));
                }
                try { saveDefaults(language); } catch (IOException e) { DebugManager.print(e); }
            }
            if(!unavailableLanguages.isEmpty()){
                Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getPluginManager().getStringTranslation("console.enable.language.add_available").getInServerDefaultLanguage().formatted("availableLanguages", LanguagesPlugin.getInstance().getDataFolder().getPath() + "\\config.yml"));
                translatedLanguages.removeAll(unavailableLanguages);
            }
            if(isReadTranslationsFromDatabase()) {
                try { downloadTranslations(); }
                catch (NotExternalDatabaseException e) {
                    Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + "§cTranslations can only be read from an external database, such as MySQL.");
                    Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getInstance().getPrefix() + "§7You can use an external database, or disable read translations from database.");
                }
            }
        }

        public void load(@Nonnull Language language) throws IOException, FileNotFoundException, InvalidConfigurationException {
            Validate.notNull(language, "Cannot load a null language.");
            Validate.isTrue(LanguagesAPI.getAvailableLanguages().contains(language), "This language is not available.");
            readDefaults(language, "yml").ifPresent(inputStreamReader -> {
                DebugManager.debug("Default " + language.getLocaleCode() + " YML translation file found for " + getPlugin().getName());
                YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(inputStreamReader);
                setDefaultTranslations(language, yamlConfiguration);
            });
            readDefaults(language, "json").ifPresent(inputStreamReader -> {
                DebugManager.debug("Default " + language.getLocaleCode() + " JSON translation file found for " + getPlugin().getName());
                JsonObject jsonObject = new JsonParser().parse(inputStreamReader).getAsJsonObject();
                setDefaultTranslations(language, jsonObject);
            });
            File folder = new File(language.getFolderPath(this));
            folder.mkdirs();
            File file = new File(language.getTranslationsFilePath(this));
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            languagesYAML.put(language, config);
            for(String keys : config.getKeys(true)){
                Translation.Type.grabTypeFromYAML(config, keys).ifPresent(type -> {
                    Translation translation = getTranslation(keys, type);
                    Object o = type.fromYAML(config, keys);
                    if(!type.isEmpty(o)) translation.setCache(language, o);
                });
            }
        }

        private Optional<InputStreamReader> readDefaults(Language language, String fileExtension) {
            if(!LanguagesAPI.getPluginManager(LanguagesPlugin.getInstance()).getTranslatedLanguages().contains(language)) return Optional.empty();
            String path = "resources/lang/" + language.getLocaleCode().toLowerCase() + "." + fileExtension;
            InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream(path);
            if(inputStream != null) return Optional.of(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            return Optional.empty();
        }

        public void save() throws IOException {
            for(Language language : getAvailableLanguages()){
                save(language);
            }
        }

        public void save(@Nonnull Language language) throws IOException {
            Validate.notNull(language, "Cannot save a null language.");
            Validate.isTrue(LanguagesAPI.getAvailableLanguages().contains(language), "This language is not available.");
            File folder = new File(language.getFolderPath(this));
            folder.mkdirs();
            File file = new File(language.getTranslationsFilePath(this));
            if(!file.exists()) file.createNewFile();
            YamlConfiguration config = new YamlConfiguration();
            try{ config.load(file); } catch (Exception e){ DebugManager.print(e); }
            config.options().header(language.getName() + " - " + language.getConsoleName() + " translations." + "\nThis plugin uses LanguagesAPI by SergiFerry\n" + LanguagesPlugin.getInstance().getSpigotResource());
            boolean canBeComments = SpigotPlugin.getServerVersion().isNewerThanOrEqual(ServerVersion.VERSION_1_18);
            if(canBeComments) config.options().width(Integer.MAX_VALUE);
            for(Translation translation : getTranslationsValues()){
                if(!translation.canBeModified()) continue;
                translation.getType().toYAML(translation.getCache(language), config, translation.getSimpleKey().toUpperCase());
                if(!canBeComments) continue;
                String defaultTranslationComment = null;
                if(translation.isTranslatedIn(getDefaultLanguage())) defaultTranslationComment = translation.getType().toYAMLComment(translation.getCache(getDefaultLanguage()));
                if(isCommentDefaultTranslations() && !getDefaultLanguage().equals(language) && defaultTranslationComment != null && !defaultTranslationComment.contains("\n"))
                    config.setComments(translation.getSimpleKey().toUpperCase(), Arrays.asList("Default translation for " + translation.getSimpleKey().toUpperCase() + " in " + getDefaultLanguage().getEnglishName() + ":", "'" + defaultTranslationComment.replaceAll("§", "&") + "'"));
                else config.setComments(translation.getSimpleKey().toUpperCase(), null);
            }
            config.save(file);
            Bukkit.getConsoleSender().sendMessage(LanguagesPlugin.getPluginManager().getStringTranslation("console.save.language").getInServerDefaultLanguage().formatted(language.getConsoleName(), plugin.getName()));
        }

        public void saveDefaults() throws IOException { for(Language language : translatedLanguages) saveDefaults(language); }

        public void saveDefaults(Language language) throws IOException {
            boolean isLanguagesPlugin = LanguagesPlugin.getInstance().equals(plugin);
            List<Language> globalLanguages = isLanguagesPlugin ? translatedLanguages : LanguagesAPI.getAvailableLanguages();
            if(!globalLanguages.contains(language)) return;
            File file = new File(language.getTranslationsFilePath(this));
            if(!file.exists()){
                save(language);
                return;
            }
            YamlConfiguration config = new YamlConfiguration();
            try { config.load(file); }
            catch (InvalidConfigurationException e) {
                save(language);
                return;
            }
            for (Translation translation : getTranslationsValues()) {
                if (!config.contains(translation.getSimpleKey().toUpperCase())){
                    save(language);
                    break;
                }
            }
        }

        public void uploadTranslations(Language language, Consumer<Translation> finishTranslation, Runnable complete) throws NotExternalDatabaseException {
            if(getDatabaseType().isLocal()) throw new NotExternalDatabaseException();
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                for(Translation translation : getTranslationsValues()){
                    if(!translation.isTranslatedIn(language)) continue;
                    try { translation.uploadToDatabase(getDatabase(), language); }
                    catch (SQLException e) { DebugManager.print(e); }
                    catch (NotExternalDatabaseException e) {}
                    if(finishTranslation != null) finishTranslation.accept(translation);
                }
                if(complete != null) complete.run();
            });
        }

        public void uploadTranslations(Consumer<Translation> finishTranslation, Runnable complete) throws NotExternalDatabaseException {
            if(getDatabaseType().isLocal()) throw new NotExternalDatabaseException();
            Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
                for(Translation translation : getTranslationsValues()){
                    translation.getTranslatedLanguages().forEach(language -> {
                        try { translation.uploadToDatabase(getDatabase(), (Language) language); }
                        catch (SQLException e) { DebugManager.print(e); }
                        catch (NotExternalDatabaseException e) {}
                    });
                    if(finishTranslation != null) finishTranslation.accept(translation);
                }
                if(complete != null) complete.run();
            });
        }


        public CompletableFuture<Set<Translation>> downloadTranslations() throws NotExternalDatabaseException {
            if(getDatabaseType().isLocal()) throw new NotExternalDatabaseException();
            CompletableFuture<Set<Translation>> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(LanguagesPlugin.getInstance(), () -> {
                Map<Language, Map<NamespacedKey, Pair<JsonArray, Translation.Type>>> map = null;
                try { map = getDatabase().getPluginTranslationsTable().get(plugin); }
                catch (SQLException e) { future.completeExceptionally(e); }
                catch (NotExternalDatabaseException e) {}
                if(map == null || map.isEmpty()){
                    future.completeExceptionally(new EmptyResultException());
                    return;
                }
                if(future.isCancelled() || future.isDone()) return;
                Set<Translation> total = new HashSet<>();
                for(Language language : map.keySet()){
                    for(NamespacedKey key : map.get(language).keySet()){
                        Pair<JsonArray, Translation.Type> pair = map.get(language).get(key);
                        Translation.Type type = pair.getSecond();
                        Object object = type.fromJson(pair.getFirst());
                        Translation translation = getTranslation(key.getKey(), type);
                        translation.setCache(language, object);
                        if(!total.contains(translation)) total.add(translation);
                    }
                }
                future.complete(total);
            });
            future.orTimeout(5, TimeUnit.SECONDS);
            return future;
        }

        public CompletableFuture<Set<Translation>> downloadTranslations(Language language) throws NotExternalDatabaseException {
            if(getDatabaseType().isLocal()) throw new NotExternalDatabaseException();
            CompletableFuture<Set<Translation>> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(LanguagesPlugin.getInstance(), () -> {
                Map<NamespacedKey, Pair<JsonArray, Translation.Type>> map = null;
                try { map = getDatabase().getPluginTranslationsTable().get(language, plugin); }
                catch (SQLException e) { DebugManager.print(e); future.completeExceptionally(e); }
                catch (NotExternalDatabaseException e) {}
                if(map == null || map.isEmpty()){
                    future.completeExceptionally(new EmptyResultException());
                    return;
                }
                if(future.isCancelled() || future.isDone()) return;
                Set<Translation> total = new HashSet<>();
                for(NamespacedKey key : map.keySet()){
                    Pair<JsonArray, Translation.Type> pair = map.get(key);
                    Translation.Type type = pair.getSecond();
                    Object object = type.fromJson(pair.getFirst());
                    Translation translation = getTranslation(key.getKey(), type);
                    translation.setCache(language, object);
                    if(!total.contains(translation)) total.add(translation);
                }
                future.complete(total);
            });
            future.orTimeout(5, TimeUnit.SECONDS);
            return future;
        }

        @Nonnull public boolean isLanguageAvailable(Language language){
            return translatedLanguages.contains(language);
        }

        public void addLanguageAvailable(@Nonnull Language language){
            Validate.notNull(language, "Language cannot be null");
            if(translatedLanguages.contains(language)) return;
            translatedLanguages.add(language);
        }

        public void addLanguageAvailable(Language... language){ Arrays.stream(language).forEach(x-> addLanguageAvailable(x)); }

        @Nonnull public List<Language> getTranslatedLanguages() { return translatedLanguages; }

        public Map<Language, YamlConfiguration> getLanguagesYAML() { return languagesYAML; }

        public List<Language> getUnavailableLanguages() { return unavailableLanguages; }

        public void setFallbackToDefaultLanguage(boolean fallbackToDefault) { this.fallbackToDefault = fallbackToDefault; }

        public boolean isFallbackToDefaultLanguage() { return fallbackToDefault; }

        public boolean isAutoColorTranslations() { return autoColorTranslations; }

        public void setAutoColorTranslations(boolean autoColorTranslations) { this.autoColorTranslations = autoColorTranslations; }

        public boolean isFallbackToSimilarLanguage() { return fallbackToSimilar; }

        public void setFallbackToSimilarLanguage(boolean fallbackToSimilar) { this.fallbackToSimilar = fallbackToSimilar; }

        public boolean isFallbackToServerLanguage() { return fallbackToServerLanguage; }

        public void setFallbackToServerLanguage(boolean fallbackToServerLanguage) { this.fallbackToServerLanguage = fallbackToServerLanguage; }

        public boolean isUseFormattedPlaceholders() { return useFormattedPlaceholders; }

        public void setUseFormattedPlaceholders(boolean useFormattedPlaceholders) { this.useFormattedPlaceholders = useFormattedPlaceholders; }

        public boolean isSaveOnDisable() { return saveOnDisable; }

        public void setSaveOnDisable(boolean saveOnDisable) { this.saveOnDisable = saveOnDisable; }

        @Nonnull public Set<String> getTranslationsKeys() { return translations.keySet(); }
        @Nonnull public Collection<Translation> getTranslationsValues() { return translations.values(); }
        @Nonnull public Set<Map.Entry<String, Translation>> getTranslationsEntrySet() { return translations.entrySet(); }

        /*
            GET TRANSLATIONS
         */

        @Nonnull
        public boolean hasTranslation(@Nonnull String simpleKey){
            Validate.notNull(simpleKey, "Simple Key cannot be null");
            return translations.containsKey(simpleKey.toLowerCase());
        }

        /**
         * Gets the {@code Translation.Type<T>} instance of the {@code Translation<T>} instance with that {@code simpleKey}.
         *
         * @param simpleKey Translation simpleKey
         * @return {@code Translation.Type<T>} instance
         * @throws IllegalArgumentException if parameter {@code translation} is {@code null}.
         */
        public <T> Optional<Translation.Type<T>> grabTranslationType(@Nonnull String simpleKey){
            Validate.notNull(simpleKey,"Simple Key cannot be null");
            Translation<T> translation = translations.getOrDefault(simpleKey.toLowerCase(), null);
            return translation == null ? Optional.empty() : Optional.of(translation.getType());
        }

        private boolean isTranslationType(@Nullable Translation.Type type1, @Nonnull Translation.Type type2){
            Validate.notNull(type2, "Cannot compare to a null type.");
            return type1 == null ? false : type1.equals(type2);
        }

        /**
         * Gets the {@code Translation.Type<T>} instance of the {@code Translation<T>} instance.
         *
         * @param translation Translation instance
         * @return {@code Translation.Type<T>} instance
         * @throws IllegalArgumentException if parameter {@code translation} is {@code null}.
         */
        @Nonnull
        public <T> Translation.Type<T> getTranslationType(@Nonnull Translation<T> translation){
            Validate.notNull(translation, "Translation cannot be null");
            return translation.getType();
        }

        /**
         * Gets or creates the {@code Translation<T>} instance if it's not created yet.
         *
         * @param simpleKey Translation simpleKey
         * @param type Translation Type
         * @return {@code Translation<T>} instance
         * @throws IllegalArgumentException if parameters {@code simpleKey} or {@code type} are {@code null}.
         */
        @Nonnull
        public <T> Translation<T> getTranslation(@Nonnull String simpleKey, @Nonnull Translation.Type<T> type) throws IllegalArgumentException{
            Validate.notNull(simpleKey, "Translation simpleKey cannot be null.");
            Validate.notNull(type, "Type cannot be null");
            simpleKey = simpleKey.toLowerCase();
            Translation translation = translations.getOrDefault(simpleKey, createTranslation(simpleKey, type));
            if(translation == null) throw new IllegalStateException("Translation '" + simpleKey + "' was not found and not created.");
            if(!translation.canBeModified()) throw new IllegalStateException("This translation cannot be modified.");
            Validate.isTrue(translation.getType().equals(type), "The translation type " + type.getName() + " is not the same as the correct " + translation.getType().getName() +". (Translation key: " + translation.getKey() + ")");
            return translation;
        }

        @Nonnull
        public <T> Translation.Unmodifiable<T> getUnmodifiableTranslation(@Nonnull String simpleKey, @Nonnull Translation.Type<T> type) throws IllegalArgumentException{
            Validate.notNull(simpleKey, "Translation simpleKey cannot be null.");
            Validate.notNull(type, "Type cannot be null");
            simpleKey = simpleKey.toLowerCase();
            Translation translation = translations.getOrDefault(simpleKey, createUnmodifiableTranslation(simpleKey, type));
            if(translation == null) throw new IllegalStateException("Translation '" + simpleKey + "' was not found and not created.");
            if(translation.canBeModified()) throw new IllegalStateException("Translation '" + simpleKey + "' is not unmodifiable.");
            Validate.isTrue(translation.getType().equals(type), "The translation type " + type.getName() + " is not the same as the correct " + translation.getType().getName() +". (Translation key: " + translation.getKey() + ")");
            return (Translation.Unmodifiable<T>) translation;
        }

        public <T> Translation.Result<T> getTranslationResult(String simpleKey, Translation.Type<T> type, Language language){ return getTranslation(simpleKey, type).getResult(language); }

        // GET TRANSLATIONS STRINGS

        @Nonnull
        public boolean isStringTranslation(@Nonnull String simpleKey) { return isTranslationType(grabTranslationType(simpleKey).orElse(null), Translation.Type.STRING); }

        /**
         * Gets or creates the {@code Translation<String>} instance if it's not created yet.
         *
         * @param simpleKey Translation simpleKey
         * @return {@code Translation<String>} instance
         * @throws IllegalArgumentException if translation is created, and it's not String type.
         */
        public Translation<String> getStringTranslation(@Nonnull String simpleKey) throws IllegalArgumentException{ return getTranslation(simpleKey, Translation.Type.STRING); }

        public Function<Language, String> getFutureStringTranslation(@Nonnull String simpleKey){ return getStringTranslation(simpleKey).getFutureTranslation(); }

        public Function<Player, String> getFutureStringTranslationForPlayer(@Nonnull String simpleKey){ return getStringTranslation(simpleKey).getFutureTranslationForPlayer(); }

        @Nonnull
        public String getTranslationString(@Nonnull String simpleKey, @Nullable Language language){ return getStringTranslation(simpleKey).get(language); }

        @Nonnull
        public String getTranslationString(@Nonnull String simpleKey, @Nonnull Player player){ return getStringTranslation(simpleKey).get(player); }

        @Nonnull public String getTranslationString(@Nonnull String simpleKey, @Nonnull CommandSender commandSender){ return getStringTranslation(simpleKey).get(commandSender); }

        @Nonnull
        public String getTranslationString(@Nonnull String simpleKey){ return getTranslationString(simpleKey, (Language) null); }

        // GET TRANSLATION LISTS

        public boolean isListTranslation(@Nonnull String simpleKey) { return isTranslationType(grabTranslationType(simpleKey).orElse(null), Translation.Type.LIST); }

        /**
         * @param simpleKey Translation simpleKey
         * @return {@code Translation<List<String>>} instance
         * @throws IllegalArgumentException if translation is not List type.
         */
        public Translation<List<String>> getListTranslation(@Nonnull String simpleKey) throws IllegalArgumentException{ return getTranslation(simpleKey, Translation.Type.LIST); }

        @Nonnull
        public List<String> getTranslationList(@Nonnull String simpleKey, @Nullable Language language){ return getTranslation(simpleKey, Translation.Type.LIST).get(language); }

        @Nonnull
        public List<String> getTranslationList(String simpleKey, @Nonnull Player player){
            Validate.notNull(player, "Player cannot be null.");
            return getTranslationList(simpleKey, getLanguage(player));
        }

        @Nonnull
        public List<String> getTranslationList(@Nonnull String simpleKey){ return getTranslationList(simpleKey, (Language) null); }

        // GET FUTURE TRANSLATIONS

        @Nonnull
        public <T> Function<Player, T> getTranslationWithFuturePlayer(String simpleKey, Translation.Type type){ return (x) -> (T) getTranslation(simpleKey, type).getFutureTranslationForPlayer(); }

        @Nonnull
        public <T> Function<Language, T> getTranslationWithFutureLanguage(String simpleKey, Translation.Type type){ return (x) -> (T) getTranslation(simpleKey, type).getFutureTranslation(); }

        /*
            SET TRANSLATIONS
         */

        @Nullable
        /**
         * Creates a new translation instance, only if it's not created yet.
         * Returns the new translation instance if the simpleKey is not used yet, otherwise null.
         *
         * @return the new translation instance or {@code null} if the simpleKey is used yet.
         */
        public <T> Translation<T> createTranslation(String simpleKey, Translation.Type<T> type){
            if(hasTranslation(simpleKey)) return null;
            return translations.put(simpleKey.toLowerCase(), new Translation(this, simpleKey, type));
        }

        public <T> Translation.Unmodifiable<T> createUnmodifiableTranslation(String simpleKey, Translation.Type<T> type){
            if(hasTranslation(simpleKey)) throw new IllegalStateException("This translation is already created.");
            return (Translation.Unmodifiable<T>) translations.put(simpleKey.toLowerCase(), new Translation.Unmodifiable(this, simpleKey, type));
        }

        public <T> void setTranslation(Language language, String simpleKey, Translation.Type<T> type, T msg){ getTranslation(simpleKey, type).set(language, msg); }

        public void setTranslationString(Language language, String simpleKey, String msg){ setTranslation(language, simpleKey, Translation.Type.STRING, msg); }

        public void setTranslationList(Language language, String simpleKey, List<String> msg){ setTranslation(language, simpleKey, Translation.Type.LIST, msg); }

        /*
            DEFAULT TRANSLATIONS
         */

        public <T> void setDefaultTranslation(Language language, String simpleKey, Translation.Type<T> type, T msg){ getTranslation(simpleKey, type).setDefault(language, msg); }

        public void setDefaultTranslationString(Language language, String simpleKey, String msg){ setDefaultTranslation(language, simpleKey, Translation.Type.STRING, msg); }

        public void setDefaultTranslationString(String simpleKey, String msg){ setDefaultTranslationString(defaultLanguage, simpleKey, msg); }

        public void setDefaultTranslationList(Language language, String simpleKey, List<String> msg){ setDefaultTranslation(language, simpleKey, Translation.Type.LIST, msg); }

        public void setDefaultTranslationList(String simpleKey, List<String> msg){ setDefaultTranslationList(defaultLanguage, simpleKey, msg); }

        public void setDefaultTranslations(Language language, @Nonnull HashMap<String, Object> translations){ translations.entrySet().forEach(x-> Translation.Type.grabTypeByObject(x.getValue()).ifPresent(t -> setDefaultTranslation(language, x.getKey(), t, x.getValue()))); }

        public void setDefaultTranslations(HashMap<String, Object> translations){ setDefaultTranslations(defaultLanguage, translations); }

        public void setDefaultTranslations(@Nonnull Language language, @Nonnull File file){
            if(!file.exists()) return;
            if(file.getName().endsWith(".yml")) setDefaultTranslations(language, YamlConfiguration.loadConfiguration(file));
            else if(file.getName().endsWith(".json")) {
                try { setDefaultTranslations(language, new JsonParser().parse(new FileReader(file)).getAsJsonObject()); }
                catch (FileNotFoundException e) { DebugManager.print(e); }
            }
        }

        public void setDefaultTranslations(@Nonnull Language language, @Nonnull JsonObject jsonObject){
            HashMap<String, Object> defaults = new HashMap<>();
            for(Map.Entry<String, JsonElement> entry : jsonObject.entrySet()){
                String s = entry.getKey();
                JsonElement e = entry.getValue();
                if(e.isJsonPrimitive()){
                    JsonPrimitive p = e.getAsJsonPrimitive();
                    if(p.isString()) defaults.put(s, p.getAsString());
                }
                else if(e.isJsonArray()){
                    JsonArray a = e.getAsJsonArray();
                    List<String> list = new ArrayList<>();
                    for (Iterator<JsonElement> it = a.iterator(); it.hasNext(); ) {
                        JsonElement e1 = it.next();
                        if(e1.isJsonPrimitive()){
                            JsonPrimitive p1 = e1.getAsJsonPrimitive();
                            if(p1.isString()) list.add( p1.getAsString());
                        }
                    }
                    defaults.put(s, list);
                }
            }
            setDefaultTranslations(language, defaults);
        }

        public void setDefaultTranslation(@Nonnull Language language, String simpleKey, FileConfiguration fileConfiguration, String configKey){
            Translation.Type.grabTypeFromYAML(fileConfiguration, configKey).ifPresent(type -> setDefaultTranslation(language, simpleKey, type, type.fromYAML(fileConfiguration, configKey)));
        }

        public void setDefaultTranslations(FileConfiguration fileConfiguration){ setDefaultTranslations(defaultLanguage, fileConfiguration); }

        public void setDefaultTranslations(Language language, ConfigurationSection section){
            HashMap<String, Object> defaults = new HashMap<>();
            for(String s : section.getKeys(true)) Translation.Type.grabTypeFromYAML(section, s).ifPresent(type -> defaults.put(s, type.fromYAML(section, s)));
            setDefaultTranslations(language, defaults);
        }

        public void setDefaultTranslations(ConfigurationSection section) { setDefaultTranslations(defaultLanguage, section); }

    }

}
