package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderPlayerEvent.Post;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class PlayerESP extends Module {
   public SliderSetting red;
   public SliderSetting green;
   public SliderSetting blue;
   public ButtonSetting teamColor;
   public ButtonSetting rainbow;
   public GroupSetting espTypes;
   private ButtonSetting twoD;
   private ButtonSetting box;
   private ButtonSetting healthBar;
   public ButtonSetting outline;
   private ButtonSetting shaded;
   private ButtonSetting skeleton;
   private ButtonSetting ring;
   public ButtonSetting redOnDamage;
   public ButtonSetting renderSelf;
   public ButtonSetting showInvis;
   private int rgb_c = 0;
   private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);
   private Map<EntityLivingBase, Integer> renderAsTwoD = new HashMap<>();

   public PlayerESP() {
      super("PlayerESP", Module.category.render, 0);
      this.registerSetting(this.espTypes = new GroupSetting("Types"));
      this.registerSetting(this.twoD = new ButtonSetting(this.espTypes, "2D", false));
      this.registerSetting(this.box = new ButtonSetting(this.espTypes, "Box", false));
      this.registerSetting(this.outline = new ButtonSetting(this.espTypes, "Outline", false));
      this.registerSetting(this.ring = new ButtonSetting(this.espTypes, "Ring", false));
      this.registerSetting(this.shaded = new ButtonSetting(this.espTypes, "Shaded", false));
      this.registerSetting(this.skeleton = new ButtonSetting(this.espTypes, "Skeleton", false));
      this.registerSetting(this.red = new SliderSetting("Red", 0.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.green = new SliderSetting("Green", 255.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.blue = new SliderSetting("Blue", 0.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.rainbow = new ButtonSetting("Rainbow", false));
      this.registerSetting(this.healthBar = new ButtonSetting("Health bar", true));
      this.registerSetting(this.redOnDamage = new ButtonSetting("Red on damage", true));
      this.registerSetting(this.renderSelf = new ButtonSetting("Render self", false));
      this.registerSetting(this.teamColor = new ButtonSetting("Team color", false));
      this.registerSetting(this.showInvis = new ButtonSetting("Show invis", true));
   }

   @Override
   public void guiUpdate() {
      this.rgb_c = new Color((int)this.red.getInput(), (int)this.green.getInput(), (int)this.blue.getInput()).getRGB();
   }

   @SubscribeEvent
   public void onRenderPlayerEvent(Post e) {
      if (this.skeleton.isToggled() && e.entityPlayer != null && Utils.nullCheck()) {
         EntityPlayer player = e.entityPlayer;
         if (player != mc.thePlayer || this.renderSelf.isToggled() && mc.gameSettings.thirdPersonView > 0) {
            if (player.deathTime != 0) {
               return;
            }

            if (!this.showInvis.isToggled() && player.isInvisible()) {
               return;
            }

            if (mc.thePlayer != player && AntiBot.isBot(player)) {
               return;
            }

            int rgb = this.rainbow.isToggled() ? Utils.getChroma(2L, 0L) : this.rgb_c;
            if (this.teamColor.isToggled()) {
               rgb = Utils.getColorFromEntity(player);
            }

            this.renderSkeleton(e.entityPlayer, e.renderer.getMainModel(), rgb, e.partialRenderTick);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderWorld(RenderWorldLastEvent e) {
      this.renderAsTwoD.clear();
      if (Utils.nullCheck()) {
         int rgb = this.rainbow.isToggled() ? Utils.getChroma(2L, 0L) : this.rgb_c;
         if (keystrokesmod.Raven.debug) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
               if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                  if (this.teamColor.isToggled()) {
                     rgb = Utils.getColorFromEntity(entity);
                  }

                  rgb = Utils.mergeAlpha(rgb, 255);
                  this.render(entity, rgb);
                  this.renderAsTwoD.put((EntityLivingBase)entity, rgb);
               }
            }

            return;
         }

         EntityPlayer selfPlayer = (EntityPlayer)(Freecam.freeEntity == null ? mc.thePlayer : Freecam.freeEntity);

         for (EntityPlayer player : mc.theWorld.playerEntities) {
            if ((player != selfPlayer || this.renderSelf.isToggled() && mc.gameSettings.thirdPersonView > 0)
               && player.deathTime == 0
               && (this.showInvis.isToggled() || !player.isInvisible())
               && (selfPlayer == player || !AntiBot.isBot(player))) {
               if (this.teamColor.isToggled()) {
                  rgb = Utils.getColorFromEntity(player);
               }

               rgb = Utils.mergeAlpha(rgb, 255);
               this.render(player, rgb);
               this.renderAsTwoD.put(player, rgb);
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onRenderTwo2D(RenderWorldLastEvent e) {
      if (Utils.nullCheck() && this.twoD.isToggled()) {
         for (Entry<EntityLivingBase, Integer> entry : this.renderAsTwoD.entrySet()) {
            this.renderTwoD(entry.getKey(), entry.getValue(), 0.0, e.partialTicks);
         }
      }
   }

   private void render(Entity en, int rgb) {
      if (this.box.isToggled()) {
         RenderUtils.renderEntity(en, 1, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
      }

      if (this.shaded.isToggled() && (ModuleManager.murderMystery == null || !ModuleManager.murderMystery.isEnabled() || ModuleManager.murderMystery.isEmpty())
         )
       {
         RenderUtils.renderEntity(en, 2, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
      }

      if (this.healthBar.isToggled()) {
         RenderUtils.renderEntity(en, 4, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
      }

      if (this.ring.isToggled()) {
         RenderUtils.renderEntity(en, 6, 0.0, 0.0, rgb, this.redOnDamage.isToggled());
      }
   }

   public void renderTwoD(EntityLivingBase en, int rgb, double expand, float partialTicks) {
      if (RenderUtils.isInViewFrustum(en)) {
         ((IAccessorEntityRenderer)mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft)mc).getTimer().renderPartialTicks, 0);
         ScaledResolution scaledResolution = new ScaledResolution(mc);
         double playerX = en.lastTickPosX + (en.posX - en.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
         double playerY = en.lastTickPosY + (en.posY - en.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
         double playerZ = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
         AxisAlignedBB bbox = en.getEntityBoundingBox().expand(0.1 + expand, 0.1 + expand, 0.1 + expand);
         AxisAlignedBB axis = new AxisAlignedBB(
            bbox.minX - en.posX + playerX,
            bbox.minY - en.posY + playerY,
            bbox.minZ - en.posZ + playerZ,
            bbox.maxX - en.posX + playerX,
            bbox.maxY - en.posY + playerY,
            bbox.maxZ - en.posZ + playerZ
         );
         Vec3[] corners = new Vec3[]{
            new Vec3(axis.minX, axis.minY, axis.minZ),
            new Vec3(axis.minX, axis.minY, axis.maxZ),
            new Vec3(axis.minX, axis.maxY, axis.minZ),
            new Vec3(axis.minX, axis.maxY, axis.maxZ),
            new Vec3(axis.maxX, axis.minY, axis.minZ),
            new Vec3(axis.maxX, axis.minY, axis.maxZ),
            new Vec3(axis.maxX, axis.maxY, axis.minZ),
            new Vec3(axis.maxX, axis.maxY, axis.maxZ)
         };
         double minX = Double.MAX_VALUE;
         double minY = Double.MAX_VALUE;
         double maxX = Double.MIN_VALUE;
         double maxY = Double.MIN_VALUE;
         boolean isInView = false;

         for (Vec3 corner : corners) {
            double x = corner.xCoord;
            double y = corner.yCoord;
            double z = corner.zCoord;
            Vec3 screenVec = RenderUtils.convertTo2D(scaledResolution.getScaleFactor(), x, y, z);
            if (screenVec != null && !(screenVec.zCoord >= 1.0003684) && !(screenVec.zCoord <= 0.0)) {
               isInView = true;
               double screenX = screenVec.xCoord;
               double screenY = screenVec.yCoord;
               if (screenX < minX) {
                  minX = screenX;
               }

               if (screenY < minY) {
                  minY = screenY;
               }

               if (screenX > maxX) {
                  maxX = screenX;
               }

               if (screenY > maxY) {
                  maxY = screenY;
               }
            }
         }

         if (isInView) {
            mc.entityRenderer.setupOverlayRendering();
            ScaledResolution res = new ScaledResolution(mc);
            int screenWidth = res.getScaledWidth();
            int screenHeight = res.getScaledHeight();
            minX = Math.max(0.0, minX);
            minY = Math.max(0.0, minY);
            maxX = Math.min(screenWidth, maxX);
            maxY = Math.min(screenHeight, maxY);
            float red = (rgb >> 16 & 0xFF) / 255.0F;
            float green = (rgb >> 8 & 0xFF) / 255.0F;
            float blue = (rgb & 0xFF) / 255.0F;
            GL11.glPushMatrix();
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glEnable(2848);
            GL11.glLineWidth(1.0F);
            GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
            GL11.glBegin(2);
            GL11.glVertex2d(minX, minY);
            GL11.glVertex2d(maxX, minY);
            GL11.glVertex2d(maxX, maxY);
            GL11.glVertex2d(minX, maxY);
            GL11.glEnd();
            GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.4F);
            GL11.glBegin(2);
            GL11.glVertex2d(minX + 1.0, minY + 1.0);
            GL11.glVertex2d(maxX - 1.0, minY + 1.0);
            GL11.glVertex2d(maxX - 1.0, maxY - 1.0);
            GL11.glVertex2d(minX + 1.0, maxY - 1.0);
            GL11.glEnd();
            GL11.glColor4f(red, green, blue, 1.0F);
            GL11.glBegin(2);
            GL11.glVertex2d(minX + 0.5, minY + 0.5);
            GL11.glVertex2d(maxX - 0.5, minY + 0.5);
            GL11.glVertex2d(maxX - 0.5, maxY - 0.5);
            GL11.glVertex2d(minX + 0.5, maxY - 0.5);
            GL11.glEnd();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(2848);
            GL11.glPopMatrix();
         }
      }
   }

   public void renderSkeleton(EntityPlayer player, ModelBiped modelBiped, int color, float partialTicks) {
      GL11.glPushMatrix();
      GL11.glDisable(2929);
      double viewerPosX = mc.getRenderManager().viewerPosX;
      double viewerPosY = mc.getRenderManager().viewerPosY;
      double viewerPosZ = mc.getRenderManager().viewerPosZ;
      double posX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks - viewerPosX;
      double posY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - viewerPosY;
      double posZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks - viewerPosZ;
      boolean wasBlendEnabled = GL11.glIsEnabled(3042);
      GL11.glPushMatrix();
      GL11.glBlendFunc(770, 771);
      if (!wasBlendEnabled) {
         GL11.glEnable(3042);
      }

      GL11.glBlendFunc(770, 771);
      GL11.glColor4f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F);
      GL11.glDisable(2896);
      GL11.glEnable(2848);
      GL11.glDisable(3553);
      GL11.glTranslated(posX, posY, posZ);
      float distance = mc.thePlayer.getDistanceToEntity(player);
      float computedLineWidth = 4.0F * ((100.0F - Math.min(distance, 100.0F)) / 100.0F);
      float lineWidth = Math.max(1.0F, computedLineWidth);
      GL11.glLineWidth(lineWidth);
      boolean isSneaking = player.isSneaking();
      float legHeight = isSneaking ? 0.6F : 0.75F;
      double legOffsetZ = isSneaking ? -0.2 : 0.0;
      GL11.glRotatef(player.renderYawOffset, 0.0F, -999.0F, 0.0F);
      GL11.glTranslated(-0.15, legHeight, legOffsetZ);
      float rightLegRotX = modelBiped.bipedRightLeg.rotateAngleX * (float) (180.0 / Math.PI);
      float rightLegRotY = modelBiped.bipedRightLeg.rotateAngleY * (float) (180.0 / Math.PI);
      float rightLegRotZ = modelBiped.bipedRightLeg.rotateAngleZ * (float) (180.0 / Math.PI);
      GL11.glRotatef(rightLegRotX, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(-rightLegRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-rightLegRotZ, 0.0F, 0.0F, 1.0F);
      this.drawLine(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
      GL11.glRotatef(rightLegRotZ, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(rightLegRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-rightLegRotX, 1.0F, 0.0F, 0.0F);
      GL11.glTranslated(0.3, 0.0, 0.0);
      float leftLegRotX = modelBiped.bipedLeftLeg.rotateAngleX * (float) (180.0 / Math.PI);
      float leftLegRotY = modelBiped.bipedLeftLeg.rotateAngleY * (float) (180.0 / Math.PI);
      float leftLegRotZ = modelBiped.bipedLeftLeg.rotateAngleZ * (float) (180.0 / Math.PI);
      GL11.glRotatef(leftLegRotX, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(-leftLegRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-leftLegRotZ, 0.0F, 0.0F, 1.0F);
      this.drawLine(0.0, 0.0, 0.0, 0.0, -legHeight, 0.0);
      GL11.glRotatef(leftLegRotZ, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(leftLegRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-leftLegRotX, 1.0F, 0.0F, 0.0F);
      GL11.glTranslated(-0.15, 0.0, 0.0);
      this.drawLine(0.15, 0.0, 0.0, -0.15, 0.0, 0.0);
      if (player.isSneaking()) {
         GL11.glRotatef(20.0F, 1.0F, 0.0F, 0.0F);
      }

      this.drawLine(0.0, 0.0, 0.0, 0.0, 0.65, 0.0);
      GL11.glTranslated(0.0, 0.65, 0.0);
      this.drawLine(0.35, 0.0, 0.0, -0.35, 0.0, 0.0);
      GL11.glTranslated(-0.35, 0.0, 0.0);
      float rightArmRotX = modelBiped.bipedRightArm.rotateAngleX * (float) (180.0 / Math.PI);
      float rightArmRotY = modelBiped.bipedRightArm.rotateAngleY * (float) (180.0 / Math.PI);
      float rightArmRotZ = modelBiped.bipedRightArm.rotateAngleZ * (float) (180.0 / Math.PI);
      GL11.glRotatef(rightArmRotX, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(-rightArmRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-rightArmRotZ, 0.0F, 0.0F, 1.0F);
      this.drawLine(0.0, 0.0, 0.0, 0.0, -0.6, 0.0);
      GL11.glRotatef(rightArmRotZ, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(rightArmRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-rightArmRotX, 1.0F, 0.0F, 0.0F);
      GL11.glTranslated(0.7, 0.0, 0.0);
      float leftArmRotX = modelBiped.bipedLeftArm.rotateAngleX * (float) (180.0 / Math.PI);
      float leftArmRotY = modelBiped.bipedLeftArm.rotateAngleY * (float) (180.0 / Math.PI);
      float leftArmRotZ = modelBiped.bipedLeftArm.rotateAngleZ * (float) (180.0 / Math.PI);
      GL11.glRotatef(leftArmRotX, 1.0F, 0.0F, 0.0F);
      GL11.glRotatef(-leftArmRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-leftArmRotZ, 0.0F, 0.0F, 1.0F);
      this.drawLine(0.0, 0.0, 0.0, 0.0, -0.6, 0.0);
      GL11.glRotatef(leftArmRotZ, 0.0F, 0.0F, 1.0F);
      GL11.glRotatef(leftArmRotY, 0.0F, 1.0F, 0.0F);
      GL11.glRotatef(-leftArmRotX, 1.0F, 0.0F, 0.0F);
      GL11.glTranslated(-0.35, 0.0, 0.0);
      GL11.glRotatef(-player.renderYawOffset, 0.0F, -999.0F, 0.0F);
      double headHeight = 0.4;
      GL11.glRotated(player.rotationYaw, 0.0, -999.0, 0.0);
      GL11.glRotated(player.rotationPitch, 999.0, 0.0, 0.0);
      this.drawLine(0.0, 0.0, 0.0, 0.0, headHeight, 0.0);
      this.drawLine(0.0, headHeight, 0.0, 0.0, headHeight, 0.25);
      GL11.glRotated(player.rotationPitch, 999.0, 0.0, 0.0);
      GL11.glRotated(-player.rotationYaw, 0.0, 999.0, 0.0);
      if (!wasBlendEnabled) {
         GL11.glDisable(3042);
      }

      GL11.glEnable(3553);
      GL11.glDisable(2848);
      GL11.glEnable(2896);
      GL11.glPopMatrix();
      GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
      GL11.glEnable(2929);
      GL11.glPopMatrix();
   }

   private void drawLine(double x1, double y1, double z1, double x2, double y2, double z2) {
      GL11.glBegin(1);
      GL11.glVertex3d(x1, y1, z1);
      GL11.glVertex3d(x2, y2, z2);
      GL11.glEnd();
   }
}
