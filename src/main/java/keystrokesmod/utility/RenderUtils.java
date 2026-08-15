package keystrokesmod.utility;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.impl.player.Freecam;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

public class RenderUtils {
   private static Minecraft mc = Minecraft.getMinecraft();
   private static Frustum frustum = new Frustum();
   private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);
   private static final FloatBuffer PROJECTION = BufferUtils.createFloatBuffer(16);
   private static final IntBuffer VIEWPORT = BufferUtils.createIntBuffer(16);
   private static final FloatBuffer SCREEN_COORDS = BufferUtils.createFloatBuffer(3);

   public static void renderBlock(BlockPos blockPos, int color, boolean outline, boolean shade) {
      renderBox(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0, 1.0, 1.0, color, outline, shade);
   }

   public static void renderChest(BlockPos blockPos, int color, boolean outline, boolean shade) {
      renderBox(blockPos.getX() + 0.0625F, blockPos.getY(), blockPos.getZ() + 0.0625F, 0.875, 0.875, 0.875, color, outline, shade);
   }

   public static void renderBlock(BlockPos blockPos, int color, double y2, boolean outline, boolean shade) {
      renderBox(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1.0, y2, 1.0, color, outline, shade);
   }

   public static void scissor(double x, double y, double width, double height) {
      ScaledResolution sr = new ScaledResolution(mc);
      int scale = sr.getScaleFactor();
      int scaledX = (int)(x * scale);
      int scaledY = (int)((sr.getScaledHeight() - (y + height)) * scale);
      int scaledWidth = (int)(width * scale);
      int scaledHeight = (int)(height * scale);
      if (scaledWidth >= 0 && scaledHeight >= 0) {
         GL11.glScissor(scaledX, scaledY, scaledWidth, scaledHeight);
      }
   }

   public static boolean isInViewFrustum(Entity entity) {
      return isInViewFrustum(entity.getEntityBoundingBox()) || entity.ignoreFrustumCheck;
   }

   private static boolean isInViewFrustum(AxisAlignedBB bb) {
      frustum.setPosition(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().posY, mc.getRenderViewEntity().posZ);
      return frustum.isBoundingBoxInFrustum(bb);
   }

   public static void drawRect(double left, double top, double right, double bottom, int color) {
      float f3 = (color >> 24 & 0xFF) / 255.0F;
      float f = (color >> 16 & 0xFF) / 255.0F;
      float f1 = (color >> 8 & 0xFF) / 255.0F;
      float f2 = (color & 0xFF) / 255.0F;
      GlStateManager.pushMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      WorldRenderer worldrenderer = tessellator.getWorldRenderer();
      GlStateManager.enableBlend();
      GlStateManager.disableTexture2D();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.color(f, f1, f2, f3);
      worldrenderer.begin(7, DefaultVertexFormats.POSITION);
      worldrenderer.pos(left, bottom, 0.0).endVertex();
      worldrenderer.pos(right, bottom, 0.0).endVertex();
      worldrenderer.pos(right, top, 0.0).endVertex();
      worldrenderer.pos(left, top, 0.0).endVertex();
      tessellator.draw();
      GlStateManager.enableTexture2D();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }

   public static void drawPlayerBoundingBox(Vec3 pos, int color) {
      GlStateManager.pushMatrix();
      double x = pos.xCoord - mc.getRenderManager().viewerPosX;
      double y = pos.yCoord - mc.getRenderManager().viewerPosY;
      double z = pos.zCoord - mc.getRenderManager().viewerPosZ;
      AxisAlignedBB bbox = mc.thePlayer.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
      AxisAlignedBB axis = new AxisAlignedBB(
         bbox.minX - mc.thePlayer.posX + x,
         bbox.minY - mc.thePlayer.posY + y,
         bbox.minZ - mc.thePlayer.posZ + z,
         bbox.maxX - mc.thePlayer.posX + x,
         bbox.maxY - mc.thePlayer.posY + y,
         bbox.maxZ - mc.thePlayer.posZ + z
      );
      float a = (color >> 24 & 0xFF) / 255.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glLineWidth(2.0F);
      GL11.glColor4f(r, g, b, a);
      drawBoundingBox(axis, r, g, b, a);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GlStateManager.popMatrix();
   }

   public static void drawOutline(float x, float y, float x2, float y2, float lineWidth, int color) {
      float f5 = (color >> 24 & 0xFF) / 255.0F;
      float f6 = (color >> 16 & 0xFF) / 255.0F;
      float f7 = (color >> 8 & 0xFF) / 255.0F;
      float f8 = (color & 0xFF) / 255.0F;
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(2848);
      GL11.glPushMatrix();
      GL11.glColor4f(f6, f7, f8, f5);
      GL11.glLineWidth(lineWidth);
      GL11.glBegin(1);
      GL11.glVertex2d(x, y);
      GL11.glVertex2d(x, y2);
      GL11.glVertex2d(x2, y2);
      GL11.glVertex2d(x2, y);
      GL11.glVertex2d(x, y);
      GL11.glVertex2d(x2, y);
      GL11.glVertex2d(x, y2);
      GL11.glVertex2d(x2, y2);
      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glDisable(2848);
   }

   public static void renderBox(double x, double y, double z, double x2, double y2, double z2, int color, boolean outline, boolean shade) {
      double xPos = x - mc.getRenderManager().viewerPosX;
      double yPos = y - mc.getRenderManager().viewerPosY;
      double zPos = z - mc.getRenderManager().viewerPosZ;
      GL11.glPushMatrix();
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3042);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      float n8 = (color >> 24 & 0xFF) / 255.0F;
      float n9 = (color >> 16 & 0xFF) / 255.0F;
      float n10 = (color >> 8 & 0xFF) / 255.0F;
      float n11 = (color & 0xFF) / 255.0F;
      GL11.glColor4f(n9, n10, n11, n8);
      AxisAlignedBB axisAlignedBB = new AxisAlignedBB(xPos, yPos, zPos, xPos + x2, yPos + y2, zPos + z2);
      if (outline) {
         RenderGlobal.drawSelectionBoundingBox(axisAlignedBB);
      }

      if (shade) {
         drawBoundingBox(axisAlignedBB, n9, n10, n11);
      }

      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   public static void renderBPS(boolean b, boolean b2) {
      ScaledResolution scaledResolution = new ScaledResolution(mc);
      String s = "";
      int n = -1;
      if (b) {
         double t = Utils.gbps((Entity)(Freecam.freeEntity == null ? mc.thePlayer : Freecam.freeEntity), 2);
         if (t < 10.0) {
            n = Color.green.getRGB();
         } else if (t < 30.0) {
            n = Color.yellow.getRGB();
         } else if (t < 60.0) {
            n = Color.orange.getRGB();
         } else if (t < 160.0) {
            n = Color.red.getRGB();
         } else {
            n = Color.black.getRGB();
         }

         s = s + t + "bps";
      }

      if (b2) {
         double h = Utils.getHorizontalSpeed();
         if (!s.isEmpty()) {
            s = s + " ";
         }

         s = s + Utils.round(h, 3);
      }

      mc.fontRendererObj
         .drawString(s, scaledResolution.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(s) / 2, scaledResolution.getScaledHeight() / 2 + 15, n, false);
   }

   public static void renderEntity(Entity e, int type, double expand, double shift, int color, boolean damage) {
      if (e instanceof EntityLivingBase) {
         float partialTicks = ((IAccessorMinecraft)mc).getTimer().renderPartialTicks;
         double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
         double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
         double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
         float d = (float)expand / 40.0F;
         if (e instanceof EntityPlayer && damage && ((EntityPlayer)e).hurtTime != 0) {
            color = Color.RED.getRGB();
         }

         GlStateManager.pushMatrix();
         if (type == 3) {
            GL11.glTranslated(x, y - 0.2, z);
            GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0, 1.0, 0.0);
            GlStateManager.disableDepth();
            GL11.glScalef(0.03F + d, 0.03F + d, 0.03F + d);
            int outline = Color.black.getRGB();
            Gui.drawRect(-20, -1, -26, 75, outline);
            Gui.drawRect(20, -1, 26, 75, outline);
            Gui.drawRect(-20, -1, 21, 5, outline);
            Gui.drawRect(-20, 70, 21, 75, outline);
            if (color != 0) {
               Gui.drawRect(-21, 0, -25, 74, color);
               Gui.drawRect(21, 0, 25, 74, color);
               Gui.drawRect(-21, 0, 24, 4, color);
               Gui.drawRect(-21, 71, 25, 74, color);
            } else {
               int st = Utils.getChroma(2L, 0L);
               int en = Utils.getChroma(2L, 1000L);
               dGR(-21, 0, -25, 74, st, en);
               dGR(21, 0, 25, 74, st, en);
               Gui.drawRect(-21, 0, 21, 4, en);
               Gui.drawRect(-21, 71, 21, 74, st);
            }

            GlStateManager.enableDepth();
         } else if (type == 4) {
            EntityLivingBase en = (EntityLivingBase)e;
            double health = en.getHealth() / en.getMaxHealth();
            int barHeight = (int)(74.0 * health);
            int healthColor = health < 0.3
               ? Color.red.getRGB()
               : (health < 0.5 ? Color.orange.getRGB() : (health < 0.7 ? Color.yellow.getRGB() : Color.green.getRGB()));
            GL11.glTranslated(x, y - 0.2, z);
            GL11.glRotated(-mc.getRenderManager().playerViewY, 0.0, 1.0, 0.0);
            GlStateManager.disableDepth();
            GL11.glScalef(0.03F + d, 0.03F + d, 0.03F + d);
            int i = (int)(21.0 + shift * 2.0);
            Gui.drawRect(i, -1, i + 4, 75, Color.black.getRGB());
            Gui.drawRect(i + 1, barHeight, i + 3, 74, Color.darkGray.getRGB());
            Gui.drawRect(i + 1, 0, i + 3, barHeight, healthColor);
            GlStateManager.enableDepth();
         } else if (type == 6) {
            drawCircle(x, y, z, 0.7F, 45, 1.5F, color, color == 0);
         } else {
            if (color == 0) {
               color = Utils.getChroma(2L, 0L);
            }

            float a = (color >> 24 & 0xFF) / 255.0F;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            AxisAlignedBB bbox = e.getEntityBoundingBox().expand(0.1 + expand, 0.1 + expand, 0.1 + expand);
            AxisAlignedBB axis = new AxisAlignedBB(
               bbox.minX - e.posX + x,
               bbox.minY - e.posY + y,
               bbox.minZ - e.posZ + z,
               bbox.maxX - e.posX + x,
               bbox.maxY - e.posY + y,
               bbox.maxZ - e.posZ + z
            );
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0F);
            GL11.glColor4f(r, g, b, a);
            if (type == 1) {
               RenderGlobal.drawSelectionBoundingBox(axis);
            } else if (type == 2) {
               drawBoundingBox(axis, r, g, b);
            }

            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
         }

         GlStateManager.popMatrix();
      }
   }

   public static void drawPolygon(double n, double n2, double n3, int n4, int n5) {
      if (n4 >= 3) {
         float n6 = (n5 >> 24 & 0xFF) / 255.0F;
         float n7 = (n5 >> 16 & 0xFF) / 255.0F;
         float n8 = (n5 >> 8 & 0xFF) / 255.0F;
         float n9 = (n5 & 0xFF) / 255.0F;
         Tessellator getInstance = Tessellator.getInstance();
         WorldRenderer getWorldRenderer = getInstance.getWorldRenderer();
         GlStateManager.enableBlend();
         GlStateManager.disableTexture2D();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GL11.glColor4f(n7, n8, n9, n6);
         getWorldRenderer.begin(6, DefaultVertexFormats.POSITION);

         for (int i = 0; i < n4; i++) {
            double n10 = (Math.PI * 2) * i / n4 + Math.toRadians(180.0);
            getWorldRenderer.pos(n + Math.sin(n10) * n3, n2 + Math.cos(n10) * n3, 0.0).endVertex();
         }

         getInstance.draw();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
      }
   }

   public static void drawBoundingBox(AxisAlignedBB abb, float r, float g, float b) {
      drawBoundingBox(abb, r, g, b, 0.25F);
   }

   public static void drawBoundingBox(AxisAlignedBB abb, float r, float g, float b, float a) {
      Tessellator ts = Tessellator.getInstance();
      WorldRenderer vb = ts.getWorldRenderer();
      vb.begin(7, DefaultVertexFormats.POSITION_COLOR);
      vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      ts.draw();
      vb.begin(7, DefaultVertexFormats.POSITION_COLOR);
      vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      ts.draw();
      vb.begin(7, DefaultVertexFormats.POSITION_COLOR);
      vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      ts.draw();
      vb.begin(7, DefaultVertexFormats.POSITION_COLOR);
      vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      ts.draw();
      vb.begin(7, DefaultVertexFormats.POSITION_COLOR);
      vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      ts.draw();
      vb.begin(7, DefaultVertexFormats.POSITION_COLOR);
      vb.pos(abb.minX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.minX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.minZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.maxY, abb.maxZ).color(r, g, b, a).endVertex();
      vb.pos(abb.maxX, abb.minY, abb.maxZ).color(r, g, b, a).endVertex();
      ts.draw();
   }

   public static void renderBlockModel(IBlockState blockState, double x, double y, double z, int color) {
      Minecraft mc = Minecraft.getMinecraft();
      BlockRendererDispatcher dispatcher = mc.getBlockRendererDispatcher();
      IBakedModel model = dispatcher.getModelFromBlockState(blockState, mc.theWorld, new BlockPos(x, y, z));
      double xPos = x - mc.getRenderManager().viewerPosX;
      double yPos = y - mc.getRenderManager().viewerPosY;
      double zPos = z - mc.getRenderManager().viewerPosZ;
      float a = (color >> 24 & 0xFF) / 255.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      GlStateManager.pushMatrix();
      GlStateManager.translate(xPos, yPos, zPos);
      GlStateManager.enableBlend();
      GlStateManager.blendFunc(770, 771);
      GlStateManager.disableTexture2D();
      GlStateManager.disableCull();
      GlStateManager.disableDepth();
      GlStateManager.depthMask(false);
      GlStateManager.color(r, g, b, a);
      renderModelColoredQuads(model, r, g, b, a);
      GlStateManager.depthMask(true);
      GlStateManager.enableDepth();
      GlStateManager.enableTexture2D();
      GlStateManager.enableCull();
      GlStateManager.disableBlend();
      GlStateManager.popMatrix();
   }

   private static void renderModelColoredQuads(IBakedModel model, float r, float g, float b, float a) {
      Tessellator tessellator = Tessellator.getInstance();
      WorldRenderer wr = tessellator.getWorldRenderer();

      for (EnumFacing face : EnumFacing.values()) {
         for (BakedQuad quad : model.getFaceQuads(face)) {
            drawColoredQuad(wr, quad, r, g, b, a, tessellator);
         }
      }

      for (BakedQuad quad : model.getGeneralQuads()) {
         drawColoredQuad(wr, quad, r, g, b, a, tessellator);
      }
   }

   private static void drawColoredQuad(WorldRenderer wr, BakedQuad quad, float r, float g, float b, float a, Tessellator tessellator) {
      int[] vertexData = quad.getVertexData();
      int vertexCount = 4;
      int intsPerVertex = vertexData.length / 4;
      wr.begin(7, DefaultVertexFormats.POSITION_COLOR);

      for (int i = 0; i < 4; i++) {
         int baseIndex = i * intsPerVertex;
         float vx = Float.intBitsToFloat(vertexData[baseIndex]);
         float vy = Float.intBitsToFloat(vertexData[baseIndex + 1]);
         float vz = Float.intBitsToFloat(vertexData[baseIndex + 2]);
         wr.pos(vx, vy, vz).color(r, g, b, a).endVertex();
      }

      tessellator.draw();
   }

   public static void drawTracerLine(Entity e, int color, float lineWidth, float partialTicks) {
      if (e != null) {
         double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
         double y = e.getEyeHeight() + e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
         double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
         float a = (color >> 24 & 0xFF) / 255.0F;
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glEnable(2848);
         GL11.glDisable(2929);
         GL11.glDisable(3553);
         GL11.glBlendFunc(770, 771);
         GL11.glEnable(3042);
         GL11.glLineWidth(lineWidth);
         GL11.glColor4f(r, g, b, a);
         GL11.glBegin(2);
         GL11.glVertex3d(0.0, mc.thePlayer.getEyeHeight(), 0.0);
         GL11.glVertex3d(x, y, z);
         GL11.glEnd();
         GL11.glDisable(3042);
         GL11.glEnable(3553);
         GL11.glEnable(2929);
         GL11.glDisable(2848);
         GL11.glDisable(3042);
         GL11.glPopMatrix();
      }
   }

   public static void dGR(int left, int top, int right, int bottom, int startColor, int endColor) {
      if (left < right) {
         int j = left;
         left = right;
         right = j;
      }

      if (top < bottom) {
         int j = top;
         top = bottom;
         bottom = j;
      }

      float f = (startColor >> 24 & 0xFF) / 255.0F;
      float f1 = (startColor >> 16 & 0xFF) / 255.0F;
      float f2 = (startColor >> 8 & 0xFF) / 255.0F;
      float f3 = (startColor & 0xFF) / 255.0F;
      float f4 = (endColor >> 24 & 0xFF) / 255.0F;
      float f5 = (endColor >> 16 & 0xFF) / 255.0F;
      float f6 = (endColor >> 8 & 0xFF) / 255.0F;
      float f7 = (endColor & 0xFF) / 255.0F;
      GlStateManager.disableTexture2D();
      GlStateManager.enableBlend();
      GlStateManager.disableAlpha();
      GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
      GlStateManager.shadeModel(7425);
      Tessellator tessellator = Tessellator.getInstance();
      WorldRenderer worldrenderer = tessellator.getWorldRenderer();
      worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
      worldrenderer.pos(right, top, 0.0).color(f1, f2, f3, f).endVertex();
      worldrenderer.pos(left, top, 0.0).color(f1, f2, f3, f).endVertex();
      worldrenderer.pos(left, bottom, 0.0).color(f5, f6, f7, f4).endVertex();
      worldrenderer.pos(right, bottom, 0.0).color(f5, f6, f7, f4).endVertex();
      tessellator.draw();
      GlStateManager.shadeModel(7424);
      GlStateManager.disableBlend();
      GlStateManager.enableAlpha();
      GlStateManager.enableTexture2D();
   }

   public static void db(int w, int h, int r) {
      int c = r == -1 ? -1089466352 : r;
      Gui.drawRect(0, 0, w, h, c);
   }

   public static void drawColoredString(String text, char lineSplit, int x, int y, long s, long shift, boolean rect, FontRenderer fontRenderer) {
      int bX = x;
      int l = 0;
      long r = 0L;

      for (int i = 0; i < text.length(); i++) {
         char c = text.charAt(i);
         if (c == lineSplit) {
            l++;
            x = bX;
            y += fontRenderer.FONT_HEIGHT + 5;
            r = shift * l;
         } else {
            fontRenderer.drawString(String.valueOf(c), x, y, Utils.getChroma(s, r), rect);
            x += fontRenderer.getCharWidth(c);
            if (c != ' ') {
               r -= 90L;
            }
         }
      }
   }

   public static void drawCircle(double x, double y, double z, double radius, int sides, float lineWidth, int color, boolean chroma) {
      float a = (color >> 24 & 0xFF) / 255.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      mc.entityRenderer.disableLightmap();
      GL11.glDisable(3553);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2929);
      GL11.glEnable(2848);
      GL11.glDepthMask(false);
      GL11.glLineWidth(lineWidth);
      if (!chroma) {
         GL11.glColor4f(r, g, b, a);
      }

      GL11.glBegin(1);
      long d = 0L;
      long ed = 15000L / sides;
      long hed = ed / 2L;

      for (int i = 0; i < sides * 2; i++) {
         if (chroma) {
            if (i % 2 != 0) {
               if (i == 47) {
                  d = hed;
               }

               d += ed;
            }

            int c = Utils.getChroma(2L, d);
            float r2 = (c >> 16 & 0xFF) / 255.0F;
            float g2 = (c >> 8 & 0xFF) / 255.0F;
            float b2 = (c & 0xFF) / 255.0F;
            GL11.glColor3f(r2, g2, b2);
         }

         double angle = (Math.PI * 2) * i / sides + Math.toRadians(180.0);
         GL11.glVertex3d(x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius);
      }

      GL11.glEnd();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glDepthMask(true);
      GL11.glDisable(2848);
      GL11.glEnable(2929);
      GL11.glDisable(3042);
      GL11.glEnable(3553);
      mc.entityRenderer.enableLightmap();
   }

   public static void drawCaret(float x, float y, int color, double width, double length) {
      GL11.glPushMatrix();
      GL11.glEnable(2848);
      GL11.glDisable(3553);
      glColor(color);
      GL11.glLineWidth((float)width);
      float halfWidth = (float)(width / 2.0);
      float xOffset = halfWidth / 2.0F;
      float yOffset = halfWidth / 2.0F;
      GL11.glBegin(1);
      GL11.glVertex2d(x - xOffset, y + yOffset);
      GL11.glVertex2d(x + length - xOffset, y - length + yOffset);
      GL11.glVertex2d(x + length - xOffset, y - length + yOffset);
      GL11.glVertex2d(x + 2.0 * length - xOffset, y + yOffset);
      GL11.glEnd();
      GL11.glEnable(3553);
      GL11.glDisable(2848);
      GL11.glPopMatrix();
   }

   public static void drawTriangle(double x, double y, double size, double widthDiv, double heightDiv, int color) {
      boolean blend = GL11.glIsEnabled(3042);
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(2848);
      GL11.glPushMatrix();
      glColor(color);
      GL11.glBegin(7);
      GL11.glVertex2d(x, y);
      GL11.glVertex2d(x - size / widthDiv, y + size);
      GL11.glVertex2d(x, y + size / heightDiv);
      GL11.glVertex2d(x + size / widthDiv, y + size);
      GL11.glVertex2d(x, y);
      GL11.glEnd();
      GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.8F);
      GL11.glBegin(2);
      GL11.glVertex2d(x, y);
      GL11.glVertex2d(x - size / widthDiv, y + size);
      GL11.glVertex2d(x, y + size / heightDiv);
      GL11.glVertex2d(x + size / widthDiv, y + size);
      GL11.glVertex2d(x, y);
      GL11.glEnd();
      GL11.glPopMatrix();
      GL11.glEnable(3553);
      if (!blend) {
         GL11.glDisable(3042);
      }

      GL11.glDisable(2848);
   }

   public static void glColor(int n) {
      GL11.glColor4f((n >> 16 & 0xFF) / 255.0F, (n >> 8 & 0xFF) / 255.0F, (n & 0xFF) / 255.0F, (n >> 24 & 0xFF) / 255.0F);
   }

   public static void drawRoundedGradientOutlinedRectangle(float x, float y, float x2, float y2, float radius, int n6, int n7, int n8) {
      x *= 2.0F;
      y *= 2.0F;
      x2 *= 2.0F;
      y2 *= 2.0F;
      GL11.glPushAttrib(1);
      GL11.glScaled(0.5, 0.5, 0.5);
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glEnable(2848);
      GL11.glBegin(9);
      glColor(n6);

      for (int i = 0; i <= 90; i += 3) {
         double n9 = i * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x + radius + Math.sin(n9) * radius * -1.0, y + radius + Math.cos(n9) * radius * -1.0);
      }

      for (int j = 90; j <= 180; j += 3) {
         double n10 = j * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x + radius + Math.sin(n10) * radius * -1.0, y2 - radius + Math.cos(n10) * radius * -1.0);
      }

      for (int k = 0; k <= 90; k += 3) {
         double n11 = k * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x2 - radius + Math.sin(n11) * radius, y2 - radius + Math.cos(n11) * radius);
      }

      for (int l = 90; l <= 180; l += 3) {
         double n12 = l * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x2 - radius + Math.sin(n12) * radius, y + radius + Math.cos(n12) * radius);
      }

      GL11.glEnd();
      GL11.glPushMatrix();
      GL11.glShadeModel(7425);
      GL11.glLineWidth(2.0F);
      GL11.glBegin(2);
      if (n7 != 0L) {
         glColor(n7);
      }

      for (int n13 = 0; n13 <= 90; n13 += 3) {
         double n14 = n13 * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x + radius + Math.sin(n14) * radius * -1.0, y + radius + Math.cos(n14) * radius * -1.0);
      }

      for (int n15 = 90; n15 <= 180; n15 += 3) {
         double n16 = n15 * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x + radius + Math.sin(n16) * radius * -1.0, y2 - radius + Math.cos(n16) * radius * -1.0);
      }

      if (n8 != 0) {
         glColor(n8);
      }

      for (int n17 = 0; n17 <= 90; n17 += 3) {
         double n18 = n17 * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x2 - radius + Math.sin(n18) * radius, y2 - radius + Math.cos(n18) * radius);
      }

      for (int n19 = 90; n19 <= 180; n19 += 3) {
         double n20 = n19 * (float) (Math.PI / 180.0);
         GL11.glVertex2d(x2 - radius + Math.sin(n20) * radius, y + radius + Math.cos(n20) * radius);
      }

      GL11.glEnd();
      GL11.glPopMatrix();
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glDisable(2848);
      GL11.glEnable(3553);
      GL11.glScaled(2.0, 2.0, 2.0);
      GL11.glPopAttrib();
      GL11.glLineWidth(1.0F);
      GL11.glShadeModel(7424);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static void draw2DPolygon(double x, double y, double radius, int sides, int color) {
      if (sides >= 3) {
         float a = (color >> 24 & 0xFF) / 255.0F;
         float r = (color >> 16 & 0xFF) / 255.0F;
         float g = (color >> 8 & 0xFF) / 255.0F;
         float b = (color & 0xFF) / 255.0F;
         Tessellator tessellator = Tessellator.getInstance();
         WorldRenderer worldrenderer = tessellator.getWorldRenderer();
         GlStateManager.enableBlend();
         GlStateManager.disableTexture2D();
         GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
         GL11.glEnable(2848);
         GL11.glColor4f(r, g, b, a);
         double rad180 = Math.toRadians(180.0);
         worldrenderer.begin(6, DefaultVertexFormats.POSITION);

         for (int i = 0; i < sides; i++) {
            double angle = (Math.PI * 2) * i / sides + rad180;
            worldrenderer.pos(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius, 0.0).endVertex();
         }

         tessellator.draw();
         GlStateManager.enableTexture2D();
         GlStateManager.disableBlend();
      }
   }

   public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
      return createFrameBuffer(framebuffer, false);
   }

   public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
      if (needsNewFramebuffer(framebuffer)) {
         if (framebuffer != null) {
            framebuffer.deleteFramebuffer();
         }

         return new Framebuffer(mc.displayWidth, mc.displayHeight, depth);
      } else {
         return framebuffer;
      }
   }

   public static boolean needsNewFramebuffer(Framebuffer framebuffer) {
      return framebuffer == null || framebuffer.framebufferWidth != mc.displayWidth || framebuffer.framebufferHeight != mc.displayHeight;
   }

   public static void bindTexture(int texture) {
      GL11.glBindTexture(3553, texture);
   }

   public static void setAlphaLimit(float limit) {
      GlStateManager.enableAlpha();
      GlStateManager.alphaFunc(516, (float)(limit * 0.01));
   }

   public static Color interpolateColorC(Color color1, Color color2, float amount) {
      amount = Math.min(1.0F, Math.max(0.0F, amount));
      return new Color(
         interpolateInt(color1.getRed(), color2.getRed(), amount),
         interpolateInt(color1.getGreen(), color2.getGreen(), amount),
         interpolateInt(color1.getBlue(), color2.getBlue(), amount),
         interpolateInt(color1.getAlpha(), color2.getAlpha(), amount)
      );
   }

   public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
      return interpolate(oldValue, newValue, (float)interpolationValue).intValue();
   }

   public static Double interpolate(double oldValue, double newValue, double interpolationValue) {
      return oldValue + (newValue - oldValue) * interpolationValue;
   }

   public static void resetColor() {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
   }

   public static Vec3 convertTo2D(int scaleFactor, double x, double y, double z) {
      GL11.glGetFloat(2982, MODELVIEW);
      GL11.glGetFloat(2983, PROJECTION);
      GL11.glGetInteger(2978, VIEWPORT);
      boolean result = GLU.gluProject((float)x, (float)y, (float)z, MODELVIEW, PROJECTION, VIEWPORT, SCREEN_COORDS);
      return result ? new Vec3(SCREEN_COORDS.get(0) / scaleFactor, (Display.getHeight() - SCREEN_COORDS.get(1)) / scaleFactor, SCREEN_COORDS.get(2)) : null;
   }

   public static void drawRoundedRectangle(float x, float y, float x2, float y2, float radius, int color) {
      if (!(x2 <= x)) {
         float width = x2 - x;
         if (width < 3.0F) {
            radius = Math.min(radius, width / 2.0F);
         }

         x = (float)(x * 2.0);
         y = (float)(y * 2.0);
         x2 = (float)(x2 * 2.0);
         y2 = (float)(y2 * 2.0);
         GL11.glPushAttrib(0);
         GL11.glScaled(0.5, 0.5, 0.5);
         GL11.glEnable(3042);
         GL11.glDisable(3553);
         GL11.glEnable(2848);
         GL11.glBegin(9);
         glColor(color);

         for (int i = 0; i <= 90; i += 3) {
            double n7 = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n7) * radius * -1.0, y + radius + Math.cos(n7) * radius * -1.0);
         }

         for (int j = 90; j <= 180; j += 3) {
            double n8 = j * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n8) * radius * -1.0, y2 - radius + Math.cos(n8) * radius * -1.0);
         }

         if (x2 - x >= 4.5) {
            for (int k = 0; k <= 90; k++) {
               double n9 = k * (float) (Math.PI / 180.0);
               GL11.glVertex2d(x2 - radius + Math.sin(n9) * radius, y2 - radius + Math.cos(n9) * radius);
            }

            for (int l = 90; l <= 180; l++) {
               double n10 = l * (float) (Math.PI / 180.0);
               GL11.glVertex2d(x2 - radius + Math.sin(n10) * radius, y + radius + Math.cos(n10) * radius);
            }
         }

         GL11.glEnd();
         GL11.glEnable(3553);
         GL11.glDisable(3042);
         GL11.glDisable(2848);
         GL11.glEnable(3553);
         GL11.glScaled(2.0, 2.0, 2.0);
         GL11.glPopAttrib();
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      }
   }

   public static void drawRectangleGL(float x, float y, float x2, float y2, int color) {
      GL11.glPushMatrix();
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(3553);
      glColor(color);
      GL11.glBegin(7);
      GL11.glVertex2f(x, y);
      GL11.glVertex2f(x, y2);
      GL11.glVertex2f(x2, y2);
      GL11.glVertex2f(x2, y);
      GL11.glEnd();
      GL11.glEnable(3553);
      GL11.glDisable(3042);
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glPopMatrix();
   }

   public static void drawRoundedGradientRect(float x, float y, float x2, float y2, float radius, int n6, int n7, int n8, int n9) {
      if (!(x2 <= x)) {
         float width = x2 - x;
         if (width < 3.0F) {
            radius = Math.min(radius, width / 2.0F);
         }

         GL11.glEnable(3042);
         GL11.glDisable(3553);
         GL11.glBlendFunc(770, 771);
         GL11.glEnable(2848);
         GL11.glShadeModel(7425);
         GL11.glPushAttrib(0);
         GL11.glScaled(0.5, 0.5, 0.5);
         x = (float)(x * 2.0);
         y = (float)(y * 2.0);
         x2 = (float)(x2 * 2.0);
         y2 = (float)(y2 * 2.0);
         GL11.glEnable(3042);
         GL11.glDisable(3553);
         glColor(n6);
         GL11.glEnable(2848);
         GL11.glShadeModel(7425);
         GL11.glBegin(9);

         for (int i = 0; i <= 90; i += 3) {
            double n10 = i * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n10) * radius * -1.0, y + radius + Math.cos(n10) * radius * -1.0);
         }

         glColor(n7);

         for (int j = 90; j <= 180; j += 3) {
            double n11 = j * (float) (Math.PI / 180.0);
            GL11.glVertex2d(x + radius + Math.sin(n11) * radius * -1.0, y2 - radius + Math.cos(n11) * radius * -1.0);
         }

         if (x2 - x >= 4.5) {
            glColor(n8);

            for (int k = 0; k <= 90; k += 3) {
               double n12 = k * (float) (Math.PI / 180.0);
               GL11.glVertex2d(x2 - radius + Math.sin(n12) * radius, y2 - radius + Math.cos(n12) * radius);
            }

            glColor(n9);

            for (int l = 90; l <= 180; l += 3) {
               double n13 = l * (float) (Math.PI / 180.0);
               GL11.glVertex2d(x2 - radius + Math.sin(n13) * radius, y + radius + Math.cos(n13) * radius);
            }
         }

         GL11.glEnd();
         GL11.glEnable(3553);
         GL11.glDisable(3042);
         GL11.glDisable(2848);
         GL11.glDisable(3042);
         GL11.glEnable(3553);
         GL11.glScaled(2.0, 2.0, 2.0);
         GL11.glPopAttrib();
         GL11.glEnable(3553);
         GL11.glDisable(3042);
         GL11.glDisable(2848);
         GL11.glShadeModel(7424);
      }
   }

   public static int setAlpha(int rgb, double alpha) {
      if (alpha < 0.0 || alpha > 1.0) {
         alpha = 0.5;
      }

      int red = rgb >> 16 & 0xFF;
      int green = rgb >> 8 & 0xFF;
      int blue = rgb & 0xFF;
      int alphaInt = (int)(alpha * 255.0);
      return alphaInt << 24 | red << 16 | green << 8 | blue;
   }
}
