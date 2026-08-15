package keystrokesmod.utility;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class BlockUtils {
   public static final Minecraft mc = Minecraft.getMinecraft();

   public static boolean isSamePos(BlockPos blockPos, BlockPos blockPos2) {
      return blockPos == blockPos2
         || blockPos.getX() == blockPos2.getX()
            && blockPos.getY() == blockPos2.getY()
            && blockPos.getZ() == blockPos2.getZ();
   }

   public static boolean notFull(Block block) {
      return block instanceof BlockFenceGate
         || block instanceof BlockLadder
         || block instanceof BlockFlowerPot
         || block instanceof BlockBasePressurePlate
         || isFluid(block)
         || block instanceof BlockFence
         || block instanceof BlockAnvil
         || block instanceof BlockEnchantmentTable
         || block instanceof BlockChest;
   }

   public static boolean isNormalBlock(Block block) {
      return block == Blocks.glass
         || block.isFullBlock()
            && block != Blocks.gravel
            && block != Blocks.sand
            && block != Blocks.soul_sand
            && block != Blocks.tnt
            && block != Blocks.crafting_table
            && block != Blocks.furnace
            && block != Blocks.dispenser
            && block != Blocks.dropper
            && block != Blocks.noteblock
            && block != Blocks.command_block;
   }

   public static BlockPos pos(double x, double y, double z) {
      return new BlockPos(x, y, z);
   }

   public static boolean isBlockPosEqual(BlockPos pos1, BlockPos pos2) {
      return pos1 == pos2
         || pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
   }

   public static BlockPos offsetPos(MovingObjectPosition mop) {
      return mop.getBlockPos().offset(mop.sideHit);
   }

   public static boolean isFluid(Block block) {
      return block.getMaterial() == Material.lava || block.getMaterial() == Material.water;
   }

   public static boolean isInteractable(Block block) {
      return block instanceof BlockChest
         || block instanceof BlockEnderChest
         || block instanceof BlockFurnace
         || block instanceof BlockTrapDoor
         || block instanceof BlockDoor
         || block instanceof BlockContainer
         || block instanceof BlockJukebox
         || block instanceof BlockFenceGate
         || block instanceof BlockEnchantmentTable
         || block instanceof BlockBrewingStand
         || block instanceof BlockBed
         || block instanceof BlockDropper
         || block instanceof BlockDispenser
         || block instanceof BlockHopper
         || block instanceof BlockAnvil
         || block instanceof BlockNote
         || block instanceof BlockWorkbench;
   }

   public static boolean isInteractable(MovingObjectPosition mv) {
      if (mv == null || mv.typeOfHit != MovingObjectType.BLOCK || mv.getBlockPos() == null) {
         return false;
      } else {
         return mc.thePlayer.isSneaking() && mc.thePlayer.getHeldItem() != null ? false : isInteractable(getBlock(mv.getBlockPos()));
      }
   }

   public static float getBlockHardness(Block block, ItemStack itemStack, boolean ignoreSlow, boolean ignoreGround) {
      float getBlockHardness = block.getBlockHardness(mc.theWorld, null);
      if (getBlockHardness < 0.0F) {
         return 0.0F;
      } else {
         return !block.getMaterial().isToolNotRequired() && (itemStack == null || !itemStack.canHarvestBlock(block))
            ? getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 100.0F
            : getToolDigEfficiency(itemStack, block, ignoreSlow, ignoreGround) / getBlockHardness / 30.0F;
      }
   }

   public static float getToolDigEfficiency(ItemStack itemStack, Block block, boolean ignoreSlow, boolean ignoreGround) {
      float n = itemStack == null ? 1.0F : itemStack.getItem().getStrVsBlock(itemStack, block);
      if (n > 1.0F) {
         int getEnchantmentLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack);
         if (getEnchantmentLevel > 0 && itemStack != null) {
            n += getEnchantmentLevel * getEnchantmentLevel + 1;
         }
      }

      if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
         n *= 1.0F + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2F;
      }

      if (!ignoreSlow) {
         if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            float n2;
            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
               case 0:
                  n2 = 0.3F;
                  break;
               case 1:
                  n2 = 0.09F;
                  break;
               case 2:
                  n2 = 0.0027F;
                  break;
               default:
                  n2 = 8.1E-4F;
            }

            n *= n2;
         }

         if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            n /= 5.0F;
         }

         if (!mc.thePlayer.onGround && !ignoreGround) {
            n /= 5.0F;
         }
      }

      return n;
   }

   public static Block getBlock(BlockPos blockPos) {
      return getBlockState(blockPos).getBlock();
   }

   public static Block getBlock(double x, double y, double z) {
      return getBlockState(new BlockPos(x, y, z)).getBlock();
   }

   public static IBlockState getBlockState(BlockPos blockPos) {
      return mc.theWorld.getBlockState(blockPos);
   }

   public static boolean check(BlockPos blockPos, Block block) {
      return getBlock(blockPos) == block;
   }

   public static boolean replaceable(BlockPos blockPos) {
      return !Utils.nullCheck() ? true : getBlock(blockPos).isReplaceable(mc.theWorld, blockPos);
   }

   public static boolean canSeeVecBlock(BlockPos pos, Vec3 vecPlayer, Vec3 vecBlockPoint) {
      MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(vecPlayer, vecBlockPoint, false, false, false);
      if (mop == null) {
         return true;
      }

      if (mop.typeOfHit == MovingObjectType.BLOCK) {
         BlockPos mopPos = mop.getBlockPos();
         if (mopPos.getX() == pos.getX() && mopPos.getY() == pos.getY() && mopPos.getZ() == pos.getZ()) {
            return true;
         }
      }

      return false;
   }

   public static boolean canBlockBeSeen(BlockPos pos) {
      Vec3 vecPlayer = new Vec3(
         mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ
      );

      for (double offsetY = 0.0; offsetY <= 0.5; offsetY += 0.5) {
         double y = pos.getY() + offsetY;
         Vec3 vecBlockPoint = new Vec3(pos.getX() + 1, y, pos.getZ() + 0.5);
         if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
            return true;
         }

         vecBlockPoint = new Vec3(pos.getX(), y, pos.getZ() + 0.5);
         if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
            return true;
         }

         vecBlockPoint = new Vec3(pos.getX() + 0.5, y, pos.getZ() + 1);
         if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
            return true;
         }

         vecBlockPoint = new Vec3(pos.getX() + 0.5, y, pos.getZ());
         if (canSeeVecBlock(pos, vecPlayer, vecBlockPoint)) {
            return true;
         }
      }

      return false;
   }

   public static EnumDyeColor getWoolColor(IBlockState state) {
      return (EnumDyeColor)state.getProperties().get(BlockColored.COLOR);
   }
}
