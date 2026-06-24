package org.minecurse.lootbags.manager;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import org.minecurse.lootbags.LootBagPlugin;
import org.minecurse.lootbags.struct.LootBag;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

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
         file.mkdir();
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

         new File(this.getPath() + "/lootbags/" + bag.getInternalName() + ".yml").delete();
         new File(this.getPath() + "/lootbags/" + bag.getInternalName() + ".yaml").delete();
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
         ObjectMapper jsonMapper = new ObjectMapper();
         jsonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
         jsonMapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

         this.convertOldJsonFiles(dir, jsonMapper);

         for (File f : Objects.requireNonNull(dir.listFiles())) {
            String name = f.getName().toLowerCase();
            if (name.endsWith(".yml") || name.endsWith(".yaml")) {
               try {
                  LootBag loaded = this.readYaml(f, jsonMapper);
                  if (loaded != null) {
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

   private void convertOldJsonFiles(File dir, ObjectMapper jsonMapper) {
      for (File f : Objects.requireNonNull(dir.listFiles())) {
         if (f.getName().toLowerCase().endsWith(".json")) {
            String ymlName = f.getName().substring(0, f.getName().length() - 5) + ".yml";
            File ymlFile = new File(dir, ymlName);
            if (!ymlFile.exists()) {
               try {
                  LootBag bag = jsonMapper.readValue(f, LootBag.class);
                  if (bag != null) {
                     this.writeYaml(ymlFile, bag, jsonMapper);
                     this.plugin.getLogger().info("Converted " + f.getName() + " to " + ymlName);
                  }
               } catch (Exception var8) {
                  this.plugin.getLogger().warning("Failed to convert " + f.getName() + " to YAML: " + var8.getMessage());
               }
            }

            f.delete();
         }
      }
   }

   public void saveToDisk() {
      File dir = new File(this.path + "/lootbags");
      dir.mkdir();
      ObjectMapper jsonMapper = new ObjectMapper();
      jsonMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      jsonMapper.setVisibility(VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));

      if (this.originalDump == null) {
         this.originalDump = Lists.newArrayList(this.LootBags);
      }

      for (LootBag LootBag2 : this.originalDump) {
         if (!this.LootBags.contains(LootBag2)) {
            new File(this.path + "/lootbags/" + LootBag2.getInternalName() + ".yml").delete();
            new File(this.path + "/lootbags/" + LootBag2.getInternalName() + ".json").delete();
         } else {
            File ymlFile = new File(this.path + "/lootbags/" + LootBag2.getInternalName() + ".yml");

            try {
               this.writeYaml(ymlFile, LootBag2, jsonMapper);
            } catch (Exception var6) {
               this.plugin.getLogger().warning("Failed to save lootbag " + LootBag2.getInternalName() + ": " + var6.getMessage());
            }
         }
      }

      this.originalDump = Lists.newArrayList(this.LootBags);
   }

   @SuppressWarnings("unchecked")
   private void writeYaml(File file, LootBag bag, ObjectMapper jsonMapper) throws IOException {
      Map<String, Object> data = jsonMapper.convertValue(bag, Map.class);
      DumperOptions options = new DumperOptions();
      options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
      options.setPrettyFlow(true);
      options.setIndent(2);
      Yaml yaml = new Yaml(options);
      String yamlStr = yaml.dump(data);
      if (file.exists()) {
         file.delete();
      }

      Files.write(file.toPath(), yamlStr.getBytes(StandardCharsets.UTF_8));
   }

   private LootBag readYaml(File file, ObjectMapper jsonMapper) throws IOException {
      String yamlStr = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      Yaml yaml = new Yaml();
      Object loaded = yaml.load(yamlStr);
      if (!(loaded instanceof Map)) {
         return null;
      } else {
         @SuppressWarnings("unchecked")
         Map<String, Object> data = (Map<String, Object>) loaded;
         return jsonMapper.convertValue(data, LootBag.class);
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
