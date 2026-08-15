package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class AutoJump extends Module {
   public ButtonSetting cancelSneaking;
   private boolean isJumping = false;

   public AutoJump() {
      super("AutoJump", Module.category.player);
      this.registerSetting(this.cancelSneaking = new ButtonSetting("Cancel when sneaking", true));
   }

   @Override
   public void onDisable() {
      this.setJump(this.isJumping = false);
   }

   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent e) {
      if (Utils.nullCheck()) {
         if (mc.thePlayer.onGround && (!this.cancelSneaking.isToggled() || !mc.thePlayer.isSneaking())) {
            if (Utils.onEdge()) {
               this.setJump(this.isJumping = true);
            } else if (this.isJumping) {
               this.setJump(this.isJumping = false);
            }
         } else if (this.isJumping) {
            this.setJump(this.isJumping = false);
         }
      }
   }

   private void setJump(boolean jumping) {
      KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), jumping);
   }
}
