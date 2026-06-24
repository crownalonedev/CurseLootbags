package org.minecurse.lootbags.struct;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.utils.DeserializedItemStack;
import org.minecurse.commons.utils.SerializedItemStack;
import org.minecurse.commons.utils.StringUtil;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.api.LootBagNotificationApi;
import org.minecurse.lootbags.menus.hype.HypeBox;
import org.minecurse.lootbags.settings.Settings;
import org.minecurse.lootmanager.struct.Reward;
import org.minecurse.lootmanager.utils.RewardUtils;
import org.minecurse.modules.utils.ItemUtil;

public class LootBag {
   private String internalName;
   private String displayName;
   private String item;
   private List<Reward> rewards;
   private List<Reward> jackpotRewards;
   private List<Reward> bonusRewards;
   private int minRewards;
   private int maxRewards;
   private boolean broadcast;
   private boolean bundle;
   private boolean glowing;
   private boolean bonusLore;
   private boolean rewardLore;
   private boolean alwaysMax;
   private boolean showcasedLootBag;
   private AnimationType animationType;
   private CrateType type;

   public LootBag(String internalName) {
      this.internalName = internalName;
      this.displayName = "&6" + internalName;
      this.item = new SerializedItemStack(new ItemBuilder(Material.CHEST).name(this.displayName)).get();
      this.rewards = Lists.newArrayList();
      this.jackpotRewards = Lists.newArrayList();
      this.bonusRewards = Lists.newArrayList();
      this.minRewards = 1;
      this.maxRewards = 1;
      this.broadcast = false;
      this.bundle = false;
      this.glowing = false;
      this.bonusLore = true;
      this.alwaysMax = true;
      this.showcasedLootBag = false;
      this.rewardLore = true;
      this.type = CrateType.LOOTBAG;
      this.animationType = AnimationType.NORMAL;
   }

   public LootBag() {
   }

   @JsonIgnore
   public String getDisplayName() {
      ItemStack item = this.getItemStack();
      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.hasDisplayName() ? meta.getDisplayName() : this.getFallbackDisplayName();
   }

   public void setDisplayName(String displayName) {
      this.displayName = displayName;
   }

   @JsonIgnore
   public void setDisplay(String displayName) {
      ItemStack item = this.getItemStack();
      ItemMeta meta = item.getItemMeta();
      meta.setDisplayName(StringUtil.color(displayName));
      item.setItemMeta(meta);
      this.item = new SerializedItemStack(item).get();
   }

   @JsonIgnore
   public Material getMaterial() {
      ItemStack item = this.getItemStack();
      return item.getType();
   }

   @JsonIgnore
   public void setMaterial(Material material) {
      ItemStack item = this.getItemStack();
      item.setType(material);
      this.item = new SerializedItemStack(item).get();
   }

   @JsonIgnore
   public String getTexture() {
      ItemStack item = this.getItemStack();
      if (item == null || item.getType() != Material.SKULL_ITEM) {
         return null;
      }

      try {
         NBTItem nbt = new NBTItem(item);
         if (!nbt.hasKey("SkullOwner")) {
            return null;
         }

         NBTCompound skullOwner = nbt.getCompound("SkullOwner");
         if (skullOwner == null || !skullOwner.hasKey("Properties")) {
            return null;
         }

         NBTCompound properties = skullOwner.getCompound("Properties");
         if (properties == null || !properties.hasKey("textures")) {
            return null;
         }

         NBTCompoundList textures = properties.getCompoundList("textures");
         for (Object entry : textures) {
            if (entry instanceof NBTCompound) {
               String value = ((NBTCompound)entry).getString("Value");
               if (value != null && !value.isEmpty()) {
                  return value;
               }
            }
         }

         return null;
      } catch (Exception ignored) {
         return null;
      }
   }

   @JsonIgnore
   public boolean hasTexture() {
      return this.getTexture() != null;
   }

   @JsonIgnore
   public void setTexture(String base64) {
      if (base64 == null || base64.trim().isEmpty() || base64.equalsIgnoreCase("null") || base64.equalsIgnoreCase("remove") || base64.equalsIgnoreCase("clear")) {
         this.removeTexture();
         return;
      }

      String currentName = this.getDisplayName();
      List<String> currentLore = this.getLore();

      ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
      ItemMeta meta = head.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(StringUtil.color(currentName));
         if (currentLore != null && !currentLore.isEmpty()) {
            meta.setLore(currentLore);
         }

         head.setItemMeta(meta);
      }

      NBTItem nbt = new NBTItem(head);
      NBTCompound skullOwner = nbt.addCompound("SkullOwner");
      skullOwner.setString("Id", UUID.randomUUID().toString());
      skullOwner.setString("Name", this.internalName == null ? "lootbag" : this.internalName);
      NBTCompound properties = skullOwner.addCompound("Properties");
      NBTCompoundList texturesList = properties.getCompoundList("textures");
      NBTListCompound textureEntry = texturesList.addCompound();
      textureEntry.setString("Value", base64.trim());
      head = nbt.getItem();

      this.item = new SerializedItemStack(head).get();
   }

   @JsonIgnore
   public void removeTexture() {
      ItemStack item = this.getItemStack();
      if (item == null) {
         return;
      }

      try {
         NBTItem nbt = new NBTItem(item);
         if (nbt.hasKey("SkullOwner")) {
            nbt.removeKey("SkullOwner");
            item = nbt.getItem();
         }
      } catch (Exception ignored) {
      }

      if (item.getType() == Material.SKULL_ITEM) {
         item.setType(Material.CHEST);
      }

      this.item = new SerializedItemStack(item).get();
   }

   @JsonIgnore
   public List<String> getLore() {
      ItemStack item = this.getItemStack();
      ItemMeta meta = item.getItemMeta();
      return meta != null && meta.hasLore() ? meta.getLore() : Lists.newArrayList();
   }

   @JsonIgnore
   public void setLore(List<String> lore) {
      ItemStack item = this.getItemStack();
      ItemMeta meta = item.getItemMeta();
      meta.setLore(lore);
      item.setItemMeta(meta);
      this.item = new SerializedItemStack(item).get();
   }

   @JsonIgnore
   public ItemStack getItemStack() {
      ItemStack itemStack = null;
      if (this.item != null && !this.item.isEmpty()) {
         try {
            itemStack = new DeserializedItemStack(this.item).get();
         } catch (RuntimeException var3) {
         }
      }

      if (itemStack == null || itemStack.getType() == Material.AIR) {
         itemStack = this.createFallbackItemStack();
         this.item = new SerializedItemStack(itemStack).get();
      }

      return itemStack;
   }

   @JsonIgnore
   public void addReward(ItemStack item) {
      this.getRewards().add(new Reward(item));
   }

   @JsonIgnore
   public void addBonus(Reward reward) {
      this.getBonusRewards().add(reward);
   }

   @JsonIgnore
   public void addJackpot(Reward reward) {
      this.getJackpotRewards().add(reward);
   }

   @JsonIgnore
   public ItemStack getItemDisplay() {
      return new ItemBuilder(this.getItemStack()).name(this.getDisplayName());
   }

   @JsonIgnore
   public ItemStack getLootBag() {
      ItemBuilder builder = new ItemBuilder(this.getItemStack()).name(this.getDisplayName());
      if (this.getType() != CrateType.MONTHLY) {
         if (this.bonusLore) {
            LootBagPlugin.getInstance()
               .getConfig()
               .getStringList("bonus-lore")
               .forEach(
                  s -> builder.lore(
                     s.replace("%amount%", this.minRewards == this.maxRewards ? "" + this.maxRewards : this.minRewards + " - " + this.maxRewards)
                  )
               );
            builder.lore("");
         }

         if (this.rewardLore) {
            if (!this.getBonusRewards().isEmpty()) {
               builder.lore("&f&lBonus Rewards &f&l(&7" + this.getBonusRewards().size() + " items&f&l)");

               for (Reward reward2 : this.getBonusRewards()) {
                  String rewardDisplayName = ItemUtil.getDisplayName(reward2.getItemStack());
                  builder.lore(" &f&l• &f&l" + reward2.getItemStack().getAmount() + "x &r" + rewardDisplayName);
               }

               if (this.isRewardLore()) {
                  builder.lore("");
               }
            }

            String rewardsTitle = this.isBundle() ? "&f&lBundle Rewards" : "&f&lRandom Rewards";
            String rewardsCount = String.valueOf(this.getMaxRewards());
            if (this.getMaxRewards() != this.getMinRewards()) {
               rewardsCount = this.getMinRewards() + " - " + this.getMaxRewards();
            }

            builder.lore(rewardsTitle + " &f&l(&7" + rewardsCount + " items&f&l)");
            if (this.getRewards().isEmpty()) {
               builder.lore("&fThis Loot Bag has no rewards :(");
            } else {
               for (Reward reward3 : this.getRewards()) {
                  String rewardDisplayName = ItemUtil.getDisplayName(reward3.getItemStack());
                  builder.lore(" &f&l• &f&l" + reward3.getItemStack().getAmount() + "x &r" + rewardDisplayName);
               }
            }
         }
      } else {
         List<Reward> filteredRewards = this.getRewards().stream().filter(reward -> !this.getBonusRewards().contains(reward)).collect(Collectors.toList());
         builder.lore("&6&lLEGENDARY");

         for (Reward reward4 : filteredRewards) {
            builder.lore(
               "&e •" + reward4.getData().displayTheString() + " &f&l" + reward4.getMax() + "x " + reward4.getItemStack().getItemMeta().getDisplayName()
            );
         }

         builder.lore("");
         builder.lore("&4&LSUPERIOR");

         for (Reward reward5 : this.getBonusRewards()) {
            builder.lore(
               "&c&l •" + reward5.getData().displayTheString() + " &f&l" + reward5.getMax() + "x " + reward5.getItemStack().getItemMeta().getDisplayName()
            );
         }

         builder.lore("");
      }

      NBTItem item = new NBTItem(builder);
      item.setString("lootBagType", this.getInternalName());
      return item.getItem();
   }

   @JsonIgnore
   public ItemStack getLootBag(Player player) {
      ItemBuilder builder = new ItemBuilder(this.getItemStack()).name(this.getDisplayName());
      if (this.getType() != CrateType.MONTHLY) {
         if (this.bonusLore) {
            LootBagPlugin.getInstance().getConfig().getStringList("bonus-lore").forEach(builder::lore);
         }

         if (this.rewardLore) {
            if (!this.getBonusRewards().isEmpty()) {
               builder.lore("&f&lBonus Rewards &f&l(&7" + this.getBonusRewards().size() + " items&f&l)");

               for (Reward reward2 : this.getBonusRewards()) {
                  String rewardDisplayName = ItemUtil.getDisplayName(reward2.getItemStack());
                  builder.lore(" &f&l• &f&l" + reward2.getItemStack().getAmount() + "x &r" + rewardDisplayName);
               }

               if (this.isRewardLore()) {
                  builder.lore("");
               }
            }

            String rewardsTitle = this.isBundle() ? "&f&lBundle Rewards" : "&f&lRandom Rewards";
            String rewardsCount = String.valueOf(this.getMaxRewards());
            if (this.getMaxRewards() != this.getMinRewards()) {
               rewardsCount = this.getMinRewards() + " - " + this.getMaxRewards();
            }

            builder.lore(rewardsTitle + " &f&l(&7" + rewardsCount + " items&f&l)");
            if (this.getRewards().isEmpty()) {
               builder.lore("&fThis Loot Bag has no rewards :(");
            } else {
               for (Reward reward3 : this.getRewards()) {
                  String rewardDisplayName = ItemUtil.getDisplayName(reward3.getItemStack());
                  builder.lore("&f&l➥ &7&l" + reward3.getItemStack().getAmount() + "x &r" + rewardDisplayName);
               }
            }
         }
      } else {
         ArrayList<Reward> filteredRewards = new ArrayList<>(
            this.getRewards().stream().filter(reward -> !this.getBonusRewards().contains(reward)).collect(Collectors.toList())
         );
         builder.lore("&fUnlocked by " + player.getDisplayName() + "&f at &b&n" + Settings.storeUrl);
         builder.lore("");
         builder.lore("&6&lLEGENDARY");

         for (Reward reward4 : filteredRewards) {
            builder.lore(
               "&e&l• " + reward4.getData().displayTheString() + " &f&l" + reward4.getMax() + "x " + reward4.getItemStack().getItemMeta().getDisplayName()
            );
         }

         ArrayList<Reward> bonus = new ArrayList<>(this.getBonusRewards());
         builder.lore("");
         builder.lore("&4&lSUPERIOR");

         for (Reward reward5 : bonus) {
            builder.lore(
               "&c&l• " + reward5.getData().displayTheString() + " &f&l" + reward5.getMax() + "x " + reward5.getItemStack().getItemMeta().getDisplayName()
            );
         }

         builder.lore("");
      }

      NBTItem item = new NBTItem(builder);
      item.setString("lootBagType", this.getInternalName());
      return item.getItem();
   }

   @JsonIgnore
   public void executeLootBag(Player player, boolean remove) {
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
      } else if (this.getAnimationType() == AnimationType.CIRCLE) {
         if (LootBagPlugin.getInstance().getAnimationManager().getAnimationHashMap().containsKey(player.getUniqueId())) {
            player.sendMessage(LootBagPlugin.prefix("&cYou are currently rolling a lootbag!"));
         } else {
            LootBagPlugin.getInstance().getAnimationManager().addPlayer(player, new CrateAnimation(player, this, this.getMaxRewards()));
            player.playSound(player.getLocation(), Sound.CHEST_OPEN, 1.0F, 0.3F);
            if (this.broadcast) {
               LootBagNotificationApi.sendLootBagBroadcast("", player);
               LootBagNotificationApi.sendLootBagBroadcast(
                  Settings.crateBroadcast.replace("%player%", player.getName()).replace("%lootbag%", this.getDisplayName()), player
               );
               LootBagNotificationApi.sendLootBagBroadcast("", player);
            }

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
      } else if (this.getType() == CrateType.LUCKY_BLOCK) {
         if (remove) {
            ItemStack hand = player.getItemInHand();
            if (hand.getAmount() > 1) {
               hand.setAmount(hand.getAmount() - 1);
               player.setItemInHand(hand);
            } else {
               player.setItemInHand(null);
            }
         }
      } else {
         ArrayList<Reward> toGive = new ArrayList<>();
         List<Reward> filteredRewards = this.getRewards().stream().filter(reward -> !this.getBonusRewards().contains(reward)).collect(Collectors.toList());
         if (this.isBundle()) {
            toGive.addAll(filteredRewards);
         } else {
            int random = this.getMinRewards() == this.getMaxRewards()
               ? this.getMaxRewards()
               : ThreadLocalRandom.current().nextInt(this.minRewards, this.maxRewards);

            for (int i = 0; i < random; i++) {
               Reward reward = RewardUtils.getRandomReward(filteredRewards);
               if (reward.getMaxPulls() != -1) {
                  while (reward.getMaxPulls() == reward.getTimesPulled()) {
                     reward = RewardUtils.getRandomReward(filteredRewards);
                  }

                  reward.setTimesPulled(reward.getTimesPulled() + 1);
               }

               toGive.add(reward);
            }
         }

         player.playSound(player.getLocation(), Sound.CHEST_OPEN, 1.0F, 0.3F);
         if (this.isBroadcast()) {
            LootBagNotificationApi.sendLootBagBroadcast("", player);
            LootBagNotificationApi.sendLootBagBroadcast(
               StringUtil.colorFormat(
                  "{0} has opened a {1}&7 and has received the following rewards:", new Object[]{player.getDisplayName(), this.getDisplayName()}
               ),
               player
            );

            for (Reward reward2 : toGive) {
               ItemStack rewardItem = reward2.getItemStack();
               LootBagNotificationApi.sendLootBagBroadcast(" &f&l• &7&l" + rewardItem.getAmount() + "x &r" + ItemUtil.getDisplayName(rewardItem), player);
               reward2.handleGive(player);
            }

            if (!this.getBonusRewards().isEmpty()) {
               LootBagNotificationApi.sendLootBagBroadcast("", player);
               LootBagNotificationApi.sendLootBagBroadcast("&f&lBonus Items &7(" + this.getBonusRewards().size() + " Items)", player);

               for (Reward reward3 : this.getBonusRewards()) {
                  reward3.handleGive(player);
                  LootBagNotificationApi.sendLootBagBroadcast(
                     " &f&l• &7&l" + reward3.getItemStack().getAmount() + "x &r" + ItemUtil.getDisplayName(reward3.getItemStack()), player
                  );
               }
            }

            LootBagNotificationApi.sendLootBagBroadcast("", player);
            if (remove) {
               ItemStack hand = player.getItemInHand();
               if (hand.getAmount() > 1) {
                  hand.setAmount(hand.getAmount() - 1);
                  player.setItemInHand(hand);
               } else {
                  player.setItemInHand(null);
               }
            }
         } else {
            player.sendMessage(LootBagPlugin.prefix("You've received your rewards for the {0}&r &7loot bag.", this.getDisplayName()));
            toGive.forEach(reward -> reward.handleGive(player));
            if (!this.getBonusRewards().isEmpty()) {
               this.getBonusRewards().forEach(reward -> reward.handleGive(player));
            }

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
      }
   }

   public String getInternalName() {
      return this.internalName;
   }

   public void setInternalName(String internalName) {
      this.internalName = internalName;
   }

   public String getItem() {
      return this.item;
   }

   public void setItem(String item) {
      this.item = item;
   }

   public List<Reward> getRewards() {
      if (this.rewards == null) {
         this.rewards = Lists.newArrayList();
      }

      return this.rewards;
   }

   public void setRewards(List<Reward> rewards) {
      this.rewards = rewards;
   }

   public List<Reward> getJackpotRewards() {
      if (this.jackpotRewards == null) {
         this.jackpotRewards = Lists.newArrayList();
      }

      return this.jackpotRewards;
   }

   public void setJackpotRewards(List<Reward> jackpotRewards) {
      this.jackpotRewards = jackpotRewards;
   }

   public List<Reward> getBonusRewards() {
      if (this.bonusRewards == null) {
         this.bonusRewards = Lists.newArrayList();
      }

      return this.bonusRewards;
   }

   public void setBonusRewards(List<Reward> bonusRewards) {
      this.bonusRewards = bonusRewards;
   }

   public int getMinRewards() {
      return this.minRewards;
   }

   public void setMinRewards(int minRewards) {
      this.minRewards = minRewards;
   }

   public int getMaxRewards() {
      return this.maxRewards;
   }

   public void setMaxRewards(int maxRewards) {
      this.maxRewards = maxRewards;
   }

   public boolean isBroadcast() {
      return this.broadcast;
   }

   public void setBroadcast(boolean broadcast) {
      this.broadcast = broadcast;
   }

   public boolean isBundle() {
      return this.bundle;
   }

   public void setBundle(boolean bundle) {
      this.bundle = bundle;
   }

   public boolean isGlowing() {
      return this.glowing;
   }

   public void setGlowing(boolean glowing) {
      this.glowing = glowing;
   }

   public boolean isBonusLore() {
      return this.bonusLore;
   }

   public void setBonusLore(boolean bonusLore) {
      this.bonusLore = bonusLore;
   }

   public boolean isRewardLore() {
      return this.rewardLore;
   }

   public void setRewardLore(boolean rewardLore) {
      this.rewardLore = rewardLore;
   }

   public boolean isAlwaysMax() {
      return this.alwaysMax;
   }

   public void setAlwaysMax(boolean alwaysMax) {
      this.alwaysMax = alwaysMax;
   }

   public boolean isShowcasedLootBag() {
      return this.showcasedLootBag;
   }

   public void setShowcasedLootBag(boolean showcasedLootBag) {
      this.showcasedLootBag = showcasedLootBag;
   }

   public AnimationType getAnimationType() {
      if (this.animationType == null) {
         this.animationType = AnimationType.NORMAL;
      }

      return this.animationType;
   }

   public void setAnimationType(AnimationType animationType) {
      this.animationType = animationType;
   }

   public CrateType getType() {
      if (this.type == null) {
         this.type = CrateType.LOOTBAG;
      }

      return this.type;
   }

   @JsonIgnore
   private String getFallbackDisplayName() {
      if (this.displayName != null && !this.displayName.isEmpty()) {
         return StringUtil.color(this.displayName);
      } else {
         return this.internalName != null && !this.internalName.isEmpty() ? StringUtil.color("&6" + this.internalName) : StringUtil.color("&6Loot Bag");
      }
   }

   @JsonIgnore
   private ItemStack createFallbackItemStack() {
      return new ItemBuilder(Material.CHEST).name(this.getFallbackDisplayName());
   }

   public void setType(CrateType type) {
      this.type = type;
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      }

      if (!(o instanceof LootBag)) {
         return false;
      }

      LootBag other = (LootBag)o;
      if (!other.canEqual(this)) {
         return false;
      }

      String this$internalName = this.getInternalName();
      String other$internalName = other.getInternalName();
      if (!Objects.equals(this$internalName, other$internalName)) {
         return false;
      }

      String this$displayName = this.getDisplayName();
      String other$displayName = other.getDisplayName();
      if (!Objects.equals(this$displayName, other$displayName)) {
         return false;
      }

      String this$item = this.getItem();
      String other$item = other.getItem();
      if (!Objects.equals(this$item, other$item)) {
         return false;
      }

      List<Reward> this$rewards = this.getRewards();
      List<Reward> other$rewards = other.getRewards();
      if (!Objects.equals(this$rewards, other$rewards)) {
         return false;
      }

      List<Reward> this$jackpotRewards = this.getJackpotRewards();
      List<Reward> other$jackpotRewards = other.getJackpotRewards();
      if (!Objects.equals(this$jackpotRewards, other$jackpotRewards)) {
         return false;
      }

      List<Reward> this$bonusRewards = this.getBonusRewards();
      List<Reward> other$bonusRewards = other.getBonusRewards();
      if (!Objects.equals(this$bonusRewards, other$bonusRewards)) {
         return false;
      }

      if (this.getMinRewards() != other.getMinRewards()) {
         return false;
      }

      if (this.getMaxRewards() != other.getMaxRewards()) {
         return false;
      }

      if (this.isBroadcast() != other.isBroadcast()) {
         return false;
      }

      if (this.isBundle() != other.isBundle()) {
         return false;
      }

      if (this.isGlowing() != other.isGlowing()) {
         return false;
      }

      if (this.isBonusLore() != other.isBonusLore()) {
         return false;
      }

      if (this.isRewardLore() != other.isRewardLore()) {
         return false;
      }

      if (this.isAlwaysMax() != other.isAlwaysMax()) {
         return false;
      }

      if (this.isShowcasedLootBag() != other.isShowcasedLootBag()) {
         return false;
      }

      AnimationType this$animationType = this.getAnimationType();
      AnimationType other$animationType = other.getAnimationType();
      if (!Objects.equals(this$animationType, other$animationType)) {
         return false;
      }

      CrateType this$type = this.getType();
      CrateType other$type = other.getType();
      return Objects.equals(this$type, other$type);
   }

   protected boolean canEqual(Object other) {
      return other instanceof LootBag;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = 1;
      String $internalName = this.getInternalName();
      result = result * 59 + ($internalName == null ? 43 : $internalName.hashCode());
      String $displayName = this.getDisplayName();
      result = result * 59 + ($displayName == null ? 43 : $displayName.hashCode());
      String $item = this.getItem();
      result = result * 59 + ($item == null ? 43 : $item.hashCode());
      List<Reward> $rewards = this.getRewards();
      result = result * 59 + ($rewards == null ? 43 : $rewards.hashCode());
      List<Reward> $jackpotRewards = this.getJackpotRewards();
      result = result * 59 + ($jackpotRewards == null ? 43 : $jackpotRewards.hashCode());
      List<Reward> $bonusRewards = this.getBonusRewards();
      result = result * 59 + ($bonusRewards == null ? 43 : $bonusRewards.hashCode());
      result = result * 59 + this.getMinRewards();
      result = result * 59 + this.getMaxRewards();
      result = result * 59 + (this.isBroadcast() ? 79 : 97);
      result = result * 59 + (this.isBundle() ? 79 : 97);
      result = result * 59 + (this.isGlowing() ? 79 : 97);
      result = result * 59 + (this.isBonusLore() ? 79 : 97);
      result = result * 59 + (this.isRewardLore() ? 79 : 97);
      result = result * 59 + (this.isAlwaysMax() ? 79 : 97);
      result = result * 59 + (this.isShowcasedLootBag() ? 79 : 97);
      AnimationType $animationType = this.getAnimationType();
      result = result * 59 + ($animationType == null ? 43 : $animationType.hashCode());
      CrateType $type = this.getType();
      return result * 59 + ($type == null ? 43 : $type.hashCode());
   }

   @Override
   public String toString() {
      return "LootBag(internalName="
         + this.getInternalName()
         + ", displayName="
         + this.getDisplayName()
         + ", item="
         + this.getItem()
         + ", rewards="
         + this.getRewards()
         + ", jackpotRewards="
         + this.getJackpotRewards()
         + ", bonusRewards="
         + this.getBonusRewards()
         + ", minRewards="
         + this.getMinRewards()
         + ", maxRewards="
         + this.getMaxRewards()
         + ", broadcast="
         + this.isBroadcast()
         + ", bundle="
         + this.isBundle()
         + ", glowing="
         + this.isGlowing()
         + ", bonusLore="
         + this.isBonusLore()
         + ", rewardLore="
         + this.isRewardLore()
         + ", alwaysMax="
         + this.isAlwaysMax()
         + ", showcasedLootBag="
         + this.isShowcasedLootBag()
         + ", animationType="
         + this.getAnimationType()
         + ", type="
         + this.getType()
         + ")";
   }
}
