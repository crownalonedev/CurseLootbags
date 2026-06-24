package org.minecurse.lootbags.battle;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.minecurse.inventorypets.utils.SkullUtils;
import org.minecurse.inventorypets.utils.SkullUtils.Type;
import org.minecurse.lootbags.LootBagPlugin;

public class BattleManager {
   private final List<BattleInfo> activeBattles = new ArrayList<>();
   private final ItemStack botHead = SkullUtils.fromUrl(
      Type.ITEM, "http://textures.minecraft.net/texture/a2a90f8cd5a8622a9a4044542cd12be9dc87a59e5fab562096b22fa75a0a8a01"
   );

   public List<BattleInfo> getActiveBattles() {
      return this.activeBattles;
   }

   public ItemStack getBotHead() {
      return this.botHead;
   }

   public static BattleManager getInstance() {
      return LootBagPlugin.getInstance().getBattleManager();
   }
}
