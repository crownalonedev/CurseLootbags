package org.minecurse.lootbags.menus;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.menu.button.Button;
import org.minecurse.commons.menu.type.chest.ChestMenu;
import org.minecurse.commons.utils.PlayerUtils;
import org.minecurse.commons.utils.SerializedItemStack;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.manager.LootBagManager;
import org.minecurse.lootbags.menus.rewards.RewardMenu;
import org.minecurse.lootbags.struct.AnimationType;
import org.minecurse.lootbags.struct.CrateType;
import org.minecurse.lootbags.struct.EditType;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootmanager.LootManagerPlugin;
import org.minecurse.lootmanager.utils.RetroUtils;

public class LootBagCreationMenu implements Listener {
   private final LootBag lootBag;
   private final ChestMenu menu;
   private final HashMap<UUID, EditType> editType = new HashMap<>();

   public LootBagCreationMenu(LootBag lootBag) {
      this.lootBag = lootBag;
      Bukkit.getServer().getPluginManager().registerEvents(this, LootManagerPlugin.getInstance());
      this.menu = new ChestMenu("Lootbag Creation: " + lootBag.getInternalName(), 6);
   }

   public void show(Player player) {
      ItemStack item = this.lootBag.getLootBag();
      ItemBuilder lore = new ItemBuilder(Material.EMPTY_MAP)
         .name("&6&lEdit Lore")
         .lore(
            this.lootBag.getItemStack().hasItemMeta() && this.lootBag.getItemStack().getItemMeta().hasLore()
               ? this.lootBag.getItemStack().getItemMeta().getLore()
               : Lists.newArrayList(new String[]{"&fNo Lore Found..."})
         )
         .lore("")
         .lore("&7Click here to edit the lootbag's lore.");
      ItemBuilder rewards = new ItemBuilder(Material.STORAGE_MINECART)
         .name("&e&lEdit Rewards")
         .lore("&7Amount of Rewards: &f" + this.lootBag.getRewards().size())
         .lore("")
         .lore("&7Click here to edit the lootbag's rewards.");
      ItemBuilder glow = new ItemBuilder(Material.SLIME_BLOCK)
         .name("&f&lItem Glow")
         .lore("&7is Glowing: &f" + RetroUtils.formatBoolean(this.lootBag.isGlowing()))
         .lore("")
         .lore("&7Click here to toggle the lootbag's item glow.");
      ItemBuilder broadcast = new ItemBuilder(this.lootBag.isBroadcast() ? Material.REDSTONE_TORCH_ON : Material.TORCH)
         .name("&d&lBroadcast Loot")
         .lore("&7Broadcasting: &f" + RetroUtils.formatBoolean(this.lootBag.isBroadcast()))
         .lore("")
         .lore("&7Click here to toggle the lootbag's broadcasting.");
      ItemBuilder internal = new ItemBuilder(Material.PAPER)
         .name("&3&lRename Internal Name")
         .lore("&7Current Internal Name: &f" + this.lootBag.getInternalName())
         .lore("")
         .lore("&7Click here to edit the lootbag's internal name.");
      ItemBuilder display = new ItemBuilder(Material.PAPER)
         .name("&3&lRename Display Name")
         .lore("&7Current Display Name: &f" + this.lootBag.getDisplayName())
         .lore("")
         .lore("&7Click here to edit the lootbag's display name.");
      ItemBuilder material = new ItemBuilder(this.lootBag.getMaterial())
         .name("&b&lMaterial")
         .lore("&7Current Material: &f" + this.lootBag.getMaterial().name())
         .lore("")
         .lore("&7Click here to edit the lootbag's material.");
      boolean hasTexture = this.lootBag.hasTexture();
      ItemBuilder texture = new ItemBuilder(Material.SKULL_ITEM)
         .durability(hasTexture ? 3 : 0)
         .name("&5&lBase64 Texture")
         .lore("&7Custom Texture: &f" + (hasTexture ? "Yes" : "No"))
         .lore("")
         .lore("&7Click here to set a base64 player-head texture.")
         .lore("&7Paste the base64 texture value in chat.")
         .lore("")
         .lore("&7Type 'remove' in chat to clear the texture.");
      ItemBuilder bundle = new ItemBuilder(Material.WORKBENCH)
         .name("&a&lBundle")
         .lore("&7Is Bundle: &f" + RetroUtils.formatBoolean(this.lootBag.isBundle()))
         .lore("")
         .lore("&7Click here to edit the lootbag's internal name.");
      ItemBuilder min = new ItemBuilder(Material.ARROW)
         .name("&c&lMinimum Items")
         .lore("&7Current Amount: &f" + this.lootBag.getMinRewards() + " / " + this.lootBag.getMaxRewards())
         .lore("")
         .lore("&7Click here to up the limit of items received.");
      ItemBuilder max = new ItemBuilder(Material.ARROW)
         .name("&a&lMaximum Items")
         .lore("&7Current Max: &f" + this.lootBag.getMaxRewards())
         .lore("")
         .lore("&7Click here to up the limit of items received.");
      ItemBuilder alwaysMax = new ItemBuilder(Material.ARROW)
         .name("&d&lAlways Max Items")
         .lore("&7Always Max: &f" + RetroUtils.formatBoolean(this.lootBag.isAlwaysMax()))
         .lore("")
         .lore("&7Click here to up the limit of items received.");
      ItemBuilder showcase = new ItemBuilder(Material.BOOK)
         .name("&a&lShowcase Lootbag")
         .lore("&7Toggled: &f" + RetroUtils.formatBoolean(this.lootBag.isShowcasedLootBag()))
         .lore("")
         .lore("&7Click here to toggle this lootbag.");
      ItemBuilder lootBagType = new ItemBuilder(Material.ENDER_PORTAL_FRAME)
         .name("&2&lLootbag Type")
         .lore("&7Type: &f" + this.lootBag.getType().name())
         .lore("")
         .lore("&7Click here to toggle this lootbag.");
      ItemBuilder bonusLore = new ItemBuilder(Material.WOOL)
         .name("&e&lBonus Lore")
         .lore("&7Toggle: &f" + RetroUtils.formatBoolean(this.lootBag.isBonusLore()))
         .lore("")
         .lore("&7Click here to toggle this lootbag.");
      ItemBuilder hideRewardLore = new ItemBuilder(Material.WOOL)
         .name("&2&lReward Lore")
         .lore("&7Toggle: &f" + RetroUtils.formatBoolean(this.lootBag.isBonusLore()))
         .lore("")
         .lore("&7Click here to toggle this lootbag.");
      ItemBuilder animationType = new ItemBuilder(Material.GLOWSTONE)
         .name("&6&lAnimation Type")
         .lore("&7Animation Type: &f" + this.lootBag.getAnimationType())
         .lore("")
         .lore("&7Click here to change this lootbag.");
      ItemBuilder metaData = new ItemBuilder(Material.ENDER_CHEST).name("&d&lCopy Meta Data").lore("&7Click here to transfer the items meta data");
      this.menu.fillSides(Button.PLACEHOLDER);
      this.menu.setButton(19, new Button(rewards, (player1, clickInformation) -> new RewardMenu(this.lootBag).show(player)));
      this.menu.setButton(20, new Button(lore, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.LORE);
         player.sendMessage(LootBagPlugin.prefix("You are now editing this lootbag's lore."));
         player.sendMessage(LootBagPlugin.prefix("Once you are holding a valid item with LORE on it, type anything"));
         player.sendMessage(LootBagPlugin.prefix("to confirm the lore or type CANCEL / NULL to cancel the process."));
         player.closeInventory();
      }));
      this.menu.setButton(34, new Button(glow, (player1, clickInformation) -> {
         boolean glowing = this.lootBag.isGlowing();
         this.lootBag.setGlowing(!glowing);
         player.sendMessage(LootBagPlugin.prefix("{0} Glow is now &7{1}.", this.lootBag.getInternalName(), RetroUtils.formatBoolean(this.lootBag.isGlowing())));
         this.refreshMenu(player);
      }));
      this.menu.setButton(33, new Button(lootBagType, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.LOOTBAG_TYPE);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Type"));
         player.sendMessage(LootBagPlugin.prefix("You must type a valid lootbag " + Arrays.toString(CrateType.values()) + ")"));
         player.closeInventory();
      }));
      this.menu.setButton(10, new Button(internal, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.INTERNAL);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Internal Name"));
         player.sendMessage(LootBagPlugin.prefix("the current internal name is '" + this.lootBag.getInternalName() + "&7'!"));
         player.closeInventory();
      }));
      this.menu.setButton(11, new Button(display, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.DISPLAY);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Display"));
         player.sendMessage(LootBagPlugin.prefix("&7the currently display name is '" + this.lootBag.getDisplayName() + "&7'!"));
         player.closeInventory();
      }));
      this.menu
         .setButton(
            28,
            new Button(
               bonusLore,
               (player1, clickInformation) -> {
                  boolean hasBonus = this.lootBag.isBonusLore();
                  this.lootBag.setBonusLore(!hasBonus);
                  player.sendMessage(
                     LootBagPlugin.prefix(
                        this.lootBag.getInternalName() + " Bonus Lore is now &7" + RetroUtils.formatBoolean(this.lootBag.isBonusLore()) + "&7."
                     )
                  );
                  this.refreshMenu(player);
               }
            )
         );
      this.menu
         .setButton(
            37,
            new Button(
               hideRewardLore,
               (player1, clickInformation) -> {
                  boolean isRewardLore = this.lootBag.isRewardLore();
                  this.lootBag.setRewardLore(!isRewardLore);
                  player.sendMessage(
                     LootBagPlugin.prefix(
                        this.lootBag.getInternalName() + " Reward Lore is now &7" + RetroUtils.formatBoolean(this.lootBag.isRewardLore()) + "&7."
                     )
                  );
                  this.refreshMenu(player);
               }
            )
         );
      this.menu.setButton(38, new Button(metaData, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.META);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Meta Data"));
         player.sendMessage(LootBagPlugin.prefix("Hold any item the contains a custom lore, display name, etc.. and type."));
         player.closeInventory();
      }));
      this.menu
         .setButton(
            29,
            new Button(
               broadcast,
               (player1, clickInformation) -> {
                  boolean broadcasting = this.lootBag.isBroadcast();
                  this.lootBag.setBroadcast(!broadcasting);
                  player.sendMessage(
                     LootBagPlugin.prefix(
                        this.lootBag.getInternalName() + " Broadcast is now &7" + RetroUtils.formatBoolean(this.lootBag.isBroadcast()) + "&7."
                     )
                  );
                  this.refreshMenu(player);
               }
            )
         );
      this.menu
         .setButton(
            24,
            new Button(
               bundle,
               (player1, clickInformation) -> {
                  boolean isBundle = this.lootBag.isBundle();
                  this.lootBag.setBundle(!isBundle);
                  player.sendMessage(
                     LootBagPlugin.prefix(this.lootBag.getInternalName() + " Bundle is now &7" + RetroUtils.formatBoolean(this.lootBag.isBundle()) + "&7.")
                  );
                  this.refreshMenu(player);
               }
            )
         );
      this.menu.setButton(15, new Button(min, (player1, clickInformation) -> {
         if (this.lootBag.isBundle()) {
            player.sendMessage(LootBagPlugin.prefix("&cThis cannot be changed since bundle is active."));
         } else if (this.lootBag.isAlwaysMax()) {
            player.sendMessage(LootBagPlugin.prefix("&cThis cannot be changed since always max is active."));
         } else {
            this.editType.put(player.getUniqueId(), EditType.MIN);
            player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Min Amount"));
            player.sendMessage(LootBagPlugin.prefix("You must type a number between (1 / " + this.lootBag.getMaxRewards() + ")"));
            player.closeInventory();
         }
      }));
      this.menu.setButton(16, new Button(max, (player1, clickInformation) -> {
         if (this.lootBag.isBundle()) {
            player.sendMessage(LootBagPlugin.prefix("&cThis cannot be changed since bundle is active."));
         } else if (this.lootBag.isAlwaysMax()) {
            player.sendMessage(LootBagPlugin.prefix("&cThis cannot be changed since always max is active."));
         } else {
            this.editType.put(player.getUniqueId(), EditType.MAX);
            player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Max Amount"));
            player.sendMessage(LootBagPlugin.prefix("The current max amount is " + this.lootBag.getMaxRewards()));
            player.closeInventory();
         }
      }));
      this.menu
         .setButton(
            25,
            new Button(
               alwaysMax,
               (player1, clickInformation) -> {
                  if (this.lootBag.isBundle()) {
                     player.sendMessage(LootBagPlugin.prefix("&cThis cannot be changed since bundle is active."));
                  } else {
                     boolean isAlwaysMax = this.lootBag.isAlwaysMax();
                     this.lootBag.setAlwaysMax(!isAlwaysMax);
                     player.sendMessage(
                        LootBagPlugin.prefix(
                           this.lootBag.getInternalName() + " &7Always Max is now " + RetroUtils.formatBoolean(this.lootBag.isAlwaysMax()) + "&7."
                        )
                     );
                     this.refreshMenu(player);
                  }
               }
            )
         );
      this.menu.setButton(40, new Button(material, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.MATERIAL);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Material"));
         player.sendMessage(LootBagPlugin.prefix("Hold a valid material and type anything to set its content."));
         player.closeInventory();
      }));
      this.menu.setButton(41, new Button(texture, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.TEXTURE);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Base64 Texture"));
         if (this.lootBag.hasTexture()) {
            player.sendMessage(LootBagPlugin.prefix("&7A custom texture is currently set on this lootbag."));
         } else {
            player.sendMessage(LootBagPlugin.prefix("&7No custom texture is currently set."));
         }
         player.sendMessage(LootBagPlugin.prefix("&7Paste the base64 texture value (from heads.minecraftfolder.com etc) in chat."));
         player.sendMessage(LootBagPlugin.prefix("&7Type 'remove' to clear the texture, or 'cancel' to abort."));
         player.closeInventory();
      }));
      this.menu.setButton(43, new Button(animationType, (player1, clickInformation) -> {
         this.editType.put(player.getUniqueId(), EditType.ANIMATION_TYPE);
         player.sendMessage(LootBagPlugin.prefix("You are now editing the lootbag's Animation Type"));
         player.sendMessage(LootBagPlugin.prefix("Select one of the following types " + Arrays.toString(AnimationType.values())));
         player.closeInventory();
      }));
      this.menu.setButton(22, new Button(item, (player1, clickInformation) -> player.getInventory().addItem(new ItemStack[]{item})));
      this.menu
         .setButton(
            30,
            new Button(
               showcase,
               (player1, clickInformation) -> {
                  if (LootBagManager.getInstance().isAlreadyToggled() && LootBagManager.getInstance().findShowcasedLootBag() != this.lootBag) {
                     player.sendMessage(
                        LootBagPlugin.prefix(
                           "&cYou must toggle off the \"" + LootBagManager.getInstance().findShowcasedLootBag().getInternalName() + "\" lootbag."
                        )
                     );
                  } else {
                     this.lootBag.setShowcasedLootBag(!this.lootBag.isShowcasedLootBag());
                     player.sendMessage(
                        LootBagPlugin.prefix(
                           this.lootBag.getInternalName() + " &7Showcase is now &7" + RetroUtils.formatBoolean(this.lootBag.isShowcasedLootBag()) + "&7."
                        )
                     );
                     this.refreshMenu(player);
                  }
               }
            )
         );
      this.menu
         .setButton(
            49,
            new Button(
               new ItemBuilder(Material.ARROW).name("&bGo Back").lore("&7Go back to select").lore("&7a new lootbag."),
               (player1, clickInformation) -> new LootbagListMenu(this.lootBag.getType()).show(player1)
            )
         );
      this.menu.buildInventory();
      this.menu.show(player);
   }

   private void refreshMenu(Player player) {
      LootBagCreationMenu menu = new LootBagCreationMenu(this.lootBag);
      menu.show(player);
      this.sound(player);
   }

   private void onFinish(Player player) {
      this.refreshMenu(player);
      this.editType.remove(player.getUniqueId());
      player.sendMessage(LootBagPlugin.prefix("Your current lootbag action has been completed."));
   }

   @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
   public void onChat(AsyncPlayerChatEvent event) {
      Player player = event.getPlayer();
      UUID playerId = player.getUniqueId();
      EditType type = this.editType.get(playerId);
      if (type != null) {
         if (!player.hasPermission("curse.admin")) {
            this.editType.remove(playerId);
         } else {
            event.setCancelled(true);
            String message = event.getMessage().toLowerCase();
            switch (type) {
               case META:
                  ItemStack handx = player.getItemInHand();
                  if (handx == null) {
                     player.sendMessage(LootBagPlugin.prefix("&cThis is not a valid material / item."));
                  } else {
                     if (!handx.hasItemMeta()) {
                        player.sendMessage(LootBagPlugin.prefix("&cThis item does not contain any item data."));
                        return;
                     }

                     ItemMeta meta = handx.getItemMeta();
                     this.lootBag.setMaterial(handx.getType());
                     if (meta.hasDisplayName()) {
                        this.lootBag.setDisplayName(meta.getDisplayName());
                     }

                     if (meta.hasLore()) {
                        this.lootBag.setLore(meta.getLore());
                     }

                     this.lootBag.setItem(new SerializedItemStack(handx).get());
                     this.lootBag.setMaterial(handx.getType());
                     this.onFinish(player);
                  }
                  break;
               case MATERIAL:
                  ItemStack hand = player.getItemInHand();
                  if (hand == null) {
                     player.sendMessage(LootBagPlugin.prefix("&cThis is not a valid material / item."));
                  } else {
                     this.lootBag.setMaterial(hand.getType());
                     this.onFinish(player);
                  }
                  break;
               case ANIMATION_TYPE:
                  AnimationType animationType;
                  try {
                     animationType = AnimationType.valueOf(message.toUpperCase());
                  } catch (Exception var17) {
                     player.sendMessage(LootBagPlugin.prefix("That is not a valid lootbag type!"));
                     return;
                  }

                  this.lootBag.setAnimationType(animationType);
                  this.onFinish(player);
                  break;
               case LOOTBAG_TYPE:
                  CrateType crateType;
                  try {
                     crateType = CrateType.valueOf(message.toUpperCase());
                  } catch (Exception var16) {
                     player.sendMessage(LootBagPlugin.prefix("That is not a valid lootbag type!"));
                     return;
                  }

                  this.lootBag.setType(crateType);
                  this.onFinish(player);
                  break;
               case MIN:
                  int amountx;
                  try {
                     amountx = Integer.parseInt(message);
                  } catch (Exception var19) {
                     player.sendMessage(LootBagPlugin.prefix("&cInvalid amount."));
                     break;
                  }

                  if (amountx <= this.lootBag.getMaxRewards() && amountx > 0) {
                     this.lootBag.setMinRewards(amountx);
                     this.onFinish(player);
                  } else {
                     player.sendMessage(LootBagPlugin.prefix("&cPlease choose a different amount."));
                     player.sendMessage(LootBagPlugin.prefix("This amount goes over the max amount [MAX: " + this.lootBag.getMaxRewards() + "]"));
                  }
                  break;
               case MAX:
                  int amount;
                  try {
                     amount = Integer.parseInt(message);
                  } catch (Exception var18) {
                     player.sendMessage(LootBagPlugin.prefix("&cInvalid amount."));
                     break;
                  }

                  if (amount <= 0) {
                     player.sendMessage(LootBagPlugin.prefix("&cPlease choose a different amount."));
                     player.sendMessage(LootBagPlugin.prefix("You cannot go below one max reward."));
                  } else {
                     this.lootBag.setMaxRewards(amount);
                     this.onFinish(player);
                  }
                  break;
               case INTERNAL:
                  Pattern regex = Pattern.compile("[a-zA-Z0-9_]*");
                  Matcher match = regex.matcher(message);
                  if (!match.matches()) {
                     player.sendMessage(LootBagPlugin.prefix("&cYou can only use A-Z letters."));
                  } else if (message.equalsIgnoreCase("cancel")) {
                     this.editType.remove(playerId);
                     player.sendMessage(LootBagPlugin.prefix("You have canceled the internal name process."));
                  } else {
                     String upper = message.toUpperCase();
                     if (LootBagPlugin.getInstance().getManager().isLootBag(upper)) {
                        player.sendMessage(LootBagPlugin.prefix("&cThis name is already taken."));
                     } else {
                        this.lootBag.setInternalName(upper);
                        this.onFinish(player);
                     }
                  }
                  break;
               case DISPLAY:
                  this.lootBag.setDisplayName(event.getMessage());
                  this.lootBag.setDisplay(event.getMessage());
                  this.onFinish(player);
                  break;
               case LORE:
                  if (message.equalsIgnoreCase("cancel")) {
                     this.editType.remove(player.getUniqueId());
                     player.sendMessage(LootBagPlugin.prefix("You have canceled the lore process."));
                     this.menu.show(player);
                     this.sound(player);
                     return;
                  }

                  if (message.equalsIgnoreCase("null")) {
                     this.editType.remove(player.getUniqueId());
                     player.sendMessage(LootBagPlugin.prefix("You have reset the lootbags lore."));
                     this.lootBag.setLore(Lists.newArrayList());
                     this.menu.show(player);
                     this.sound(player);
                     return;
                  }

                  ItemStack item = player.getItemInHand();
                  if (item == null || item.getType().equals(Material.AIR)) {
                     player.sendMessage(LootBagPlugin.prefix("&cYou must be holding a valid item."));
                     return;
                  }

                  if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                     ItemMeta itemMeta = item.getItemMeta();
                     this.lootBag.setLore(itemMeta.getLore());
                     this.onFinish(player);
                     player.sendMessage(LootBagPlugin.prefix("Your current lootbag action has been completed."));
                  } else {
                     player.sendMessage(LootBagPlugin.prefix("&cYou must be holding a valid item."));
                  }
                  break;
               case TEXTURE:
                  if (message.equalsIgnoreCase("cancel")) {
                     this.editType.remove(player.getUniqueId());
                     player.sendMessage(LootBagPlugin.prefix("You have canceled the texture process."));
                     this.menu.show(player);
                     this.sound(player);
                     return;
                  }

                  String textureValue = event.getMessage();
                  if (textureValue.equalsIgnoreCase("remove") || textureValue.equalsIgnoreCase("clear") || textureValue.equalsIgnoreCase("null")) {
                     this.lootBag.removeTexture();
                     player.sendMessage(LootBagPlugin.prefix("The base64 texture has been removed from {0}.", this.lootBag.getInternalName()));
                     this.onFinish(player);
                     return;
                  }

                  try {
                     this.lootBag.setTexture(textureValue);
                     player.sendMessage(LootBagPlugin.prefix("The base64 texture has been applied to {0}.", this.lootBag.getInternalName()));
                     this.onFinish(player);
                  } catch (Exception var20) {
                     player.sendMessage(LootBagPlugin.prefix("&cThat does not appear to be a valid base64 texture value."));
                     this.editType.remove(player.getUniqueId());
                     this.menu.show(player);
                     this.sound(player);
                  }
            }
         }
      }
   }

   private void sound(Player player) {
      PlayerUtils.playSound(player, Sound.LEVEL_UP, 0.75F);
   }

   public ChestMenu getMenu() {
      return this.menu;
   }
}
