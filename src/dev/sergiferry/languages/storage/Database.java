package dev.sergiferry.languages.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
import dev.sergiferry.languages.api.Translation;
import dev.sergiferry.languages.api.exceptions.NotExternalDatabaseException;
import dev.sergiferry.languages.debug.DebugManager;
import dev.sergiferry.languages.utils.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Creado por SergiFerry el 25/09/2023
 */
public abstract class Database {

    protected final String database;
    protected final Type type;
    protected Connection connection;
    protected Map<String, Table> tables;

    protected Database(String database, Type type) {
        this.database = database;
        this.type = type;
        this.connection = null;
        this.tables = new HashMap<>();
        //
        addTable(new PlayerLanguagesTable(this));
        try {
            addTable(new ServerLanguagesTable(this));
            addTable(new PluginTranslationsTable(this));
        }
        catch (NotExternalDatabaseException e) {}
    }

    public enum Type{
        SQLITE, MYSQL;

        public boolean isLocal(){ return this.equals(SQLITE); }
    }

    public abstract void connect() throws SQLException, ClassNotFoundException;

    public void addTable(Table table){ tables.put(table.getName(), table); }

    public void createTables() throws SQLException {
        for(Table table : tables.values()){
            table.create();
        }
    }

    public void disconnect() throws SQLException {
        if(this.connection == null) return;
        if(this.connection.isClosed()) return;
        this.connection.close();
    }

    public void checkConnection() throws SQLException {
        if(this.connection == null || this.connection.isClosed()) {
            try { connect(); }
            catch (ClassNotFoundException e) {
                DebugManager.print(e);
                throw new IllegalStateException("Cannot find " + type.name().toLowerCase() + " drivers");
            }
        }
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException{
        checkConnection();
        DebugManager.debug("SQL -> " + sql);
        return connection.prepareStatement(sql);
    }

    public Type getType() { return type; }

    public PlayerLanguagesTable getPlayerLanguagesTable(){ return (PlayerLanguagesTable) tables.get("player_languages"); }

    public ServerLanguagesTable getServerLanguagesTable() throws NotExternalDatabaseException {
        if(getType().isLocal()) throw new NotExternalDatabaseException();
        return (ServerLanguagesTable) tables.get("server_languages"); }

    public PluginTranslationsTable getPluginTranslationsTable() throws NotExternalDatabaseException {
        if(getType().isLocal()) throw new NotExternalDatabaseException();
        return (PluginTranslationsTable) tables.get("plugin_translations");
    }

    public static class PlayerLanguagesTable extends Table{

        private PrimaryColumn<String, UUID> uuidColumn;
        private Column<String, Language> pluginLanguageColumn;
        private Column<String, String> clientLanguageColumn;

        public PlayerLanguagesTable(Database database) {
            super(database, "player_languages");
            this.uuidColumn = new PrimaryColumn<>(this, "uuid", Column.Type.VARCHAR, 36, UUID.class, u -> u.toString(), s -> Optional.of(UUID.fromString(s)));
            this.pluginLanguageColumn = new Column<>(this, "pl_lang", Column.Type.VARCHAR, 16, Language.class, l -> l != null ? l.getLocaleCode().toLowerCase() : null, s -> Language.grabLanguage(s));
            this.clientLanguageColumn = new Column<>(this, "cl_lang", Column.Type.VARCHAR, 16, String.class, l -> l, s -> Optional.ofNullable(s));
        }

        public void set(UUID uuid, Language lang, String locale) throws SQLException {
            String sql = "REPLACE INTO " + getName() + " (" + getAllVariablesSyntax() + ")" +
                    " VALUES (" + getAllValuesEmptySyntax() + ")";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, uuidColumn.convert(uuid));
            statement.setString(2, pluginLanguageColumn.convert(lang));
            statement.setString(3, clientLanguageColumn.convert(locale));
            statement.executeUpdate();
        }

        public void set(UUID uuid, String locale) throws SQLException {
            String sql = "UPDATE " + getName() +
                    " SET " + clientLanguageColumn.getName() + " = ?" +
                    " WHERE " + uuidColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, clientLanguageColumn.convert(locale));
            statement.setString(2, uuidColumn.convert(uuid));
            statement.executeUpdate();
        }

        public Pair<Optional<Language>, Optional<String>> get(UUID uuid) throws SQLException {
            String sql = "SELECT " + pluginLanguageColumn.getName() + ", " + clientLanguageColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + uuidColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, uuidColumn.convert(uuid));
            ResultSet result = statement.executeQuery();
            if(!result.next()) return new Pair<>(Optional.empty(), Optional.empty());
            return new Pair<>(pluginLanguageColumn.grab(result), clientLanguageColumn.grab(result));
        }

        public Optional<Language> getLanguage(UUID uuid) throws SQLException {
            String sql = "SELECT " + pluginLanguageColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + uuidColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, uuidColumn.convert(uuid));
            ResultSet result = statement.executeQuery();
            if(!result.next()) return Optional.empty();
            return pluginLanguageColumn.grab(result);
        }

        public Optional<String> getLocale(UUID uuid) throws SQLException {
            String sql = "SELECT " + clientLanguageColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + uuidColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, uuidColumn.convert(uuid));
            ResultSet result = statement.executeQuery();
            if(!result.next()) return Optional.empty();
            return clientLanguageColumn.grab(result);
        }

        public void delete(UUID uuid) throws SQLException {
            String sql = "DELETE FROM " + getName() +
                    " WHERE " + uuidColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, uuidColumn.convert(uuid));
            statement.executeUpdate();
        }

    }

    public static class PluginTranslationsTable extends Table.ExternalTable {

        private Column<String, NamespacedKey> messageKeyColumn;
        private Column<String, Language> languageColumn;
        private Column<String, JsonArray> translationColumn;
        private Column<String, Translation.Type> typeColumn;

        public PluginTranslationsTable(Database database) throws NotExternalDatabaseException {
            super(database, "plugin_translations");
            this.messageKeyColumn = new Column<>(this, "messageKey", Column.Type.VARCHAR, 255, NamespacedKey.class, o -> o.toString(), s -> Optional.ofNullable(NamespacedKey.fromString(s)));
            this.languageColumn = new Column<>(this, "lang", Column.Type.VARCHAR, 16, Language.class, o -> o.getLocaleCode().toLowerCase(), s -> Language.grabLanguage(s));
            this.translationColumn = new Column<>(this, "translation", Column.Type.TEXT, JsonArray.class, o -> o.toString(), s -> Optional.of(new JsonParser().parse(s).getAsJsonArray()));
            this.typeColumn = new Column<>(this, "type", Column.Type.VARCHAR, 255, Translation.Type.class, o -> o.getName(), s -> Translation.Type.grabTypeByName(s));
        }

        public void set(NamespacedKey namespacedKey, Language language, JsonArray translations, Translation.Type type) throws SQLException {
            delete(namespacedKey, language);
            String sql = "REPLACE INTO " + getName() + " (" + getAllVariablesSyntax() + ") VALUES (" + getAllValuesEmptySyntax() + ")";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, messageKeyColumn.convert(namespacedKey));
            statement.setString(2, languageColumn.convert(language));
            statement.setString(3, translationColumn.convert(translations));
            statement.setString(4, typeColumn.convert(type));
            statement.executeUpdate();
        }

        public Optional<JsonArray> get(NamespacedKey messageKey, Language language) throws SQLException {
            String sql = "SELECT " + translationColumn.getName() +
                    " FROM " + getName() +
                    " WHERE (" + messageKeyColumn.getName() + ", " + languageColumn.getName() + ") = (?, ?)";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, messageKeyColumn.convert(messageKey));
            statement.setString(2, languageColumn.convert(language));
            ResultSet result = statement.executeQuery();
            if(!result.next()) return Optional.empty();
            return translationColumn.grab(result);
        }

        public Map<Language, Pair<JsonArray, Translation.Type>> get(NamespacedKey messageKey) throws SQLException{
            String sql = "SELECT " + languageColumn.getName() + ", " + translationColumn.getName() + ", " + typeColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + messageKeyColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, messageKeyColumn.convert(messageKey));
            ResultSet result = statement.executeQuery();
            Map<Language, Pair<JsonArray, Translation.Type>> map = new HashMap<>();
            while(result.next()){
                Language language = languageColumn.grab(result).orElse(null);
                if(language == null) continue;
                JsonArray array = translationColumn.grab(result).orElse(null);
                Translation.Type type = typeColumn.grab(result).orElse(null);
                if(array == null || type == null) continue;
                map.put(language, new Pair<>(array, type));
            }
            return map;
        }

        public Map<NamespacedKey, Pair<JsonArray, Translation.Type>> get(Language language, Plugin plugin) throws SQLException{
            String namespace = plugin.getName().toLowerCase(Locale.ROOT);
            String sql = "SELECT " + messageKeyColumn.getName() + ", " + translationColumn.getName() + ", " + typeColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + languageColumn.getName() + " = ? AND " + messageKeyColumn.getName() + " LIKE ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, languageColumn.convert(language));
            statement.setString(2, namespace + ":%");
            ResultSet result = statement.executeQuery();
            Map<NamespacedKey,  Pair<JsonArray, Translation.Type>> map = new HashMap<>();
            while(result.next()){
                NamespacedKey namespacedKey = NamespacedKey.fromString(messageKeyColumn.read(result), plugin);
                if(namespacedKey == null) continue;
                JsonArray array = translationColumn.grab(result).orElse(null);
                Translation.Type type = typeColumn.grab(result).orElse(null);
                if(array == null || type == null) continue;
                map.put(namespacedKey, new Pair<>(array, type));
            }
            return map;
        }

        public Map<Language, Map<NamespacedKey, Pair<JsonArray, Translation.Type>>> get(Plugin plugin) throws SQLException{
            String namespace = plugin.getName().toLowerCase(Locale.ROOT);
            String sql = "SELECT " + messageKeyColumn.getName() + ", " + languageColumn.getName() + ", " + translationColumn.getName() + ", " + typeColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + messageKeyColumn.getName() + " LIKE ? AND " + languageColumn.getName() + " IN (" + StringUtils.getStringFromList(LanguagesAPI.getAvailableLanguages().stream().map(x-> "'" + x.getLocaleCode().toLowerCase() + "'").toList(), StringUtils.StringListSeparator.COMMA, true) + ")";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, namespace + ":%");
            ResultSet result = statement.executeQuery();
            Map<Language, Map<NamespacedKey, Pair<JsonArray, Translation.Type>>> map = new HashMap<>();
            while(result.next()){
                Language language = languageColumn.grab(result).orElse(null);
                if(language == null || !language.isAvailable()) continue;
                map.putIfAbsent(language, new HashMap<>());
                NamespacedKey namespacedKey = NamespacedKey.fromString(messageKeyColumn.read(result), plugin);
                if(namespacedKey == null) continue;
                JsonArray array = translationColumn.grab(result).orElse(null);
                Translation.Type type = typeColumn.grab(result).orElse(null);
                if(array == null || type == null) continue;
                map.get(language).put(namespacedKey, new Pair<>(array, type));
            }
            return map;
        }


        public void delete(NamespacedKey messageKey, Language language) throws SQLException {
            String sql = "DELETE FROM " + getName() + " WHERE (" + messageKeyColumn.getName() + ", " + languageColumn.getName() + ") = (?, ?)";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, messageKeyColumn.convert(messageKey));
            statement.setString(2, languageColumn.convert(language));

            statement.executeUpdate();
        }

    }

    public static class ServerLanguagesTable extends Table.ExternalTable {

        private Column<String, String> keyColumn;
        private Column<String, String> valueColumn;

        public ServerLanguagesTable(Database database) throws NotExternalDatabaseException {
            super(database, "server_languages");
            this.keyColumn = new PrimaryColumn<>(this, "optionKey", Column.Type.VARCHAR, 255, String.class, s -> s, s-> Optional.of(s));
            this.valueColumn = new Column<>(this, "optionValue", Column.Type.TEXT, String.class, s -> s, s-> Optional.of(s));
        }

        public void set(String key, String value) throws SQLException {
            String sql = "REPLACE INTO " + getName() + " (" + getAllVariablesSyntax() + ") VALUES (" + getAllValuesEmptySyntax() + ")";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, keyColumn.convert(key));
            statement.setString(2, valueColumn.convert(value));
            statement.executeUpdate();
        }

        public Optional<String> get(String key) throws SQLException {
            String sql = "SELECT " + valueColumn.getName() +
                    " FROM " + getName() +
                    " WHERE " + keyColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, keyColumn.convert(key));
            ResultSet result = statement.executeQuery();
            if(!result.next()) return Optional.empty();
            return valueColumn.grab(result);
        }

        public void delete(String key) throws SQLException {
            String sql = "DELETE FROM " + getName() +
                    " WHERE " + keyColumn.getName() + " = ?";
            PreparedStatement statement = getDatabase().prepareStatement(sql);
            statement.setString(1, keyColumn.convert(key));
            statement.executeUpdate();
        }

    }

    public abstract static class Table{

        private Database database;
        private String name;
        private List<Column> columns;
        private Column primary;

        public Table(Database database, String name) {
            this.database = database;
            this.name = name;
            this.columns = new ArrayList<>();
            this.primary = null;
        }

        public Database getDatabase() { return database; }

        public String getName() { return name; }

        public Optional<Column> getColumn(String name) { return columns.stream().filter(x-> x.name.equals(name)).findFirst(); }

        public Optional<Column> grabPrimary() { return Optional.ofNullable(primary); }

        protected String getValuesEmptySyntax(Column... columns){
            String variables = "";
            for(Column column : columns) variables = variables + ", ?";
            return variables.replaceFirst(", ", "");
        }

        protected String getAllValuesEmptySyntax(){ return getValuesEmptySyntax(columns.toArray(new Column[columns.size()])); }

        protected String getAllVariablesSyntax(){ return getAllVariablesSyntax(false); }

        protected String getAllVariablesSyntax(boolean withType){ return getVariablesSyntax(withType, columns.toArray(new Column[columns.size()])); }

        protected String getVariablesSyntax(Column... columns) { return getVariablesSyntax(false, columns); }

        protected String getVariablesSyntax(boolean withType, Column... columns) {
            String variables = "";
            for (Column column : columns)
                variables = variables + ", " + column.name + (withType ? " " + column.columnType.format + (column.length.isPresent() ? "(" + column.length.get() + ")" : "") : "");
            return variables.replaceFirst(", ", "");
        }

        public void create() throws SQLException{
            String variables = getAllVariablesSyntax(true);
            if(primary != null) variables = variables + ", PRIMARY KEY (" + primary.name + (primary.length.isPresent() && database.getType().equals(Type.MYSQL) ? "(" + primary.length.get() + ")" : "") +  ")";
            String sql = "CREATE TABLE IF NOT EXISTS " +  name + " (" + variables + ")";
            DebugManager.debug("SQL -> " + sql);
            database.connection.createStatement().execute(sql);
        }

        public abstract static class ExternalTable extends Table{

            public ExternalTable(Database database, String name) throws NotExternalDatabaseException {
                super(database, name);
                if(database.getType().isLocal()) throw new NotExternalDatabaseException();
            }

        }
    }

    public static class Column<T, O> {

        private final Table table;
        private final String name;
        private final Type<T> columnType;
        private Optional<Integer> length;
        private final Class<O> objectType;
        private final Function<O, T> convert;
        private final Function<T, Optional<O>> reconvert;

        public Column(Table table, String name, Type<T> columnType, Class<O> objectType, Function<O, T> convert, Function<T, Optional<O>> reconvert) {
            this.table = table;
            this.name = name;
            this.columnType = columnType;
            this.objectType = objectType;
            this.convert = convert;
            this.reconvert = reconvert;
            this.length = Optional.empty();
            table.columns.add(this);
        }

        public Column(Table table, String name, Type<T> columnType, Integer length, Class<O> objectType, Function<O, T> convert, Function<T, Optional<O>> reconvert) {
            this(table, name, columnType, objectType, convert, reconvert);
            setLength(length);
        }

        public String getName() { return name; }

        public Type<T> getColumnType() { return columnType; }

        public void setLength(Integer length) { this.length = Optional.ofNullable(length); }

        public T convert(O o){ return convert.apply(o); }

        public Optional<O> reconvert(T t){
            if(t == null) return Optional.empty();
            return reconvert.apply(t);
        }

        public Optional<O> grab(ResultSet resultSet){ return reconvert(read(resultSet)); }

        @Nullable
        public T read(ResultSet resultSet){ return getColumnType().read(resultSet, getName()); }

        public static class Type<T>{

            protected static final Type<String> TEXT = new Type<>("TEXT", String.class, (resultSet, s) -> {
                try { return resultSet.getString(s); } catch (SQLException e) { DebugManager.print(e); return null; }
            });

            protected static final Type<Integer> INT = new Type<>("INT", Integer.class, (resultSet, s) -> {
                try { return resultSet.getInt(s); } catch (SQLException e) { DebugManager.print(e); return null; }
            });

            protected static final Type<String> VARCHAR = new Type<>("VARCHAR", String.class, (resultSet, s) -> {
                try { return resultSet.getString(s); } catch (SQLException e) { DebugManager.print(e); return null; }
            });

            private String format;
            private Class<T> type;
            private BiFunction<ResultSet, String, T> read;

            private Type(String format, Class<T> type, BiFunction<ResultSet, String, T> read) {
                this.format = format;
                this.type = type;
                this.read = read;
            }

            public T read(ResultSet resultSet, String column) { return read.apply(resultSet, column); }

            public String getFormat() { return format; }

            public Class<T> getType() { return type; }

        }

    }

    public static class PrimaryColumn<T, O> extends Column<T, O>{

        public PrimaryColumn(Table table, String name, Type<T> columnType, Class<O> objectType, Function<O, T> convert, Function<T, Optional<O>> reconvert) {
            super(table, name, columnType, objectType, convert, reconvert);
            Validate.isTrue(table.primary == null);
            table.primary = this;
        }

        public PrimaryColumn(Table table, String name, Type<T> columnType, Integer length, Class<O> objectType, Function<O, T> convert, Function<T, Optional<O>> reconvert) {
            this(table, name, columnType, objectType, convert, reconvert);
            setLength(length);
        }

    }

}
