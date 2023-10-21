package dev.sergiferry.languages.api;

import com.google.gson.JsonArray;
import dev.sergiferry.languages.api.exceptions.NotExternalDatabaseException;
import dev.sergiferry.languages.api.minecraft.MinecraftTranslation;
import dev.sergiferry.languages.debug.DebugManager;
import dev.sergiferry.languages.integration.IntegrationsManager;
import dev.sergiferry.languages.storage.Database;
import dev.sergiferry.languages.utils.StringUtils;
import dev.sergiferry.languages.utils.TriConsumer;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import oshi.util.tuples.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class Translation<T> implements Supplier<T>, Keyed {

    private final static Pattern PLACEHOLDER_NAME_PATTERN = Pattern.compile("([^\\s{}]+)");
    private final static Pattern PLACEHOLDER_PATTERN = Pattern.compile("[{]" + PLACEHOLDER_NAME_PATTERN.pattern() + "}");

    public static class Type<T> {

        private static final Map<String, Type> TYPE_BY_NAME;
        private static final Map<Class, Type> TYPE_BY_CLASS;

        static{
            TYPE_BY_NAME = new HashMap<>();
            TYPE_BY_CLASS = new HashMap<>();
        }

        public static Optional<Type> grabTypeByObject(Object o){ return grabTypeByClass(o.getClass()); }
        public static Optional<Type> grabTypeByClass(Class c){ return TYPE_BY_CLASS.keySet().stream().filter(x-> x.isAssignableFrom(c)).map(x-> TYPE_BY_CLASS.get(x)).findFirst(); }
        public static Optional<Type> grabTypeByName(@Nonnull String s){ return Optional.ofNullable(TYPE_BY_NAME.getOrDefault(s.toUpperCase(), null)); }

        public static Optional<Type> grabTypeFromYAML(ConfigurationSection section, String key){ return TYPE_BY_NAME.values().stream().filter(x-> x.isType(section, key)).findFirst(); }

        public static final Type<String> STRING = new Type<>(
                String.class,
                true,
                string -> String.valueOf(string),
                string -> string,
                string -> string,
                string -> string.isBlank(),
                (string, section, key) -> {
                    if(string == null) string = "";
                    else string = string.replaceAll("§", "&");
                    section.set(key, string);
                },
                (section, key) -> section.getString(key),
                (section, key) -> section.isString(key),
                string -> string,
                string -> {
                    JsonArray array = new JsonArray();
                    array.add(string);
                    return array;
                },
                array -> array.get(0).getAsString()
        );
        public static final Type<List<String>> LIST = new Type(
                List.class,
                true,
                list ->  StringUtils.getStringFromList((List<String>) list, StringUtils.StringListSeparator.NEW_LINE),
                string -> Arrays.asList((String) string),
                list -> {
                    List<String> stringList = new ArrayList<>();
                    stringList.addAll((List<String>) list);
                    return stringList;
                },
                list -> ((List<String>) list).isEmpty(),
                (list, section, key) -> {
                    List<String> arrayList = ((List<String>) list);
                    if(arrayList == null) arrayList = new ArrayList<>();
                    else arrayList = StringUtils.replaceAll(arrayList,"§", "&");
                    ((ConfigurationSection) section).set((String) key, arrayList);
                },
                (section, key) -> ((ConfigurationSection) section).getStringList((String) key),
                (section, key) -> ((ConfigurationSection) section).isList((String) key),
                list -> StringUtils.getStringFromList((List<String>) list, StringUtils.StringListSeparator.COMMA),
                list -> {
                    JsonArray array = new JsonArray();
                    ((List<String>) list).forEach(x-> array.add(x));
                    return array;
                },
                array -> {
                    List<String> list = new ArrayList<>();
                    ((JsonArray) array).forEach(x-> list.add(x.getAsString()));
                    return list;
                }
        );

        private Class<T> typeClass;
        private String name;
        private boolean containsText;
        private Function<T, String> toString;
        private Function<String, T> fromString;
        private Function<T, T> clone;
        private Function<T, Boolean> isEmpty;
        private TriConsumer<T, ConfigurationSection, String> toYML;
        private BiFunction<ConfigurationSection, String, T> fromYML;
        private BiFunction<ConfigurationSection, String, Boolean> typeFromYML;
        private Function<T, String> toYMLComment;
        private Function<T, JsonArray> toJson;
        private Function<JsonArray, T> fromJson;

        private Type(
                Class<T> typeClass,
                boolean containsText,
                Function<T, String> toString,
                Function<String, T> fromString,
                Function<T, T> clone,
                Function<T, Boolean> isEmpty,
                TriConsumer<T, ConfigurationSection, String> toYML,
                BiFunction<ConfigurationSection, String, T> fromYML,
                BiFunction<ConfigurationSection, String, Boolean> typeFromYML,
                Function<T, String> toYMLComment,
                Function<T, JsonArray> toJson,
                Function<JsonArray, T> fromJson
        ){
            this.typeClass = typeClass;
            this.name = typeClass.getSimpleName().toUpperCase();
            this.containsText = containsText;
            this.clone = clone;
            this.isEmpty = isEmpty;
            this.toString = toString;
            this.fromString = fromString;
            this.toYML = toYML;
            this.fromYML = fromYML;
            this.typeFromYML = typeFromYML;
            this.toYMLComment = toYMLComment;
            this.toJson = toJson;
            this.fromJson = fromJson;
            TYPE_BY_NAME.put(name, this);
            TYPE_BY_CLASS.put(typeClass, this);
        }

        public String getName() { return name; }

        public boolean containsText() { return containsText; }

        @Override public String toString(){ return getName(); }

        public T fromString(String s){ return fromString.apply(s); }

        public Class<T> getTypeClass() { return typeClass; }

        public boolean isEmpty(T t){ return t == null || isEmpty.apply(t); }

        public T clone(T pointer){ return clone.apply(pointer); }

        public JsonArray toJson(T t){ return toJson.apply(t); }

        public T fromJson(JsonArray array){ return fromJson.apply(array); }

        public void toYAML(T t, ConfigurationSection section, String key) { toYML.accept(t, section, key); }

        public T fromYAML(ConfigurationSection section, String key){ return fromYML.apply(section, key); }

        public String toYAMLComment(T t) { return toYMLComment.apply(t); }

        public boolean isType(ConfigurationSection section, String key){ return section.contains(key) && typeFromYML.apply(section, key); }

        public String toString(T t) {
            return toString.apply(t);
        }

    }

    private final LanguagesAPI.PluginManager pluginManager;
    private final Map<Language, T> values;
    private final NamespacedKey key;
    private final Type<T> type;
    private final List<Placeholder> placeholders;

    protected Translation(LanguagesAPI.PluginManager pluginManager, String simpleKey, Type<T> type) {
        this.pluginManager = pluginManager;
        this.key = new NamespacedKey(pluginManager.getPlugin(), simpleKey);
        this.values = new HashMap<>();
        this.type = type;
        this.placeholders = new ArrayList<>();
    }

    public void setDefault(@Nonnull Language language, @Nonnull T value){
        values.putIfAbsent(language, value);
        if(language.equals(pluginManager.getDefaultLanguage())) checkPlaceholders(type.toString.apply(value));
    }

    private void checkPlaceholders(String string){
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(string);
        while (matcher.find()) {
            String placeholderWithBrackets = matcher.group();
            String placeholderWithoutBrackets = placeholderWithBrackets.substring(1, placeholderWithBrackets.length()-1);
            int startIndex = matcher.start();
            if(startIndex >= 2 && string.substring(startIndex - 2, startIndex).equals("$T")) continue;
            if(startIndex >= 3 && string.substring(startIndex - 3, startIndex).equals("$MC")) continue;
            if(!placeholders.contains(placeholderWithoutBrackets)) placeholders.add(new Placeholder(placeholderWithoutBrackets, Placeholder.Type.STRING));
        }
    }

    public record Placeholder(String tag, Type type){
        public enum Type{ STRING }
    }

    public void set(@Nonnull Language language, @Nonnull T value){
        setCache(language, value);
        Bukkit.getScheduler().runTaskAsynchronously(pluginManager.getPlugin(), () -> {
            try { uploadToDatabase(LanguagesAPI.getDatabase(), language); }
            catch (SQLException e) { DebugManager.print(e); } catch (NotExternalDatabaseException e) {}
        });
    }

    protected void uploadToDatabase(Database database, Language language) throws SQLException, NotExternalDatabaseException {
        if(LanguagesAPI.getDatabaseType().isLocal()) throw new NotExternalDatabaseException();
        if(!isTranslatedIn(language)) return;
        database.getPluginTranslationsTable().set(getKey(), language, type.toJson(getCache(language)), type);
    }

    protected void downloadFromDatabase(Database database, Language language) throws SQLException, NotExternalDatabaseException {
        if(LanguagesAPI.getDatabaseType().isLocal()) throw new NotExternalDatabaseException();
        database.getPluginTranslationsTable().get(getKey(), language).ifPresent(array -> setCache(language, type.fromJson(array)));
    }

    public void setCache(@Nonnull Language language, @Nonnull T value){ values.put(language, value); }

    public T getCache(@Nonnull Language language){ return values.get(language); }

    public boolean isTranslatedIn(@Nonnull Language language) { return values.containsKey(language); }

    public boolean isTranslatedIn(@Nonnull Collection<Language> languages){ return languages.stream().filter(x-> isTranslatedIn(x)).findFirst().isPresent(); }

    public T get(@Nonnull CommandSender commandSender){
        Validate.notNull(commandSender, "CommandSender cannot be null.");
        if(commandSender instanceof Player) return get((Player) commandSender);
        return get();
    }

    public T get(@Nonnull Player player) {
        Validate.notNull(player, "Player cannot be null.");
        return (T) getResult(LanguagesAPI.getLanguage(player)).replacePlaceholderAPI(player).getFinalResult();
    }

    public T get(@Nonnull Language language){
        return getResult(language).getFinalResult();
    }

    public T getPlainResult(@Nonnull Language language) { return getResult(language).getPlainResult(); }

    public T getInServerDefaultLanguage() { return get(LanguagesAPI.getServerLanguage()); }

    public T getInPluginDefaultLanguage() { return get(pluginManager.getDefaultLanguage()); }

    @Override public T get() { return getInServerDefaultLanguage(); }

    public Function<Player, T> getFutureTranslationForPlayer(){ return player -> get(player); }

    public Function<Language, T> getFutureTranslation(){ return language -> get(language); }

    public Result<T> getResult(Language language){ return new FindTask<T>(pluginManager, this,language).getResult(); }

    public Result<T> getResult(Player player){ return getResult(LanguagesAPI.getLanguage(player)); }

    public Function<Player, Result<T>> getFutureResultForPlayer(){ return player -> getResult(player); }

    public Function<Language, Result<T>> getFutureResult(){ return language -> getResult(language); }

    public Type<T> getType() { return type; }

    public Set<Language> getTranslatedLanguages() { return values.keySet(); }

    public LanguagesAPI.PluginManager getPluginManager() {
        return pluginManager;
    }

    public boolean canBeModified(){ return !(this instanceof Translation.Unmodifiable<T>); }

    public String getSimpleKey() { return key.getKey(); }

    public List<Placeholder> getPlaceholders() { return placeholders; }

    @Nonnull @Override public NamespacedKey getKey() { return key; }

    public static class Unmodifiable<T> extends Translation<T>{

        protected Unmodifiable(LanguagesAPI.PluginManager pluginManager, String simpleKey, Type<T> type) { super(pluginManager, simpleKey, type); }

        @Override @Deprecated public void set(@Nonnull Language language, @Nonnull T value) { throw new IllegalStateException("This translation is unmodifiable"); }
    }

    private class FindTask<T>{

        private final LanguagesAPI.PluginManager pluginManager;
        private final Language initialLanguage;
        private final Translation translation;
        private final Result result;

        protected FindTask(LanguagesAPI.PluginManager pluginManager, Translation translation, Language language) {
            this.pluginManager = pluginManager;
            this.translation = translation;
            this.initialLanguage = language;
            Pair<Language, T> resulted = task(initialLanguage);
            result = new Result(pluginManager, translation, resulted.getA(), resulted.getB());
        }

        @Nonnull
        private Pair<Language, T> task(Language language){
            if(language == null){
                if(pluginManager.isFallbackToDefaultLanguage()) return new Pair<>(pluginManager.getDefaultLanguage(), (T) translation.get(pluginManager.getDefaultLanguage()));
                if(translation.isTranslatedIn(LanguagesAPI.getServerLanguage())) return new Pair<> (LanguagesAPI.getServerLanguage(), (T) translation.get(LanguagesAPI.getServerLanguage()));
            }
            else{
                if(translation.isTranslatedIn(language)) return new Pair<>(language, (T) translation.getCache(language));
                if(pluginManager.isFallbackToSimilarLanguage()){
                    Language similar = language.getSimilarLanguages().stream().filter(x-> !x.equals(language) && translation.isTranslatedIn(x)).findFirst().orElse(null);
                    if(similar != null) return new Pair<>(similar, (T) translation.getCache(similar));
                }
                if(pluginManager.isFallbackToServerLanguage() && !language.equals(LanguagesAPI.getServerLanguage()) && translation.isTranslatedIn(LanguagesAPI.getServerLanguage())) return new Pair<>(LanguagesAPI.getServerLanguage(), (T) translation.getCache(LanguagesAPI.getServerLanguage()));
                if(pluginManager.isFallbackToDefaultLanguage() && !language.equals(pluginManager.getDefaultLanguage())) return new Pair<>(pluginManager.getDefaultLanguage(), (T) translation.get(pluginManager.getDefaultLanguage()));
            }
            // Return code
            if(translation.getType().containsText()) return new Pair<>(null, (T) translation.getType().fromString(translation.getKey().toString()));
            // Throw error
            throw new IllegalStateException("Impossible to return translation.");
        }

        @Nullable
        public Language getInitialLanguage() {
            return initialLanguage;
        }

        @Nonnull
        public Translation getMessage() {
            return translation;
        }

        @Nonnull
        public Result getResult(){
            return result;
        }

        @Nonnull
        public LanguagesAPI.PluginManager getPluginManager() { return pluginManager; }
    }

    public static class Result<T>{

        private final LanguagesAPI.PluginManager pluginManager;
        private final Language language;
        private final Translation<T> translation;
        private final T plainResult;
        private T replacedResult;

        protected Result(LanguagesAPI.PluginManager pluginManager, Translation<T> translation, @Nullable Language language, @Nullable T result){
            Validate.notNull(translation, "Message cannot be null.");
            this.pluginManager = pluginManager;
            this.translation = translation;
            this.language = language;
            this.plainResult = translation.getType().clone(result);
            this.replacedResult = translation.getType().clone(result);
            replaceAnotherTranslations();
            replaceMinecraftTranslations();
            if(translation.getPluginManager().isAutoColorTranslations()) replaceChatColors();
            replacePlaceholderTags();
        }

        private Result replacePlaceholderTags(){
            if(!pluginManager.isUseFormattedPlaceholders()) return this;
            if(hasNoResult()) return this;
            if(!translation.getType().containsText()) return this;
            if(translation.getPlaceholders().isEmpty()) return this;
            if(translation.getType().equals(Type.STRING)) replacedResult = (T) replacePlaceholderTags((String) replacedResult);
            if(translation.getType().equals(Type.LIST)){
                List<String> toReplace = (List<String>) replacedResult;
                for(int i = 0; i < toReplace.size(); i++){
                    String replaced = replacePlaceholderTags(toReplace.get(i));
                    toReplace.set(i, replaced);
                }
                replacedResult = (T) toReplace;
            }
            return this;
        }

        private String replacePlaceholderTags(String string){
            if(translation.getPlaceholders().isEmpty()) return string;
            //DebugManager.print("Replacing: " + string);
            for(int i = 1; i <= translation.placeholders.size(); i++){
                String placeholderTag = translation.placeholders.get(i - 1).tag();
                //DebugManager.print("Index " + i + " -> Tag: " + placeholderTag);
                string = string.replace("{" + placeholderTag + "}", "%" + i + "$s");
            }
            //DebugManager.print("Replaced: " + string);
            return string;
        }

        public Result replacePlaceholderAPI(Player player){
            if(hasNoResult()) return this;
            if(!translation.getType().containsText()) return this;
            if(!IntegrationsManager.isUsingPlaceholderAPI()) return this;
            if(translation.getType().equals(Type.STRING)) replacedResult = (T) IntegrationsManager.getPlaceholderAPI().replace(player, (String) replacedResult);
            if(translation.getType().equals(Type.LIST)) replacedResult = (T) IntegrationsManager.getPlaceholderAPI().replace(player, (List<String>) replacedResult);
            return this;
        }

        public Result replaceAnotherTranslations(){
            if(hasNoResult()) return this;
            if(!translation.getType().containsText()) return this;
            if(translation.getType().equals(Type.STRING)) replacedResult = (T) regexAnotherTranslations((String) replacedResult);
            if(translation.getType().equals(Type.LIST)){
                List<String> toReplace = (List<String>) replacedResult;
                for(int i = 0; i < toReplace.size(); i++){
                    String replaced = regexAnotherTranslations(toReplace.get(i));
                    toReplace.set(i, replaced);
                }
                replacedResult = (T) toReplace;
            }
            return this;
        }

        public Result replaceMinecraftTranslations(){
            if(hasNoResult()) return this;
            if(!translation.getType().containsText()) return this;
            if(translation.getType().equals(Type.STRING)) replacedResult = (T) regexMinecraftTranslations((String) replacedResult);
            if(translation.getType().equals(Type.LIST)){
                List<String> toReplace = (List<String>) replacedResult;
                for(int i = 0; i < toReplace.size(); i++){
                    String replaced = regexMinecraftTranslations(toReplace.get(i));
                    toReplace.set(i, replaced);
                }
                replacedResult = (T) toReplace;
            }
            return this;
        }

        private String regexAnotherTranslations(String toReplace){
            if(!toReplace.contains("$T{")) return toReplace;
            StringBuilder newTemplate = new StringBuilder(toReplace);
            Matcher matcher = Pattern.compile("[$]T[{](.*?)}").matcher(toReplace);
            while (matcher.find()) {
                String placeholderWithBrackets = matcher.group();
                String placeholderWithoutBrackets = placeholderWithBrackets.substring(3, placeholderWithBrackets.length()-1);
                if(placeholderWithoutBrackets.equalsIgnoreCase(translation.getSimpleKey())) continue;
                if(!pluginManager.hasTranslation(placeholderWithoutBrackets)) continue;
                if(!pluginManager.grabTranslationType(placeholderWithoutBrackets).isPresent()) continue;
                if(!pluginManager.grabTranslationType(placeholderWithoutBrackets).get().equals(Type.STRING)) continue;
                int index = toReplace.indexOf(placeholderWithBrackets);
                if(index == -1) continue;
                String trans = pluginManager.getTranslationString(placeholderWithoutBrackets, language);
                newTemplate.replace(index, index + placeholderWithBrackets.length(), trans);
            }
            return newTemplate.toString();
        }

        private String regexMinecraftTranslations(String toReplace){
            if(!toReplace.contains("$MC{")) return toReplace;
            StringBuilder newTemplate = new StringBuilder(toReplace);
            Matcher matcher = Pattern.compile("[$]MC[{](.*?)}").matcher(toReplace);
            while (matcher.find()) {
                String placeholderWithBrackets = matcher.group();
                String placeholderWithoutBrackets = placeholderWithBrackets.substring(4, placeholderWithBrackets.length()-1);
                if(!MinecraftTranslation.containsTranslation(language, placeholderWithoutBrackets)) continue;
                String paramName = "$MC{" + placeholderWithoutBrackets + "}";
                int index = toReplace.indexOf(paramName);
                if(index == -1) continue;
                String trans = MinecraftTranslation.getTranslation(language, placeholderWithoutBrackets);
                newTemplate.replace(index, index + paramName.length(), trans);
            }
            return newTemplate.toString();
        }

        public Result replaceChatColors(){
            if(hasNoResult()) return this;
            if(!translation.getType().containsText()) return this;
            if(translation.getType().equals(Type.STRING)) replacedResult = (T) StringUtils.formatColor((String) replacedResult);
            if(translation.getType().equals(Type.LIST)){
                List<String> toReplace = (List<String>) replacedResult;
                for(int i = 0; i < toReplace.size(); i++){
                    String replaced = StringUtils.formatColor(toReplace.get(i));
                    toReplace.set(i, replaced);
                }
                replacedResult = (T) toReplace;
            }
            return this;
        }

        public Language getLanguage() { return language; }

        public Translation<T> getTranslation() { return translation; }

        public boolean hasNoResult() { return language == null; }

        public T getFinalResult() { return replacedResult; }

        public T getPlainResult() { return plainResult; }

        @Override
        public String toString(){
            if(hasNoResult()) return getTranslation().getKey().toString();
            return translation.getType().toString(replacedResult);
        }

        public String toString(StringUtils.StringListSeparator stringListSeparator){
            if(!translation.getType().equals(Type.LIST) || hasNoResult()) return toString();
            return StringUtils.getStringFromList((List<String>) replacedResult, stringListSeparator);
        }

    }
}
