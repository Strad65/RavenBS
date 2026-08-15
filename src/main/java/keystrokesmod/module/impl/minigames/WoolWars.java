package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockStairs;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class WoolWars extends Module {
   public SliderSetting breakSpeed;
   public SliderSetting range;
   public SliderSetting breakDelay;
   public SliderSetting placeDelay;
   public ButtonSetting onlyMiddleClick;
   public ButtonSetting onlyVisible;
   private final int middlePositionColors = new Color(255, 153, 204).getRGB();
   private final int miningColor = new Color(200, 100, 255).getRGB();
   private final int placeColor = new Color(150, 70, 255).getRGB();
   private BlockPos middlePos;
   private BlockPos miningPos;
   private MovingObjectPosition placeMop;
   private float curBlockDamageMP;
   private int delay;
   private int swapBack = -1;
   private double lastRange;
   private double rangeSq;
   private float placingYaw;
   private float placingPitch;
   private boolean fakeSwing;

   public WoolWars() {
      super("WoolWars", Module.category.minigames, 0);
      this.registerSetting(new DescriptionSetting("Nukes and places at control point."));
      this.registerSetting(this.breakSpeed = new SliderSetting("Break speed", 0.2, 0.0, 0.8, 0.05));
      this.registerSetting(this.breakDelay = new SliderSetting("Delay after breaking", 3.0, 1.0, 10.0, 1.0));
      this.registerSetting(this.placeDelay = new SliderSetting("Delay after placing", 1.0, 1.0, 10.0, 1.0));
      this.registerSetting(this.range = new SliderSetting("Range", 5.0, 1.0, 8.0, 0.5));
      this.registerSetting(this.onlyVisible = new ButtonSetting("Only visible", true));
      this.registerSetting(this.onlyMiddleClick = new ButtonSetting("Only while middle clicking", true));
   }

   @Override
   public void guiUpdate() {
      if (this.lastRange != this.range.getInput()) {
         this.lastRange = this.range.getInput();
         this.rangeSq = Math.pow(this.lastRange + 2.0, 2.0);
      }
   }

   @Override
   public void onDisable() {
      this.swapBack();
      this.reset();
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent event) {
      if (!this.isWoolWars()) {
         this.reset();
      } else {
         if (this.middlePos == null) {
            this.middlePos = this.getMiddlePos();
         } else if (!mc.thePlayer.capabilities.allowFlying
            && mc.thePlayer.getDistanceSq(this.middlePos) < this.rangeSq
            && this.isActiveRound()
            && (!this.onlyMiddleClick.isToggled() || Mouse.isButtonDown(2))) {
            if (this.swapBack == -1) {
               this.swapBack = mc.thePlayer.inventory.currentItem;
            }

            if (this.delay > 0 && --this.delay > 0) {
               if (this.fakeSwing) {
                  mc.thePlayer.swingItem();
               }

               return;
            }

            if (this.placeMop != null) {
               return;
            }

            if (this.miningPos == null) {
               List<BlockPos> posList = this.getPossiblePos(this.middlePos, true);
               if (!posList.isEmpty()) {
                  BlockPos closestPos = this.getClosestPos(posList, true);
                  if (closestPos != null) {
                     int blockSlot = this.getBlockSlot();
                     if (blockSlot == -1) {
                        return;
                     }

                     Utils.switchSlot(blockSlot, true);

                     for (int i = 0; i < 360; i += 10) {
                        float yaw = (float)(mc.thePlayer.rotationYaw + i + this.randomRotationOffset());

                        for (int j = 20; j < 90; j += 5) {
                           float pitch = RotationUtils.clampPitch((float)(j + this.randomRotationOffset()));
                           MovingObjectPosition mop = Utils.getTarget(this.lastRange, yaw, pitch);
                           if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK && BlockUtils.isBlockPosEqual(BlockUtils.offsetPos(mop), closestPos)) {
                              this.placeMop = mop;
                              this.placingYaw = yaw;
                              this.placingPitch = pitch;
                              return;
                           }
                        }
                     }

                     return;
                  }
               }

               posList = this.getPossiblePos(this.middlePos, false);
               if (posList.isEmpty()) {
                  this.middlePos = null;
                  this.swapBack();
                  return;
               }

               BlockPos closestPos = this.getClosestPos(posList, false);
               if (closestPos == null) {
                  return;
               }

               this.miningPos = closestPos;
               this.switchToSlot(Utils.getTool(BlockUtils.getBlock(closestPos)));
               this.miningPos = closestPos;
               mc.thePlayer.swingItem();
               startBreak(this.miningPos);
            } else if (!Utils.isPossibleToReach(this.miningPos, this.lastRange)) {
               abortBreak(this.miningPos);
               this.miningPos = null;
               this.curBlockDamageMP = this.delay = 0;
               return;
            }

            this.curBlockDamageMP = this.curBlockDamageMP
               + BlockUtils.getBlockHardness(BlockUtils.getBlock(this.miningPos), mc.thePlayer.getHeldItem(), false, false);
            if (this.curBlockDamageMP < this.breakSpeed.getInput()) {
               this.curBlockDamageMP = (float)this.breakSpeed.getInput();
            }

            if (this.curBlockDamageMP >= 1.0F) {
               stopBreak(this.miningPos);
               mc.playerController.onPlayerDestroyBlock(this.miningPos, EnumFacing.UP);
               this.miningPos = null;
               this.curBlockDamageMP = 0.0F;
               this.delay = (int)this.breakDelay.getInput();
               this.fakeSwing = true;
            }

            mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), this.miningPos, (int)(this.curBlockDamageMP * 10.0F) - 1);
            mc.thePlayer.swingItem();
         } else if (this.miningPos != null) {
            abortBreak(this.miningPos);
            this.miningPos = null;
            this.curBlockDamageMP = this.delay = 0;
            this.swapBack();
         } else if (this.swapBack != -1) {
            this.swapBack();
         }
      }
   }

   public double randomRotationOffset() {
      return Math.random() - 0.5;
   }

   public boolean switchToSlot(int slot) {
      if (slot == -1) {
         return false;
      }

      mc.thePlayer.inventory.currentItem = slot;
      return true;
   }

   private BlockPos getMiddlePos() {
      BlockPos middlePos = null;
      int y;
      int startY = y = (int)Math.floor(mc.thePlayer.posY + 20.0);

      while (y > -1) {
         BlockPos pos = BlockUtils.pos(0.0, y, 0.0);
         if (BlockUtils.getBlock(pos.add(0, 0, 2)) instanceof BlockStairs || this.isControlPointBlock(pos, false)) {
            middlePos = pos;
            break;
         }

         y--;
      }

      if (middlePos == null) {
         for (int var5 = startY; var5 > -1; var5--) {
            BlockPos pos = BlockUtils.pos(0.0, var5, 6.0);
            if (BlockUtils.getBlock(pos.add(0, 0, 2)) instanceof BlockStairs || this.isControlPointBlock(pos, false)) {
               middlePos = pos;
               break;
            }
         }
      }

      return middlePos;
   }

   private List<BlockPos> getPossiblePos(BlockPos middlePos, boolean airOnly) {
      List<BlockPos> posList = new ArrayList<>();

      for (int zOffset = -1; zOffset <= 1; zOffset++) {
         for (int xOffset = -1; xOffset <= 1; xOffset++) {
            BlockPos pos = new BlockPos(middlePos.getX() + xOffset, middlePos.getY(), middlePos.getZ() + zOffset);
            if (airOnly ? BlockUtils.getBlock(pos) instanceof BlockAir : this.isControlPointBlock(pos, true)) {
               posList.add(pos);
            }
         }
      }

      return posList;
   }

   private BlockPos getClosestPos(List<BlockPos> posList, boolean down) {
      BlockPos closestPos = null;
      double leastDistSq = this.rangeSq + 1.0;

      for (BlockPos pos : posList) {
         if (Utils.isPossibleToReach(down ? pos.down() : pos, this.lastRange) && (!this.onlyVisible.isToggled() || BlockUtils.canBlockBeSeen(pos))) {
            double distSq = mc.thePlayer.getDistanceSq(pos);
            if (!(distSq >= leastDistSq)) {
               leastDistSq = distSq;
               closestPos = pos;
            }
         }
      }

      return closestPos;
   }

   private boolean isControlPointBlock(BlockPos pos, boolean verifyWoolColor) {
      Block block = BlockUtils.getBlock(pos);
      if (block != Blocks.wool) {
         return block == Blocks.snow || block == Blocks.quartz_block;
      }

      if (!verifyWoolColor) {
         return true;
      }

      EnumDyeColor teamColor = null;

      for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
         ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
         if (stack != null && stack.getItem() instanceof ItemBlock && ((ItemBlock)stack.getItem()).getBlock() == Blocks.wool) {
            teamColor = EnumDyeColor.byMetadata(stack.getMetadata());
            break;
         }
      }

      return BlockUtils.getWoolColor(BlockUtils.getBlockState(pos)) != teamColor;
   }

   private boolean isActiveRound() {
      for (String line : Utils.getSidebarLines()) {
         String strip = Utils.stripString(line);
         if (strip.contains("State: Active Round")) {
            return true;
         }
      }

      return false;
   }

   public int getBlockSlot() {
      for (int slot = 0; slot < InventoryPlayer.getHotbarSize(); slot++) {
         ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
         if (stack != null && stack.getItem() instanceof ItemBlock) {
            Block block = ((ItemBlock)stack.getItem()).getBlock();
            if (BlockUtils.isNormalBlock(block)) {
               return slot;
            }
         }
      }

      return -1;
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      if (this.placeMop != null) {
         if (this.placingPitch > 90.0F) {
            if (mc.playerController
               .onPlayerRightClick(
                  mc.thePlayer,
                  mc.theWorld,
                  mc.thePlayer.getHeldItem(),
                  this.placeMop.getBlockPos(),
                  this.placeMop.sideHit,
                  this.placeMop.hitVec
               )) {
               mc.thePlayer.swingItem();
               mc.getItemRenderer().resetEquippedProgress();
               this.delay = (int)this.placeDelay.getInput();
               this.fakeSwing = false;
            }

            this.placeMop = null;
         } else {
            this.placingPitch += 300.0F;
         }

         e.setYaw(this.placingYaw);
         e.setPitch(this.placingPitch - 300.0F);
      } else {
         if (this.miningPos != null) {
            float[] rotations = RotationUtils.getRotationsToBlock(this.miningPos, EnumFacing.UP, e.getYaw(), e.getPitch());
            if (rotations != null) {
               e.setRotations(rotations[0], rotations[1]);
            }
         }

         if (this.delay > 0 && (!this.onlyMiddleClick.isToggled() || Mouse.isButtonDown(2))) {
            List<BlockPos> posList = this.getPossiblePos(this.middlePos, true);
            BlockPos closestPos = null;
            if (!posList.isEmpty()) {
               closestPos = this.getClosestPos(posList, true);
            }

            if (closestPos == null) {
               posList = this.getPossiblePos(this.middlePos, false);
               closestPos = this.getClosestPos(posList, false);
            }

            if (closestPos != null) {
               float[] rotations = RotationUtils.getRotationsToBlock(closestPos, EnumFacing.UP, e.getYaw(), e.getPitch());
               if (rotations != null) {
                  e.setRotations(rotations[0], rotations[1]);
               }
            }
         }
      }
   }

   private boolean isWoolWars() {
      if (Utils.nullCheck() && Utils.isHypixel()) {
         Scoreboard scoreboard = mc.theWorld.getScoreboard();
         if (scoreboard == null) {
            return false;
         }

         ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
         return objective != null && Utils.stripString(objective.getDisplayName()).contains("WOOL WARS");
      } else {
         return false;
      }
   }

   private void swapBack() {
      if (this.swapBack != -1) {
         mc.thePlayer.inventory.currentItem = this.swapBack;
         this.swapBack = -1;
      }
   }

   private void reset() {
      this.middlePos = this.miningPos = null;
      this.placeMop = null;
      this.curBlockDamageMP = this.delay = 0;
      this.swapBack = -1;
   }

   @SubscribeEvent
   public void onRender(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         if (this.middlePos != null) {
            for (BlockPos pos : this.getPossiblePos(this.middlePos, false)) {
               RenderUtils.renderBlock(pos, this.middlePositionColors, true, false);
            }
         }

         if (this.miningPos != null) {
            RenderUtils.renderBlock(this.miningPos, this.miningColor, false, true);
         } else if (this.placeMop != null) {
            RenderUtils.renderBlock(BlockUtils.offsetPos(this.placeMop), this.placeColor, false, true);
         }
      }
   }

   @SubscribeEvent
   public void onMouse(MouseEvent e) {
      if (e.button == 0) {
         if (e.buttonstate && (this.miningPos != null || this.placeMop != null)) {
            e.setCanceled(true);
         }
      } else if (e.button == 1 && (this.miningPos != null || this.placeMop != null)) {
         e.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.reset();
      }
   }

   public static void startBreak(BlockPos pos) {
      mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.START_DESTROY_BLOCK, pos, EnumFacing.UP));
   }

   public static void stopBreak(BlockPos pos) {
      mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.STOP_DESTROY_BLOCK, pos, EnumFacing.UP));
   }

   public static void abortBreak(BlockPos pos) {
      mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.ABORT_DESTROY_BLOCK, pos, EnumFacing.DOWN));
   }
}
