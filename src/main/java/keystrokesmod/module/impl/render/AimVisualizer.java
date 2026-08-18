package keystrokesmod.module.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class AimVisualizer extends Module {
   private ButtonSetting killAuraAim;
   private ButtonSetting scaffoldAim;
   private SliderSetting scaffoldHistoryTime;

   private static final int RED_COLOR = new Color(255, 0, 0, 255).getRGB();
   private static final float LINE_WIDTH = 2.0F;
   private static final double POINT_SIZE = 8.0;

   private Queue<AimPoint> scaffoldHistory = new LinkedList<>();
   private Vec3 lastScaffoldHitVec = null;

   private static class AimPoint {
      Vec3 position;
      long timestamp;

      AimPoint(Vec3 position, long timestamp) {
         this.position = position;
         this.timestamp = timestamp;
      }
   }

   public AimVisualizer() {
      super("AimVisualizer", Module.category.render);
      this.registerSetting(this.killAuraAim = new ButtonSetting("KillAura aim", true));
      this.registerSetting(this.scaffoldAim = new ButtonSetting("Scaffold aim", true));
      this.registerSetting(this.scaffoldHistoryTime = new SliderSetting("Scaffold history (ms)", 2000.0, 0.0, 10000.0, 500.0));
   }

   @Override
   public void onDisable() {
      this.scaffoldHistory.clear();
      this.lastScaffoldHitVec = null;
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (!Utils.nullCheck()) {
         return;
      }

      float partialTicks = ((IAccessorMinecraft)mc).getTimer().renderPartialTicks;
      Vec3 eyePos = mc.thePlayer.getPositionEyes(partialTicks);

      // Render KillAura aim
      if (this.killAuraAim.isToggled() && ModuleManager.killAura != null && ModuleManager.killAura.isEnabled()) {
         if (ModuleManager.killAura.attackingEntity != null) {
            MovingObjectPosition mov = RotationUtils.rayTrace(
               10.0,
               partialTicks,
               RotationUtils.serverRotations,
               ModuleManager.killAura.attackingEntity
            );

            if (mov != null && mov.hitVec != null) {
               this.renderAimPoint(eyePos, mov.hitVec, partialTicks);
            }
         }
      }

      // Render Scaffold aim with history
      if (this.scaffoldAim.isToggled()) {
         long currentTime = System.currentTimeMillis();
         long historyDuration = (long)this.scaffoldHistoryTime.getInput();

         // Update scaffold history
         if (ModuleManager.scaffold != null && ModuleManager.scaffold.isEnabled) {
            Vec3 hitVec = ModuleManager.scaffold.getHitVec();
            if (hitVec != null && !hitVec.equals(this.lastScaffoldHitVec)) {
               this.scaffoldHistory.add(new AimPoint(hitVec, currentTime));
               this.lastScaffoldHitVec = hitVec;
            }
         }

         // Remove expired points
         Iterator<AimPoint> iterator = this.scaffoldHistory.iterator();
         while (iterator.hasNext()) {
            AimPoint point = iterator.next();
            if (currentTime - point.timestamp > historyDuration) {
               iterator.remove();
            } else {
               break; // Queue is ordered by time, so we can stop here
            }
         }

         // Render every past aim point as a dot, and the line only for the current one
         AimPoint newest = null;
         for (AimPoint point : this.scaffoldHistory) {
            this.renderAimDot(point.position);
            newest = point;
         }

         if (newest != null) {
            this.renderAimLine(eyePos, newest.position);
         }
      }
   }

   private void renderAimPoint(Vec3 eyePos, Vec3 aimPoint, float partialTicks) {
      this.renderAimLine(eyePos, aimPoint);
      this.renderAimDot(aimPoint);
   }

   private void renderAimLine(Vec3 eyePos, Vec3 aimPoint) {
      double eyeX = eyePos.xCoord - mc.getRenderManager().viewerPosX;
      double eyeY = eyePos.yCoord - mc.getRenderManager().viewerPosY;
      double eyeZ = eyePos.zCoord - mc.getRenderManager().viewerPosZ;

      double aimX = aimPoint.xCoord - mc.getRenderManager().viewerPosX;
      double aimY = aimPoint.yCoord - mc.getRenderManager().viewerPosY;
      double aimZ = aimPoint.zCoord - mc.getRenderManager().viewerPosZ;

      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GL11.glEnable(GL11.GL_LINE_SMOOTH);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      GL11.glLineWidth(LINE_WIDTH);
      GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
      GL11.glBegin(GL11.GL_LINES);
      GL11.glVertex3d(eyeX, eyeY, eyeZ);
      GL11.glVertex3d(aimX, aimY, aimZ);
      GL11.glEnd();

      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      GL11.glDisable(GL11.GL_BLEND);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   private void renderAimDot(Vec3 aimPoint) {
      double aimX = aimPoint.xCoord - mc.getRenderManager().viewerPosX;
      double aimY = aimPoint.yCoord - mc.getRenderManager().viewerPosY;
      double aimZ = aimPoint.zCoord - mc.getRenderManager().viewerPosZ;

      GlStateManager.pushMatrix();
      GlStateManager.disableTexture2D();
      GlStateManager.disableDepth();
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

      GL11.glPointSize((float)POINT_SIZE);
      GL11.glColor4f(1.0F, 0.0F, 0.0F, 1.0F);
      GL11.glBegin(GL11.GL_POINTS);
      GL11.glVertex3d(aimX, aimY, aimZ);
      GL11.glEnd();

      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GL11.glDisable(GL11.GL_BLEND);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }
}
