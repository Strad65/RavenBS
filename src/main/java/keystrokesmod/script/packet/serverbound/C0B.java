package keystrokesmod.script.packet.serverbound;

import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public class C0B extends CPacket {
   public String action;
   public int horsePower;

   public C0B(String action, int horsePower) {
      super(null);
      this.action = action;
      this.horsePower = horsePower;
   }

   public C0B(C0BPacketEntityAction packet) {
      super(packet);
      this.action = packet.getAction().name();
      this.horsePower = packet.getAuxData();
   }

   public C0BPacketEntityAction convert() {
      return new C0BPacketEntityAction(Minecraft.getMinecraft().thePlayer, Utils.getEnum(Action.class, this.action), this.horsePower);
   }
}
