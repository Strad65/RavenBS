package keystrokesmod.event;

import keystrokesmod.script.model.PlayerState;
import net.minecraftforge.fml.common.eventhandler.Event;

public class ClientRotationEvent extends Event {
   public Float yaw;
   public Float pitch;
   private float tYaw;
   private float tPitch;

   public ClientRotationEvent(Float yaw, Float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public float getYaw() {
      return this.tYaw;
   }

   public float getPitch() {
      return this.tPitch;
   }

   public void setYaw(Float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(Float pitch) {
      this.pitch = pitch;
   }

   public void setRotations(Float yaw, Float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public boolean isEquals(PlayerState e) {
      return e.yaw == this.tYaw && e.pitch == this.tPitch;
   }
}
