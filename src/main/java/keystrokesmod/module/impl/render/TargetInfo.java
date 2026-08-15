package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.io.IOException;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class TargetInfo extends Module {
   private SliderSetting mode;
   private SliderSetting theme;
   private ButtonSetting renderHUD;
   private ButtonSetting renderESP;
   private SliderSetting espMode;
   private ButtonSetting showStatus;
   private ButtonSetting healthColor;
   private Timer fadeTimer;
   private Timer healthBarTimer = null;
   private EntityLivingBase target;
   private long lastAliveMS;
   private double lastHealth;
   private float lastHealthBar;
   public int posX = 70;
   public int posY = 30;
   private String[] modes = new String[]{"Modern", "Legacy"};
   private String[] espModes = new String[]{"Theme", "Team", "Hurttime"};
   private SliderSetting min;

   public TargetInfo() {
      super("TargetInfo", Module.category.render);
      this.registerSetting(new DescriptionSetting("Only works with KillAura."));
      this.registerSetting(this.renderHUD = new ButtonSetting("Render HUD", true));
      this.registerSetting(this.mode = new SliderSetting("Mode", 1, this.modes));
      this.registerSetting(this.theme = new SliderSetting("Theme", 0, Theme.themes));
      this.registerSetting(new ButtonSetting("Edit position", () -> mc.displayGuiScreen(new TargetInfo.EditScreen())));
      this.registerSetting(this.renderESP = new ButtonSetting("Render ESP", true));
      this.registerSetting(this.espMode = new SliderSetting("ESP mode", 1, this.espModes));
      this.registerSetting(this.min = new SliderSetting("Minimum hurttime", 1.0, 1.0, 9.0, 1.0));
      this.registerSetting(this.showStatus = new ButtonSetting("Show win or loss", true));
      this.registerSetting(this.healthColor = new ButtonSetting("Traditional health color", false));
   }

   @Override
   public void guiUpdate() {
      this.min.setVisible(this.espMode.getInput() == 2.0, this);
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (!Utils.nullCheck()) {
         this.reset();
      } else {
         if (ev.phase == Phase.END) {
            if (mc.currentScreen != null) {
               this.reset();
               return;
            }

            if (KillAura.attackingEntity != null) {
               this.target = KillAura.attackingEntity;
               this.lastAliveMS = System.currentTimeMillis();
               this.fadeTimer = null;
            } else {
               if (this.target == null) {
                  return;
               }

               if (System.currentTimeMillis() - this.lastAliveMS >= 400L && this.fadeTimer == null) {
                  (this.fadeTimer = new Timer(400.0F)).start();
               }
            }

            String playerInfo = this.target.getDisplayName().getFormattedText();
            double health = this.target.getHealth() / this.target.getMaxHealth();
            if (this.target.isDead) {
               health = 0.0;
            }

            if (health != this.lastHealth) {
               (this.healthBarTimer = new Timer(this.mode.getInput() == 0.0 ? 500.0F : 350.0F)).start();
            }

            this.lastHealth = health;
            playerInfo = playerInfo + " " + Utils.getHealthStr(this.target, true);
            if (this.renderHUD.isToggled()) {
               this.drawTargetHUD(this.fadeTimer, playerInfo, health);
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderWorld(RenderWorldLastEvent renderWorldLastEvent) {
      if (this.renderESP.isToggled() && Utils.nullCheck()) {
         if (KillAura.target != null) {
            RenderUtils.renderEntity(KillAura.target, 2, 0.0, 0.0, this.returnColor(), false);
         }
      }
   }

   private int returnColor() {
      if (this.espMode.getInput() == 0.0) {
         return Theme.getGradient((int)this.theme.getInput(), 0.0);
      } else if (this.espMode.getInput() == 1.0) {
         return Utils.mergeAlpha(Utils.getColorFromEntity(KillAura.target), 255);
      } else if (this.espMode.getInput() == 2.0) {
         int dc = new Color(251, 86, 86, 255).getRGB();
         int nc = new Color(107, 255, 103, 255).getRGB();
         return Utils.getHurttime(KillAura.target) >= this.min.getInput() ? dc : nc;
      } else {
         return 0;
      }
   }

   private void drawTargetHUD(Timer fadeTimer, String string, double health) {
      if (this.showStatus.isToggled()) {
         string = string + " " + (health <= Utils.getTotalHealth(mc.thePlayer) / mc.thePlayer.getMaxHealth() ? "§aW" : "§cL");
      }

      ScaledResolution scaledResolution = new ScaledResolution(mc);
      int padding = 8;
      int targetStrWithPadding = mc.fontRendererObj.getStringWidth(string) + 8;
      int x = scaledResolution.getScaledWidth() / 2 - targetStrWithPadding / 2 + this.posX;
      int y = scaledResolution.getScaledHeight() / 2 + 15 + this.posY;
      int n6 = x - 8;
      int n7 = y - 8;
      int n8 = x + targetStrWithPadding;
      int n9 = y + mc.fontRendererObj.FONT_HEIGHT + 5 - 6 + 8;
      int alpha = fadeTimer == null ? 255 : 255 - fadeTimer.getValueInt(0, 255, 1);
      if (alpha > 0) {
         int maxAlphaOutline = alpha > 110 ? 110 : alpha;
         int maxAlphaBackground = alpha > 210 ? 210 : alpha;
         int[] gradientColors = Theme.getGradients((int)this.theme.getInput());
         switch ((int)this.mode.getInput()) {
            case 0:
               float bloomRadius = fadeTimer == null ? 2.0F : 2.0F * alpha / 255.0F;
               float blurRadius = fadeTimer == null ? 3.0F : 3.0F * alpha / 255.0F;
               BlurUtils.prepareBloom();
               RoundedUtils.drawRound(n6, n7, Math.abs((float)n6 - n8), Math.abs((float)n7 - (n9 + 13)), 8.0F, true, new Color(0, 0, 0, maxAlphaBackground));
               BlurUtils.bloomEnd(3, bloomRadius);
               BlurUtils.prepareBlur();
               RoundedUtils.drawRound(
                  n6,
                  n7,
                  Math.abs((float)n6 - n8),
                  Math.abs((float)n7 - (n9 + 13)),
                  8.0F,
                  true,
                  new Color(Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline))
               );
               BlurUtils.blurEnd(2, blurRadius);
               break;
            case 1:
               RenderUtils.drawRoundedGradientOutlinedRectangle(
                  n6,
                  n7,
                  n8,
                  n9 + 13,
                  10.0F,
                  Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline),
                  Utils.mergeAlpha(gradientColors[0], alpha),
                  Utils.mergeAlpha(gradientColors[1], alpha)
               );
         }

         int n13 = n6 + 6;
         int n14 = n8 - 6;
         int n15 = n9;
         RenderUtils.drawRoundedRectangle(n13, n15, n14, n15 + 5, 4.0F, Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline));
         int mergedGradientLeft = Utils.mergeAlpha(gradientColors[0], maxAlphaBackground);
         int mergedGradientRight = Utils.mergeAlpha(gradientColors[1], maxAlphaBackground);
         float healthBar = (int)(n14 + (n13 - n14) * (1.0 - health));
         boolean smoothBack = false;
         if (healthBar != this.lastHealthBar && this.lastHealthBar - n13 >= 3.0F && this.healthBarTimer != null) {
            int type = this.mode.getInput() == 0.0 ? 4 : 1;
            float diff = this.lastHealthBar - healthBar;
            if (diff > 0.0F) {
               this.lastHealthBar = this.lastHealthBar - this.healthBarTimer.getValueFloat(0.0F, diff, type);
            } else {
               smoothBack = true;
               this.lastHealthBar = this.healthBarTimer.getValueFloat(this.lastHealthBar, healthBar, type);
            }
         } else {
            this.lastHealthBar = healthBar;
         }

         if (this.healthColor.isToggled()) {
            mergedGradientLeft = mergedGradientRight = Utils.mergeAlpha(Utils.getColorForHealth(health), maxAlphaBackground);
         }

         if (this.lastHealthBar > n14) {
            this.lastHealthBar = n14;
         }

         switch ((int)this.mode.getInput()) {
            case 0:
               RenderUtils.drawRoundedRectangle(n13, n15, this.lastHealthBar, n15 + 5, 4.0F, Utils.darkenColor(mergedGradientRight, 25.0));
               RenderUtils.drawRoundedGradientRect(
                  n13,
                  n15,
                  smoothBack ? this.lastHealthBar : healthBar,
                  n15 + 5,
                  4.0F,
                  mergedGradientLeft,
                  mergedGradientLeft,
                  mergedGradientRight,
                  mergedGradientRight
               );
               break;
            case 1:
               RenderUtils.drawRoundedGradientRect(
                  n13, n15, this.lastHealthBar, n15 + 5, 4.0F, mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight
               );
         }

         GL11.glPushMatrix();
         GL11.glEnable(3042);
         mc.fontRendererObj.drawString(string, x, y, new Color(220, 220, 220, 255).getRGB() & 16777215 | Utils.clamp(alpha + 15) << 24, true);
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      } else {
         this.target = null;
         this.healthBarTimer = null;
      }
   }

   private void reset() {
      this.fadeTimer = null;
      this.target = null;
      this.healthBarTimer = null;
   }

   class EditScreen extends GuiScreen {
      GuiButtonExt resetPosition;
      boolean d = false;
      int miX = 0;
      int miY = 0;
      int maX = 0;
      int maY = 0;
      int aX = 70;
      int aY = 30;
      int laX = 0;
      int laY = 0;
      int lmX = 0;
      int lmY = 0;
      int clickMinX = 0;

      public void initGui() {
         super.initGui();
         this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
         this.aX = TargetInfo.this.posX;
         this.aY = TargetInfo.this.posY;
      }

      public void drawScreen(int mX, int mY, float pt) {
         ScaledResolution res = new ScaledResolution(this.mc);
         drawRect(0, 0, this.width, this.height, -1308622848);
         int miX = this.aX;
         int miY = this.aY;
         String playerInfo = this.mc.thePlayer.getDisplayName().getFormattedText();
         double health = this.mc.thePlayer.getHealth() / this.mc.thePlayer.getMaxHealth();
         if (this.mc.thePlayer.isDead) {
            health = 0.0;
         }

         TargetInfo.this.lastHealth = health;
         playerInfo = playerInfo + " " + Utils.getHealthStr(this.mc.thePlayer, true);
         TargetInfo.this.drawTargetHUD(null, playerInfo, health);
         if (TargetInfo.this.showStatus.isToggled()) {
            playerInfo = playerInfo
               + " "
               + (health <= Utils.getTotalHealth(this.mc.thePlayer) / this.mc.thePlayer.getMaxHealth() ? "§aW" : "§cL");
         }

         int stringWidth = this.mc.fontRendererObj.getStringWidth(playerInfo) + 8;
         int maX = res.getScaledWidth() / 2 - stringWidth / 2 + miX + this.mc.fontRendererObj.getStringWidth(playerInfo) + 8;
         int maY = res.getScaledHeight() / 2 + 15 + miY + this.mc.fontRendererObj.FONT_HEIGHT + 5 - 6 + 8;
         this.miX = miX;
         this.miY = miY;
         this.maX = maX;
         this.maY = maY;
         this.clickMinX = miX;
         TargetInfo.this.posX = miX;
         TargetInfo.this.posY = miY;
         String edit = "Edit the HUD position by dragging.";
         int x = res.getScaledWidth() / 2 - this.fontRendererObj.getStringWidth(edit) / 2;
         int y = res.getScaledHeight() / 2 - 20;
         RenderUtils.drawColoredString(edit, '-', x, y, 2L, 0L, true, this.mc.fontRendererObj);

         try {
            this.handleInput();
         } catch (IOException var17) {
         }

         super.drawScreen(mX, mY, pt);
      }

      protected void mouseClickMove(int mX, int mY, int b, long t) {
         super.mouseClickMove(mX, mY, b, t);
         if (b == 0) {
            if (this.d) {
               this.aX = this.laX + (mX - this.lmX);
               this.aY = this.laY + (mY - this.lmY);
            } else if (mX > this.clickMinX && mX < this.maX && mY > this.miY && mY < this.maY) {
               this.d = true;
               this.lmX = mX;
               this.lmY = mY;
               this.laX = this.aX;
               this.laY = this.aY;
            }
         }
      }

      protected void mouseReleased(int mX, int mY, int s) {
         super.mouseReleased(mX, mY, s);
         if (s == 0) {
            this.d = false;
         }
      }

      public void actionPerformed(GuiButton b) {
         if (b == this.resetPosition) {
            this.aX = TargetInfo.this.posX = 70;
            this.aY = TargetInfo.this.posY = 30;
         }
      }

      public boolean doesGuiPauseGame() {
         return false;
      }
   }
}
