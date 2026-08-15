package keystrokesmod.module.impl.player;

import java.awt.Color;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoFall extends Module {
   public SliderSetting mode;
   private SliderSetting minFallDistance;
   private ButtonSetting disableAdventure;
   private ButtonSetting ignoreVoid;
   private ButtonSetting voidC;
   private ButtonSetting hideSound;
   public ButtonSetting renderTimer;
   private ButtonSetting disableTp;
   private String[] modes = new String[]{"Spoof", "NoGround", "Packet A", "Packet B", "Prediction", "Blink"};
   private int color = new Color(0, 187, 255, 255).getRGB();
   private double initialY;
   private double dynamic;
   public boolean isFalling;
   private double timerVal = 1.0;
   private int n;
   public boolean bnFalling;
   public boolean blink;
   private int y;
   private boolean tp;
   private double lastX;
   private double lastY;
   private double lastZ;

   public NoFall() {
      super("NoFall", Module.category.player);
      this.registerSetting(this.mode = new SliderSetting("Mode", 2, this.modes));
      this.registerSetting(this.disableAdventure = new ButtonSetting("Disable adventure", false));
      this.registerSetting(this.minFallDistance = new SliderSetting("Minimum fall distance", 3.0, 0.0, 10.0, 0.1));
      this.registerSetting(this.ignoreVoid = new ButtonSetting("Ignore void", false));
      this.registerSetting(this.voidC = new ButtonSetting("Experimental void check", true));
      this.registerSetting(this.renderTimer = new ButtonSetting("Render Blink Timer", true));
      this.registerSetting(this.disableTp = new ButtonSetting("Disable on teleport", true));
   }

   @Override
   public void guiUpdate() {
      this.renderTimer.setVisible(this.mode.getInput() == 5.0, this);
   }

   @Override
   public void onDisable() {
      Utils.resetTimer();
      this.blink = false;
      this.tp = false;
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (e.getPacket() instanceof S08PacketPlayerPosLook && this.n > 0) {
         this.n = 34;
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.mode.getInput() == 5.0) {
         if (!this.blink
            && Utils.fallDist() >= this.minFallDistance.getInput()
            && Utils.isEdgeOfBlock()
            && mc.thePlayer.onGround
            && !Utils.jumpDown()
            && !ModuleManager.scaffold.isEnabled
            && !ModuleManager.bhop.isEnabled()
            && !LongJump.function) {
            this.blink = true;
            this.bnFalling = false;
            this.y = (int)mc.thePlayer.posY;
         } else if (this.blink && !this.bnFalling && mc.thePlayer.posY > this.y) {
            this.blink = false;
         }

         if (mc.thePlayer.posY < this.y && !mc.thePlayer.onGround && this.blink) {
            this.bnFalling = true;
         } else if (this.bnFalling) {
            this.blink = false;
         }

         if (mc.thePlayer.posY <= this.y - 31 && this.blink) {
            this.blink = false;
         }
      }

      if (mc.thePlayer.posY >= this.lastY + 3.5
         && !(mc.thePlayer.posY >= this.lastY + 10.0)
         && ModuleUtils.hasTeleported
         && !ModuleUtils.worldChange) {
         if (this.disableTp.isToggled()) {
            Utils.modulePrint("§cMost likely staff checked, disabling NoFall until on ground");
            this.tp = true;
            Utils.ping();
         }
      } else if (mc.thePlayer.onGround && this.tp) {
         this.tp = false;
         Utils.modulePrint("§aNoFall re-enabled");
      }

      this.lastX = mc.thePlayer.posX;
      this.lastY = mc.thePlayer.posY;
      this.lastZ = mc.thePlayer.posZ;
      if (this.reset()) {
         Utils.resetTimer();
         this.initialY = mc.thePlayer.posY;
         this.isFalling = false;
         this.n = 0;
         this.timerVal = 1.0;
      } else {
         if (mc.thePlayer.fallDistance >= this.minFallDistance.getInput()) {
            this.isFalling = true;
         }

         double predictedY = mc.thePlayer.posY + mc.thePlayer.motionY;
         double distanceFallen = this.initialY - predictedY;
         if (this.isFalling && this.mode.getInput() == 2.0) {
            if (mc.thePlayer.motionY >= -1.0) {
               this.dynamic = 3.0;
            }

            if (mc.thePlayer.motionY < -1.0) {
               this.dynamic = 4.0;
            }

            if (mc.thePlayer.motionY < -2.0) {
               this.dynamic = 5.0;
            }

            if (distanceFallen >= this.dynamic) {
               if (mc.thePlayer.motionY < -0.01) {
                  this.timerVal = 0.8;
               }

               if (mc.thePlayer.motionY < -1.0) {
                  this.timerVal = 0.7;
               }

               if (mc.thePlayer.motionY < -1.6) {
                  this.timerVal = 0.6;
               }

               Utils.getTimer().timerSpeed = (float)this.timerVal;
               mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
               this.initialY = mc.thePlayer.posY;
            }
         }

         if (this.isFalling && this.mode.getInput() == 3.0) {
            if (mc.thePlayer.motionY < -2.0) {
               this.dynamic = 4.0;
            } else {
               this.dynamic = 3.0;
            }

            Utils.resetTimer();
            if (mc.thePlayer.ticksExisted % 2 == 0) {
               if (mc.thePlayer.motionY < -0.01) {
                  this.timerVal = 0.62;
               }

               if (mc.thePlayer.motionY < -1.0) {
                  this.timerVal = 0.5;
               }

               if (mc.thePlayer.motionY < -1.6) {
                  this.timerVal = 0.46;
               }

               if (mc.thePlayer.motionY < -2.1) {
                  this.timerVal = 0.41;
               }

               Utils.getTimer().timerSpeed = (float)this.timerVal;
            }

            if (distanceFallen >= this.dynamic) {
               mc.getNetHandler().addToSendQueue(new C03PacketPlayer(true));
               this.initialY = mc.thePlayer.posY;
            }
         }

         if (this.isFalling && this.mode.getInput() == 4.0 && distanceFallen >= 3.0 && this.n <= 4) {
            mc.thePlayer.motionY = 0.0;
            this.n++;
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onPreMotion(PreMotionEvent e) {
      switch ((int)this.mode.getInput()) {
         case 0:
            e.setOnGround(true);
            break;
         case 1:
            e.setOnGround(false);
            break;
         case 5:
            if (this.blink) {
               e.setOnGround(true);
            }
      }
   }

   @Override
   public String getInfo() {
      return this.modes[(int)this.mode.getInput()];
   }

   private boolean reset() {
      if (this.disableAdventure.isToggled() && mc.playerController.getCurrentGameType().isAdventure()) {
         return true;
      } else if (this.ignoreVoid.isToggled() && Utils.overVoid()) {
         return true;
      } else if (Utils.isBedwarsPractice()) {
         return true;
      } else if (Utils.spectatorCheck()) {
         return true;
      } else if (Utils.isReplay()) {
         return true;
      } else if (mc.thePlayer.onGround) {
         return true;
      } else if (BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1.0, mc.thePlayer.posZ))
         != Blocks.air) {
         return true;
      } else if (mc.thePlayer.motionY > -0.0784) {
         return true;
      } else if (mc.thePlayer.capabilities.isCreativeMode) {
         return true;
      } else if (Utils.overVoid() && mc.thePlayer.posY <= 41.0) {
         return true;
      } else if (mc.thePlayer.capabilities.isFlying) {
         return true;
      } else {
         return this.voidC.isToggled() && Utils.overVoid() && !this.dist() ? true : this.tp;
      }
   }

   public boolean dist() {
      double minMotion = 0.15;
      int dist1 = 1;
      int dist2 = 3;
      int dist3 = 5;
      int dist4 = 7;
      if (mc.thePlayer.isCollidedHorizontally) {
         return false;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ) > dist1) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ) > dist1
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ) > dist1
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 1) > dist1
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 1) > dist1
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ - 1) > dist1
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ + 1) > dist1
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 1) > dist1
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ - 1) > dist1
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ) > dist2
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 2) > dist2
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 1) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ + 1) > dist2
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 2) > dist2
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ + 2) > dist2
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ - 2) > dist2
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 1) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 2) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 1) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 2) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ - 1) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ - 2) > dist3
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ - 3) > dist3
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 1) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 2) > dist3
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ + 3) > dist3
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionX >= minMotion
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionX <= -minMotion
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 3) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 2) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ + 1) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 3, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 2, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 1, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 3) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 2) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX + 4, (int)mc.thePlayer.posZ - 1) > dist4
         && mc.thePlayer.motionX >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ + 4) > dist4
         && mc.thePlayer.motionZ >= minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 3) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 2) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ + 1) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 3) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 2) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 4, (int)mc.thePlayer.posZ - 1) > dist4
         && mc.thePlayer.motionX <= -minMotion) {
         return true;
      } else if (Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 3, (int)mc.thePlayer.posZ - 4) > dist4
         && mc.thePlayer.motionZ <= -minMotion) {
         return true;
      } else {
         return Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 2, (int)mc.thePlayer.posZ - 4) > dist4
               && mc.thePlayer.motionZ <= -minMotion
            ? true
            : Utils.distanceToGround(mc.thePlayer, (int)mc.thePlayer.posX - 1, (int)mc.thePlayer.posZ - 4) > dist4
               && mc.thePlayer.motionZ <= -minMotion;
      }
   }
}
