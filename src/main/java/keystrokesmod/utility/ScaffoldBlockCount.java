package keystrokesmod.utility;

import java.awt.Color;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.shader.BlurUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class ScaffoldBlockCount {
   private final Minecraft mc;
   public static Timer fadeTimer;
   public static Timer fadeInTimer;
   private float previousAlpha;
   private static final int PAD = 3;

   public ScaffoldBlockCount(Minecraft mc) {
      this.mc = mc;
      fadeTimer = null;
      (fadeInTimer = new Timer(150.0F)).start();
   }

   /** Build the display string for the given block count. */
   public static String buildText(int blocks) {
      String color;
      if (blocks <= 5) {
         color = "§c";
      } else if (blocks <= 15) {
         color = "§6";
      } else if (blocks <= 25) {
         color = "§e";
      } else {
         color = "";
      }
      return color + blocks + " §rblock" + (blocks == 1 ? "" : "s");
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (this.previousAlpha <= 10.0F && fadeInTimer == null) {
         this.onDisable();
      } else if (Utils.nullCheck() && ModuleManager.scaffold.showBlockCount.isToggled()) {
         if (ev.phase == Phase.END) {
            if (this.mc.currentScreen != null) {
               return;
            }

            ScaledResolution scaledResolution = new ScaledResolution(this.mc);
            int blocks = ModuleManager.scaffold.totalBlocks();

            float alpha = fadeTimer == null ? 255.0F : 255 - fadeTimer.getValueInt(0, 255, 1);
            if (fadeInTimer != null) {
               alpha = fadeInTimer.getValueFloat(10.0F, 255.0F, 1);
               if (alpha == 255.0F) {
                  fadeInTimer = null;
               }
            }

            this.previousAlpha = alpha;
            this.drawBlockCountHUD(scaledResolution, blocks, alpha,
                  ModuleManager.scaffold.blockCountPosX,
                  ModuleManager.scaffold.blockCountPosY);
         }
      }
   }

   /**
    * Renders the block-count HUD at the given screen position offset.
    * Called both from onRenderTick and from Scaffold's BlockCountEditScreen preview.
    */
   public void drawBlockCountHUD(ScaledResolution res, int blocks, float alpha, int offsetX, int offsetY) {
      String text = buildText(blocks);
      int x = res.getScaledWidth() / 2 + 8 + offsetX;
      int y = res.getScaledHeight() / 2 + 4 + offsetY;
      int textWidth = this.mc.fontRendererObj.getStringWidth(text);
      int bgX1 = x - PAD;
      int bgY1 = y - PAD;
      int bgX2 = x + textWidth + PAD;
      int bgY2 = y + this.mc.fontRendererObj.FONT_HEIGHT + PAD;

      // Bloom/shadow effect
      boolean bloomEnabled = ModuleManager.scaffold.blockCountBloomToggle.isToggled();
      float bloomRadius = bloomEnabled ? (float) ModuleManager.scaffold.blockCountBloom.getInput() : 0.0f;
      int bloomAlpha = (int)(alpha * 0.43f); // Similar to TargetInfo's maxAlphaOutline logic
      if (bloomRadius > 0.0f) {
         BlurUtils.bloomRect(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 3, bloomRadius,
                             new Color(0, 0, 0, bloomAlpha));
      }

      // Blur background
      boolean blurEnabled = ModuleManager.scaffold.blockCountBlurToggle.isToggled();
      float blurRadius = blurEnabled ? (float) ModuleManager.scaffold.blockCountBlur.getInput() : 0.0f;
      int bgAlpha = (int)(alpha * 0.78f);
      if (blurRadius > 0.0f) {
         // Mask the blurred game scene to this rect (sharp corners)
         BlurUtils.blurRect(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 2, blurRadius);
      } else {
         // No blur — draw a plain translucent rect
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         Gui.drawRect(bgX1, bgY1, bgX2, bgY2, new Color(0, 0, 0, bgAlpha).getRGB());
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }

      // Text
      int colorAlpha = Utils.mergeAlpha(-1, (int)alpha);
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      this.mc.fontRendererObj.drawStringWithShadow(text, x, y, colorAlpha);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   public void beginFade() {
      (fadeTimer = new Timer(150.0F)).start();
      fadeInTimer = null;
   }

   public void onDisable() {
      fadeInTimer = null;
      fadeTimer = null;
      FMLCommonHandler.instance().bus().unregister(this);
   }
}
