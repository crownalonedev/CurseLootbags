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
      this.menu = new ChestMenu("Editing: " + lootBag.getInternalName(), 6);
   }

   public void show(Player player) {
      this.menu.fillSides(Button.PLACEHOLDER);

      ItemStack preview = this.lootBag.getLootBag();

      this.menu.setButton(4, new Button(new ItemBuilder(preview).name("&f&l" + this.lootBag.getDisplayName()).lore("&7This is how your lootbag looks."), (p, c) -> p.getInventory().addItem(new ItemStack[]{preview})));

      this.menu.setButton(19, new Button(new ItemBuilder(Material.NAME_TAG).name("&3&lDisplay Name").lore("&7" + this.lootBag.getDisplayName()).lore("").lore("&a&l» &7Click to edit in chat."), (p, c) -> this.startChatEdit(p, EditType.DISPLAY, "&7Current: " + this.lootBag.getDisplayName(), "&7Type the new display name (& color codes allowed).")));
      this.menu.setButton(21, new Button(new ItemBuilder(Material.BOOK_AND_QUILL).name("&6&lLore").lore("&7Lines: &f" + (this.lootBag.getLore() != null ? this.lootBag.getLore().size() : 0)).lore("").lore("&a&l» &7Click to add a line in chat.").lore("&7Type 'clear' to wipe, 'cancel' to stop."), (p, c) -> this.startChatEdit(p, EditType.LORE, "&7Type a lore line to add.", "&7Type 'clear' to wipe, 'cancel' to finish.")));
      this.menu.setButton(23, new Button(new ItemBuilder(this.lootBag.getMaterial()).name("&b&lMaterial").lore("&7" + this.lootBag.getMaterial().name()).lore("").lore("&a&l» &7Hold an item, click & type."), (p, c) -> this.startChatEdit(p, EditType.MATERIAL, "&7Hold the item to use as material, then type anything.")));
      this.menu.setButton(25, new Button(new ItemBuilder(Material.SKULL_ITEM).durability(this.lootBag.hasTexture() ? 3 : 0).name("&5&lTexture").lore("&7" + (this.lootBag.hasTexture() ? "&aSet" : "&cNone")).lore("").lore("&a&l» &7Click to set base64 in chat.").lore("&7Type 'remove' to clear, 'cancel' to stop."), (p, c) -> { this.editType.put(p.getUniqueId(), EditType.TEXTURE); p.sendMessage(LootBagPlugin.prefix("&6Texture Editor &7- Status: " + (this.lootBag.hasTexture() ? "&aSet" : "&cNone"))); p.sendMessage(LootBagPlugin.prefix("&7Paste base64 in chat. 'remove' to clear, 'cancel' to abort.")); p.closeInventory(); }));

      this.menu.setButton(29, new Button(new ItemBuilder(Material.STORAGE_MINECART).name("&e&lRewards").lore("&7Count: &f" + this.lootBag.getRewards().size()).lore("").lore("&a&l» &7Click to edit rewards."), (p, c) -> new RewardMenu(this.lootBag).show(p)));
      this.menu.setButton(31, new Button(new ItemBuilder(Material.ARROW).amount(this.lootBag.getMaxRewards()).name("&a&lMax Rewards").lore("&7Min: &f" + this.lootBag.getMinRewards() + " &8| &7Max: &f" + this.lootBag.getMaxRewards()).lore("").lore("&a&l» &7Click to set max via chat."), (p, c) -> { if (this.lootBag.isBundle()) { p.sendMessage(LootBagPlugin.prefix("&cDisable Bundle first.")); return; } this.startChatEdit(p, EditType.MAX, "&7Current max: " + this.lootBag.getMaxRewards() + ". Type a new number."); }));
      this.menu.setButton(33, new Button(new ItemBuilder(Material.ARROW).amount(this.lootBag.getMinRewards()).name("&c&lMin Rewards").lore("&7Min: &f" + this.lootBag.getMinRewards()).lore("").lore("&a&l» &7Click to set min via chat."), (p, c) -> { if (this.lootBag.isBundle()) { p.sendMessage(LootBagPlugin.prefix("&cDisable Bundle first.")); return; } if (this.lootBag.isAlwaysMax()) { p.sendMessage(LootBagPlugin.prefix("&cDisable Always Max first.")); return; } this.startChatEdit(p, EditType.MIN, "&7Type a number between 1 and " + this.lootBag.getMaxRewards() + "."); }));

      this.menu.setButton(37, new Button(this.toggleItem(Material.GOLDEN_APPLE, "&d&lAlways Max", this.lootBag.isAlwaysMax()), (p, c) -> { if (this.lootBag.isBundle()) { p.sendMessage(LootBagPlugin.prefix("&cDisable Bundle first.")); return; } this.lootBag.setAlwaysMax(!this.lootBag.isAlwaysMax()); p.sendMessage(LootBagPlugin.prefix("Always Max: " + RetroUtils.formatBoolean(this.lootBag.isAlwaysMax()))); this.refreshMenu(p); }));
      this.menu.setButton(38, new Button(this.toggleItem(Material.CHEST, "&a&lBundle", this.lootBag.isBundle()), (p, c) -> { this.lootBag.setBundle(!this.lootBag.isBundle()); p.sendMessage(LootBagPlugin.prefix("Bundle: " + RetroUtils.formatBoolean(this.lootBag.isBundle()))); this.refreshMenu(p); }));
      this.menu.setButton(39, new Button(this.toggleItem(Material.GLOWSTONE_DUST, "&f&lGlow", this.lootBag.isGlowing()), (p, c) -> { this.lootBag.setGlowing(!this.lootBag.isGlowing()); p.sendMessage(LootBagPlugin.prefix("Glow: " + RetroUtils.formatBoolean(this.lootBag.isGlowing()))); this.refreshMenu(p); }));
      this.menu.setButton(40, new Button(this.toggleItem(this.lootBag.isBroadcast() ? Material.REDSTONE_TORCH_ON : Material.TORCH, "&d&lBroadcast", this.lootBag.isBroadcast()), (p, c) -> { this.lootBag.setBroadcast(!this.lootBag.isBroadcast()); p.sendMessage(LootBagPlugin.prefix("Broadcast: " + RetroUtils.formatBoolean(this.lootBag.isBroadcast()))); this.refreshMenu(p); }));
      this.menu.setButton(41, new Button(this.toggleItem(this.lootBag.isBonusLore() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, "&e&lBonus Lore", this.lootBag.isBonusLore()), (p, c) -> { this.lootBag.setBonusLore(!this.lootBag.isBonusLore()); p.sendMessage(LootBagPlugin.prefix("Bonus Lore: " + RetroUtils.formatBoolean(this.lootBag.isBonusLore()))); this.refreshMenu(p); }));
      this.menu.setButton(42, new Button(this.toggleItem(this.lootBag.isRewardLore() ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK, "&2&lReward Lore", this.lootBag.isRewardLore()), (p, c) -> { this.lootBag.setRewardLore(!this.lootBag.isRewardLore()); p.sendMessage(LootBagPlugin.prefix("Reward Lore: " + RetroUtils.formatBoolean(this.lootBag.isRewardLore()))); this.refreshMenu(p); }));
      this.menu.setButton(43, new Button(this.toggleItem(Material.BOOK, "&a&lShowcase", this.lootBag.isShowcasedLootBag()), (p, c) -> { if (LootBagManager.getInstance().isAlreadyToggled() && LootBagManager.getInstance().findShowcasedLootBag() != this.lootBag) { p.sendMessage(LootBagPlugin.prefix("&cDisable showcase on \"" + LootBagManager.getInstance().findShowcasedLootBag().getInternalName() + "\" first.")); return; } this.lootBag.setShowcasedLootBag(!this.lootBag.isShowcasedLootBag()); p.sendMessage(LootBagPlugin.prefix("Showcase: " + RetroUtils.formatBoolean(this.lootBag.isShowcasedLootBag()))); this.refreshMenu(p); }));

      this.menu.setButton(48, new Button(new ItemBuilder(Material.PAPER).name("&3&lInternal Name").lore("&7" + this.lootBag.getInternalName()).lore("").lore("&a&l» &7Click to rename in chat."), (p, c) -> this.startChatEdit(p, EditType.INTERNAL, "&7Current: " + this.lootBag.getInternalName(), "&7Type the new internal name (A-Z, 0-9, _ only).")));
      this.menu.setButton(49, new Button(new ItemBuilder(Material.ARROW).name("&b&l« Go Back").lore("&7Return to lootbag list."), (p, c) -> new LootbagListMenu(this.lootBag.getType()).show(p)));
      this.menu.setButton(50, new Button(new ItemBuilder(Material.ENDER_PORTAL_FRAME).name("&2&lType: &f" + this.lootBag.getType().name()).lore("").lore("&a&l» &7Click to cycle type."), (p, c) -> { CrateType[] types = CrateType.values(); CrateType next = types[(this.lootBag.getType().ordinal() + 1) % types.length]; this.lootBag.setType(next); p.sendMessage(LootBagPlugin.prefix("Type: &f" + next.name())); this.refreshMenu(p); }));

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

   private ItemBuilder toggleItem(Material material, String name, boolean enabled) {
      return new ItemBuilder(material)
         .name(name)
         .lore("&7" + (enabled ? "&a&lENABLED" : "&c&lDISABLED"))
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
