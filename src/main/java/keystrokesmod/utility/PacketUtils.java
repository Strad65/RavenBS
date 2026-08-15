package keystrokesmod.utility;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Packet;

public class PacketUtils {
   public static List<Packet> skipSendEvent = new ArrayList<>();
   public static List<Packet> skipReceiveEvent = new ArrayList<>();

   public static void sendPacketNoEvent(Packet packet) {
      if (packet != null && !packet.getClass().getSimpleName().startsWith("S")) {
         skipSendEvent.add(packet);
         keystrokesmod.Raven.mc.thePlayer.sendQueue.addToSendQueue(packet);
      }
   }

   public static void receivePacketNoEvent(Packet packet) {
      try {
         skipReceiveEvent.add(packet);
         packet.processPacket(keystrokesmod.Raven.mc.getNetHandler());
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}
