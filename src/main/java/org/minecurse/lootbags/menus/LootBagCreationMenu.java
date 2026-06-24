package org.minecurse.lootbags.menus;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

      // ── Section helpers ──────────────────────────────────────────────
      // Display section (top row of content)
      ItemBuilder display = new ItemBuilder(Material.NAME_TAG)
         .name("&3&lDisplay Name")
         .lore("&7Current: &f" + this.lootBag.getDisplayName())
         .lore("")
         .lore("&a&l» &7Click to rename via chat.");
      ItemBuilder lore = new ItemBuilder(Material.BOOK_AND_QUILL)
         .name("&6&lLore Lines")
         .lore(
            this.lootBag.getLore() != null && !this.lootBag.getLore().isEmpty()
               ? this.lootBag.getLore()
               : Lists.newArrayList("&fNo lore set.")
         )
         .lore("")
         .lore("&a&l» &7Click to add a lore line via chat.")
         .lore("&7Type 'clear' to wipe, 'cancel' to abort.");
      ItemBuilder internal = new ItemBuilder(Material.PAPER)
         .name("&3&lInternal Name")
         .lore("&7Current: &f" + this.lootBag.getInternalName())
         .lore("")
         .lore("&a&l» &7Click to rename via chat.");
      ItemBuilder material = new ItemBuilder(this.lootBag.getMaterial())
         .name("&b&lMaterial")
         .lore("&7Current: &f" + this.lootBag.getMaterial().name())
         .lore("")
         .lore("&a&l» &7Hold an item, then click & type.");
      boolean hasTexture = this.lootBag.hasTexture();
      ItemBuilder texture = new ItemBuilder(Material.SKULL_ITEM)
         .durability(hasTexture ? 3 : 0)
         .name("&5&lBase64 Texture")
         .lore("&7Status: " + (hasTexture ? "&a&lSet" : "&c&lNone"))
         .lore("")
         .lore("&a&l» &7Click to paste a base64 value in chat.")
         .lore("&7Type 'remove' to clear, 'cancel' to abort.");
      ItemBuilder metaData = new ItemBuilder(Material.ENDER_CHEST)
         .name("&d&lCopy Item Meta")
         .lore("&7Copy display name, lore & enchants")
         .lore("&7from the item you're holding.")
         .lore("")
         .lore("&a&l» &7Hold an item, then click & type.");

      // Rewards section
      ItemBuilder rewards = new ItemBuilder(Material.STORAGE_MINECART)
         .name("&e&lEdit Rewards")
         .lore("&7Rewards: &f" + this.lootBag.getRewards().size())
         .lore("")
         .lore("&a&l» &7Click to open the rewards editor.");
      ItemBuilder min = new ItemBuilder(Material.ARROW)
         .amount(this.lootBag.getMinRewards())
         .name("&c&lMin Rewards")
         .lore("&7Range: &f" + this.lootBag.getMinRewards() + " &8\u2192 &f" + this.lootBag.getMaxRewards())
         .lore("")
         .lore("&a&l» &7Click to set via chat.");
      ItemBuilder max = new ItemBuilder(Material.ARROW)
         .amount(this.lootBag.getMaxRewards())
         .name("&a&lMax Rewards")
         .lore("&7Current: &f" + this.lootBag.getMaxRewards())
         .lore("")
         .lore("&a&l» &7Click to set via chat.");
      ItemBuilder alwaysMax = toggleItem(Material.GOLDEN_APPLE, "&d&lAlways Max", this.lootBag.isAlwaysMax(),
         "&7Force max rewards every time.");
      ItemBuilder bundle = toggleItem(Material.CHEST, "&a&lBundle Mode", this.lootBag.isBundle(),
         "&7Give ALL rewards instead of random.");

      // Toggles section
      ItemBuilder glow = toggleItem(Material.GLOWSTONE_DUST, "&f&lItem Glow", this.lootBag.isGlowing(),
         "&7Enchant-glow on the lootbag item.");
      ItemBuilder broadcast = toggleItem(this.lootBag.isBroadcast() ? Material.REDSTONE_TORCH_ON : Material.TORCH,
         "&d&lBroadcast Loot", this.lootBag.isBroadcast(), "&7Announce opens to the server.");
      ItemBuilder bonusLore = toggleItem(this.lootBag.isBonusLore() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
         "&e&lBonus Lore", this.lootBag.isBonusLore(), "&7Show bonus-reward lore lines.");
      ItemBuilder hideRewardLore = toggleItem(this.lootBag.isRewardLore() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK,
         "&2&lReward Lore", this.lootBag.isRewardLore(), "&7Show reward-list lore lines.");
      ItemBuilder showcase = toggleItem(Material.BOOK, "&a&lShowcase", this.lootBag.isShowcasedLootBag(),
         "&7Feature this lootbag in the display menu.");

      // Type section
      ItemBuilder lootBagType = new ItemBuilder(Material.ENDER_PORTAL_FRAME)
         .name("&2&lLootbag Type")
         .lore("&7Current: &f" + this.lootBag.getType().name())
         .lore("")
         .lore("&a&l» &7Click to cycle through types.");
      ItemBuilder animationType = new ItemBuilder(Material.GLOWSTONE)
         .name("&6&lAnimation Type")
         .lore("&7Current: &f" + this.lootBag.getAnimationType().name())
         .lore("")
         .lore("&a&l» &7Click to cycle through animations.");

      // ── Layout (6 rows = 54 slots) ───────────────────────────────────
      this.menu.fillSides(Button.PLACEHOLDER);

      // Row 1 (slots 10-16): Identity & appearance
      this.menu.setButton(10, new Button(display, (p, c) -> startChatEdit(p, EditType.DISPLAY,
         "&7Current display: " + this.lootBag.getDisplayName(),
         "&7Type the new display name (use & codes for colors).")));
      this.menu.setButton(11, new Button(internal, (p, c) -> startChatEdit(p, EditType.INTERNAL,
         "&7Current internal: " + this.lootBag.getInternalName(),
         "&7Type the new internal name (A-Z, 0-9, _ only).")));
      this.menu.setButton(12, new Button(lore, (p, c) -> startChatEdit(p, EditType.LORE,
         "&7Type a new lore line to ADD it.",
         "&7Type 'clear' to wipe all lore, 'cancel' to abort.")));
      this.menu.setButton(13, new Button(material, (p, c) -> startChatEdit(p, EditType.MATERIAL,
         "&7Hold the item you want as the material, then type anything.")));
      this.menu.setButton(14, new Button(texture, (p, c) -> {
         this.editType.put(p.getUniqueId(), EditType.TEXTURE);
         p.sendMessage(LootBagPlugin.prefix("&6Base64 Texture Editor"));
         p.sendMessage(LootBagPlugin.prefix("&7Status: " + (this.lootBag.hasTexture() ? "&aSet" : "&cNone")));
         p.sendMessage(LootBagPlugin.prefix("&7Paste the base64 value in chat."));
         p.sendMessage(LootBagPlugin.prefix("&7Type 'remove' to clear, 'cancel' to abort."));
         p.closeInventory();
      }));
      this.menu.setButton(15, new Button(metaData, (p, c) -> startChatEdit(p, EditType.META,
         "&7Hold an item with the name/lore/enchants you want to copy, then type.")));
      this.menu.setButton(16, new Button(item, (p, c) -> p.getInventory().addItem(new ItemStack[]{item})));

      // Row 2 (slots 19-25): Rewards
      this.menu.setButton(19, new Button(rewards, (p, c) -> new RewardMenu(this.lootBag).show(p)));
      this.menu.setButton(20, new Button(min, (p, c) -> {
         if (this.lootBag.isBundle()) {
            p.sendMessage(LootBagPlugin.prefix("&cDisable Bundle mode first."));
            return;
         }
         if (this.lootBag.isAlwaysMax()) {
            p.sendMessage(LootBagPlugin.prefix("&cDisable Always Max first."));
            return;
         }
         startChatEdit(p, EditType.MIN, "&7Type a number between 1 and " + this.lootBag.getMaxRewards() + ".");
      }));
      this.menu.setButton(21, new Button(max, (p, c) -> {
         if (this.lootBag.isBundle()) {
            p.sendMessage(LootBagPlugin.prefix("&cDisable Bundle mode first."));
            return;
         }
         if (this.lootBag.isAlwaysMax()) {
            p.sendMessage(LootBagPlugin.prefix("&cDisable Always Max first."));
            return;
         }
         startChatEdit(p, EditType.MAX, "&7Current max: " + this.lootBag.getMaxRewards() + ". Type a new number.");
      }));
      this.menu.setButton(22, new Button(alwaysMax, (p, c) -> {
         if (this.lootBag.isBundle()) {
            p.sendMessage(LootBagPlugin.prefix("&cDisable Bundle mode first."));
            return;
         }
         this.lootBag.setAlwaysMax(!this.lootBag.isAlwaysMax());
         p.sendMessage(LootBagPlugin.prefix("Always Max: " + RetroUtils.formatBoolean(this.lootBag.isAlwaysMax())));
         this.refreshMenu(p);
      }));
      this.menu.setButton(23, new Button(bundle, (p, c) -> {
         this.lootBag.setBundle(!this.lootBag.isBundle());
         p.sendMessage(LootBagPlugin.prefix("Bundle Mode: " + RetroUtils.formatBoolean(this.lootBag.isBundle())));
         this.refreshMenu(p);
      }));
      this.menu.setButton(24, new Button(lootBagType, (p, c) -> {
         CrateType[] types = CrateType.values();
         CrateType next = types[(this.lootBag.getType().ordinal() + 1) % types.length];
         this.lootBag.setType(next);
         p.sendMessage(LootBagPlugin.prefix("Lootbag Type: &f" + next.name()));
         this.refreshMenu(p);
      }));
      this.menu.setButton(25, new Button(animationType, (p, c) -> {
         AnimationType[] types = AnimationType.values();
         AnimationType next = types[(this.lootBag.getAnimationType().ordinal() + 1) % types.length];
         this.lootBag.setAnimationType(next);
         p.sendMessage(LootBagPlugin.prefix("Animation Type: &f" + next.name()));
         this.refreshMenu(p);
      }));

      // Row 3 (slots 28-34): Toggles
      this.menu.setButton(28, new Button(glow, (p, c) -> {
         this.lootBag.setGlowing(!this.lootBag.isGlowing());
         p.sendMessage(LootBagPlugin.prefix("Item Glow: " + RetroUtils.formatBoolean(this.lootBag.isGlowing())));
         this.refreshMenu(p);
      }));
      this.menu.setButton(29, new Button(broadcast, (p, c) -> {
         this.lootBag.setBroadcast(!this.lootBag.isBroadcast());
         p.sendMessage(LootBagPlugin.prefix("Broadcast: " + RetroUtils.formatBoolean(this.lootBag.isBroadcast())));
         this.refreshMenu(p);
      }));
      this.menu.setButton(30, new Button(bonusLore, (p, c) -> {
         this.lootBag.setBonusLore(!this.lootBag.isBonusLore());
         p.sendMessage(LootBagPlugin.prefix("Bonus Lore: " + RetroUtils.formatBoolean(this.lootBag.isBonusLore())));
         this.refreshMenu(p);
      }));
      this.menu.setButton(31, new Button(hideRewardLore, (p, c) -> {
         this.lootBag.setRewardLore(!this.lootBag.isRewardLore());
         p.sendMessage(LootBagPlugin.prefix("Reward Lore: " + RetroUtils.formatBoolean(this.lootBag.isRewardLore())));
         this.refreshMenu(p);
      }));
      this.menu.setButton(32, new Button(showcase, (p, c) -> {
         if (LootBagManager.getInstance().isAlreadyToggled() && LootBagManager.getInstance().findShowcasedLootBag() != this.lootBag) {
            p.sendMessage(LootBagPlugin.prefix("&cDisable showcase on \"" + LootBagManager.getInstance().findShowcasedLootBag().getInternalName() + "\" first."));
            return;
         }
         this.lootBag.setShowcasedLootBag(!this.lootBag.isShowcasedLootBag());
         p.sendMessage(LootBagPlugin.prefix("Showcase: " + RetroUtils.formatBoolean(this.lootBag.isShowcasedLootBag())));
         this.refreshMenu(p);
      }));

      // Bottom: back button
      this.menu.setButton(49, new Button(
         new ItemBuilder(Material.ARROW).name("&b&l« Go Back").lore("&7Return to the lootbag list."),
         (p, c) -> new LootbagListMenu(this.lootBag.getType()).show(p)
      ));

      this.menu.buildInventory();
      this.menu.show(player);
   }

   private void startChatEdit(Player player, EditType type, String... messages) {
      this.editType.put(player.getUniqueId(), type);
      for (String m : messages) {
         player.sendMessage(LootBagPlugin.prefix(m));
      }
      player.closeInventory();
   }

   private ItemBuilder toggleItem(Material material, String name, boolean enabled, String description) {
      return new ItemBuilder(material)
         .name(name)
         .lore("&7Status: " + (enabled ? "&a&lENABLED" : "&c&lDISABLED"))
         .lore("")
         .lore(description)
         .lore("")
         .lore("&a&l» &7Click to toggle.");
   }

   private void refreshMenu(Player player) {
      LootBagManager.getInstance().saveToDisk();
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
                     player.sendMessage(LootBagPlugin.prefix("Lore editing canceled."));
                     this.menu.show(player);
                     this.sound(player);
                     return;
                  }

                  if (message.equalsIgnoreCase("clear") || message.equalsIgnoreCase("null")) {
                     this.lootBag.setLore(Lists.newArrayList());
                     player.sendMessage(LootBagPlugin.prefix("All lore lines cleared."));
                     this.onFinish(player);
                     return;
                  }

                  List<String> currentLore = this.lootBag.getLore();
                  if (currentLore == null) {
                     currentLore = Lists.newArrayList();
                  }

                  currentLore.add(event.getMessage());
                  this.lootBag.setLore(currentLore);
                  player.sendMessage(LootBagPlugin.prefix("Added lore line: &f" + event.getMessage()));
                  player.sendMessage(LootBagPlugin.prefix("&7Type another line to add more, 'clear' to wipe, or 'cancel' to finish."));
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
