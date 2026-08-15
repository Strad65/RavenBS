package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Bhop extends Module {
   public SliderSetting mode;
   public static SliderSetting friction;
   public static SliderSetting speedSetting;
   private ButtonSetting liquidDisable;
   public ButtonSetting disablerOnly;
   private ButtonSetting sneakDisable;
   private ButtonSetting jumpMoving;
   private ButtonSetting jumpUnblocking;
   public ButtonSetting slowBackwards;
   public ButtonSetting damageBoost;
   public ButtonSetting strafe;
   public ButtonSetting damageBoostRequireKey;
   public GroupSetting damageBoostGroup;
   public GroupSetting strafeGroup;
   private SliderSetting strafeDegrees;
   public KeySetting damageBoostKey;
   public String[] modes = new String[]{"Strafe", "Ground", "Legit", "9 tick", "8 tick", "7 tick", "NCP"};
   public boolean hopping;
   public boolean lowhop;
   public boolean didMove;
   public boolean setRotation;
   public boolean isNormalPos;
   public boolean running;
   private int dt;

   public Bhop() {
      super("Bhop", Module.category.movement);
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
      this.registerSetting(this.disablerOnly = new ButtonSetting("Require disabler", false));
      this.registerSetting(speedSetting = new SliderSetting("Speed", 2.0, 0.8, 3.0, 0.01));
      this.registerSetting(friction = new SliderSetting("Friction multiplier", 1.0, 1.0, 1.3, 0.01));
      this.registerSetting(this.liquidDisable = new ButtonSetting("Disable in liquid", true));
      this.registerSetting(this.sneakDisable = new ButtonSetting("Disable while sneaking", false));
      this.registerSetting(this.jumpMoving = new ButtonSetting("Only jump when moving", false));
      this.registerSetting(this.jumpUnblocking = new ButtonSetting("Only jump while unblocking", false));
      this.registerSetting(this.slowBackwards = new ButtonSetting("Slow backwards", false));
      this.registerSetting(this.damageBoostGroup = new GroupSetting("Damage boost"));
      this.registerSetting(this.damageBoost = new ButtonSetting(this.damageBoostGroup, "Enable Damage boost", false));
      this.registerSetting(this.damageBoostRequireKey = new ButtonSetting(this.damageBoostGroup, "Require key", false));
      this.registerSetting(this.damageBoostKey = new KeySetting(this.damageBoostGroup, "Enable key", 51));
      this.registerSetting(this.strafeGroup = new GroupSetting("Direction strafe"));
      this.registerSetting(this.strafe = new ButtonSetting(this.strafeGroup, "Enable Direction strafe", false));
      this.registerSetting(this.strafeDegrees = new SliderSetting(this.strafeGroup, "Degrees", 80.0, 50.0, 90.0, 5.0));
   }

   @Override
   public void guiUpdate() {
      this.damageBoostKey.setVisible(this.damageBoostRequireKey.isToggled(), this);
      this.disablerOnly.setVisible(this.mode.getInput() >= 3.0, this);
      speedSetting.setVisible(this.mode.getInput() != 6.0, this);
   }

   @Override
   public String getInfo() {
      return this.modes[(int)this.mode.getInput()];
   }

   private boolean conditions() {
      return (!this.jumpMoving.isToggled() || Utils.isMoving()) && (!this.jumpUnblocking.isToggled() || !ModuleUtils.isBlocked);
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      if (this.dt > 0) {
         this.dt--;
      }

      if ((!mc.thePlayer.isInWater() && !mc.thePlayer.isInLava() || !this.liquidDisable.isToggled())
         && (!mc.thePlayer.isSneaking() || !this.sneakDisable.isToggled())) {
         if (!ModuleManager.scaffold.moduleEnabled && !ModuleManager.scaffold.lowhop) {
            if (!LongJump.function) {
               if (!ModuleManager.invmove.active()) {
                  if (this.mode.getInput() >= 0.0 && this.mode.getInput() != 6.0 && this.mode.getInput() != 2.0) {
                     if (mc.thePlayer.onGround && this.conditions()) {
                        if (mc.thePlayer.moveForward <= -0.5
                           && !ModuleManager.killAura.rotating
                           && !Utils.noSlowingBackWithBow()
                           && !ModuleManager.scaffold.isEnabled) {
                           this.setRotation = true;
                        }

                        if (this.mode.getInput() != 3.0) {
                           mc.thePlayer.jump();
                        } else {
                           mc.thePlayer.motionY = 0.41999998688698;
                        }

                        this.running = true;
                        if (mc.thePlayer.posY % 1.0 == 0.0) {
                           this.isNormalPos = true;
                        } else {
                           this.isNormalPos = false;
                        }

                        double speed = speedSetting.getInput() - 0.52;
                        double speedModifier = speed;
                        int speedAmplifier = Utils.getSpeedAmplifier();
                        switch (speedAmplifier) {
                           case 1:
                              speedModifier = speed + 0.02;
                              break;
                           case 2:
                              speedModifier = speed + 0.04;
                              break;
                           case 3:
                              speedModifier = speed + 0.1;
                        }

                        if (Utils.isMoving()) {
                           if (!Utils.noSlowingBackWithBow() && !ModuleManager.sprint.disableBackwards() && !this.slowBackwards()) {
                              Utils.setSpeed(speedModifier * ModuleUtils.applyFrictionMulti());
                           } else {
                              Utils.setSpeed(speedModifier - 0.3);
                           }

                           this.didMove = true;
                        }

                        this.hopping = true;
                     }

                     if (mc.thePlayer.moveForward <= 0.5 && this.hopping) {
                        ModuleUtils.handleSlow();
                     }

                     if (!mc.thePlayer.onGround) {
                        this.hopping = false;
                     }
                  }

                  switch ((int)this.mode.getInput()) {
                     case 0:
                        if (Utils.isMoving() && !mc.thePlayer.onGround) {
                           Utils.setSpeed(Utils.getHorizontalSpeed());
                        }
                     case 1:
                     default:
                        break;
                     case 6:
                        if (mc.thePlayer.onGround && (!this.jumpMoving.isToggled() || Utils.isMoving())) {
                           mc.thePlayer.jump();
                           double speed = this.getNCPSpeed();
                           double speedModifier = speed;
                           int speedAmplifier = Utils.getSpeedAmplifier();
                           switch (speedAmplifier) {
                              case 1:
                                 speedModifier = speed + 0.05;
                                 break;
                              case 2:
                                 speedModifier = speed + 0.1;
                                 break;
                              case 3:
                                 speedModifier = speed + 0.2;
                           }

                           Utils.setSpeed(speedModifier);
                           if (mc.thePlayer.hurtTime > 0 && !LongJump.function) {
                              if (Utils.getHorizontalSpeed() < 0.5) {
                                 Utils.setSpeed(0.5);
                              } else {
                                 Utils.setSpeed(Utils.getHorizontalSpeed());
                              }
                           }

                           this.running = true;
                        }

                        if (!mc.thePlayer.onGround) {
                           Utils.setSpeed(Utils.getHorizontalSpeed());
                        }
                  }

                  if (this.strafe.isToggled()) {
                     this.airStrafe();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (mc.thePlayer.onGround && !mc.thePlayer.capabilities.isFlying && !ModuleManager.scaffold.isEnabled) {
         if (this.hopping) {
            mc.thePlayer.movementInput.jump = false;
         }

         if (this.mode.getInput() == 2.0 && this.conditions()) {
            mc.thePlayer.movementInput.jump = true;
         }
      }
   }

   private double getNCPSpeed() {
      double speed = 0.88;
      Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
      if (block instanceof BlockSlab || block instanceof BlockStairs || mc.thePlayer.isCollidedHorizontally) {
         this.dt = 4;
      }

      if (this.dt > 0) {
         speed = 0.54;
      }

      return speed;
   }

   private boolean slowBackwards() {
      return this.slowBackwards.isToggled() && mc.thePlayer.moveForward <= -0.5;
   }

   public float hardcodedYaw() {
      float simpleYaw = 0.0F;
      float f = 0.8F;
      if (mc.thePlayer.moveForward == 0.0F) {
         if (mc.thePlayer.moveStrafing >= f) {
            simpleYaw += 90.0F;
         }

         if (mc.thePlayer.moveStrafing <= -f) {
            simpleYaw -= 90.0F;
         }
      } else if (mc.thePlayer.moveForward <= -f) {
         simpleYaw -= 180.0F;
         if (mc.thePlayer.moveStrafing >= f) {
            simpleYaw -= 45.0F;
         }

         if (mc.thePlayer.moveStrafing <= -f) {
            simpleYaw += 45.0F;
         }
      }

      return simpleYaw;
   }

   private void airStrafe() {
      if (!mc.thePlayer.onGround
         && mc.thePlayer.hurtTime < 3
         && (mc.thePlayer.motionX != 0.0 || mc.thePlayer.motionZ != 0.0)) {
         float moveDir = this.moveDirection(mc.thePlayer.rotationYaw);
         float currentMotionDir = this.strafeDirection();
         float diff = Math.abs(moveDir - currentMotionDir);
         int range = (int)this.strafeDegrees.getInput();
         if (diff > 180 - range && diff < 180 + range) {
            mc.thePlayer.motionX = -(mc.thePlayer.motionX * 0.85);
            mc.thePlayer.motionZ = -(mc.thePlayer.motionZ * 0.85);
         }
      }
   }

   private float moveDirection(float rawYaw) {
      float yaw = (rawYaw % 360.0F + 360.0F) % 360.0F > 180.0F ? (rawYaw % 360.0F + 360.0F) % 360.0F - 360.0F : (rawYaw % 360.0F + 360.0F) % 360.0F;
      float forward = 1.0F;
      if (mc.thePlayer.moveForward < 0.0F) {
         yaw += 180.0F;
      }

      if (mc.thePlayer.moveForward < 0.0F) {
         forward = -0.5F;
      }

      if (mc.thePlayer.moveForward > 0.0F) {
         forward = 0.5F;
      }

      if (mc.thePlayer.moveStrafing > 0.0F) {
         yaw -= 90.0F * forward;
      }

      if (mc.thePlayer.moveStrafing < 0.0F) {
         yaw += 90.0F * forward;
      }

      return yaw;
   }

   private float strafeDirection() {
      float yaw = (float)Math.toDegrees(Math.atan2(-mc.thePlayer.motionX, mc.thePlayer.motionZ));
      if (yaw < 0.0F) {
         yaw += 360.0F;
      }

      return yaw;
   }

   @Override
   public void onDisable() {
      this.hopping = false;
      this.running = false;
      this.dt = 0;
   }
}
