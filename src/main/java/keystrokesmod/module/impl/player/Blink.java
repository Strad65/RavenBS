package keystrokesmod.module.impl.player;

import java.awt.Color;
import keystrokesmod.event.SendAllPacketsEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlinkHandler;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Blink extends Module {
   private static SliderSetting inf;
   private static SliderSetting maximumBlinkTicks;
   public ButtonSetting renderTimer;
   public ButtonSetting initialPosition;
   private ButtonSetting disableOnBreak;
   private ButtonSetting disableOnAttack;
   private int color = new Color(0, 187, 255, 255).getRGB();
   public boolean blink;

   public Blink() {
      super("Blink", Module.category.player);
      this.registerSetting(maximumBlinkTicks = new SliderSetting("Maximum duration", "s", 0.0, 0.0, 10.0, 0.1));
      this.registerSetting(this.initialPosition = new ButtonSetting("Show initial position", true));
      this.registerSetting(this.renderTimer = new ButtonSetting("Render Timer", false));
      this.registerSetting(this.disableOnBreak = new ButtonSetting("Disable on Break", false));
      this.registerSetting(this.disableOnAttack = new ButtonSetting("Disable on Attack", false));
   }

   @Override
   public void onDisable() {
      this.blink = false;
   }

   @SubscribeEvent
   public void onSendPacketAll(SendAllPacketsEvent e) {
      if (this.disableOnBreak.isToggled() && (Utils.usingBedAura() || ModuleUtils.isBreaking)) {
         this.disable();
      }

      if (this.disableOnAttack.isToggled() && e.getPacket() instanceof C02PacketUseEntity) {
         this.disable();
      }

      if (maximumBlinkTicks.getInput() != 0.0 && BlinkHandler.blinkTicks > maximumBlinkTicks.getInput() * 20.0) {
         this.disable();
      }

      this.blink = true;
   }

   @Override
   public String getInfo() {
      return BlinkHandler.blinkTicks + "";
   }
}
