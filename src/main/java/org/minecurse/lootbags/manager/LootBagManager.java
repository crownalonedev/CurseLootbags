package org.minecurse.lootbags.manager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.struct.LootBag;

public class LootBagManager {
   private final JavaPlugin plugin;
   private final String path;
   private final String file;
   private List<LootBag> LootBags = Lists.newArrayList();
   private List<LootBag> originalDump;

   public LootBagManager(JavaPlugin plugin, String path, String file) {
      this.plugin = plugin;
      this.path = path;
      this.file = file;
      this.initialize();
   }

   public static LootBagManager getInstance() {
      return LootBagPlugin.getInstance().getManager();
   }

   public void addLootBag(LootBag LootBag2) {
      this.LootBags.add(LootBag2);
      if (this.originalDump == null) {
         this.originalDump = Lists.newArrayList();
      }

      this.originalDump.add(LootBag2);
   }

   public void initialize() {
      File file = new File(this.path);
      if (!file.exists()) {
         file.mkdirs();
      }

      if (!(file = new File(this.path + "/" + this.file)).exists()) {
         try {
            file.createNewFile();
         } catch (IOException var3) {
            throw new RuntimeException(var3);
         }
      }
   }

   public LootBag getByName(String name) {
      for (LootBag LootBag2 : this.LootBags) {
         if (LootBag2.getInternalName().equalsIgnoreCase(name)) {
            return LootBag2;
         }
      }

      return null;
   }

   public LootBag findLootBag(String name) {
      for (LootBag LootBag2 : this.LootBags) {
         if (LootBag2.getInternalName().equalsIgnoreCase(name)) {
            return LootBag2;
         }
      }

      return null;
   }

   public boolean isLootBag(String name) {
      for (LootBag LootBag2 : this.LootBags) {
         if (LootBag2.getInternalName().equalsIgnoreCase(name)) {
            return true;
         }
      }

      return false;
   }

   public void removeLootBag(LootBag bag) {
      if (bag != null) {
         this.LootBags.remove(bag);
         if (this.originalDump != null) {
            this.originalDump.remove(bag);
         }

         new File(this.getPath() + "/lootbags/" + bag.getInternalName() + ".json").delete();
      }
   }

   public LootBag findShowcasedLootBag() {
      return this.getLootBags().stream().filter(LootBag::isShowcasedLootBag).findFirst().orElse(null);
   }

   public boolean isAlreadyToggled() {
      return this.findShowcasedLootBag() != null;
   }

   public void loadFromDisk() {
      File dir = new File(this.getPath() + "/lootbags");
      this.LootBags = Lists.newArrayList();
      if (dir.exists()) {
         ObjectMapper mapper = new ObjectMapper();
         mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
         mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

         for (File f : Objects.requireNonNull(dir.listFiles())) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".json")) {
               try {
                  LootBag loaded = mapper.readValue(f, LootBag.class);
                  if (loaded != null) {
                     loaded.modernize();
                     this.LootBags.add(loaded);
                  }
               } catch (Exception var9) {
                  this.plugin.getLogger().warning("Failed to load lootbag from " + f.getName() + ": " + var9.getMessage());
               }
            }
         }

         this.LootBags.sort(Comparator.comparing(LootBag::getInternalName));
         this.originalDump = Lists.newArrayList(this.LootBags);
      } else {
         this.originalDump = Lists.newArrayList();
      }
   }

   public void saveToDisk() {
      File dir = new File(this.path + "/lootbags");
      if (!dir.exists()) {
         dir.mkdirs();
      }
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

      if (this.originalDump == null) {
         this.originalDump = Lists.newArrayList(this.LootBags);
      }

      // Delete files for lootbags that were removed
      for (LootBag old : this.originalDump) {
         if (!this.LootBags.contains(old)) {
            File oldFile = new File(this.path + "/lootbags/" + old.getInternalName() + ".json");
            if (oldFile.exists()) {
               oldFile.delete();
            }
         }
      }

      // Save all current lootbags
      for (LootBag bag : this.LootBags) {
         File jsonFile = new File(this.path + "/lootbags/" + bag.getInternalName() + ".json");

         try (java.io.FileOutputStream fos = new java.io.FileOutputStream(jsonFile)) {
            mapper.writeValue(fos, bag);
         } catch (Exception var6) {
            this.plugin.getLogger().warning("Failed to save lootbag " + bag.getInternalName() + ": " + var6.toString());
         }
      }

      this.originalDump = Lists.newArrayList(this.LootBags);
   }

   public void saveSingle(LootBag bag) {
      File dir = new File(this.path + "/lootbags");
      if (!dir.exists()) {
         dir.mkdirs();
      }
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      mapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
      File jsonFile = new File(this.path + "/lootbags/" + bag.getInternalName() + ".json");

      try (java.io.FileOutputStream fos = new java.io.FileOutputStream(jsonFile)) {
         mapper.writeValue(fos, bag);
      } catch (Exception var6) {
         this.plugin.getLogger().warning("Failed to save lootbag " + bag.getInternalName() + ": " + var6.toString());
      }

      // Update originalDump to reflect current state
      if (this.originalDump == null) {
         this.originalDump = Lists.newArrayList(this.LootBags);
      } else if (!this.originalDump.contains(bag)) {
         this.originalDump.add(bag);
      }
   }

   public JavaPlugin getPlugin() {
      return this.plugin;
   }

   public String getPath() {
      return this.path;
   }

   public String getFile() {
      return this.file;
   }

   public List<LootBag> getLootBags() {
      return this.LootBags;
   }

   public List<LootBag> getOriginalDump() {
      return this.originalDump;
   }
}
