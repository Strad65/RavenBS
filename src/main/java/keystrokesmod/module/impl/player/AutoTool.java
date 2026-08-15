package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import org.lwjgl.input.Mouse;

public class AutoTool extends Module {
   private SliderSetting hoverDelay;
   private SliderSetting swapDelay;
   private ButtonSetting rightDisable;
   private ButtonSetting requireCrouch;
   private ButtonSetting requireMouse;
   public ButtonSetting spoofItem;
   private ButtonSetting swapBack;
   private boolean hasSwapped = false;
   private int swapDelayTick = 0;
   public int previousSlot = -1;
   private long ticksHovered;

   public AutoTool() {
      super("AutoTool", Module.category.player);
      this.registerSetting(this.hoverDelay = new SliderSetting("Hover delay", 0.0, 0.0, 20.0, 1.0));
      this.registerSetting(this.swapDelay = new SliderSetting("Swap delay", 0.0, 0.0, 20.0, 1.0));
      this.registerSetting(this.rightDisable = new ButtonSetting("Disable while right click", true));
      this.registerSetting(this.requireCrouch = new ButtonSetting("Only while crouching", false));
      this.registerSetting(this.requireMouse = new ButtonSetting("Require mouse down", true));
      this.registerSetting(this.spoofItem = new ButtonSetting("Spoof item", false));
      this.registerSetting(this.swapBack = new ButtonSetting("Swap to previous slot", true));
      this.closetModule = true;
   }

   @Override
   public void onDisable() {
      this.resetVariables(true);
   }

   public void setSlot(int currentItem) {
      if (currentItem != -1 && currentItem != mc.thePlayer.inventory.currentItem) {
         mc.thePlayer.inventory.currentItem = currentItem;
         this.hasSwapped = true;
         this.swapDelayTick = (int)this.swapDelay.getInput();
      }
   }

   @Override
   public void onUpdate() {
      if (this.spoofItem.isToggled() && this.previousSlot != mc.thePlayer.inventory.currentItem && this.previousSlot != -1) {
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "&7Modifying held item renderer");
         }

         ((IMixinItemRenderer)mc.getItemRenderer()).setCancelUpdate(true);
         ((IMixinItemRenderer)mc.getItemRenderer()).setCancelReset(true);
      }

      if (mc.inGameHasFocus
         && mc.currentScreen == null
         && (!this.rightDisable.isToggled() || !Mouse.isButtonDown(1))
         && mc.thePlayer.capabilities.allowEdit
         && (!this.requireCrouch.isToggled() || mc.thePlayer.isSneaking())) {
         if (!Mouse.isButtonDown(0) && this.requireMouse.isToggled()) {
            this.resetSlot();
         } else {
            MovingObjectPosition over = mc.objectMouseOver;
            if (over != null && over.typeOfHit == MovingObjectType.BLOCK) {
               if (this.hoverDelay.getInput() != 0.0) {
                  long ticks = this.ticksHovered + 1L;
                  this.ticksHovered = ticks;
                  if (ticks < this.hoverDelay.getInput()) {
                     return;
                  }
               }

               int slot = Utils.getTool(BlockUtils.getBlock(over.getBlockPos()));
               if (slot != -1) {
                  if (this.previousSlot == -1) {
                     this.previousSlot = mc.thePlayer.inventory.currentItem;
                  }

                  if (!this.hasSwapped) {
                     this.setSlot(slot);
                  } else if (slot != mc.thePlayer.inventory.currentItem
                     && this.swapDelayTick-- <= 0
                     && mc.thePlayer.inventory.currentItem != slot) {
                     this.setSlot(slot);
                     this.swapDelayTick = (int)this.swapDelay.getInput();
                  }
               }
            } else {
               this.resetSlot();
               this.resetVariables(true);
            }
         }
      } else {
         this.resetVariables(false);
      }
   }

   private void resetVariables(boolean resetHover) {
      if (resetHover) {
         this.ticksHovered = 0L;
      }

      this.resetSlot();
      this.previousSlot = -1;
      this.hasSwapped = false;
      this.swapDelayTick = 0;
   }

   private void resetSlot() {
      if (this.previousSlot != -1 && this.swapBack.isToggled()) {
         this.setSlot(this.previousSlot);
         this.previousSlot = -1;
         this.hasSwapped = false;
         this.swapDelayTick = 0;
      }
   }
}
