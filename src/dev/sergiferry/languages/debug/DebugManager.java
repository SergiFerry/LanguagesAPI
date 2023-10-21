package dev.sergiferry.languages.debug;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Creado por SergiFerry el 15/10/2022
 */
public class DebugManager {

    public static Boolean DEBUG = false;
    public static boolean isDebugEnabled() { return DEBUG; }

    public static void debug(CommandSender sender, String message) {
        if(!isDebugEnabled()) return;
        if(!sender.isOp()) return;
        sender.sendMessage("§c[LAPI-DEBUG] §7" + message);
    }

    public static void debug(String message){ debug(Bukkit.getConsoleSender(), message); }

    public static void print(Throwable e){
         if(!isDebugEnabled()) return;
         e.printStackTrace();
    }

}
