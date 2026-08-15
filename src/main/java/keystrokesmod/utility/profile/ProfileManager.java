package keystrokesmod.utility.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.event.PostProfileLoadEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.Sprint;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.impl.render.TargetInfo;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class ProfileManager {
   public static Minecraft mc = Minecraft.getMinecraft();
   public File directory;
   public List<Profile> profiles = new ArrayList<>();

   public ProfileManager() {
      this.directory = new File(mc.mcDataDir + File.separator + "keystrokes", "profiles");
      if (!this.directory.exists()) {
         boolean success = this.directory.mkdirs();
         if (!success) {
            System.out.println("There was an issue creating profiles directory.");
            return;
         }
      }

      if (this.directory.listFiles().length == 0) {
         this.saveProfile(new Profile("default", 0));
      }
   }

   public void saveProfile(Profile profile) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("keybind", profile.getModule().getKeycode());
      JsonArray jsonArray = new JsonArray();

      for (Module module : keystrokesmod.Raven.moduleManager.getModules()) {
         if (!module.ignoreOnSave) {
            JsonObject moduleInformation = getJsonObject(module);
            jsonArray.add(moduleInformation);
         }
      }

      if (keystrokesmod.Raven.scriptManager != null && keystrokesmod.Raven.scriptManager.scripts != null) {
         for (Module module : keystrokesmod.Raven.scriptManager.scripts.values()) {
            if (!module.ignoreOnSave) {
               JsonObject moduleInformation = getJsonObject(module);
               jsonArray.add(moduleInformation);
            }
         }
      }

      jsonObject.add("modules", jsonArray);

      try (FileWriter fileWriter = new FileWriter(new File(this.directory, profile.getName() + ".json"))) {
         Gson gson = new GsonBuilder().setPrettyPrinting().create();
         gson.toJson(jsonObject, fileWriter);
      } catch (Exception e) {
         this.failedMessage("save", profile.getName());
         e.printStackTrace();
      }
   }

   private static JsonObject getJsonObject(Module module) {
      JsonObject moduleInformation = new JsonObject();
      moduleInformation.addProperty(
         "name",
         module.moduleCategory() == Module.category.scripts && !(module instanceof keystrokesmod.script.Manager) ? "sc-" + module.getName() : module.getName()
      );
      if (module.canBeEnabled) {
         moduleInformation.addProperty("enabled", module.isEnabled());
         moduleInformation.addProperty("hidden", module.isHidden());
         moduleInformation.addProperty("keybind", module.getKeycode());
      }

      if (module instanceof HUD) {
         moduleInformation.addProperty("posX", HUD.posX);
         moduleInformation.addProperty("posY", HUD.posY);
      } else if (module instanceof TargetInfo) {
         moduleInformation.addProperty("posX", ModuleManager.targetInfo.posX);
         moduleInformation.addProperty("posY", ModuleManager.targetInfo.posY);
      } else if (module instanceof Sprint) {
         moduleInformation.addProperty("posX", ModuleManager.sprint.posX);
         moduleInformation.addProperty("posY", ModuleManager.sprint.posY);
         moduleInformation.addProperty("text", ModuleManager.sprint.text);
      } else if (module instanceof Gui) {
         for (CategoryComponent c : ClickGui.categories) {
            moduleInformation.addProperty(c.category.name(), c.x + "," + c.y + "," + c.opened);
         }
      }

      for (Setting setting : module.getSettings()) {
         if (setting instanceof ButtonSetting && !((ButtonSetting)setting).isMethodButton) {
            moduleInformation.addProperty(setting.getName(), ((ButtonSetting)setting).isToggled());
         } else if (setting instanceof SliderSetting) {
            moduleInformation.addProperty(setting.getName(), ((SliderSetting)setting).getInput());
         } else if (setting instanceof KeySetting) {
            moduleInformation.addProperty(setting.getName(), ((KeySetting)setting).getKey());
         }
      }

      return moduleInformation;
   }

   public void loadProfile(String name) {
      for (File file : this.getProfileFiles()) {
         if (!file.exists()) {
            this.failedMessage("load", name);
            System.out.println("Failed to load " + name);
            return;
         }

         if (file.getName().equals(name + ".json")) {
            if (keystrokesmod.Raven.scriptManager != null) {
               for (Module module : keystrokesmod.Raven.scriptManager.scripts.values()) {
                  if (module.canBeEnabled()) {
                     module.disable();
                     module.setBind(0);
                  }
               }
            }

            for (Module module : keystrokesmod.Raven.getModuleManager().getModules()) {
               if (module.canBeEnabled()) {
                  module.disable();
                  module.setBind(0);
               }
            }

            try {
               try (FileReader fileReader = new FileReader(file)) {
                  JsonParser jsonParser = new JsonParser();
                  JsonObject profileJson = jsonParser.parse(fileReader).getAsJsonObject();
                  if (profileJson == null) {
                     this.failedMessage("load", name);
                     return;
                  }

                  JsonArray modules = profileJson.getAsJsonArray("modules");
                  if (modules != null) {
                     boolean currentProfileGuiSave = Settings.loadGuiPositions.isToggled();

                     for (JsonElement moduleJson : modules) {
                        JsonObject moduleInformation = moduleJson.getAsJsonObject();
                        String moduleName = moduleInformation.get("name").getAsString();
                        if (moduleName != null && !moduleName.isEmpty()) {
                           Module module = keystrokesmod.Raven.moduleManager.getModule(moduleName);
                           if (module == null && moduleName.startsWith("sc-") && keystrokesmod.Raven.scriptManager != null) {
                              for (Module module1 : keystrokesmod.Raven.scriptManager.scripts.values()) {
                                 if (module1.getName().equals(moduleName.substring(3))) {
                                    module = module1;
                                 }
                              }
                           }

                           if (module != null) {
                              if (module.canBeEnabled()) {
                                 if (moduleInformation.has("enabled")) {
                                    boolean enabled = moduleInformation.get("enabled").getAsBoolean();
                                    if (enabled) {
                                       module.enable();
                                    } else {
                                       module.disable();
                                    }
                                 }

                                 if (moduleInformation.has("hidden")) {
                                    boolean hidden = moduleInformation.get("hidden").getAsBoolean();
                                    module.setHidden(hidden);
                                 }

                                 if (moduleInformation.has("keybind")) {
                                    int keybind = moduleInformation.get("keybind").getAsInt();
                                    module.setBind(keybind);
                                 }
                              }

                              if (module.getName().equals("HUD")) {
                                 if (moduleInformation.has("posX")) {
                                    int hudX = moduleInformation.get("posX").getAsInt();
                                    HUD.posX = hudX;
                                 }

                                 if (moduleInformation.has("posY")) {
                                    int hudY = moduleInformation.get("posY").getAsInt();
                                    HUD.posY = hudY;
                                 }
                              } else if (module.getName().equals("TargetInfo")) {
                                 if (moduleInformation.has("posX")) {
                                    int posX = moduleInformation.get("posX").getAsInt();
                                    ModuleManager.targetInfo.posX = posX;
                                 }

                                 if (moduleInformation.has("posY")) {
                                    int posY = moduleInformation.get("posY").getAsInt();
                                    ModuleManager.targetInfo.posY = posY;
                                 }
                              } else if (module.getName().equals("Sprint")) {
                                 if (moduleInformation.has("posX")) {
                                    float posX = moduleInformation.get("posX").getAsFloat();
                                    ModuleManager.sprint.posX = posX;
                                 }

                                 if (moduleInformation.has("posY")) {
                                    float posY = moduleInformation.get("posY").getAsFloat();
                                    ModuleManager.sprint.posY = posY;
                                 }

                                 if (moduleInformation.has("text")) {
                                    String text = moduleInformation.get("text").getAsString();
                                    ModuleManager.sprint.text = text;
                                 }
                              } else if (currentProfileGuiSave && module.getName().equals("Gui")) {
                                 for (Entry<String, JsonElement> setting : moduleInformation.entrySet()) {
                                    String settingName = setting.getKey();
                                    if (Module.categoriesString.contains(settingName)) {
                                       String element = setting.getValue().getAsString();
                                       String[] statesStr = element.split(",");
                                       float posX = Float.parseFloat(statesStr[0]);
                                       float posY = Float.parseFloat(statesStr[1]);

                                       for (CategoryComponent c : ClickGui.categories) {
                                          if (c.category.name().equals(settingName)) {
                                             c.setX(posX, true);
                                             c.setY(posY, true);
                                             if (statesStr.length > 2) {
                                                boolean opened = Boolean.parseBoolean(statesStr[2]);
                                                c.opened = opened;
                                             }
                                             break;
                                          }
                                       }
                                    }
                                 }
                              }

                              for (Setting setting : module.getSettings()) {
                                 setting.loadProfile(moduleInformation);
                              }

                              keystrokesmod.Raven.currentProfile = this.getProfile(name);
                           }
                        }
                     }

                     MinecraftForge.EVENT_BUS.post(new PostProfileLoadEvent(keystrokesmod.Raven.currentProfile.getName()));
                     continue;
                  }

                  this.failedMessage("load", name);
               }

               return;
            } catch (Exception e) {
               this.failedMessage("load", name);
               e.printStackTrace();
            }
         }
      }
   }

   public void deleteProfile(String name) {
      Iterator<Profile> iterator = this.profiles.iterator();

      while (iterator.hasNext()) {
         Profile profile = iterator.next();
         if (profile.getName().equals(name)) {
            iterator.remove();
         }
      }

      if (this.directory.exists()) {
         File[] files = this.directory.listFiles();

         for (File file : files) {
            if (file.getName().equals(name + ".json")) {
               file.delete();
            }
         }
      }
   }

   public void loadProfiles() {
      this.profiles.clear();
      if (this.directory.exists()) {
         File[] files = this.directory.listFiles();

         for (File file : files) {
            try {
               try (FileReader fileReader = new FileReader(file)) {
                  JsonParser jsonParser = new JsonParser();
                  JsonObject profileJson = jsonParser.parse(fileReader).getAsJsonObject();
                  String profileName = file.getName().replace(".json", "");
                  if (profileJson != null) {
                     int keybind = 0;
                     if (profileJson.has("keybind")) {
                        keybind = profileJson.get("keybind").getAsInt();
                     }

                     Profile profile = new Profile(profileName, keybind);
                     this.profiles.add(profile);
                     continue;
                  }

                  this.failedMessage("load", profileName);
               }

               return;
            } catch (Exception e) {
               Utils.sendMessage("&cFailed to load profiles.");
               e.printStackTrace();
            }
         }

         for (CategoryComponent categoryComponent : ClickGui.categories) {
            if (categoryComponent.category == Module.category.profiles) {
               categoryComponent.reloadModules(true);
            }
         }

         Utils.sendMessage("&b" + keystrokesmod.Raven.profileManager.getProfileFiles().size() + " &7profiles loaded.");
      }
   }

   public List<File> getProfileFiles() {
      List<File> profileFiles = new ArrayList<>();
      if (this.directory.exists()) {
         File[] files = this.directory.listFiles();

         for (File file : files) {
            if (file.getName().endsWith(".json")) {
               profileFiles.add(file);
            }
         }
      }

      return profileFiles;
   }

   public Profile getProfile(String name) {
      for (Profile profile : this.profiles) {
         if (profile.getName().equals(name)) {
            return profile;
         }
      }

      return null;
   }

   public void failedMessage(String reason, String name) {
      Utils.sendMessage("&cFailed to " + reason + ": &b" + name);
   }
}
