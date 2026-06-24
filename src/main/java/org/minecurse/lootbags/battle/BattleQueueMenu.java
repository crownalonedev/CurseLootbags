package org.minecurse.lootbags.battle;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.utils.LocUtil;
import org.minecurse.commons.utils.PlayerUtils;
import org.minecurse.commons.utils.StringUtil;
import org.minecurse.commons.utils.inventory.ClickableItem;
import org.minecurse.commons.utils.inventory.SmartInventory;
import org.minecurse.commons.utils.inventory.content.InventoryContents;
import org.minecurse.commons.utils.inventory.content.InventoryProvider;
import org.minecurse.inventorypets.utils.SkullUtils;
import org.minecurse.inventorypets.utils.SkullUtils.Type;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.settings.Settings;
import org.minecurse.lootbags.struct.CrateType;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootbags.utils.LootBagUtils;

public class BattleQueueMenu implements InventoryProvider {
   private static final SmartInventory inventory = SmartInventory.builder().title("Hype Box Battles").provider(new BattleQueueMenu()).size(1, 9).build();

   public void init(Player player, InventoryContents contents) {
      int slot = 0;

      for (BattleInfo info : BattleManager.getInstance().getActiveBattles()) {
         if (!info.isActive()) {
            Player battler = info.getPlayerOne().getPlayer();
            ItemBuilder itemBuilder1 = new ItemBuilder(SkullUtils.fromName(Type.ITEM, battler.getName()))
               .name(StringUtil.color(battler.getDisplayName() + "&f's Hype Battle"))
               .lore(
                  new String[]{
                     "",
                     "&f&l • &fType: " + info.getLootBag().getDisplayName(),
                     " &F&l• &fAmount: &b" + info.getCount(),
                     "",
                     "&7Click here to join the battle."
                  }
               );
            contents.set(
               slot++,
               ClickableItem.of(
                  itemBuilder1,
                  event -> {
                     ItemStack hand = player.getItemInHand();
                     LootBag lootBag = LootBagUtils.getLootBag(hand);
                     if (lootBag == null || lootBag.getType() != CrateType.HYPE_BOX) {
                        player.sendMessage(LootBagPlugin.hypePrefix("&CYou must be holding a valid Hype Box."));
                        PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
                     } else if (battler == player) {
                        player.sendMessage(LootBagPlugin.hypePrefix("&CYou can not battle yourself."));
                     } else {
                        int totalBoxes = LootBagUtils.countLootBags(player, lootBag);
                        if (totalBoxes < info.getCount()) {
                           player.sendMessage(LootBagPlugin.hypePrefix("&CYou must have {0} of these Hype Box in your inventory.", info.getCount()));
                           PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
                        } else if (!battler.isOnline()) {
                           player.sendMessage(LootBagPlugin.hypePrefix("&CThis player has logged out."));
                           PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
                        } else if (LootBagPlugin.getInstance().getBattleManager().getActiveBattles().size() > 3) {
                           player.sendMessage(LootBagPlugin.hypePrefix("&CThere is currently 4 Hype Box Battles running, please wait."));
                           battler.sendMessage(
                              LootBagPlugin.hypePrefix("&cA player tried to join your Hype Box Battle but there was already 3 running! &7(/hypebox removebattle)")
                           );
                        } else if (info.isActive()) {
                           player.sendMessage(LootBagPlugin.hypePrefix("&CThis battle is already on-going."));
                           PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
                        } else if (!BattleManager.getInstance().getActiveBattles().contains(info)) {
                           player.sendMessage(LootBagPlugin.hypePrefix("&cThis hypebox battle no longer exists."));
                           PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
                        } else {
                           LootBagUtils.removeLootBags(player, lootBag, info.getCount());
                           info.setPlayerTwo(new PlayerBattle(player, false));
                           player.closeInventory();
                           info.start();
                        }
                     }
                  }
               )
            );
         }
      }

      ItemBuilder itemBuilder = new ItemBuilder(Material.STAINED_GLASS_PANE)
         .durability(14)
         .name("&a&lEnter Hype Box Battle Queue")
         .lore(
            new String[]{
               "",
               "&fClick to create a Hype Box Battle",
               "&fwhilst holding your Hype Boxes.",
               "",
               "&c&lH&6&ly&e&lp&a&le &9&lB&5&lo&c&lx &e&lB&a&la&3&lt&9&lt&5&ll&c&le&6&ls&e&l?",
               "&fHype Box Battles are battles between either",
               "&fanother player or the " + Settings.botNamePlain + ". You will",
               "&feach open a certain amount of Hype Boxes, and",
               "&fwhoever has reached a \"higher\" total value of",
               "&fthe rewards won, will win all the rewards. If",
               "&fthe battle ends in a tie, a 50 / 50 will decide",
               "&fwho wins it all!",
               ""
            }
         );
      contents.set(8, ClickableItem.of(itemBuilder, event -> {
         ItemStack hand = player.getItemInHand();
         LootBag lootBag = LootBagUtils.getLootBag(hand);
         if (lootBag == null || lootBag.getType() != CrateType.HYPE_BOX) {
            player.sendMessage(LootBagPlugin.hypePrefix("&CYou must be holding a Hype Box."));
            PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
         } else {
            int totalBoxes = LootBagUtils.countLootBags(player, lootBag);
            if (totalBoxes < 3) {
               player.sendMessage(LootBagPlugin.hypePrefix("&cYou need at least 3 Hype box to create a battle."));
               PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
            } else {
               PlayerUtils.playSound(player, Sound.ANVIL_USE, 1.25F);
               new BattleCreateMenu(lootBag, totalBoxes).getInventory().open(player);
            }
         }
      }));
   }

   public static SmartInventory getInventory() {
      return inventory;
   }
}
