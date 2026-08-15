package keystrokesmod.module.impl.player;

import java.util.Iterator;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Tower extends Module {
   public final SliderSetting towerMove;
   private SliderSetting speedSetting;
   private ButtonSetting disableDiagonal;
   public final SliderSetting verticalTower;
   private final SliderSetting slowedSpeed;
   private final SliderSetting slowedTicks;
   private final ButtonSetting disableWhileHurt;
   private GroupSetting cancelKnockbackGroup;
   private final ButtonSetting cancelKnockback;
   private ButtonSetting cancelVelocityRequired;
   public SliderSetting extraBlockDelay;
   private final String[] towerMoveModes = new String[]{"None", "Vanilla", "3 tick", "Edge", "2.5 tick", "1.5 tick", "1 tick", "10 tick", "Jump"};
   private final String[] verticalTowerModes = new String[]{"None", "Vanilla", "Extra block", "3 tick", "Edge", "Prediction"};
   private int slowTicks;
   private boolean wasTowering;
   private boolean vtowering;
   private int towerTicks;
   public boolean towering;
   private boolean hasTowered;
   private boolean startedTowerInAir;
   private boolean setLowMotion;
   private boolean firstJump;
   private int cMotionTicks;
   private int placeTicks;
   public int dCount;
   public float yaw;
   private int vt;
   public boolean blink;
   public int activeTicks;
   public float pitch;
   public boolean finishedTower;
   public boolean delay;
   public int delayTicks;
   private boolean aligning;
   private boolean aligned;
   private boolean placed;
   private double blockX;
   private double firstX;
   private double firstY;
   private double firstZ;
   public boolean placeExtraBlock;
   public int ebDelay;
   public boolean firstVTP;
   public boolean speed;
   private int grounds;
   private int towerVL;
   public int upFaces;
   private boolean jump;
   private boolean disableJump;

   public Tower() {
      super("Tower", Module.category.player);
      this.registerSetting(this.towerMove = new SliderSetting("Tower Move", 0, this.towerMoveModes));
      this.registerSetting(this.speedSetting = new SliderSetting("Speed", 3.0, 0.5, 8.0, 0.1));
      this.registerSetting(this.disableDiagonal = new ButtonSetting("Disable while diagonal", false));
      this.registerSetting(this.verticalTower = new SliderSetting("Vertical Tower", 0, this.verticalTowerModes));
      this.registerSetting(this.extraBlockDelay = new SliderSetting("Extra block delay", "", 0.0, 0.0, 10.0, 1.0));
      this.registerSetting(this.slowedSpeed = new SliderSetting("Slowed speed", "%", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(this.slowedTicks = new SliderSetting("Slowed ticks", 1.0, 0.0, 20.0, 1.0));
      this.registerSetting(this.disableWhileHurt = new ButtonSetting("Disable while hurt", false));
      this.registerSetting(this.cancelKnockbackGroup = new GroupSetting("Cancel knockback"));
      this.registerSetting(this.cancelKnockback = new ButtonSetting(this.cancelKnockbackGroup, "Enable Cancel knockback", false));
      this.registerSetting(this.cancelVelocityRequired = new ButtonSetting(this.cancelKnockbackGroup, "Require velocity enabled", false));
      this.canBeEnabled = false;
   }

   @Override
   public void guiUpdate() {
      this.extraBlockDelay.setVisible(this.verticalTower.getInput() == 2.0, this);
      this.speedSetting.setVisible(this.towerMove.getInput() > 0.0 && (!Settings.movementFix.isToggled() || this.towerMove.getInput() != 8.0), this);
      this.disableDiagonal.setVisible(this.towerMove.getInput() > 0.0, this);
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (Utils.nullCheck() && this.cancelKnockback()) {
         if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity)e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
               e.setCanceled(true);
            }
         } else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
         }
      }
   }

   boolean disableDiag() {
      return this.disableDiagonal.isToggled() && Utils.scaffoldDiagonal(false);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onClientRotation(ClientRotationEvent e) {
      if (Utils.nullCheck()) {
         int valY = (int)Math.round(mc.thePlayer.posY % 1.0 * 10000.0);
         int simpleY = (int)Math.round(mc.thePlayer.posY % 1.0 * 100.0);
         if (this.towerVL > 0) {
            this.towerVL--;
         }

         if (this.delay && ++this.delayTicks > 2) {
            this.delay = false;
            this.delayTicks = 0;
         }

         this.blink = false;
         if (this.towerMove.getInput() > 0.0) {
            if (this.canTower() && Utils.keysDown() && !this.delay && !this.disableDiag()) {
               this.activeTicks++;
               this.speed = false;
               this.wasTowering = this.hasTowered = true;
               if (this.disableWhileHurt.isToggled() && ModuleUtils.damage) {
                  this.towerTicks = 0;
                  this.towering = false;
                  return;
               }

               label405: {
                  switch ((int)this.towerMove.getInput()) {
                     case 1:
                        mc.thePlayer.motionY = 0.41965;
                        switch (this.towerTicks) {
                           case 1:
                              mc.thePlayer.motionY = 0.33;
                              break;
                           case 2:
                              mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                        }

                        if (this.towerTicks >= 3) {
                           this.towerTicks = 0;
                        }
                     case 2:
                        break;
                     case 3:
                        if ((mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround || this.towerVL > 0) && !this.setLowMotion) {
                           this.towering = true;
                        }

                        if (!this.towering) {
                           if (this.setLowMotion) {
                              this.cMotionTicks++;
                              if (this.cMotionTicks == 1) {
                                 mc.thePlayer.motionY = 0.08F;
                                 Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                              } else if (this.cMotionTicks == 4) {
                                 this.cMotionTicks = 0;
                                 this.setLowMotion = false;
                                 this.towering = true;
                                 Utils.setSpeed(this.getTowerGroundSpeed(this.getSpeedLevel()) - 0.02);
                              }
                           }
                           break label405;
                        }

                        if (valY == 0) {
                           mc.thePlayer.motionY = 0.42F;
                           Utils.setSpeed(this.getTowerGroundSpeed(this.getSpeedLevel()));
                           this.startedTowerInAir = false;
                        } else if (valY > 4000 && valY < 4300) {
                           mc.thePlayer.motionY = 0.33F;
                           Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                           this.speed = true;
                        } else if (valY > 7000) {
                           if (this.setLowMotion) {
                              this.towering = false;
                           }

                           mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                        }

                        this.towerVL = 2;
                        break label405;
                     case 4:
                        if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround) {
                           this.towering = true;
                        }

                        if (this.towering) {
                           this.towerTicks = mc.thePlayer.onGround ? 0 : ++this.towerTicks;
                           switch (simpleY) {
                              case 0:
                                 mc.thePlayer.motionY = 0.42F;
                                 if (this.towerTicks == 6) {
                                    mc.thePlayer.motionY = -0.078400001525879;
                                 }

                                 Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                                 this.speed = true;
                                 break label405;
                              case 42:
                                 mc.thePlayer.motionY = 0.33F;
                                 Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                                 this.speed = true;
                                 break label405;
                              case 75:
                                 mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                           }
                        }
                        break label405;
                     case 5:
                        if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround) {
                           this.towering = true;
                        }

                        if (this.towering) {
                           this.towerTicks = mc.thePlayer.onGround ? 0 : ++this.towerTicks;
                           switch (this.towerTicks) {
                              case 0:
                                 mc.thePlayer.motionY = 0.42F;
                                 Utils.setSpeed(this.get15tickspeed(this.getSpeedLevel()));
                                 this.speed = true;
                                 break label405;
                              case 1:
                                 mc.thePlayer.motionY = 0.33F;
                                 Utils.setSpeed(Utils.getHorizontalSpeed());
                                 break label405;
                              case 2:
                                 mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                 break label405;
                              case 3:
                                 mc.thePlayer.motionY = 0.42F;
                                 Utils.setSpeed(Utils.getHorizontalSpeed());
                                 break label405;
                              case 4:
                                 mc.thePlayer.motionY = 0.33F;
                                 Utils.setSpeed(Utils.getHorizontalSpeed());
                                 break label405;
                              case 5:
                                 mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0 + 1.0E-7;
                                 break label405;
                              case 6:
                                 mc.thePlayer.motionY = -0.01F;
                           }
                        }
                        break label405;
                     case 6:
                        if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround) {
                           this.grounds++;
                        }

                        if (mc.thePlayer.posY % 1.0 == 0.0) {
                           this.towering = true;
                        }

                        if (this.towering) {
                           this.towerTicks = mc.thePlayer.onGround ? 0 : ++this.towerTicks;
                           switch (this.towerTicks) {
                              case 0:
                                 mc.thePlayer.motionY = 0.42F;
                                 Utils.setSpeed(this.get1tickspeed(this.getSpeedLevel()));
                                 this.speed = true;
                                 break label405;
                              case 1:
                                 mc.thePlayer.motionY = 0.33F;
                                 Utils.setSpeed(Utils.getHorizontalSpeed());
                                 break label405;
                              case 2:
                                 mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                 break label405;
                              case 3:
                                 mc.thePlayer.motionY = 0.005;
                           }
                        }
                        break label405;
                     case 7:
                        if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround) {
                           this.towering = true;
                        }

                        if (this.towering) {
                           this.towerTicks++;
                           switch (this.towerTicks) {
                              case 1:
                              case 4:
                              case 7:
                                 mc.thePlayer.motionY = 0.42F;
                                 Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                                 this.speed = true;
                                 break label405;
                              case 2:
                              case 5:
                              case 8:
                                 mc.thePlayer.motionY = 0.33F;
                                 Utils.setSpeed(Utils.getHorizontalSpeed());
                                 break label405;
                              case 3:
                              case 6:
                                 mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                                 break label405;
                              case 9:
                                 mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0 + 1.0E-7;
                                 break label405;
                              case 10:
                                 mc.thePlayer.motionY = -0.01F;
                                 this.towerTicks = 0;
                                 Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                                 this.speed = true;
                           }
                        }
                        break label405;
                     case 8:
                        if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround) {
                           this.towering = true;
                        }

                        if (this.towering && mc.thePlayer.onGround) {
                           if (Settings.movementFix.isToggled()) {
                              this.jump = !ModuleManager.scaffold.jumpFacingForward.isToggled();
                           } else {
                              mc.thePlayer.jump();
                              Utils.setSpeed(this.get15tickspeed(this.getSpeedLevel()));
                           }

                           this.speed = true;
                        }
                     default:
                        break label405;
                  }

                  if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround || this.towerVL > 0) {
                     this.towering = true;
                  }

                  if (this.towering) {
                     if (valY == 0) {
                        mc.thePlayer.motionY = 0.42F;
                        Utils.setSpeed(this.getTowerGroundSpeed(this.getSpeedLevel()));
                        this.startedTowerInAir = false;
                     } else if (valY > 4000 && valY < 4300) {
                        mc.thePlayer.motionY = 0.33F;
                        Utils.setSpeed(this.getTowerSpeed(this.getSpeedLevel()));
                        this.speed = true;
                     } else if (valY > 7000) {
                        mc.thePlayer.motionY = 1.0 - mc.thePlayer.posY % 1.0;
                     }

                     this.towerVL = 2;
                  }
               }

               if (this.speed) {
                  if (!Settings.movementFix.isToggled()) {
                     this.blink = true;
                     ModuleManager.scaffold.rotateForward(false);
                  } else {
                     ModuleManager.scaffold.rotateForward(true);
                  }

                  Scaffold.jumpDelayVal = 5;
                  Scaffold.airTickVal = 4;
               }
            } else {
               if (this.finishedTower) {
                  this.finishedTower = false;
               }

               if (this.hasTowered) {
                  this.finishedTower = true;
               }

               if (this.wasTowering && this.modulesEnabled()) {
                  if (this.slowedTicks.getInput() > 0.0 && this.slowedTicks.getInput() != 100.0 && this.slowTicks++ < this.slowedTicks.getInput()) {
                     mc.thePlayer.motionX = mc.thePlayer.motionX * (this.slowedSpeed.getInput() / 100.0);
                     mc.thePlayer.motionZ = mc.thePlayer.motionZ * (this.slowedSpeed.getInput() / 100.0);
                  } else {
                     ModuleUtils.handleSlow();
                  }

                  if (this.slowTicks >= this.slowedTicks.getInput()) {
                     this.slowTicks = 0;
                     this.wasTowering = false;
                  }
               } else {
                  if (this.wasTowering) {
                     this.wasTowering = false;
                  }

                  this.slowTicks = 0;
               }

               if ((this.speed || this.hasTowered && mc.thePlayer.onGround) && !Settings.movementFix.isToggled()) {
                  Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
               }

               this.hasTowered = this.towering = this.firstJump = this.startedTowerInAir = this.setLowMotion = this.speed = this.jump = false;
               this.cMotionTicks = this.placeTicks = this.towerTicks = this.grounds = this.upFaces = this.activeTicks = 0;
               this.reset();
            }
         }

         if (this.verticalTower.getInput() > 0.0) {
            if (this.canTower() && !Utils.keysDown()) {
               this.wasTowering = true;
               switch ((int)this.verticalTower.getInput()) {
                  case 1:
                     mc.thePlayer.motionY = 0.42F;
                     break;
                  case 2:
                     if (!this.aligned) {
                        if (mc.thePlayer.onGround) {
                           if (!this.aligning) {
                              this.blockX = (int)mc.thePlayer.posX + 1;
                              this.firstX = mc.thePlayer.posX - 10.0;
                              this.firstY = mc.thePlayer.posY;
                              this.firstZ = mc.thePlayer.posZ;
                           }

                           mc.thePlayer.motionX = 0.2;
                           this.aligning = true;
                        }

                        if (this.aligning && mc.thePlayer.posX >= this.blockX) {
                           this.aligned = true;
                        }

                        this.yaw = RotationUtils.getRotations(this.firstX, this.firstY, this.firstZ)[0];
                        this.pitch = 0.0F;
                     }

                     if (this.aligned) {
                        if (this.placed) {
                           this.yaw = RotationUtils.getRotations(this.firstX, this.firstY, this.firstZ)[0];
                           this.pitch = 86.6F;
                        } else {
                           this.yaw = RotationUtils.getRotations(this.firstX, this.firstY, this.firstZ)[0];
                           this.pitch = 0.0F;
                        }

                        this.placeExtraBlock = true;
                        Utils.setSpeed(0.0);
                        mc.thePlayer.motionX = 0.0;
                        mc.thePlayer.motionY = this.verticalTowerValue();
                        mc.thePlayer.motionZ = 0.0;
                        this.towerVL = 2;
                     }
                     break;
                  case 3:
                     if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround || this.towerVL > 0) {
                        this.vtowering = true;
                     }

                     if (this.vtowering) {
                        mc.thePlayer.motionY = this.verticalTowerValue();
                        this.towerVL = 2;
                        mc.thePlayer.motionX = 0.0;
                        mc.thePlayer.motionZ = 0.0;
                     }
                     break;
                  case 4:
                     if (mc.thePlayer.posY % 1.0 == 0.0 && mc.thePlayer.onGround || this.towerVL > 0) {
                        this.vtowering = true;
                     }

                     if (this.vtowering) {
                        this.vt++;
                        this.towerVL = 2;
                        if (this.vt <= 6 && this.verticalTowerValue() != 0.0) {
                           mc.thePlayer.motionY = this.verticalTowerValue();
                        } else {
                           this.vt = 0;
                        }
                     }
                     break;
                  case 5:
                     if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.42F;
                     }

                     if (mc.thePlayer.motionY <= 0.0 && Utils.getHorizontalSpeed() <= 0.02 && mc.thePlayer.motionY >= -0.09) {
                        mc.thePlayer.motionY = -0.38;
                     }
               }
            } else {
               this.yaw = this.pitch = 0.0F;
               this.aligning = this.aligned = this.placed = this.vtowering = false;
               this.firstX = 0.0;
               this.placeExtraBlock = this.firstVTP = false;
               this.ebDelay = this.vt = 0;
               ModuleManager.scaffold.placedVP = false;
            }
         }
      }
   }

   public boolean isVerticalTowering() {
      return this.canTower() && !Utils.keysDown() && this.verticalTower.getInput() > 0.0;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      this.disableJump = false;
      if (ModuleManager.scaffold.isEnabled) {
         if (this.canTower() && Utils.keysDown() && this.towerMove.getInput() > 0.0 && !this.disableDiag()) {
            this.disableJump = true;
            if (!this.firstJump) {
               if (!mc.thePlayer.onGround) {
                  if (!this.startedTowerInAir) {
                  }

                  this.startedTowerInAir = true;
               } else if (mc.thePlayer.onGround) {
                  this.firstJump = true;
               }
            }
         }

         if (this.canTower() && !Utils.keysDown() && this.verticalTower.getInput() > 0.0) {
            this.disableJump = true;
         }

         if (this.delay) {
            this.disableJump = true;
         }

         if (this.disableJump) {
            if (this.disableWhileHurt.isToggled() && ModuleUtils.damage && !this.delay) {
               return;
            }

            mc.thePlayer.movementInput.jump = false;
         }
      }
   }

   @SubscribeEvent
   public void onSendPacket(SendPacketEvent e) {
      if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
         if (this.firstVTP) {
            this.ebDelay++;
            if (this.ebDelay >= this.extraBlockDelay.getInput() + 2.0) {
               this.ebDelay = 0;
            }
         }

         if (this.canTower() && Utils.isMoving()) {
            this.placeTicks++;
            if (((C08PacketPlayerBlockPlacement)e.getPacket()).getPlacedBlockDirection() == 1 && this.hasTowered) {
               this.upFaces++;
               if (this.placeTicks > 5) {
                  this.dCount++;
                  if (this.dCount >= 2) {
                     this.setLowMotion = true;
                  }
               }
            } else {
               this.dCount = this.upFaces = 0;
            }
         } else {
            this.placeTicks = this.upFaces = 0;
         }

         if (this.aligned) {
            this.placed = true;
         }
      }
   }

   private void reset() {
      this.towerTicks = 0;
      this.towering = false;
      this.placeTicks = 0;
      this.setLowMotion = false;
   }

   public boolean cancelKnockback() {
      if (!this.canTower()) {
         return false;
      } else {
         return this.cancelVelocityRequired.isToggled() && !ModuleManager.velocity.isEnabled() ? false : this.cancelKnockback.isToggled();
      }
   }

   public boolean canTower() {
      if (Utils.nullCheck()
         && Utils.jumpDown()
         && Utils.tabbedIn()
         && (Utils.isMoving() || this.verticalTower.getInput() != 0.0)
         && (!Utils.isMoving() || this.towerMove.getInput() != 0.0)) {
         if (ModuleManager.scaffold.fastScaffoldKeepY || ModuleManager.scaffold.getJumpLevel() != 0) {
            return false;
         } else if (mc.thePlayer.isCollidedHorizontally) {
            return false;
         } else {
            return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() ? false : this.modulesEnabled();
         }
      } else {
         return false;
      }
   }

   private boolean modulesEnabled() {
      return ModuleManager.scaffold.moduleEnabled && ModuleManager.scaffold.holdingBlocks() && ModuleManager.scaffold.hasSwapped && !LongJump.function;
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

   private double verticalTowerValue() {
      int valY = (int)Math.round(mc.thePlayer.posY % 1.0 * 10000.0);
      double value = 0.0;
      if (valY == 0) {
         value = 0.42F;
      } else if (valY > 4000 && valY < 4300) {
         value = 0.33F;
      } else if (valY > 7000) {
         value = 1.0 - mc.thePlayer.posY % 1.0;
      }

      return value;
   }

   private double getTowerSpeed(int speedLevel) {
      if (speedLevel == 0) {
         return this.speedSetting.getInput() / 10.0;
      } else if (speedLevel == 1) {
         return this.speedSetting.getInput() / 10.0 + 0.04;
      } else if (speedLevel == 2) {
         return this.speedSetting.getInput() / 10.0 + 0.08;
      } else if (speedLevel == 3) {
         return this.speedSetting.getInput() / 10.0 + 0.12;
      } else {
         return speedLevel == 4 ? this.speedSetting.getInput() / 10.0 + 0.12 : this.speedSetting.getInput() / 10.0;
      }
   }

   private double getTowerGroundSpeed(int speedLevel) {
      if (speedLevel == 0) {
         return this.speedSetting.getInput() / 10.0 - 0.085;
      } else if (speedLevel == 1) {
         return this.speedSetting.getInput() / 10.0 - 0.05;
      } else if (speedLevel == 2) {
         return this.speedSetting.getInput() / 10.0;
      } else if (speedLevel == 3) {
         return this.speedSetting.getInput() / 10.0 + 0.05;
      } else {
         return speedLevel == 4 ? this.speedSetting.getInput() / 10.0 + 0.1 : this.speedSetting.getInput() / 10.0 - 0.08;
      }
   }

   private double get15tickspeed(int speedLevel) {
      if (speedLevel == 0) {
         return this.speedSetting.getInput() / 10.0;
      } else if (speedLevel == 1) {
         return this.speedSetting.getInput() / 10.0 + 0.04;
      } else if (speedLevel == 2) {
         return this.speedSetting.getInput() / 10.0 + 0.08;
      } else if (speedLevel == 3) {
         return this.speedSetting.getInput() / 10.0 + 0.12;
      } else {
         return speedLevel == 4 ? this.speedSetting.getInput() / 10.0 + 0.13 : this.speedSetting.getInput() / 10.0;
      }
   }

   private double get1tickspeed(int speedLevel) {
      if (speedLevel == 0) {
         return this.speedSetting.getInput() / 10.0;
      } else if (speedLevel == 1) {
         return this.speedSetting.getInput() / 10.0 + 0.03;
      } else if (speedLevel == 2) {
         return this.speedSetting.getInput() / 10.0 + 0.06;
      } else if (speedLevel == 3) {
         return this.speedSetting.getInput() / 10.0 + 0.1;
      } else {
         return speedLevel == 4 ? this.speedSetting.getInput() / 10.0 + 0.11 : this.speedSetting.getInput() / 10.0;
      }
   }
}
