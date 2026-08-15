package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.Packet;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;

public class FakeLag extends Module {
   public SliderSetting packetDelaySlider;
   private ConcurrentSkipListMap<Long, List<Packet<?>>> packetQueue = new ConcurrentSkipListMap<>();
   private Timer timer;
   private long packetDelay;

   public FakeLag() {
      super("Fake Lag", Module.category.player, 0);
      this.registerSetting(this.packetDelaySlider = new SliderSetting("Packet delay", "ms", 0.0, 0.0, 1500.0, 20.0));
   }

   @Override
   public String getInfo() {
      return this.packetDelay + "ms";
   }

   @Override
   public void guiUpdate() {
      if (this.packetDelay != this.packetDelaySlider.getInput()) {
         if (this.isEnabled()) {
            this.onDisable();
         }

         this.packetDelay = (int)this.packetDelaySlider.getInput();
      }
   }

   @Override
   public void onEnable() {
      if (mc.isSingleplayer()) {
         Utils.sendMessage("&cFake lag cannot be enabled in singleplayer.");
         this.disable();
      } else if (ModuleManager.blink.isEnabled()) {
         Utils.sendMessage("&cCannot use fake lag with blink!");
         this.disable();
      } else {
         (this.timer = new Timer()).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
               FakeLag.this.updatePacketQueue(false);
            }
         }, 0L, 10L);
      }
   }

   @Override
   public void onDisable() {
      if (this.timer != null) {
         this.timer.cancel();
         this.timer.purge();
         this.timer = null;
      }

      this.updatePacketQueue(true);
   }

   private void updatePacketQueue(boolean flush) {
      if (!this.packetQueue.isEmpty()) {
         if (flush) {
            for (Entry<Long, List<Packet<?>>> entry : this.packetQueue.entrySet()) {
               for (Packet packet : entry.getValue()) {
                  PacketUtils.sendPacketNoEvent(packet);
               }
            }

            this.packetQueue.clear();
         } else {
            long now = System.currentTimeMillis();
            Iterator<Entry<Long, List<Packet<?>>>> it = this.packetQueue.entrySet().iterator();

            while (it.hasNext()) {
               Entry<Long, List<Packet<?>>> entry2 = it.next();
               if (now < entry2.getKey()) {
                  break;
               }

               for (Packet packet2 : entry2.getValue()) {
                  PacketUtils.sendPacketNoEvent(packet2);
               }

               it.remove();
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPacketSent(SendPacketEvent e) {
      if (Utils.nullCheck() && !mc.isSingleplayer() && (int)this.packetDelaySlider.getInput() != 0 && !e.isCanceled()) {
         long time = System.currentTimeMillis() + (int)this.packetDelaySlider.getInput();
         List<Packet<?>> packetList = this.packetQueue.get(time);
         if (packetList == null) {
            packetList = new ArrayList<>();
         }

         packetList.add(e.getPacket());
         this.packetQueue.put(time, packetList);
         e.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent e) {
      if (mc.theWorld == null) {
         this.packetQueue.clear();
         this.disable();
      }
   }
}
