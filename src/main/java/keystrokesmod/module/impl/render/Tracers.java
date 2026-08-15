package keystrokesmod.module.impl.render;

import java.awt.Color;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Tracers extends Module {
   public ButtonSetting a;
   public SliderSetting b;
   public SliderSetting c;
   public SliderSetting d;
   public ButtonSetting e;
   public SliderSetting f;
   private boolean g;
   private int rgb_c = 0;

   public Tracers() {
      super("Tracers", Module.category.render, 0);
      this.registerSetting(this.a = new ButtonSetting("Show invis", true));
      this.registerSetting(this.f = new SliderSetting("Line Width", 1.0, 1.0, 5.0, 1.0));
      this.registerSetting(this.b = new SliderSetting("Red", 0.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.c = new SliderSetting("Green", 255.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.d = new SliderSetting("Blue", 0.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.e = new ButtonSetting("Rainbow", false));
   }

   @Override
   public void onEnable() {
      this.g = mc.gameSettings.viewBobbing;
      if (this.g) {
         mc.gameSettings.viewBobbing = false;
      }
   }

   @Override
   public void onDisable() {
      mc.gameSettings.viewBobbing = this.g;
   }

   @Override
   public void onUpdate() {
      if (mc.gameSettings.viewBobbing) {
         mc.gameSettings.viewBobbing = false;
      }
   }

   @Override
   public void guiUpdate() {
      this.rgb_c = new Color((int)this.b.getInput(), (int)this.c.getInput(), (int)this.d.getInput()).getRGB();
   }

   @SubscribeEvent
   public void o(RenderWorldLastEvent ev) {
      if (Utils.nullCheck()) {
         int rgb = this.e.isToggled() ? Utils.getChroma(2L, 0L) : this.rgb_c;
         if (!keystrokesmod.Raven.debug) {
            for (EntityPlayer en : mc.theWorld.playerEntities) {
               if (en != mc.thePlayer && en.deathTime == 0 && (this.a.isToggled() || !en.isInvisible()) && !AntiBot.isBot(en)) {
                  RenderUtils.drawTracerLine(en, rgb, (float)this.f.getInput(), ((IAccessorMinecraft)mc).getTimer().renderPartialTicks);
               }
            }

            return;
         }

         for (Entity en : mc.theWorld.loadedEntityList) {
            if (en instanceof EntityLivingBase && en != mc.thePlayer) {
               RenderUtils.drawTracerLine(en, rgb, (float)this.f.getInput(), ((IAccessorMinecraft)mc).getTimer().renderPartialTicks);
            }
         }
      }
   }
}
