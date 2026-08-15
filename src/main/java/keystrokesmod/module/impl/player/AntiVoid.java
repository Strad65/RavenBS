package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlinkHandler;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiVoid extends Module {
   private static SliderSetting distance;
   private ButtonSetting disableLJ;
   private ButtonSetting disablePractice;
   public ButtonSetting renderTimer;
   public boolean wait;
   public double y;
   public boolean blink;
   public boolean setPos;

   public AntiVoid() {
      super("AntiVoid", Module.category.player);
      this.registerSetting(distance = new SliderSetting("Distance", "", 5.0, 1.0, 10.0, 0.5));
      this.registerSetting(this.renderTimer = new ButtonSetting("Render Timer", false));
      this.registerSetting(this.disableLJ = new ButtonSetting("Disable with Long Jump", false));
      this.registerSetting(this.disablePractice = new ButtonSetting("Disable in Practice", false));
   }

   @Override
   public void onDisable() {
      this.blink = this.setPos = false;
   }

   @Override
   public String getInfo() {
      return BlinkHandler.blinkTicks + "";
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      this.handle();
      if (!Utils.overVoid() || mc.thePlayer.onGround) {
         this.setPos = false;
         this.blink = false;
      }

      if ((!this.dist() || !Utils.overVoid()) && (!this.disableLJ.isToggled() || !LongJump.function)) {
         if (this.blink && mc.thePlayer.posY <= this.y - distance.getInput()) {
            this.setPos = true;
            this.blink = false;
            this.wait = true;
         }
      } else {
         this.setPos = false;
         this.blink = false;
         this.wait = true;
      }
   }

   private void handle() {
      if (this.blink || Utils.overVoid() && !mc.thePlayer.onGround) {
         if (this.blink || !Utils.isReplay() && !Utils.spectatorCheck() && (!Utils.isBedwarsPractice() || !this.disablePractice.isToggled())) {
            if (mc.thePlayer.ticksExisted > 10) {
               if (!this.wait) {
                  this.blink = true;
               }
            }
         }
      } else {
         this.y = mc.thePlayer.posY;
         this.wait = false;
      }
   }

   public boolean dist() {
      double minMotion = 0.15;
      int dist1 = 1;
      int dist2 = 3;
      int dist3 = 5;
      int dist4 = 7;
      if (mc.thePlayer.isCollidedHorizontally) {
         return false;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ) > dist1) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ) > dist1
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ) > dist1
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 1) > dist1
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 1) > dist1
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ - 1) > dist1
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ + 1) > dist1
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 1) > dist1
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ - 1) > dist1
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ) > dist2
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 2) > dist2
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 1) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ + 1) > dist2
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 2) > dist2
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ + 2) > dist2
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 1) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 2) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 1) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 2) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ - 1) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ - 2) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 1) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 2) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 3) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 2) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 1) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 3) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 2) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 1) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 3) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 2) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 1) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 3) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 2) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 1) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else {
         return Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ - 4) > dist4
               && mc.thePlayer.motionZ <= -minMotion
            ? true
            : Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ - 4) > dist4
               && mc.thePlayer.motionZ <= -minMotion;
      }
   }
}
