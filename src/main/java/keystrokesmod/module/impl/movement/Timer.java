package keystrokesmod.module.impl.movement;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class Timer extends Module {
   private SliderSetting speed;
   private ButtonSetting strafeOnly;

   public Timer() {
      super("Timer", Module.category.movement, 0);
      this.registerSetting(this.speed = new SliderSetting("Speed", 1.0, 0.5, 2.5, 0.01));
      this.registerSetting(this.strafeOnly = new ButtonSetting("Strafe only", false));
   }

   @Override
   public String getInfo() {
      return Utils.asWholeNum(this.speed.getInput());
   }

   @Override
   public void onUpdate() {
      if (!(mc.currentScreen instanceof ClickGui)) {
         if (this.strafeOnly.isToggled() && mc.thePlayer.moveStrafing == 0.0F) {
            Utils.resetTimer();
            return;
         }

         ((IAccessorMinecraft)mc).getTimer().timerSpeed = (float)this.speed.getInput();
      } else {
         Utils.resetTimer();
      }
   }

   @Override
   public void onDisable() {
      Utils.resetTimer();
   }
}
