package dev.sergiferry.languages.api.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.sergiferry.languages.api.Language;
import dev.sergiferry.languages.api.LanguagesAPI;
import dev.sergiferry.languages.api.minecraft.sorts.*;
import dev.sergiferry.spigot.server.ServerVersion;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scoreboard.Team;
import oshi.util.tuples.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Creado por SergiFerry el 06/09/2022
 */
public abstract class MinecraftTranslation {

    protected static final String F = "%f";
    protected static final String S = "%s";

    protected static <F> Result getResult(Language language, SingleType<F> type, F firstObject){
        SingleMinecraftTranslation<F> singleMinecraftTranslation = new SingleMinecraftTranslation<>(language, type, firstObject);
        return singleMinecraftTranslation.getTranslatedMessage();
    }

    protected static <F, S> Result getResult(Language language, BiType<F, S> type, F firstObject, S secondObject){
        BiMinecraftTranslation<F, S> biMinecraftTranslation = new BiMinecraftTranslation<>(language, type, firstObject, secondObject);
        return biMinecraftTranslation.getTranslatedMessage();
    }

    protected static <F> String getTranslation(Language language, SingleType<F> type, F firstObject){ return getResult(language, type, firstObject).getFinalResult(); }

    protected static <F, S> String getTranslation(Language language, BiType<F, S> type, F firstObject, S secondObject){ return getResult(language, type, firstObject, secondObject).getFinalResult(); }

    public static boolean existTranslation(String code) { return LangReader.existsKey(code); }
    public static boolean containsTranslation(Language language, String code) { return LangReader.containsKey(language, code); }

    public static String getTranslation(Language language, String code) { return getTranslation(language, Type.CUSTOM, code); }
    public static String getTranslation(Player player, String code) { return getTranslation(LanguagesAPI.getLanguage(player), code); }

    public static String getLanguageDetail(Language language, LanguageDetail detail){ return getTranslation(language, Type.LANGUAGE_DETAIL, detail); }
    public static String getLanguageDetail(Player player, LanguageDetail detail){ return getLanguageDetail(LanguagesAPI.getLanguage(player), detail); }

    public static String getMaterial(Language language, Material material){
        if(material.isBlock()) return getTranslation(language, Type.BLOCK, material);
        else return getTranslation(language, Type.ITEM, material);
    }
    public static String getMaterial(Player player, Material material) { return getMaterial(LanguagesAPI.getLanguage(player), material); }

    public static String getBiome(Language language, Biome biome){ return getTranslation(language, Type.BIOME, biome); }
    public static String getBiome(Player player, Biome biome){ return getBiome(LanguagesAPI.getLanguage(player), biome); }

    public static String getPotionEffect(Language language, PotionEffectType potionEffectType){ return getTranslation(language, Type.POTION_EFFECT, potionEffectType); }
    public static String getPotionEffect(Player player, PotionEffectType potionEffectType){ return getPotionEffect(LanguagesAPI.getLanguage(player), potionEffectType); }

    public static String getPotionEffect(Language language, PotionEffectType potionEffectType, Integer potency){ return getTranslation(language, Type.POTION_EFFECT, potionEffectType) + " " + getPotionPotency(language, potency); }
    public static String getPotionEffect(Player player, PotionEffectType potionEffectType, Integer potency){ return getPotionEffect(LanguagesAPI.getLanguage(player), potionEffectType, potency); }

    public static String getPotionPotency(Language language, Integer potency){
        if(potency < 1 || potency > 5) return potency.toString();
        return getTranslation(language, Type.POTION_POTENCY, potency);
    }
    public static String getPotionPotency(Player player, Integer potency){ return getPotionPotency(LanguagesAPI.getLanguage(player), potency); }

    public static String getMerchantLevel(Language language, Integer level){ return getTranslation(language, Type.MERCHANT_LEVEL, level); }
    public static String getMerchantLevel(Player player, Integer level){ return getMerchantLevel(LanguagesAPI.getLanguage(player), level); }

    public static String getEnchantment(Language language, Enchantment enchantment){ return getTranslation(language, Type.ENCHANTMENT_NAME, enchantment); }
    public static String getEnchantment(Player player, Enchantment enchantment){ return getEnchantment(LanguagesAPI.getLanguage(player), enchantment); }

    public static String getEnchantment(Language language, Enchantment enchantment, Integer level){ return getEnchantment(language, enchantment) + " " + getEnchantmentLevel(language, level); }
    public static String getEnchantment(Player player, Enchantment enchantment, Integer level){ return getEnchantment(LanguagesAPI.getLanguage(player), enchantment, level); }

    public static String getEnchantmentLevel(Language language, Integer level){
        if(level < 1 || level > 10) return level.toString();
        return getTranslation(language, Type.ENCHANTMENT_LEVEL, level);
    }
    public static String getEnchantmentLevel(Player player, Integer level){ return getEnchantmentLevel(LanguagesAPI.getLanguage(player), level); }

    public static String getEntityType(Language language, EntityType entityType){ return getTranslation(language, Type.ENTITY_TYPE, entityType); }
    public static String getEntityType(Player player, EntityType entityType){ return getEntityType(LanguagesAPI.getLanguage(player), entityType); }

    public static String getVillagerProfession(Language language, Villager.Profession profession){ return getTranslation(language, Type.ENTITY_VILLAGER_PROFESSION, profession); }
    public static String getVillagerProfession(Player player, Villager.Profession profession){ return getVillagerProfession(LanguagesAPI.getLanguage(player), profession); }

    public static String getTropicalFishType(Language language, TropicalFish.Pattern pattern){ return getTranslation(language, Type.TROPICAL_FISH_TYPE, pattern); }
    public static String getTropicalFishType(Player player, TropicalFish.Pattern pattern){ return getTropicalFishType(LanguagesAPI.getLanguage(player), pattern); }

    public static String getColor(Language language, DyeColor dyeColor){ return getTranslation(language, Type.COLOR_NAME, dyeColor); }
    public static String getColor(Player player, DyeColor dyeColor){ return getColor(LanguagesAPI.getLanguage(player), dyeColor); }

    public static String getBannerPattern(Language language, PatternType patternType, DyeColor dyeColor){ return getTranslation(language, Type.BANNER_PATTERN, patternType, dyeColor); }
    public static String getBannerPattern(Player player, PatternType patternType, DyeColor dyeColor){ return getBannerPattern(LanguagesAPI.getLanguage(player), patternType, dyeColor); }

    public static String getItemPotionName(Language language, PotionSort potionSort, PotionType potionType){ return getTranslation(language, Type.ITEM_POTION_NAME, potionSort, potionType); }
    public static String getItemPotionName(Player player, PotionSort potionSort, PotionType potionType){ return getItemPotionName(LanguagesAPI.getLanguage(player), potionSort, potionType); }

    public static String getDifficulty(Language language, Difficulty difficulty) { return getTranslation(language, Type.DIFFICULTY, difficulty); }
    public static String getDifficulty(Player player, Difficulty difficulty) { return getDifficulty(LanguagesAPI.getLanguage(player), difficulty); }

    public static String getGameMode(Language language, GameMode gameMode) { return getTranslation(language, Type.GAME_MODE, gameMode); }
    public static String getGameMode(Player player, GameMode gameMode) { return getGameMode(LanguagesAPI.getLanguage(player), gameMode); }

    public static String getWorldType(Language language, WorldType worldType) { return getTranslation(language, Type.WORLD_TYPE, worldType); }
    public static String getWorldType(Player player, WorldType worldType) { return getWorldType(LanguagesAPI.getLanguage(player), worldType); }

    public static String getShieldName(Language language, DyeColor dyeColor) { return getTranslation(language, Type.SHIELD_NAME, dyeColor); }
    public static String getShieldName(Player player, DyeColor dyeColor) { return getShieldName(LanguagesAPI.getLanguage(player), dyeColor); }

    public static String getTippedArrowName(Language language, PotionEffectType potionEffectType) { return getTranslation(language, Type.TIPPED_ARROW_EFFECT, potionEffectType); }
    public static String getTippedArrowName(Player player, PotionEffectType potionEffectType) { return getTippedArrowName(LanguagesAPI.getLanguage(player), potionEffectType); }

    public static String getStatistic(Language language, Statistic statistic) { return getTranslation(language, Type.STATISTIC, statistic); }
    public static String getStatistic(Player player, Statistic statistic) { return getStatistic(LanguagesAPI.getLanguage(player), statistic); }

    public static String getAttribute(Language language, Attribute attribute) { return getTranslation(language, Type.ATTRIBUTE, attribute); }
    public static String getAttribute(Player player, Attribute attribute) { return getAttribute(LanguagesAPI.getLanguage(player), attribute); }

    public static String getInventoryType(Language language, InventoryType inventoryType) { return getTranslation(language, Type.INVENTORY_TYPE, inventoryType); }
    public static String getInventoryType(Player player, InventoryType inventoryType) { return getInventoryType(LanguagesAPI.getLanguage(player), inventoryType); }

    public static String getTeamOption(Language language, Team.Option option, Team.OptionStatus status){ return getTranslation(language, Type.TEAM_OPTION, option, status); }
    public static String getTeamOption(Player player, Team.Option option, Team.OptionStatus status){ return getTeamOption(LanguagesAPI.getLanguage(player), option, status); }

    public static String getItemGroup(Language language, ItemGroup itemGroup){ return getTranslation(language, Type.ITEM_GROUP, itemGroup); }
    public static String getItemGroup(Player player, ItemGroup itemGroup){ return getItemGroup(LanguagesAPI.getLanguage(player), itemGroup); }

    public static String getDeathScreen(Language language, DeathScreen deathScreen){ return getTranslation(language, Type.DEATH_SCREEN, deathScreen); }
    public static String getDeathScreen(Player player, DeathScreen deathScreen){ return getDeathScreen(LanguagesAPI.getLanguage(player), deathScreen); }

    public static String getDeathMessage(Language language, DeathMessage deathMessage){ return getTranslation(language, Type.DEATH_MESSAGE, deathMessage); }
    public static String getDeathMessage(Player player, DeathMessage deathMessage){ return getDeathMessage(LanguagesAPI.getLanguage(player), deathMessage); }

    protected final Language language;
    protected final Type type;
    private String key;

    protected MinecraftTranslation(@Nullable Language language, @Nonnull Type type){
        Validate.notNull(type, "Type cannot be null");
        if(language == null) language = LanguagesAPI.getServerLanguage();
        this.language = language;
        this.type = type;
    }

    public abstract String generateKey();

    public String getKey() { return key; }

    public Result getTranslatedMessage(){
        return new FindTask(this, language).getResult();
    }

    public Type getType() { return type; }


    protected static class SingleMinecraftTranslation<F> extends MinecraftTranslation{

        protected F firstObject;

        protected SingleMinecraftTranslation(Language language, SingleType<F> type, F firstObject) {
            super(language, type);
            this.firstObject = firstObject;
            if(firstObject == null && !type.isFirstNullable()) throw new IllegalArgumentException("First object cannot be null");
            super.key = generateKey();
        }

        @Override
        public SingleType<F> getType(){ return (SingleType<F>) super.type; }

        public F getFirstObject() { return firstObject; }

        @Override
        public String generateKey() {
            String f = getType().getFirstKeyFunction().apply(firstObject);
            if(f == null || f.equals("")) throw new IllegalStateException("No first object detected correctly.");
            if(getType().getFirstTypeClass().equals(PotionEffectType.class) && f.equals("effect.none")) return f;
            if(getType().getFirstTypeClass().equals(EntityType.class) && f.equals("entity.notFound")) return f;
            return type.getKeyPattern().replaceFirst(F, f);
        }
    }

    protected static class BiMinecraftTranslation<F, S> extends MinecraftTranslation{

        protected F firstObject;
        protected S secondObject;

        protected BiMinecraftTranslation(Language language, BiType<F, S> type, F firstObject, S secondObject) {
            super(language,type);
            this.firstObject = firstObject;
            if(firstObject == null && !type.isFirstNullable()) throw new IllegalArgumentException("First object cannot be null");
            this.secondObject = secondObject;
            if(secondObject == null && !type.isSecondNullable()) throw new IllegalArgumentException("Second object cannot be null");
            super.key = generateKey();
        }

        public F getFirstObject() { return firstObject; }

        public S getSecondObject() { return secondObject; }

        @Override
        public BiType<F, S> getType(){ return (BiType<F, S>) super.type; }

        @Override
        public String generateKey() {
            String f = getType().getFirstKeyFunction().apply(firstObject, secondObject), s = getType().getSecondKeyFunction().apply(firstObject, secondObject);
            if(f == null || f.equals("")) throw new IllegalStateException("No first object detected correctly.");
            if(s == null || s.equals("")) throw new IllegalStateException("No second object detected correctly.");
            return type.getKeyPattern().replaceFirst(F, f).replaceFirst(S, s);
        }
    }


    protected static class Type{

        private static final Function<PotionEffectType, String> POTION_EFFECT_TYPE_STRING_FUNCTION = x -> {
            if(x == null || ServerVersion.getServerVersion().isOlderThanOrEqual(ServerVersion.VERSION_1_17_1)) return "effect.none";
            return x.getKey().getKey();
        };

        //

        public static final SingleType<String>                  CUSTOM                      = new SingleType<>("CUSTOM", String.class, F, f -> f);
        public static final SingleType<LanguageDetail>          LANGUAGE_DETAIL             = new SingleType<>("LANGUAGE_DETAIL", LanguageDetail.class, "language." + F, f -> {
            return f.getKey();
        });
        public static final SingleType<Biome>                   BIOME                       = new SingleType<>("BIOME", Biome.class, "biome.minecraft." + F, f -> {
            if(f.equals(Biome.CUSTOM)) throw new IllegalArgumentException("Biome CUSTOM is not supported.");
            return f.getKey().getKey();
        });
        public static final SingleType<Material>                BLOCK                       = new SingleType<>("BLOCK",Material.class, "block.minecraft." + F, f -> {
            if(!f.isBlock()) throw new IllegalArgumentException(f.name() + " is not a block");
            return f.getKey().getKey();
        });
        public static final SingleType<Material>                ITEM                        = new SingleType<>("ITEM",Material.class, "item.minecraft." + F, f -> {
            if(!f.isItem()) throw new IllegalArgumentException(f.name() + " is not an item");
            return f.getKey().getKey();
        });
        @SupportsNull(first = true)
        public static final SingleType<PotionEffectType>        POTION_EFFECT               = new SingleType<>("POTION_EFFECT", PotionEffectType.class, "effect.minecraft." + F, POTION_EFFECT_TYPE_STRING_FUNCTION);
        public static final SingleType<Integer>                 ENCHANTMENT_LEVEL           = new SingleType<>("ENCHANTMENT_LEVEL", Integer.class, "enchantment.level." + F, f -> {
            if(f < 1 || f > 10) throw new IllegalArgumentException("Enchantment level can only be from 1 to 10");
            return f.toString();
        });
        public static final SingleType<Enchantment>             ENCHANTMENT_NAME            = new SingleType<>("ENCHANTMENT_NAME", Enchantment.class, "enchantment.minecraft." + F,  f ->{
            return f.getKey().getKey();
        });
        @SupportsNull(first = true)
        public static final SingleType<EntityType>              ENTITY_TYPE                 = new SingleType<>("ENTITY_TYPE", EntityType.class, "entity.minecraft." + F, f -> {
            if(f == null || f.equals(EntityType.UNKNOWN)) return "entity.notFound";
            return f.getKey().getKey();
        });
        public static final SingleType<Villager.Profession>     ENTITY_VILLAGER_PROFESSION  = new SingleType<>("ENTITY_VILLAGER_PROFESSION", Villager.Profession.class, "entity.minecraft.villager." + F, f ->{
            return f.getKey().getKey();
        });
        public static final SingleType<TropicalFish.Pattern>    TROPICAL_FISH_TYPE          = new SingleType<>("TROPICAL_FISH_TYPE", TropicalFish.Pattern.class, "entity.minecraft.tropical_fish.type." + F, f -> {
            return f.name().toLowerCase();
        });
        public static final SingleType<DyeColor>                COLOR_NAME                  = new SingleType<>("COLOR_NAME", DyeColor.class, "color.minecraft." + F, f -> {
            return f.name().toLowerCase();
        });
        public static final SingleType<WorldType>               WORLD_TYPE                  = new SingleType<>("WORLD_TYPE", WorldType.class, "generator.minecraft." + F, f -> {
            return f.getName().toLowerCase();
        });
        public static final SingleType<Difficulty>              DIFFICULTY                  = new SingleType<>("DIFFICULTY", Difficulty.class, "options.difficulty." + F, f -> {
           return f.name().toLowerCase();
        });
        public static final SingleType<GameMode>                GAME_MODE                   = new SingleType<>("GAME_MODE", GameMode.class, "gameMode." + F, f -> {
            return f.name().toLowerCase();
        });
        public static final SingleType<DyeColor>                SHIELD_NAME                 = new SingleType<>("SHIELD_NAME", DyeColor.class, "item.minecraft.shield." + F, f -> {
            return f.name().toLowerCase();
        });
        @SupportsNull(first = true)
        public static final SingleType<PotionEffectType>        TIPPED_ARROW_EFFECT         = new SingleType<>("TIPPED_ARROW_EFFECT", PotionEffectType.class, "item.minecraft.tipped_arrow.effect." + F, POTION_EFFECT_TYPE_STRING_FUNCTION);
        public static final SingleType<Integer>                 POTION_POTENCY              = new SingleType<>("POTION_POTENCY", Integer.class, "potion.potency." + F, f -> {
            if(f < 1 || f > 5) throw new IllegalArgumentException("Potion potency can only be from 1 to 5");
            return f.toString();
        });
        public static final SingleType<Integer>                 MERCHANT_LEVEL              = new SingleType<>("MERCHANT_LEVEL", Integer.class, "merchant.level." + F, f -> {
            if(f < 1 || f > 5) throw new IllegalArgumentException("Merchant level can only be from 1 to 5");
            return f.toString();
        });
        public static final SingleType<Statistic>               STATISTIC                   = new SingleType<>("STATISTIC", Statistic.class, "stat.minecraft." + F, f -> {
            return f.getKey().getKey();
        });
        public static final SingleType<InventoryType>           INVENTORY_TYPE              = new SingleType<>("INVENTORY_TYPE", InventoryType.class, "container." + F, f -> switch (f){
            case ENDER_CHEST -> "enderchest";
            case SHULKER_BOX -> "shulkerBox";
            case CARTOGRAPHY -> "cartography_table";
            case ENCHANTING ->  "enchant";
            case GRINDSTONE -> "grindstone_title";
            case ANVIL -> "repair";
            case SMITHING, SMITHING_NEW -> "upgrade";
            default -> f.name().toLowerCase();
        });
        public static final SingleType<ItemGroup>               ITEM_GROUP                  = new SingleType<>("ITEN_GROUP", ItemGroup.class, "itemGroup." + F, f -> {
            return f.getKey();
        });
        public static final SingleType<DeathScreen>             DEATH_SCREEN                = new SingleType<>("DEATH_SCREEN", DeathScreen.class, "deathScreen." + F, f -> {
            return f.getKey();
        });
        public static final SingleType<DeathMessage>            DEATH_MESSAGE               = new SingleType<>("DEATH_MESSAGE", DeathMessage.class, "death." + F, f -> {
            return f.getKey();
        });
        public static final SingleType<FilledMap>               FILLED_MAP                  = new SingleType<>("FILLED_MAP", FilledMap.class, "filled_map." + F, f -> {
            return f.getKey();
        });
        public static final SingleType<Attribute>               ATTRIBUTE                   = new SingleType<>("ATTRIBUTE", Attribute.class, "attribute.name." + F, f -> {
            return f.getKey().getKey();
        });

        // Bi Types

        public static final BiType<PatternType, DyeColor>       BANNER_PATTERN              = new BiType<>("BANNER_PATTERN", PatternType.class, DyeColor.class, "block.minecraft.banner." + F + "." + S, (f, s) -> {
            return f.name().toLowerCase();
        }, (f, s) -> {
            return s.name().toLowerCase();
        });
        public static final BiType<PotionSort, PotionType>      ITEM_POTION_NAME            = new BiType<>("ITEM_POTION_NAME", PotionSort.class, PotionType.class, "item.minecraft." + F + ".effect." + S, (f, s) -> {
            return f.toString();
        }, (f, s) -> {
            if(s.equals(PotionType.REGEN)) return s.getEffectType().getName();
            else if(s.equals(PotionType.INSTANT_HEAL)) return "healing";
            else if(s.equals(PotionType.INSTANT_DAMAGE)) return "harming";
            else if(s.equals(PotionType.JUMP)) return "leaping";
            else if(s.equals(PotionType.UNCRAFTABLE)) return "empty";
            else if(s.equals(PotionType.SPEED)) return "swiftness";
            else return s.name().toLowerCase();
        });
        public static final BiType<Team.Option, Team.OptionStatus> TEAM_OPTION              = new BiType<>("TEAM_OPTION", Team.Option.class, Team.OptionStatus.class, "team." + F + "." + S, (f, s) -> switch (f){
            case COLLISION_RULE -> "collision";
            case NAME_TAG_VISIBILITY -> "visibility";
            default -> throw new IllegalArgumentException("This Team Option cannot be used");
        }, (f, s) -> switch(s){
            case NEVER -> "never";
            case ALWAYS -> "always";
            case FOR_OWN_TEAM -> switch (f){
                case COLLISION_RULE -> "pushOwnTeam";
                case NAME_TAG_VISIBILITY -> "hideForOwnTeam";
                default -> null;
            };
            case FOR_OTHER_TEAMS -> switch (f){
                case COLLISION_RULE -> "pushOtherTeams";
                case NAME_TAG_VISIBILITY -> "hideForOtherTeams";
                default -> null;
            };
        });

        private final String keyPattern;
        private final String name;

        private Type(String name, String keyPattern){
            this.name = name;
            this.keyPattern = keyPattern;
        }

        public String getName() { return name; }

        @Override
        public String toString(){ return getName(); }

        public String getKeyPattern() { return keyPattern; }

        public boolean isSingle(){ return !isDouble(); }

        public boolean isDouble(){ return this instanceof BiType; }


        protected @interface SupportsNull {
            boolean first() default false; boolean second() default false;
        }

        private Field getField() throws NoSuchFieldException {
            return MinecraftTranslation.Type.class.getDeclaredField(name);
        }

        protected boolean hasAnnotation(){
            try { return getField().isAnnotationPresent(Type.SupportsNull.class); }
            catch (NoSuchFieldException | SecurityException e) { return false; }
        }

        protected Type.SupportsNull getAnnotation(){
            try { return getField().getAnnotation(Type.SupportsNull.class); }
            catch (NoSuchFieldException | SecurityException e) { return null; }
        }
    }

    protected static class SingleType<F> extends Type{

        private final Function<F, String> firstKeyFunction;
        private final Class<F> firstTypeClass;

        private SingleType(String name, Class<F> firstTypeClass, String codePattern, Function<F, String> firstKeyFunction) {
            super(name, codePattern);
            this.firstTypeClass = firstTypeClass;
            this.firstKeyFunction = firstKeyFunction;
        }

        public Function<F, String> getFirstKeyFunction() { return firstKeyFunction; }

        public Class<F> getFirstTypeClass() { return firstTypeClass; }

        public boolean isFirstNullable(){
            if(!hasAnnotation()) return false;
            return getAnnotation().first();
        }
    }

    protected static class BiType<F, S> extends Type{

        private final BiFunction<F, S, String> firstKeyFunction;
        private final BiFunction<F, S, String> secondKeyFunction;
        private final Class<F> firstTypeClass;
        private final Class<S> secondTypeClass;

        private BiType(String name, Class<F> firstTypeClass, Class<S> secondTypeClass, String codePattern, BiFunction<F, S, String> firstKeyFunction, BiFunction<F, S, String> secondKeyFunction) {
            super(name,codePattern);
            this.firstTypeClass = firstTypeClass;
            this.secondTypeClass = secondTypeClass;
            this.firstKeyFunction = firstKeyFunction;
            this.secondKeyFunction = secondKeyFunction;
        }

        public BiFunction<F, S, String> getFirstKeyFunction() { return firstKeyFunction; }

        public BiFunction<F, S, String> getSecondKeyFunction() { return secondKeyFunction; }

        public Class<F> getFirstTypeClass() { return firstTypeClass; }

        public Class<S> getSecondTypeClass() { return secondTypeClass; }

        public boolean isFirstNullable(){
            if(!hasAnnotation()) return false;
            return getAnnotation().first();
        }

        public boolean isSecondNullable(){
            if(!hasAnnotation()) return false;
            return getAnnotation().second();
        }
    }

    private static class FindTask{

        private final Language initialLanguage;
        private final MinecraftTranslation translation;
        private final Result result;

        protected FindTask(MinecraftTranslation translation, Language language) {
            this.translation = translation;
            this.initialLanguage = language;
            Pair<Language, String> resulted = task(initialLanguage);
            result = new Result(translation, resulted.getA(), resulted.getB());
        }

        @Nonnull
        private Pair<Language, String> task(Language language){
            if(language != null){
                if(LangReader.containsKey(language, translation.getKey())) return new Pair<>(language, LangReader.getValue(language, translation.getKey()));
                Language similar = language.getSimilarLanguages().stream().filter(x-> !x.equals(language) && LangReader.containsKey(x, translation.getKey())).findFirst().orElse(null);
                if(similar != null && LangReader.containsKey(similar, translation.getKey())) return new Pair<>(similar, LangReader.getValue(similar, translation.getKey()));
            }
            if(!language.equals(LanguagesAPI.getServerLanguage()) && LangReader.containsKey(LanguagesAPI.getServerLanguage(), translation.getKey())) return new Pair<>(LanguagesAPI.getServerLanguage(), LangReader.getValue(LanguagesAPI.getServerLanguage(), translation.getKey()));
            if(!language.equals(Language.en_US) && LangReader.containsKey(Language.en_US, translation.getKey())) return new Pair<>(Language.en_US, LangReader.getValue(Language.en_US, translation.getKey()));
            return new Pair<>(language, null);
        }

        @Nullable
        public Language getInitialLanguage() {
            return initialLanguage;
        }

        @Nonnull
        public MinecraftTranslation getMessage() {
            return translation;
        }

        @Nonnull
        public Result getResult(){
            return result;
        }

    }

    protected static class Result {

        private final Language language;
        private final MinecraftTranslation translation;
        private String result;

        protected Result(MinecraftTranslation translation, Language language, @Nullable String result) {
            Validate.notNull(translation, "Message cannot be null.");
            Validate.notNull(language, "Language cannot be null.");
            this.translation = translation;
            this.language = language;
            this.result = result;
        }

        public Language getLanguage() { return language; }

        @Nonnull
        public String getFinalResult() {
            if(result == null) return translation.getKey();
            return result;
        }

        @Nonnull
        public boolean isFound() { return result != null; }

        @Override
        public String toString(){
            return getFinalResult();
        }
    }

    protected static class LangReader{

        private static Map<Language, JsonObject> JSON_CACHE;

        static{
            JSON_CACHE = new HashMap<>();
        }

        private LangReader() {}

        private static boolean read(Language language) {
            if(!LanguagesAPI.getAvailableLanguages().contains(language)) return false;
            InputStream inputStream = getFileFromResourceAsStream(String.format(Language.MINECRAFT_TRANSLATIONS_FILE_PATH, language.getMinecraftFileName()));
            InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            JSON_CACHE.put(language, new Gson().fromJson(reader, JsonObject.class));
            return true;
        }

        private static boolean check(Language language){
            if(JSON_CACHE.containsKey(language)) return true;
            return read(language);
        }

        @Nullable
        protected static String getValue(@Nonnull Language language, @Nonnull String key) {
            Validate.notNull(language, "Language cannot be null");
            Validate.notNull(key, "Key cannot be null");
            if(!check(language)) return null;
            JsonElement element = JSON_CACHE.get(language).get(key);
            if(element == null) return null;
            return element.getAsString();
        }

        protected static boolean containsKey(@Nonnull Language language, @Nonnull String key){
            Validate.notNull(language, "Language cannot be null");
            Validate.notNull(key, "Key cannot be null");
            check(language);
            if(!JSON_CACHE.containsKey(language)) return false;
            return JSON_CACHE.get(language).has(key);
        }

        protected static boolean existsKey(@Nonnull String key){
           return containsKey(Language.en_US, key);
        }

        @Nonnull
        protected static List<String> getKeys(@Nonnull Language language) {
            Validate.notNull(language, "Language cannot be null");
            Validate.isTrue(check(language), "Minecraft Translations for this language cannot be accesses right now.");
            List<String> keys = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : JSON_CACHE.get(language).entrySet()) keys.add(entry.getKey());
            return Collections.unmodifiableList(keys);
        }

        @Nonnull
        private static InputStream getFileFromResourceAsStream(String path) {
            Validate.notNull(path, "Path cannot be null");
            ClassLoader classLoader = LangReader.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(path);
            if (inputStream == null) throw new IllegalArgumentException("Filepath not found: " + path);
            return inputStream;
        }
    }
}
