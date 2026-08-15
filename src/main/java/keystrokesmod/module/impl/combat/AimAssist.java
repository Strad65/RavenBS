package keystrokesmod.module.impl.combat;

import keystrokesmod.event.ClientLookEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.helper.RotationHelper;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AimAssist extends Module {
   private SliderSetting mode;
   private SliderSetting speed;
   private SliderSetting fov;
   private SliderSetting distance;
   private ButtonSetting clickAim;
   private ButtonSetting weaponOnly;
   private ButtonSetting disableWhileMining;
   private ButtonSetting aimInvis;
   private ButtonSetting ignoreTeammates;
   private String[] aimModes = new String[]{"Normal", "Silent"};
   private Float[] lookingAt = null;

   public AimAssist() {
      super("AimAssist", Module.category.combat, 0);
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.aimModes));
      this.registerSetting(this.speed = new SliderSetting("Speed", 45.0, 1.0, 100.0, 1.0));
      this.registerSetting(this.fov = new SliderSetting("FOV", 90.0, 15.0, 360.0, 1.0));
      this.registerSetting(this.distance = new SliderSetting("Distance", 4.5, 1.0, 10.0, 0.5));
      this.registerSetting(this.clickAim = new ButtonSetting("Click aim", true));
      this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
      this.registerSetting(this.disableWhileMining = new ButtonSetting("Disable while mining", false));
      this.registerSetting(this.aimInvis = new ButtonSetting("Aim invis", false));
      this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", false));
   }

   @Override
   public String getInfo() {
      return this.aimModes[(int)this.mode.getInput()];
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      this.lookingAt = null;
      if (this.mode.getInput() != 0.0 && this.conditionsMet()) {
         Entity en = this.getEnemy();
         if (en != null) {
            if (this.speed.getInput() == 100.0) {
               float[] rotations = RotationUtils.getRotations(en);
               if (rotations != null) {
                  float yaw = rotations[0];
                  float pitch = MathHelper.clamp_float(rotations[1], -90.0F, 90.0F);
                  RotationHelper.get().setRotations(yaw, pitch);
                  this.lookingAt = new Float[]{yaw, pitch};
               }
            } else {
               double diff = Utils.aimDifference(en, this.mode.getInput() == 1.0);
               float val = (float)(-(diff / (101.0 - this.speed.getInput()))) * 1.2F;
               float[] rots = RotationUtils.serverRotations;
               float yaw = rots[0] + val;
               RotationHelper.get().setYaw(yaw);
               this.lookingAt = new Float[]{yaw};
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.mode.getInput() != 1.0 && this.conditionsMet()) {
         Entity en = this.getEnemy();
         if (en != null) {
            if (this.speed.getInput() == 100.0) {
               float[] t = Utils.getRotationsOld(en);
               if (t != null) {
                  float y = t[0];
                  float p = t[1];
                  mc.thePlayer.rotationYaw = y;
                  mc.thePlayer.rotationPitch = p;
               }
            } else {
               double n = Utils.aimDifference(en, false);
               if (n > 1.0 || n < -1.0) {
                  float val = (float)(-(n / (101.0 - this.speed.getInput())));
                  mc.thePlayer.rotationYaw += val;
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onClientLook(ClientLookEvent e) {
      if (this.lookingAt != null) {
         if (this.lookingAt.length == 2 && this.lookingAt[1] != null) {
            e.pitch = this.lookingAt[1];
         }

         if (this.lookingAt[0] != null) {
            e.yaw = this.lookingAt[0];
         }
      }
   }

   private Entity getEnemy() {
      int n = (int)this.fov.getInput();

      for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
         if (entityPlayer != mc.thePlayer
            && entityPlayer.deathTime == 0
            && !Utils.isFriended(entityPlayer)
            && (!this.ignoreTeammates.isToggled() || !Utils.isTeammate(entityPlayer))
            && (this.aimInvis.isToggled() || !entityPlayer.isInvisible())
            && !(mc.thePlayer.getDistanceToEntity(entityPlayer) > this.distance.getInput())
            && !AntiBot.isBot(entityPlayer)
            && (n == 360 || Utils.inFov(n, entityPlayer))) {
            return entityPlayer;
         }
      }

      return null;
   }

   private boolean conditionsMet() {
      if (mc.currentScreen != null || !mc.inGameHasFocus) {
         return false;
      } else if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
         return false;
      } else {
         return this.clickAim.isToggled() && !Utils.isClicking() ? false : !this.disableWhileMining.isToggled() || !this.isMining();
      }
   }

   private boolean isMining() {
      return Mouse.isButtonDown(0)
         && mc.objectMouseOver != null
         && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
         && mc.objectMouseOver.getBlockPos() != null;
   }
}
