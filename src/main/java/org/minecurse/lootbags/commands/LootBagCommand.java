package org.minecurse.lootbags.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.menu.button.Button;
import org.minecurse.commons.menu.type.paginated.PaginatedMenu;
import org.minecurse.commons.utils.PlayerUtils;
import org.minecurse.commons.utils.RandomUtil;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.manager.LootBagManager;
import org.minecurse.lootbags.menus.LootBagCreationMenu;
import org.minecurse.lootbags.menus.LootBagDisplayMenu;
import org.minecurse.lootbags.menus.LootbagCategoryMenu;
import org.minecurse.lootbags.struct.CrateAnimation;
import org.minecurse.lootbags.struct.CrateType;
import org.minecurse.lootbags.struct.HalfType;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootmanager.struct.Reward;
import org.minecurse.lootmanager.utils.RewardUtils;

@CommandAlias("el|lootbox|lootboxes")
public class LootBagCommand extends BaseCommand {
   @HelpCommand
   @Default
   public void onHelp(CommandSender sender) {
      if (sender instanceof Player) {
         if (sender.hasPermission("curse.admin")) {
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el give <player> <lootBag> <amount>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el givehalf <player> <lootBag> <left:right>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el create <internalName>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el edit <lootBag>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el setdisplay <lootBag> <name...>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el settexture <lootBag> <base64|remove>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el delete <lootBag>"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el list"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el gui"));
            sender.sendMessage(LootBagPlugin.prefix("&cUsage: /el save"));
         } else {
            LootBagDisplayMenu.getInventory().open((Player)sender);
         }
      }
   }

   @Subcommand("display")
   public void onDisplay(Player player) {
      LootBagDisplayMenu.getInventory().open(player);
   }

   @CommandPermission("curse.admin")
   @Subcommand("testanimation")
   public void onAnimation(Player player) {
      LootBag bag = LootBagPlugin.getInstance().getManager().findLootBag("lootbox_takeover");

      for (int i = 0; i < 75; i++) {
         new CrateAnimation(player, bag, bag.getMaxRewards());
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("list")
   public void onList(CommandSender sender) {
      LootBagManager manager = LootBagManager.getInstance();
      if (manager.getLootBags().isEmpty()) {
         sender.sendMessage(LootBagPlugin.prefix("&cThere are no lootbags available."));
      } else {
         manager.getLootBags()
            .forEach(lootBag -> LootBagPlugin.prefix("Lootbag - Internal({0}&7), Display({1}&r&7).", lootBag.getInternalName(), lootBag.getDisplayName()));
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("gui")
   public void onGui(Player player) {
      player.sendMessage(LootBagPlugin.prefix("You are now viewing the Lootbag gui."));
      PlayerUtils.playSound(player, Sound.LEVEL_UP, 0.75F);
      new LootbagCategoryMenu().show(player);
   }

   @CommandPermission("curse.admin")
   @Subcommand("edit")
   @CommandCompletion("@lootBags")
   public void onEdit(Player player, LootBag bag) {
      player.sendMessage(LootBagPlugin.prefix("You are now editing the {0} &7lootbag.", bag.getInternalName()));
      PlayerUtils.playSound(player, Sound.LEVEL_UP, 0.75F);
      new LootBagCreationMenu(bag).show(player);
   }

   @CommandPermission("curse.admin")
   @Subcommand("setdisplay|displayname|rename")
   @CommandCompletion("@lootBags")
   public void onSetDisplay(CommandSender sender, LootBag bag, @Flags("greedy") String displayName) {
      if (displayName == null || displayName.trim().isEmpty()) {
         sender.sendMessage(LootBagPlugin.prefix("&cYou must provide a display name."));
         return;
      }

      bag.setDisplayName(displayName);
      bag.setDisplay(displayName);
      sender.sendMessage(LootBagPlugin.prefix("The display name of {0} &7is now {1}&7.", bag.getInternalName(), bag.getDisplayName()));
      if (sender instanceof Player) {
         PlayerUtils.playSound((Player)sender, Sound.LEVEL_UP, 0.75F);
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("settexture|texture")
   @CommandCompletion("@lootBags")
   public void onSetTexture(CommandSender sender, LootBag bag, @Flags("greedy") String base64) {
      if (base64 == null || base64.trim().isEmpty()) {
         sender.sendMessage(LootBagPlugin.prefix("&cYou must provide a base64 texture value (or 'remove')."));
         return;
      }

      if (base64.trim().equalsIgnoreCase("remove") || base64.trim().equalsIgnoreCase("clear") || base64.trim().equalsIgnoreCase("null")) {
         bag.removeTexture();
         sender.sendMessage(LootBagPlugin.prefix("The base64 texture has been removed from {0}.", bag.getInternalName()));
      } else {
         try {
            bag.setTexture(base64);
            sender.sendMessage(LootBagPlugin.prefix("The base64 texture has been applied to {0}.", bag.getInternalName()));
         } catch (Exception var5) {
            sender.sendMessage(LootBagPlugin.prefix("&cThat does not appear to be a valid base64 texture value."));
            return;
         }
      }

      if (sender instanceof Player) {
         PlayerUtils.playSound((Player)sender, Sound.LEVEL_UP, 0.75F);
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("save")
   public void onSave(CommandSender sender) {
      LootBagManager.getInstance().saveToDisk();
      sender.sendMessage(LootBagPlugin.prefix("All lootbags have been saved to disk."));
   }

   @CommandPermission("curse.admin")
   @Subcommand("delete")
   @CommandCompletion("@lootBags")
   public void onDelete(CommandSender sender, LootBag bag) {
      sender.sendMessage(LootBagPlugin.prefix("The lootbag {0} &7has been deleted.", bag.getInternalName()));
      LootBagManager.getInstance().removeLootBag(bag);
   }

   @CommandPermission("curse.admin")
   @Subcommand("create")
   public void onCreate(Player player, String arg) {
      LootBagManager manager = LootBagManager.getInstance();
      if (manager.isLootBag(arg)) {
         player.sendMessage(LootBagPlugin.prefix("&cThis name is already taken."));
      } else {
         LootBag lootBag = new LootBag(arg);
         manager.addLootBag(lootBag);
         player.sendMessage(LootBagPlugin.prefix("You are now creating the {0} lootbag.", lootBag.getInternalName()));
         PlayerUtils.playSound(player, Sound.LEVEL_UP, 0.75F);
         new LootBagCreationMenu(lootBag).show(player);
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("givehalf|half")
   @CommandCompletion("@players @lootBags")
   public void onGive(CommandSender sender, OnlinePlayer targetPlayer, LootBag bag, @Default("LEFT") HalfType type) {
      Player target = targetPlayer != null ? targetPlayer.getPlayer() : (Player)sender;
      PlayerUtils.giveOrDropProtectedItem(target, HalfType.build(bag, type), 30);
      if (target != sender) {
         sender.sendMessage(LootBagPlugin.prefix("You've given {0} a {1}&r &7loot bag Half.", target.getName(), bag.getDisplayName()));
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("give")
   @CommandCompletion("@players @lootBags")
   public void onGiveHalf(CommandSender sender, OnlinePlayer targetPlayer, LootBag bag, @Default("1") Integer amount) {
      Player target = targetPlayer != null ? targetPlayer.getPlayer() : (Player)sender;
      ItemStack itemStack = bag.getLootBag();
      itemStack.setAmount(amount);
      PlayerUtils.giveOrDropProtectedItem(target, itemStack, 30);
      target.sendMessage(LootBagPlugin.prefix("You've received a {0}&r &7loot bag.", bag.getDisplayName()));
      if (target != sender) {
         sender.sendMessage(LootBagPlugin.prefix("You've given {0} a {1}&r &7loot bag.", target.getName(), bag.getDisplayName()));
      }
   }

   @CommandPermission("curse.admin")
   @Subcommand("testloot")
   @CommandCompletion("@lootBags nothing")
   public void onTestLoot(Player player, LootBag bag, @Default("1") Integer amount) {
      ArrayList<Reward> pulled = new ArrayList<>();
      if (bag.getType() == CrateType.BUNDLE) {
         player.sendMessage(LootBagPlugin.prefix("&cYou may not generate rewards of bundles with this."));
      } else if (bag.getRewards().isEmpty()) {
         player.sendMessage(LootBagPlugin.prefix("&cThere are no rewards for this lootbag."));
      } else {
         if (bag.getType() == CrateType.MONTHLY) {
            for (int x = 0; x < amount; x++) {
               int pulls = bag.getMaxRewards() - 1;

               for (int y = 0; y < pulls; y++) {
                  Reward reward2 = RewardUtils.getRandomReward(
                     bag.getRewards().stream().filter(reward1 -> !bag.getBonusRewards().contains(reward1)).collect(Collectors.toList())
                  );
                  pulled.add(reward2);
               }

               Reward bonus = RewardUtils.getRandomReward(bag.getBonusRewards());
               pulled.add(bonus);
            }
         } else if (bag.getType() == CrateType.HYPE_BOX) {
            for (int x = 0; x < amount; x++) {
               Reward reward3 = RewardUtils.getRandomReward(bag.getRewards());
               pulled.add(reward3);
            }
         } else {
            for (int x = 0; x < amount; x++) {
               int pulls = bag.getMinRewards() == bag.getMaxRewards() ? bag.getMaxRewards() : RandomUtil.getRandInt(bag.getMinRewards(), bag.getMaxRewards());

               for (int y = 0; y < pulls; y++) {
                  Reward reward4 = RewardUtils.getRandomReward(bag.getRewards());
                  pulled.add(reward4);
               }
            }
         }

         player.sendMessage(LootBagPlugin.prefix("Finished pulling rewards for {0}&7.", bag.getDisplayName()));
         PlayerUtils.playSound(player, Sound.LEVEL_UP, 1.0F);
         PaginatedMenu menu = new PaginatedMenu("Lootbag Loot Test", 6, 28);
         menu.fillSides(Button.PLACEHOLDER);
         pulled.forEach(
            reward -> menu.addButton(
               new ItemBuilder(reward.getItemStack().clone())
                  .amount(reward.getMin() == reward.getMax() ? reward.getMin() : RandomUtil.getRandInt(reward.getMin(), reward.getMax()))
            )
         );
         menu.buildInventory();
         menu.show(player);
      }
   }
}
