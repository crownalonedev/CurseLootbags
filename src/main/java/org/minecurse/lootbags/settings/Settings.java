package org.minecurse.lootbags.settings;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
   public static boolean debug;
   public static String storeUrl;
   public static String serverName;
   public static String crateBroadcast;
   public static String botName;
   public static String botNamePlain;
   public static String saleBroadcastLine1;
   public static String saleBroadcastLine2;
   public static String crateHologramLine;
   public static String displayMenuLockedLine1;
   public static String displayMenuLockedLine2;
   public static String hypeWaveEndedMessage;

   public static void load(FileConfiguration config) {
      debug = config.getBoolean("debug", false);
      storeUrl = config.getString("store-url", "store.infusedpvp.org");
      serverName = config.getString("server-name", "Curse");
      botName = config.getString("bot-name", "&a&lCurse Bot");
      if (botName.equals("&5&lCurse Bot")) {
         botName = "&a&lCurse Bot";
         config.set("bot-name", botName);
         org.minecurse.lootbags.LootBagPlugin.getInstance().saveConfig();
      }
      botNamePlain = config.getString("bot-name-plain", "Curse Bot");
      String rawCrateBroadcast = config.getString("crate-broadcast", "&5&l%server-name% &7┃ &6%player%&f is opening %lootbag%");
      crateBroadcast = rawCrateBroadcast.replace("%server-name%", serverName);
      saleBroadcastLine1 = applyPlaceholders(config.getString("sale-broadcast.line1", "&7Unlock the %lootbag% &r &7here!"));
      saleBroadcastLine2 = applyPlaceholders(config.getString("sale-broadcast.line2", "&b&n%store-url%"));
      crateHologramLine = applyPlaceholders(config.getString("crate-hologram-line", "&dUnlock at &d%store-url%"));
      displayMenuLockedLine1 = applyPlaceholders(config.getString("display-menu.locked-line1", "&c&l(!) &CThis item is locked!"));
      displayMenuLockedLine2 = applyPlaceholders(config.getString("display-menu.locked-line2", "&7Purchase this item at &n%store-url%&7!"));
      hypeWaveEndedMessage = applyPlaceholders(config.getString("hype-wave-ended", "&cThe Hype Wave has ended!"));
   }

   private static String applyPlaceholders(String input) {
      return input == null ? "" : input.replace("%store-url%", storeUrl);
   }
}
