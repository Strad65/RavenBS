package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import java.awt.Color;
import java.io.IOException;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.shader.BlurUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.opengl.GL11;
import keystrokesmod.utility.BlinkHandler;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.ScaffoldBlockCount;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Scaffold extends Module {
   private final SliderSetting motion;
   public SliderSetting rotation;
   public SliderSetting fakeRotation;
   public SliderSetting sprint;
   private SliderSetting floatFirstJump;
   public SliderSetting fastScaffold;
   private SliderSetting multiPlace;
   private SliderSetting rotationSpeed;
   public ButtonSetting autoSwap;
   private ButtonSetting fastOnRMB;
   public ButtonSetting highlightBlocks;
   public ButtonSetting jumpFacingForward;
   public ButtonSetting safeWalk;
   public ButtonSetting showBlockCount;
   public SliderSetting blockCountBlur;
   public int blockCountPosX = 0;
   public int blockCountPosY = 0;
   private ButtonSetting silentSwing;
   private ButtonSetting prioritizeSprintWithSpeed;
   private String[] rotationModes = new String[]{"§cDisabled", "Simple", "Offset", "Precise", "Center", "Center2"};
   private String[] fakeRotationModes = new String[]{"§cDisabled", "None", "Strict", "Smooth", "Spin", "Precise"};
   private String[] sprintModes = new String[]{"§cDisabled", "Vanilla", "Float"};
   private String[] fastScaffoldModes = new String[]{"§cDisabled", "Jump A", "Jump B", "Jump B Low", "Jump E", "Keep-Y", "Keep-Y Low"};
   private String[] multiPlaceModes = new String[]{"§cDisabled", "1 extra", "2 extra", "3 extra", "4 extra"};
   public Map<BlockPos, Timer> highlight = new HashMap<>();
   public boolean canBlockFade;
   private ScaffoldBlockCount scaffoldBlockCount;
   public AtomicInteger lastSlot = new AtomicInteger(-1);
   private int spoofSlot;
   public boolean hasSwapped;
   private int blockSlot = -1;
   public boolean hasPlaced;
   private boolean finishProcedure;
   private boolean stopUpdate;
   private boolean stopUpdate2;
   private Scaffold.PlaceData lastPlacement;
   private EnumFacing[] facings = new EnumFacing[]{EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP};
   private BlockPos[] offsets = new BlockPos[]{
      new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0)
   };
   private Vec3 targetBlock;
   private Scaffold.PlaceData blockInfo;
   private Vec3 blockPos;
   private Vec3 hitVec;
   private Vec3 lookVec;
   private boolean rotateForward;
   private double startYPos = -1.0;
   public boolean fastScaffoldKeepY;
   public boolean firstKeepYPlace;
   private boolean rotatingForward;
   private int keepYTicks;
   public boolean lowhop;
   private int rotationDelay;
   private boolean floatJumped;
   private boolean floatStarted;
   private boolean floatWasEnabled;
   private boolean floatKeepY;
   public int offsetDelay;
   public boolean placedVP;
   public boolean jump;
   private int floatTicks;
   public boolean blink;
   public boolean canSprint;
   public boolean canSprint2;
   private boolean idle;
   private int idleTicks;
   private boolean didJump;
   private int placeIdle;
   public boolean moduleEnabled;
   public boolean isEnabled;
   private boolean disabledModule;
   private boolean dontDisable;
   private boolean towerEdge;
   private int disableTicks;
   private int scaffoldTicks;
   private boolean lockRotation;
   private boolean was451;
   private boolean was452;
   private float minPitch;
   private float minOffset;
   private float pOffset;
   private float edge;
   private long firstStroke;
   private long yawEdge;
   private long vlS;
   private long swDelay;
   private float lastEdge2;
   private float yawAngle;
   private float theYaw;
   private boolean enabledOffGround = false;
   private float[] blockRotations;
   public float yaw;
   public float pitch;
   public float blockYaw;
   public float yawOffset;
   public float lastOffset;
   private boolean set2;
   private float maxOffset;
   private int sameMouse;
   private int randomF;
   private int yawChanges;
   private int dynamic;
   private boolean getVTR;
   private boolean resetm;
   private float VTRY;
   private float normalYaw;
   private float normalPitch;
   private int switchvl;
   private int dt;
   private float getSmooth;
   private float lastYawS;
   private float smoothedYaw;
   private boolean neg;
   private boolean wasForward;
   private float yawWithOffset;
   private int rt;
   private int forwardTicks;
   private float fakeYaw;
   private float fakePitch;
   private float fakeYaw1;
   private float fakeYaw2;
   private boolean firstRotate;
   private int canSnap;
   private boolean began;
   private boolean cantRotate;
   private boolean startRotation;
   private int srt;
   public static int jumpDelayVal;
   public static int airTickVal;
   private int rnj;
   private int frd;
   private int rnv;
   private int rtd;
   private float rnf = 20.0F;
   private float bv = 20.0F;
   private boolean canForward;
   private int jumpDelay;
   private float uForward;
   private float uStrafe;
   private float lastMY;
   private int back;
   public int b1t;
   private float lrnf;
   private float offsetvv = 45.0F;
   private boolean bvs;
   private int snapDelay;
   private int btm;
   private int scycle;
   private int sv;
   private long vdl;
   private float lastey;
   private static double distance = 3.5;
   double[] speedLevels = new double[]{0.48, 0.5, 0.52, 0.58, 0.68};
   double[] floatSpeedLevels = new double[]{0.2, 0.22, 0.27, 0.29, 0.3};
   private float finalhYaw;

   // Center mode (Telly) state
   private boolean tellyWasAirborne = false;
   private int tellyJumpTimer = 0;
   private static final int TELLY_JUMP_DELAY = 2;

   public Scaffold() {
      super("Scaffold", Module.category.player);
      this.registerSetting(this.motion = new SliderSetting("Motion", "%", 100.0, 50.0, 150.0, 1.0));
      this.registerSetting(this.rotation = new SliderSetting("Rotation", 1, this.rotationModes));
      this.registerSetting(this.fakeRotation = new SliderSetting("Rotation (fake)", 0, this.fakeRotationModes));
      this.registerSetting(this.sprint = new SliderSetting("Sprint mode", 0, this.sprintModes));
      this.registerSetting(this.prioritizeSprintWithSpeed = new ButtonSetting("Prioritize sprint with speed", false));
      this.registerSetting(this.floatFirstJump = new SliderSetting("§eFloat §rfirst jump speed", "%", 100.0, 50.0, 100.0, 1.0));
      this.registerSetting(this.fastScaffold = new SliderSetting("Fast scaffold", 0, this.fastScaffoldModes));
      this.registerSetting(this.multiPlace = new SliderSetting("Multi-place", 0, this.multiPlaceModes));
      this.registerSetting(this.rotationSpeed = new SliderSetting("Rotation speed", "°/t", 180.0, 1.0, 180.0, 1.0));
      this.registerSetting(this.autoSwap = new ButtonSetting("Auto swap", true));
      this.registerSetting(this.fastOnRMB = new ButtonSetting("Fast on RMB", true));
      this.registerSetting(this.highlightBlocks = new ButtonSetting("Highlight blocks", true));
      this.registerSetting(this.jumpFacingForward = new ButtonSetting("Jump facing forward", false));
      this.registerSetting(this.safeWalk = new ButtonSetting("Safewalk", true));
      this.registerSetting(this.showBlockCount = new ButtonSetting("Show block count", true));
      this.registerSetting(this.blockCountBlur = new SliderSetting("Block count blur", "px", 3.0, 0.0, 10.0, 0.5));
      this.registerSetting(new ButtonSetting("Edit block count pos", () -> mc.displayGuiScreen(new Scaffold.BlockCountEditScreen())));
      this.registerSetting(this.silentSwing = new ButtonSetting("Silent swing", false));
      this.alwaysOn = true;
   }

   @Override
   public void guiUpdate() {
      this.prioritizeSprintWithSpeed.setVisible(this.sprint.getInput() > 0.0, this);
      this.floatFirstJump.setVisible(this.sprint.getInput() == 2.0, this);
      boolean showBC = this.showBlockCount.isToggled();
      this.blockCountBlur.setVisible(showBC, this);
   }

   @Override
   public void onDisable() {
      if (ModuleManager.tower.canTower() && (ModuleManager.tower.dCount == 0 || !Utils.isMoving())) {
         this.towerEdge = true;
      }

      this.disabledModule = true;
      this.moduleEnabled = false;
      if (!this.isEnabled) {
         this.scaffoldBlockCount.beginFade();
      }
   }

   @Override
   public void onEnable() {
      this.dt = 0;
      this.isEnabled = true;
      this.moduleEnabled = true;
      ModuleUtils.fadeEdge = 0;
      this.edge = -9.9999994E8F;
      this.minPitch = 80.0F;
      if (!mc.thePlayer.onGround) {
         this.rotationDelay = Utils.randomizeInt(2, 3);
         this.enabledOffGround = true;
      }

      this.lastEdge2 = mc.thePlayer.rotationYaw;
      FMLCommonHandler.instance().bus().register(this.scaffoldBlockCount = new ScaffoldBlockCount(mc));
      this.lastSlot.set(-1);
      this.hasPlaced = false;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onMouse(MouseEvent e) {
      if (this.isEnabled) {
         if (e.button == 0 || e.button == 1) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      if (Utils.nullCheck()) {
         this.normalYaw = mc.thePlayer.rotationYaw;
         this.normalPitch = mc.thePlayer.rotationPitch;
         if (this.dt <= 0) {
            this.dynamic = 0;
            if (this.targetBlock != null) {
               // Use optimized multi-sample aiming for Center mode
               if (this.rotation.getInput() == 4.0 && this.blockInfo != null) {
                  this.blockRotations = this.calculateCenterModeRotations();
               } else if (this.rotation.getInput() == 5.0 && this.blockInfo != null) {
                  this.blockRotations = this.calculateCenter2ModeRotations();
               } else {
                  Vec3 lookAt = new Vec3(
                     this.targetBlock.xCoord - this.lookVec.xCoord,
                     this.targetBlock.yCoord - this.lookVec.yCoord,
                     this.targetBlock.zCoord - this.lookVec.zCoord
                  );
                  this.blockRotations = RotationUtils.getRotations(lookAt);
               }
               this.targetBlock = null;
               this.fakeYaw1 = mc.thePlayer.rotationYaw - this.hardcodedYaw();
               if (this.yawEdge == 0L) {
                  this.randomF = Utils.randomizeInt(0, 9);
                  this.yawEdge = Utils.time();
               }

               this.dynamic++;
            }

            this.randomF = 0;
            if (this.fakeRotation.getInput() > 0.0) {
               if (this.fakeRotation.getInput() == 1.0) {
                  this.fakeYaw = this.normalYaw;
                  this.fakePitch = this.normalPitch;
               } else if (this.fakeRotation.getInput() == 2.0) {
                  this.fakeYaw = this.fakeYaw1;
                  if (this.blockRotations != null) {
                     this.fakePitch = this.blockRotations[1] + 5.0F;
                  } else {
                     this.fakePitch = 80.0F;
                  }
               } else if (this.fakeRotation.getInput() == 3.0) {
                  this.fakeYaw2 = mc.thePlayer.rotationYaw - this.hardcodedYaw();
                  float yawDifference = Utils.getAngleDifference(this.lastEdge2, this.fakeYaw2);

                  // Apply rotation speed limit
                  float maxRotation = (float)this.rotationSpeed.getInput();
                  if (Math.abs(yawDifference) > maxRotation) {
                     yawDifference = Math.copySign(maxRotation, yawDifference);
                  }

                  this.fakeYaw2 = this.lastEdge2 + yawDifference;
                  this.lastEdge2 = this.fakeYaw2;
                  this.fakeYaw = this.fakeYaw2;
                  if (this.blockRotations != null) {
                     this.fakePitch = this.blockRotations[1] + 5.0F;
                  } else {
                     this.fakePitch = 80.0F;
                  }
               } else if (this.fakeRotation.getInput() == 4.0) {
                  this.fakeYaw += 25.714285F;
                  this.fakePitch = 90.0F;
               } else if (this.fakeRotation.getInput() == 5.0) {
                  if (this.blockRotations != null) {
                     this.fakeYaw2 = this.blockRotations[0];
                     this.fakePitch = this.blockRotations[1];
                  } else {
                     this.fakeYaw2 = mc.thePlayer.rotationYaw - this.hardcodedYaw() - 180.0F;
                     this.fakePitch = 88.0F;
                  }

                  float yawDifference = Utils.getAngleDifference(this.lastEdge2, this.fakeYaw2);

                  // Apply rotation speed limit
                  float maxRotation = (float)this.rotationSpeed.getInput();
                  if (Math.abs(yawDifference) > maxRotation) {
                     yawDifference = Math.copySign(maxRotation, yawDifference);
                  }

                  this.fakeYaw2 = this.lastEdge2 + yawDifference;
                  this.lastEdge2 = this.fakeYaw2;
                  this.fakeYaw = this.fakeYaw2;
               }

               RotationUtils.setFakeRotations(this.fakeYaw, this.fakePitch);
            } else if (this.canSprint2 && this.rotation.getInput() == 1.0) {
               RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw - this.hardcodedYaw() - this.offsetvv, this.pitch);
            }

            if (!this.isEnabled) {
               this.dt++;
            } else {
               this.scaffoldTicks++;
               this.canBlockFade = true;
               if (Utils.keysDown()
                  && this.usingFastScaffold()
                  && !ModuleManager.invmove.active()
                  && this.fastScaffold.getInput() >= 1.0
                  && !Utils.jumpDown()
                  && !LongJump.function
                  && this.getJumpLevel() == 0) {
                  this.fastScaffoldKeepY = true;
               } else if (this.fastScaffoldKeepY && mc.thePlayer.onGround) {
                  this.fastScaffoldKeepY = this.firstKeepYPlace = false;
                  this.startYPos = -1.0;
                  this.keepYTicks = 0;
               }

               if (this.fastScaffoldKeepY && mc.thePlayer.onGround && Utils.isMoving() && this.scaffoldTicks > 1) {
                  this.jump = !this.jumpFacingForward.isToggled();
                  jumpDelayVal = 4;
                  airTickVal = 5;
                  this.rotateForward(true);
                  if (this.startYPos == -1.0 || Math.abs(this.startYPos - mc.thePlayer.posY) > 2.0) {
                     this.startYPos = mc.thePlayer.posY;
                     this.fastScaffoldKeepY = true;
                  }
               }

               if (this.sprint.getInput() == 1.0) {
                  this.canSprint2 = !this.usingFastScaffold()
                     && !this.fastScaffoldKeepY
                     && !Utils.jumpDown()
                     && !LongJump.function
                     && mc.thePlayer.onGround;
               }

               if (this.sprint.getInput() == 2.0) {
                  if (Utils.isMoving() && this.idle && this.idleTicks++ > 4) {
                     if (this.floatKeepY) {
                        this.startYPos = -1.0;
                     }

                     this.floatStarted = this.floatJumped = this.floatKeepY = this.floatWasEnabled = false;
                     this.floatTicks = this.rt = 0;
                     this.canSprint2 = false;
                     this.offsetDelay = 0;
                     this.idle = false;
                     this.idleTicks = 0;
                     ModuleUtils.groundTicks = 9;
                  }

                  if (!this.usingFastScaffold() && !this.fastScaffoldKeepY && !ModuleManager.tower.canTower() && !LongJump.function) {
                     if (ModuleUtils.stillTicks > 2 && mc.thePlayer.onGround) {
                        this.idle = true;
                        this.idleTicks = 0;
                     }

                     this.floatWasEnabled = true;
                     if (!this.floatStarted && this.offsetDelay == 0) {
                        if (ModuleUtils.groundTicks > 8 && mc.thePlayer.onGround) {
                           this.canSprint2 = true;
                           this.floatKeepY = true;
                           this.startYPos = e.posY;
                           this.rotateForward(true);
                           mc.thePlayer.jump();
                           if (Utils.isMoving()) {
                              double fvl = (this.getSpeed(this.getSpeedLevel()) - Utils.randomizeDouble(3.0E-4, 1.0E-4))
                                 * (this.floatFirstJump.getInput() / 100.0);
                              Utils.setSpeed(fvl);
                           }

                           this.floatJumped = true;
                        } else if (ModuleUtils.groundTicks <= 8 && mc.thePlayer.onGround) {
                           this.floatStarted = true;
                        }

                        if (this.floatJumped && !mc.thePlayer.onGround) {
                           this.floatStarted = true;
                        }
                     }

                     if (this.floatStarted && mc.thePlayer.onGround) {
                        this.floatKeepY = false;
                        this.startYPos = -1.0;
                        if (this.moduleEnabled && mc.thePlayer.posY % 1.0 == 0.0) {
                           this.floatTicks++;
                           this.rotateForward = false;
                           this.rotationDelay = 0;
                           ModuleManager.tower.delay = false;
                           this.canSprint2 = true;
                           if (this.didJump && !mc.gameSettings.keyBindJump.isKeyDown() && mc.thePlayer.onGround) {
                              mc.thePlayer.jump();
                           } else if (!this.idle) {
                              switch (this.floatTicks) {
                                 case 1:
                                 case 4:
                                 case 6:
                                    ModuleManager.tower.delay = true;
                                    ModuleManager.tower.delayTicks = 0;
                                    e.setPosY(e.getPosY() + 0.001);
                                 case 2:
                                 case 3:
                                 case 5:
                                 case 7:
                                 default:
                                    break;
                                 case 8:
                                    this.floatTicks = 0;
                              }

                              if (Utils.isMoving() && !ModuleManager.invmove.active()) {
                                 Utils.setSpeed(this.getFloatSpeed(this.getSpeedLevel()));
                              }
                           }

                           ModuleUtils.groundTicks = 0;
                           this.offsetDelay = 2;
                        }
                     }
                  } else if (this.floatWasEnabled && this.moduleEnabled) {
                     if (mc.thePlayer.onGround) {
                        Utils.setSpeed(Utils.getHorizontalSpeed() / 2.0);
                     }

                     if (this.floatKeepY) {
                        this.startYPos = -1.0;
                     }

                     this.floatStarted = this.floatJumped = this.floatKeepY = this.floatWasEnabled = false;
                     this.floatTicks = this.rt = 0;
                     this.canSprint2 = false;
                     this.idle = false;
                     this.idleTicks = 0;
                  }

                  this.didJump = false;
                  if (ModuleManager.tower.delay && mc.gameSettings.keyBindJump.isKeyDown()) {
                     this.didJump = true;
                     this.canSprint2 = true;
                  }
               }

               if (this.blockRotations != null) {
                  if (mc.thePlayer.rotationYaw == this.lastOffset) {
                     this.sameMouse++;
                  } else {
                     this.sameMouse = 0;
                     this.yawChanges++;
                  }

                  if (this.sameMouse > 2) {
                     this.yawChanges = 0;
                  }

                  this.lastOffset = mc.thePlayer.rotationYaw;
                  if (this.yawChanges > 15) {
                     this.randomF = 1;
                     this.yawEdge = Utils.time();
                  }

                  if (this.yawEdge > 0L && Utils.time() - this.yawEdge > 500L) {
                     this.yawEdge = 0L;
                  }
               } else {
                  this.fakeYaw1 = mc.thePlayer.rotationYaw - this.hardcodedYaw();
               }
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onClientRotation(ClientRotationEvent e) {
      if (Utils.nullCheck()) {
         this.canSprint = false;
         if (!this.isEnabled) {
            this.lastMY = this.getMotionYaw();
         } else {
            this.placeIdle++;
            switch ((int)this.rotation.getInput()) {
               case 1:
                  this.simpleRots(e);
                  break;
               case 2:
                  this.offsetRots(e);
                  break;
               case 3:
                  this.preciseRots(e);
                  break;
               case 4:
                  this.centerRots(e);
                  break;
               case 5:
                  this.center2Rots(e);
                  break;
            }

            if (this.edge != 1.0F) {
               this.switchvl++;
               this.edge = 1.0F;
            }

            if (mc.thePlayer.onGround) {
               this.enabledOffGround = false;
            }

            if (this.rotationDelay > 0) {
               this.rotationDelay--;
            }

            if (this.wasForward) {
               this.forwardTicks++;
            }

            if (ModuleUtils.inAirTicks >= airTickVal || this.rotation.getInput() != 1.0 && !mc.thePlayer.onGround) {
               this.rotateForward = false;
               this.jumpDelay = 0;
               this.forwardTicks = 0;
               this.wasForward = false;
               this.lockRotation = false;
            }

            if (this.rotateForward && this.jumpFacingForward.isToggled()) {
               if (this.rotation.getInput() > 0.0) {
                  if (this.back == 0) {
                     this.rnj++;
                     this.rnf = 0.0F;
                  }

                  float forwardYaw = mc.thePlayer.rotationYaw - (this.hardcodedYaw() - 180.0F) - this.rnf;
                  if (++this.jumpDelay >= jumpDelayVal || this.rotation.getInput() != 1.0 && mc.thePlayer.onGround) {
                     this.jump = true;
                  }

                  e.setYaw(forwardYaw);
                  e.setPitch(90.0F);
                  this.lockRotation = true;
                  this.wasForward = true;
                  this.rotatingForward = true;
                  this.canSprint = true;
                  this.blockRotations = null;
                  this.theYaw = forwardYaw;
                  this.back = 2;
                  this.bvs = false;
                  this.b1t = 0;
                  if (ModuleUtils.inAirTicks == 1) {
                     this.rotationDelay = airTickVal;
                  }
               }
            } else {
               this.rotatingForward = false;
            }

            if (this.jump && mc.thePlayer.onGround) {
               mc.thePlayer.setSprinting(true);
            }

            if (!Settings.movementFix.isToggled()
               && mc.thePlayer.motionX == 0.0
               && mc.thePlayer.motionZ == 0.0
               && this.blockRotations != null) {
               e.setYaw(this.blockRotations[0]);
            }

            if (ModuleManager.tower.isVerticalTowering()) {
               if (this.blockRotations != null && (!this.getVTR || ModuleManager.tower.ebDelay <= 1 || !ModuleManager.tower.firstVTP)) {
                  this.VTRY = this.blockRotations[0];
                  this.getVTR = true;
               }

               if (this.getVTR) {
                  e.setYaw(this.VTRY);
                  this.back = 2;
               }

               if (ModuleManager.tower.yaw != 0.0F) {
                  e.setYaw(ModuleManager.tower.yaw);
                  this.back = 2;
               }

               if (ModuleManager.tower.pitch != 0.0F) {
                  e.setPitch(ModuleManager.tower.pitch);
               }
            } else {
               this.getVTR = false;
            }

            if (this.back > 0) {
               this.back--;
            }

            this.lastey = this.theYaw;
         }
      }
   }

   private void handleV() {
      float yawBackwards2 = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - this.hardcodedYaw();
      double dif = this.lastMY - this.getMotionYaw();
      double v = 2.5;
      float offset = this.yawWithOffset - yawBackwards2;
      if (!(offset > this.yawAngle) && !(offset < -this.yawAngle)) {
         if ((!(dif >= 0.0) || !(dif < v)) && (!(dif <= 0.0) || !(dif > -v)) && !mc.thePlayer.onGround) {
            this.getSmooth = this.yaw;
            float yawDifference = Utils.getAngleDifference(this.lastYawS, this.getSmooth);

            // Apply rotation speed limit
            float maxRotation = (float)this.rotationSpeed.getInput();
            if (Math.abs(yawDifference) > maxRotation) {
               yawDifference = Math.copySign(maxRotation, yawDifference);
            }

            this.getSmooth = this.lastYawS + yawDifference;
            this.lastYawS = this.getSmooth;
            this.smoothedYaw = this.getSmooth;
            this.yaw = this.smoothedYaw;
         } else {
            this.lastYawS = this.getSmooth = this.smoothedYaw = this.yaw;
         }
      } else {
         this.lastYawS = this.getSmooth = this.smoothedYaw = this.yaw;
      }
   }

   private void handleSmoothing() {
      this.handleV();
      this.lastMY = this.getMotionYaw();
   }

   private void simpleRots(ClientRotationEvent e) {
      if (!this.fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
         float moveAngle = (float)this.getMovementAngle();
         float relativeYaw = mc.thePlayer.rotationYaw + moveAngle;
         float normalizedYaw = (relativeYaw % 360.0F + 360.0F) % 360.0F;
         float quad = normalizedYaw % 90.0F;
         float rotOffset = 0.0F;
         float backv = mc.thePlayer.rotationYaw - this.hardcodedYaw();
         if (this.dynamic > 0 && this.vdl == 0L || !this.hasPlaced) {
            if (quad > 45.0F) {
               if (this.offsetvv != -45.0F) {
                  this.offsetvv = -45.0F;
                  this.vdl = Utils.time();
               }
            } else if (this.offsetvv != 45.0F) {
               this.offsetvv = 45.0F;
               this.vdl = Utils.time();
            }
         }

         this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw() - this.offsetvv;
      } else {
         this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw();
         if (this.back > 0 && this.blockRotations != null) {
         }
      }

      if (this.vdl > 0L && Utils.time() - this.vdl > 250L) {
         this.vdl = 0L;
      }

      if (this.canSprint2) {
         this.b1t++;
         if (this.b1t == 1) {
            this.began = this.blink = true;
            this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw() - this.offsetvv;
         } else if (this.b1t <= 2) {
            this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw() - 135.0F;
         } else {
            this.b1t = 0;
         }

         if ((mc.thePlayer.motionX != 0.0 || mc.thePlayer.motionY != -0.0784000015258789 || mc.thePlayer.motionZ != 0.0)
            && this.placeIdle <= 8) {
            this.btm = 0;
         } else {
            this.btm++;
            if (this.btm >= 19) {
               this.began = this.blink = false;
            }
         }
      } else if (this.began) {
         this.began = this.blink = false;
      }

      if (this.blockRotations != null) {
         this.pitch = this.blockRotations[1];
      } else {
         this.pitch = 74.0F;
      }

      if (!this.cantRotate) {
         e.setRotations(this.yaw, this.pitch);
         this.theYaw = this.yaw;
      }

      if (this.b1t == 2) {
         mc.thePlayer.setSprinting(false);
         BlinkHandler.release();
      }
   }

   Vec3 getBestFacing(Vec3 playerVec, Vec3 blockPos) {
      double dx = blockPos.xCoord + 0.5 - playerVec.xCoord;
      double dz = blockPos.zCoord + 0.5 - playerVec.zCoord;
      if (Math.abs(dx) > Math.abs(dz)) {
         return dx > 0.0 ? new Vec3(-1.0, 0.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
      } else {
         return dz > 0.0 ? new Vec3(0.0, 0.0, -1.0) : new Vec3(0.0, 0.0, 1.0);
      }
   }

   private void offsetRots(ClientRotationEvent e) {
      float moveAngle = (float)this.getMovementAngle();
      float relativeYaw = mc.thePlayer.rotationYaw + moveAngle;
      float normalizedYaw = (relativeYaw % 360.0F + 360.0F) % 360.0F;
      float quad = normalizedYaw % 90.0F;
      float side = MathHelper.wrapAngleTo180_float(this.getMotionYaw() - this.yaw);
      float yawBackwards = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - this.hardcodedYaw();
      float blockYawOffset = MathHelper.wrapAngleTo180_float(yawBackwards - this.blockYaw);
      long strokeDelay = 250L;
      float first = 77.0F;
      float sec = first;
      if (quad <= 5.0F || quad >= 85.0F) {
         this.yawAngle = 121.525F;
         this.minOffset = 11.0F;
         this.minPitch = first;
      }

      if (quad > 5.0F && quad <= 15.0F || quad >= 75.0F && quad < 85.0F) {
         this.yawAngle = 123.425F;
         this.minOffset = 9.0F;
         this.minPitch = first;
      }

      if (quad > 15.0F && quad <= 25.0F || quad >= 65.0F && quad < 75.0F) {
         this.yawAngle = 127.425F;
         this.minOffset = 8.0F;
         this.minPitch = first;
      }

      if (quad > 25.0F && quad <= 32.0F || quad >= 58.0F && quad < 65.0F) {
         this.yawAngle = 131.325F;
         this.minOffset = 7.0F;
         this.minPitch = sec;
      }

      if (quad > 32.0F && quad <= 38.0F || quad >= 52.0F && quad < 58.0F) {
         this.yawAngle = 133.525F;
         this.minOffset = 6.0F;
         this.minPitch = sec;
      }

      if (quad > 38.0F && quad <= 42.0F || quad >= 48.0F && quad < 52.0F) {
         this.yawAngle = 135.825F;
         this.minOffset = 4.0F;
         this.minPitch = sec;
      }

      if (quad > 42.0F && quad <= 45.0F || quad >= 45.0F && quad < 48.0F) {
         this.yawAngle = 138.625F;
         this.minOffset = 3.0F;
         this.minPitch = sec;
      }

      float offset = this.yawAngle;
      float nigger = 0.0F;
      if (quad > 45.0F) {
         nigger = 10.0F;
      } else {
         nigger = -10.0F;
      }

      if (this.switchvl > 0) {
         this.firstStroke = Utils.time();
         this.switchvl = 0;
         this.vlS = 0L;
         this.resetm = true;
      } else {
         this.vlS = Utils.time();
      }

      if (this.firstStroke > 0L && Utils.time() - this.firstStroke > strokeDelay) {
         this.firstStroke = 0L;
      }

      if (Utils.fallDist() <= 2.0 && Utils.getHorizontalSpeed() > 0.1) {
         this.enabledOffGround = false;
      }

      if (this.enabledOffGround) {
         if (this.blockRotations != null) {
            this.yaw = this.blockRotations[0];
            this.pitch = this.blockRotations[1];
         } else {
            this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw() - nigger;
            this.pitch = this.minPitch;
         }

         e.setRotations(this.yaw, this.pitch);
      } else {
         if (this.blockRotations != null) {
            this.blockYaw = this.blockRotations[0];
            this.pitch = this.blockRotations[1];
            this.yawOffset = blockYawOffset;
            if (this.pitch < this.minPitch) {
               this.pitch = this.minPitch;
            }
         } else {
            this.pitch = this.minPitch;
            if (this.edge == 1.0F && (quad <= 3.0F || quad >= 87.0F) && !Utils.scaffoldDiagonal(false)) {
               this.firstStroke = Utils.time();
            }

            this.yawOffset = 5.0F;
            this.dynamic = 2;
         }

         if (Utils.isMoving() && Utils.getHorizontalSpeed() != 0.0) {
            float motionYaw = this.getMotionYaw();
            float newYaw = motionYaw - offset * Math.signum(MathHelper.wrapAngleTo180_float(motionYaw - this.yaw));
            this.yaw = MathHelper.wrapAngleTo180_float(newYaw);
            if (quad > 3.0F && quad < 87.0F && this.dynamic > 0) {
               if (quad < 45.0F) {
                  if (this.firstStroke == 0L) {
                     if (side >= 0.0F) {
                        this.set2 = false;
                     } else {
                        this.set2 = true;
                     }
                  }

                  if (this.was452) {
                     this.switchvl++;
                  }

                  this.was451 = true;
                  this.was452 = false;
               } else {
                  if (this.firstStroke == 0L) {
                     if (side >= 0.0F) {
                        this.set2 = true;
                     } else {
                        this.set2 = false;
                     }
                  }

                  if (this.was451) {
                     this.switchvl++;
                  }

                  this.was452 = true;
                  this.was451 = false;
               }
            }

            double minSwitch = !Utils.scaffoldDiagonal(false) ? 9.0 : 15.0;
            if (side >= 0.0F) {
               if (this.yawOffset <= -minSwitch && this.firstStroke == 0L && this.dynamic > 0) {
                  if (quad <= 3.0F || quad >= 87.0F) {
                     if (this.set2) {
                        this.switchvl++;
                     }

                     this.set2 = false;
                  }
               } else if (this.yawOffset >= 0.0F
                  && this.firstStroke == 0L
                  && this.dynamic > 0
                  && (quad <= 3.0F || quad >= 87.0F)
                  && this.yawOffset >= minSwitch) {
                  if (!this.set2) {
                     this.switchvl++;
                  }

                  this.set2 = true;
               }

               if (this.set2) {
                  if (this.yawOffset <= 0.0F) {
                     this.yawOffset = 0.0F;
                  }

                  if (this.yawOffset >= this.minOffset) {
                     this.yawOffset = this.minOffset;
                  }

                  this.theYaw = this.yaw + offset * 2.0F - this.yawOffset;
                  e.setRotations(this.theYaw, this.pitch);
                  return;
               }
            } else if (side <= 0.0F) {
               if (this.yawOffset >= minSwitch && this.firstStroke == 0L && this.dynamic > 0) {
                  if (quad <= 3.0F || quad >= 87.0F) {
                     if (this.set2) {
                        this.switchvl++;
                     }

                     this.set2 = false;
                  }
               } else if (this.yawOffset <= 0.0F
                  && this.firstStroke == 0L
                  && this.dynamic > 0
                  && (quad <= 3.0F || quad >= 87.0F)
                  && this.yawOffset <= -minSwitch) {
                  if (!this.set2) {
                     this.switchvl++;
                  }

                  this.set2 = true;
               }

               if (this.set2) {
                  if (this.yawOffset >= 0.0F) {
                     this.yawOffset = 0.0F;
                  }

                  if (this.yawOffset <= -this.minOffset) {
                     this.yawOffset = -this.minOffset;
                  }

                  this.theYaw = this.yaw - offset * 2.0F - this.yawOffset;
                  e.setRotations(this.theYaw, this.pitch);
                  return;
               }
            }

            if (side >= 0.0F) {
               if (this.yawOffset >= 0.0F) {
                  this.yawOffset = 0.0F;
               }

               if (this.yawOffset <= -this.minOffset) {
                  this.yawOffset = -this.minOffset;
               }
            } else if (side <= 0.0F) {
               if (this.yawOffset <= 0.0F) {
                  this.yawOffset = 0.0F;
               }

               if (this.yawOffset >= this.minOffset) {
                  this.yawOffset = this.minOffset;
               }
            }

            this.theYaw = this.yaw - this.yawOffset;
            e.setRotations(this.theYaw, this.pitch);
         } else {
            e.setRotations(this.theYaw, this.pitch);
         }
      }
   }

   private void preciseRots(ClientRotationEvent e) {
      if (this.blockRotations != null) {
         this.yaw = this.blockRotations[0];
         this.pitch = this.blockRotations[1];
      } else {
         this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw();
         this.pitch = 80.0F;
      }

      e.setRotations(this.yaw, this.pitch);
      this.theYaw = this.yaw;
   }

   private void center2Rots(ClientRotationEvent e) {
      if (this.fastOnRMB.isToggled() && !this.fastOnRMB()) {
         this.simpleRots(e);
         return;
      }

      // Use the exact rotation computed to face the block's clicked face center.
      // blockRotations[0] is the precise yaw that passes GrimAC RotationPlace rayTrace.
      if (this.blockRotations != null) {
         this.yaw = this.blockRotations[0];
         this.pitch = this.blockRotations[1];
      } else {
         this.yaw = mc.thePlayer.rotationYaw - this.hardcodedYaw();
         this.pitch = 85.0F;
      }

      e.setRotations(this.yaw, this.pitch);
      this.theYaw = this.yaw;
      this.applyCenterMoveFix(this.yaw);
   }


   private void centerRots(ClientRotationEvent e) {
      // If Fast on RMB is enabled and RMB not held -> behave like Simple
      if (this.fastOnRMB.isToggled() && !this.fastOnRMB()) {
         this.simpleRots(e);
         return;
      }

      // Backwards rotation: face direction opposite to movement
      float backwardsYaw = mc.thePlayer.rotationYaw - this.hardcodedYaw();
      this.yaw = backwardsYaw;
      this.pitch = this.blockRotations != null ? this.blockRotations[1] : 85.0F;

      e.setRotations(this.yaw, this.pitch);
      this.theYaw = this.yaw;

      // Always apply silent move fix
      this.applyCenterMoveFix(this.yaw);

      // Telly jump logic: trigger jump on ground when moving
      if (mc.thePlayer.onGround && Utils.isMoving() && !ModuleManager.tower.canTower()) {
         if (this.tellyJumpTimer <= 0) {
            this.jump = true;
            jumpDelayVal = 4;
            airTickVal = 5;
            this.canSprint = true;
         } else {
            this.tellyJumpTimer--;
         }
      }

      // Track airborne state for jump timing
      if (!mc.thePlayer.onGround) {
         this.tellyWasAirborne = true;
      } else if (this.tellyWasAirborne) {
         this.tellyJumpTimer = TELLY_JUMP_DELAY;
         this.tellyWasAirborne = false;
      }
   }

   private void applyCenterMoveFix(float serverYaw) {
      float yawDiff = MathHelper.wrapAngleTo180_float(serverYaw - mc.thePlayer.rotationYaw);
      float rad = (float) Math.toRadians(yawDiff);
      float cos = (float) Math.cos(rad);
      float sin = (float) Math.sin(rad);

      float origForward = mc.thePlayer.movementInput.moveForward;
      float origStrafe = mc.thePlayer.movementInput.moveStrafe;

      mc.thePlayer.movementInput.moveForward = origForward * cos - origStrafe * sin;
      mc.thePlayer.movementInput.moveStrafe = origStrafe * cos + origForward * sin;

      Settings.fixedForward = mc.thePlayer.movementInput.moveForward;
      Settings.fixedStrafe = mc.thePlayer.movementInput.moveStrafe;
   }

   private float[] calculateCenter2ModeRotations() {
      if (this.blockInfo == null) {
         return null;
      }

      BlockPos blockPos = this.blockInfo.blockPos;
      EnumFacing facing = this.blockInfo.enumFacing;

      // Exact center of the clicked face on the existing block.
      // getCoord() returns ±0.5 for the relevant axis, 0.0 otherwise.
      double aimX = blockPos.getX() + 0.5 + this.getCoord(facing, "x");
      double aimY = blockPos.getY() + 0.5 + this.getCoord(facing, "y");
      double aimZ = blockPos.getZ() + 0.5 + this.getCoord(facing, "z");

      // Compute rotation directly to an exact world coordinate.
      // We must NOT use RotationUtils.getRotations(Vec3) here because that
      // method adds +1.0 to every coordinate (designed for block-corner input).
      float[] rot = getRotationsToWorldPoint(aimX, aimY, aimZ);

      // Verify the rotation actually hits the intended face via rayTrace
      MovingObjectPosition mop = RotationUtils.rayTraceCustom(
         mc.playerController.getBlockReachDistance(), rot[0], rot[1]
      );

      if (mop != null
          && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
          && mop.getBlockPos().equals(blockPos)
          && mop.sideHit == facing) {
         return rot;
      }

      // Fallback: standard lookAt calculation
      Vec3 lookAt = new Vec3(
         this.targetBlock.xCoord - this.lookVec.xCoord,
         this.targetBlock.yCoord - this.lookVec.yCoord,
         this.targetBlock.zCoord - this.lookVec.zCoord
      );
      return RotationUtils.getRotations(lookAt);
   }

   /**
    * Compute yaw/pitch to aim at an exact world coordinate.
    * Unlike RotationUtils.getRotations(Vec3), this does not apply any +1.0 offset.
    */
   private float[] getRotationsToWorldPoint(double worldX, double worldY, double worldZ) {
      double dx = worldX - mc.thePlayer.posX;
      double dy = worldY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
      double dz = worldZ - mc.thePlayer.posZ;
      float yaw = (float)(Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
      yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
      float pitch = (float)(-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI)));
      pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);
      return new float[]{yaw, pitch};
   }

   private float[] calculateCenterModeRotations() {
      if (this.blockInfo == null) {
         return null;
      }

      BlockPos blockPos = this.blockInfo.blockPos;
      EnumFacing facing = this.blockInfo.enumFacing;

      // Multi-sample offsets (5 points per axis)
      double[] offsets = {0.15, 0.35, 0.5, 0.65, 0.85};
      double[] xOff = offsets, yOff = offsets, zOff = offsets;

      // Constrain to edge based on facing direction
      switch (facing) {
         case NORTH:
            zOff = new double[]{0.02};
            break;
         case EAST:
            xOff = new double[]{0.98};
            break;
         case SOUTH:
            zOff = new double[]{0.98};
            break;
         case WEST:
            xOff = new double[]{0.02};
            break;
         case DOWN:
            yOff = new double[]{0.02};
            break;
         case UP:
            yOff = new double[]{0.98};
            break;
      }

      float bestYaw = this.yaw;
      float bestPitch = this.pitch;
      double bestDist = Double.MAX_VALUE;

      // Sample all combinations and find the one closest to current rotation
      for (double dx : xOff) {
         for (double dy : yOff) {
            for (double dz : zOff) {
               Vec3 targetVec = new Vec3(
                  blockPos.getX() + dx,
                  blockPos.getY() + dy,
                  blockPos.getZ() + dz
               );
               float[] rot = RotationUtils.getRotations(targetVec);

               // Verify this rotation actually hits the target block and face via rayTrace
               MovingObjectPosition mop = RotationUtils.rayTraceCustom(
                  mc.playerController.getBlockReachDistance(), rot[0], rot[1]
               );

               if (mop != null
                   && mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
                   && mop.getBlockPos().equals(blockPos)
                   && mop.sideHit == facing) {

                  // Calculate angular distance from current rotation
                  double dist = Math.abs(MathHelper.wrapAngleTo180_float(rot[0] - this.yaw))
                              + Math.abs(rot[1] - this.pitch);

                  if (dist < bestDist) {
                     bestDist = dist;
                     bestYaw = rot[0];
                     bestPitch = rot[1];
                  }
               }
            }
         }
      }

      // Return best rotation if found, otherwise fallback to default calculation
      if (bestDist < Double.MAX_VALUE) {
         return new float[]{bestYaw, bestPitch};
      } else {
         Vec3 lookAt = new Vec3(
            this.targetBlock.xCoord - this.lookVec.xCoord,
            this.targetBlock.yCoord - this.lookVec.yCoord,
            this.targetBlock.zCoord - this.lookVec.zCoord
         );
         return RotationUtils.getRotations(lookAt);
      }
   }

   private boolean canJump() {
      return !ModuleManager.tower.canTower() && !ModuleManager.tower.delay && Utils.jumpDown()
         || ModuleManager.tower.towerMove.getInput() > 0.0 && ModuleManager.tower.canTower() && Utils.isMoving() && ModuleManager.tower.disableDiag();
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (ModuleManager.scaffold.isEnabled) {
         if ((!this.fastScaffoldKeepY || this.floatKeepY || !Settings.movementFix.isToggled() || this.jump || ModuleManager.tower.delay) && !this.canJump()) {
            mc.thePlayer.movementInput.jump = false;
            if (this.jump && mc.thePlayer.onGround) {
               this.canSprint = true;
               if (Settings.movementFix.isToggled()) {
                  mc.thePlayer.movementInput.jump = true;
               } else {
                  mc.thePlayer.jump();
               }

               if (!Settings.movementFix.isToggled()) {
                  Utils.setSpeed(this.getSpeed(this.getSpeedLevel()) * ModuleUtils.applyFrictionMulti());
               }

               if (this.fastScaffold.getInput() == 6.0 || this.fastScaffold.getInput() == 3.0 && this.firstKeepYPlace) {
                  this.lowhop = true;
               }
            }

            this.jump = false;
         }
      }
   }

   @SubscribeEvent
   public void onSlotUpdate(SlotUpdateEvent e) {
      if (this.isEnabled && this.autoSwap.isToggled()) {
         this.lastSlot.set(e.slot);
         e.setCanceled(true);
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onPreUpdate(PreUpdateEvent e) {
      this.stopUpdate = this.stopUpdate2 = false;
      if (!this.isEnabled) {
         this.stopUpdate2 = true;
      }

      if (LongJump.function) {
         this.startYPos = -1.0;
      }

      if (LongJump.stopModules) {
         this.stopUpdate2 = true;
      }

      if (!this.stopUpdate2) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
         if (this.holdingBlocks() && this.setSlot()) {
            this.hasSwapped = true;
            if (!this.stopUpdate) {
               int mode = (int)this.fastScaffold.getInput();
               if (!ModuleManager.tower.placeExtraBlock) {
                  if (this.rotation.getInput() == 0.0 || this.rotationDelay == 0) {
                     this.placeBlock(0, 0);
                  }
               } else if (ModuleManager.tower.ebDelay == 0 || !ModuleManager.tower.firstVTP) {
                  this.placeBlock(0, 0);
                  this.placedVP = true;
               }

               if (ModuleManager.tower.placeExtraBlock) {
                  this.placeBlock(0, -1);
               }

               if (this.fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                  this.keepYTicks++;
                  if ((int)mc.thePlayer.posY > (int)this.startYPos) {
                     switch (mode) {
                        case 1:
                           if (!this.firstKeepYPlace && this.keepYTicks == 3) {
                              this.placeBlock(1, 0);
                              this.firstKeepYPlace = true;
                           }
                           break;
                        case 2:
                           if (!this.firstKeepYPlace && this.keepYTicks == 8 || this.keepYTicks == 11) {
                              this.placeBlock(1, 0);
                              this.firstKeepYPlace = true;
                           }
                           break;
                        case 3:
                           if (!this.firstKeepYPlace && this.keepYTicks == 8 || this.firstKeepYPlace && this.keepYTicks == 7) {
                              this.placeBlock(1, 0);
                              this.firstKeepYPlace = true;
                           }
                           break;
                        case 4:
                           if (!this.firstKeepYPlace && this.keepYTicks == 7) {
                              this.placeBlock(1, 0);
                              this.firstKeepYPlace = true;
                           }
                     }
                  }

                  if (mc.thePlayer.onGround) {
                     this.keepYTicks = 0;
                  }

                  if ((int)mc.thePlayer.posY == (int)this.startYPos) {
                     this.firstKeepYPlace = false;
                  }
               }

               this.handleMotion();
            }
         }
      }

      if (this.disabledModule) {
         if (this.hasPlaced && (this.towerEdge || this.floatStarted && Utils.isMoving())) {
            this.dontDisable = true;
         }

         if (this.dontDisable && ++this.disableTicks >= 2) {
            this.isEnabled = false;
         }

         if (!this.dontDisable) {
            this.isEnabled = false;
         }

         if (!this.isEnabled) {
            this.disabledModule = this.dontDisable = false;
            this.disableTicks = 0;
            if (ModuleManager.tower.speed) {
               Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
            }

            if (this.lastSlot.get() != -1) {
               mc.thePlayer.inventory.currentItem = this.lastSlot.get();
               this.lastSlot.set(-1);
            }

            this.blockSlot = -1;
            if (this.autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
               ((IMixinItemRenderer)mc.getItemRenderer()).setCancelUpdate(false);
               ((IMixinItemRenderer)mc.getItemRenderer()).setCancelReset(false);
            }

            if (this.offsetDelay > 0) {
               ModuleManager.sprint.requireJump = false;
            }

            this.scaffoldBlockCount.beginFade();
            this.hasSwapped = this.hasPlaced = false;
            this.targetBlock = null;
            this.blockInfo = null;
            this.blockRotations = null;
            this.fastScaffoldKeepY = this.firstKeepYPlace = this.rotateForward = this.rotatingForward = this.floatStarted = this.floatJumped = this.floatWasEnabled = this.towerEdge = this.was451 = this.was452 = this.enabledOffGround = this.finishProcedure = this.jump = this.blink = this.canSprint = this.canSprint2 = this.idle = this.didJump = this.firstRotate = this.bvs = this.began = this.cantRotate = this.startRotation = this.lockRotation = this.tellyWasAirborne = false;
            this.rotationDelay = this.keepYTicks = this.scaffoldTicks = this.floatTicks = this.rt = this.idleTicks = this.frd = this.back = this.b1t = this.canSnap = this.btm = this.snapDelay = this.srt = this.placeIdle = this.tellyJumpTimer = 0;
            this.forwardTicks = 0;
            this.wasForward = false;
            this.jumpDelay = 0;
            this.canForward = false;
            this.firstStroke = this.vlS = 0L;
            this.startYPos = -1.0;
            this.lookVec = null;
            this.lastPlacement = null;
         }
      }
   }

   @Override
   public String getInfo() {
      String s;
      if (this.sprint.getInput() > 0.0) {
         s = this.sprintModes[(int)this.sprint.getInput()];
      } else {
         s = this.rotationModes[(int)this.rotation.getInput()];
      }

      String info;
      if (this.fastOnRMB.isToggled()) {
         info = this.fastOnRMB() ? this.fastScaffoldModes[(int)this.fastScaffold.getInput()] : s;
      } else {
         info = this.fastScaffold.getInput() > 0.0 ? this.fastScaffoldModes[(int)this.fastScaffold.getInput()] : s;
      }

      return info;
   }

   // Getters for AimVisualizer
   public Vec3 getHitVec() {
      return this.hitVec;
   }

   public boolean stopFastPlace() {
      return this.isEnabled();
   }

   public boolean sprint() {
      return this.isEnabled && (this.canSprint || this.canSprint2);
   }

   public void rotateForward(boolean delay) {
      if (this.jumpFacingForward.isToggled() && this.rotation.getInput() > 0.0) {
         if (!this.rotatingForward) {
            if (delay) {
               this.rtd = 5;
            } else {
               this.rtd = 0;
            }
         }

         this.rotateForward = true;
      }
   }

   public boolean blockAbove() {
      return !(
         BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2.0, mc.thePlayer.posZ)) instanceof BlockAir
      );
   }

   private boolean usingFloat() {
      return this.sprint.getInput() == 2.0 && Utils.isMoving() && !this.usingFastScaffold();
   }

   private boolean sprintScaf() {
      return this.sprint.getInput() > 0.0 && Utils.isMoving() && mc.thePlayer.onGround && !this.usingFastScaffold() && !ModuleManager.tower.canTower();
   }

   public boolean usingFastScaffold() {
      return this.fastScaffold.getInput() > 0.0 && (!this.fastOnRMB.isToggled() || this.fastOnRMB() && Utils.tabbedIn()) && !this.prioritizeSprint();
   }

   public boolean fastOnRMB() {
      return this.fastOnRMB.isToggled() && Utils.tabbedIn() && (Mouse.isButtonDown(1) || ModuleManager.bhop.isEnabled() || this.defPS());
   }

   private boolean defPS() {
      return this.prioritizeSprintWithSpeed.isToggled() && (this.sprint.getInput() == 0.0 || this.getSpeedLevel() == 0);
   }

   private boolean prioritizeSprint() {
      return this.prioritizeSprintWithSpeed.isToggled() && this.sprint.getInput() > 0.0 && this.getSpeedLevel() > 0 && !this.fastOnRMB();
   }

   public boolean safewalk() {
      return this.isEnabled() && this.safeWalk.isToggled();
   }

   public boolean stopRotation() {
      return this.isEnabled() && this.rotation.getInput() > 0.0;
   }

   private void place(Scaffold.PlaceData block) {
      ItemStack heldItem = mc.thePlayer.getHeldItem();
      if (heldItem != null && heldItem.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)heldItem.getItem())) {
         if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            if (this.silentSwing.isToggled()) {
               mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            } else {
               mc.thePlayer.swingItem();
               if (this.holdingBlocks()) {
                  mc.getItemRenderer().resetEquippedProgress();
               }
            }

            if (ModuleManager.tower.placeExtraBlock) {
               ModuleManager.tower.firstVTP = true;
            }

            this.highlight.put(block.blockPos.offset(block.enumFacing), null);
            this.hasPlaced = true;
            this.placeIdle = 0;
         }
      }
   }

   public boolean canSafewalk() {
      if (!this.safeWalk.isToggled()) {
         return false;
      } else if (this.usingFastScaffold()) {
         return false;
      } else {
         return ModuleManager.tower.canTower() ? false : this.isEnabled;
      }
   }

   public int totalBlocks() {
      int totalBlocks = 0;

      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
         if (stack != null && stack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)stack.getItem()) && stack.stackSize > 0) {
            totalBlocks += stack.stackSize;
         }
      }

      return totalBlocks;
   }

   private void placeBlock(int yOffset, int xOffset) {
      this.locateAndPlaceBlock(yOffset, xOffset);
      int input = (int)this.multiPlace.getInput();
      if (input >= 1) {
         this.locateAndPlaceBlock(yOffset, xOffset);
         if (input >= 2) {
            this.locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 3) {
               this.locateAndPlaceBlock(yOffset, xOffset);
               if (input >= 4) {
                  this.locateAndPlaceBlock(yOffset, xOffset);
               }
            }
         }
      }
   }

   private void locateAndPlaceBlock(int yOffset, int xOffset) {
      this.locateBlocks(yOffset, xOffset);
      if (this.blockInfo != null) {
         this.lastPlacement = this.blockInfo;
         this.place(this.blockInfo);
         this.blockInfo = null;
      }
   }

   private void locateBlocks(int yOffset, int xOffset) {
      List<Scaffold.PlaceData> blocksInfo = this.findBlocks(yOffset, xOffset);
      if (blocksInfo != null) {
         double sumX = 0.0;
         double sumY = !mc.thePlayer.onGround ? 0.0 : blocksInfo.get(0).blockPos.getY();
         double sumZ = 0.0;
         int index = 0;

         for (Scaffold.PlaceData blockssInfo : blocksInfo) {
            if (index > 1 || !Utils.isDiagonal(false) && index > 0 && mc.thePlayer.onGround) {
               break;
            }

            sumX += blockssInfo.blockPos.getX();
            if (!mc.thePlayer.onGround) {
               sumY += blockssInfo.blockPos.getY();
            }

            sumZ += blockssInfo.blockPos.getZ();
            index++;
         }

         double avgX = sumX / index;
         double avgY = !mc.thePlayer.onGround ? sumY / index : blocksInfo.get(0).blockPos.getY();
         double avgZ = sumZ / index;
         this.targetBlock = new Vec3(avgX, avgY, avgZ);
         Scaffold.PlaceData blockInfo2 = blocksInfo.get(0);
         int blockX = blockInfo2.blockPos.getX();
         int blockY = blockInfo2.blockPos.getY();
         int blockZ = blockInfo2.blockPos.getZ();
         EnumFacing blockFacing = blockInfo2.enumFacing;
         this.blockInfo = blockInfo2;

         double hitX, hitY, hitZ;
         if (this.rotation.getInput() == 5.0) {
            // Center2: exact center of the clicked face (one coordinate is an integer)
            hitX = blockX + 0.5 + this.getCoord(blockFacing, "x");
            hitY = blockY + 0.5 + this.getCoord(blockFacing, "y");
            hitZ = blockZ + 0.5 + this.getCoord(blockFacing, "z");
         } else {
            hitX = blockX + 0.5 + this.getCoord(blockFacing.getOpposite(), "x") * 0.5;
            hitY = blockY + 0.5 + this.getCoord(blockFacing.getOpposite(), "y") * 0.5;
            hitZ = blockZ + 0.5 + this.getCoord(blockFacing.getOpposite(), "z") * 0.5;
         }
         this.lookVec = new Vec3(
            0.5 + this.getCoord(blockFacing.getOpposite(), "x") * 0.5,
            0.5 + this.getCoord(blockFacing.getOpposite(), "y") * 0.5,
            0.5 + this.getCoord(blockFacing.getOpposite(), "z") * 0.5
         );
         this.hitVec = new Vec3(hitX, hitY, hitZ);
         this.blockInfo.hitVec = this.hitVec;
      }
   }

   private double getCoord(EnumFacing facing, String axis) {
      switch (axis) {
         case "x":
            return facing == EnumFacing.WEST ? -0.5 : (facing == EnumFacing.EAST ? 0.5 : 0.0);
         case "y":
            return facing == EnumFacing.DOWN ? -0.5 : (facing == EnumFacing.UP ? 0.5 : 0.0);
         case "z":
            return facing == EnumFacing.NORTH ? -0.5 : (facing == EnumFacing.SOUTH ? 0.5 : 0.0);
         default:
            return 0.0;
      }
   }

   private List<Scaffold.PlaceData> findBlocks(int yOffset, int xOffset) {
      int x = (int)Math.floor(mc.thePlayer.posX + xOffset);
      int y = (int)Math.floor((this.startYPos != -1.0 ? this.startYPos : mc.thePlayer.posY) + yOffset);
      int z = (int)Math.floor(mc.thePlayer.posZ);
      BlockPos base = new BlockPos(x, y - 1, z);
      if (!BlockUtils.replaceable(base)) {
         return null;
      }

      EnumFacing[] allFacings = this.getFacingsSorted();
      List<EnumFacing> validFacings = new ArrayList<>(5);

      for (EnumFacing facing : allFacings) {
         if (facing != EnumFacing.UP && this.placeConditions(facing, yOffset, xOffset)) {
            validFacings.add(facing);
         }
      }

      List<Scaffold.PlaceData> possibleBlocks = new ArrayList<>();
      Queue<BlockPos> queue = new LinkedList<>();
      Set<BlockPos> visited = new HashSet<>();
      Map<BlockPos, Integer> distances = new HashMap<>();
      queue.offer(base);
      visited.add(base);
      distances.put(base, 0);

      while (!queue.isEmpty()) {
         BlockPos current = queue.poll();
         int currentDistance = distances.get(current);
         if (!(currentDistance >= distance)) {
            for (EnumFacing facing : validFacings) {
               BlockPos neighbor = current.offset(facing);
               if (!BlockUtils.replaceable(neighbor) && !BlockUtils.isInteractable(BlockUtils.getBlock(neighbor))) {
                  possibleBlocks.add(new Scaffold.PlaceData(neighbor, facing.getOpposite()));
               } else if (BlockUtils.replaceable(neighbor) && !visited.contains(neighbor)) {
                  visited.add(neighbor);
                  distances.put(neighbor, currentDistance + 1);
                  queue.offer(neighbor);
               }
            }
         }
      }

      return possibleBlocks.isEmpty() ? null : possibleBlocks;
   }

   private EnumFacing[] getFacingsSorted() {
      EnumFacing lastFacing = EnumFacing.getHorizontal(
         MathHelper.floor_double(((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() * 4.0F / 360.0F + 0.5) & 3
      );
      EnumFacing perpClockwise = lastFacing.rotateY();
      EnumFacing perpCounterClockwise = lastFacing.rotateYCCW();
      EnumFacing opposite = lastFacing.getOpposite();
      float yaw = ((IAccessorEntityPlayerSP)mc.thePlayer).getLastReportedYaw() % 360.0F;
      if (yaw > 180.0F) {
         yaw -= 360.0F;
      } else if (yaw < -180.0F) {
         yaw += 360.0F;
      }

      float diffClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - this.getFacingAngle(perpClockwise)));
      float diffCounterClockwise = Math.abs(MathHelper.wrapAngleTo180_float(yaw - this.getFacingAngle(perpCounterClockwise)));
      EnumFacing firstPerp;
      EnumFacing secondPerp;
      if (diffClockwise <= diffCounterClockwise) {
         firstPerp = perpClockwise;
         secondPerp = perpCounterClockwise;
      } else {
         firstPerp = perpCounterClockwise;
         secondPerp = perpClockwise;
      }

      return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN, lastFacing, firstPerp, secondPerp, opposite};
   }

   private float getFacingAngle(EnumFacing facing) {
      switch (facing) {
         case WEST:
            return 90.0F;
         case NORTH:
            return 180.0F;
         case EAST:
            return -90.0F;
         default:
            return 0.0F;
      }
   }

   private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
      if (xCondition == -1) {
         return !ModuleManager.tower.placeExtraBlock ? enumFacing == EnumFacing.EAST : enumFacing == EnumFacing.DOWN;
      } else if (ModuleManager.tower.placeExtraBlock) {
         return enumFacing == EnumFacing.WEST;
      } else {
         return yCondition == 1 ? enumFacing == EnumFacing.DOWN : true;
      }
   }

   float applyGcd(float value) {
      float gcd = 0.064F;
      return (float)(value - value % (gcd * 0.15));
   }

   public float getMotionYaw() {
      return (float)Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F;
   }

   public int getSpeedLevel() {
      Iterator var1 = mc.thePlayer.getActivePotionEffects().iterator();
      if (var1.hasNext()) {
         PotionEffect potionEffect = (PotionEffect)var1.next();
         return potionEffect.getEffectName().equals("potion.moveSpeed") ? potionEffect.getAmplifier() + 1 : 0;
      } else {
         return 0;
      }
   }

   public int getJumpLevel() {
      Iterator var1 = mc.thePlayer.getActivePotionEffects().iterator();
      if (var1.hasNext()) {
         PotionEffect potionEffect = (PotionEffect)var1.next();
         return potionEffect.getEffectName().equals("potion.jump") ? potionEffect.getAmplifier() + 1 : 0;
      } else {
         return 0;
      }
   }

   public double getSpeed(int speedLevel) {
      return speedLevel >= 0 ? this.speedLevels[speedLevel] : this.speedLevels[0];
   }

   double getFloatSpeed(int speedLevel) {
      double min = 0.0;
      double value = 0.0;
      double input = this.motion.getInput() / 100.0;
      if (mc.thePlayer.moveStrafing != 0.0F && mc.thePlayer.moveForward != 0.0F) {
         min = 0.003;
      }

      value = this.floatSpeedLevels[speedLevel] - min;
      if (speedLevel == 2) {
         value = (Utils.scaffoldDiagonal(false) ? 0.255 : 0.265) - min;
      }

      return value * input;
   }

   private void handleMotion() {
      if (!this.usingFastScaffold()
         && !this.usingFloat()
         && !ModuleManager.tower.canTower()
         && this.motion.getInput() != 100.0
         && mc.thePlayer.onGround) {
         double input = this.motion.getInput() / 100.0;
         mc.thePlayer.motionX *= input;
         mc.thePlayer.motionZ *= input;
      }
   }

   public float hardcodedYaw() {
      float simpleYaw = 0.0F;
      boolean w = Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode());
      boolean s = Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode());
      boolean a = Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode());
      boolean d = Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode());
      boolean dupe = a & d;
      if (!this.lockRotation) {
         if (w) {
            simpleYaw -= 180.0F;
            if (!dupe) {
               if (a) {
                  simpleYaw += 45.0F;
               }

               if (d) {
                  simpleYaw -= 45.0F;
               }
            }
         } else if (!s) {
            simpleYaw -= 180.0F;
            if (!dupe) {
               if (a) {
                  simpleYaw += 90.0F;
               }

               if (d) {
                  simpleYaw -= 90.0F;
               }
            }
         } else if (!w && !dupe) {
            if (a) {
               simpleYaw -= 45.0F;
            }

            if (d) {
               simpleYaw += 45.0F;
            }
         }

         this.finalhYaw = simpleYaw;
      }

      return this.finalhYaw;
   }

   public boolean holdingBlocks() {
      ItemStack heldItem = mc.thePlayer.getHeldItem();
      if (this.autoSwap.isToggled()
         && ModuleManager.autoSwap.spoofItem.isToggled()
         && this.lastSlot.get() != mc.thePlayer.inventory.currentItem
         && this.totalBlocks() > 0) {
         ((IMixinItemRenderer)mc.getItemRenderer()).setCancelUpdate(true);
         ((IMixinItemRenderer)mc.getItemRenderer()).setCancelReset(true);
      }

      return this.autoSwap.isToggled() && this.getSlot() != -1
         || heldItem != null && heldItem.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)heldItem.getItem());
   }

   private double getMovementAngle() {
      double angle = Settings.movementFix.isToggled()
         ? Math.toDegrees(Math.atan2(-Settings.fixedStrafe, Settings.fixedForward))
         : Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
      return angle == 0.0 ? 0.0 : angle;
   }

   private int getSlot() {
      int slot = -1;
      int highestStack = -1;
      ItemStack heldItem = mc.thePlayer.getHeldItem();

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
         if (itemStack != null
            && itemStack.getItem() instanceof ItemBlock
            && Utils.canBePlaced((ItemBlock)itemStack.getItem())
            && itemStack.stackSize > 0
            && (Utils.getBedwarsStatus() != 2 || !(((ItemBlock)itemStack.getItem()).getBlock() instanceof BlockTNT))
            && (
               itemStack == null
                  || heldItem == null
                  || !(heldItem.getItem() instanceof ItemBlock)
                  || !Utils.canBePlaced((ItemBlock)heldItem.getItem())
                  || !ModuleManager.autoSwap.sameType.isToggled()
                  || itemStack.getItem().getClass().equals(heldItem.getItem().getClass())
            )
            && itemStack.stackSize > highestStack) {
            highestStack = itemStack.stackSize;
            slot = i;
         }
      }

      return slot;
   }

   public static boolean bypassRots() {
      return ModuleManager.scaffold.rotation.getInput() == 2.0 || ModuleManager.scaffold.rotation.getInput() == 0.0;
   }

   public boolean setSlot() {
      ItemStack heldItem = mc.thePlayer.getHeldItem();
      int slot = this.getSlot();
      if (slot == -1) {
         return false;
      }

      if (this.blockSlot == -1) {
         this.blockSlot = slot;
      }

      if (this.lastSlot.get() == -1) {
         this.lastSlot.set(mc.thePlayer.inventory.currentItem);
      }

      if (this.autoSwap.isToggled() && this.blockSlot != -1) {
         if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
            mc.thePlayer.inventory.currentItem = slot;
            this.spoofSlot = slot;
         } else if (heldItem == null
            || !(heldItem.getItem() instanceof ItemBlock)
            || !Utils.canBePlaced((ItemBlock)heldItem.getItem())
            || mc.thePlayer.getHeldItem().stackSize <= ModuleManager.autoSwap.swapAt.getInput()) {
            mc.thePlayer.inventory.currentItem = slot;
            this.spoofSlot = slot;
         }
      }

      if (heldItem != null && heldItem.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)heldItem.getItem())) {
         return true;
      }

      this.blockSlot = -1;
      return false;
   }

   class BlockCountEditScreen extends GuiScreen {
      private GuiButtonExt resetPosition;
      private boolean dragging = false;
      private int aX = 0, aY = 0;
      private int laX = 0, laY = 0, lmX = 0, lmY = 0;
      private int bgX1, bgY1, bgX2, bgY2;

      @Override
      public void initGui() {
         super.initGui();
         this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
         this.aX = Scaffold.this.blockCountPosX;
         this.aY = Scaffold.this.blockCountPosY;
      }

      @Override
      public void drawScreen(int mX, int mY, float pt) {
         ScaledResolution res = new ScaledResolution(mc);
         drawRect(0, 0, this.width, this.height, -1308622848);

         int blocks = Scaffold.this.totalBlocks();
         String text = keystrokesmod.utility.ScaffoldBlockCount.buildText(blocks);
         int x = res.getScaledWidth() / 2 + 8 + this.aX;
         int y = res.getScaledHeight() / 2 + 4 + this.aY;
         int textWidth = mc.fontRendererObj.getStringWidth(text);
         int PAD = 3;
         this.bgX1 = x - PAD;
         this.bgY1 = y - PAD;
         this.bgX2 = x + textWidth + PAD;
         this.bgY2 = y + mc.fontRendererObj.FONT_HEIGHT + PAD;

         Scaffold.this.blockCountPosX = this.aX;
         Scaffold.this.blockCountPosY = this.aY;

         // Blur background
         float blurR = (float) Scaffold.this.blockCountBlur.getInput();
         int bgAlpha = 199;
         if (blurR > 0.0f) {
            BlurUtils.blurRect(this.bgX1, this.bgY1, this.bgX2 - this.bgX1, this.bgY2 - this.bgY1, 2, blurR);
         } else {
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            Gui.drawRect(this.bgX1, this.bgY1, this.bgX2, this.bgY2, new Color(0, 0, 0, bgAlpha).getRGB());
            GL11.glDisable(3042);
            GL11.glPopMatrix();
         }

         // Text
         GL11.glPushMatrix();
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         mc.fontRendererObj.drawStringWithShadow(text, x, y, -1);
         GL11.glDisable(3042);
         GL11.glPopMatrix();

         // Hint
         String hint = "Drag to reposition the block count display.";
         int hx = res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(hint) / 2;
         int hy = res.getScaledHeight() / 2 - 20;
         mc.fontRendererObj.drawString(hint, hx, hy, 0xFFAAAAAA);

         try {
            this.handleInput();
         } catch (IOException e) {
         }

         super.drawScreen(mX, mY, pt);
      }

      @Override
      protected void mouseClickMove(int mX, int mY, int b, long t) {
         super.mouseClickMove(mX, mY, b, t);
         if (b == 0) {
            if (this.dragging) {
               this.aX = this.laX + (mX - this.lmX);
               this.aY = this.laY + (mY - this.lmY);
            } else if (mX >= this.bgX1 && mX <= this.bgX2 && mY >= this.bgY1 && mY <= this.bgY2) {
               this.dragging = true;
               this.lmX = mX;
               this.lmY = mY;
               this.laX = this.aX;
               this.laY = this.aY;
            }
         }
      }

      @Override
      protected void mouseReleased(int mX, int mY, int s) {
         super.mouseReleased(mX, mY, s);
         if (s == 0) {
            this.dragging = false;
         }
      }

      @Override
      public void actionPerformed(GuiButton b) {
         if (b == this.resetPosition) {
            this.aX = Scaffold.this.blockCountPosX = 0;
            this.aY = Scaffold.this.blockCountPosY = 0;
         }
      }

      @Override
      public boolean doesGuiPauseGame() {
         return false;
      }
   }

   static class PlaceData {
      EnumFacing enumFacing;
      BlockPos blockPos;
      Vec3 hitVec;

      PlaceData(BlockPos blockPos, EnumFacing enumFacing) {
         this.enumFacing = enumFacing;
         this.blockPos = blockPos;
      }

      public PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
         this.enumFacing = enumFacing;
         this.blockPos = blockPos;
      }
   }
}
