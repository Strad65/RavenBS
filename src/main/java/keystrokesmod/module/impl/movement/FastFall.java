package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FastFall extends Module {
   public SliderSetting mode;
   private ButtonSetting disableAdventure;
   private ButtonSetting ignoreVoid;
   private ButtonSetting disableNoFall;
   private String[] modes = new String[]{"Accelerate", "Timer"};
   private double initialY;
   private boolean isFalling;
   private int fallTicks;
   private int motion;
   private SliderSetting ticks;

   public FastFall() {
      super("FastFall", Module.category.player);
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
      this.registerSetting(this.disableAdventure = new ButtonSetting("Disable adventure", false));
      this.registerSetting(this.ignoreVoid = new ButtonSetting("Ignore void", true));
      this.registerSetting(this.disableNoFall = new ButtonSetting("Disable while NoFalling", true));
      this.registerSetting(this.ticks = new SliderSetting("Intervals", 2.0, 1.0, 10.0, 1.0));
   }

   @Override
   public void onDisable() {
      Utils.resetTimer();
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.reset()) {
         if (this.isFalling) {
            Utils.resetTimer();
         }

         this.initialY = mc.thePlayer.posY;
         this.isFalling = false;
         this.fallTicks = this.motion = 0;
      } else {
         if (mc.thePlayer.fallDistance >= 2.0) {
            this.isFalling = true;
         }

         double predictedY = mc.thePlayer.posY + mc.thePlayer.motionY;
         double distanceFallen = this.initialY - predictedY;
         if (this.isFalling && this.mode.getInput() == 0.0) {
            this.fallTicks++;
            Utils.resetTimer();
            if (this.fallTicks >= this.ticks.getInput()) {
               mc.thePlayer.motionY = mc.thePlayer.motionY - this.motion / 95.0;
               this.fallTicks = 0;
               this.motion++;
            }
         }

         if (this.isFalling && this.mode.getInput() == 1.0) {
            this.fallTicks++;
            Utils.resetTimer();
            if (this.fallTicks >= this.ticks.getInput()) {
               Utils.getTimer().timerSpeed = 1.5F;
               this.fallTicks = 0;
            }
         }
      }
   }

   @Override
   public String getInfo() {
      return this.modes[(int)this.mode.getInput()];
   }

   private boolean isVoid() {
      return Utils.overVoid(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
   }

   private boolean reset() {
      if (this.disableAdventure.isToggled() && mc.playerController.getCurrentGameType().isAdventure()) {
         return true;
      } else if (this.ignoreVoid.isToggled() && this.isVoid()) {
         return true;
      } else if (Utils.isReplay()) {
         return true;
      } else if (mc.thePlayer.onGround) {
         return true;
      } else if (mc.thePlayer.motionY > -0.0784) {
         return true;
      } else if (mc.thePlayer.capabilities.isCreativeMode) {
         return true;
      } else if (mc.thePlayer.capabilities.isFlying) {
         return true;
      } else {
         return ModuleManager.scaffold.isEnabled ? true : ModuleManager.noFall.isFalling;
      }
   }
}
