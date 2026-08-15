package keystrokesmod.script.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;

public class Entity {
   public net.minecraft.entity.Entity entity;
   public String type;
   public int entityId;
   public boolean isLiving;
   public boolean isPlayer;
   public boolean isUser;
   private static HashMap<Integer, Entity> cache = new HashMap<>();

   public Entity(net.minecraft.entity.Entity entity) {
      this.entity = entity;
      if (entity != null) {
         this.type = entity.getClass().getSimpleName();
         this.entityId = entity.getEntityId();
         this.isLiving = entity instanceof EntityLivingBase;
         this.isPlayer = entity instanceof EntityPlayer;
         if (this.isPlayer
            && Minecraft.getMinecraft().thePlayer != null
            && entity.getUniqueID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
            this.isUser = true;
         }
      }
   }

   public static Entity convert(net.minecraft.entity.Entity entity) {
      if (entity == null) {
         return null;
      }

      int id = entity.getEntityId() + System.identityHashCode(entity);
      Entity cachedEntity = cache.get(id);
      if (cachedEntity == null) {
         cachedEntity = new Entity(entity);
         cache.put(id, cachedEntity);
      }

      return cachedEntity;
   }

   public static void clearCache() {
      cache.clear();
   }

   public boolean allowEditing() {
      return !(this.entity instanceof EntityPlayer) ? false : ((EntityPlayer)this.entity).capabilities.allowEdit;
   }

   public double distanceTo(Vec3 position) {
      return this.entity.getDistance(position.x, position.y, position.z);
   }

   public double distanceToSq(Vec3 position) {
      return this.entity.getDistanceSq(position.x, position.y, position.z);
   }

   public double distanceToGround() {
      return Utils.distanceToGround(this.entity);
   }

   public boolean isHoldingBlock() {
      return this.isLiving
         && ((EntityLivingBase)this.entity).getHeldItem() != null
         && ((EntityLivingBase)this.entity).getHeldItem().getItem() instanceof ItemBlock;
   }

   public boolean isHoldingWeapon() {
      return this.isLiving && Utils.holdingWeapon((EntityLivingBase)this.entity);
   }

   public float getAbsorption() {
      return !(this.entity instanceof EntityLivingBase) ? -1.0F : ((EntityLivingBase)this.entity).getAbsorptionAmount();
   }

   public Vec3 getBlockPosition() {
      return new Vec3(this.entity.getPosition().getX(), this.entity.getPosition().getY(), this.entity.getPosition().getZ());
   }

   public String getDisplayName() {
      return this.entity instanceof EntityItem ? ((EntityItem)this.entity).getEntityItem().getDisplayName() : this.entity.getDisplayName().getUnformattedText();
   }

   public Entity getRidingEntity() {
      return convert(this.entity.ridingEntity);
   }

   public Entity getRiddenByEntity() {
      return convert(this.entity.riddenByEntity);
   }

   public Vec3 getServerPosition() {
      return new Vec3(this.entity.serverPosX, this.entity.serverPosY, this.entity.serverPosZ);
   }

   public int getExperienceLevel() {
      return !(this.entity instanceof EntityPlayer) ? 0 : ((EntityPlayer)this.entity).experienceLevel;
   }

   public float getExperience() {
      return !(this.entity instanceof EntityPlayer) ? 0.0F : ((EntityPlayer)this.entity).experience;
   }

   public float getFallDistance() {
      return this.entity.fallDistance;
   }

   public String getUUID() {
      return this.entity.getUniqueID().toString();
   }

   public String getCustomNameTag() {
      return this.entity.getCustomNameTag();
   }

   public double getBPS() {
      if (!this.isLiving) {
         return 0.0;
      }

      double x = this.entity.posX - this.entity.prevPosX;
      double z = this.entity.posZ - this.entity.prevPosZ;
      return Math.sqrt(x * x + z * z) * 20.0;
   }

   public String getFacing() {
      return this.entity.getHorizontalFacing().name();
   }

   public float getHealth() {
      return !(this.entity instanceof EntityLivingBase) ? -1.0F : ((EntityLivingBase)this.entity).getHealth();
   }

   public boolean isSleeping() {
      return this.isPlayer ? ((EntityPlayer)this.entity).isPlayerSleeping() : false;
   }

   public float getEyeHeight() {
      return this.entity.getEyeHeight();
   }

   public float getHeight() {
      return this.entity.height;
   }

   public float getWidth() {
      return this.entity.width;
   }

   public boolean isBurning() {
      return this.entity.isBurning();
   }

   public ItemStack getHeldItem() {
      if (this.entity instanceof EntityItem) {
         net.minecraft.item.ItemStack item = ((EntityItem)this.entity).getEntityItem();
         return item == null ? null : new ItemStack(item, (byte)0);
      }

      if (!(this.entity instanceof EntityLivingBase)) {
         return null;
      }

      net.minecraft.item.ItemStack stack = ((EntityLivingBase)this.entity).getHeldItem();
      return stack == null ? null : new ItemStack(stack, (byte)0);
   }

   public int getHurtTime() {
      return !(this.entity instanceof EntityLivingBase) ? -1 : ((EntityLivingBase)this.entity).hurtTime;
   }

   public boolean isConsuming() {
      return Utils.isConsuming(this.entity);
   }

   public Vec3 getLastPosition() {
      return new Vec3(this.entity.lastTickPosX, this.entity.lastTickPosY, this.entity.lastTickPosZ);
   }

   public float getMaxHealth() {
      return !(this.entity instanceof EntityLivingBase) ? -1.0F : ((EntityLivingBase)this.entity).getMaxHealth();
   }

   public int getMaxHurtTime() {
      return !(this.entity instanceof EntityLivingBase) ? -1 : ((EntityLivingBase)this.entity).maxHurtTime;
   }

   public String getName() {
      return this.entity instanceof EntityItem
         ? ((EntityItem)this.entity).getEntityItem().getItem().getRegistryName().substring(10)
         : this.entity.getName();
   }

   public NetworkPlayer getNetworkPlayer() {
      return NetworkPlayer.convert(Minecraft.getMinecraft().getNetHandler().getPlayerInfo(this.entity.getUniqueID()));
   }

   public float getPitch() {
      return this.entity.rotationPitch;
   }

   public Vec3 getPosition() {
      return this.entity == null ? null : new Vec3(this.entity.posX, this.entity.posY, this.entity.posZ);
   }

   public List<Object[]> getPotionEffects() {
      List<Object[]> potionEffects = new ArrayList<>();
      if (!(this.entity instanceof EntityLivingBase)) {
         return potionEffects;
      }

      for (PotionEffect potionEffect : ((EntityLivingBase)this.entity).getActivePotionEffects()) {
         Object[] potionData = new Object[]{potionEffect.getPotionID(), potionEffect.getEffectName(), potionEffect.getAmplifier(), potionEffect.getDuration()};
         potionEffects.add(potionData);
      }

      return potionEffects;
   }

   public ItemStack getArmorInSlot(int slot) {
      return this.isPlayer && slot >= 0 && slot <= 3 ? ItemStack.convert(((EntityPlayer)this.entity).inventory.armorInventory[slot]) : null;
   }

   public double getSpeed() {
      return Utils.getHorizontalSpeed(this.entity);
   }

   public int getSwingProgress() {
      return this.isLiving ? ((EntityLivingBase)this.entity).swingProgressInt : -1;
   }

   public float getPrevSwingProgress() {
      return this.isLiving ? ((EntityLivingBase)this.entity).prevSwingProgress : -1.0F;
   }

   public int getTicksExisted() {
      return this.entity.ticksExisted;
   }

   public float getYaw() {
      return this.entity.rotationYaw;
   }

   public int getFireResistance() {
      return this.entity.fireResistance;
   }

   public float getPrevYaw() {
      return this.entity.prevRotationYaw;
   }

   public float getPrevPitch() {
      return this.entity.prevRotationPitch;
   }

   public boolean isCreative() {
      return !(this.entity instanceof EntityPlayer) ? false : ((EntityPlayer)this.entity).capabilities.isCreativeMode;
   }

   public boolean isCollided() {
      return !(this.entity instanceof EntityPlayer)
         ? Minecraft.getMinecraft().theWorld.checkBlockCollision(this.entity.getEntityBoundingBox().expand(0.05, 0.0, 0.05))
         : this.entity.isCollided;
   }

   public boolean isCollidedHorizontally() {
      return this.entity.isCollidedHorizontally;
   }

   public boolean isCollidedVertically() {
      return this.entity.isCollidedVertically;
   }

   public boolean isDead() {
      return this.entity.isDead || this.isLiving && ((EntityLivingBase)this.entity).deathTime > 0;
   }

   public int getHunger() {
      return this.isPlayer && ((EntityPlayer)this.entity).getFoodStats() != null ? ((EntityPlayer)this.entity).getFoodStats().getFoodLevel() : 0;
   }

   public float getSaturation() {
      return this.isPlayer && ((EntityPlayer)this.entity).getFoodStats() != null ? ((EntityPlayer)this.entity).getFoodStats().getSaturationLevel() : 0.0F;
   }

   public float getAir() {
      return this.entity.getAir();
   }

   public boolean isInvisible() {
      return this.entity.isInvisible();
   }

   public boolean isInWater() {
      return this.entity.isInWater();
   }

   public boolean isInLava() {
      return this.entity.isInLava();
   }

   public Entity getFisher() {
      return this.entity instanceof EntityFishHook ? convert(((EntityFishHook)this.entity).angler) : null;
   }

   public boolean isInLiquid() {
      return !this.entity.isOffsetPositionInLiquid(0.0, 0.0, 0.0);
   }

   public boolean isOnLadder() {
      return this.isLiving && ((EntityLivingBase)this.entity).isOnLadder();
   }

   public boolean isOnEdge() {
      return Utils.onEdge(this.entity);
   }

   public boolean isSprinting() {
      return this.entity.isSprinting();
   }

   public boolean isSneaking() {
      return this.entity.isSneaking();
   }

   public boolean isUsingItem() {
      return !(this.entity instanceof EntityPlayer) ? false : ((EntityPlayer)this.entity).isUsingItem();
   }

   public boolean onGround() {
      return this.entity.onGround;
   }

   public void setMotion(double x, double y, double z) {
      this.entity.motionX = x;
      this.entity.motionY = y;
      this.entity.motionZ = z;
   }

   public Vec3 getMotion() {
      return new Vec3(this.entity.motionX, this.entity.motionY, this.entity.motionZ);
   }

   public void setPitch(float pitch) {
      this.entity.rotationPitch = pitch;
   }

   public void setYaw(float yaw) {
      this.entity.rotationYaw = yaw;
   }

   public void setPosition(Vec3 position) {
      this.entity.setPosition(position.x, position.y, position.z);
   }

   public void setPosition(double x, double y, double z) {
      this.entity.setPosition(x, y, z);
   }
}
