package keystrokesmod.utility.profile;

import java.awt.Desktop;
import java.io.IOException;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;

public class Manager extends Module {
   private ButtonSetting loadProfiles;
   private ButtonSetting openFolder;
   private ButtonSetting createProfile;

   public Manager() {
      super("Manager", Module.category.profiles);
      this.registerSetting(this.createProfile = new ButtonSetting("Create profile", () -> {
         if (Utils.nullCheck() && keystrokesmod.Raven.profileManager != null) {
            String name = "profile-";

            for (int i = 1; i <= 100; i++) {
               if (keystrokesmod.Raven.profileManager.getProfile(name + i) == null) {
                  name = name + i;
                  keystrokesmod.Raven.profileManager.saveProfile(new Profile(name, 0));
                  Utils.sendMessage("&7Created profile: &b" + name);
                  keystrokesmod.Raven.profileManager.loadProfiles();
                  break;
               }
            }
         }
      }));
      this.registerSetting(this.loadProfiles = new ButtonSetting("Load profiles", () -> {
         if (Utils.nullCheck() && keystrokesmod.Raven.profileManager != null) {
            keystrokesmod.Raven.profileManager.loadProfiles();
         }
      }));
      this.registerSetting(this.openFolder = new ButtonSetting("Open folder", () -> {
         try {
            Desktop.getDesktop().open(keystrokesmod.Raven.profileManager.directory);
         } catch (IOException ex) {
            keystrokesmod.Raven.profileManager.directory.mkdirs();
            Utils.sendMessage("&cError locating folder, recreated.");
         }
      }));
      this.ignoreOnSave = true;
      this.canBeEnabled = false;
   }
}
