package keystrokesmod.module.impl.player;

import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.script.model.Simulation;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.Utils;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Safewalk extends Module {
   private SliderSetting motion;
   public static ButtonSetting blocksOnly;
   public static ButtonSetting pitchCheck;
   public static ButtonSetting disableOnForward;
   public static ButtonSetting legit;
   private SliderSetting edgeOffset;
   private SliderSetting unsneakDelay;
   private SliderSetting sneakOnJump;
   public static ButtonSetting sneakKeyPressed;
   public static ButtonSetting holdingBlocks;
   public static ButtonSetting rmbDown;
   public static ButtonSetting lookingDown;
   public static ButtonSetting disableForward;
   public boolean isSneaking;
   private boolean wasOn;
   private double HW = 0.3;
   private double[][] CORNERS = new double[][]{{-this.HW, -this.HW}, {this.HW, -this.HW}, {-this.HW, this.HW}, {this.HW, this.HW}};
   private boolean setSneaking;
   private int sneakJumpDelayTicks;
   private int sneakJumpStartTick = -1;
   private int unsneakDelayTicks;
   private int unsneakStartTick = -1;

   public Safewalk() {
      super("Safewalk", Module.category.player, 0);
      this.registerSetting(this.motion = new SliderSetting("Motion", "x", 1.0, 0.5, 1.2, 0.01));
      this.registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
      this.registerSetting(disableOnForward = new ButtonSetting("Disable on forward", false));
      this.registerSetting(pitchCheck = new ButtonSetting("Pitch check", false));
      this.registerSetting(legit = new ButtonSetting("Legit", false));
      this.registerSetting(this.edgeOffset = new SliderSetting("Edge offset", " blocks", 0.0, 0.0, 0.3, 0.01));
      this.registerSetting(this.unsneakDelay = new SliderSetting("Unsneak delay", "ms", 50.0, 50.0, 300.0, 5.0));
      this.registerSetting(this.sneakOnJump = new SliderSetting("Sneak on jump", "ms", 50.0, 50.0, 300.0, 5.0));
      this.registerSetting(sneakKeyPressed = new ButtonSetting("Sneak key pressed", false));
      this.registerSetting(holdingBlocks = new ButtonSetting("Holding blocks", false));
      this.registerSetting(rmbDown = new ButtonSetting("RMB down", false));
      this.registerSetting(lookingDown = new ButtonSetting("Looking down", false));
      this.registerSetting(disableForward = new ButtonSetting("Disable on forward", false));
   }

   @Override
   public void guiUpdate() {
      blocksOnly.setVisible(!legit.isToggled(), this);
      disableOnForward.setVisible(!legit.isToggled(), this);
      pitchCheck.setVisible(!legit.isToggled(), this);
      this.edgeOffset.setVisible(legit.isToggled(), this);
      this.unsneakDelay.setVisible(legit.isToggled(), this);
      this.sneakOnJump.setVisible(legit.isToggled(), this);
      sneakKeyPressed.setVisible(legit.isToggled(), this);
      holdingBlocks.setVisible(legit.isToggled(), this);
      rmbDown.setVisible(legit.isToggled(), this);
      lookingDown.setVisible(legit.isToggled(), this);
      disableForward.setVisible(legit.isToggled(), this);
   }

   @Override
   public String getInfo() {
      return legit.isToggled() ? (int)this.unsneakDelay.getInput() + "ms" : "";
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @Override
   public void onUpdate() {
      if (this.motion.getInput() != 1.0 && mc.thePlayer.onGround && Utils.isMoving() && safewalkSettingsMet()) {
         mc.thePlayer.motionX = mc.thePlayer.motionX * this.motion.getInput();
         mc.thePlayer.motionZ = mc.thePlayer.motionZ * this.motion.getInput();
      }
   }

   public static boolean canSafeWalk() {
      if (ModuleManager.safeWalk == null || !ModuleManager.safeWalk.isEnabled()) {
         return false;
      } else {
         return legit.isToggled() ? false : safewalkSettingsMet();
      }
   }

   private static boolean safewalkSettingsMet() {
      if (ModuleManager.scaffold.isEnabled) {
         return false;
      }

      if (blocksOnly.isToggled()) {
         ItemStack held = mc.thePlayer.getHeldItem();
         if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return false;
         }
      }

      return disableOnForward.isToggled() && mc.thePlayer.moveForward > -0.2
         ? false
         : !pitchCheck.isToggled() || !(mc.thePlayer.rotationPitch < 70.0F);
   }

   public boolean legitScafSettingsMet() {
      if (!Utils.tabbedIn()) {
         return false;
      }

      if (ModuleManager.scaffold.isEnabled) {
         return false;
      }

      if (holdingBlocks.isToggled()) {
         ItemStack held = mc.thePlayer.getHeldItem();
         if (held == null || !(held.getItem() instanceof ItemBlock)) {
            return false;
         }
      }

      if (disableForward.isToggled() && mc.thePlayer.moveForward > -0.2) {
         return false;
      } else if (lookingDown.isToggled() && mc.thePlayer.rotationPitch < 70.0F) {
         return false;
      } else if (sneakKeyPressed.isToggled() && !Utils.isBindDown(mc.gameSettings.keyBindSneak)) {
         return false;
      } else {
         return !sneakKeyPressed.isToggled() && Utils.isBindDown(mc.gameSettings.keyBindSneak)
            ? false
            : !rmbDown.isToggled() || Utils.keybinds.isMouseDown(1);
      }
   }

   private void reset() {
      if (legit.isToggled()) {
         this.setSneaking = false;
         this.sneakJumpDelayTicks = this.unsneakDelayTicks = 0;
         this.sneakJumpStartTick = this.unsneakStartTick = -1;
         if (this.wasOn) {
            this.wasOn = false;
            if (!Utils.tabbedIn()) {
               if (mc.thePlayer.isSneaking()) {
                  Utils.setSneak(false);
               }
            } else {
               if (mc.thePlayer.isSneaking() && !Utils.sneakDown()) {
                  Utils.setSneak(false);
               }

               if (!mc.thePlayer.isSneaking() && Utils.sneakDown()) {
                  Utils.setSneak(true);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPrePlayerInput(PrePlayerInputEvent e) {
      if (legit.isToggled()) {
         if (!this.legitScafSettingsMet()) {
            this.reset();
         } else {
            if (!this.setSneaking) {
               e.setSneak(false);
            }
         }
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (legit.isToggled()) {
         if (!this.legitScafSettingsMet()) {
            this.reset();
         } else {
            this.wasOn = true;
            if (Utils.jumpDown()
               && mc.thePlayer.onGround
               && (mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F)
               && this.sneakOnJump.getInput() > 0.0) {
               this.sneakJumpStartTick = mc.thePlayer.ticksExisted;
               double raw = this.sneakOnJump.getInput() / 50.0;
               int base = (int)raw;
               this.sneakJumpDelayTicks = base + (Utils.randomizeDouble(0.0, 1.0) < raw - base ? 1 : 0);
               this.pressSneak(true);
            } else {
               Vec3 position = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
               Simulation sim = Simulation.create();
               if (mc.thePlayer.isSneaking()) {
                  sim.setForward(mc.thePlayer.moveForward / 0.3F);
                  sim.setStrafe(mc.thePlayer.moveStrafing / 0.3F);
                  sim.setSneak(false);
               }

               sim.tick();
               Vec3 simPosition = sim.getPosition();
               double edgeOffsetv = this.computeEdgeOffset(simPosition, position);
               if (Double.isNaN(edgeOffsetv)) {
                  if (this.setSneaking) {
                     this.tryReleaseSneak(true);
                  }
               } else {
                  boolean shouldSneak = edgeOffsetv > this.edgeOffset.getInput();
                  boolean shouldRelease = this.setSneaking;
                  if (shouldSneak) {
                     this.pressSneak(true);
                  } else if (shouldRelease) {
                     this.tryReleaseSneak(true);
                  }
               }
            }
         }
      }
   }

   private void pressSneak(boolean resetDelay) {
      Utils.setSneak(true);
      this.setSneaking = true;
      if (resetDelay) {
         this.unsneakStartTick = -1;
      }
   }

   private void tryReleaseSneak(boolean resetDelay) {
      int existed = mc.thePlayer.ticksExisted;
      if (this.unsneakStartTick == -1 && this.sneakJumpStartTick == -1) {
         this.unsneakStartTick = existed;
         double raw = (this.unsneakDelay.getInput() - 50.0) / 50.0;
         int base = (int)raw;
         this.unsneakDelayTicks = base + (Utils.randomizeDouble(0.0, 1.0) < raw - base ? 1 : 0);
      }

      if (existed - this.sneakJumpStartTick < this.sneakJumpDelayTicks) {
         this.pressSneak(false);
      } else if (existed - this.unsneakStartTick < this.unsneakDelayTicks) {
         this.pressSneak(false);
      } else {
         this.releaseSneak(resetDelay);
      }
   }

   private void releaseSneak(boolean resetDelay) {
      Utils.setSneak(false);
      this.setSneaking = false;
      if (resetDelay) {
         this.unsneakStartTick = this.sneakJumpStartTick = -1;
      }
   }

   private double computeEdgeOffset(Vec3 pos1, Vec3 pos2) {
      int floorY = (int)(pos1.y - 0.01);
      double best = Double.NaN;

      for (double[] c : this.CORNERS) {
         int bx = (int)Math.floor(pos2.x + c[0]);
         int bz = (int)Math.floor(pos2.z + c[1]);
         if (!ScriptDefaults.world.getBlockAt(bx, floorY, bz).name.equals("air")) {
            double offX = Math.abs(pos1.x - (bx + (pos1.x < bx + 0.5 ? 0 : 1)));
            double offZ = Math.abs(pos1.z - (bz + (pos1.z < bz + 0.5 ? 0 : 1)));
            boolean xDiff = (int)Math.floor(pos1.x) != bx;
            boolean zDiff = (int)Math.floor(pos1.z) != bz;
            double cornerDist;
            if (xDiff) {
               cornerDist = zDiff ? Math.max(offX, offZ) : offX;
            } else {
               cornerDist = zDiff ? offZ : 0.0;
            }

            best = Double.isNaN(best) ? cornerDist : Math.min(best, cornerDist);
         }
      }

      return best;
   }
}
