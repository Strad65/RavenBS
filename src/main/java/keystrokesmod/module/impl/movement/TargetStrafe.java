package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class TargetStrafe extends Module {
   private ButtonSetting requireBhop;
   private ButtonSetting requireJump;
   private ButtonSetting requireRMB;
   private SliderSetting radius;
   private double angle;

   public TargetStrafe() {
      super("TargetStrafe", Module.category.movement);
      this.registerSetting(this.requireBhop = new ButtonSetting("Require bhop", false));
      this.registerSetting(this.requireJump = new ButtonSetting("Require jump key", false));
      this.registerSetting(this.requireRMB = new ButtonSetting("Require RMB", false));
      this.registerSetting(this.radius = new SliderSetting("Radius", 0.6, 0.0, 3.0, 0.1));
   }

   @Override
   public void guiUpdate() {
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onMoveInput(PrePlayerInputEvent e) {
      if (!this.requireBhop.isToggled() || ModuleManager.bhop.isEnabled()) {
         if (!this.requireJump.isToggled() || Utils.jumpDown()) {
            if (!this.requireRMB.isToggled() || Mouse.isButtonDown(1)) {
               if (!ModuleManager.scaffold.isEnabled) {
                  if (KillAura.target != null) {
                     EntityLivingBase targetPosition = KillAura.target;
                     this.angle++;
                     double offsetX = (float)this.radius.getInput() * Math.cos(this.angle);
                     double offsetZ = (float)this.radius.getInput() * Math.sin(this.angle);
                     double directionX = targetPosition.getPosition().getX() + offsetX - mc.thePlayer.posX;
                     double directionZ = targetPosition.getPosition().getZ() + offsetZ - mc.thePlayer.posZ;
                     double magnitude = Math.sqrt(directionX * directionX + directionZ * directionZ);
                     if (magnitude > 0.01) {
                        directionX /= magnitude;
                        directionZ /= magnitude;
                        double yawRadians = Math.toRadians(-mc.thePlayer.rotationYaw);
                        double rotatedX = directionX * Math.cos(yawRadians) - directionZ * Math.sin(yawRadians);
                        double rotatedZ = directionX * Math.sin(yawRadians) + directionZ * Math.cos(yawRadians);
                        e.setStrafe((float)rotatedX);
                        e.setForward((float)rotatedZ);
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
   }
}
