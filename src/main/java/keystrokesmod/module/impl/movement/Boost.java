package keystrokesmod.module.impl.movement;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class Boost extends Module {
   public static DescriptionSetting c;
   public static SliderSetting a;
   public static SliderSetting b;
   private int i = 0;
   private boolean t = false;

   public Boost() {
      super("Boost", Module.category.movement, 0);
      this.registerSetting(c = new DescriptionSetting("20 ticks are in 1 second"));
      this.registerSetting(a = new SliderSetting("Multiplier", 2.0, 1.0, 3.0, 0.05));
      this.registerSetting(b = new SliderSetting("Time (ticks)", 15.0, 1.0, 80.0, 1.0));
   }

   @Override
   public void onEnable() {
      if (ModuleManager.timer.isEnabled()) {
         this.t = true;
         ModuleManager.timer.disable();
      }
   }

   @Override
   public void onDisable() {
      this.i = 0;
      if (((IAccessorMinecraft)mc).getTimer().timerSpeed != 1.0F) {
         Utils.resetTimer();
      }

      if (this.t) {
         ModuleManager.timer.enable();
      }

      this.t = false;
   }

   @Override
   public void onUpdate() {
      if (this.i == 0) {
         this.i = mc.thePlayer.ticksExisted;
      }

      ((IAccessorMinecraft)mc).getTimer().timerSpeed = (float)a.getInput();
      if (this.i == mc.thePlayer.ticksExisted - b.getInput()) {
         Utils.resetTimer();
         this.disable();
      }
   }
}
