package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Momentum extends Module {
   public ButtonSetting renderTimer;
   public boolean blink;
   private int wait;
   private int latestY;
   private boolean setY;

   public Momentum() {
      super("Momentum", Module.category.movement, 0);
      this.registerSetting(this.renderTimer = new ButtonSetting("Render Timer", false));
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (mc.thePlayer.onGround && !this.blink) {
         this.reset();
      } else {
         if (mc.thePlayer.motionY < -0.0784000015258789) {
            if (!this.setY) {
               this.latestY = (int)mc.thePlayer.posY;
               this.setY = true;
            }
         } else {
            this.setY = false;
         }

         if (Utils.fallDistZ() >= 0.0 && Utils.fallDistZ() <= 2.0 && this.latestY - mc.thePlayer.posY >= 2.0) {
            this.blink = true;
         }

         if (this.blink) {
            this.wait++;
            if (!mc.thePlayer.onGround && this.wait >= 2 || ModuleUtils.groundTicks >= 3) {
               this.reset();
            }
         }
      }
   }

   private void reset() {
      this.blink = this.setY = false;
      this.wait = this.latestY = 0;
   }
}
