package keystrokesmod.module.impl.render;

import java.awt.Color;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class Arrows extends Module {
   private SliderSetting arrow;
   private SliderSetting radius;
   private ButtonSetting teamColor;
   private ButtonSetting hideTeammates;
   private ButtonSetting enemiesOnly;
   private ButtonSetting renderFriends;
   private ButtonSetting renderEnemies;
   private ButtonSetting renderDistance;
   private ButtonSetting renderOnlyOffScreen;
   private ButtonSetting renderInGUIs;
   private int friendColor = new Color(0, 255, 0, 255).getRGB();
   private int enemyColor = new Color(255, 0, 0, 255).getRGB();
   private String[] arrowTypes = new String[]{"Caret", "Greater than", "Triangle"};

   public Arrows() {
      super("Arrows", Module.category.render);
      this.registerSetting(this.arrow = new SliderSetting("Arrow", 0, this.arrowTypes));
      this.registerSetting(this.radius = new SliderSetting("Circle radius", 50.0, 30.0, 200.0, 5.0));
      this.registerSetting(this.teamColor = new ButtonSetting("Team color", true));
      this.registerSetting(this.hideTeammates = new ButtonSetting("Hide teammates", true));
      this.registerSetting(this.enemiesOnly = new ButtonSetting("Show enemies only", false));
      this.registerSetting(this.renderFriends = new ButtonSetting("Render friends (green)", true));
      this.registerSetting(this.renderEnemies = new ButtonSetting("Render enemies (red)", true));
      this.registerSetting(this.renderDistance = new ButtonSetting("Render distance", true));
      this.registerSetting(this.renderOnlyOffScreen = new ButtonSetting("Render only offscreen", false));
      this.registerSetting(this.renderInGUIs = new ButtonSetting("Render in GUIs", false));
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent event) {
      if (event.phase == Phase.END) {
         if ((mc.currentScreen == null || this.renderInGUIs.isToggled()) && Utils.nullCheck()) {
            try {
               for (EntityPlayer en : mc.theWorld.playerEntities) {
                  if (en != null && en != mc.thePlayer && !AntiBot.isBot(en)) {
                     this.renderIndicatorFor(en, event.renderTickTime);
                  }
               }
            } catch (Exception var4) {
            }
         }
      }
   }

   private void renderIndicatorFor(EntityPlayer en, float partialTicks) {
      if (!this.renderOnlyOffScreen.isToggled() || !RenderUtils.isInViewFrustum(en)) {
         int color = -1;
         if (!Utils.isTeammate(en) || !this.hideTeammates.isToggled()) {
            if (Utils.isEnemy(en) || !this.enemiesOnly.isToggled()) {
               if (this.renderFriends.isToggled() && Utils.isFriended(en)) {
                  color = this.friendColor;
               } else if (this.renderEnemies.isToggled() && Utils.isEnemy(en)) {
                  color = this.enemyColor;
               } else if (this.teamColor.isToggled()) {
                  color = Utils.getColorFromEntity(en);
               }

               double x = en.lastTickPosX + (en.posX - en.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
               double y = en.lastTickPosY + (en.posY - en.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY + en.height / 2.0F;
               double z = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
               ((IAccessorEntityRenderer)mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft)mc).getTimer().renderPartialTicks, 0);
               ScaledResolution scaledResolution = new ScaledResolution(mc);
               Vec3 vec = RenderUtils.convertTo2D(scaledResolution.getScaleFactor(), x, y, z);
               if (vec != null) {
                  mc.entityRenderer.setupOverlayRendering();
                  ScaledResolution res = new ScaledResolution(mc);
                  double dx = vec.xCoord - res.getScaledWidth() / 2.0;
                  double dy = vec.yCoord - res.getScaledHeight() / 2.0;
                  boolean inFrustum = vec.zCoord < 1.0003684;
                  if (!inFrustum) {
                     dx *= -1.0;
                     dy *= -1.0;
                  }

                  double angle1 = Math.atan2(dx, dy);
                  double angle2 = Math.atan2(dy, dx) * (float) (180.0 / Math.PI) + 90.0;
                  double hypotenuse = Math.hypot(dx, dy);
                  double radiusInput = this.radius.getInput();
                  if (inFrustum && hypotenuse < radiusInput + 15.0) {
                     return;
                  }

                  double baseX = res.getScaledWidth() / 2.0;
                  double baseY = res.getScaledHeight() / 2.0;
                  double sinAng = Math.sin(angle1);
                  double cosAng = Math.cos(angle1);
                  double renderX = baseX + radiusInput * sinAng;
                  double renderY = baseY + radiusInput * cosAng;
                  GlStateManager.pushMatrix();
                  GlStateManager.translate(renderX, renderY, 0.0);
                  GlStateManager.rotate((float)angle2, 0.0F, 0.0F, 1.0F);
                  GlStateManager.scale(1.0F, 1.0F, 1.0F);
                  int arrowInput = (int)this.arrow.getInput();
                  if (arrowInput == 0) {
                     if (color == -1) {
                        GL11.glColor3d(1.0, 1.0, 1.0);
                     } else {
                        int rgb = color;
                        float red = (rgb >> 16 & 0xFF) / 255.0F;
                        float green = (rgb >> 8 & 0xFF) / 255.0F;
                        float blue = (rgb & 0xFF) / 255.0F;
                        GL11.glColor4f(red, green, blue, 1.0F);
                     }

                     GL11.glEnable(3042);
                     GL11.glDisable(3553);
                     GL11.glBlendFunc(770, 771);
                     GL11.glEnable(2848);
                     double halfAngle = 0.61086524F;
                     double size = 9.0;
                     double offsetY = 5.0;
                     GL11.glLineWidth(3.0F);
                     GL11.glBegin(3);
                     GL11.glVertex2d(Math.sin(-halfAngle) * size, Math.cos(-halfAngle) * size - offsetY);
                     GL11.glVertex2d(0.0, -offsetY);
                     GL11.glVertex2d(Math.sin(halfAngle) * size, Math.cos(halfAngle) * size - offsetY);
                     GL11.glEnd();
                     GL11.glEnable(3553);
                     GL11.glDisable(3042);
                     GL11.glDisable(2848);
                  } else if (arrowInput == 1) {
                     GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                     GlStateManager.scale(1.5, 1.5, 1.5);
                     mc.fontRendererObj.drawString(">", -2.0F, -4.0F, color, false);
                  } else if (arrowInput == 2) {
                     RenderUtils.draw2DPolygon(0.0, 0.0, 5.0, 3, Utils.mergeAlpha(color, 255));
                  }

                  GlStateManager.popMatrix();
                  renderX = baseX + (radiusInput - 13.0) * sinAng;
                  renderY = baseY + (radiusInput - 13.0) * cosAng;
                  GlStateManager.pushMatrix();
                  GlStateManager.translate(renderX, renderY, 0.0);
                  GlStateManager.scale(0.8, 0.8, 0.8);
                  if (this.renderDistance.isToggled()) {
                     String text = (int)mc.thePlayer.getDistanceToEntity(en) + "m";
                     mc.fontRendererObj.drawString(text, -mc.fontRendererObj.getStringWidth(text) / 2, -4.0F, -1, true);
                  }

                  GlStateManager.popMatrix();
               }
            }
         }
      }
   }
}
