package org.minecurse.lootbags;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import java.util.stream.Collectors;
import net.minelink.ctplus.TagManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.minecurse.commons.utils.StringUtil;
import org.minecurse.lootbags.battle.BattleManager;
import org.minecurse.lootbags.commands.HypeBoxCommand;
import org.minecurse.lootbags.commands.HypeViewCommand;
import org.minecurse.lootbags.commands.LootBagCommand;
import org.minecurse.lootbags.commands.LootBagsCommand;
import org.minecurse.lootbags.listeners.HyperBoxListener;
import org.minecurse.lootbags.listeners.LootBagListener;
import org.minecurse.lootbags.manager.AnimationManager;
import org.minecurse.lootbags.manager.HypeManager;
import org.minecurse.lootbags.manager.LootBagManager;
import org.minecurse.lootbags.settings.Settings;
import org.minecurse.lootbags.struct.CrateAnimation;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootbags.tasks.LootBagSaleTask;

public class LootBagPlugin extends JavaPlugin {
   private LootBagManager manager;
   private PaperCommandManager paperCommandManager;
   private HypeManager hypeManager;
   private AnimationManager animationManager;
   private TagManager tagManager;
   private BattleManager battleManager;

   public LootBagManager getManager() {
      return this.manager;
   }

   public PaperCommandManager getPaperCommandManager() {
      return this.paperCommandManager;
   }

   public HypeManager getHypeManager() {
      return this.hypeManager;
   }

   public AnimationManager getAnimationManager() {
      return this.animationManager;
   }

   public TagManager getTagManager() {
      return this.tagManager;
   }

   public BattleManager getBattleManager() {
      return this.battleManager;
   }

   public static LootBagPlugin getInstance() {
      return (LootBagPlugin)getPlugin(LootBagPlugin.class);
   }

   public static String prefix(String string, Object... arguments) {
      return StringUtil.color("&a&lLootbags &8➼ &7" + StringUtil.colorFormat(string, arguments));
   }

   public static String hypePrefix(String str, Object... objects) {
      return StringUtil.color("&c&lH&E&LY&A&LP&B&LE&D&LB&C&LO&E&LX &8➼ &f" + StringUtil.colorFormat(str, objects));
   }

   public void onEnable() {
      this.saveDefaultConfig();
      this.getConfig().options().copyDefaults(true);
      this.saveConfig();
      Settings.load(this.getConfig());
      this.init();
      this.registerListeners();
      this.registerCommands();
      this.hypeManager = new HypeManager();
      this.animationManager = new AnimationManager();
      this.battleManager = new BattleManager();
   }

   private void init() {
      this.manager = new LootBagManager(this, this.getDataFolder().getPath(), "lootbags.json");
      this.manager.initialize();
      this.manager.loadFromDisk();
      new LootBagSaleTask().runTaskTimer(this, 500L, 8400L);
   }

   private void registerCommands() {
      this.paperCommandManager = new PaperCommandManager(this);
      this.paperCommandManager.getCommandContexts().registerContext(LootBag.class, c -> {
         String arg = c.popFirstArg();
         LootBag bag = LootBagManager.getInstance().getByName(arg);
         if (bag != null) {
            return bag;
         } else {
            throw new InvalidCommandArgument(prefix("&cThe loot bag '{0}' does not exist.", arg));
         }
      });
      this.paperCommandManager
         .getCommandCompletions()
         .registerCompletion("lootBags", c -> LootBagManager.getInstance().getLootBags().stream().map(LootBag::getInternalName).collect(Collectors.toList()));
      this.paperCommandManager.registerCommand(new LootBagCommand());
      this.paperCommandManager.registerCommand(new HypeBoxCommand());
      this.paperCommandManager.registerCommand(new HypeViewCommand(this));
      this.paperCommandManager.registerCommand(new LootBagsCommand());
   }

   private void registerListeners() {
      PluginManager manager = this.getServer().getPluginManager();
      manager.registerEvents(new LootBagListener(this), this);
      manager.registerEvents(new HyperBoxListener(this), this);
   }

   public void onDisable() {
      this.manager.saveToDisk();
      this.animationManager.getAnimationHashMap().values().forEach(CrateAnimation::killAnimation);
   }
}
