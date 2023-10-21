package dev.sergiferry.languages.api.location;

import org.apache.commons.lang.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class Country {

    private static final Map<Alpha2, Country> MAP_ALPHA2 = new HashMap<>();

    @Nonnull
    public static Boolean isRegistered(String code){
        if(code.length() == 2) return isRegistered(getAlpha2(code));
        return false;
    }

    public static Optional<Country> grabCountry(String code){
        if(code.length() == 2) return grabCountry(getAlpha2(code));
        return Optional.empty();
    }

    @Nonnull public static Boolean isRegistered(Alpha2 alpha2){ return MAP_ALPHA2.containsKey(alpha2); }
    public static Optional<Country> grabCountry(Alpha2 alpha2){ return Optional.ofNullable(MAP_ALPHA2.getOrDefault(alpha2, null)); }

    //https://www.iban.com/country-codes

    public static final Country NONE = null;
    public static final Country AF = new Country("Afghanistan", "AF", Continent.AS);
    public static final Country AL = new Country("Albania", "AL", Continent.EU);
    public static final Country DZ = new Country("Algeria", "DZ", Continent.AF);
    public static final Country AS = new Country("American Samoa", "AS");
    public static final Country AD = new Country("Andorra", "AD", Continent.EU);
    public static final Country AO = new Country("Angola", "AO");
    public static final Country AI = new Country("Anguilla", "AI");
    public static final Country AQ = new Country("Antarctica", "AQ", Continent.AN);
    public static final Country AG = new Country("Antigua and Barbuda", "AG");
    public static final Country AR = new Country("Argentina", "AR", Continent.SA);
    public static final Country AM = new Country("Armenia", "AM");
    public static final Country AW = new Country("Aruba", "AW");
    public static final Country AU = new Country("Australia", "AU", Continent.OC);
    public static final Country AT = new Country("Austria", "AT", Continent.EU);
    public static final Country AZ = new Country("Azerbaijan", "AZ");
    public static final Country BS = new Country("Bahamas", "BS");
    public static final Country BH = new Country("Bahrain", "BH");
    public static final Country BD = new Country("Bangladesh", "BD");
    public static final Country BB = new Country("Barbados", "BB");
    public static final Country BY = new Country("Belarus", "BY");
    public static final Country BE = new Country("Belgium", "BE", Continent.EU);
    public static final Country BZ = new Country("Belize", "BZ");
    public static final Country BJ = new Country("Benin", "BJ");
    public static final Country BM = new Country("Bermuda", "BM");
    public static final Country BT = new Country("Bhutan", "BT");
    public static final Country BO = new Country("Bolivia", "BO");
    public static final Country BQ = new Country("Bonaire", "BQ");
    public static final Country BA = new Country("Bosnia and Herzegovina", "BA");
    public static final Country BW = new Country("Botswana", "BW");
    public static final Country BV = new Country("Bouvet Island", "BV");
    public static final Country BR = new Country("Brazil", "BR");
    public static final Country IO = new Country("British Indian Ocean Territory", "IO");
    public static final Country BN = new Country("Brunei Darussalam", "BN");
    public static final Country BG = new Country("Bulgaria", "BG");
    public static final Country BF = new Country("Burkina Faso", "BF");
    public static final Country BI = new Country("Burundi", "BI");
    public static final Country CV = new Country("Cabo Verde", "CV");
    public static final Country KH = new Country("Cambodia", "KH");
    public static final Country CM = new Country("Cameroon", "CM");
    public static final Country CA = new Country("Canada", "CA");
    public static final Country KY = new Country("Cayman Islands", "KY");
    public static final Country CF = new Country("Central African Republic", "CF");
    public static final Country TD = new Country("Chad", "TD");
    public static final Country CL = new Country("Chile", "CL");
    public static final Country CN = new Country("China", "CN");
    public static final Country CX = new Country("Christmas Island", "CX");
    public static final Country CC = new Country("Cocos (Keeling) Islands", "CC");
    public static final Country CO = new Country("Colombia", "CO");
    public static final Country KM = new Country("Comoros", "KM");
    public static final Country CD = new Country("Democratic Republic of the Congo", "CD");
    public static final Country CG = new Country("Congo", "CG");
    public static final Country CK = new Country("Cook Islands", "CK");
    public static final Country CR = new Country("Costa Rica", "CR");
    public static final Country HR = new Country("Croatia", "HR");
    public static final Country CU = new Country("Cuba", "CU");
    public static final Country CW = new Country("Curaçao", "CW");
    public static final Country CY = new Country("Cyprus", "CY");
    public static final Country CZ = new Country("Czechia", "CZ");
    public static final Country CI = new Country("Côte d'Ivoire", "CI");
    public static final Country DK = new Country("Denmark", "DK");
    public static final Country DJ = new Country("Djibouti", "DJ");
    public static final Country DM = new Country("Dominica", "DM");
    public static final Country DO = new Country("Dominican Republic", "DO");
    public static final Country EC = new Country("Ecuador", "EC");
    public static final Country EG = new Country("Egypt", "EG");
    public static final Country SV = new Country("El Salvador", "SV");
    public static final Country GQ = new Country("Equatorial Guinea", "GQ");
    public static final Country ER = new Country("Eritrea", "ER");
    public static final Country EE = new Country("Estonia", "EE");
    public static final Country SZ = new Country("Eswatini", "SZ");
    public static final Country ET = new Country("Ethiopia", "ET");
    public static final Country FK = new Country("Falkland Islands", "FK");
    public static final Country FO = new Country("Faroe Islands", "FO");
    public static final Country FJ = new Country("Fiji", "FJ");
    public static final Country FI = new Country("Finland", "FI");
    public static final Country FR = new Country("France", "FR");
    public static final Country GF = new Country("French Guiana", "GF");
    public static final Country PF = new Country("French Polynesia", "PF");
    public static final Country TF = new Country("French Southern Territories", "TF");
    public static final Country GA = new Country("Gabon", "GA");
    public static final Country GM = new Country("Gambia", "GM");
    public static final Country GE = new Country("Georgia", "GE");
    public static final Country DE = new Country("Germany", "DE");
    public static final Country GH = new Country("Ghana", "GH");
    public static final Country GI = new Country("", "GI");
    public static final Country GR = new Country("", "GR");
    public static final Country GL = new Country("", "GL");
    public static final Country GD = new Country("", "GD");
    public static final Country GP = new Country("", "GP");
    public static final Country GU = new Country("", "GU");
    public static final Country GT = new Country("", "GT");
    public static final Country GG = new Country("", "GG");
    public static final Country GN = new Country("", "GN");
    public static final Country GW = new Country("", "GW");
    public static final Country GY = new Country("", "GY");
    public static final Country HT = new Country("", "HT");
    public static final Country HM = new Country("", "HM");
    public static final Country VA = new Country("", "VA");
    public static final Country HN = new Country("", "HN");
    public static final Country HK = new Country("", "HK");
    public static final Country HU = new Country("", "HU");
    public static final Country IS = new Country("", "IS");
    public static final Country IN = new Country("", "IN");
    public static final Country ID = new Country("", "ID");
    public static final Country IR = new Country("", "IR");
    public static final Country IQ = new Country("", "IQ");
    public static final Country IE = new Country("", "IE");
    public static final Country IM = new Country("", "IM");
    public static final Country IL = new Country("", "IL");
    public static final Country IT = new Country("", "IT");
    public static final Country JM = new Country("", "JM");
    public static final Country JP = new Country("", "JP");
    public static final Country JE = new Country("", "JE");
    public static final Country JO = new Country("", "JO");
    public static final Country KZ = new Country("", "KZ");
    public static final Country KE = new Country("", "KE");
    public static final Country KI = new Country("", "KI");
    public static final Country KP = new Country("", "KP");
    public static final Country KR = new Country("", "KR");
    public static final Country KW = new Country("", "KW");
    public static final Country KG = new Country("", "KG");
    public static final Country LA = new Country("", "LA");
    public static final Country LV = new Country("", "LV");
    public static final Country LB = new Country("", "LB");
    public static final Country LS = new Country("", "LS");
    public static final Country LR = new Country("", "LR");
    public static final Country LY = new Country("", "LY");
    public static final Country LI = new Country("", "LI");
    public static final Country LT = new Country("", "LT");
    public static final Country LU = new Country("", "LU");






    //public static final Country AA = new Country("", "");

    //TODO ...
    public static final Country ES = new Country("Spain", "ES", Continent.EU);

    @Nonnull private final String englishName;
    @Nullable private final Continent continent;
    @Nonnull private final Alpha2 alpha2;

    private Country(@Nonnull String englishName, @Nonnull Alpha2 alpha2, @Nullable Continent continent){
        this.englishName = englishName;
        this.continent = continent;
        this.alpha2 = alpha2;
        MAP_ALPHA2.put(alpha2, this);
    }

    private Country(@Nonnull String name, @Nonnull String alpha2){
        this(name,new Alpha2(alpha2), null);
    }

    private Country(@Nonnull String name, @Nonnull String alpha2, @Nullable Continent continent){
        this(name, new Alpha2(alpha2), continent);
    }

    @Nonnull public String getEnglishName() {
        return englishName;
    }

    @Nullable public Continent getContinent() {
        return continent;
    }

    @Nonnull public Alpha2 getAlpha2() {
        return alpha2;
    }

    public record Alpha2(String code){
        public Alpha2{ Validate.isTrue(code.length() == 2, "This Alpha2 code is not correct."); }
        public boolean equals(Alpha2 var){ return this.code.equalsIgnoreCase(var.code); }
        public String toString(){ return this.code; }
    }

    @Nonnull public static Alpha2 getAlpha2(String code){ return new Alpha2(code); }

}
