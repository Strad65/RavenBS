package keystrokesmod.utility;

import com.google.common.base.Predicates;
import java.util.List;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class RotationUtils {
   public static final Minecraft mc = Minecraft.getMinecraft();
   public static float renderPitch;
   public static float prevRenderPitch;
   public static float renderYaw;
   public static float prevRenderYaw;
   public static float[] serverRotations = new float[]{0.0F, 0.0F};
   public static Float[] fakeRotations;
   public static boolean setFakeRotations;

   public static void setFakeRotations(float yaw, float pitch) {
      fakeRotations = new Float[]{yaw, pitch};
      setFakeRotations = true;
   }

   public static void setRenderYaw(float yaw) {
      mc.thePlayer.rotationYawHead = yaw;
      if (Settings.rotateBody.isToggled() && Settings.fullBody.isToggled()) {
         mc.thePlayer.prevRenderYawOffset = prevRenderYaw;
         mc.thePlayer.renderYawOffset = yaw;
      }
   }

   public static float[] getRotations(BlockPos blockPos, float n, float n2) {
      float[] array = getRotations(blockPos);
      return fixRotation(array[0], array[1], n, n2);
   }

   public static float[] getRotationsToBlock(BlockPos blockPos, EnumFacing facing, float yaw, float pitch) {
      float[] array = getRotationsToBlock(blockPos, facing);
      return fixRotation(array[0], array[1], yaw, pitch);
   }

   public static float[] getRotations(BlockPos blockPos) {
      double x = blockPos.getX() + 0.45 - mc.thePlayer.posX;
      double y = blockPos.getY() + 0.45 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      double z = blockPos.getZ() + 0.45 - mc.thePlayer.posZ;
      float angleToBlock = (float)(Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
      float deltaYaw = MathHelper.wrapAngleTo180_float(angleToBlock - mc.thePlayer.rotationYaw);
      float yaw = mc.thePlayer.rotationYaw + deltaYaw;
      double distance = MathHelper.sqrt_double(x * x + z * z);
      float angleToBlockPitch = (float)(-(Math.atan2(y, distance) * (180.0 / Math.PI)));
      float deltaPitch = MathHelper.wrapAngleTo180_float(angleToBlockPitch - mc.thePlayer.rotationPitch);
      float pitch = mc.thePlayer.rotationPitch + deltaPitch;
      pitch = clampPitch(pitch);
      return new float[]{yaw, pitch};
   }

   public static float[] getRotations(double posX, double posY, double posZ) {
      double x = posX + 1.0 - mc.thePlayer.posX;
      double y = posY + 1.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      double z = posZ + 1.0 - mc.thePlayer.posZ;
      float angleToBlock = (float)(Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
      float deltaYaw = MathHelper.wrapAngleTo180_float(angleToBlock - mc.thePlayer.rotationYaw);
      float yaw = mc.thePlayer.rotationYaw + deltaYaw;
      double distance = MathHelper.sqrt_double(x * x + z * z);
      float angleToBlockPitch = (float)(-(Math.atan2(y, distance) * (180.0 / Math.PI)));
      float deltaPitch = MathHelper.wrapAngleTo180_float(angleToBlockPitch - mc.thePlayer.rotationPitch);
      float pitch = mc.thePlayer.rotationPitch + deltaPitch;
      pitch = clampPitch(pitch);
      return new float[]{yaw, pitch};
   }

   public static float[] getRotations(Vec3 vec3) {
      double x = vec3.xCoord + 1.0 - mc.thePlayer.posX;
      double y = vec3.yCoord + 1.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      double z = vec3.zCoord + 1.0 - mc.thePlayer.posZ;
      float angleToBlock = (float)(Math.atan2(z, x) * (180.0 / Math.PI)) - 90.0F;
      float deltaYaw = MathHelper.wrapAngleTo180_float(angleToBlock - mc.thePlayer.rotationYaw);
      float yaw = mc.thePlayer.rotationYaw + deltaYaw;
      double distance = MathHelper.sqrt_double(x * x + z * z);
      float angleToBlockPitch = (float)(-(Math.atan2(y, distance) * (180.0 / Math.PI)));
      float deltaPitch = MathHelper.wrapAngleTo180_float(angleToBlockPitch - mc.thePlayer.rotationPitch);
      float pitch = mc.thePlayer.rotationPitch + deltaPitch;
      pitch = clampPitch(pitch);
      return new float[]{yaw, pitch};
   }

   public static float[] getRotations(Entity entity, float yaw, float pitch) {
      float[] array = getRotations(entity);
      return array == null ? null : fixRotation(array[0], array[1], yaw, pitch);
   }

   public static float[] getRotationsToBlock(BlockPos pos, EnumFacing facing) {
      double diffX = pos.getX() + 0.45 - mc.thePlayer.posX;
      double diffY = pos.getY() + 0.45 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      double diffZ = pos.getZ() + 0.45 - mc.thePlayer.posZ;
      if (facing != null) {
         diffX += facing.getDirectionVec().getX() * 0.5;
         diffY += facing.getDirectionVec().getY() * 0.5;
         diffZ += facing.getDirectionVec().getZ() * 0.5;
      }

      double dist = MathHelper.sqrt_double(diffX * diffX + diffZ * diffZ);
      float yaw = (float)(Math.atan2(diffZ, diffX) * (float) (180.0 / Math.PI)) - 90.0F;
      float pitch = (float)(-(Math.atan2(diffY, dist) * (float) (180.0 / Math.PI)));
      return new float[]{
         mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw),
         clampPitch(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch))
      };
   }

   public static double distanceFromYaw(Entity entity, boolean b) {
      return Math.abs(
         MathHelper.wrapAngleTo180_double(
            i(entity.posX, entity.posZ) - (b && PreMotionEvent.setRenderYaw() ? renderYaw : mc.thePlayer.rotationYaw)
         )
      );
   }

   public static float i(double n, double n2) {
      return (float)(Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * (float) (180.0 / Math.PI) * -1.0);
   }

   public static boolean isPossibleToHit(Entity target, double reach, float[] rotations) {
      Vec3 eyePosition = mc.thePlayer.getPositionEyes(1.0F);
      float yaw = rotations[0];
      float pitch = rotations[1];
      float radianYaw = -yaw * (float) (Math.PI / 180.0) - (float) Math.PI;
      float radianPitch = -pitch * (float) (Math.PI / 180.0);
      float cosYaw = MathHelper.cos(radianYaw);
      float sinYaw = MathHelper.sin(radianYaw);
      float cosPitch = -MathHelper.cos(radianPitch);
      float sinPitch = MathHelper.sin(radianPitch);
      Vec3 lookVector = new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
      double lookVecX = lookVector.xCoord * reach;
      double lookVecY = lookVector.yCoord * reach;
      double lookVecZ = lookVector.zCoord * reach;
      Vec3 endPosition = eyePosition.addVector(lookVecX, lookVecY, lookVecZ);
      Entity renderViewEntity = mc.getRenderViewEntity();
      AxisAlignedBB expandedBox = renderViewEntity.getEntityBoundingBox().addCoord(lookVecX, lookVecY, lookVecZ).expand(1.0, 1.0, 1.0);

      for (Entity entity : mc.theWorld.getEntitiesWithinAABBExcludingEntity(renderViewEntity, expandedBox)) {
         if (entity == target && entity.canBeCollidedWith()) {
            float borderSize = entity.getCollisionBorderSize();
            AxisAlignedBB entityBox = entity.getEntityBoundingBox().expand(borderSize, borderSize, borderSize);
            MovingObjectPosition intercept = entityBox.calculateIntercept(eyePosition, endPosition);
            return intercept != null;
         }
      }

      return false;
   }

   public static boolean inRange(BlockPos blockPos, double n) {
      float[] array = getRotations(blockPos);
      Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0F);
      float n2 = -array[0] * (float) (Math.PI / 180.0);
      float n3 = -array[1] * (float) (Math.PI / 180.0);
      float cos = MathHelper.cos(n2 - (float) Math.PI);
      float sin = MathHelper.sin(n2 - (float) Math.PI);
      float n4 = -MathHelper.cos(n3);
      Vec3 vec3 = new Vec3(sin * n4, MathHelper.sin(n3), cos * n4);
      Block block = BlockUtils.getBlock(blockPos);
      IBlockState blockState = BlockUtils.getBlockState(blockPos);
      if (block != null && blockState != null) {
         AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, blockPos, blockState);
         if (boundingBox != null) {
            Vec3 targetVec = getPositionEyes.addVector(vec3.xCoord * n, vec3.yCoord * n, vec3.zCoord * n);
            MovingObjectPosition intercept = boundingBox.calculateIntercept(getPositionEyes, targetVec);
            if (intercept != null) {
               return true;
            }
         }
      }

      return false;
   }

   public static float[] getRotations(Entity entity) {
      return getRotations(entity, RotationUtils.PLAYER_OFFSETS.NONE);
   }

   public static float[] getRotations(Entity entity, RotationUtils.PLAYER_OFFSETS playerOffset) {
      if (entity == null) {
         return null;
      }

      double n = entity.posX - mc.thePlayer.posX;
      double n2 = entity.posZ - mc.thePlayer.posZ;
      double n3;
      if (entity instanceof EntityLivingBase) {
         EntityLivingBase entityLivingBase = (EntityLivingBase)entity;
         n3 = entityLivingBase.posY
            + playerOffset.getHeightOffset(entityLivingBase) * 0.9
            - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      } else {
         n3 = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0
            - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      }

      return new float[]{
         mc.thePlayer.rotationYaw
            + MathHelper.wrapAngleTo180_float((float)(Math.atan2(n2, n) * (float) (180.0 / Math.PI)) - 90.0F - mc.thePlayer.rotationYaw),
         clampPitch(
            mc.thePlayer.rotationPitch
               + MathHelper.wrapAngleTo180_float(
                  (float)(-(Math.atan2(n3, MathHelper.sqrt_double(n * n + n2 * n2)) * (float) (180.0 / Math.PI))) - mc.thePlayer.rotationPitch
               )
               + 3.0F
         )
      };
   }

   public static float[] getRotationsPredicated(Entity entity, int ticks) {
      if (entity == null) {
         return null;
      }

      if (ticks == 0) {
         return getRotations(entity);
      }

      double posX = entity.posX;
      double posY = entity.posY;
      double posZ = entity.posZ;
      double n2 = posX - entity.lastTickPosX;
      double n3 = posZ - entity.lastTickPosZ;

      for (int i = 0; i < ticks; i++) {
         posX += n2;
         posZ += n3;
      }

      double n4 = posX - mc.thePlayer.posX;
      double n5;
      if (entity instanceof EntityLivingBase) {
         n5 = posY + entity.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      } else {
         n5 = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0
            - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      }

      double n6 = posZ - mc.thePlayer.posZ;
      return new float[]{
         applyVanilla(
            mc.thePlayer.rotationYaw
               + MathHelper.wrapAngleTo180_float((float)(Math.atan2(n6, n4) * (float) (180.0 / Math.PI)) - 90.0F - mc.thePlayer.rotationYaw)
         ),
         clampPitch(
            mc.thePlayer.rotationPitch
               + MathHelper.wrapAngleTo180_float(
                  (float)(-(Math.atan2(n5, MathHelper.sqrt_double(n4 * n4 + n6 * n6)) * (float) (180.0 / Math.PI))) - mc.thePlayer.rotationPitch
               )
               + 3.0F
         )
      };
   }

   public static float clampPitch(float n) {
      return MathHelper.clamp_float(n, -90.0F, 90.0F);
   }

   public static float[] fixRotation(float targetYaw, float targetPitch, float yaw, float pitch) {
      float n5 = targetYaw - yaw;
      float abs = Math.abs(n5);
      float n7 = targetPitch - pitch;
      float n8 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
      double n9 = n8 * n8 * n8 * 1.2;
      float n10 = (float)(Math.round(n5 / n9) * n9);
      float n11 = (float)(Math.round(n7 / n9) * n9);
      targetYaw = yaw + n10;
      targetPitch = pitch + n11;
      if (abs >= 1.0F) {
         int n12 = (int)Settings.randomYawFactor.getInput();
         if (n12 != 0) {
            int n13 = n12 * 100 + Utils.randomizeInt(-30, 30);
            targetYaw = (float)(targetYaw + Utils.randomizeInt(-n13, n13) / 100.0);
         }
      } else if (abs <= 0.04) {
         targetYaw = (float)(targetYaw + (abs > 0.0F ? 0.01 : -0.01));
      }

      return new float[]{targetYaw, clampPitch(targetPitch)};
   }

   public static float angle(double n, double n2) {
      return (float)(Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * (float) (180.0 / Math.PI) * -1.0);
   }

   public static MovingObjectPosition rayCast(double distance, float yaw, float pitch, boolean collisionCheck) {
      Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0F);
      float n4 = -yaw * (float) (Math.PI / 180.0);
      float n5 = -pitch * (float) (Math.PI / 180.0);
      float cos = MathHelper.cos(n4 - (float) Math.PI);
      float sin = MathHelper.sin(n4 - (float) Math.PI);
      float n6 = -MathHelper.cos(n5);
      Vec3 vec3 = new Vec3(sin * n6, MathHelper.sin(n5), cos * n6);
      return mc.theWorld
         .rayTraceBlocks(
            getPositionEyes,
            getPositionEyes.addVector(vec3.xCoord * distance, vec3.yCoord * distance, vec3.zCoord * distance),
            true,
            collisionCheck,
            true
         );
   }

   public static MovingObjectPosition rayTraceCustom(double blockReachDistance, float yaw, float pitch) {
      Vec3 vec3 = mc.thePlayer.getPositionEyes(1.0F);
      Vec3 vec31 = getVectorForRotation(pitch, yaw);
      Vec3 vec32 = vec3.addVector(
         vec31.xCoord * blockReachDistance, vec31.yCoord * blockReachDistance, vec31.zCoord * blockReachDistance
      );
      return mc.theWorld.rayTraceBlocks(vec3, vec32, false, false, true);
   }

   public static Vec3 getVectorForRotation(float pitch, float yaw) {
      float f = MathHelper.cos(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f1 = MathHelper.sin(-yaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f2 = -MathHelper.cos(-pitch * (float) (Math.PI / 180.0));
      float f3 = MathHelper.sin(-pitch * (float) (Math.PI / 180.0));
      return new Vec3(f1 * f2, f3, f * f2);
   }

   public static float applyVanilla(float yaw, boolean stop) {
      if (stop) {
         return yaw;
      }

      int scaleFactor = (int)Math.floor(serverRotations[0] / 360.0F);
      float unwrappedYaw = yaw + 360 * scaleFactor;
      if (unwrappedYaw < serverRotations[0] - 180.0F) {
         unwrappedYaw += 360.0F;
      } else if (unwrappedYaw > serverRotations[0] + 180.0F) {
         unwrappedYaw -= 360.0F;
      }

      float deltaYaw = unwrappedYaw - serverRotations[0];
      return serverRotations[0] + deltaYaw;
   }

   public static MovingObjectPosition rayTrace(double range, float partialTicks, float[] rotations, EntityLivingBase ignoreCollision) {
      if (ignoreCollision != null) {
         MovingObjectPosition target = rayTraceIgnore(range, partialTicks, rotations, ignoreCollision);
         if (target != null) {
            return target;
         }
      }

      Entity targetEntity = null;
      double d0 = range;
      if (rotations == null) {
         rotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
      }

      MovingObjectPosition hitObject = rayTraceCustom(d0, rotations[0], rotations[1]);
      double distanceTo = d0;
      Vec3 vec3 = mc.thePlayer.getPositionEyes(partialTicks);
      if (mc.playerController.extendedReach()) {
         d0 = 6.0;
         distanceTo = 6.0;
      }

      if (hitObject != null) {
         distanceTo = hitObject.hitVec.distanceTo(vec3);
      }

      Vec3 vec31 = getVectorForRotation(rotations[1], rotations[0]);
      Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
      Vec3 vec33 = null;
      float f = 1.0F;
      List<Entity> list = mc.theWorld
         .getEntitiesInAABBexcluding(
            mc.thePlayer,
            mc.thePlayer.getEntityBoundingBox().addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand(f, f, f),
            Predicates.and(EntitySelectors.NOT_SPECTATING, Entity::canBeCollidedWith)
         );
      double d2 = distanceTo;

      for (int j = 0; j < list.size(); j++) {
         Entity entity1 = list.get(j);
         float f1 = entity1.getCollisionBorderSize();
         AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().expand(f1, f1, f1);
         MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);
         if (axisalignedbb.isVecInside(vec3)) {
            if (d2 >= 0.0) {
               targetEntity = entity1;
               vec33 = movingobjectposition == null ? vec3 : movingobjectposition.hitVec;
               d2 = 0.0;
            }
         } else if (movingobjectposition != null) {
            double d3 = vec3.distanceTo(movingobjectposition.hitVec);
            if (d3 < d2 || d2 == 0.0) {
               if (entity1 != mc.thePlayer.ridingEntity || mc.thePlayer.canRiderInteract()) {
                  targetEntity = entity1;
                  vec33 = movingobjectposition.hitVec;
                  d2 = d3;
               } else if (d2 == 0.0) {
                  targetEntity = entity1;
                  vec33 = movingobjectposition.hitVec;
               }
            }
         }
      }

      return targetEntity != null && d2 < distanceTo ? new MovingObjectPosition(targetEntity, vec33) : null;
   }

   public static MovingObjectPosition rayTraceIgnore(double range, float partialTicks, float[] rotations, EntityLivingBase ignoreCollision) {
      MovingObjectPosition blockHit = rayTraceCustom(range, rotations[0], rotations[1]);
      Vec3 start = mc.thePlayer.getPositionEyes(partialTicks);
      double blockDistance = range;
      if (blockHit != null) {
         blockDistance = blockHit.hitVec.distanceTo(start);
      }

      if (ignoreCollision != null) {
         if (rotations == null) {
            rotations = new float[]{mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch};
         }

         Vec3 lookVec = getVectorForRotation(rotations[1], rotations[0]);
         Vec3 end = start.addVector(lookVec.xCoord * range, lookVec.yCoord * range, lookVec.zCoord * range);
         float f1 = ignoreCollision.getCollisionBorderSize();
         AxisAlignedBB aabb = ignoreCollision.getEntityBoundingBox().expand(f1, f1, f1);
         MovingObjectPosition ignoreMOP = aabb.calculateIntercept(start, end);
         if (aabb.isVecInside(start)) {
            return new MovingObjectPosition(ignoreCollision, start);
         }

         if (ignoreMOP != null) {
            double ignoreDist = start.distanceTo(ignoreMOP.hitVec);
            if (ignoreDist < blockDistance) {
               return new MovingObjectPosition(ignoreCollision, ignoreMOP.hitVec);
            }
         }
      }

      return blockHit != null ? blockHit : null;
   }

   public static float applyVanilla(float yaw) {
      return applyVanilla(yaw, false);
   }

   public enum PLAYER_OFFSETS {
      EYE,
      CHEST,
      FOOT,
      NONE;

      public double getHeightOffset(Entity entity) {
         switch (this) {
            case NONE:
            case EYE:
               return entity.getEyeHeight();
            case CHEST:
               return entity.height / 2.0F;
            case FOOT:
               return 0.0;
            default:
               return entity.getEyeHeight();
         }
      }
   }
}
