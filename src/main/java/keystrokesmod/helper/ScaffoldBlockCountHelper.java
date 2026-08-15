package keystrokesmod.helper;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class ScaffoldBlockCountHelper {
   private final Minecraft mc;
   private Timer fadeTimer;
   private Timer fadeInTimer;
   private float previousAlpha;

   public ScaffoldBlockCountHelper(Minecraft mc) {
      this.mc = mc;
      this.fadeTimer = null;
      (this.fadeInTimer = new Timer(150.0F)).start();
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (this.previousAlpha <= 10.0F && this.fadeInTimer == null) {
         this.onDisable();
      } else if (Utils.nullCheck() && ModuleManager.scaffold.showBlockCount.isToggled()) {
         if (ev.phase == Phase.END) {
            if (this.mc.currentScreen != null) {
               return;
            }

            ScaledResolution scaledResolution = new ScaledResolution(this.mc);
            int blocks = ModuleManager.scaffold.totalBlocks();
            String color = "§";
            if (blocks <= 5) {
               color = color + "c";
            } else if (blocks <= 15) {
               color = color + "6";
            } else if (blocks <= 25) {
               color = color + "e";
            } else {
               color = "";
            }

            float alpha = this.fadeTimer == null ? 255.0F : 255 - this.fadeTimer.getValueInt(0, 255, 1);
            if (this.fadeInTimer != null) {
               alpha = this.fadeInTimer.getValueFloat(10.0F, 255.0F, 1);
               if (alpha == 255.0F) {
                  this.fadeInTimer = null;
               }
            }

            this.previousAlpha = alpha;
            int colorAlpha = Utils.mergeAlpha(-1, (int)this.previousAlpha);
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            this.mc
               .fontRendererObj
               .drawStringWithShadow(
                  color + blocks + " §rblock" + (blocks == 1 ? "" : "s"),
                  scaledResolution.getScaledWidth() / 2 + 8,
                  scaledResolution.getScaledHeight() / 2 + 4,
                  colorAlpha
               );
            GL11.glDisable(3042);
            GL11.glPopMatrix();
         }
      }
   }

   public void beginFade() {
      (this.fadeTimer = new Timer(150.0F)).start();
      this.fadeInTimer = null;
   }

   public void onDisable() {
      MinecraftForge.EVENT_BUS.unregister(this);
      this.fadeInTimer = null;
      this.fadeTimer = null;
   }
}
