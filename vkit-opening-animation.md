Use this prompt in the other plugin:

````text
You are working in a Bukkit/Paper Minecraft plugin. Port the Hype Box opening animation below into this plugin and adapt package names, plugin singleton access, reward APIs, and menu/inventory APIs to match the target plugin.

Goal:
- Implement the Hype Box opening animation as an inventory spinner.
- Opening a Hype Box should create a 3x9 inventory.
- Slot 4 shows the Hype Box item.
- The middle row slots 9 through 17 show a rolling strip of reward preview items.
- The animation plays NOTE_PLING with increasing pitch while rolling.
- The animation slows down as it approaches the end.
- At tick 46, it lands on the preselected reward, plays LEVEL_UP, fires/handles a finish event, gives the reward, broadcasts the result, and removes the player from the "opening hype boxes" tracking set.
- Closing the inventory before completion should still finish once and give the preselected reward.
- While a player is opening, block commands, chat, teleport glitches, and unrelated inventory opens.

Important behavior from the source:
- The winning reward is selected once in the constructor with `RewardUtils.getRandomReward(lootBag.getRewards())`.
- `displayingItems` is prefilled with 46 random reward item previews.
- The winning reward item is forced into `displayingItems[41]`, which aligns with the spinner stop point.
- At `ticks >= 30`, two legendary preview items are placed near the strip to create a fake near-miss effect.
- `alreadyCalled` prevents duplicate finish handling from close and normal completion.
- `finished` prevents the close listener from firing after normal completion.

External APIs used in the original code:
- Bukkit/Paper: `Player`, `Bukkit`, `Material`, `Sound`, `ItemStack`, inventory events.
- CurseCommons inventory framework: `SmartInventory`, `InventoryProvider`, `InventoryContents`, `ClickableItem`, `InventoryListener`.
- CurseCommons utilities: `ItemBuilder`, `RunnableBuilder`, `PlayerUtils`, `RandomUtil`, `StringUtil`.
- Loot manager: `Reward`, `RewardUtils`, `Rarity`.
- Fanciful chat: `FancyMessage`.

If the target plugin does not have `SmartInventory`, replace it with a normal Bukkit `Inventory` plus a `BukkitRunnable` that updates slots 9-17 at the same intervals. Keep the same state variables and finish safeguards.
`HypeChatTask` is optional. Omit `createHypeChat()` or replace it with the target plugin's W/L chat reward flow if that feature does not exist.

Source code to port:

```java
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
            } else {
               PlayerUtils.addItems(player, new ItemStack[]{this.lootBag.getLootBag()});
               this.finished = true;
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
```

Optional watcher view used by the click-to-watch command:

```java
package org.minecurse.lootbags.menus.hype;

import java.util.List;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.utils.PlayerUtils;
import org.minecurse.commons.utils.StringUtil;
import org.minecurse.commons.utils.inventory.ClickableItem;
import org.minecurse.commons.utils.inventory.SmartInventory;
import org.minecurse.commons.utils.inventory.content.InventoryContents;
import org.minecurse.commons.utils.inventory.content.InventoryProvider;

public class HypeBoxView implements InventoryProvider {
   private final HypeBox hypeBox;
   private final int tickEndingPoint;
   private final List<ItemStack> displayingItems;
   private final SmartInventory inventory;
   private float pitch;
   private int ticks;

   public HypeBoxView(HypeBox hypeBox) {
      this.hypeBox = hypeBox;
      this.tickEndingPoint = hypeBox.getTickEndingPoint();
      this.ticks = hypeBox.getTicks();
      this.displayingItems = hypeBox.getDisplayingItems();
      this.inventory = SmartInventory.builder()
         .id("hype-box-" + hypeBox.getOpener().getUniqueId())
         .provider(this)
         .size(3, 9)
         .title(StringUtil.color(hypeBox.getLootBag().getDisplayName() + " &8- " + hypeBox.getOpener().getDisplayName()))
         .build();
      this.pitch = 1.1F;
   }

   public void init(Player player, InventoryContents contents) {
      contents.fillBorders(ClickableItem.empty(HypeBox.getSIDES()));
      contents.set(4, ClickableItem.empty(this.hypeBox.getLootBag().getLootBag()));

      for (int i = 0; i < 9; i++) {
         int itemIndex = (this.displayingItems.size() - 1 - i + this.ticks) % this.displayingItems.size();
         contents.set(9 + i, ClickableItem.empty(this.displayingItems.get(itemIndex)));
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

   public SmartInventory getInventory() {
      return this.inventory;
   }
}
```

```java
package org.minecurse.lootbags.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.minecurse.lootbags.menus.hype.HypeBox;
import org.minecurse.lootmanager.struct.Reward;

public class HypeBoxFinishEvent extends Event {
   private static final HandlerList HANDLERS_LIST = new HandlerList();
   private final Player player;
   private final HypeBox hypeBox;
   private final Reward item;

   public HypeBoxFinishEvent(Player player, HypeBox hypeBox, Reward item) {
      this.player = player;
      this.hypeBox = hypeBox;
      this.item = item;
   }

   public static HandlerList getHandlerList() {
      return HANDLERS_LIST;
   }

   public HandlerList getHandlers() {
      return HANDLERS_LIST;
   }

   public void call() {
      Bukkit.getServer().getPluginManager().callEvent(this);
   }

   public Player getPlayer() {
      return this.player;
   }

   public HypeBox getHypeBox() {
      return this.hypeBox;
   }

   public Reward getItem() {
      return this.item;
   }
}
```

```java
package org.minecurse.lootbags.manager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.menus.hype.HypeBox;
import org.minecurse.lootbags.tasks.HypeChatTask;

public class HypeManager {
   private final List<HypeBox> activeHypeBoxes = new ArrayList<>();
   private final Set<UUID> openingHypeBoxes = new HashSet<>();
   private HypeChatTask hypeChatTask;

   public List<HypeBox> getActiveHypeBoxes() {
      return this.activeHypeBoxes;
   }

   public Set<UUID> getOpeningHypeBoxes() {
      return this.openingHypeBoxes;
   }

   public void setHypeChatTask(HypeChatTask hypeChatTask) {
      this.hypeChatTask = hypeChatTask;
   }

   public HypeChatTask getHypeChatTask() {
      return this.hypeChatTask;
   }

   public void createHypeChat() {
      if (this.hypeChatTask == null) {
         this.hypeChatTask = new HypeChatTask(this);
         this.hypeChatTask.runTaskTimerAsynchronously(LootBagPlugin.getInstance(), 0L, 20L);
      }
   }

   public void removePlayer(Player player) {
      this.openingHypeBoxes.remove(player.getUniqueId());
   }

   public void addPlayer(Player player) {
      this.openingHypeBoxes.add(player.getUniqueId());
   }

   public HypeBox getByUUID(UUID uuid) {
      return this.activeHypeBoxes.stream().filter(hypeBox -> hypeBox.getOpener().getUniqueId().equals(uuid)).findFirst().orElse(null);
   }
}
```

Finish handling from the listener:

```java
@EventHandler
public void onHypeOpen(HypeBoxFinishEvent event) {
   Player player = event.getPlayer();
   this.plugin.getHypeManager().removePlayer(event.getPlayer());
   this.plugin.getHypeManager().getActiveHypeBoxes().remove(event.getHypeBox());
   Reward item = event.getItem();

   for (Player all : Bukkit.getOnlinePlayers()) {
      all.sendMessage(
         StringUtil.color(
            "&c&lH&E&LY&A&LP&B&LE&D&LB&C&LO&E&LX &8-> &r"
               + player.getDisplayName()
               + " &fopened a &r"
               + event.getHypeBox().getLootBag().getDisplayName()
               + "&f and received "
               + ItemUtil.getDisplayName(item.getItemStack().clone())
               + "&f! &a&lW &for &c&lL&f?"
         )
      );
   }

   item.handleGive(player);
   this.plugin.getHypeManager().createHypeChat();
}
```

Player lock protections from the listener:

```java
@EventHandler
public void onQuit(PlayerQuitEvent event) {
   this.plugin.getHypeManager().removePlayer(event.getPlayer());
}

@EventHandler
public void onDeath(PlayerDeathEvent event) {
   this.plugin.getHypeManager().removePlayer(event.getEntity());
}

@EventHandler
public void onKick(PlayerKickEvent event) {
   this.plugin.getHypeManager().removePlayer(event.getPlayer());
}

@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
public void onCommand(PlayerCommandPreprocessEvent event) {
   if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(event.getPlayer().getUniqueId())) {
      event.setMessage("");
      event.setCancelled(true);
   }
}

@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
public void onChat(AsyncPlayerChatEvent event) {
   if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
   }
}

@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
public void onTeleport(PlayerTeleportEvent event) {
   if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
   }
}

@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
public void onOpen(InventoryOpenEvent event) {
   Player player = (Player) event.getPlayer();
   UUID playerUUID = player.getUniqueId();
   if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(playerUUID)) {
      if (event.getView().getTopInventory().getTitle() == null || !event.getView().getTopInventory().getTitle().contains(StringUtil.color("&8 - Hype Box"))) {
         event.setCancelled(true);
      }
   }
}
```

Opening hook from the lootbag flow:

```java
if (this.getType() == CrateType.HYPE_BOX) {
   new HypeBox(this, player);
   if (remove) {
      ItemStack hand = player.getItemInHand();
      if (hand.getAmount() > 1) {
         hand.setAmount(hand.getAmount() - 1);
         player.setItemInHand(hand);
      } else {
         player.setItemInHand(null);
      }
   }
}
```

Implementation checklist:
- Register the manager during plugin enable.
- Register the Hype Box listener.
- Add the open hook wherever the target plugin consumes/opens its Hype Box item.
- Replace `LootBag`, `Reward`, `RewardUtils`, and `Rarity.LEGENDARY` with target plugin equivalents.
- Replace `SmartInventory` with target plugin inventory framework or plain Bukkit inventory task.
- Keep the one-shot finish guard: `finished` plus `alreadyCalled`.
- Keep player cleanup on quit, death, kick, and successful finish.
- Test normal completion, early close, player quit during roll, empty/null reward item fallback, and duplicate finish prevention.
````
