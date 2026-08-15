package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.settings.KeyBinding;

public class AntiAFK extends Module {
   private SliderSetting afk;
   private ButtonSetting jump;
   private ButtonSetting jumpWhenCollided;
   private ButtonSetting randomClicks;
   private ButtonSetting swapItem;
   private SliderSetting spin;
   private ButtonSetting randomizeDelta;
   private ButtonSetting randomizePitch;
   private SliderSetting minDelay;
   private SliderSetting maxDelay;
   private String[] afkModes = new String[]{"None", "Wander", "Lateral shuffle", "Forward", "Backward", "Lobby"};
   private String[] spinModes = new String[]{"None", "Random", "Right", "Left"};
   private int ticks;
   private int afkTicks;
   private boolean c;
   public boolean stop = false;
   private boolean stopFlying;
   private int sfTicks;
   private int randomDelay;

   public AntiAFK() {
      super("AntiAFK", Module.category.player);
      this.registerSetting(this.afk = new SliderSetting("AFK", 0, this.afkModes));
      this.registerSetting(this.jump = new ButtonSetting("Jump", false));
      this.registerSetting(this.jumpWhenCollided = new ButtonSetting("Jump only when collided", false));
      this.registerSetting(this.randomClicks = new ButtonSetting("Random clicks", false));
      this.registerSetting(this.swapItem = new ButtonSetting("Swap item", false));
      this.registerSetting(this.spin = new SliderSetting("Spin", 0, this.spinModes));
      this.registerSetting(this.randomizeDelta = new ButtonSetting("Randomize delta", true));
      this.registerSetting(this.randomizePitch = new ButtonSetting("Randomize pitch", true));
      this.registerSetting(this.minDelay = new SliderSetting("Minimum delay ticks", 10.0, 4.0, 160.0, 2.0));
      this.registerSetting(this.maxDelay = new SliderSetting("Maximum delay ticks", 80.0, 4.0, 160.0, 2.0));
   }

   @Override
   public void onEnable() {
      this.ticks = this.h();
      this.c = Utils.getRandom().nextBoolean();
   }

   @Override
   public void onUpdate() {
      if (!this.stop) {
         if (mc.currentScreen == null || mc.currentScreen instanceof GuiChat) {
            this.ticks--;
            switch ((int)this.afk.getInput()) {
               case 1:
                  if (this.c) {
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Utils.getRandom().nextBoolean());
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Utils.getRandom().nextBoolean());
                  } else {
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Utils.getRandom().nextBoolean());
                     KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Utils.getRandom().nextBoolean());
                  }
                  break;
               case 2:
                  KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), this.c);
                  KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), !this.c);
                  break;
               case 3:
                  KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
                  break;
               case 4:
                  KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), true);
                  break;
               case 5:
                  if (!Utils.isMoving() && !Utils.jumpDown()) {
                     this.afkTicks++;
                  } else {
                     if (this.sfTicks > 0 && !Utils.jumpDown()) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                     }

                     this.afkTicks = this.sfTicks = 0;
                  }

                  if (this.afkTicks >= 1000) {
                     if (mc.thePlayer.capabilities.isFlying) {
                        this.stopFlying = true;
                     } else if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                     }

                     this.afkTicks = 0;
                  }

                  if (this.stopFlying && ++this.sfTicks > -1) {
                     if (this.sfTicks == 1) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                     } else if (this.sfTicks == 2) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                     } else if (this.sfTicks == 4) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), true);
                     } else if (this.sfTicks == 5) {
                        KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
                        this.sfTicks = 0;
                        this.stopFlying = false;
                     }
                  }
            }

            switch ((int)this.spin.getInput()) {
               case 1:
                  mc.thePlayer.rotationYaw = (float)(mc.thePlayer.rotationYaw + this.c(this.c));
                  this.d();
                  break;
               case 2:
                  mc.thePlayer.rotationYaw = (float)(mc.thePlayer.rotationYaw + this.c(true));
                  this.d();
                  break;
               case 3:
                  mc.thePlayer.rotationYaw = (float)(mc.thePlayer.rotationYaw + this.c(false));
                  this.d();
            }

            if (this.jump.isToggled() && mc.thePlayer.onGround && (!this.jumpWhenCollided.isToggled() || mc.thePlayer.isCollidedHorizontally)) {
               mc.thePlayer.jump();
            }

            if (this.ticks == 0) {
               if (this.swapItem.isToggled()) {
                  mc.thePlayer.inventory.currentItem = Utils.randomizeInt(0, 8);
               }

               if (this.randomClicks.isToggled()) {
                  ((IAccessorMinecraft)mc).callClickMouse();
               }

               this.ticks = this.h();
               this.c = !this.c;
            }
         }
      }
   }

   private double a() {
      int n = Utils.getRandom().nextBoolean() ? 1 : -1;
      if (!this.randomizeDelta.isToggled()) {
         return 2 * n;
      }

      double n2 = Utils.randomizeInt(100, 500) / 100.0;
      if (n2 % 1.0 == 0.0) {
         n2 += Utils.randomizeInt(1, 10) / 10.0 * n;
      }

      return n2 * n;
   }

   @Override
   public void onDisable() {
      this.b(0);
      this.stop = false;
   }

   private void b(int n) {
      switch (n) {
         case 1:
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
            break;
         case 2:
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
            break;
         case 3:
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
            break;
         case 4:
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
      }
   }

   private int h() {
      return this.minDelay.getInput() == this.maxDelay.getInput()
         ? (int)this.minDelay.getInput()
         : Utils.randomizeInt((int)this.minDelay.getInput(), (int)this.maxDelay.getInput());
   }

   private void d() {
      if (this.randomizePitch.isToggled()) {
         mc.thePlayer.rotationPitch = RotationUtils.clampPitch((float)(mc.thePlayer.rotationPitch + this.a()));
      }
   }

   private double c(boolean b) {
      int n = b ? 1 : -1;
      if (!this.randomizeDelta.isToggled()) {
         return 3 * n;
      }

      double n2 = Utils.randomizeInt(100, 1000) / 100.0;
      if (n2 % 1.0 == 0.0) {
         n2 += Utils.randomizeInt(1, 10) / 10.0 * n;
      }

      return n2 * n;
   }
}
