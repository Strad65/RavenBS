package keystrokesmod.module.impl.movement;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;

public class LongJump extends Module {
   private SliderSetting mode;
   private SliderSetting boostSetting;
   private SliderSetting speedSetting;
   private SliderSetting verticalMotion;
   private SliderSetting motionDecay;
   private ButtonSetting manual;
   private ButtonSetting onlyWithVelocity;
   private KeySetting disableKey;
   private KeySetting flatKey;
   private ButtonSetting allowStrafe;
   private ButtonSetting invertYaw;
   private ButtonSetting stopMovement;
   private ButtonSetting jump;
   private ButtonSetting hideExplosion;
   public ButtonSetting spoofItem;
   private ButtonSetting beginFlat;
   private ButtonSetting silentSwing;
   private ButtonSetting renderFloatProgress;
   private KeySetting verticalKey;
   private SliderSetting pitchVal;
   public String[] modes = new String[]{"Float", "Boost", "Delay"};
   private boolean manualWasOn;
   private float yaw;
   private float pitch;
   private boolean notMoving;
   private boolean enabled;
   private boolean swapped;
   public static boolean function;
   private int boostTicks = -1;
   private int delayTicks = -1;
   public int lastSlot = -1;
   public int spoofSlot = -1;
   private int stopTime;
   private int rotateTick;
   private long fireballTime;
   private long MAX_EXPLOSION_DIST_SQ = 9L;
   public static boolean stopVelocity;
   public static boolean stopModules;
   public static boolean slotReset;
   public static int slotResetTicks;
   private int firstSlot = -1;
   private int color = new Color(0, 187, 255, 255).getRGB();
   private float barWidth = 60.0F;
   private float barHeight = 4.0F;
   private float filledWidth;
   private float barX;
   private float barY;
   private double motionDecayVal;
   private List<Map<String, Object>> packets = new ArrayList<>();
   private boolean delaying;

   public LongJump() {
      super("Long Jump", Module.category.movement);
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
      this.registerSetting(this.manual = new ButtonSetting("Manual", false));
      this.registerSetting(this.onlyWithVelocity = new ButtonSetting("Only while velocity enabled", false));
      this.registerSetting(this.disableKey = new KeySetting("Disable key", 57));
      this.registerSetting(this.boostSetting = new SliderSetting("Horizontal boost", 1.7, 0.0, 3.0, 0.05));
      this.registerSetting(this.verticalMotion = new SliderSetting("Vertical motion", 0.0, 0.2, 1.0, 0.01));
      this.registerSetting(this.motionDecay = new SliderSetting("Motion decay", "%", 43.0, 1.0, 100.0, 1.0));
      this.registerSetting(this.allowStrafe = new ButtonSetting("Allow strafe", false));
      this.registerSetting(this.invertYaw = new ButtonSetting("Invert yaw", true));
      this.registerSetting(this.stopMovement = new ButtonSetting("Stop movement", false));
      this.registerSetting(this.jump = new ButtonSetting("Jump", false));
      this.registerSetting(this.hideExplosion = new ButtonSetting("Hide explosion", false));
      this.registerSetting(this.spoofItem = new ButtonSetting("Spoof item", false));
      this.registerSetting(this.silentSwing = new ButtonSetting("Silent swing", false));
      this.registerSetting(this.renderFloatProgress = new ButtonSetting("Render progress", false));
      this.registerSetting(this.beginFlat = new ButtonSetting("Begin flat", false));
      this.registerSetting(this.verticalKey = new KeySetting("Vertical key", 57));
      this.registerSetting(this.flatKey = new KeySetting("Flat key", 57));
   }

   @Override
   public String getInfo() {
      return this.modes[(int)this.mode.getInput()];
   }

   @Override
   public void guiUpdate() {
      this.allowStrafe.setVisible(this.mode.getInput() != 2.0, this);
      this.boostSetting.setVisible(this.mode.getInput() != 2.0, this);
      this.invertYaw.setVisible(this.mode.getInput() != 2.0, this);
      this.jump.setVisible(this.mode.getInput() != 2.0, this);
      this.stopMovement.setVisible(this.mode.getInput() != 2.0, this);
      this.onlyWithVelocity.setVisible(this.manual.isToggled(), this);
      this.disableKey.setVisible(this.manual.isToggled(), this);
      this.spoofItem.setVisible(!this.manual.isToggled(), this);
      this.verticalMotion.setVisible(this.mode.getInput() == 0.0, this);
      this.motionDecay.setVisible(this.mode.getInput() == 0.0, this);
      this.beginFlat.setVisible(this.mode.getInput() == 0.0, this);
      this.verticalKey.setVisible(this.mode.getInput() == 0.0 && this.beginFlat.isToggled(), this);
      this.flatKey.setVisible(this.mode.getInput() == 0.0 && !this.beginFlat.isToggled(), this);
   }

   @Override
   public void onEnable() {
      if (ModuleUtils.profileTicks > 1) {
         if (!this.manual.isToggled()) {
            if (Utils.getTotalHealth(mc.thePlayer) <= 3.0F) {
               Utils.sendMessage("&cPrevented throwing fireball due to low health");
               this.disable();
               return;
            }

            this.enabled();
         }

         this.filledWidth = 0.0F;
         ScaledResolution scaledResolution = new ScaledResolution(mc);
         int[] disp = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
         this.barX = disp[0] / 2 - this.barWidth / 2.0F;
         this.barY = disp[1] / 2 + 12;
      }
   }

   @Override
   public void onDisable() {
      this.disabled();
   }

   @SubscribeEvent
   public void onMouse(MouseEvent e) {
      if (e.button == 1
         && Utils.holdingFireball()
         && this.manual.isToggled()
         && !this.enabled
         && (!this.onlyWithVelocity.isToggled() || this.onlyWithVelocity.isToggled() && ModuleManager.velocity.isEnabled())) {
         this.enabled();
         e.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.manual.isToggled()) {
         this.manualWasOn = true;
      } else {
         if (this.manualWasOn) {
            this.disabled();
         }

         this.manualWasOn = false;
      }

      if (this.manual.isToggled() && this.disableKey.isPressed() && Utils.jumpDown()) {
         function = false;
         this.disabled();
      }

      if (this.spoofItem.isToggled() && this.lastSlot != -1 && !this.manual.isToggled()) {
         ((IMixinItemRenderer)mc.getItemRenderer()).setCancelUpdate(true);
         ((IMixinItemRenderer)mc.getItemRenderer()).setCancelReset(true);
      }

      if (this.swapped && this.rotateTick == 0) {
         this.resetSlot();
         this.swapped = false;
      }

      if (function) {
         if (this.enabled) {
            if (!Utils.isMoving() && this.mode.getInput() == 0.0) {
               this.notMoving = true;
            }

            if (this.boostSetting.getInput() == 0.0 && this.verticalMotion.getInput() == 0.0) {
               Utils.modulePrint("&cValues are set to 0!");
               this.disabled();
               return;
            }

            int fireballSlot = this.setupFireballSlot(true);
            if (fireballSlot != -1) {
               if (!this.manual.isToggled()) {
                  this.lastSlot = this.spoofSlot = mc.thePlayer.inventory.currentItem;
                  if (mc.thePlayer.inventory.currentItem != fireballSlot) {
                     mc.thePlayer.inventory.currentItem = fireballSlot;
                     this.swapped = true;
                  }
               }

               this.rotateTick = 1;
               if (this.stopMovement.isToggled() && this.mode.getInput() != 2.0) {
                  this.stopTime = 1;
               }
            }

            this.enabled = false;
         }

         if (this.notMoving) {
            this.motionDecayVal = 21.0;
         } else {
            this.motionDecayVal = this.motionDecay.getInput() / 2.5;
         }

         if (this.stopTime == -1) {
            if (this.delayTicks > -1) {
               this.delayTicks++;
            }

            if (++this.boostTicks > (!this.verticalKey() ? 33 : (!this.notMoving ? 32 : 33))) {
               this.disabled();
               return;
            }
         }

         long FIREBALL_TIMEOUT = 750L;
         if (Utils.isHypixel()) {
            if (this.jump.isToggled() && this.mode.getInput() != 2.0) {
               FIREBALL_TIMEOUT = 350L;
            } else {
               FIREBALL_TIMEOUT = 300L;
            }
         }

         if (this.fireballTime <= 0L || Utils.time() - this.fireballTime <= FIREBALL_TIMEOUT && !(mc.thePlayer.motionY < -0.0784000015258789)) {
            if (this.boostTicks > 0 && this.mode.getInput() != 2.0) {
               if (this.mode.getInput() == 0.0) {
                  this.modifyVertical();
               }

               if (this.allowStrafe.isToggled() && this.boostTicks < 32) {
                  Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer));
               }
            }

            if (this.mode.getInput() != 2.0) {
               this.filledWidth = this.barWidth * this.boostTicks / (!this.notMoving ? 32 : 33);
            } else {
               this.filledWidth = this.barWidth * this.delayTicks / 20.0F;
            }

            if (this.stopMovement.isToggled() && !this.notMoving && this.mode.getInput() != 2.0 && this.stopTime > 0) {
               this.stopTime++;
            }

            if (mc.thePlayer.onGround && this.boostTicks > 2) {
               this.disabled();
            }

            if (this.firstSlot != -1) {
               mc.thePlayer.inventory.currentItem = this.firstSlot;
            }
         } else {
            Utils.modulePrint("&cFireball timed out.");
            this.disabled();
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (Utils.nullCheck()) {
         if (ev.phase != Phase.END || mc.currentScreen == null && this.renderFloatProgress.isToggled() && function) {
            this.color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
            RenderUtils.drawRoundedRectangle(this.barX, this.barY, this.barX + this.barWidth, this.barY + this.barHeight, 3.0F, -11184811);
            RenderUtils.drawRoundedRectangle(this.barX, this.barY, this.barX + this.filledWidth, this.barY + this.barHeight, 3.0F, this.color);
         }
      }
   }

   @SubscribeEvent
   public void onSlotUpdate(SlotUpdateEvent e) {
      if (this.lastSlot != -1) {
         this.spoofSlot = e.slot;
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onPreMotion(PreMotionEvent e) {
      if (Utils.nullCheck()) {
         if (this.rotateTick >= 3) {
            this.rotateTick = 0;
         }

         if (this.rotateTick >= 1) {
            if (this.mode.getInput() != 2.0) {
               if ((this.invertYaw.isToggled() || this.stopMovement.isToggled()) && !this.notMoving) {
                  if (!this.stopMovement.isToggled()) {
                     this.yaw = mc.thePlayer.rotationYaw - 180.0F;
                     this.pitch = 90.0F;
                  } else {
                     this.yaw = mc.thePlayer.rotationYaw - 180.0F;
                     this.pitch = 66.3F;
                  }
               } else {
                  this.yaw = mc.thePlayer.rotationYaw;
                  this.pitch = 90.0F;
               }

               e.setRotations(this.yaw, this.pitch);
            } else {
               e.setPitch(90.0F);
            }
         }

         if (this.rotateTick > 0 && ++this.rotateTick >= 3) {
            int fireballSlot = this.setupFireballSlot(false);
            if (fireballSlot != -1) {
               this.fireballTime = System.currentTimeMillis();
               mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
               if (this.silentSwing.isToggled()) {
                  mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
               } else {
                  mc.thePlayer.swingItem();
                  if (!this.spoofItem.isToggled()) {
                     mc.getItemRenderer().resetEquippedProgress();
                  }
               }

               stopVelocity = true;
               this.boostTicks = -1;
            }
         }

         if (this.boostTicks == 1 && this.mode.getInput() != 2.0) {
            this.modifyHorizontal();
            stopVelocity = false;
         }
      }
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (Utils.holdingFireball()
         && this.manual.isToggled()
         && !this.enabled
         && (!this.onlyWithVelocity.isToggled() || this.onlyWithVelocity.isToggled() && ModuleManager.velocity.isEnabled())) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
         if (Mouse.isButtonDown(1)) {
            this.enabled();
         }
      }

      if (function) {
         mc.thePlayer.movementInput.jump = false;
         if (this.rotateTick == 3) {
            mc.thePlayer.movementInput.jump = this.jump.isToggled() || this.mode.getInput() == 2.0;
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onMoveInput(PrePlayerInputEvent e) {
      if (function) {
         if (this.rotateTick > 0 || this.fireballTime > 0L) {
            if (Utils.isMoving()) {
               e.setForward(1.0F);
            }

            e.setStrafe(0.0F);
         }

         if (this.notMoving && this.boostTicks < 3) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
            Utils.setSpeed(0.0);
         }

         if (this.stopMovement.isToggled() && !this.notMoving && this.stopTime >= 1 && this.mode.getInput() != 2.0) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
            Utils.setSpeed(0.0);
         }
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (function && Utils.nullCheck()) {
         Packet packet = e.getPacket();
         if (this.boostTicks > -1) {
            if (packet instanceof S08PacketPlayerPosLook) {
               Utils.modulePrint("&cReceived setback, disabling.");
               this.disabled();
            }
         } else {
            if (this.hideExplosion.isToggled()
               && this.fireballTime != 0L
               && (packet instanceof S0EPacketSpawnObject || packet instanceof S2APacketParticles || packet instanceof S29PacketSoundEffect)) {
               e.setCanceled(true);
            }

            if (packet instanceof S27PacketExplosion) {
               S27PacketExplosion s27 = (S27PacketExplosion)packet;
               if (this.fireballTime == 0L
                  || mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ()) > this.MAX_EXPLOSION_DIST_SQ
                  )
                {
                  Utils.modulePrint("&cToo far from fireball, disabling.");
                  this.disabled();
                  return;
               }

               if (this.mode.getInput() != 2.0) {
                  this.stopTime = -1;
                  this.fireballTime = 0L;
                  this.resetSlot();
                  this.boostTicks = 0;
               }
            }

            if (e.getPacket() instanceof S12PacketEntityVelocity) {
               S12PacketEntityVelocity s12 = (S12PacketEntityVelocity)e.getPacket();
               if (s12.getEntityID() == mc.thePlayer.getEntityId() && this.mode.getInput() == 2.0) {
                  this.delaying = true;
                  this.delayTicks = 0;
                  this.stopTime = -1;
                  this.fireballTime = 0L;
                  this.resetSlot();
                  this.boostTicks = 0;
               }
            }
         }

         if (this.delaying) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("packet", e.getPacket());
            entry.put("time", Utils.time());
            synchronized (this.packets) {
               this.packets.add(entry);
            }

            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      if (!this.packets.isEmpty()) {
         if (this.mode.getInput() == 2.0 && function) {
            long now = Utils.time();
            long delayv = 1000L;

            while (!this.packets.isEmpty()) {
               long timestamp = (Long)this.packets.get(0).get("time");
               if (now - timestamp < delayv) {
                  break;
               }

               this.flushOne();
               this.disabled();
            }

            if (!this.containsVelocity()) {
               this.flushAll();
            }
         }
      }
   }

   private int getFireballSlot() {
      int n = -1;

      for (int i = 0; i < 9; i++) {
         ItemStack getStackInSlot = mc.thePlayer.inventory.getStackInSlot(i);
         if (getStackInSlot != null && getStackInSlot.getItem() == Items.fire_charge) {
            n = i;
            break;
         }
      }

      return n;
   }

   private void enabled() {
      function = true;
      this.enabled = true;
      stopModules = true;
      this.packets.clear();
      this.delaying = false;
   }

   private void disabled() {
      this.resetSlot();
      slotReset = false;
      stopVelocity = false;
      function = false;
      this.notMoving = this.enabled = this.swapped = false;
      this.boostTicks = this.stopTime = this.rotateTick = slotResetTicks = this.delayTicks = 0;
      this.fireballTime = 0L;
      this.filledWidth = 0.0F;
      this.flushAll();
      if (!this.manual.isToggled()) {
         this.disable();
      }
   }

   private int setupFireballSlot(boolean pre) {
      int fireballSlot = this.getFireballSlot();
      if (fireballSlot == -1) {
         Utils.modulePrint("&cFireball not found.");
         this.disabled();
      } else if (pre && Utils.distanceToGround(mc.thePlayer) > 3.0) {
         Utils.modulePrint("&cCan't throw fireball right now.");
         this.disabled();
         fireballSlot = -1;
      }

      return fireballSlot;
   }

   private void resetSlot() {
      if (this.lastSlot != -1 && !this.manual.isToggled()) {
         mc.thePlayer.inventory.currentItem = this.lastSlot;
         this.lastSlot = -1;
         this.spoofSlot = -1;
         this.firstSlot = -1;
         if (this.spoofItem.isToggled()) {
            ((IMixinItemRenderer)mc.getItemRenderer()).setCancelUpdate(false);
            ((IMixinItemRenderer)mc.getItemRenderer()).setCancelReset(false);
         }
      }

      slotReset = true;
      stopModules = false;
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

   void modifyHorizontal() {
      if (this.boostSetting.getInput() != 0.0) {
         double speed = this.boostSetting.getInput() - Utils.randomizeDouble(1.0E-4, 0.0);
         if (Utils.isMoving()) {
            Utils.setSpeed(speed);
         }
      }
   }

   private void modifyVertical() {
      if (this.verticalMotion.getInput() != 0.0) {
         double ver = (!this.notMoving ? this.verticalMotion.getInput() : 1.16) * (1.0 / (1.0 + 0.05 * this.getSpeedLevel()))
            + Utils.randomizeDouble(1.0E-4, 0.1);
         double decay = this.motionDecayVal / 1000.0;
         if (this.mode.getInput() == 0.0) {
            if (this.boostTicks > 1 && !this.verticalKey()) {
               if (this.boostTicks > 1 || this.boostTicks <= (!this.notMoving ? 32 : 33)) {
                  mc.thePlayer.motionY = Utils.randomizeDouble(0.0101, 0.01);
               }
            } else if (this.boostTicks >= 1 && this.boostTicks <= (!this.notMoving ? 32 : 33)) {
               mc.thePlayer.motionY = ver - this.boostTicks * decay;
            } else if (this.boostTicks >= (!this.notMoving ? 32 : 33) + 3) {
               mc.thePlayer.motionY += 0.028;
               Utils.modulePrint("If you get this clip it & send in the raven bs v2 discord");
            }
         }
      }
   }

   private boolean verticalKey() {
      if (this.notMoving) {
         return true;
      } else {
         return this.beginFlat.isToggled() ? this.verticalKey.isPressed() : !this.flatKey.isPressed();
      }
   }

   void flushOne() {
      synchronized (this.packets) {
         Map<String, Object> entry = this.packets.remove(0);
         PacketUtils.receivePacketNoEvent((Packet)entry.get("packet"));
      }
   }

   void flushAll() {
      while (!this.packets.isEmpty()) {
         this.flushOne();
      }

      this.delaying = false;
   }

   boolean containsVelocity() {
      synchronized (this.packets) {
         int id = mc.thePlayer.getEntityId();

         for (Map<String, Object> entry : this.packets) {
            Packet p = (Packet)entry.get("packet");
            if (p instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity)p).getEntityID() == id) {
               return true;
            }
         }

         return false;
      }
   }
}
