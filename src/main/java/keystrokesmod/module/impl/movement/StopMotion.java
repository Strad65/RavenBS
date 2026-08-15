package keystrokesmod.module.impl.movement;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;

public class StopMotion extends Module {
   private ButtonSetting stopX;
   private ButtonSetting stopY;
   private ButtonSetting stopZ;

   public StopMotion() {
      super("Stop Motion", Module.category.movement, 0);
      this.registerSetting(this.stopX = new ButtonSetting("Stop X", true));
      this.registerSetting(this.stopY = new ButtonSetting("Stop Y", true));
      this.registerSetting(this.stopZ = new ButtonSetting("Stop Z", true));
   }

   @Override
   public void onEnable() {
      if (this.stopX.isToggled()) {
         mc.thePlayer.motionX = 0.0;
      }

      if (this.stopY.isToggled()) {
         mc.thePlayer.motionY = 0.0;
      }

      if (this.stopZ.isToggled()) {
         mc.thePlayer.motionZ = 0.0;
      }

      this.disable();
   }
}
