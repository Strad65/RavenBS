package keystrokesmod.module.impl.player;

import java.awt.Color;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class Disabler extends Module {
   private SliderSetting disablerTicks;
   private SliderSetting activationDelay;
   private ButtonSetting resetDisabler;
   int tickCounter = 0;
   boolean waitingForGround = false;
   boolean applyingMotion = false;
   int stateTickCounter = 0;
   boolean warningDisplayed = false;
   int sprintToggleTick = 0;
   boolean shouldRun = false;
   long lobbyTime = 0L;
   long finished = 0L;
   long activationDelayMillis;
   final long checkDisabledTime = 4000L;
   private int color = new Color(0, 187, 255, 255).getRGB();
   private float barWidth = 60.0F;
   private float barHeight = 4.0F;
   private float filledWidth;
   private float barX;
   private float barY;
   private boolean shouldRender;
   private double firstY;
   private boolean reset;
   private float savedYaw;
   private float savedPitch;
   private boolean worldJoin;
   private int wDelay;
   public boolean disablerLoaded;
   public boolean running;

   public Disabler() {
      super("Disabler", Module.category.player);
      this.registerSetting(this.disablerTicks = new SliderSetting("Ticks", "", 100.0, 85.0, 150.0, 5.0));
      this.registerSetting(this.activationDelay = new SliderSetting("Activation delay", " seconds", 0.0, 0.0, 4.0, 0.5));
      this.registerSetting(this.resetDisabler = new ButtonSetting("§cReset", false));
   }

   @Override
   public void onEnable() {
      if (!this.disablerLoaded) {
         this.resetState();
      }
   }

   @Override
   public void onDisable() {
      this.shouldRun = false;
      this.running = false;
   }

   private void resetState() {
      this.savedYaw = mc.thePlayer.rotationYaw;
      this.savedPitch = mc.thePlayer.rotationPitch;
      this.shouldRun = true;
      this.tickCounter = 0;
      this.applyingMotion = false;
      this.waitingForGround = true;
      this.stateTickCounter = 0;
      this.warningDisplayed = false;
      this.running = false;
      this.sprintToggleTick = 0;
      this.lobbyTime = Utils.time();
      this.finished = 0L;
      this.shouldRender = false;
      this.reset = false;
      this.worldJoin = false;
      this.activationDelayMillis = (long)(this.activationDelay.getInput() * 1000.0);
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.resetState();
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onPreMotion(PreMotionEvent e) {
      if (this.resetDisabler.isToggled()) {
         Utils.modulePrint("&7[&dR&7] &cdisabler resetting...");
         this.resetState();
         this.disablerLoaded = false;
         this.resetDisabler.disable();
      }

      if (Utils.getLobbyStatus() != 1 && Utils.hypixelStatus() == 1 && !Utils.isReplay()) {
         long now = System.currentTimeMillis();
         if (this.finished != 0L && mc.thePlayer.onGround && now - this.finished > 4000L) {
            Utils.modulePrint("&7[&dR&7] &adisabler enabled");
            this.finished = 0L;
            this.filledWidth = 0.0F;
            this.disablerLoaded = true;
         }

         if (this.shouldRun) {
            if (now - this.lobbyTime >= this.activationDelayMillis) {
               this.running = true;
               e.setRotations(0.0F, this.savedPitch);
               if (this.waitingForGround) {
                  if (mc.thePlayer.onGround) {
                     mc.thePlayer.motionY = 0.42F;
                     this.waitingForGround = false;
                     this.worldJoin = false;
                  }
               } else {
                  if (ModuleUtils.inAirTicks >= 10 || this.worldJoin && ++this.wDelay >= 3) {
                     if (!this.applyingMotion) {
                        this.applyingMotion = true;
                        this.firstY = mc.thePlayer.posY;
                     }

                     if (this.tickCounter < this.disablerTicks.getInput()) {
                        this.shouldRender = true;
                        mc.thePlayer.motionX = 0.0;
                        mc.thePlayer.motionY = 0.0;
                        mc.thePlayer.motionZ = 0.0;
                        if (mc.thePlayer.posY != this.firstY) {
                           if (!this.reset) {
                              this.resetState();
                              this.activationDelayMillis = 2000L;
                              this.reset = true;
                              Utils.modulePrint("&7[&dR&7] &adisabler reset, wait 2s");
                           } else {
                              this.shouldRun = false;
                              this.applyingMotion = false;
                              this.running = false;
                              Utils.modulePrint("&7[&dR&7] &cfailed to reset disabler, re-enable to try again");
                           }
                        }

                        if (mc.thePlayer.ticksExisted % 2 == 0) {
                           e.setPosZ(e.getPosZ() + 0.075);
                           e.setPosX(e.getPosX() + 0.075);
                        }

                        this.tickCounter++;
                     } else if (!this.warningDisplayed) {
                        double totalTimeSeconds = (now - this.lobbyTime) / 1000.0;
                        this.warningDisplayed = true;
                        this.finished = now;
                        this.shouldRender = false;
                        this.shouldRun = false;
                        this.applyingMotion = false;
                        this.running = false;
                     }
                  }

                  this.filledWidth = (float)(this.barWidth * this.tickCounter / this.disablerTicks.getInput());
                  ScaledResolution scaledResolution = new ScaledResolution(mc);
                  int[] disp = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
                  this.barX = disp[0] / 2 - this.barWidth / 2.0F;
                  this.barY = disp[1] / 2 + 12;
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onMoveInput(PrePlayerInputEvent e) {
      if (this.running && !Utils.isReplay() && !Utils.spectatorCheck()) {
         e.setForward(0.0F);
         e.setStrafe(0.0F);
         mc.thePlayer.movementInput.jump = false;
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (Utils.nullCheck()) {
         if (ev.phase != Phase.END || mc.currentScreen == null && this.shouldRun && this.shouldRender) {
            this.color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
            RenderUtils.drawRoundedRectangle(this.barX, this.barY, this.barX + this.barWidth, this.barY + this.barHeight, 3.0F, -11184811);
            RenderUtils.drawRoundedRectangle(this.barX, this.barY, this.barX + this.filledWidth, this.barY + this.barHeight, 3.0F, this.color);
         }
      }
   }
}
