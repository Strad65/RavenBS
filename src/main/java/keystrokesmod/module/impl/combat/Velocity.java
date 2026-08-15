package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Velocity extends Module {
   public SliderSetting mode;
   public static SliderSetting vertical;
   public static SliderSetting horizontal;
   public static SliderSetting reverseHorizontal;
   public static SliderSetting explosionsHorizontal;
   public static SliderSetting explosionsVertical;
   public static SliderSetting verticalM;
   public static SliderSetting minExtraSpeed;
   public static SliderSetting extraSpeedBoost;
   private SliderSetting chance;
   private ButtonSetting onlyWhileAttacking;
   private ButtonSetting onlyWhileTargeting;
   private ButtonSetting onlyWhileSwinging;
   private ButtonSetting disableS;
   private ButtonSetting zzWhileNotTargeting;
   private ButtonSetting delayPacket;
   public ButtonSetting allowSelfFireball;
   public static ButtonSetting reverseDebug;
   private KeySetting switchToReverse;
   private KeySetting switchToPacket;
   private ButtonSetting requireMouseDown;
   private ButtonSetting requireMovingForward;
   private ButtonSetting requireAim;
   private ButtonSetting disableLobby;
   private boolean stopFBvelo;
   public boolean disableVelo;
   private boolean buttonDown;
   private boolean pDown;
   private boolean rDown;
   private boolean setJump;
   private boolean ignoreNext;
   private boolean aiming;
   private int lastHurtTime;
   private double lastFallDistance;
   public boolean blink;
   private int delayTicks;
   private boolean exp;
   private boolean canJump;
   private int db;
   private boolean t1;
   private int t2;
   private List<Map<String, Object>> packets = new ArrayList<>();
   private SliderSetting delay;
   private boolean delaying;
   private boolean conditionals;
   private String[] modes = new String[]{"Normal", "Packet", "Reverse", "Jump", "Delay", "Timer"};

   public Velocity() {
      super("Velocity", Module.category.combat);
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
      this.registerSetting(this.delay = new SliderSetting("Maximuim Delay", "ms", 400.0, 0.0, 1000.0, 50.0));
      this.registerSetting(horizontal = new SliderSetting("Horizontal", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(vertical = new SliderSetting("Vertical", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(verticalM = new SliderSetting("Vertical Motion Limit", 1.0, -1.0, 1.0, 0.1));
      this.registerSetting(reverseHorizontal = new SliderSetting("-Horizontal", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(explosionsHorizontal = new SliderSetting("Horizontal (Explosions)", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(explosionsVertical = new SliderSetting("Vertical (Explosions)", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(minExtraSpeed = new SliderSetting("Maximum speed for extra boost", 0.0, 0.0, 0.7, 0.01));
      this.registerSetting(extraSpeedBoost = new SliderSetting("Extra speed boost multiplier", "%", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(this.chance = new SliderSetting("Chance", "%", 100.0, 0.0, 100.0, 1.0));
      this.registerSetting(this.onlyWhileAttacking = new ButtonSetting("Only while attacking", false));
      this.registerSetting(this.onlyWhileSwinging = new ButtonSetting("Only while swinging", false));
      this.registerSetting(this.onlyWhileTargeting = new ButtonSetting("Only while targeting", false));
      this.registerSetting(this.disableS = new ButtonSetting("Disable while holding S", false));
      this.registerSetting(this.zzWhileNotTargeting = new ButtonSetting("00 while not targeting", false));
      this.registerSetting(this.allowSelfFireball = new ButtonSetting("Allow self fireball", false));
      this.registerSetting(this.switchToReverse = new KeySetting("Switch to reverse", 57));
      this.registerSetting(this.switchToPacket = new KeySetting("Switch to packet", 57));
      this.registerSetting(reverseDebug = new ButtonSetting("Show reverse debug messages", false));
      this.registerSetting(this.requireMouseDown = new ButtonSetting("Require mouse down", false));
      this.registerSetting(this.requireMovingForward = new ButtonSetting("Require moving forward", false));
      this.registerSetting(this.requireAim = new ButtonSetting("Require aim", false));
      this.registerSetting(this.disableLobby = new ButtonSetting("Disable in lobby", false));
   }

   @Override
   public void guiUpdate() {
      this.delay.setVisible(this.mode.getInput() == 4.0, this);
      this.onlyWhileAttacking.setVisible(this.mode.getInput() == 0.0, this);
      this.onlyWhileSwinging.setVisible(this.mode.getInput() == 0.0, this);
      this.onlyWhileTargeting.setVisible(this.mode.getInput() == 0.0, this);
      this.disableS.setVisible(this.mode.getInput() == 0.0, this);
      this.allowSelfFireball.setVisible(this.mode.getInput() == 1.0, this);
      this.zzWhileNotTargeting.setVisible(this.mode.getInput() == 1.0, this);
      this.switchToReverse.setVisible(this.mode.getInput() == 1.0, this);
      this.switchToPacket.setVisible(this.mode.getInput() == 2.0, this);
      horizontal.setVisible(this.mode.getInput() != 2.0 && this.mode.getInput() != 3.0 && this.mode.getInput() != 4.0 && this.mode.getInput() != 5.0, this);
      vertical.setVisible(this.mode.getInput() != 2.0 && this.mode.getInput() != 3.0 && this.mode.getInput() != 4.0 && this.mode.getInput() != 5.0, this);
      verticalM.setVisible(this.mode.getInput() == 1.0, this);
      this.chance.setVisible(this.mode.getInput() != 2.0 && this.mode.getInput() != 4.0 && this.mode.getInput() != 5.0, this);
      reverseHorizontal.setVisible(this.mode.getInput() == 2.0, this);
      explosionsHorizontal.setVisible(
         this.mode.getInput() != 0.0 && this.mode.getInput() != 3.0 && this.mode.getInput() != 4.0 && this.mode.getInput() != 5.0, this
      );
      explosionsVertical.setVisible(
         this.mode.getInput() != 0.0 && this.mode.getInput() != 3.0 && this.mode.getInput() != 4.0 && this.mode.getInput() != 5.0, this
      );
      minExtraSpeed.setVisible(this.mode.getInput() == 2.0, this);
      extraSpeedBoost.setVisible(this.mode.getInput() == 2.0, this);
      reverseDebug.setVisible(this.mode.getInput() == 2.0, this);
      this.requireMouseDown.setVisible(this.mode.getInput() == 3.0, this);
      this.requireMovingForward.setVisible(this.mode.getInput() == 3.0, this);
      this.requireAim.setVisible(this.mode.getInput() == 3.0, this);
   }

   @Override
   public String getInfo() {
      String name = "";
      if (this.mode.getInput() == 0.0 || this.mode.getInput() == 1.0) {
         name = (int)horizontal.getInput() + "% " + (int)vertical.getInput() + "%";
      }

      if (this.mode.getInput() == 2.0) {
         name = "-" + (int)reverseHorizontal.getInput() + "%";
      }

      if (this.mode.getInput() == 3.0 || this.mode.getInput() == 4.0 || this.mode.getInput() == 5.0) {
         name = this.modes[(int)this.mode.getInput()];
      }

      return name;
   }

   @Override
   public void onDisable() {
      this.blink = false;
      this.delayTicks = 0;
      this.stopFBvelo = this.disableVelo = false;
      this.buttonDown = this.pDown = this.rDown = false;
      this.setJump = this.ignoreNext = this.aiming = false;
      this.lastHurtTime = 0;
      this.lastFallDistance = 0.0;
      this.db = 0;
      this.exp = false;
      this.canJump = false;
      this.flushAll();
      this.t1 = false;
      if (this.t2 != 0) {
         Utils.resetTimer();
      }

      this.t2 = 0;
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.mode.getInput() == 5.0 && this.t1) {
         this.t2++;
         Utils.getTimer().timerSpeed = 0.65F;
         if (this.t2 >= 3) {
            Utils.resetTimer();
            this.t2 = 0;
            this.t1 = false;
         }
      }

      if (Utils.tabbedIn()) {
         if (this.switchToReverse.isPressed() && this.mode.getInput() == 1.0 && !this.buttonDown) {
            this.mode.setValue(2.0);
            this.buttonDown = true;
            Utils.modulePrint(Utils.formatColor("&7[&dR&7]&7 Switched to &bReverse&7 Velocity mode"));
         }

         if (this.switchToPacket.isPressed() && this.mode.getInput() == 2.0 && !this.buttonDown) {
            this.mode.setValue(1.0);
            this.buttonDown = true;
            Utils.modulePrint(Utils.formatColor("&7[&dR&7]&7 Switched to &bPacket&7 Velocity mode"));
         }
      }

      if (!this.switchToReverse.isPressed() && !this.switchToPacket.isPressed()) {
         this.buttonDown = false;
      } else {
         this.buttonDown = true;
      }

      if (this.db > 0) {
         this.db--;
      }

      int hurtTime = mc.thePlayer.hurtTime;
      boolean onGround = mc.thePlayer.onGround;
      if (onGround && this.lastFallDistance > 3.0 && !mc.thePlayer.capabilities.allowFlying) {
         this.ignoreNext = true;
      }

      if (hurtTime > this.lastHurtTime) {
         boolean mouseDown = Mouse.isButtonDown(0) || !this.requireMouseDown.isToggled();
         boolean aimingAt = this.aiming || !this.requireAim.isToggled();
         boolean forward = mc.gameSettings.keyBindForward.isKeyDown() || !this.requireMovingForward.isToggled();
         this.handlejr(onGround, aimingAt, forward, mouseDown);
         this.ignoreNext = false;
      }

      this.lastHurtTime = hurtTime;
      this.lastFallDistance = mc.thePlayer.fallDistance;
   }

   private void handlejr(boolean onGround, boolean aimingAt, boolean forward, boolean mouseDown) {
      if (this.mode.getInput() == 3.0) {
         if (!this.disableLobby.isToggled() || !Utils.isLobby()) {
            if (this.db <= 0) {
               if (!this.ignoreNext
                  && onGround
                  && aimingAt
                  && forward
                  && mouseDown
                  && Utils.randomizeDouble(0.0, 100.0) < this.chance.getInput()
                  && !this.hasBadEffect()) {
                  KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), this.setJump = true);
                  KeyBinding.onTick(mc.gameSettings.keyBindJump.getKeyCode());
                  if (keystrokesmod.Raven.debug) {
                     Utils.sendModuleMessage(this, "&7jumping enabled");
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      if (this.setJump && !Utils.jumpDown()) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), this.setJump = false);
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "&7jumping disabled");
         }
      }

      this.conditionals = this.conditionals();
      if (!this.packets.isEmpty()) {
         long now = Utils.time();
         long delayv = (long)this.delay.getInput();

         while (!this.packets.isEmpty()) {
            long timestamp = (Long)this.packets.get(0).get("time");
            if ((now - timestamp < delayv || this.mode.getInput() != 4.0) && (now - timestamp < 150L || this.mode.getInput() != 5.0)) {
               break;
            }

            this.flushOne();
         }

         if ((!this.conditionals || mc.thePlayer.onGround || Utils.overVoid()) && this.mode.getInput() == 4.0 || !this.containsVelocity()) {
            this.flushAll();
         }
      }
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (this.canJump && mc.thePlayer.onGround) {
      }

      this.canJump = false;
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (e.getPacket() instanceof S27PacketExplosion) {
         this.db = 10;
      }

      if (Utils.nullCheck() && !LongJump.stopVelocity && !e.isCanceled() && (!this.disableLobby.isToggled() || !Utils.isLobby())) {
         if (this.mode.getInput() == 1.0 || this.mode.getInput() == 4.0 || this.mode.getInput() == 5.0) {
            if (ModuleManager.bhop.isEnabled()
               && ModuleManager.bhop.damageBoost.isToggled()
               && ModuleUtils.firstDamage
               && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed())) {
               return;
            }

            if (e.getPacket() instanceof S27PacketExplosion) {
               Packet packet = e.getPacket();
               S27PacketExplosion s27PacketExplosion = (S27PacketExplosion)e.getPacket();
               S27PacketExplosion s27 = (S27PacketExplosion)packet;
               if (this.mode.getInput() == 1.0) {
                  if (this.allowSelfFireball.isToggled()
                     && ModuleUtils.threwFireball
                     && (
                        mc.thePlayer.getPosition().distanceSq(s27.getX(), s27.getY(), s27.getZ())
                              <= ModuleUtils.MAX_EXPLOSION_DIST_SQ
                           || this.disableVelo
                     )) {
                     this.disableVelo = true;
                     ModuleUtils.threwFireball = false;
                     e.setCanceled(false);
                     return;
                  }

                  if (!this.dontEditMotion() && !this.disableVelo) {
                     if (explosionsHorizontal.getInput() == 0.0 && explosionsVertical.getInput() > 0.0) {
                        mc.thePlayer.motionY = mc.thePlayer.motionY
                           + s27PacketExplosion.func_149144_d() * explosionsVertical.getInput() / 100.0;
                     } else if (explosionsHorizontal.getInput() > 0.0 && explosionsVertical.getInput() == 0.0) {
                        mc.thePlayer.motionX = mc.thePlayer.motionX
                           + s27PacketExplosion.func_149149_c() * explosionsHorizontal.getInput() / 100.0;
                        mc.thePlayer.motionZ = mc.thePlayer.motionZ
                           + s27PacketExplosion.func_149147_e() * explosionsHorizontal.getInput() / 100.0;
                     } else if (explosionsHorizontal.getInput() > 0.0 && explosionsVertical.getInput() > 0.0) {
                        mc.thePlayer.motionX = mc.thePlayer.motionX
                           + s27PacketExplosion.func_149149_c() * explosionsHorizontal.getInput() / 100.0;
                        mc.thePlayer.motionY = mc.thePlayer.motionY
                           + s27PacketExplosion.func_149144_d() * explosionsVertical.getInput() / 100.0;
                        mc.thePlayer.motionZ = mc.thePlayer.motionZ
                           + s27PacketExplosion.func_149147_e() * explosionsHorizontal.getInput() / 100.0;
                     }
                  }
               }

               this.stopFBvelo = true;
               if (this.mode.getInput() == 1.0) {
                  e.setCanceled(true);
                  this.disableVelo = false;
               }
            }

            if (e.getPacket() instanceof S12PacketEntityVelocity
               && ((S12PacketEntityVelocity)e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
               S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity)e.getPacket();
               if ((this.mode.getInput() == 4.0 || this.mode.getInput() == 5.0 && !this.disableVelo)
                  && s12PacketEntityVelocity.getEntityID() == mc.thePlayer.getEntityId()
                  && this.conditionals
                  && !this.stopFBvelo) {
                  this.t1 = this.mode.getInput() == 5.0;
                  this.delaying = true;
                  if (this.stopFBvelo) {
                     this.exp = true;
                  }
               }

               if (this.mode.getInput() == 1.0) {
                  if (!this.stopFBvelo) {
                     if (!this.dontEditMotion() && !this.disableVelo) {
                        if (horizontal.getInput() == 0.0 && vertical.getInput() > 0.0) {
                           mc.thePlayer.motionY = s12PacketEntityVelocity.getMotionY() / 8000.0 * vertical.getInput() / 100.0;
                        } else if (horizontal.getInput() > 0.0 && vertical.getInput() == 0.0) {
                           mc.thePlayer.motionX = s12PacketEntityVelocity.getMotionX() / 8000.0 * horizontal.getInput() / 100.0;
                           mc.thePlayer.motionZ = s12PacketEntityVelocity.getMotionZ() / 8000.0 * horizontal.getInput() / 100.0;
                        } else if (horizontal.getInput() > 0.0 && vertical.getInput() > 0.0) {
                           mc.thePlayer.motionX = s12PacketEntityVelocity.getMotionX() / 8000.0 * horizontal.getInput() / 100.0;
                           mc.thePlayer.motionY = s12PacketEntityVelocity.getMotionY() / 8000.0 * vertical.getInput() / 100.0;
                           mc.thePlayer.motionZ = s12PacketEntityVelocity.getMotionZ() / 8000.0 * horizontal.getInput() / 100.0;
                        }
                     }
                  } else if (!this.dontEditMotion() && !this.disableVelo) {
                     if (explosionsHorizontal.getInput() == 0.0 && explosionsVertical.getInput() > 0.0) {
                        mc.thePlayer.motionY = s12PacketEntityVelocity.getMotionY() / 8000.0 * explosionsVertical.getInput() / 100.0;
                     } else if (explosionsHorizontal.getInput() > 0.0 && explosionsVertical.getInput() == 0.0) {
                        mc.thePlayer.motionX = s12PacketEntityVelocity.getMotionX() / 8000.0 * explosionsHorizontal.getInput() / 100.0;
                        mc.thePlayer.motionZ = s12PacketEntityVelocity.getMotionZ() / 8000.0 * explosionsHorizontal.getInput() / 100.0;
                     } else if (explosionsHorizontal.getInput() > 0.0 && explosionsVertical.getInput() > 0.0) {
                        mc.thePlayer.motionX = s12PacketEntityVelocity.getMotionX() / 8000.0 * explosionsHorizontal.getInput() / 100.0;
                        mc.thePlayer.motionY = s12PacketEntityVelocity.getMotionY() / 8000.0 * explosionsVertical.getInput() / 100.0;
                        mc.thePlayer.motionZ = s12PacketEntityVelocity.getMotionZ() / 8000.0 * explosionsHorizontal.getInput() / 100.0;
                     }
                  }
               }

               this.stopFBvelo = false;
               if (this.mode.getInput() == 1.0 && !this.disableVelo) {
                  e.setCanceled(true);
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

   boolean conditionals() {
      if (this.disableLobby.isToggled() && Utils.isLobby()) {
         return false;
      } else {
         return mc.thePlayer.isCollidedHorizontally ? false : !mc.thePlayer.capabilities.isFlying;
      }
   }

   void flushOne() {
      synchronized (this.packets) {
         Map<String, Object> entry = this.packets.remove(0);
         PacketUtils.receivePacketNoEvent((Packet)entry.get("packet"));
         if (this.mode.getInput() == 4.0
            && entry.get("packet") instanceof S12PacketEntityVelocity
            && ((S12PacketEntityVelocity)entry.get("packet")).getEntityID() == mc.thePlayer.getEntityId()) {
            S12PacketEntityVelocity s12PacketEntityVelocity = (S12PacketEntityVelocity)entry.get("packet");
            this.canJump = true;
         }
      }
   }

   void flushAll() {
      while (!this.packets.isEmpty()) {
         this.flushOne();
      }

      this.delaying = false;
      this.exp = false;
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

   @SubscribeEvent
   public void onLivingUpdate(LivingUpdateEvent ev) {
      if (this.mode.getInput() == 0.0 && Utils.nullCheck() && !LongJump.stopVelocity && !ModuleManager.bedAura.cancelKnockback()) {
         if (mc.thePlayer.maxHurtTime <= 0 || mc.thePlayer.hurtTime != mc.thePlayer.maxHurtTime) {
            return;
         }

         if (this.onlyWhileAttacking.isToggled() && !ModuleUtils.isAttacking) {
            return;
         }

         if (this.onlyWhileSwinging.isToggled() && !ModuleUtils.isSwinging) {
            return;
         }

         if (this.onlyWhileTargeting.isToggled() && (mc.objectMouseOver == null || mc.objectMouseOver.entityHit == null)) {
            return;
         }

         if (this.disableS.isToggled() && Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
            return;
         }

         if (this.chance.getInput() == 0.0) {
            return;
         }

         if (this.disableLobby.isToggled() && Utils.isLobby()) {
            return;
         }

         if (this.chance.getInput() != 100.0) {
            double ch = Math.random();
            if (ch >= this.chance.getInput() / 100.0) {
               return;
            }
         }

         if (horizontal.getInput() != 100.0) {
            mc.thePlayer.motionX = mc.thePlayer.motionX * (horizontal.getInput() / 100.0);
            mc.thePlayer.motionZ = mc.thePlayer.motionZ * (horizontal.getInput() / 100.0);
         }

         if (vertical.getInput() != 100.0) {
            mc.thePlayer.motionY = mc.thePlayer.motionY * (vertical.getInput() / 100.0);
         }
      }
   }

   @SubscribeEvent
   public void onSendPacket(SendPacketEvent e) {
      if (e.getPacket() instanceof C05PacketPlayerLook) {
         this.checkAim(((C05PacketPlayerLook)e.getPacket()).getYaw(), ((C05PacketPlayerLook)e.getPacket()).getPitch());
      } else if (e.getPacket() instanceof C06PacketPlayerPosLook) {
         this.checkAim(((C06PacketPlayerPosLook)e.getPacket()).getYaw(), ((C06PacketPlayerPosLook)e.getPacket()).getPitch());
      }
   }

   public boolean dontEditMotion() {
      return mc.thePlayer.motionY >= verticalM.getInput() && !mc.thePlayer.onGround
         || this.mode.getInput() == 1.0 && this.zzWhileNotTargeting.isToggled() && KillAura.attackingEntity == null;
   }

   private boolean hasBadEffect() {
      Iterator var1 = mc.thePlayer.getActivePotionEffects().iterator();
      if (!var1.hasNext()) {
         return false;
      }

      PotionEffect potionEffect = (PotionEffect)var1.next();
      String name = potionEffect.getEffectName();
      return name.equals("potion.jump") || name.equals("potion.poison") || name.equals("potion.wither");
   }

   private void checkAim(float yaw, float pitch) {
      MovingObjectPosition result = RotationUtils.rayTrace(5.0, ((IAccessorMinecraft)mc).getTimer().renderPartialTicks, new float[]{yaw, pitch}, null);
      this.aiming = result != null && result.typeOfHit == MovingObjectType.ENTITY && result.entityHit instanceof EntityOtherPlayerMP;
   }
}
