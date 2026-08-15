package keystrokesmod.utility;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import keystrokesmod.event.PostUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class PacketsHandler {
   public Minecraft mc = Minecraft.getMinecraft();
   public PacketsHandler.PacketData C0A = new PacketsHandler.PacketData();
   public PacketsHandler.PacketData C08 = new PacketsHandler.PacketData();
   public PacketsHandler.PacketData C07 = new PacketsHandler.PacketData();
   public PacketsHandler.PacketData C02 = new PacketsHandler.PacketData();
   public PacketsHandler.PacketData C02_INTERACT_AT = new PacketsHandler.PacketData();
   public PacketsHandler.PacketData C09 = new PacketsHandler.PacketData();
   public AtomicInteger playerSlot = new AtomicInteger(-1);
   public AtomicInteger serverSlot = new AtomicInteger(-1);
   private final boolean handleSlots = true;

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onSendPacket(SendPacketEvent e) {
      if (!e.isCanceled()) {
         Packet<?> packet = e.getPacket();
         if (packet instanceof C02PacketUseEntity) {
            if (this.C07.sentCurrentTick.get()) {
               e.setCanceled(true);
               return;
            }

            if (((C02PacketUseEntity)packet).getAction() == Action.INTERACT_AT) {
               this.C02_INTERACT_AT.sentCurrentTick.set(true);
            }

            this.C02.sentCurrentTick.set(true);
         } else if (packet instanceof C08PacketPlayerBlockPlacement) {
            this.C08.sentCurrentTick.set(true);
         } else if (packet instanceof C07PacketPlayerDigging) {
            this.C07.sentCurrentTick.set(true);
         } else if (packet instanceof C0APacketAnimation) {
            this.C0A.sentCurrentTick.set(true);
         } else if (packet instanceof C09PacketHeldItemChange) {
            C09PacketHeldItemChange slotPacket = (C09PacketHeldItemChange)packet;
            int slotId = slotPacket.getSlotId();
            if (slotId == this.playerSlot.get() && slotId == this.serverSlot.get()) {
               if (keystrokesmod.Raven.debug) {
                  Utils.sendMessage("&7bad packet detected (same slot): &b" + slotId);
               }

               e.setCanceled(true);
               return;
            }

            this.C09.sentCurrentTick.set(true);
            this.playerSlot.set(slotId);
            this.serverSlot.set(slotId);
         }
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (e.getPacket() instanceof S09PacketHeldItemChange) {
         S09PacketHeldItemChange packet = (S09PacketHeldItemChange)e.getPacket();
         int index = packet.getHeldItemHotbarIndex();
         if (index >= 0 && index < InventoryPlayer.getHotbarSize()) {
            this.serverSlot.set(index);
         }
      } else if (e.getPacket() instanceof S0CPacketSpawnPlayer && Minecraft.getMinecraft().thePlayer != null) {
         S0CPacketSpawnPlayer packet = (S0CPacketSpawnPlayer)e.getPacket();
         if (packet.getEntityID() != Minecraft.getMinecraft().thePlayer.getEntityId()) {
            return;
         }

         this.playerSlot.set(-1);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPostUpdate(PostUpdateEvent e) {
      this.C08.updateStatesPostUpdate();
      this.C07.updateStatesPostUpdate();
      this.C02.updateStatesPostUpdate();
      this.C0A.updateStatesPostUpdate();
      this.C02_INTERACT_AT.updateStatesPostUpdate();
      this.C09.updateStatesPostUpdate();
   }

   public void handlePacket(Packet<?> packet) {
      if (packet instanceof C09PacketHeldItemChange) {
         int slotId = ((C09PacketHeldItemChange)packet).getSlotId();
         this.playerSlot.set(slotId);
         this.C09.sentCurrentTick.set(true);
      } else if (packet instanceof C02PacketUseEntity) {
         this.C02.sentCurrentTick.set(true);
         if (((C02PacketUseEntity)packet).getAction() == Action.INTERACT_AT) {
            this.C02_INTERACT_AT.sentCurrentTick.set(true);
         }
      } else if (packet instanceof C07PacketPlayerDigging) {
         this.C07.sentCurrentTick.set(true);
      } else if (packet instanceof C08PacketPlayerBlockPlacement) {
         this.C08.sentCurrentTick.set(true);
      } else if (packet instanceof C0APacketAnimation) {
         this.C0A.sentCurrentTick.set(true);
      }
   }

   public boolean sent() {
      return this.C02.sentCurrentTick.get()
         || this.C08.sentCurrentTick.get()
         || this.C09.sentCurrentTick.get()
         || this.C07.sentCurrentTick.get()
         || this.C0A.sentCurrentTick.get();
   }

   public boolean updateSlot(int slot) {
      if (this.playerSlot.get() != slot && slot != -1) {
         this.mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(slot));
         this.playerSlot.set(slot);
         return true;
      } else {
         return false;
      }
   }

   public static class PacketData {
      public AtomicBoolean sentLastTick = new AtomicBoolean(false);
      public AtomicBoolean sentCurrentTick = new AtomicBoolean(false);

      public void updateStatesPostUpdate() {
         this.sentLastTick.set(this.sentCurrentTick.get());
         this.sentCurrentTick.set(false);
      }
   }
}
