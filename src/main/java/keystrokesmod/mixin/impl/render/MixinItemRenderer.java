package keystrokesmod.mixin.impl.render;

import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer implements IMixinItemRenderer {
   private ItemStack originalItemToRender;
   @Shadow
   private ItemStack itemToRender;
   public boolean cancelUpdate = false;
   public boolean cancelReset = false;
   @Shadow
   private float equippedProgress;
   @Shadow
   private float prevEquippedProgress;

   @Inject(method = "renderItemInFirstPerson", at = @At("HEAD"))
   private void modifyRenderItemPre(float p_renderItemInFirstPerson_1_, CallbackInfo info) {
      this.originalItemToRender = this.itemToRender;
      this.itemToRender = Utils.getSpoofedItem(this.originalItemToRender);
   }

   @Inject(method = "renderItemInFirstPerson", at = @At("RETURN"))
   private void modifyRenderItemPost(float p_renderItemInFirstPerson_1_, CallbackInfo info) {
      this.itemToRender = this.originalItemToRender;
   }

   @Inject(method = "updateEquippedItem", at = @At("HEAD"), cancellable = true)
   private void onUpdateEquippedItem(CallbackInfo ci) {
      if (this.cancelUpdate) {
         this.cancelUpdate = false;
         this.equippedProgress = 1.0F;
         this.prevEquippedProgress = 1.0F;
         ci.cancel();
      }
   }

   @Inject(method = "resetEquippedProgress", at = @At("HEAD"), cancellable = true)
   public void injectResetEquippedProgress(CallbackInfo ci) {
      if (this.cancelReset) {
         this.cancelReset = false;
         this.equippedProgress = 1.0F;
         this.prevEquippedProgress = 1.0F;
         ci.cancel();
      }
   }

   @Inject(method = "resetEquippedProgress2", at = @At("HEAD"), cancellable = true)
   public void injectResetEquippedProgress2(CallbackInfo ci) {
      if (this.cancelReset) {
         this.cancelReset = false;
         this.equippedProgress = 1.0F;
         this.prevEquippedProgress = 1.0F;
         ci.cancel();
      }
   }

   @Override
   public void setCancelUpdate(boolean cancel) {
      this.cancelUpdate = cancel;
   }

   @Override
   public void setCancelReset(boolean reset) {
      this.cancelReset = reset;
   }
}
