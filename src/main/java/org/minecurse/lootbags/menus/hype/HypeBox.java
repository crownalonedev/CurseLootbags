package org.minecurse.lootbags.menus.hype;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.runnable.RunnableBuilder;
import org.minecurse.commons.utils.PlayerUtils;
import org.minecurse.commons.utils.RandomUtil;
import org.minecurse.commons.utils.StringUtil;
import org.minecurse.commons.utils.inventory.ClickableItem;
import org.minecurse.commons.utils.inventory.InventoryListener;
import org.minecurse.commons.utils.inventory.SmartInventory;
import org.minecurse.commons.utils.inventory.content.InventoryContents;
import org.minecurse.commons.utils.inventory.content.InventoryProvider;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.events.HypeBoxFinishEvent;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootbags.utils.ColorUtil;
import org.minecurse.lootmanager.struct.Rarity;
import org.minecurse.lootmanager.struct.Reward;
import org.minecurse.lootmanager.utils.RewardUtils;
import org.minecurse.modules.utils.fanciful.FancyMessage;

public class HypeBox implements InventoryProvider {
   private static final ItemBuilder SIDES = new ItemBuilder(Material.STAINED_GLASS_PANE).durability(7).name("&7");
   private final List<ItemStack> displayingItems = new ArrayList<>();
   private final LootBag lootBag;
   private final Player opener;
   private final SmartInventory inventory;
   private final int tickEndingPoint;
   private final Reward reward;
   private float pitch;
   private int ticks;
   private boolean justRigged;
   private boolean finished = false;
   private boolean alreadyCalled = false;
   private final int rerollAttempts;

   public HypeBox(LootBag lootBag, Player opener) {
      this.lootBag = lootBag;
      this.opener = opener;
      this.tickEndingPoint = 46;
      this.justRigged = false;
      this.ticks = 0;
      this.rerollAttempts = 0;
      this.pitch = 1.1F;
      LootBagPlugin.getInstance().getHypeManager().getActiveHypeBoxes().add(this);

      for (int i = 0; i < this.tickEndingPoint; i++) {
         ItemStack displayItem = this.safeRewardItem(RewardUtils.getRandomReward(this.getLootBag().getRewards()));
         if (displayItem == null) {
            displayItem = new ItemStack(Material.BARRIER);
         }
         this.displayingItems.add(displayItem);
      }

      for (Player all : Bukkit.getOnlinePlayers()) {
         all.sendMessage("");
         all.sendMessage(LootBagPlugin.hypePrefix(opener.getDisplayName() + " &fis opening a &r" + lootBag.getDisplayName()));
         if (all != opener) {
            new FancyMessage("")
               .text(StringUtil.color("&a&l[CLICK TO WATCH]"))
               .tooltip(ColorUtil.Green + "Click Here To View!")
               .command("/supersecrethypeviewcommand " + opener.getUniqueId())
               .send(all);
         }

         all.sendMessage("");
      }

      this.reward = RewardUtils.getRandomReward(lootBag.getRewards());
      ItemStack rewardItem = this.safeRewardItem(this.reward);
      this.displayingItems.set(41, rewardItem != null ? rewardItem : new ItemStack(Material.BARRIER));
      this.inventory = SmartInventory.builder()
         .id("hype-box")
         .provider(this)
         .size(3, 9)
         .title(StringUtil.color(lootBag.getDisplayName() + "&8 - Hype Box"))
         .closeable(true)
         .clickable(false)
         .listener(new InventoryListener(InventoryCloseEvent.class, event -> {
            if (!this.finished) {
               LootBagPlugin.getInstance().getHypeManager().getActiveHypeBoxes().remove(this);
               if (!this.alreadyCalled) {
                  this.alreadyCalled = true;
                  HypeBoxFinishEvent finishEvent = new HypeBoxFinishEvent(this.getOpener(), this, this.reward);
                  finishEvent.call();
               }
            }
         }))
         .build();
      this.inventory.open(opener);
      LootBagPlugin.getInstance().getHypeManager().addPlayer(opener.getPlayer());
   }

   public static ItemBuilder getSIDES() {
      return SIDES;
   }

   public void init(Player player, InventoryContents contents) {
      contents.fillBorders(ClickableItem.empty(SIDES));
      contents.set(4, ClickableItem.empty(this.lootBag.getLootBag()));

      for (int i = 0; i < 9; i++) {
         int itemIndex = (this.displayingItems.size() - 1 - i + this.ticks) % this.displayingItems.size();
         contents.set(9 + i, ClickableItem.empty(this.displayingItems.get(itemIndex)));
      }

      if (this.ticks >= 30 && !this.justRigged) {
         List<Reward> filtered = this.lootBag.getRewards().stream().filter(reward -> reward.getRarity() == Rarity.LEGENDARY).collect(Collectors.toList());
         if (!filtered.isEmpty()) {
            ItemStack rigged1 = this.safeRewardItem(filtered.get(RandomUtil.getRandInt(0, filtered.size() - 1)));
            ItemStack rigged2 = this.safeRewardItem(filtered.get(RandomUtil.getRandInt(0, filtered.size() - 1)));
            if (rigged1 != null) this.displayingItems.set(3, rigged1.clone());
            if (rigged2 != null) this.displayingItems.set(5, rigged2.clone());
            this.justRigged = true;
         }
      }

      if (this.ticks >= this.tickEndingPoint) {
         RunnableBuilder.bind(() -> {
            PlayerUtils.playSound(player, Sound.LEVEL_UP, 3.0F);
            if (this.reward != null && contents.get(1, 4).isPresent()) {
               this.finished = true;
               if (!this.alreadyCalled) {
                  this.alreadyCalled = true;
                  HypeBoxFinishEvent finishEvent = new HypeBoxFinishEvent(player, this, this.reward);
                  finishEvent.call();
               }
               this.closeInventory(player);
            } else {
               PlayerUtils.addItems(player, new ItemStack[]{this.lootBag.getLootBag()});
               this.finished = true;
               this.closeInventory(player);
            }
         }).runSyncLater(15L);
      }
   }

   public void update(Player player, InventoryContents contents) {
      if (this.tickEndingPoint > this.ticks) {
         int diff = this.tickEndingPoint - this.ticks;
         int interval = diff >= 6 ? this.ticks / 8 + 1 : (diff >= 3 ? this.ticks / 3 + 1 : this.ticks / 2 + 1);
         int state = (Integer)contents.property("state", 0);
         contents.setProperty("state", state + 1);
         if (state % interval == 0) {
            PlayerUtils.playSound(player, Sound.NOTE_PLING, this.pitch);
            this.pitch += 0.02F;
            this.ticks++;
            this.init(player, contents);
         }
      }
   }

   public List<ItemStack> getDisplayingItems() {
      return this.displayingItems;
   }

   public LootBag getLootBag() {
      return this.lootBag;
   }

   public Player getOpener() {
      return this.opener;
   }

   public SmartInventory getInventory() {
      return this.inventory;
   }

   public int getTickEndingPoint() {
      return this.tickEndingPoint;
   }

   public float getPitch() {
      return this.pitch;
   }

   public int getTicks() {
      return this.ticks;
   }

   public Reward getReward() {
      return this.reward;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public boolean isAlreadyCalled() {
      return this.alreadyCalled;
   }

   private void closeInventory(Player player) {
      if (player.isOnline()) {
         player.closeInventory();
      }
   }

   private ItemStack safeRewardItem(Reward reward) {
      if (reward == null) {
         return null;
      }

      try {
         return reward.getItemStack();
      } catch (Exception var3) {
         return null;
      }
   }
}
