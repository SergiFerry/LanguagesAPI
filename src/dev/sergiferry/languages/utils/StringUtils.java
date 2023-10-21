package dev.sergiferry.languages.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creado por SergiFerry el 03/09/2022
 */
public class StringUtils {

    private static final Pattern hexPattern;

    static{
        hexPattern = Pattern.compile("&#[a-fA-F0-9]{6}");
    }

    /*public static String formatHexColor(String msg){
        if(msg == null) return null;
        Matcher match = hexPattern.matcher(msg);
        while (match.find()){
            String color = msg.substring(match.start() + 1, match.end());
            msg = msg.replace(match.group(), ChatColor.of(color) + "");
            match = hexPattern.matcher(msg);
        }
        return msg;
    }*/

    public static String formatColor(String message){
        if(message == null) return null; if(message.isEmpty() || message.isBlank()) return message;
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            ChatColor hexColor = ChatColor.of(matcher.group().substring(1));
            String before = message.substring(0, matcher.start());
            String after = message.substring(matcher.end());
            message = before + hexColor + after;
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String getStringInside(String string, char c){
        Pattern pattern = Pattern.compile(c + "(.*?)" + c);
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()) return matcher.group(1);
        return "";
    }

    public static String getStringFromList(List<String> list, StringListSeparator stringListSeparator){
        return getStringFromList(list, stringListSeparator, true);
    }

    public static String getStringFromList(List<String> list, StringListSeparator stringListSeparator, boolean firstDifferent){
        String string = "";
        boolean first = true;
        for(String a : list){
            string = string + (!first || !firstDifferent ? stringListSeparator.getFormat() : "") + a;
            first = false;
        }
        return string;
    }

    public static List<String> replaceAll(List<String> list, String regex, String replacement){
        List<String> finalList = new ArrayList<>();
        for(String s : list){ finalList.add(s.replaceAll(regex, replacement)); }
        return finalList;
    }

    public enum StringListSeparator{
        NEW_LINE("\n"), COMMA(", "), NEW_LINE_TEXT("\\n"), LISTED("\n§r- ");

        private String format;

        StringListSeparator(String format){
            this.format = format;
        }

        public String getFormat() { return format; }
    }

}
