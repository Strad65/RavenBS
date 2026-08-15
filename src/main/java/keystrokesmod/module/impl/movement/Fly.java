package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.apache.commons.lang3.RandomUtils;

public class Fly extends Module {
   public SliderSetting mode;
   public static SliderSetting horizontalSpeed;
   private SliderSetting verticalSpeed;
   private ButtonSetting showBPS;
   private ButtonSetting stopMotion;
   private boolean d;
   private boolean a = false;
   private float firstYaw;
   private float firstPitch;
   private String[] modes = new String[]{"Vanilla", "Fast", "Fast 2", "Freeze", "OP"};
   private int fm;

   public Fly() {
      super("Fly", Module.category.movement);
      this.registerSetting(this.mode = new SliderSetting("Fly", 0, this.modes));
      this.registerSetting(horizontalSpeed = new SliderSetting("Horizontal speed", 2.0, 0.0, 9.0, 0.1));
      this.registerSetting(this.verticalSpeed = new SliderSetting("Vertical speed", 2.0, 0.0, 9.0, 0.1));
      this.registerSetting(this.showBPS = new ButtonSetting("Show BPS", false));
      this.registerSetting(this.stopMotion = new ButtonSetting("Stop motion", false));
   }

   @Override
   public void guiUpdate() {
      horizontalSpeed.setVisible(this.mode.getInput() < 3.0, this);
      this.verticalSpeed.setVisible(this.mode.getInput() < 3.0, this);
   }

   @Override
   public void onEnable() {
      this.d = mc.thePlayer.capabilities.isFlying;
      this.firstYaw = mc.thePlayer.rotationYaw;
      this.firstPitch = mc.thePlayer.rotationPitch;
   }

   @Override
   public void onUpdate() {
      switch ((int)this.mode.getInput()) {
         case 0:
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.capabilities.setFlySpeed((float)(0.05F * horizontalSpeed.getInput()));
            mc.thePlayer.capabilities.isFlying = true;
            break;
         case 1:
            mc.thePlayer.onGround = true;
            if (mc.currentScreen == null) {
               if (Utils.jumpDown()) {
                  mc.thePlayer.motionY = 0.3 * this.verticalSpeed.getInput();
               } else if (Utils.jumpDown()) {
                  mc.thePlayer.motionY = -0.3 * this.verticalSpeed.getInput();
               } else {
                  mc.thePlayer.motionY = 0.0;
               }
            } else {
               mc.thePlayer.motionY = 0.0;
            }

            mc.thePlayer.capabilities.setFlySpeed(0.2F);
            mc.thePlayer.capabilities.isFlying = true;
            setSpeed(0.85 * horizontalSpeed.getInput());
            break;
         case 2:
            double nextDouble = RandomUtils.nextDouble(1.0E-7, 1.2E-7);
            if (mc.thePlayer.ticksExisted % 2 == 0) {
               nextDouble = -nextDouble;
            }

            if (!mc.thePlayer.onGround) {
               mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + nextDouble, mc.thePlayer.posZ);
            }

            mc.thePlayer.motionY = 0.0;
            setSpeed(0.4 * horizontalSpeed.getInput());
            break;
         case 3:
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
            Utils.setSpeed(0.0);
            break;
         case 4:
            this.op();
      }
   }

   private void op() {
      if (mc.thePlayer.motionY >= -0.0784000015258789) {
         this.fm = 0;
      } else {
         this.fm++;
      }

      double hSpeed = 0.3;
      double vSpeed = 1.2;
      if (mc.thePlayer.hurtTime > 0) {
         hSpeed += 0.5;
         vSpeed += 0.5;
      }

      if (mc.currentScreen == null) {
         if (Utils.jumpDown()) {
            mc.thePlayer.motionY = 0.3 * vSpeed;
         } else if (Utils.sneakDown()) {
            double fallMulti = this.fm / 100.0;
            mc.thePlayer.motionY = (-0.3 - fallMulti) * vSpeed;
         } else {
            mc.thePlayer.motionY = 0.0;
         }
      } else {
         mc.thePlayer.motionY = 0.0;
      }

      mc.thePlayer.capabilities.setFlySpeed(0.2F);
      mc.thePlayer.capabilities.isFlying = true;
      setSpeed(0.85 * hSpeed);
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onMoveInput(PrePlayerInputEvent e) {
      if (this.mode.getInput() == 3.0) {
         e.setForward(0.0F);
         e.setStrafe(0.0F);
      }
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      if (this.mode.getInput() == 3.0) {
         e.setYaw(this.firstYaw);
         e.setPitch(this.firstPitch);
      }
   }

   @Override
   public void onDisable() {
      this.fm = 0;
      if (mc.thePlayer.capabilities.allowFlying) {
         mc.thePlayer.capabilities.isFlying = this.d;
      } else {
         mc.thePlayer.capabilities.isFlying = false;
      }

      this.d = false;
      switch ((int)this.mode.getInput()) {
         case 0:
         case 1:
            mc.thePlayer.capabilities.setFlySpeed(0.05F);
            break;
         case 2:
            this.a = false;
      }

      if (this.stopMotion.isToggled()) {
         mc.thePlayer.motionZ = 0.0;
         mc.thePlayer.motionY = 0.0;
         mc.thePlayer.motionX = 0.0;
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent e) {
      if (this.showBPS.isToggled() && e.phase == Phase.END && Utils.nullCheck()) {
         if (mc.currentScreen == null && !mc.gameSettings.showDebugInfo) {
            RenderUtils.renderBPS(true, false);
         }
      }
   }

   public static void setSpeed(double n) {
      if (n == 0.0) {
         mc.thePlayer.motionZ = 0.0;
         mc.thePlayer.motionX = 0.0;
      } else {
         double n3 = mc.thePlayer.movementInput.moveForward;
         double n4 = mc.thePlayer.movementInput.moveStrafe;
         float rotationYaw = mc.thePlayer.rotationYaw;
         if (n3 == 0.0 && n4 == 0.0) {
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionX = 0.0;
         } else {
            if (n3 != 0.0) {
               if (n4 > 0.0) {
                  rotationYaw += n3 > 0.0 ? -45 : 45;
               } else if (n4 < 0.0) {
                  rotationYaw += n3 > 0.0 ? 45 : -45;
               }

               n4 = 0.0;
               if (n3 > 0.0) {
                  n3 = 1.0;
               } else if (n3 < 0.0) {
                  n3 = -1.0;
               }
            }

            double radians = Math.toRadians(rotationYaw + 90.0F);
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);
            mc.thePlayer.motionX = n3 * n * cos + n4 * n * sin;
            mc.thePlayer.motionZ = n3 * n * sin - n4 * n * cos;
         }
      }
   }
}
