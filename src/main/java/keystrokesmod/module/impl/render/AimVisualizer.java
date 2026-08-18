package keystrokesmod.module.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class AimVisualizer extends Module {
   private ButtonSetting killAuraAim;
   private ButtonSetting scaffoldAim;

   private static final int RED_COLOR = new Color(255, 0, 0, 255).getRGB();
   private static final float LINE_WIDTH = 2.0F;
   private static final double POINT_SIZE = 8.0;

   public AimVisualizer() {
      super("AimVisualizer", Module.category.render);
      this.registerSetting(this.killAuraAim = new ButtonSetting("KillAura aim", true));
      this.registerSetting(this.scaffoldAim = new ButtonSetting("Scaffold aim", true));
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

      // Render Scaffold aim
      if (this.scaffoldAim.isToggled() && ModuleManager.scaffold != null && ModuleManager.scaffold.isEnabled) {
         Vec3 targetBlock = ModuleManager.scaffold.getTargetBlock();
         float[] blockRots = ModuleManager.scaffold.getBlockRotations();
         if (targetBlock != null && blockRots != null) {
            // Calculate the hit point using current rotations
            float[] rots = blockRots;
            double reach = 4.5;
            Vec3 lookVec = RotationUtils.getVectorForRotation(rots[1], rots[0]);
            Vec3 aimPoint = eyePos.addVector(
               lookVec.xCoord * reach,
               lookVec.yCoord * reach,
               lookVec.zCoord * reach
            );

            this.renderAimPoint(eyePos, aimPoint, partialTicks);
         }
      }
   }

   private void renderAimPoint(Vec3 eyePos, Vec3 aimPoint, float partialTicks) {
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

      float r = 1.0F;
      float g = 0.0F;
      float b = 0.0F;
      float a = 1.0F;

      // Draw line from eye to aim point
      GL11.glLineWidth(LINE_WIDTH);
      GL11.glColor4f(r, g, b, a);
      GL11.glBegin(GL11.GL_LINES);
      GL11.glVertex3d(eyeX, eyeY, eyeZ);
      GL11.glVertex3d(aimX, aimY, aimZ);
      GL11.glEnd();

      // Draw point at aim location
      GL11.glPointSize((float)POINT_SIZE);
      GL11.glBegin(GL11.GL_POINTS);
      GL11.glVertex3d(aimX, aimY, aimZ);
      GL11.glEnd();

      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GL11.glDisable(GL11.GL_LINE_SMOOTH);
      GL11.glDisable(GL11.GL_BLEND);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }
}
