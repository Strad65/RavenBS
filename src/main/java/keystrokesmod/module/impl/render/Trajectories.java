package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemSnowball;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class Trajectories extends Module {
   private ButtonSetting autoScale;
   private ButtonSetting disableUnchargedBow;
   private ButtonSetting highlightEntities;
   private ButtonSetting shortenLine;

   public Trajectories() {
      super("Trajectories", Module.category.render);
      this.registerSetting(this.autoScale = new ButtonSetting("Auto-scale", true));
      this.registerSetting(this.disableUnchargedBow = new ButtonSetting("Disable uncharged bow", true));
      this.registerSetting(this.highlightEntities = new ButtonSetting("Highlight on entity", true));
      this.registerSetting(this.shortenLine = new ButtonSetting("Shorten line", false));
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (Utils.nullCheck() && mc.thePlayer.getHeldItem() != null) {
         Item item = mc.thePlayer.getHeldItem().getItem();
         boolean usingBow = item instanceof ItemBow;
         if (usingBow || item instanceof ItemSnowball || item instanceof ItemEgg || item instanceof ItemEnderPearl) {
            if (!usingBow || !this.disableUnchargedBow.isToggled() || mc.thePlayer.isUsingItem()) {
               float yaw = (float)Math.toRadians(mc.thePlayer.rotationYaw);
               float pitch = (float)Math.toRadians(mc.thePlayer.rotationPitch);
               double arrowPosX = mc.thePlayer.lastTickPosX
                  + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * e.partialTicks
                  - MathHelper.cos(yaw) * 0.16F;
               double arrowPosY = mc.thePlayer.lastTickPosY
                  + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * e.partialTicks
                  + mc.thePlayer.getEyeHeight()
                  - 0.1;
               double arrowPosZ = mc.thePlayer.lastTickPosZ
                  + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * e.partialTicks
                  - MathHelper.sin(yaw) * 0.16F;
               float arrowMotionFactor = usingBow ? 1.0F : 0.4F;
               float arrowMotionX = -MathHelper.sin(yaw) * MathHelper.cos(pitch) * arrowMotionFactor;
               float arrowMotionY = -MathHelper.sin(pitch) * arrowMotionFactor;
               float arrowMotionZ = MathHelper.cos(yaw) * MathHelper.cos(pitch) * arrowMotionFactor;
               double arrowMotion = Math.sqrt(arrowMotionX * arrowMotionX + arrowMotionY * arrowMotionY + arrowMotionZ * arrowMotionZ);
               arrowMotionX = (float)(arrowMotionX / arrowMotion);
               arrowMotionY = (float)(arrowMotionY / arrowMotion);
               arrowMotionZ = (float)(arrowMotionZ / arrowMotion);
               if (usingBow) {
                  float bowPower = (72000 - mc.thePlayer.getItemInUseCount()) / 20.0F;
                  bowPower = (bowPower * bowPower + bowPower * 2.0F) / 3.0F;
                  if (bowPower > 1.0F) {
                     bowPower = 1.0F;
                  }

                  bowPower *= 3.0F;
                  arrowMotionX *= bowPower;
                  arrowMotionY *= bowPower;
                  arrowMotionZ *= bowPower;
               } else {
                  arrowMotionX = (float)(arrowMotionX * 1.5);
                  arrowMotionY = (float)(arrowMotionY * 1.5);
                  arrowMotionZ = (float)(arrowMotionZ * 1.5);
               }

               GL11.glPushMatrix();
               GL11.glEnable(2848);
               GL11.glBlendFunc(770, 771);
               GL11.glEnable(3042);
               GL11.glDisable(3553);
               GL11.glDisable(2929);
               GL11.glEnable(32925);
               GL11.glDepthMask(false);
               RenderManager renderManager = mc.getRenderManager();
               double gravity = usingBow ? 0.05 : 0.03;
               List<double[]> posList = new ArrayList<>();
               MovingObjectPosition block = null;
               Entity entity = null;
               EnumFacing facingEntity = null;
               EnumFacing facingBlock = null;

               for (int i = 0; i < 750; i++) {
                  posList.add(
                     new double[]{arrowPosX - renderManager.viewerPosX, arrowPosY - renderManager.viewerPosY, arrowPosZ - renderManager.viewerPosZ}
                  );
                  Vec3 arrowVec = new Vec3(arrowPosX, arrowPosY, arrowPosZ);
                  Vec3 arrowVecNext = new Vec3(arrowPosX + arrowMotionX, arrowPosY + arrowMotionY, arrowPosZ + arrowMotionZ);
                  arrowPosX = arrowVecNext.xCoord;
                  arrowPosY = arrowVecNext.yCoord;
                  arrowPosZ = arrowVecNext.zCoord;
                  arrowMotionX = (float)(arrowMotionX * 0.99);
                  float var45 = (float)(arrowMotionY * 0.99);
                  arrowMotionZ = (float)(arrowMotionZ * 0.99);
                  arrowMotionY = (float)(var45 - gravity);
                  double size = 0.5;
                  AxisAlignedBB arrowBox = new AxisAlignedBB(
                        arrowPosX - size, arrowPosY - size, arrowPosZ - size, arrowPosX + size, arrowPosY + size, arrowPosZ + size
                     )
                     .addCoord(arrowMotionX, arrowMotionY, arrowMotionZ)
                     .expand(1.0, 1.0, 1.0);
                  List<Entity> list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.getRenderViewEntity(), arrowBox);
                  double minDistSq = 0.0;

                  for (Entity en : list) {
                     if (en instanceof EntityLivingBase
                        && !(en instanceof EntityArmorStand)
                        && en.canBeCollidedWith()
                        && ((EntityLivingBase)en).deathTime == 0
                        && (!(en instanceof EntityPlayer) || !AntiBot.isBot(en))) {
                        AxisAlignedBB axis = en.getEntityBoundingBox().expand(0.3F, 0.3F, 0.3F);
                        MovingObjectPosition mop = axis.calculateIntercept(arrowVec, arrowVecNext);
                        if (mop != null) {
                           if (minDistSq == 0.0) {
                              entity = en;
                              facingEntity = mop.sideHit;
                           } else {
                              double distSq = arrowVec.squareDistanceTo(mop.hitVec);
                              if (!(distSq >= minDistSq)) {
                                 entity = en;
                                 facingEntity = mop.sideHit;
                                 minDistSq = distSq;
                              }
                           }
                        }
                     }
                  }

                  block = mc.theWorld.rayTraceBlocks(arrowVec, arrowVecNext);
                  if (block != null) {
                     facingBlock = block.sideHit;
                  }

                  if (entity != null || block != null) {
                     break;
                  }
               }

               if (entity != null && block != null) {
                  if (mc.thePlayer.getDistanceSqToEntity(entity) >= mc.thePlayer.getDistanceSqToCenter(block.getBlockPos())) {
                     entity = null;
                     facingEntity = null;
                  } else {
                     block = null;
                     facingBlock = null;
                  }
               }

               EnumFacing facing = facingEntity == null ? facingBlock : facingEntity;
               if (entity != null && this.highlightEntities.isToggled()) {
                  GL11.glColor3d(1.0, 0.0, 0.0);
                  GL11.glLineWidth(2.5F);
               } else {
                  if (facingBlock == EnumFacing.UP) {
                     GL11.glColor3d(0.0, 1.0, 0.0);
                  } else {
                     GL11.glColor3d(1.0, 1.0, 1.0);
                  }

                  GL11.glLineWidth(1.8F);
               }

               GL11.glBegin(3);

               for (int j = 0; j < posList.size(); j++) {
                  if (j != 0 || !this.shortenLine.isToggled()) {
                     double[] pos = posList.get(j);
                     GL11.glVertex3d(pos[0], pos[1], pos[2]);
                  }
               }

               double renderX = arrowPosX - renderManager.viewerPosX;
               double renderY = arrowPosY - renderManager.viewerPosY;
               double renderZ = arrowPosZ - renderManager.viewerPosZ;
               double distSq2 = 0.0;
               if (entity != null) {
                  distSq2 = mc.thePlayer.getDistanceSq(entity.getPosition());
               } else if (block != null) {
                  distSq2 = mc.thePlayer.getDistanceSq(block.getBlockPos());
               }

               if (facing != null) {
                  double size2 = this.autoScale.isToggled() ? Math.min(0.1 * (1.0 + distSq2 / 500.0), 0.5) : 0.1;
                  switch (facing) {
                     case WEST:
                     case EAST:
                        GL11.glVertex3d(renderX, renderY, renderZ);
                        GL11.glVertex3d(renderX, renderY - size2, renderZ - size2);
                        GL11.glVertex3d(renderX, renderY + size2, renderZ + size2);
                        GL11.glVertex3d(renderX, renderY, renderZ);
                        GL11.glVertex3d(renderX, renderY - size2, renderZ + size2);
                        GL11.glVertex3d(renderX, renderY + size2, renderZ - size2);
                        break;
                     case NORTH:
                     case SOUTH:
                        GL11.glVertex3d(renderX, renderY, renderZ);
                        GL11.glVertex3d(renderX - size2, renderY - size2, renderZ);
                        GL11.glVertex3d(renderX + size2, renderY + size2, renderZ);
                        GL11.glVertex3d(renderX, renderY, renderZ);
                        GL11.glVertex3d(renderX + size2, renderY - size2, renderZ);
                        GL11.glVertex3d(renderX - size2, renderY + size2, renderZ);
                        break;
                     case DOWN:
                     case UP:
                        GL11.glVertex3d(renderX, renderY, renderZ);
                        GL11.glVertex3d(renderX - size2, renderY, renderZ - size2);
                        GL11.glVertex3d(renderX + size2, renderY, renderZ + size2);
                        GL11.glVertex3d(renderX, renderY, renderZ);
                        GL11.glVertex3d(renderX + size2, renderY, renderZ - size2);
                        GL11.glVertex3d(renderX - size2, renderY, renderZ + size2);
                  }
               }

               GL11.glEnd();
               GL11.glDisable(3042);
               GL11.glEnable(3553);
               GL11.glEnable(2929);
               GL11.glDisable(32925);
               GL11.glDepthMask(true);
               GL11.glDisable(2848);
               GL11.glPopMatrix();
            }
         }
      }
   }
}
