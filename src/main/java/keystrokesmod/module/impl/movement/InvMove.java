package keystrokesmod.module.impl.movement;

import java.util.Iterator;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendAllPacketsEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Keyboard;

public class InvMove extends Module {
   private SliderSetting modes;
   private int ticks;
   private boolean binds;
   private boolean stopMoving;
   private String[] modesString = new String[]{"Vanilla", "Stop movement", "Motion", "Only menus"};

   public InvMove() {
      super("InvMove", Module.category.movement);
      this.registerSetting(this.modes = new SliderSetting("Modes", 1, this.modesString));
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @SubscribeEvent
   public void onSendPacketAll(SendAllPacketsEvent e) {
      if (Utils.nullCheck()) {
         if (e.getPacket() instanceof C0EPacketClickWindow) {
            this.stopMoving = true;
            this.ticks = 0;
         }
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (Utils.nullCheck()) {
         if (!this.guiCheck()) {
            this.reset();
         } else {
            if (this.stopMoving) {
               this.ticks++;
               if (this.ticks >= 10) {
                  this.ticks = 0;
                  this.stopMoving = false;
               }
            }

            if (this.modes.getInput() == 3.0 && !this.nonInteractGUIs()) {
               this.allowBinds(false);
            } else {
               this.allowBinds(true);
               if (this.modes.getInput() == 1.0) {
                  if (!this.nonInteractGUIs()) {
                     if (this.stopMoving) {
                        this.allowBinds(false);
                     }
                  } else {
                     this.reset();
                  }
               }

               if (this.modes.getInput() == 2.0 && !this.nonInteractGUIs()) {
                  if (!mc.thePlayer.onGround) {
                     this.motionSet(0.56, 0.0);
                  } else if (mc.thePlayer.isSprinting()) {
                     if (this.getSpeedLevel() == 0) {
                        this.motionSet(0.72, 0.03);
                     } else {
                        this.motionSet(0.64, 0.03);
                     }
                  } else {
                     this.motionSet(0.97, 0.03);
                  }
               }

               boolean foodLvlMet = mc.thePlayer.getFoodStats().getFoodLevel() > 6.0F || mc.thePlayer.capabilities.allowFlying;
               if ((Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()) || ModuleManager.sprint.isEnabled())
                  && mc.thePlayer.movementInput.moveForward >= 0.8F
                  && foodLvlMet
                  && !mc.thePlayer.isSprinting()) {
                  mc.thePlayer.setSprinting(true);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (Utils.nullCheck()) {
         if (this.guiCheck()) {
            if (!this.nonInteractGUIs()) {
               if (this.modes.getInput() == 2.0) {
                  mc.thePlayer.movementInput.jump = false;
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (Utils.nullCheck()) {
         if (this.guiCheck()) {
            if (Keyboard.isKeyDown(208) && mc.thePlayer.rotationPitch < 90.0F) {
               mc.thePlayer.rotationPitch++;
            }

            if (Keyboard.isKeyDown(200) && mc.thePlayer.rotationPitch > -90.0F) {
               mc.thePlayer.rotationPitch--;
            }

            if (Keyboard.isKeyDown(205)) {
               mc.thePlayer.rotationYaw++;
            }

            if (Keyboard.isKeyDown(203)) {
               mc.thePlayer.rotationYaw--;
            }
         }
      }
   }

   private void reset() {
      this.ticks = 0;
      this.stopMoving = false;
      if (!this.binds) {
         this.allowBinds(true);
      }
   }

   public boolean active() {
      return ModuleManager.invmove != null && ModuleManager.invmove.isEnabled() && this.guiCheck() && !this.nonInteractGUIs() && this.modes.getInput() == 2.0;
   }

   private int getSpeedLevel() {
      Iterator var1 = mc.thePlayer.getActivePotionEffects().iterator();
      if (var1.hasNext()) {
         PotionEffect potionEffect = (PotionEffect)var1.next();
         return potionEffect.getEffectName().equals("potion.moveSpeed") ? potionEffect.getAmplifier() + 1 : 0;
      } else {
         return 0;
      }
   }

   private boolean guiCheck() {
      return mc.currentScreen == null ? false : !(mc.currentScreen instanceof GuiChat);
   }

   private boolean nonInteractGUIs() {
      return mc.currentScreen instanceof ClickGui ? true : mc.currentScreen instanceof GuiIngameMenu;
   }

   private void motionSet(double val, double strafe) {
      mc.thePlayer.motionX = mc.thePlayer.motionX * (mc.thePlayer.moveStrafing == 0.0F ? val : val - strafe);
      mc.thePlayer.motionZ = mc.thePlayer.motionZ * (mc.thePlayer.moveStrafing == 0.0F ? val : val - strafe);
   }

   private void allowBinds(boolean allowKeys) {
      if (allowKeys) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), Utils.jumpDown());
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), Keyboard.isKeyDown(mc.gameSettings.keyBindSprint.getKeyCode()));
         this.binds = true;
      } else {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
         this.binds = false;
      }
   }
}
