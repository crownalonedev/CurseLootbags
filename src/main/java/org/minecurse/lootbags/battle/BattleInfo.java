package org.minecurse.lootbags.battle;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.struct.LootBag;

public class BattleInfo {
   private final LootBag lootBag;
   private final int count;
   private PlayerBattle playerOne;
   private PlayerBattle playerTwo;
   private BattleMenu battleMenu;
   private boolean isActive = false;
   private final List<Player> spectators = new ArrayList<>();

   public List<Player> getSpectators() {
      return this.spectators;
   }

   public LootBag getLootBag() {
      return this.lootBag;
   }

   public int getCount() {
      return this.count;
   }

   public PlayerBattle getPlayerOne() {
      return this.playerOne;
   }

   public void setPlayerOne(PlayerBattle playerOne) {
      this.playerOne = playerOne;
   }

   public PlayerBattle getPlayerTwo() {
      return this.playerTwo;
   }

   public void setPlayerTwo(PlayerBattle playerTwo) {
      this.playerTwo = playerTwo;
   }

   public BattleMenu getBattleMenu() {
      return this.battleMenu;
   }

   public BattleInfo(PlayerBattle playerOne, PlayerBattle playerTwo, LootBag lootBag, int count) {
      this.playerOne = playerOne;
      this.playerTwo = playerTwo;
      this.lootBag = lootBag;
      this.count = count;
      BattleManager.getInstance().getActiveBattles().add(this);
   }

   public boolean isActive() {
      return this.isActive;
   }

   public void start() {
      this.isActive = true;
      this.playerOne.getPlayer().sendMessage(LootBagPlugin.hypePrefix("&aYour battle has started!"));
      this.battleMenu = new BattleMenu(this);
      this.battleMenu.getInventory().open(this.playerOne.getPlayer());
      if (!this.playerTwo.isBot()) {
         this.battleMenu.getInventory().open(this.playerTwo.getPlayer());
         this.playerTwo.getPlayer().sendMessage(LootBagPlugin.hypePrefix("&aYour battle has started!"));
      }

      String p1Name = this.playerOne.getPlayer().getDisplayName();
      String p2Name = this.playerTwo.isBot() ? org.minecurse.lootbags.settings.Settings.botName : this.playerTwo.getPlayer().getDisplayName();

      net.md_5.bungee.api.chat.BaseComponent[] msg = net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
         LootBagPlugin.hypePrefix("&fA new battle " + p1Name + " &fvs. " + p2Name + " &fwith &b" + this.count + "x &r" + this.lootBag.getDisplayName() + "&f is occurring! ")
      );
      net.md_5.bungee.api.chat.TextComponent click = new net.md_5.bungee.api.chat.TextComponent(
         net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&a&l[CLICK TO WATCH]")
      );
      click.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
         net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/hypebox watch " + this.playerOne.getPlayer().getName()
      ));
      click.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
         net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, 
         net.md_5.bungee.api.chat.TextComponent.fromLegacyText(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', "&7Click to spectate the battle!"))
      ));

      net.md_5.bungee.api.chat.TextComponent finalMsg = new net.md_5.bungee.api.chat.TextComponent("");
      for (net.md_5.bungee.api.chat.BaseComponent b : msg) {
         finalMsg.addExtra(b);
      }
      finalMsg.addExtra(click);

      for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
         p.spigot().sendMessage(finalMsg);
      }
   }
}
