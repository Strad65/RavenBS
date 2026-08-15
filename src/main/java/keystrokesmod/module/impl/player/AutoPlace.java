package keystrokesmod.module.impl.player;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AutoPlace extends Module {
   private SliderSetting mode;
   private SliderSetting frameDelay;
   private SliderSetting minPlaceDelay;
   private ButtonSetting disableLeft;
   private ButtonSetting holdRight;
   private ButtonSetting fastPlaceOnJump;
   private ButtonSetting pitchCheck;
   private double cachedFrameDelay = 0.0;
   private long lastPlace = 0L;
   private int frameCount = 0;
   private MovingObjectPosition lastRayTrace = null;
   private BlockPos lastBlockPos = null;
   private String[] modes = new String[]{"Post", "Multi-place"};

   public AutoPlace() {
      super("AutoPlace", Module.category.player);
      this.registerSetting(new DescriptionSetting("Best with safewalk."));
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
      this.registerSetting(this.frameDelay = new SliderSetting("Frame delay", 8.0, 0.0, 30.0, 1.0));
      this.registerSetting(this.minPlaceDelay = new SliderSetting("Min place delay", 60.0, 25.0, 500.0, 5.0));
      this.registerSetting(this.disableLeft = new ButtonSetting("Disable if LMB down", false));
      this.registerSetting(this.holdRight = new ButtonSetting("RMB required", true));
      this.registerSetting(this.fastPlaceOnJump = new ButtonSetting("Fast place on jump", true));
      this.registerSetting(this.pitchCheck = new ButtonSetting("Pitch check", false));
   }

   @Override
   public void guiUpdate() {
      if (this.cachedFrameDelay != this.frameDelay.getInput()) {
         this.resetVariables();
      }

      this.cachedFrameDelay = this.frameDelay.getInput();
   }

   @Override
   public void onDisable() {
      if (this.holdRight.isToggled()) {
         this.setRightClickDelay(4);
      }

      this.resetVariables();
   }

   @Override
   public void onUpdate() {
      if (mc.currentScreen == null && !mc.thePlayer.capabilities.isFlying) {
         ItemStack heldItem = mc.thePlayer.getHeldItem();
         if (heldItem != null && heldItem.getItem() instanceof ItemBlock) {
            if (this.fastPlaceOnJump.isToggled() && this.holdRight.isToggled() && !ModuleManager.fastPlace.isEnabled() && Mouse.isButtonDown(1)) {
               if (mc.thePlayer.motionY > 0.0) {
                  this.setRightClickDelay(1);
               } else if (!this.pitchCheck.isToggled() || mc.thePlayer.rotationPitch >= 70.0F) {
                  this.setRightClickDelay(1000);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (Utils.nullCheck()) {
         if (this.conditions()) {
            if (this.mode.getInput() == 1.0) {
               this.dp();
               this.dp();
               this.dp();
               this.dp();
            }
         }
      }
   }

   private void dp() {
      MovingObjectPosition mouseOverResult = mc.objectMouseOver;
      if (mouseOverResult != null
         && mouseOverResult.typeOfHit == MovingObjectType.BLOCK
         && mouseOverResult.sideHit != EnumFacing.UP
         && mouseOverResult.sideHit != EnumFacing.DOWN) {
         if (this.lastRayTrace != null && this.frameCount < this.frameDelay.getInput()) {
            this.frameCount++;
         } else {
            ItemStack heldItem = mc.thePlayer.getHeldItem();
            this.lastRayTrace = mouseOverResult;
            BlockPos currentBlockPosition = mouseOverResult.getBlockPos();
            if (this.lastBlockPos == null
               || currentBlockPosition.getX() != this.lastBlockPos.getX()
               || currentBlockPosition.getY() != this.lastBlockPos.getY()
               || currentBlockPosition.getZ() != this.lastBlockPos.getZ()) {
               Block targetBlock = mc.theWorld.getBlockState(currentBlockPosition).getBlock();
               if (targetBlock != null && targetBlock != Blocks.air && !(targetBlock instanceof BlockLiquid)) {
                  if (mc.playerController
                     .onPlayerRightClick(
                        mc.thePlayer, mc.theWorld, heldItem, currentBlockPosition, mouseOverResult.sideHit, mouseOverResult.hitVec
                     )) {
                     ReflectionUtils.setButton(1, true);
                     mc.thePlayer.swingItem();
                     mc.getItemRenderer().resetEquippedProgress();
                     ReflectionUtils.setButton(1, false);
                     this.lastBlockPos = currentBlockPosition;
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onHighlight(DrawBlockHighlightEvent e) {
      if (Utils.nullCheck()) {
         if (this.conditions()) {
            if (this.mode.getInput() == 0.0) {
               MovingObjectPosition mouseOverResult = mc.objectMouseOver;
               if (mouseOverResult != null
                  && mouseOverResult.typeOfHit == MovingObjectType.BLOCK
                  && mouseOverResult.sideHit != EnumFacing.UP
                  && mouseOverResult.sideHit != EnumFacing.DOWN) {
                  if (this.lastRayTrace != null && this.frameCount < this.frameDelay.getInput()) {
                     this.frameCount++;
                  } else {
                     ItemStack heldItem = mc.thePlayer.getHeldItem();
                     this.lastRayTrace = mouseOverResult;
                     BlockPos currentBlockPosition = mouseOverResult.getBlockPos();
                     if (this.lastBlockPos == null
                        || currentBlockPosition.getX() != this.lastBlockPos.getX()
                        || currentBlockPosition.getY() != this.lastBlockPos.getY()
                        || currentBlockPosition.getZ() != this.lastBlockPos.getZ()) {
                        Block targetBlock = mc.theWorld.getBlockState(currentBlockPosition).getBlock();
                        if (targetBlock != null && targetBlock != Blocks.air && !(targetBlock instanceof BlockLiquid)) {
                           long currentTime = System.currentTimeMillis();
                           if (!(currentTime - this.lastPlace < this.minPlaceDelay.getInput())) {
                              this.lastPlace = currentTime;
                              if (mc.playerController
                                 .onPlayerRightClick(
                                    mc.thePlayer,
                                    mc.theWorld,
                                    heldItem,
                                    currentBlockPosition,
                                    mouseOverResult.sideHit,
                                    mouseOverResult.hitVec
                                 )) {
                                 ReflectionUtils.setButton(1, true);
                                 mc.thePlayer.swingItem();
                                 mc.getItemRenderer().resetEquippedProgress();
                                 ReflectionUtils.setButton(1, false);
                                 this.lastBlockPos = currentBlockPosition;
                                 this.frameCount = 0;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void setRightClickDelay(int delay) {
      ((IAccessorMinecraft)mc).setRightClickDelayTimer(delay);
   }

   private void resetVariables() {
      this.lastBlockPos = null;
      this.lastRayTrace = null;
      this.frameCount = 0;
   }

   private boolean conditions() {
      ItemStack heldItem = mc.thePlayer.getHeldItem();
      if (mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) {
         return false;
      } else if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock)) {
         return false;
      } else if (this.disableLeft.isToggled() && Mouse.isButtonDown(0)) {
         return false;
      } else if (this.holdRight.isToggled() && !Mouse.isButtonDown(1)) {
         return false;
      } else if (ModuleManager.scaffold.isEnabled) {
         return false;
      } else {
         return this.pitchCheck.isToggled() && mc.thePlayer.rotationPitch < 70.0F ? false : !(mc.thePlayer.moveForward > -0.2);
      }
   }
}
