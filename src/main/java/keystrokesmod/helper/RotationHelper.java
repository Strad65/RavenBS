package keystrokesmod.helper;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.GameTickEvent;
import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.StrafeEvent;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RotationHelper {
   private static RotationHelper INSTANCE = new RotationHelper();
   private Float serverYaw = null;
   private Float serverPitch = null;
   private boolean setRotations = false;
   public boolean forceMovementFix = false;
   private Minecraft mc = Minecraft.getMinecraft();

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPreUpdate(PreUpdateEvent e) {
      ClientRotationEvent event = new ClientRotationEvent(this.serverYaw, this.serverPitch);
      MinecraftForge.EVENT_BUS.post(event);
      if (event.yaw != null && !event.yaw.isNaN()) {
         this.serverYaw = event.yaw;
         this.setRotations = true;
      }

      if (event.pitch != null && !event.pitch.isNaN()) {
         this.serverPitch = event.pitch;
         this.setRotations = true;
      }
   }

   @SubscribeEvent
   public void onRunTick(GameTickEvent e) {
      this.serverYaw = this.serverPitch = null;
      this.setRotations = this.forceMovementFix = false;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPostInput(PostPlayerInputEvent event) {
      if (this.fixMovement()) {
         float sneakMultiplier = this.mc.thePlayer.movementInput.sneak ? 0.3F : 1.0F;
         float yaw = this.serverYaw;
         float forward = this.mc.thePlayer.movementInput.moveForward;
         float strafe = this.mc.thePlayer.movementInput.moveStrafe;
         if (forward != 0.0F || strafe != 0.0F) {
            double angle = MathHelper.wrapAngleTo180_double(Math.toDegrees(getDirection(this.mc.thePlayer.rotationYaw, forward, strafe)));
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            float closestDifference = Float.MAX_VALUE;

            for (float pfRaw = -1.0F; pfRaw <= 1.0F; pfRaw++) {
               for (float psRaw = -1.0F; psRaw <= 1.0F; psRaw++) {
                  if (pfRaw != 0.0F || psRaw != 0.0F) {
                     float predictedForward = pfRaw * sneakMultiplier;
                     float predictedStrafe = psRaw * sneakMultiplier;
                     double predictedAngle = MathHelper.wrapAngleTo180_double(Math.toDegrees(getDirection(yaw, predictedForward, predictedStrafe)));
                     double difference = Math.abs(angle - predictedAngle);
                     if (difference < closestDifference) {
                        closestDifference = (float)difference;
                        closestForward = predictedForward;
                        closestStrafe = predictedStrafe;
                     }
                  }
               }
            }

            this.mc.thePlayer.movementInput.moveForward = closestForward;
            this.mc.thePlayer.movementInput.moveStrafe = closestStrafe;
            Settings.fixedForward = closestForward;
            Settings.fixedStrafe = closestStrafe;
         }
      }
   }

   @SubscribeEvent
   public void onStrafe(StrafeEvent e) {
      if (this.fixMovement()) {
         e.setYaw(this.serverYaw);
      }
   }

   @SubscribeEvent
   public void onJump(JumpEvent e) {
      if (this.fixMovement()) {
         e.setYaw(this.serverYaw);
      }
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      if (this.setRotations) {
         if (this.serverYaw != null && !this.serverYaw.isNaN()) {
            e.setYaw(this.serverYaw);
         }

         if (this.serverPitch != null && !this.serverPitch.isNaN()) {
            e.setPitch(this.serverPitch);
         }
      }
   }

   private boolean fixMovement() {
      return (Settings.movementFix != null && Settings.movementFix.isToggled() || this.forceMovementFix) && this.setRotations;
   }

   public static double getDirection(float rotationYaw, double moveForward, double moveStrafing) {
      if (moveForward < 0.0) {
         rotationYaw += 180.0F;
      }

      float forward = 1.0F;
      if (moveForward < 0.0) {
         forward = -0.5F;
      } else if (moveForward > 0.0) {
         forward = 0.5F;
      }

      if (moveStrafing > 0.0) {
         rotationYaw -= 90.0F * forward;
      }

      if (moveStrafing < 0.0) {
         rotationYaw += 90.0F * forward;
      }

      return Math.toRadians(rotationYaw);
   }

   public static RotationHelper get() {
      return INSTANCE;
   }

   public void setRotations(float yaw, float pitch) {
      this.serverYaw = yaw;
      this.serverPitch = pitch;
      this.setRotations = true;
   }

   public void setYaw(float yaw) {
      this.serverYaw = yaw;
      this.setRotations = true;
   }

   public void setPitch(float pitch) {
      this.serverPitch = pitch;
      this.setRotations = true;
   }
}
