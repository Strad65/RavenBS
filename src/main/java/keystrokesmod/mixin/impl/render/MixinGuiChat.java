package keystrokesmod.mixin.impl.render;

import java.util.List;
import keystrokesmod.module.ModuleManager;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(GuiChat.class)
public abstract class MixinGuiChat extends MixinGuiScreen {
   @Shadow
   protected GuiTextField inputField;
   @Shadow
   private List<String> foundPlayerNames;
   @Shadow
   private boolean waitingOnAutocomplete;

   @Shadow
   public abstract void onAutocompleteResponse(String[] var1);

   @Inject(method = "keyTyped", at = @At("RETURN"))
   private void updateLength(CallbackInfo callbackInfo) {
      if (this.inputField.getText().startsWith(".") && ModuleManager.canExecuteChatCommand()) {
         keystrokesmod.Raven.commandManager.autoComplete(this.inputField.getText());
      } else {
         this.inputField.setMaxStringLength(100);
      }
   }

   @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
   private void handleClientCommandCompletion(String full, String ignored, CallbackInfo callbackInfo) {
      if (keystrokesmod.Raven.commandManager.autoComplete(full) && ModuleManager.canExecuteChatCommand()) {
         this.waitingOnAutocomplete = true;
         String[] latestAutoComplete = keystrokesmod.Raven.commandManager.latestAutoComplete;
         if (full.toLowerCase().endsWith(latestAutoComplete[latestAutoComplete.length - 1].toLowerCase())) {
            return;
         }

         this.onAutocompleteResponse(latestAutoComplete);
         callbackInfo.cancel();
      }
   }
}
