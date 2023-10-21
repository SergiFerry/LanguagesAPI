package dev.sergiferry.languages.api.location;

import org.apache.commons.lang.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Creado por SergiFerry el 24/08/2022
 */
public class Continent {

    private static final Map<String, Continent> MAP = new HashMap<>();

    public static Boolean isRegistered(String isoCode){ return MAP.containsKey(isoCode); }
    public static Optional<Continent> grabContinent(String isoCode){ return Optional.ofNullable(MAP.getOrDefault(isoCode, null)); }

    //https://www.php.net/manual/en/function.geoip-continent-code-by-name.php

    public static final Continent AF = new Continent("AF", "Africa");
    public static final Continent AN = new Continent("AN", "Antarctica");
    public static final Continent AS = new Continent("AS", "Asia");
    public static final Continent EU = new Continent("EU", "Europe");
    public static final Continent NA = new Continent("NA", "North America");
    public static final Continent OC = new Continent("OC", "Oceania");
    public static final Continent SA = new Continent("SA", "South America");

    private final String isoCode;
    private final String englishName;

    private Continent(String isoCode, String englishName){
        Validate.isTrue(!MAP.containsKey(isoCode), "This continent is already registered.");
        this.isoCode = isoCode;
        this.englishName = englishName;
        MAP.put(isoCode, this);
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getEnglishName() {
        return englishName;
    }
}
