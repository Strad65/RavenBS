package keystrokesmod.module.impl.combat;

import java.util.Objects;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.model.Vec3;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LagRange extends Module {
   public SliderSetting latency;
   private SliderSetting activationDist;
   private SliderSetting hurttime;
   private ButtonSetting ignoreTeammates;
   private ButtonSetting weaponOnly;
   public ButtonSetting renderTimer;
   public ButtonSetting initialPosition;
   private int disableTicks;
   private Vec3 lastPosition;
   private double closest;
   private double startFallHeight;
   private boolean function;
   public boolean blink;
   private long delay;

   public LagRange() {
      super("LagRange", Module.category.combat, 0);
      this.registerSetting(this.latency = new SliderSetting("Latency", "ms", 300.0, 10.0, 500.0, 10.0));
      this.registerSetting(this.activationDist = new SliderSetting("Activation Distance", " blocks", 7.0, 0.0, 20.0, 1.0));
      this.registerSetting(this.hurttime = new SliderSetting("Hurttime", 2.0, 0.0, 10.0, 1.0));
      this.registerSetting(this.initialPosition = new ButtonSetting("Show initial position", true));
      this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", false));
      this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
   }

   @Override
   public String getInfo() {
      return (int)this.latency.getInput() + "ms";
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      this.disableTicks--;
      double boxSize = this.activationDist.getInput();
      Vec3 myPosition = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
      boolean onGround = mc.thePlayer.onGround;
      if (ModuleUtils.isBreaking) {
         this.disableTicks = 1;
         this.function = false;
      }

      if (Utils.getHorizontalSpeed() > 0.4) {
         this.disableTicks = 5;
         this.function = false;
      }

      if (Utils.holdingFishingRod() && ModuleUtils.rcTick == 1) {
         this.disableTicks = 1;
         this.function = false;
      }

      if (Utils.isReplay() || Utils.isLobby()) {
         this.disableTicks = 1;
         this.function = false;
      }

      if (mc.thePlayer.motionX == 0.0
         && mc.thePlayer.motionY == -0.0784000015258789
         && mc.thePlayer.motionZ == 0.0
         && !Utils.isMoving()) {
         this.disableTicks = 1;
         this.function = false;
      }

      this.a(boxSize, myPosition);
      boolean correctHeldItem = !this.weaponOnly.isToggled();
      if (!correctHeldItem) {
         boolean holdingWeapon = false;
         holdingWeapon = Utils.holdingWeapon();
         correctHeldItem = holdingWeapon;
      }

      this.function = correctHeldItem && this.disableTicks < 0 && this.closest != -1.0 && this.closest < boxSize * boxSize;
      if (this.lastPosition != null && !onGround && this.lastPosition.y > myPosition.y && myPosition.y > this.startFallHeight) {
         this.startFallHeight = myPosition.y;
      } else if (onGround && myPosition.y < this.startFallHeight) {
         if (this.startFallHeight - myPosition.y > 3.0 && !mc.thePlayer.capabilities.allowFlying) {
            this.disableTicks = 5;
            this.function = false;
         }

         this.startFallHeight = -Double.MAX_VALUE;
      }

      this.lastPosition = myPosition;
   }

   private void a(double boxSize, Vec3 myPosition) {
      this.closest = -1.0;

      for (Entity entity : mc.theWorld.loadedEntityList) {
         if (entity != null
            && entity != mc.thePlayer
            && !entity.isDead
            && entity instanceof EntityPlayer
            && !Utils.isFriended((EntityPlayer)entity)
            && ((EntityPlayer)entity).deathTime == 0
            && !AntiBot.isBot(entity)
            && (!Utils.isTeammate(entity) || !this.ignoreTeammates.isToggled())) {
            double maxRange = this.activationDist.getInput();
            if (mc.thePlayer.getDistanceToEntity(entity) < maxRange + maxRange / 3.0) {
               Vec3 position = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
               double distanceSq = position.distanceToSq(myPosition);
               if (this.closest == -1.0 || distanceSq < this.closest) {
                  this.closest = distanceSq;
               }
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onSendPacket(SendPacketEvent e) {
      if (e.getPacket() instanceof C02PacketUseEntity) {
         C02PacketUseEntity c02 = (C02PacketUseEntity)e.getPacket();
         int enemyHT = Utils.getHurttime(c02.getEntityFromWorld(mc.theWorld));
         if (Objects.equals(String.valueOf(c02.getAction()), "ATTACK") && enemyHT <= this.hurttime.getInput()) {
            this.disableTicks = 1;
            this.function = false;
         }
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      if (this.function) {
         if (this.delay == -1L) {
            this.delay = Utils.time();
            this.blink = true;
         }

         if (this.delay > 0L && Utils.time() - this.delay >= this.latency.getInput()) {
            this.delay = -1L;
            this.blink = false;
         }
      } else {
         this.blink = false;
         this.delay = -1L;
      }
   }
}
