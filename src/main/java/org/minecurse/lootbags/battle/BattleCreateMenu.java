package org.minecurse.lootbags.battle;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.utils.LocUtil;
import org.minecurse.commons.utils.PlayerUtils;
import org.minecurse.commons.utils.inventory.ClickableItem;
import org.minecurse.commons.utils.inventory.SmartInventory;
import org.minecurse.commons.utils.inventory.content.InventoryContents;
import org.minecurse.commons.utils.inventory.content.InventoryProvider;
import org.minecurse.inventorypets.utils.SkullUtils;
import org.minecurse.inventorypets.utils.SkullUtils.Type;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.struct.CrateType;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootbags.utils.LootBagUtils;

public class BattleCreateMenu implements InventoryProvider {
   private final LootBag lootBag;
   private final int count;
   private final SmartInventory inventory;

   public SmartInventory getInventory() {
      return this.inventory;
   }

   public BattleCreateMenu(LootBag lootBag, int count) {
      this.lootBag = lootBag;
      this.count = count;
      this.inventory = SmartInventory.builder().title("Hype Box Battle Creation").size(1, 9).provider(this).build();
   }

   public void init(Player player, InventoryContents contents) {
      ItemBuilder itemBuilder1 = new ItemBuilder(SkullUtils.fromName(Type.ITEM, player.getName()))
         .name("&a&lBattle Player")
         .lore("&fClick here to battle a player.");
      ItemBuilder itemBuilder2 = new ItemBuilder(BattleManager.getInstance().getBotHead()).name(org.minecurse.lootbags.settings.Settings.botName).lore("&fClick here to battle the bot.");
      contents.set(2, ClickableItem.of(itemBuilder1, event -> {
         ItemStack hand = player.getItemInHand();
         LootBag lootBag = LootBagUtils.getLootBag(hand);
         if (lootBag == null || lootBag.getType() != CrateType.HYPE_BOX) {
            player.sendMessage(LootBagPlugin.hypePrefix("&CYou must be holding a valid Hype Box."));
            PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
            player.closeInventory();
         } else if (LootBagUtils.countLootBags(player, lootBag) >= this.count) {
            LootBagUtils.removeLootBags(player, lootBag, this.count);
            player.closeInventory();
            new BattleInfo(new PlayerBattle(player, false), null, lootBag, this.count);
            player.sendMessage(LootBagPlugin.hypePrefix("&aYour battle has been created!"));
            PlayerUtils.playSound(player, Sound.ANVIL_USE, 1.25F);

            net.md_5.bungee.api.chat.BaseComponent[] msg = net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
               LootBagPlugin.hypePrefix("&e" + player.getName() + " &fhas created a battle for &b" + this.count + "x &r" + lootBag.getDisplayName() + "&f! ")
            );
            net.md_5.bungee.api.chat.TextComponent click = new net.md_5.bungee.api.chat.TextComponent(
               net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&a&l[CLICK TO VIEW]")
            );
            click.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
               net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/hypebox battle"
            ));
            click.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
               net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
               net.md_5.bungee.api.chat.TextComponent.fromLegacyText(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&7Click to join the battle!"))
            ));

            net.md_5.bungee.api.chat.TextComponent finalMsg = new net.md_5.bungee.api.chat.TextComponent("");
            for (net.md_5.bungee.api.chat.BaseComponent b : msg) {
               finalMsg.addExtra(b);
            }
            finalMsg.addExtra(click);

            for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
               p.spigot().sendMessage(finalMsg);
            }
         } else {
            player.sendMessage(LootBagPlugin.hypePrefix("&cYou need at least 1 Hype box to create a battle."));
            PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
            player.closeInventory();
         }
      }));
      ItemStack lb = this.lootBag.getLootBag();
      lb.setAmount(this.count);
      contents.set(4, ClickableItem.empty(lb));
      contents.set(6, ClickableItem.of(itemBuilder2, event -> {
         ItemStack hand = player.getItemInHand();
         LootBag lootBag = LootBagUtils.getLootBag(hand);
         if (lootBag == null || lootBag.getType() != CrateType.HYPE_BOX) {
            player.sendMessage(LootBagPlugin.hypePrefix("&CYou must be holding a valid Hype Box."));
            PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
            player.closeInventory();
         } else if (LootBagUtils.countLootBags(player, lootBag) >= this.count) {
            LootBagUtils.removeLootBags(player, lootBag, this.count);

            player.sendMessage(LootBagPlugin.hypePrefix("&aYour battle has been created!"));
            BattleInfo battle = new BattleInfo(new PlayerBattle(player, false), new PlayerBattle(player, true), lootBag, this.count);
            battle.start();
         } else {
            player.sendMessage(LootBagPlugin.hypePrefix("&cYou need at least 1 Hype box to create a battle."));
            PlayerUtils.playSound(player, Sound.ITEM_BREAK, 0.5F);
            player.closeInventory();
         }
      }));
   }
}
