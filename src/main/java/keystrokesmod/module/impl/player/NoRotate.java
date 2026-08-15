package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class NoRotate extends Module {
   private float prevPitch = 0.0F;
   private float prevYaw = 0.0F;

   public NoRotate() {
      super("NoRotate", Module.category.player);
   }

   public void handlePlayerPosLookPre() {
      if (this.isEnabled()) {
         if (mc.thePlayer != null && mc.thePlayer.rotationPitch != 0.0F) {
            this.prevPitch = mc.thePlayer.rotationPitch % 360.0F;
            this.prevYaw = mc.thePlayer.rotationYaw % 360.0F;
         }
      }
   }

   public void handlePlayerPosLook(S08PacketPlayerPosLook packet) {
      if (this.isEnabled()) {
         if (packet.getPitch() != 0.0F && mc.thePlayer != null) {
            mc.thePlayer.prevRotationYaw = this.prevYaw;
            mc.thePlayer.prevRotationPitch = this.prevPitch;
            mc.thePlayer.rotationPitch = this.prevPitch;
            mc.thePlayer.rotationYaw = this.prevYaw;
         }
      }
   }
}
