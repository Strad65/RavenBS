package keystrokesmod.utility;

import java.util.Iterator;
import java.util.Objects;
import java.util.Map.Entry;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendAllPacketsEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.Velocity;
import keystrokesmod.module.impl.movement.Bhop;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.utility.command.impl.Status;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModuleUtils {
   private final Minecraft mc;
   public static boolean isBreaking;
   public static boolean threwFireball;
   public static boolean threwFireballLow;
   public static long MAX_EXPLOSION_DIST_SQ = 10L;
   private long FIREBALL_TIMEOUT = 500L;
   private long fireballTime = 0L;
   public static int inAirTicks;
   public static int groundTicks;
   public static int stillTicks;
   public static int rcTick;
   public static int fadeEdge;
   public static double offsetValue = 0.00100012;
   public static boolean isAttacking;
   public static boolean isSwinging;
   public static boolean hasAttacked;
   private int attackingTicks;
   private int swingingTicks;
   public static int unTargetTicks;
   public static int profileTicks = -1;
   public static int swapTick;
   public static int lastY;
   public static int thisY;
   public static boolean lastTickOnGround;
   public static boolean lastTickPos1;
   public static boolean lastYDif;
   private boolean thisTickOnGround;
   private boolean thisTickPos1;
   public static boolean firstDamage;
   public static boolean isBlocked;
   public static boolean damage;
   private int damageTicks;
   private boolean lowhopAir;
   private int edgeTick;
   private boolean dontCheckFD;
   public static boolean canSlow;
   public static boolean didSlow;
   public static boolean setSlow;
   public static boolean hasSlowed;
   private static boolean allowFriction;
   private float yaw;
   private boolean ldmg;
   private int placeFrequency;
   private int removeFrequency;
   private int heldDelay;
   private int rcDelay;
   public static boolean worldChange;
   public float fixedForward;
   public float fixedStrafe;
   private int sf;
   public static boolean hasTeleported;
   private int htpt;
   private int ft = 0;

   public ModuleUtils(Minecraft mc) {
      this.mc = mc;
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (Utils.nullCheck()) {
         if (e.entity == this.mc.thePlayer) {
            ModuleManager.disabler.disablerLoaded = false;
            inAirTicks = 0;
            worldChange = true;
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onSendPacketsAll(SendAllPacketsEvent e) {
      if (Utils.nullCheck()) {
         Packet packet = e.getPacket();
         if (packet instanceof C07PacketPlayerDigging && isBlocked) {
            C07PacketPlayerDigging c07 = (C07PacketPlayerDigging)packet;
            if (Objects.equals(String.valueOf(c07.getStatus()), "RELEASE_USE_ITEM")) {
               isBlocked = false;
            }
         }

         if (packet instanceof C09PacketHeldItemChange && isBlocked) {
            isBlocked = false;
         }

         if (packet instanceof C08PacketPlayerBlockPlacement && Utils.holdingSword() && !BlockUtils.isInteractable(this.mc.objectMouseOver) && !isBlocked) {
            isBlocked = true;
         }

         if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity c02 = (C02PacketUseEntity)packet;
            if (Objects.equals(String.valueOf(c02.getAction()), "ATTACK")) {
               hasAttacked = true;
            }

            isAttacking = true;
            this.attackingTicks = 5;
         }

         if (packet instanceof C0APacketAnimation) {
            isSwinging = true;
            this.swingingTicks = 5;
         }

         if (e.getPacket() instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging c07 = (C07PacketPlayerDigging)packet;
            if (Objects.equals(String.valueOf(c07.getStatus()), "START_DESTROY_BLOCK")) {
               isBreaking = true;
            }

            if (Objects.equals(String.valueOf(c07.getStatus()), "ABORT_DESTROY_BLOCK")
               || Objects.equals(String.valueOf(c07.getStatus()), "STOP_DESTROY_BLOCK")) {
               isBreaking = false;
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onSendPacket(SendPacketEvent e) {
      if (Utils.nullCheck()) {
         if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            this.placeFrequency++;
            this.sf = 0;
         }

         if (e.getPacket() instanceof C09PacketHeldItemChange) {
            swapTick = 2;
         }

         if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingFireball() && Utils.keybinds.isMouseDown(1)) {
            this.fireballTime = System.currentTimeMillis();
            threwFireball = true;
            if (this.mc.thePlayer.rotationPitch > 50.0F) {
               threwFireballLow = true;
            }
         }
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (Utils.nullCheck()) {
         if (!e.isCanceled()) {
            if (e.getPacket() instanceof S27PacketExplosion) {
               firstDamage = false;
               this.dontCheckFD = true;
            }

            if (e.getPacket() instanceof S12PacketEntityVelocity
               && ((S12PacketEntityVelocity)e.getPacket()).getEntityID() == this.mc.thePlayer.getEntityId()) {
               damage = true;
               this.damageTicks = 0;
               if (!this.dontCheckFD) {
                  firstDamage = true;
               }

               this.dontCheckFD = false;
               this.ldmg = true;
            }

            if (e.getPacket() instanceof S08PacketPlayerPosLook) {
               hasTeleported = true;
               this.htpt = 2;
            }
         }
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      hasAttacked = false;
      if (this.mc.thePlayer.hurtTime == 9) {
         this.ft = -1;
      }

      if (this.bhopBoostConditions() && this.ft == -1) {
         double base = Utils.getHorizontalSpeed();
         if (base <= 0.0) {
            base = 0.01;
         }

         Utils.setSpeed(base);
         this.ft++;
      }

      if (this.veloBoostConditions() && this.ft == -1) {
         double added = 0.0;
         if (Utils.getHorizontalSpeed() <= Velocity.minExtraSpeed.getInput()) {
            added = Velocity.extraSpeedBoost.getInput() / 100.0;
            if (Velocity.reverseDebug.isToggled()) {
               Utils.modulePrint("&7[&dR&7] Applied extra boost | Original speed: " + Utils.getHorizontalSpeed());
            }
         }

         double base = Utils.getHorizontalSpeed();
         if (base <= 0.0) {
            base = 0.01;
         }

         Utils.setSpeed(base * (Velocity.reverseHorizontal.getInput() / 100.0) * (1.0 + added));
         this.ft++;
      }

      firstDamage = false;
      worldChange = false;
   }

   private boolean bhopBoostConditions() {
      return ModuleManager.bhop.isEnabled()
         && ModuleManager.bhop.damageBoost.isToggled()
         && (!ModuleManager.bhop.damageBoostRequireKey.isToggled() || ModuleManager.bhop.damageBoostKey.isPressed());
   }

   private boolean veloBoostConditions() {
      return ModuleManager.velocity.isEnabled() && ModuleManager.velocity.mode.getInput() == 2.0;
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (++this.sf > 5) {
         ModuleManager.scaffold.hasPlaced = false;
      }

      if (hasTeleported && this.htpt > 0) {
         this.htpt--;
      } else {
         this.htpt = 0;
         hasTeleported = false;
      }

      rcTick = Utils.keybinds.isMouseDown(1) ? ++rcTick : 0;
      if (this.placeFrequency > 0) {
         if (++this.removeFrequency > 2) {
            this.removeFrequency = 0;
            this.placeFrequency--;
         }
      } else {
         this.removeFrequency = 0;
      }

      if (!Utils.keybinds.isMouseDown(1)) {
         if (++this.rcDelay > 3) {
            this.placeFrequency = 0;
         }

         this.heldDelay = 0;
      } else {
         this.rcDelay = 0;
      }

      if (this.holdingBlocks() && this.rcDelay == 0) {
         this.heldDelay++;
      } else {
         if (this.heldDelay > 0) {
            this.heldDelay--;
         }

         if (this.rcDelay == 0
            && this.heldDelay > 0
            && (this.placeFrequency > 1 || this.heldDelay > 4)
            && this.getSlot() != -1
            && ModuleManager.autoSwap.legit.isToggled()) {
            this.mc.thePlayer.inventory.currentItem = this.getSlot();
         }
      }

      if (swapTick > 0) {
         swapTick--;
      }

      if (ModuleManager.killAura.stoppedTargeting && ++unTargetTicks >= 2) {
         ModuleManager.killAura.stoppedTargeting = false;
      }

      if (canSlow || ModuleManager.scaffold.isEnabled) {
         double motionVal = 0.9507832 - inAirTicks / 10000.0 - Utils.randomizeDouble(1.0E-5, 6.0E-5);
         if (!hasSlowed) {
            motionVal -= 0.15;
         }

         if (this.mc.thePlayer.hurtTime == 0 && !setSlow && !this.mc.thePlayer.onGround) {
            hasSlowed = true;
            setSlow = true;
         }

         didSlow = true;
      }

      if (didSlow && this.mc.thePlayer.onGround) {
         didSlow = false;
         canSlow = false;
      }

      if (groundTicks > 1) {
         hasSlowed = false;
      }

      if (this.mc.thePlayer.onGround || this.mc.thePlayer.hurtTime != 0) {
         setSlow = false;
      }

      if (!ModuleManager.bhop.running && !ModuleManager.scaffold.fastScaffoldKeepY) {
         allowFriction = false;
      } else if (!this.mc.thePlayer.onGround) {
         allowFriction = true;
      }

      if (damage && ++this.damageTicks >= 8) {
         firstDamage = false;
         damage = false;
         this.damageTicks = 0;
      }

      profileTicks++;
      if (isAttacking) {
         if (this.attackingTicks <= 0) {
            isAttacking = false;
         } else {
            this.attackingTicks--;
         }
      }

      if (isSwinging) {
         if (this.swingingTicks <= 0) {
            isSwinging = false;
         } else {
            this.swingingTicks--;
         }
      }

      if (LongJump.slotReset && ++LongJump.slotResetTicks >= 2) {
         LongJump.stopModules = false;
         LongJump.slotResetTicks = 0;
         LongJump.slotReset = false;
      }

      if (this.fireballTime > 0L && System.currentTimeMillis() - this.fireballTime > this.FIREBALL_TIMEOUT / 3L) {
         threwFireballLow = false;
         ModuleManager.velocity.disableVelo = false;
      }

      if (this.fireballTime > 0L && System.currentTimeMillis() - this.fireballTime > this.FIREBALL_TIMEOUT) {
         threwFireballLow = false;
         threwFireball = false;
         this.fireballTime = 0L;
         ModuleManager.velocity.disableVelo = false;
      }

      if (Status.cooldown != 0 && this.mc.thePlayer.ticksExisted % 20 == 0) {
         Status.cooldown--;
      }
   }

   private int getSlot() {
      int slot = -1;
      int highestStack = -1;
      ItemStack heldItem = this.mc.thePlayer.getHeldItem();

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = this.mc.thePlayer.inventory.mainInventory[i];
         if (itemStack != null
            && itemStack.getItem() instanceof ItemBlock
            && Utils.canBePlaced((ItemBlock)itemStack.getItem())
            && itemStack.stackSize > 0
            && (Utils.getBedwarsStatus() != 2 || !(((ItemBlock)itemStack.getItem()).getBlock() instanceof BlockTNT))
            && (
               heldItem == null
                  || !(heldItem.getItem() instanceof ItemBlock)
                  || !Utils.canBePlaced((ItemBlock)heldItem.getItem())
                  || itemStack.getItem().getClass().equals(heldItem.getItem().getClass())
            )
            && itemStack.stackSize > highestStack) {
            highestStack = itemStack.stackSize;
            slot = i;
         }
      }

      return slot;
   }

   private boolean holdingBlocks() {
      ItemStack heldItem = this.mc.thePlayer.getHeldItem();
      return heldItem != null && heldItem.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock)heldItem.getItem());
   }

   private boolean tower() {
      return ModuleManager.tower.canTower() && ModuleManager.tower.towerMove.getInput() != 8.0;
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      int simpleY = (int)Math.round(e.posY % 1.0 * 10000.0);
      if (ModuleManager.scaffold.offsetDelay > 0) {
         ModuleManager.scaffold.offsetDelay--;
      }

      lastTickOnGround = this.thisTickOnGround;
      this.thisTickOnGround = this.mc.thePlayer.onGround;
      lastTickPos1 = this.thisTickPos1;
      this.thisTickPos1 = this.mc.thePlayer.posY % 1.0 == 0.0;
      lastY = thisY;
      thisY = (int)this.mc.thePlayer.posY;
      if (thisY < lastY + 2 && thisY > lastY - 2) {
         lastYDif = false;
      } else {
         lastYDif = true;
      }

      inAirTicks = this.mc.thePlayer.onGround ? 0 : ++inAirTicks;
      groundTicks = !this.mc.thePlayer.onGround ? 0 : ++groundTicks;
      stillTicks = Utils.isMoving() ? 0 : ++stillTicks;
      this.handleLowhop();
      if (ModuleManager.bhop.setRotation) {
         if (!ModuleManager.killAura.rotating && !ModuleManager.scaffold.isEnabled) {
            this.yaw = ModuleManager.scaffold.getMotionYaw()
               - 130.625F * Math.signum(MathHelper.wrapAngleTo180_float(ModuleManager.scaffold.getMotionYaw() - this.yaw));
            e.setYaw(this.yaw);
            RotationUtils.setFakeRotations(this.mc.thePlayer.rotationYaw, this.mc.thePlayer.rotationPitch);
         }

         if (this.mc.thePlayer.onGround) {
            ModuleManager.bhop.setRotation = false;
         }
      }

      if (ModuleManager.scaffold.canBlockFade && !ModuleManager.scaffold.isEnabled && ++fadeEdge >= 45) {
         ModuleManager.scaffold.canBlockFade = false;
         fadeEdge = 0;
         ModuleManager.scaffold.highlight.clear();
      }

      this.ldmg = false;
   }

   private void resetLowhop() {
      ModuleManager.bhop.lowhop = ModuleManager.scaffold.lowhop = false;
      ModuleManager.bhop.didMove = false;
      this.lowhopAir = false;
      this.edgeTick = 0;
   }

   public static void handleSlow() {
      didSlow = false;
      canSlow = true;
   }

   public static double applyFrictionMulti() {
      int speedAmplifier = Utils.getSpeedAmplifier();
      return speedAmplifier > 1 && allowFriction ? Bhop.friction.getInput() : 1.0;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (ModuleManager.scaffold.canBlockFade) {
         if (Utils.nullCheck() && ModuleManager.scaffold.highlightBlocks.isToggled() && !ModuleManager.scaffold.highlight.isEmpty()) {
            Iterator<Entry<BlockPos, Timer>> iterator = ModuleManager.scaffold.highlight.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<BlockPos, Timer> entry = iterator.next();
               if (entry.getValue() == null) {
                  entry.setValue(new Timer(750.0F));
                  entry.getValue().start();
               }

               int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
               if (alpha == 0) {
                  iterator.remove();
               } else {
                  RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int)HUD.theme.getInput(), 0.0), alpha), true, false);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent e) {
      if (Utils.nullCheck()) {
         String stripped = Utils.stripColor(e.message.getUnformattedText());
         if (stripped.contains("You tipped ") && stripped.contains(" in") && stripped.contains("!") && Status.start) {
            Status.start = false;
            Utils.modulePrint("§a " + Status.ign + " is online");
            e.setCanceled(true);
         }

         if ((
               stripped.contains("You've already tipped someone in the past hour in") && stripped.contains("! Wait a bit and try again!")
                  || stripped.contains("You've already tipped that person today in ")
            )
            && Status.start) {
            Status.start = false;
            Utils.modulePrint("§a " + Status.ign + " is online");
            e.setCanceled(true);
         }

         if (stripped.contains("That player is not online, try another user!") && Status.start) {
            Status.start = false;
            Utils.modulePrint("§7 " + Status.ign + " is offline");
            e.setCanceled(true);
         }

         if (stripped.contains("Can't find a player by the name of '") && Status.start) {
            Status.cooldown = 0;
            Status.start = false;
            Status.currentMode = Status.lastMode;
            Utils.modulePrint("§7 " + Status.ign + " doesn't exist");
            e.setCanceled(true);
         }

         if (stripped.contains("That's not a valid username!") && Status.start) {
            Status.cooldown = 0;
            Status.start = false;
            Status.currentMode = Status.lastMode;
            Utils.modulePrint("§binvalid username");
            e.setCanceled(true);
         }

         if (stripped.contains("You cannot give yourself tips!") && Status.start) {
            Status.cooldown = 0;
            Status.start = false;
            Status.currentMode = Status.lastMode;
            Utils.modulePrint("§a " + Status.ign + " is online");
            e.setCanceled(true);
         }
      }
   }

   private void handleLowhop() {
      Block blockAbove = BlockUtils.getBlock(
         new BlockPos(this.mc.thePlayer.posX, this.mc.thePlayer.posY + 2.0, this.mc.thePlayer.posZ)
      );
      Block blockBelow = BlockUtils.getBlock(
         new BlockPos(this.mc.thePlayer.posX, this.mc.thePlayer.posY - 1.0, this.mc.thePlayer.posZ)
      );
      Block blockBelow2 = BlockUtils.getBlock(
         new BlockPos(this.mc.thePlayer.posX, this.mc.thePlayer.posY - 2.0, this.mc.thePlayer.posZ)
      );
      Block block = BlockUtils.getBlock(
         new BlockPos(this.mc.thePlayer.posX, this.mc.thePlayer.posY, this.mc.thePlayer.posZ)
      );
      int simpleY = (int)Math.round(this.mc.thePlayer.posY % 1.0 * 10000.0);
      if ((ModuleManager.bhop.didMove || ModuleManager.scaffold.lowhop)
         && (!ModuleManager.bhop.disablerOnly.isToggled() || ModuleManager.bhop.disablerOnly.isToggled() && ModuleManager.disabler.disablerLoaded)) {
         if (ModuleManager.scaffold.lowhop) {
            switch (simpleY) {
               case 1138:
                  this.mc.thePlayer.motionY -= 0.13;
                  break;
               case 2031:
                  this.mc.thePlayer.motionY -= 0.2;
                  this.resetLowhop();
                  break;
               case 4200:
                  this.mc.thePlayer.motionY = 0.39;
            }
         } else if (ModuleManager.bhop.didMove) {
            if (this.mc.thePlayer.isCollidedVertically
               || this.ldmg && Velocity.vertical.getInput() != 0.0 && !ModuleManager.velocity.dontEditMotion()
               || block instanceof BlockSlab) {
               this.resetLowhop();
               return;
            }

            switch ((int)ModuleManager.bhop.mode.getInput()) {
               case 3:
                  switch (simpleY) {
                     case 13:
                        this.mc.thePlayer.motionY -= 0.02483;
                        ModuleManager.bhop.lowhop = true;
                        break;
                     case 2000:
                        this.mc.thePlayer.motionY -= 0.1913;
                        break;
                     case 7016:
                        this.mc.thePlayer.motionY += 0.08;
                  }

                  if (inAirTicks >= 7 && Utils.isMoving()) {
                     Utils.setSpeed(Utils.getHorizontalSpeed(this.mc.thePlayer));
                  }

                  if (inAirTicks >= 9) {
                     this.resetLowhop();
                  }
                  break;
               case 4:
                  if (ModuleManager.bhop.isNormalPos && !(block instanceof BlockStairs)) {
                     boolean g1 = Utils.distanceToGround(this.mc.thePlayer) <= 1.2;
                     if (inAirTicks < 9 && (inAirTicks < 5 || g1)) {
                        if (inAirTicks == 1) {
                           this.mc.thePlayer.motionY = 0.38999998569488;
                           ModuleManager.bhop.lowhop = true;
                        }

                        if (inAirTicks == 2) {
                           this.mc.thePlayer.motionY = 0.30379999189377;
                        }

                        if (inAirTicks == 3) {
                           this.mc.thePlayer.motionY = 0.08842400075912;
                        }

                        if (inAirTicks == 4) {
                           this.mc.thePlayer.motionY = -0.19174457909538;
                        }

                        if (inAirTicks == 5 && g1) {
                           this.mc.thePlayer.motionY = -0.26630949469659;
                        }

                        if (inAirTicks == 6 && g1) {
                           this.mc.thePlayer.motionY = -0.26438340940798;
                        }

                        if (inAirTicks == 7 && g1) {
                           this.mc.thePlayer.motionY = -0.33749574778843;
                        }

                        if (inAirTicks >= 6 && Utils.isMoving()) {
                           Utils.setSpeed(Utils.getHorizontalSpeed(this.mc.thePlayer));
                        }
                     } else {
                        this.resetLowhop();
                     }
                  } else {
                     this.resetLowhop();
                  }
                  break;
               case 5:
                  switch (simpleY) {
                     case 1138:
                        this.mc.thePlayer.motionY -= 0.13;
                        break;
                     case 2031:
                        this.mc.thePlayer.motionY -= 0.2;
                        this.resetLowhop();
                        break;
                     case 4200:
                        this.mc.thePlayer.motionY = 0.39;
                        ModuleManager.bhop.lowhop = true;
                  }
            }
         }
      }

      if (!this.mc.thePlayer.onGround) {
         this.lowhopAir = true;
      } else if (this.lowhopAir) {
         this.resetLowhop();
         if (!ModuleManager.bhop.isEnabled()) {
            ModuleManager.bhop.isNormalPos = false;
         }
      }
   }
}
