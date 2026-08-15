package keystrokesmod.module.impl.player;

import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WaterBucket extends Module {
   public ButtonSetting pickupWater;
   public ButtonSetting silentAim;
   public ButtonSetting switchToItem;
   private final long PLACE_DELAY = 500L;
   private final long PICKUP_WAIT = 150L;
   private long lastPlace = 0L;
   private boolean shouldPickup = false;
   private int lastSlot = -1;

   public WaterBucket() {
      super("Water Bucket", Module.category.player);
      this.registerSetting(this.pickupWater = new ButtonSetting("Pickup water", true));
      this.registerSetting(this.silentAim = new ButtonSetting("Silent aim", true));
      this.registerSetting(this.switchToItem = new ButtonSetting("Switch to item", true));
   }

   @Override
   public void onDisable() {
      this.lastPlace = 0L;
      this.shouldPickup = false;
      this.lastSlot = -1;
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (Utils.nullCheck() && !mc.isGamePaused() && !mc.thePlayer.capabilities.isFlying && !mc.thePlayer.capabilities.isCreativeMode) {
         if (this.fallCheck()) {
            MovingObjectPosition mop = Utils.getTarget(
               mc.playerController.getBlockReachDistance(), mc.thePlayer.rotationYaw, this.silentAim.isToggled() ? 90.0F : mc.thePlayer.rotationPitch
            );
            if (mop != null && mop.typeOfHit == MovingObjectType.BLOCK && mop.sideHit == EnumFacing.UP) {
               long now = System.currentTimeMillis();
               if (Utils.timeBetween(this.lastPlace, now) >= 500L) {
                  if (!this.isItem(mc.thePlayer.getHeldItem(), Items.water_bucket) && this.switchToItem.isToggled()) {
                     this.attemptSwitch();
                  }

                  if (this.silentAim.isToggled() || !(mc.thePlayer.rotationPitch < 80.0F)) {
                     this.lastPlace = now;
                     this.useCurrentItem();
                     if (!(this.shouldPickup = this.pickupWater.isToggled())) {
                        this.lastSlot = -1;
                     }

                     if (keystrokesmod.Raven.debug) {
                        Utils.sendModuleMessage(
                           this,
                           "&7Placed with motionY &d"
                              + Utils.round(mc.thePlayer.motionY, 2)
                              + " &7and fall distance &d"
                              + Utils.round(mc.thePlayer.fallDistance, 2)
                        );
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onUpdate() {
      if (!mc.isGamePaused()) {
         if (this.shouldPickup
            && Utils.timeBetween(this.lastPlace, System.currentTimeMillis()) > 150L
            && this.isItem(mc.thePlayer.getHeldItem(), Items.bucket)) {
            this.shouldPickup = false;
            this.useCurrentItem();
            if (this.lastSlot != -1) {
               Utils.switchSlot(this.lastSlot, true);
               this.lastSlot = -1;
            }
         }
      }
   }

   @SubscribeEvent
   public void onClientRotation(ClientRotationEvent e) {
      if (this.silentAim.isToggled()
         && (this.fallCheck() || Utils.timeBetween(this.lastPlace, System.currentTimeMillis()) < 500L)
         && this.getWaterBucketSlot() != -1) {
         e.setYaw(mc.thePlayer.rotationYaw);
         e.setPitch(90.0F);
      }
   }

   private void attemptSwitch() {
      int slot = this.getWaterBucketSlot();
      if (slot != -1) {
         this.lastSlot = mc.thePlayer.inventory.currentItem;
         Utils.switchSlot(slot, true);
      }
   }

   private int getWaterBucketSlot() {
      for (int slot = 0; slot < InventoryPlayer.getHotbarSize(); slot++) {
         if (this.isItem(mc.thePlayer.inventory.getStackInSlot(slot), Items.water_bucket)) {
            return slot;
         }
      }

      return -1;
   }

   private void useCurrentItem() {
      mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
   }

   private boolean isItem(ItemStack itemStack, Item item) {
      return itemStack != null && itemStack.getItem() == item;
   }

   private boolean fallCheck() {
      return !mc.thePlayer.onGround && mc.thePlayer.fallDistance >= 3.3;
   }
}
