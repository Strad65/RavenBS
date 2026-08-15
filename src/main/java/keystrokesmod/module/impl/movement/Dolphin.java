package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Dolphin extends Module {
   public SliderSetting horSpeed;
   public SliderSetting verSpeed;
   public ButtonSetting buoyant;
   public ButtonSetting disableUsing;
   public ButtonSetting disableVerticalWhileMoving;
   public ButtonSetting forwardOnly;

   public Dolphin() {
      super("Dolphin", Module.category.movement, 0);
      this.registerSetting(this.horSpeed = new SliderSetting("Horizontal speed", 1.0, 1.0, 8.0, 0.1));
      this.registerSetting(this.verSpeed = new SliderSetting("Vertical speed", 1.0, 1.0, 8.0, 0.1));
      this.registerSetting(this.buoyant = new ButtonSetting("Buoyant", false));
      this.registerSetting(this.disableUsing = new ButtonSetting("Disable while using", true));
      this.registerSetting(this.disableVerticalWhileMoving = new ButtonSetting("Disable vertical while moving", false));
      this.registerSetting(this.forwardOnly = new ButtonSetting("Forward only", true));
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent event) {
      if (mc.thePlayer.isInWater() && !mc.thePlayer.capabilities.isFlying) {
         if (!this.disableUsing.isToggled() || !mc.thePlayer.isUsingItem()) {
            if (!this.forwardOnly.isToggled() || Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
               if (this.buoyant.isToggled()) {
                  mc.thePlayer.motionY = 0.0;
               }

               if (Utils.isUserMoving()) {
                  double horizontalSpeed = 0.078 * this.horSpeed.getInput();
                  Utils.setSpeed(horizontalSpeed);
                  if (this.disableVerticalWhileMoving.isToggled()) {
                     return;
                  }
               }

               if (Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
                  mc.thePlayer.motionY = 0.02 + 0.04 * this.verSpeed.getInput();
               } else if (!mc.thePlayer.onGround && Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
                  mc.thePlayer.movementInput.sneak = false;
                  mc.thePlayer.motionY = -0.1 - 0.03 * this.verSpeed.getInput();
               } else if (this.buoyant.isToggled()) {
                  mc.thePlayer.motionY = 0.0;
               }
            }
         }
      }
   }
}
