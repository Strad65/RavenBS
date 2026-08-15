package keystrokesmod.module.impl.other;

import java.util.List;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LatencyAlerts extends Module {
   private SliderSetting interval;
   private SliderSetting highLatency;
   private ButtonSetting ignoreLimbo;
   private long lastPacket;
   private long lastAlert;

   public LatencyAlerts() {
      super("Latency Alerts", Module.category.other);
      this.registerSetting(new DescriptionSetting("Detects packet loss."));
      this.registerSetting(this.interval = new SliderSetting("Alert interval", " second", 3.0, 0.0, 5.0, 0.1));
      this.registerSetting(this.highLatency = new SliderSetting("High latency", " second", 0.5, 0.1, 5.0, 0.1));
      this.registerSetting(this.ignoreLimbo = new ButtonSetting("Ignore limbo", true));
      this.closetModule = true;
   }

   @SubscribeEvent
   public void onPacketReceive(ReceivePacketEvent e) {
      this.lastPacket = System.currentTimeMillis();
   }

   @Override
   public void onUpdate() {
      if (!mc.isSingleplayer() && (!this.ignoreLimbo.isToggled() || !this.inLimbo())) {
         long currentMs = System.currentTimeMillis();
         if (currentMs - this.lastPacket >= this.highLatency.getInput() * 1000.0 && currentMs - this.lastAlert >= this.interval.getInput() * 1000.0) {
            Utils.sendMessage("&7Packet loss detected: §c" + Math.abs(System.currentTimeMillis() - this.lastPacket) + "&7ms");
            this.lastAlert = System.currentTimeMillis();
         }
      } else {
         this.lastPacket = System.currentTimeMillis();
         this.lastAlert = System.currentTimeMillis();
      }
   }

   @Override
   public void onDisable() {
      this.lastPacket = 0L;
      this.lastAlert = 0L;
   }

   @Override
   public void onEnable() {
      this.lastPacket = System.currentTimeMillis();
   }

   public boolean inLimbo() {
      List<String> scoreboard = Utils.getSidebarLines();
      return (scoreboard == null || scoreboard.isEmpty()) && mc.theWorld.provider.getDimensionName().equals("The End");
   }
}
