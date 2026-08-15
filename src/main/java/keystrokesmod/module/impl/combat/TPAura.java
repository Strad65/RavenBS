package keystrokesmod.module.impl.combat;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TPAura extends Module {
   private SliderSetting range;
   private ButtonSetting weaponOnly;
   private double x = 0.0;
   private double z = 0.0;
   private double y = 0.0;

   public TPAura() {
      super("TPAura", Module.category.combat);
      this.registerSetting(this.range = new SliderSetting("Range", 0.0, 0.0, 50.0, 1.0));
      this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
   }

   @SubscribeEvent
   public void onLivingUpdate(LivingUpdateEvent e) {
      if (Utils.nullCheck() && mc.thePlayer.maxHurtTime > 0 && mc.thePlayer.hurtTime == mc.thePlayer.maxHurtTime) {
         this.updatePosition();
      }
   }

   private void updatePosition() {
      this.x = Utils.randomizeInt(-15, 15) / 10.0;
      this.y = Utils.randomizeInt(10, 15) / 10.0;
      this.z = Utils.randomizeInt(-15, 15) / 10.0;
   }

   @Override
   public void onEnable() {
      if (this.range.getInput() == 0.0) {
         Utils.sendMessage("&cTPAura range values are set to 0.");
         this.disable();
      } else {
         this.updatePosition();
      }
   }

   @Override
   public void onUpdate() {
      if (!this.weaponOnly.isToggled() || Utils.holdingWeapon()) {
         double rangeSq = this.range.getInput() * this.range.getInput();

         for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
            if (entityPlayer != mc.thePlayer
               && entityPlayer.deathTime == 0
               && !(mc.thePlayer.getDistanceSqToEntity(entityPlayer) > rangeSq)
               && !AntiBot.isBot(entityPlayer)
               && !Utils.isFriended(entityPlayer)) {
               mc.thePlayer.setPosition(entityPlayer.posX + this.x, entityPlayer.posY + this.y, entityPlayer.posZ + this.z);
               Utils.attackEntity(entityPlayer, true, false);
               break;
            }
         }
      }
   }
}
