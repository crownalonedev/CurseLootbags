package org.minecurse.lootbags.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.battle.BattleInfo;
import org.minecurse.lootbags.battle.BattleManager;
import org.minecurse.lootbags.battle.BattleQueueMenu;
import org.minecurse.lootbags.menus.hype.HypeBoxListMenu;
import org.minecurse.lootbags.utils.DropUtils;

@CommandAlias("hypebox|hypeboxes|hype")
public class HypeBoxCommand extends BaseCommand {
   @Default
   public void onDefault(Player player) {
      HypeBoxListMenu.getInventory().open(player);
   }

   @Subcommand("battle")
   public void onBattle(Player player) {
      BattleQueueMenu.getInventory().open(player);
   }

   @Subcommand("removebattle")
   public void onRemove(Player player) {
      BattleManager battleManager = LootBagPlugin.getInstance().getBattleManager();
      BattleInfo battleInfo = null;

      for (BattleInfo battleLoop : battleManager.getActiveBattles()) {
         if (battleLoop.getPlayerOne().getPlayer().getName().equals(player.getName()) && battleLoop.getPlayerTwo() == null && !battleLoop.isActive()) {
            battleInfo = battleLoop;
         }
      }

      if (battleInfo == null) {
         player.sendMessage(LootBagPlugin.hypePrefix("&cYou could not cancel your Hype Box battle, maybe it is running."));
      } else {
         for (int i = 0; i < battleInfo.getCount(); i++) {
            ItemStack itemStack = battleInfo.getLootBag().getLootBag();
            DropUtils.giveOrDropProtectedItem(player, itemStack);
         }
         battleManager.getActiveBattles().remove(battleInfo);
      }
   }

   @Subcommand("watch")
   public void onWatch(Player player, String targetName) {
      for (BattleInfo info : BattleManager.getInstance().getActiveBattles()) {
         if (info.isActive() && (info.getPlayerOne().getPlayer().getName().equalsIgnoreCase(targetName) || (!info.getPlayerTwo().isBot() && info.getPlayerTwo().getPlayer().getName().equalsIgnoreCase(targetName)))) {
            if (!info.getSpectators().contains(player)) {
               info.getSpectators().add(player);
            }
            info.getBattleMenu().getSpectatorInventory().open(player);
            player.sendMessage(LootBagPlugin.hypePrefix("&aYou are now spectating the battle!"));
            return;
         }
      }
      player.sendMessage(LootBagPlugin.hypePrefix("&cThat player is not currently in an active battle."));
   }
}
