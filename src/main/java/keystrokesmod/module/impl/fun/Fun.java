package keystrokesmod.module.impl.fun;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;

public class Fun {
   public static class ExtraBobbing extends Module {
      public SliderSetting level;
      private boolean b;

      public ExtraBobbing() {
         super("Extra Bobbing", Module.category.fun, 0);
         this.registerSetting(this.level = new SliderSetting("Level", 1.0, 0.0, 8.0, 0.1));
      }

      @Override
      public void onEnable() {
         this.b = mc.gameSettings.viewBobbing;
         if (!this.b) {
            mc.gameSettings.viewBobbing = true;
         }
      }

      @Override
      public void onDisable() {
         mc.gameSettings.viewBobbing = this.b;
      }

      @Override
      public void onUpdate() {
         if (!mc.gameSettings.viewBobbing) {
            mc.gameSettings.viewBobbing = true;
         }

         if (mc.thePlayer.movementInput.moveForward != 0.0F || mc.thePlayer.movementInput.moveStrafe != 0.0F) {
            EntityPlayerSP var10000 = mc.thePlayer;
            var10000.cameraYaw = (float)(var10000.cameraYaw + this.level.getInput() / 2.0);
         }
      }
   }

   public static class FlameTrail extends Module {
      public SliderSetting a;

      public FlameTrail() {
         super("Flame Trail", Module.category.fun, 0);
      }

      @Override
      public void onUpdate() {
         Vec3 vec = mc.thePlayer.getLookVec();
         double x = mc.thePlayer.posX - vec.xCoord * 2.0;
         double y = mc.thePlayer.posY + (mc.thePlayer.getEyeHeight() - 0.2);
         double z = mc.thePlayer.posZ - vec.zCoord * 2.0;
         mc.thePlayer.worldObj.spawnParticle(EnumParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0, new int[]{0});
      }
   }

   public static class SlyPort extends Module {
      public SliderSetting range;
      public ButtonSetting playSound;
      public ButtonSetting playersOnly;
      public ButtonSetting aim;

      public SlyPort() {
         super("SlyPort", Module.category.fun, 0);
         this.registerSetting(new DescriptionSetting("Teleport behind enemies."));
         this.registerSetting(this.range = new SliderSetting("Range", 6.0, 2.0, 15.0, 1.0));
         this.registerSetting(this.aim = new ButtonSetting("Aim", true));
         this.registerSetting(this.playSound = new ButtonSetting("Play sound", true));
         this.registerSetting(this.playersOnly = new ButtonSetting("Players only", true));
      }

      @Override
      public void onEnable() {
         Entity en = this.ge();
         if (en != null) {
            this.tp(en);
         }

         this.disable();
      }

      private void tp(Entity en) {
         if (this.playSound.isToggled()) {
            mc.thePlayer.playSound("mob.endermen.portal", 1.0F, 1.0F);
         }

         Vec3 vec = en.getLookVec();
         double x = en.posX - vec.xCoord * 2.5;
         double z = en.posZ - vec.zCoord * 2.5;
         mc.thePlayer.setPosition(x, mc.thePlayer.posY, z);
         if (this.aim.isToggled()) {
            Utils.aim(en, 0.0F, false);
         }
      }

      private Entity ge() {
         Entity en = null;
         double r = Math.pow(this.range.getInput(), 2.0);
         double dist = r + 1.0;

         for (Entity ent : mc.theWorld.loadedEntityList) {
            if (ent != mc.thePlayer
               && ent instanceof EntityLivingBase
               && ((EntityLivingBase)ent).deathTime == 0
               && (!this.playersOnly.isToggled() || ent instanceof EntityPlayer)
               && !AntiBot.isBot(ent)) {
               double d = mc.thePlayer.getDistanceSqToEntity(ent);
               if (!(d > r) && !(dist < d)) {
                  dist = d;
                  en = ent;
               }
            }
         }

         return en;
      }
   }

   public static class Spin extends Module {
      public SliderSetting rotation;
      public SliderSetting speed;
      private float yaw;

      public Spin() {
         super("Spin", Module.category.fun, 0);
         this.registerSetting(this.rotation = new SliderSetting("Rotation", 360.0, 30.0, 360.0, 1.0));
         this.registerSetting(this.speed = new SliderSetting("Speed", 25.0, 1.0, 60.0, 1.0));
      }

      @Override
      public void onEnable() {
         this.yaw = mc.thePlayer.rotationYaw;
      }

      @Override
      public void onDisable() {
         this.yaw = 0.0F;
      }

      @Override
      public void onUpdate() {
         double left = this.yaw + this.rotation.getInput() - mc.thePlayer.rotationYaw;
         if (left < this.speed.getInput()) {
            EntityPlayerSP var10000 = mc.thePlayer;
            var10000.rotationYaw = (float)(var10000.rotationYaw + left);
            this.disable();
         } else {
            EntityPlayerSP var10000 = mc.thePlayer;
            var10000.rotationYaw = (float)(var10000.rotationYaw + this.speed.getInput());
            if (mc.thePlayer.rotationYaw >= this.yaw + this.rotation.getInput()) {
               this.disable();
            }
         }
      }
   }
}
