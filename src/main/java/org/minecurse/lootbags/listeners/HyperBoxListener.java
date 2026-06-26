package org.minecurse.lootbags.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.utils.StringUtil;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.battle.BattleInfo;
import org.minecurse.lootbags.battle.BattleMenu;
import org.minecurse.lootbags.events.HypeBoxFinishEvent;
import org.minecurse.lootbags.settings.Settings;
import org.minecurse.lootmanager.struct.Reward;
import org.minecurse.modules.utils.ItemUtil;

public class HyperBoxListener implements Listener {
   private final LootBagPlugin plugin;
   private final Map<UUID, Integer> pendingCreativeDrops = new HashMap<>();

   public HyperBoxListener(LootBagPlugin plugin) {
      this.plugin = plugin;
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.plugin.getHypeManager().removePlayer(event.getPlayer());
   }

   @EventHandler
   public void onQuit(PlayerDeathEvent event) {
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
   public void onCommand(AsyncPlayerChatEvent event) {
      if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onHypeOpen(HypeBoxFinishEvent event) {
      Player player = event.getPlayer();
      this.plugin.getHypeManager().removePlayer(event.getPlayer());
      this.plugin.getHypeManager().getActiveHypeBoxes().remove(event.getHypeBox());
      Reward item = event.getItem();

      for (Player all : Bukkit.getOnlinePlayers()) {
         all.sendMessage(
            StringUtil.color(
               "&c&lH&E&LY&A&LP&B&LE&D&LB&C&LO&E&LX &8⤖ &r"
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

   @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
   public void onGlitch(PlayerTeleportEvent event) {
      if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
   public void onInteract(PlayerInteractEvent event) {
      if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)
         && event.getClickedBlock() != null
         && event.getClickedBlock().getType().name().contains("CHEST")
         && this.plugin.getHypeManager().getOpeningHypeBoxes().contains(event.getPlayer().getUniqueId())) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
   public void onOpen(InventoryOpenEvent event) {
      Player player = (Player)event.getPlayer();
      UUID playerUUID = player.getUniqueId();
      if (this.plugin.getHypeManager().getOpeningHypeBoxes().contains(playerUUID)) {
         if (event.getView().getTopInventory().getTitle() == null || !event.getView().getTopInventory().getTitle().contains(StringUtil.color("&8 - Hype Box"))) {
            event.setCancelled(true);
         }
      } else {
         BattleInfo battleInfo = null;

         for (BattleInfo battleLoop : LootBagPlugin.getInstance().getBattleManager().getActiveBattles()) {
            if (battleLoop.getPlayerOne() != null
                  && battleLoop.getPlayerOne().getPlayer() != null
                  && battleLoop.getPlayerOne().getPlayer().getName().equals(player.getName())
               || battleLoop.getPlayerTwo() != null
                  && battleLoop.getPlayerTwo().getPlayer() != null
                  && battleLoop.getPlayerTwo().getPlayer().getName().equals(player.getName())) {
               battleInfo = battleLoop;
               break;
            }
         }

         if (battleInfo != null) {
            if (event.getView().getTopInventory().getTitle() != null
               && event.getView().getTopInventory().getTitle().contains(StringUtil.color("&8 - Hype Box Battle"))) {
               return;
            }

            event.setCancelled(true);
         }
      }
   }

   @EventHandler
   public void onInventoryClick1(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof BattleMenu) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onInventoryDrag(InventoryDragEvent event) {
      if (event.getInventory().getHolder() instanceof BattleMenu) {
         event.setCancelled(true);
      }
   }

   @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
   public void onHypeChat(AsyncPlayerChatEvent event) {
      if (this.plugin.getHypeManager().getHypeChatTask() != null) {
         Player player = event.getPlayer();
         String message = event.getMessage().toUpperCase();
         if (message.equalsIgnoreCase("W") || message.equalsIgnoreCase("L")) {
            if (this.plugin.getHypeManager().getHypeChatTask().getRedeemed().contains(player.getUniqueId())) {
               return;
            }

            this.plugin.getHypeManager().getHypeChatTask().getRedeemed().add(player.getUniqueId());
            Bukkit.getScheduler()
               .runTask(LootBagPlugin.getInstance(), () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "gold give " + player.getName() + " 5"));
         }
      }
   }

   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      Player player = (Player)event.getWhoClicked();
      this.scheduleInventoryUpdate(player, "inventory click");
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onCreativeDrop(InventoryCreativeEvent event) {
      if (event.getRawSlot() >= 0) {
         return;
      }

      Player player = (Player)event.getWhoClicked();
      ItemStack cursor = event.getCursor();
      if (player.getGameMode() != GameMode.CREATIVE || cursor == null || cursor.getType() == Material.AIR) {
         return;
      }

      if (event.isCancelled()) {
         if (Settings.debug) {
            this.plugin.getLogger().info("Creative category drop was cancelled for " + player.getName() + ".");
         }
         return;
      }

      ItemStack drop = cursor.clone();
      UUID playerId = player.getUniqueId();
      this.incrementPendingCreativeDrop(playerId);
      if (Settings.debug) {
         this.plugin.getLogger().info("Tracking creative category drop for " + player.getName() + ": " + drop.getType() + " x" + drop.getAmount() + ".");
      }

      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (!this.consumePendingCreativeDrop(playerId)) {
            return;
         }

         if (player.isOnline()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
            if (Settings.debug) {
               this.plugin.getLogger().info("Manually spawned missing creative category drop for " + player.getName() + ".");
            }
         }
      }, 1L);
   }

   @EventHandler(priority = EventPriority.MONITOR)
   public void onDrop(PlayerDropItemEvent event) {
      Player player = event.getPlayer();
      if (player.getGameMode() == GameMode.CREATIVE && this.consumePendingCreativeDrop(player.getUniqueId()) && Settings.debug) {
         this.plugin.getLogger().info(
            "Server " + (event.isCancelled() ? "cancelled" : "handled") + " creative category drop for " + player.getName() + "."
         );
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      Player player = (Player)event.getPlayer();
      this.scheduleInventoryUpdate(player, "inventory close");
   }

   private void scheduleInventoryUpdate(Player player, String reason) {
      if (player.getGameMode() == GameMode.CREATIVE) {
         if (Settings.debug) {
            this.plugin.getLogger().info("Skipping forced inventory update for creative player " + player.getName() + " after " + reason + ".");
         }
         return;
      }

      Bukkit.getScheduler().runTaskLater(LootBagPlugin.getInstance(), () -> {
         if (player.isOnline()) {
            player.updateInventory();
         }
      }, 1L);
   }

   private void incrementPendingCreativeDrop(UUID playerId) {
      Integer count = this.pendingCreativeDrops.get(playerId);
      this.pendingCreativeDrops.put(playerId, count == null ? 1 : count + 1);
   }

   private boolean consumePendingCreativeDrop(UUID playerId) {
      Integer count = this.pendingCreativeDrops.get(playerId);
      if (count == null || count <= 0) {
         return false;
      }

      if (count == 1) {
         this.pendingCreativeDrops.remove(playerId);
      } else {
         this.pendingCreativeDrops.put(playerId, count - 1);
      }

      return true;
   }
}
