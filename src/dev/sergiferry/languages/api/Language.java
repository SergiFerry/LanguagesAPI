package dev.sergiferry.languages.api;

import dev.sergiferry.languages.api.location.Country;
import dev.sergiferry.languages.api.minecraft.MinecraftTranslation;
import dev.sergiferry.languages.api.minecraft.sorts.LanguageDetail;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Creado por SergiFerry el 23/08/2022
 */
public class Language {

    private static final List<Language> ALL = new ArrayList<>();
    private static final Map<String, Language> MAP = new HashMap<>();

    @Nonnull public static List<Language> getAllLanguages() { return ALL; }
    @Nonnull public static List<Language> getLanguages(Predicate<Language> filter) { return ALL.stream().filter(filter).toList(); }
    @Nonnull public static List<Language> getLanguages(Country country) { return getLanguages(x-> x.getCountry().isPresent() && x.getCountry().get().equals(country) || x.getAdditionalCountries().stream().filter(y-> y.equals(country)).findAny().isPresent()); }

    @Nonnull public static Set<String> getKeys() { return MAP.keySet(); }
    @Nonnull public static Set<Map.Entry<String, Language>> getEntrySet() { return MAP.entrySet(); }

    @Nonnull public static Boolean isRegistered(@Nonnull String localeCode){ return MAP.containsKey(localeCode.toLowerCase()); }
    public static Optional<Language> grabLanguage(@Nonnull String localeCode){ return Optional.ofNullable(MAP.getOrDefault(localeCode.toLowerCase(), null)); }
    public static Optional<Language> grabClientLanguage(@Nonnull Player player){ return grabLanguage(player.getLocale().toLowerCase()); }

    public static Language registerLanguage(String localeCode, String name, String englishName) { return new Language(localeCode, name, englishName); }
    public static Language registerLanguage(String localeCode, String name, String englishName, Country country) { return new Language(localeCode, name, englishName, country); }
    public static Language registerLanguage(String localeCode, String name, String englishName, Country country, Country... additionalCountries) { return new Language(localeCode, name, englishName, country, additionalCountries); }

    /**
     * This value can be changed on the future, and will be updated with the latest Minecraft version release.
     */
    protected @interface LanguageID {
        int id(); String addedOn();
    }

    protected @interface LanguageRM {
        String from(); String to();
    }

    private static final String UNKNOWN = "";
    public static final String MINECRAFT_TRANSLATIONS_FILE_PATH = "resources/minecraft/%s.json";

    // https://minecraft-archive.fandom.com/wiki/Languages
    // https://minecraft.fandom.com/wiki/Language

    @LanguageID(id = 1, addedOn = "1.1")        public static final Language af_ZA = new Language("af_ZA", "Afrikaans", "Afrikaans");
    @LanguageID(id = 2, addedOn = "1.1")        public static final Language ar_SA = new Language("ar_SA", "اللغة العربية", "Arabic");
    @LanguageID(id = 3, addedOn = "1.7.4")      public static final Language ast_ES = new Language("ast_ES", "Asturianu", "Asturian", Country.ES);
    @LanguageID(id = 4, addedOn = "1.7.10")     public static final Language az_AZ = new Language("az_AZ", "Azərbaycanca", "Azerbaijani");
    @LanguageID(id = 5, addedOn = "1.14.3")     public static final Language ba_RU = new Language("ba_RU", "Башҡортса", "Bashkir"); //Country.RU
    @LanguageID(id = 6, addedOn = "1.14")       public static final Language bar_DE = new Language(new String[] {"bar_DE", "bar"}, "Boarisch", "Bavarian"); //Country.DE
    @LanguageID(id = 7, addedOn = "1.9")        public static final Language be_BY = new Language("be_BY", "Беларуская", "Belarusian");
    @LanguageID(id = 8, addedOn = "1.1")        public static final Language bg_BG = new Language("bg_BG", "Български", "Bulgarian");
    @LanguageID(id = 9, addedOn = "1.9")        public static final Language br_FR = new Language("br_FR", "Brezhoneg", "Breton");
    @LanguageID(id = 10, addedOn = "1.13.1")    public static final Language brb_NL = new Language(new String[] {"brb_NL", "brb"}, "Braobans", "Brabantian"); //Country.NL & BE
    @LanguageID(id = 11, addedOn = "1.13")      public static final Language bs_BA = new Language("bs_BA", "Braobans", "Bosnian");
    @LanguageID(id = 12, addedOn = "1.1")       public static final Language ca_ES = new Language("ca_ES", "Català", "Catalan", Country.ES, Country.AD);
    @LanguageID(id = 13, addedOn = "1.1")       public static final Language cs_CZ = new Language("cs_CZ", "Čeština", "Czech");
    @LanguageID(id = 14, addedOn = "1.1")       public static final Language cy_GB = new Language("cy_GB", "Cymraeg", "Welsh");
    @LanguageID(id = 15, addedOn = "1.1")       public static final Language da_DK = new Language("da_DK", "Dansk", "Danish");
    @LanguageID(id = 16, addedOn = "1.10")      public static final Language de_AT = new Language("de_AT", "Österreichisches Deitsch", "Austrian German"); //Country.AT
    @LanguageID(id = 17, addedOn = "1.13")      public static final Language de_CH = new Language("de_CH", "Schwiizerdutsch", "Swiss German"); //Country.CH
    @LanguageID(id = 18, addedOn = "1.1")       public static final Language de_DE = new Language("de_DE", "Deutsch", "German");
    @LanguageID(id = 19, addedOn = "1.1")       public static final Language el_GR = new Language("el_GR", "Ελληνικά", "Greek");
    @LanguageID(id = 20, addedOn = "1.3.1")     public static final Language en_AU = new Language("en_AU", "English (Australia)", "Australian English");
    @LanguageID(id = 21, addedOn = "1.1")       public static final Language en_CA = new Language("en_CA", "English (Canada)", "Canadian English");
    @LanguageID(id = 22, addedOn = "1.1")       public static final Language en_GB = new Language("en_GB", "English (United Kingdom)", "British English");
    @LanguageID(id = 23, addedOn = "1.9")       public static final Language en_NZ = new Language("en_NZ", "English (New Zealand)", "New Zealand English");
    @LanguageRM(from = "1.10", to = UNKNOWN)    public static final Language en_ZA = new Language("en_ZA", "English (South Africa)", "South African English");
    @LanguageID(id = 24, addedOn = "1.1")       public static final Language en_7S = new Language(new String[] {"en_7S", "qpe", "en_pt"}, "Pirate Speak", "Pirate English");
    @LanguageID(id = 25, addedOn = "1.9.2")     public static final Language en_UD = new Language("en_UD", "ɥsᴉꞁᵷuƎ (ɯopᵷuᴉꞰ pǝʇᴉuՈ)", "British English (upside down)");
    @LanguageID(id = 26, addedOn = "0.0")       public static final Language en_US = new Language("en_US", "English (United States)", "American English");
    @LanguageID(id = 27, addedOn = "1.13")      public static final Language ang_GB = new Language(new String[]{"ang_GB", "enp"}, "Anglish", "English puristic"); //Country.GB
    @LanguageID(id = 28, addedOn = "1.13")      public static final Language sha_GB = new Language(new String[] {"sha_GB", "enws"}, "Shakespearean English", "Early Modern English"); //Country.GB
    @LanguageID(id = 29, addedOn = "1.2")       public static final Language eo_UY = new Language("eo_UY","Esperanto", "Esperanto");
    @LanguageID(id = 30, addedOn = "1.1")       public static final Language es_AR = new Language("es_AR", "Español (Argentina)", "Argentinian Spanish");
    @LanguageID(id = 31, addedOn = "1.13")      public static final Language es_CL = new Language("es_CL", "Español (Chile)", "Chilean Spanish");
    @LanguageID(id = 32, addedOn = "1.13")      public static final Language es_EC = new Language("es_EC", "Español (Ecuador)", "Ecuadorian Spanish");
    @LanguageID(id = 33, addedOn = "1.1")       public static final Language es_ES = new Language("es_ES", "Español (España)", "Spanish", Country.ES);
    @LanguageRM(from = "1.1", to = "1.1")       public static final Language es_LA = new Language("es_LA", "Español (Latinoamericano)", "Latino-American Spanish", Country.NONE);
    @LanguageID(id = 34, addedOn = "1.1")       public static final Language es_MX = new Language("es_MX", "Español (México)", "Mexican Spanish");
    @LanguageID(id = 35, addedOn = "1.1")       public static final Language es_UY = new Language("es_UY", "Español (Uruguay)", "Uruguayan Spanish");
    @LanguageID(id = 36, addedOn = "1.1")       public static final Language es_VE = new Language("es_VE", "Español (Venezuela)", "Venezuelan Spanish");
    @LanguageID(id = 37, addedOn = "1.16")      public static final Language and_ES = new Language(new String[] {"and_ES", "esan"}, "Andalûh (Andaluçía)", "Andalusian",Country.ES);
    @LanguageID(id = 38, addedOn = "1.1")       public static final Language et_EE = new Language("et_EE", "Eesti", "Estonian");
    @LanguageID(id = 39, addedOn = "1.1")       public static final Language eu_ES = new Language("eu_ES", "Euskara", "Basque", Country.ES);
    @LanguageID(id = 40, addedOn = "1.7.2")     public static final Language fa_IR = new Language("fa_IR", "فارسی", "Persian");
    @LanguageID(id = 41, addedOn = "1.1")       public static final Language fi_FI = new Language("fi_FI", "Suomi", "Finnish");
    @LanguageID(id = 42, addedOn = "1.7.2")     public static final Language fil_PH = new Language("fil_PH", "Filipino", "Filipino");
    @LanguageID(id = 43, addedOn = "1.9")       public static final Language fo_FO = new Language("fo_FO", "Føroyskt", "Faroese");
    @LanguageID(id = 44, addedOn = "1.1")       public static final Language fr_CA = new Language("fr_CA", "Français québécois", "Canadian French");
    @LanguageID(id = 45, addedOn = "1.1")       public static final Language fr_FR = new Language("fr_FR", "Français", "French");
    @LanguageID(id = 46, addedOn = "1.13.1")    public static final Language fra_DE = new Language("fra_DE", "Fränggisch", "East Franconian");
    @LanguageID(id = 47, addedOn = "1.18.2")    public static final Language fur_IT = new Language("fur_IT", "Furlan", "Friulian"); //Country.IT
    @LanguageID(id = 48, addedOn = "1.9")       public static final Language fy_NL = new Language("fy_NL", "Frysk", "Frisian");
    @LanguageID(id = 49, addedOn = "1.3.1")     public static final Language ga_IE = new Language("ga_IE", "Gaeilge", "Irish");
    @LanguageID(id = 50, addedOn = "1.9.2")     public static final Language gd_GB = new Language("gd_GB", "Gàidhlig", "Scottish Gaelic");
    @LanguageID(id = 51, addedOn = "1.1")       public static final Language gl_ES = new Language("gl_ES", "Galego", "Galician", Country.ES);
    @LanguageID(id = 52, addedOn = "1.10")      public static final Language haw_US = new Language("haw_US", "ʻŌlelo Hawaiʻi", "Hawaiian");
    @LanguageID(id = 53, addedOn = "1.1")       public static final Language he_IL = new Language("he_IL", "עברית","Hebrew");
    @LanguageRM(from= "1.7.4", to= "1.17.1")    public static final Language gv_IM = new Language("gv_IM", "Gaelg", "Manx");
    @LanguageID(id = 54, addedOn = "1.1")       public static final Language hi_IN = new Language("hi_IN", "हिन्दी", "Hindi");
    @LanguageID(id = 55, addedOn = "1.1")       public static final Language hr_HR = new Language("hr_HR", "Hrvatski", "Croatian");
    @LanguageID(id = 56, addedOn = "1.1")       public static final Language hu_HU = new Language("hu_HU", "Magyar", "Hungarian");
    @LanguageID(id = 57, addedOn = "1.7")       public static final Language hy_AM = new Language("hy_AM", "Հայերեն", "Armenian");
    @LanguageID(id = 58, addedOn = "1.3.1")     public static final Language id_ID = new Language("id_ID", "Bahasa Indonesia", "Indonesian");
    @LanguageID(id = 59, addedOn = "1.13")      public static final Language ig_NG = new Language("ig_NG", "Igbo", "Igbo");
    @LanguageID(id = 60, addedOn = "1.11")      public static final Language io_EN = new Language("io_EN", "Ido", "Ido", Country.NONE);
    @LanguageID(id = 61, addedOn = "1.1")       public static final Language is_IS = new Language("is_IS", "Íslenska", "Icelandic");
    @LanguageID(id = 62, addedOn = "1.16")      public static final Language isv = new Language("isv", "Medžuslovjansky", "Interslavic", Country.NONE);
    @LanguageID(id = 63, addedOn = "1.1")       public static final Language it_IT = new Language("it_IT", "Italiano", "Italian");
    @LanguageID(id = 64, addedOn = "1.1")       public static final Language ja_JP = new Language("ja_JP", "日本語", "Japanese");
    @LanguageID(id = 65, addedOn = "1.9")       public static final Language jbo_EN = new Language("jbo_EN", "la .lojban.", "Lojban", Country.NONE);
    @LanguageID(id = 66, addedOn = "1.1")       public static final Language ka_GE = new Language("ka_GE", "ქართული", "Georgian");
    @LanguageRM(from= "1.13", to= "1.17")       public static final Language kab_DZ = new Language("kab_DZ", "Taqbaylit", "Kabyle");
    @LanguageID(id = 67, addedOn = "1.14")      public static final Language kk_kz = new Language("kk_kz", "Қазақша", "Kazakh"); //Country.KAZAHISTAN
    @LanguageID(id = 68, addedOn = "1.13")      public static final Language kn_IN = new Language("kn_IN", "ಕನ್ನಡ", "Kannada"); //Country.INDIA
    @LanguageID(id = 69, addedOn = "1.1")       public static final Language ko_KR = new Language("ko_KR", "한국어", "Korean"); //Country.KOREA
    @LanguageID(id = 70, addedOn = "1.9")       public static final Language ksh_DE = new Language(new String[] {"ksh_DE", "ksh"}, "Kölsch/Ripoarisch", "Kölsch/Ripuarian");
    @LanguageID(id = 71, addedOn = "1.5")       public static final Language kw_GB = new Language("kw_GB", "Kernewek", "Cornish");
    @LanguageID(id = 72, addedOn = "1.7")       public static final Language la_LA = new Language("la_LA", "Latina", "Latin");
    @LanguageID(id = 73, addedOn = "1.7")       public static final Language lb_LU = new Language("lb_LU", "Lëtzebuergesch", "Luxembourgish");
    @LanguageID(id = 74, addedOn = "1.9")       public static final Language li_LI = new Language("li_LI", "Limburgs", "Limburgish");
    @LanguageID(id = 75, addedOn = "1.18")      public static final Language lmo_IT = new Language(new String[] {"lmo_IT", "lmo"}, "Lombard", "Lombard");
    @LanguageID(id = 76, addedOn = "1.20")      public static final Language lo_LA = new Language("lo_LA", "ລາວ (ປະເທດລາວ)", "Lao");
    @LanguageID(id = 76, addedOn = "1.9")       public static final Language lol_US = new Language("lol_US", "LOLCAT", "LOLCAT", Country.NONE);
    @LanguageID(id = 77, addedOn = "1.1")       public static final Language lt_LT = new Language("lt_LT", "Lietuvių", "Lithuanian");
    @LanguageID(id = 78, addedOn = "1.1")       public static final Language lv_LV = new Language("lv_LV", "Latviešu", "Latvian");
    @LanguageID(id = 79, addedOn = "1.17.1")    public static final Language lzh_CN = new Language(new String[] {"lzh_CN", "lzh"}, "文言", "Classical Chinese"); //Country.CHINA
    @LanguageID(id = 80, addedOn = "1.9")       public static final Language mk_MK = new Language("mk_MK", "Македонски", "Macedonian");
    @LanguageRM(from= "1.7.10", to= "1.17")     public static final Language mi_NZ = new Language("mi_NZ", "Te Reo Māori", "Māori");
    @LanguageID(id = 81, addedOn = "1.10")      public static final Language mn_MN = new Language("mn_MN", "Монгол", "Mongolian");
    @LanguageRM(from= "1.13.1", to= "1.17")     public static final Language moh_CA = new Language("moh_CA", "Kanien’kéha", "Mohawk");
    @LanguageID(id = 82, addedOn = "1.1")       public static final Language ms_MY = new Language("ms_MY", "Bahasa Melayu", "Malay");
    @LanguageID(id = 83, addedOn = "1.2")       public static final Language mt_MT = new Language("mt_MT", "Malti", "Maltese");
    @LanguageID(id = 84, addedOn = "1.19.3")    public static final Language nhe_MX = new Language(new String[] {"nhe_MX", "nah"}, "Mēxikatlahtōlli", "Nahuatl"); //Country.MEXICO &
    @LanguageID(id = 85, addedOn = "1.7.4")     public static final Language nds_DE = new Language("nds_DE", "Platdüütsk", "Low German"); //Country.GERMANY & NETHERLANDS
    @LanguageID(id = 86, addedOn = "1.13")      public static final Language nl_BE = new Language("nl_BE", "Vlaams", "Dutch, Flemish"); //Country.BELGIUM & NETHERLANDS
    @LanguageID(id = 87, addedOn = "1.1")       public static final Language nl_NL = new Language("nl_NL", "Nederlands", "Dutch"); //Country.NETHERLANDS
    @LanguageID(id = 88, addedOn = "1.1")       public static final Language nn_NO = new Language("nn_NO", "Norsk Nynorsk", "Norwegian Nynorsk");
    @LanguageID(id = 89, addedOn = "1.1")       public static final Language no_NO = new Language(new String[] {"no_NO", "nb_NO"}, "Norsk Bokmål", "Norwegian");
    @LanguageRM(from = "1.13.1", to= "1.17")    public static final Language nuk_CA = new Language(new String[] {"nuk_CA", "nuk"}, "Nuučaan̓uł", "Nuu-chah-nulth"); //Country.CANADA
    @LanguageID(id = 90, addedOn = "1.7")       public static final Language oc_FR = new Language("oc_FR", "Occitan", "Occitan"); //Country.FRANCE
    @LanguageRM(from="1.13", to= "1.17")        public static final Language oj_CA = new Language(new String[] {"oj_CA", "oji"}, "Ojibwemowin", "Ojibwe"); //Country.CANADA & US
    @LanguageID(id = 91, addedOn = "1.13")      public static final Language ovd_SE = new Language(new String[] {"ovd_SE", "ovd"}, "Övdalsk", "Elfdalian"); //Country.SWEEDEN
    @LanguageID(id = 92, addedOn = "1.1")       public static final Language pl_PL = new Language("pl_PL", "Polski", "Polish");
    @LanguageID(id = 93, addedOn = "1.1")       public static final Language pt_BR = new Language("pt_BR", "Português (Brasil)", "Brazilian Portuguese");
    @LanguageID(id = 94, addedOn = "1.1")       public static final Language pt_PT = new Language("pt_PT", "Português (Portugal)", "Portuguese");
    @LanguageID(id = 95, addedOn = "1.1")       public static final Language qya_AA = new Language("qya_AA", "Quenya", "Quenya (Form of Elvish from LOTR)", Country.NONE);
    @LanguageID(id = 96, addedOn = "1.1")       public static final Language ro_RO = new Language("ro_RO", "Română", "Romanian");
    @LanguageID(id = 97, addedOn = "1.17")      public static final Language rpr_RU = new Language(new String[] {"rpr_RU", "rpr"}, "Дореформенный русскiй", "Russian (Pre-revolutionary)");
    @LanguageID(id = 98, addedOn = "1.1")       public static final Language ru_RU = new Language("ru_RU", "Русский", "Russian");
    @LanguageID(id = 100, addedOn = "1.19.3")   public static final Language ry_UA = new Language("ry_UA", "Руснацькый (Пудкарпатя, Украина)", "Rusyn");
    @LanguageID(id = 101, addedOn = "1.20")     public static final Language sah_RU = new Language(new String[] {"sah_RU", "sah_sah"}, "Сахалыы (Cаха Сирэ)", "Yakut");
    @LanguageID(id = 99, addedOn = "1.8")       public static final Language se_NO = new Language("se_NO", "Davvisámegiella", "Northern Sami"); //Country.NORWAY
    @LanguageID(id = 100, addedOn = "1.1")      public static final Language sk_SK = new Language("sk_SK", "Slovenčina", "Slovak");
    @LanguageID(id = 101, addedOn = "1.1")      public static final Language sl_SI = new Language("sl_SI", "Slovenščina", "Slovenian");
    @LanguageID(id = 102, addedOn = "1.9")      public static final Language so_SO = new Language("so_SO", "Af-Soomaali", "Somali");
    @LanguageID(id = 103, addedOn = "1.9")      public static final Language sq_AL = new Language("sq_AL", "Shqip", "Albanian");
    @LanguageID(id = 107, addedOn = "1.20.1")   public static final Language sr_CS = new Language("sr_CS", "Srpski (Srbija)", "Serbian (Latin)");
    @LanguageID(id = 104, addedOn = "1.1")      public static final Language sr_SP = new Language("sr_SP", "Српски", "Serbian");
    @LanguageID(id = 105, addedOn = "1.1")      public static final Language sv_SE = new Language("sv_SE", "Svenska", "Swedish");
    @LanguageRM(from= "1.10", to= "1.17.1")     public static final Language swg_DE = new Language(new String[] {"swg_DE", "swg"}, "Oschdallgaierisch", "Allgovian German");
    @LanguageID(id = 106, addedOn = "1.13")     public static final Language sxu_DE = new Language(new String[] {"sxu_DE", "sxu"}, "Säggs'sch", "Upper Saxon German");
    @LanguageID(id = 107, addedOn = "1.13")     public static final Language szl_PL = new Language(new String[] {"szl_PL", "szl"}, "Ślōnskŏ gŏdka", "Silesian");
    @LanguageID(id = 108, addedOn = "1.13")     public static final Language ta_IN = new Language("ta_IN", "தமிழ்", "Tamil");
    @LanguageID(id = 109, addedOn = "1.1")      public static final Language th_TH = new Language("th_TH", "ภาษาไทย", "Thai");
    @LanguageID(id = 110, addedOn = "1.15.1")   public static final Language tl_PH = new Language("tl_PH", "Tagalog", "Tagalog");
    @LanguageID(id = 111, addedOn = "1.1")      public static final Language tlh_AA = new Language("tlh_AA", "tlhIngan Hol", "Klingon", Country.NONE);
    @LanguageID(id = 112, addedOn = "1.18")     public static final Language tok = new Language("tok", "toki pona", "Toki Pona", Country.NONE);
    @LanguageID(id = 113, addedOn = "1.1")      public static final Language tr_TR = new Language("tr_TR", "Türkçe", "Turkish");
    @LanguageID(id = 114, addedOn = "1.13.1")   public static final Language tt_RU = new Language("tt_RU", "Татарча", "Tatar");
    @LanguageRM(from = "1.9", to = "1.17")      public static final Language tzl_TZL = new Language("tzl_TZL", "Talossan", "Talossan");
    @LanguageID(id = 115, addedOn = "1.1")      public static final Language uk_UA = new Language("uk_UA", "Українська", "Ukrainian");
    @LanguageID(id = 116, addedOn = "1.7.4")    public static final Language val_ES = new Language("val_ES", "Català (Valencià)", "Valencian");
    @LanguageID(id = 117, addedOn = "1.13")     public static final Language vec_IT = new Language("vec_IT", "Vèneto", "Venetian");
    @LanguageID(id = 118, addedOn = "1.1")      public static final Language vi_VN = new Language("vi_VN", "Tiếng Việt", "Vietnamese");
    @LanguageID(id = 119, addedOn = "1.13.1")   public static final Language vmf_DE = new Language("vmf_DE", "Frängisch", "Franconian");
    @LanguageID(id = 120, addedOn = "1.15")     public static final Language yi_DE = new Language("yi_DE", "ייִדיש", "Yiddish");
    @LanguageID(id = 121, addedOn = "1.13")     public static final Language yo_NG = new Language("yo_NG", "Yorùbá", "Yoruba");
    @LanguageID(id = 122, addedOn = "1.1")      public static final Language zh_CN = new Language("zh_CN", "简体中文（中国大陆）", "Chinese (China Mainland)");
    @LanguageID(id = 123, addedOn = "1.17")     public static final Language zh_HK = new Language("zh_HK", "繁體中文（香港特別行政區）", "Chinese (Hong Kong)");
    @LanguageID(id = 124, addedOn = "1.1")      public static final Language zh_TW = new Language("zh_TW", "繁體中文（台灣）", "Chinese (Taiwan)");
    @LanguageID(id = 125, addedOn = "1.19")     public static final Language zlm_MY = new Language(new String[] {"zlm_MY", "zlm_arab"}, "بهاس ملايو (مليسيا)", "Malay (Jawi)"); //Country.MALAYSIA

    @Nonnull private final List<String> localeCode;
    @Nonnull private final String name;
    @Nonnull private final String englishName;
    @Nullable private Country country;
    @Nullable private final List<Country> additionalCountries;
    @Nullable private List<Language> similarLanguages;
    @Nullable private String minecraftFileName;

    private Language(@Nonnull String[] localeCode, @Nonnull String name, @Nonnull String englishName, @Nullable Country country, @Nullable Country... additionalCountries){
        for (String s : localeCode) { Validate.isTrue(!MAP.containsKey(s.toLowerCase()), "This locale code is already registered. " + localeCode); }
        Validate.notNull(localeCode);
        Validate.notNull(name);
        Validate.notNull(englishName);
        this.localeCode = Arrays.stream(localeCode).toList();
        this.name = name;
        this.englishName = englishName;
        this.country = country;
        if(additionalCountries == null) this.additionalCountries = new ArrayList<>();
        else this.additionalCountries = Arrays.stream(additionalCountries).toList();
        ClassLoader classLoader = getClass().getClassLoader();
        for(String s : localeCode){
            MAP.put(s.toLowerCase(), this);
            String lowerCase = s.toLowerCase();
            InputStream inputStream = classLoader.getResourceAsStream(String.format(MINECRAFT_TRANSLATIONS_FILE_PATH, lowerCase));
            if (inputStream != null) minecraftFileName = lowerCase;
        }
        if(minecraftFileName == null && hasLanguageID()){
            Bukkit.getLogger().warning(englishName + " does not have minecraft translations file.");
            return;
        }
        ALL.add(this);
    }

    private Language(@Nonnull String localeCode, @Nonnull String name, @Nonnull String englishName, @Nullable Country country, @Nullable Country... additionalCountries){
        this(new String[] {localeCode}, name, englishName, country, additionalCountries);
    }

    private Language(@Nonnull String[] localeCode, @Nonnull String name, @Nonnull String englishName, @Nullable Country country){
        this(localeCode, name, englishName, country, null);
    }

    private Language(@Nonnull String[] localeCode, @Nonnull String name, @Nonnull String englishName){
        this(localeCode, name, englishName, null, null);
        if(country == null && localeCode[0].contains("_")) country = Country.grabCountry(localeCode[0].split("_")[1]).orElse(null);
    }

    private Language(@Nonnull String localeCode, @Nonnull String name, @Nonnull String englishName, @Nullable Country country){
        this(localeCode, name, englishName, country, null);
    }

    private Language(@Nonnull String localeCode, @Nonnull String name, @Nonnull String englishName){
        this(localeCode, name, englishName, null, null);
        if(country == null && localeCode.contains("_")) country = Country.grabCountry(localeCode.split("_")[1]).orElse(null);
    }

    public boolean isAvailable() { return LanguagesAPI.getAvailableLanguages().contains(this); }

    public boolean hasSimilarAvailable() { return getSimilarLanguages().stream().filter(x-> x.isAvailable()).findFirst().isPresent(); }

    public List<String> getLocaleCodes() { return localeCode; }

    @Nonnull public String getLocaleCode() { return localeCode.stream().findFirst().orElseThrow(); }

    @Nonnull public String getName() { return name; }

    @Nonnull
    public String getNameAndRegion(){
        if(!LanguagesAPI.getAvailableLanguages().contains(this)) return this.name;
        return MinecraftTranslation.getLanguageDetail(this, LanguageDetail.NAME) + " (" + MinecraftTranslation.getLanguageDetail(this, LanguageDetail.REGION) + ")";
    }

    public String getConsoleName() { return getEnglishName() + " [" + getLocaleCode().toLowerCase() + "]"; }

    @Nonnull public String getEnglishName() { return englishName; }

    public Optional<Country> getCountry() { return Optional.ofNullable(country); }

    public boolean hasCountry() { return country != null; }

    @Nullable public String getMinecraftFileName() { return minecraftFileName; }

    public List<Country> getAdditionalCountries() { return additionalCountries; }

    public boolean hasAdditionalCountries(){ return additionalCountries != null; }

    protected String getFolderPath(LanguagesAPI.PluginManager pluginManager){
        if(this.equals(pluginManager.getDefaultLanguage()) && pluginManager.hasDefaultLanguageFile()) return pluginManager.getDefaultLanguageFile().getParent();
        return pluginManager.getPlugin().getDataFolder().getPath() + "/languages/";
    }

    protected String getTranslationsFilePath(LanguagesAPI.PluginManager pluginManager){
        if(this.equals(pluginManager.getDefaultLanguage()) && pluginManager.hasDefaultLanguageFile()) return pluginManager.getDefaultLanguageFile().getPath();
        return getFolderPath(pluginManager) + getLocaleCode().toLowerCase() + ".yml";
    }

    private Field getField() throws NoSuchFieldException { return Language.class.getDeclaredField(getLocaleCode()); }

    protected boolean isLegacyLanguage(){
        try { return getField().isAnnotationPresent(LanguageRM.class); }
        catch (NoSuchFieldException | SecurityException e) { return false; }
    }

    protected boolean hasLanguageID(){
        try { return getField().isAnnotationPresent(LanguageID.class); }
        catch (NoSuchFieldException | SecurityException e) { return false; }
    }

    protected LanguageID getLanguageID(){
        try { return getField().getAnnotation(LanguageID.class); }
        catch (NoSuchFieldException | SecurityException e) { return null; }
    }

    public void addSimilarLanguage(@Nonnull Language language){
        Validate.notNull(language, "Language cannot be null");
        if(similarLanguages == null) loadSimilarLanguages();
        if(similarLanguages.contains(language)) return;
        similarLanguages.add(language);
    }

    public void addSimilarLanguages(@Nonnull Language... languages){
        Validate.notNull(languages, "Languages cannot be null");
        Arrays.stream(languages).forEach(x-> addSimilarLanguage(x));
    }

    public void addSimilarLanguages(@Nonnull List<Language> languages){
        Validate.notNull(languages, "Languages cannot be null");
        languages.forEach(x-> addSimilarLanguage(x));
    }

    @Nonnull
    public List<Language> getSimilarLanguages(){
        if(similarLanguages == null) loadSimilarLanguages();
        return similarLanguages;
    }

    private void loadSimilarLanguages(){
        this.similarLanguages = new ArrayList<>();
        //
        if(this.equals(ca_ES)) similarLanguages.addAll(List.of(val_ES, es_ES));
        else if(this.equals(val_ES)) similarLanguages.addAll(List.of(ca_ES, es_ES));
        else if(this.equals(and_ES)) similarLanguages.addAll(List.of(es_ES));
        else if(this.equals(eu_ES)) similarLanguages.addAll(List.of(es_ES));
        else if(this.equals(gl_ES)) similarLanguages.addAll(List.of(es_ES));
        else similarLanguages.addAll(Language.getAllLanguages().stream().filter(x-> x.getLocaleCode().split("_")[0].equals(this.getLocaleCode().split("_")[0])).collect(Collectors.toList()));
    }

}
