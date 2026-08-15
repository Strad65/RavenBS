package keystrokesmod.utility.profile;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;

public class ProfileModule extends Module {
   private Profile profile;
   public boolean saved = true;

   public ProfileModule(Profile profile, String name, int bind) {
      super(name, Module.category.profiles, bind);
      this.profile = profile;
      this.registerSetting(new ButtonSetting("Save profile", () -> {
         Utils.sendMessage("&7Saved profile: &b" + this.getName());
         keystrokesmod.Raven.profileManager.saveProfile(this.profile);
         this.saved = true;
      }));
      this.registerSetting(new ButtonSetting("Remove profile", () -> {
         Utils.sendMessage("&7Removed profile: &b" + this.getName());
         keystrokesmod.Raven.profileManager.deleteProfile(this.getName());
      }));
   }

   @Override
   public void toggle() {
      if (mc.currentScreen instanceof ClickGui || mc.currentScreen == null) {
         if (this.profile == keystrokesmod.Raven.currentProfile && this.saved) {
            return;
         }

         keystrokesmod.Raven.profileManager.loadProfile(this.getName());
         keystrokesmod.Raven.currentProfile = this.profile;
         if (Settings.sendMessage.isToggled()) {
            Utils.sendMessage("&7Enabled profile: &b" + this.getName());
         }

         this.saved = true;
      }
   }

   @Override
   public boolean isEnabled() {
      return keystrokesmod.Raven.currentProfile == null ? false : keystrokesmod.Raven.currentProfile.getModule() == this;
   }
}
