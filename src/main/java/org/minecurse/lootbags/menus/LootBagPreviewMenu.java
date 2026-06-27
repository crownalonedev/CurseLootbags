package org.minecurse.lootbags.menus;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.minecurse.commons.item.ItemBuilder;
import org.minecurse.commons.menu.button.Button;
import org.minecurse.commons.menu.type.paginated.PaginatedMenu;
import org.minecurse.lootbags.struct.LootBag;
import org.minecurse.lootmanager.struct.Reward;

public class LootBagPreviewMenu {
   private static final DecimalFormat PERCENT_FORMAT = new DecimalFormat("0.##");
   private final LootBag lootBag;
   private final PaginatedMenu inventory;

   public LootBagPreviewMenu(LootBag lootBag) {
      this.lootBag = lootBag;
      this.inventory = new PaginatedMenu(lootBag.getDisplayName() + " &7Rewards (" + this.getPreviewRewards().size() + "&7)", 6, 28);
   }

   public void show(Player player) {
      this.inventory.fillSides(Button.PLACEHOLDER);
      List<Reward> randomRewards = this.lootBag
         .getRewards()
         .stream()
         .filter(reward -> !this.lootBag.getBonusRewards().contains(reward))
         .collect(Collectors.toList());
      double totalChance = randomRewards.stream().mapToDouble(Reward::getChance).sum();
      ArrayList<Reward> rewards = this.getPreviewRewards();

      for (Reward reward : rewards.stream().sorted(Comparator.comparing(Reward::getChance)).collect(Collectors.toList())) {
         ItemBuilder builder = new ItemBuilder(reward.getItemStack().clone());
         builder.lore("");
         builder.lore(this.getChanceLore(reward, totalChance));
         this.inventory.addButton(new Button(builder, (player1, clickInformation) -> {
            if (player.hasPermission("curse.admin")) {
               player.getInventory().addItem(new ItemStack[]{reward.getItemStack()});
            }
         }));
      }

      this.inventory.buildInventory();
      this.inventory.show(player);
   }

   public PaginatedMenu getInventory() {
      return this.inventory;
   }

   private String getChanceLore(Reward reward, double totalChance) {
      if (this.lootBag.getBonusRewards().contains(reward)) {
         return "&7Chance: &a100% &7(Bonus)";
      } else if (this.lootBag.isBundle()) {
         return "&7Chance: &a100% &7(Bundle)";
      } else if (totalChance <= 0.0) {
         return "&7Roll Chance: &a0%";
      }

      double chance = reward.getChance() / totalChance * 100.0;
      return "&7Roll Chance: &a" + PERCENT_FORMAT.format(chance) + "%";
   }

   private ArrayList<Reward> getPreviewRewards() {
      ArrayList<Reward> rewards = new ArrayList<>(this.lootBag.getRewards());
      rewards.addAll(this.lootBag.getBonusRewards().stream().filter(reward -> !rewards.contains(reward)).collect(Collectors.toList()));
      return rewards;
   }
}
