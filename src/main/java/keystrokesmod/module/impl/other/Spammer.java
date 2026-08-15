package keystrokesmod.module.impl.other;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Spammer extends Module {
   private ButtonSetting spamBypass;
   private SliderSetting delay;
   public String message = "";
   private String bypass;
   private int delayTicks;
   private int uses;

   public Spammer() {
      super("Spammer", Module.category.other);
      this.registerSetting(this.delay = new SliderSetting("Delay", "s", 3.0, 0.5, 30.0, 0.5));
      this.registerSetting(this.spamBypass = new ButtonSetting("Spam bypass", false));
   }

   @Override
   public void onEnable() {
      this.reset();
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.uses == 0) {
         this.bypass = "15L2QVJSHU95DPQZX3";
      } else if (this.uses == 1) {
         this.bypass = "LO4MDH1798JX5YQ935";
      } else if (this.uses == 2) {
         this.bypass = "987YQM6ND5AG01LFU3";
      } else if (this.uses == 3) {
         this.bypass = "83NMQJX5HQID83L32G";
      } else if (this.uses == 4) {
         this.bypass = "PL256GHBTNZQ38HJFM";
      } else if (this.uses == 5) {
         this.bypass = "LMP8B4GZ96BHMDU328";
      } else if (this.uses >= 6) {
         this.bypass = "OPF3HJ2K3J167YGUQW";
         this.uses = 0;
      }

      if (!this.spamBypass.isToggled()) {
         this.bypass = "";
      }

      if (!this.message.isEmpty()) {
         this.delayTicks++;
         if (this.delayTicks >= this.delay.getInput() * 20.0) {
            mc.thePlayer.sendChatMessage(this.message + " " + this.bypass);
            this.delayTicks = 0;
            this.uses++;
         }
      }
   }

   public void reset() {
      this.delayTicks = 0;
   }
}
