package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorEntityLivingBase;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class DelayRemover extends Module {
   public ButtonSetting oldReg;
   public ButtonSetting removeJumpTicks;
   public ButtonSetting whileMoving;

   public DelayRemover() {
      super("Delay Remover", Module.category.player, 0);
      this.registerSetting(this.oldReg = new ButtonSetting("1.7 hitreg", true));
      this.registerSetting(this.removeJumpTicks = new ButtonSetting("Remove jump ticks", false));
      this.registerSetting(this.whileMoving = new ButtonSetting(" ^ only while moving", false));
      this.closetModule = true;
   }

   @Override
   public void guiUpdate() {
      this.whileMoving.setVisible(this.removeJumpTicks.isToggled(), this);
   }

   @SubscribeEvent
   public void onTick(PlayerTickEvent e) {
      if (e.phase == Phase.END && mc.inGameHasFocus && Utils.nullCheck()) {
         if (this.oldReg.isToggled()) {
            ((IAccessorMinecraft)mc).setLeftClickCounter(0);
         }

         if (this.removeJumpTicks.isToggled()) {
            if (this.whileMoving.isToggled() && mc.thePlayer.motionX == 0.0 && mc.thePlayer.motionZ == 0.0) {
               return;
            }

            ((IAccessorEntityLivingBase)mc.thePlayer).setJumpTicks(0);
         }
      }
   }
}
