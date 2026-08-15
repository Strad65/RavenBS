package keystrokesmod.module.impl.combat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreSlotScrollEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.event.SlotUpdateEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlinkHandler;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityGiantZombie;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Mouse;

public class KillAura extends Module {
   private SliderSetting aps;
   public SliderSetting autoBlockMode;
   private SliderSetting fov;
   private SliderSetting attackRange;
   private SliderSetting swingRange;
   private SliderSetting blockRange;
   public SliderSetting rotationMode;
   public SliderSetting rotateMode;
   private SliderSetting rotationSmoothing;
   private SliderSetting sortMode;
   private SliderSetting switchDelay;
   private SliderSetting targets;
   private ButtonSetting attackMobs;
   private ButtonSetting targetInvis;
   private ButtonSetting disableInInventory;
   private ButtonSetting disableWhileBlocking;
   private ButtonSetting disableWhileMining;
   private ButtonSetting hitThroughBlocks;
   private ButtonSetting ignoreTeammates;
   public ButtonSetting manualBlock;
   private ButtonSetting prioritizeEnemies;
   private ButtonSetting requireMouseDown;
   private ButtonSetting silentSwing;
   private ButtonSetting weaponOnly;
   private String[] autoBlockModes = new String[]{"Manual", "Vanilla", "Partial", "Interact A", "Interact B", "Swap", "Hypixel", "Hypixel 2"};
   private String[] rotationModes = new String[]{"Silent", "Lock view", "None"};
   private String[] rotateModes = new String[]{"Attack", "Swing"};
   private String[] sortModes = new String[]{"Distance", "Health", "Hurttime", "Yaw"};
   private String[] swapBlacklist = new String[]{"compass", "snowball", "spawn", "skull"};
   public static EntityLivingBase target;
   public static EntityLivingBase attackingEntity;
   private HashMap<Integer, Integer> hitMap = new HashMap<>();
   private List<Entity> hostileMobs = new ArrayList<>();
   private Map<Integer, Boolean> golems = new HashMap<>();
   private double attackv = 0.006;
   public boolean blockingClient;
   public boolean blockingServer;
   private int interactTicks;
   private boolean partialDown;
   private int partialTicks;
   private boolean wasUsing;
   public boolean hasAutoblocked;
   public boolean hasBlocked;
   private boolean swapped;
   public boolean blink;
   private long lastTime = 0L;
   private long delay;
   private boolean shouldAttack;
   private int previousAutoBlockMode;
   private boolean reset;
   private boolean rotated;
   private boolean sendUnBlock;
   private int delayTicks = 0;
   private boolean lastPressedLeft;
   private boolean lastPressedRight;
   public boolean stoppedTargeting;
   public boolean targeting;
   public boolean rotating;
   private int cycle;
   public int sAttacked;
   private int lastSet;
   private boolean hasTargeted;
   public boolean t;
   private int getTicks;
   private boolean attacked;
   private boolean hb2;
   double dst2;

   public KillAura() {
      super("KillAura", Module.category.combat);
      this.registerSetting(this.aps = new SliderSetting("APS", 16.0, 1.0, 20.0, 0.5));
      this.registerSetting(this.autoBlockMode = new SliderSetting("Autoblock", 0, this.autoBlockModes));
      this.registerSetting(this.fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
      this.registerSetting(this.attackRange = new SliderSetting("Range (attack)", 3.0, 3.0, 6.0, 0.05));
      this.registerSetting(this.swingRange = new SliderSetting("Range (swing)", 3.3, 3.0, 8.0, 0.05));
      this.registerSetting(this.blockRange = new SliderSetting("Range (block)", 6.0, 3.0, 12.0, 0.05));
      this.registerSetting(this.rotationMode = new SliderSetting("Rotation mode", 0, this.rotationModes));
      this.registerSetting(this.rotateMode = new SliderSetting("Rotate on", 0, this.rotateModes));
      this.registerSetting(this.rotationSmoothing = new SliderSetting("Rotation smoothing", 0.0, 0.0, 10.0, 1.0));
      this.registerSetting(this.sortMode = new SliderSetting("Sort mode", 0, this.sortModes));
      this.registerSetting(this.switchDelay = new SliderSetting("Switch delay", "ms", 200.0, 50.0, 1000.0, 25.0));
      this.registerSetting(this.targets = new SliderSetting("Targets", 3.0, 1.0, 10.0, 1.0));
      this.registerSetting(this.targetInvis = new ButtonSetting("Target invis", true));
      this.registerSetting(this.attackMobs = new ButtonSetting("Attack mobs", false));
      this.registerSetting(this.disableInInventory = new ButtonSetting("Disable in inventory", true));
      this.registerSetting(this.disableWhileBlocking = new ButtonSetting("Disable while blocking", false));
      this.registerSetting(this.disableWhileMining = new ButtonSetting("Disable while mining", false));
      this.registerSetting(this.hitThroughBlocks = new ButtonSetting("Hit through blocks", true));
      this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", true));
      this.registerSetting(this.manualBlock = new ButtonSetting("Manual block", false));
      this.registerSetting(this.prioritizeEnemies = new ButtonSetting("Prioritize enemies", false));
      this.registerSetting(this.requireMouseDown = new ButtonSetting("Require mouse down", false));
      this.registerSetting(this.silentSwing = new ButtonSetting("Silent swing while blocking", false));
      this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
   }

   @Override
   public String getInfo() {
      return this.rotationMode.getInput() == 2.0 ? String.valueOf((int)this.fov.getInput()) : this.rotationModes[(int)this.rotationMode.getInput()];
   }

   @Override
   public void onEnable() {
      if (this.rotationMode.getInput() == 0.0 && this.autoBlockMode.getInput() == 0.0) {
         this.delayTicks = 1;
      }
   }

   @Override
   public void onDisable() {
      this.hitMap.clear();
      this.lastSet = 0;
      if (this.autoBlockOverride()) {
         this.resetAutoblock(true);
      }

      this.blink = false;
      this.interactTicks = 0;
      this.setTarget(null);
      if (this.rotated || this.reset) {
         this.resetYaw();
      }

      this.rotated = false;
      this.swapped = false;
      this.partialTicks = 0;
      this.delayTicks = 0;
      this.sAttacked = 0;
      this.t = false;
      this.hasTargeted = false;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onPreUpdate(PreUpdateEvent e) {
      this.targeting = false;
      this.wasUsing = mc.gameSettings.keyBindUseItem.isKeyDown();
      this.hb2 = false;
      if (target == null) {
         if ((!Utils.holdingSword() || this.canNb()) && this.hasTargeted && Mouse.isButtonDown(1) && Utils.tabbedIn()) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), true);
         }

         this.hasTargeted = false;
      } else {
         this.hasTargeted = true;
      }

      if ((
            this.autoBlockMode.getInput() == 2.0
               || this.autoBlockMode.getInput() == 3.0
               || this.autoBlockMode.getInput() == 4.0
               || this.autoBlockMode.getInput() == 6.0
         )
         && (target == null || !this.manualBlock() && this.manualBlock.isToggled())) {
         if (ModuleUtils.swapTick == 0 && !ModuleUtils.isBlocked) {
            this.interactTicks = 1;
            this.getTicks = (int)this.getAPSToTicks(10.0);
         } else {
            this.interactTicks = 0;
         }
      }

      if (this.reset) {
         this.resetYaw();
         this.reset = false;
      }

      if (ModuleManager.scaffold.isEnabled) {
         this.setTarget(null);
         this.resetAutoblock(false);
      } else {
         if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            boolean pressedLeft = Mouse.isButtonDown(0);
            if (pressedLeft && !this.lastPressedLeft) {
               this.onCustomMouse(0, true);
            }

            if (!pressedLeft && this.lastPressedLeft) {
               this.onCustomMouse(0, false);
            }

            boolean pressedRight = Mouse.isButtonDown(1);
            if (pressedRight && !this.lastPressedRight) {
               this.onCustomMouse(1, true);
            }

            if (!pressedRight && this.lastPressedRight) {
               this.onCustomMouse(1, false);
            }

            this.lastPressedRight = pressedRight;
            this.lastPressedLeft = pressedLeft;
         }

         if (!this.basicCondition() || !this.settingCondition()) {
            this.setTarget(null);
         }

         this.delayTicks--;
         if (this.delayTicks < 0) {
            if (this.sendUnBlock) {
               this.sendUnBlock = false;
               if (!keystrokesmod.Raven.packetsHandler.C07.sentCurrentTick.get()) {
                  if (!this.canNb()) {
                     this.sendDigPacket();
                  }
               }
            } else if (target == null) {
               this.resetAutoblock(true);
            } else if (ModuleManager.bedAura.shouldUnblock) {
               this.resetAutoblock(true);
            } else if (ModuleManager.bedAura.stopAutoblock) {
               this.resetAutoblock(false);
            } else {
               double distanceToBB = getDistanceToBoundingBox(target);
               boolean inBlockRange = distanceToBB <= this.blockRange.getInput();
               if (this.autoBlockOverride() && inBlockRange && (this.manualBlock() || !this.manualBlock.isToggled())) {
                  if (inBlockRange && this.autoBlockOverride() && this.manualBlock()) {
                     this.handleAutoBlock(distanceToBB);
                     this.hb2 = true;
                  } else if (this.autoBlockOverride() && !Utils.holdingSword() || !inBlockRange || !this.manualBlock()) {
                     this.resetAutoblock(true);
                     this.interactTicks = 0;
                  }
               } else {
                  this.handleSwingAndAttack(distanceToBB, false);
                  this.resetAutoblock(true);
                  this.interactTicks = 0;
               }

               if (inBlockRange) {
                  this.handleBlocking();
               }

               if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
                  boolean pressedRight = Mouse.isButtonDown(1);
                  if (pressedRight && !this.lastPressedRight) {
                     this.onCustomMouse(1, true);
                  }

                  if (!pressedRight && this.lastPressedRight) {
                     this.onCustomMouse(1, false);
                  }

                  this.lastPressedRight = pressedRight;
               }

               this.targeting = true;
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onClientRotation(ClientRotationEvent e) {
      this.rotating = false;
      this.handleTarget();
      if (this.delayTicks >= 0) {
         if (this.rotated) {
            this.resetYaw(e);
         }
      } else if (!this.basicCondition() || !this.settingCondition()) {
         this.setTarget(null);
         if (this.rotated) {
            this.resetYaw(e);
         }
      } else if (target == null) {
         if (this.rotated) {
            this.resetYaw(e);
         }
      } else if (ModuleManager.bedAura.stopAutoblock) {
         if (this.rotated) {
            this.resetYaw(e);
         }
      } else {
         if (this.rotationMode.getInput() != 2.0) {
            if (this.rotateMode.getInput() == 0.0 && this.inRange(target, this.attackRange.getInput() - this.attackv)
               || this.rotateMode.getInput() == 1.0 && this.inRange(target, this.swingRange.getInput())) {
               float[] rotations = RotationUtils.getRotations(target, RotationUtils.prevRenderYaw, RotationUtils.prevRenderPitch);
               float[] smoothedRotations = this.getRotationsSmoothed(rotations);
               if (this.rotationMode.getInput() == 0.0) {
                  e.yaw = smoothedRotations[0];
                  e.pitch = smoothedRotations[1];
                  this.rotated = true;
                  this.rotating = true;
               } else {
                  mc.thePlayer.rotationYaw = smoothedRotations[0];
                  mc.thePlayer.rotationPitch = smoothedRotations[1];
                  this.rotating = true;
               }
            } else if (this.rotationMode.getInput() == 0.0 && this.rotated) {
               this.reset = true;
               e.yaw = RotationUtils.serverRotations[0];
               e.pitch = RotationUtils.serverRotations[1];
               this.rotated = false;
               this.rotating = true;
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onSendPacket(SendPacketEvent e) {
      if (Utils.nullCheck()) {
         Packet packet = e.getPacket();
         if (packet instanceof C08PacketPlayerBlockPlacement) {
            C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement)e.getPacket();
            if (this.delayTicks >= 0 && p.getStack() != null && p.getStack().getItem() instanceof ItemSword && p.getPlacedBlockDirection() != 255) {
               e.setCanceled(true);
            }
         }

         if (packet instanceof C08PacketPlayerBlockPlacement
            && (this.autoBlockMode.getInput() == 3.0 || this.autoBlockMode.getInput() == 6.0)
            && target != null
            && (ModuleUtils.swapTick > 0 && !this.blockingClient || this.interactTicks == 1)) {
            e.setCanceled(true);
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.rotationMode.getInput() == 1.0 && target != null && this.inRange(target, this.attackRange.getInput() - this.attackv)) {
         float[] rotations = RotationUtils.getRotations(target, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
         float[] smoothedRotations = this.getRotationsSmoothed(rotations);
         mc.thePlayer.rotationYaw = smoothedRotations[0];
         mc.thePlayer.rotationPitch = smoothedRotations[1];
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent event) {
      if (Utils.nullCheck()) {
         if (event.phase == Phase.START && System.currentTimeMillis() - this.lastTime >= this.delay && target != null) {
            this.lastTime = System.currentTimeMillis();
            this.updateAttackDelay();
            if (target != null) {
               this.shouldAttack = true;
            }

            if (this.rotationMode.getInput() == 0.0) {
            }
         }
      }
   }

   @SubscribeEvent
   public void onMouse(MouseEvent e) {
      if (e.button == 0 || e.button == 1) {
         if (!Utils.holdingWeapon() || target == null || !this.settingCondition()) {
            return;
         }

         e.setCanceled(true);
      }
   }

   @SubscribeEvent
   public void onScrollSlot(PreSlotScrollEvent e) {
      int slot = e.slot;
      slot = slot > 0 ? 1 : (slot < 0 ? -1 : 0);
      slot = Math.floorMod(mc.thePlayer.inventory.currentItem - slot, 9);
      ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
      if (stack != null && stack.getItem() instanceof ItemSword && this.wasUsing && Utils.lookingAtBlock()) {
         this.onSwapSlot();
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "&7Scroll swap detected, setting delay to &b" + this.delayTicks + "&7. (&d" + mc.thePlayer.ticksExisted + "&7)");
         }
      }
   }

   @SubscribeEvent
   public void onSlotUpdate(SlotUpdateEvent e) {
      ItemStack stack = mc.thePlayer.inventory.getStackInSlot(e.slot);
      if (stack != null && stack.getItem() instanceof ItemSword && this.wasUsing && Utils.lookingAtBlock()) {
         this.onSwapSlot();
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "&7Swap detected, setting delay to &b" + this.delayTicks + "&7. (&d" + mc.thePlayer.ticksExisted + "&7)");
         }
      }
   }

   @SubscribeEvent
   public void onSetAttackTarget(LivingSetAttackTargetEvent e) {
      if (e.entity != null && !this.hostileMobs.contains(e.entity)) {
         if (!(e.target instanceof EntityPlayer) || !e.target.getName().equals(mc.thePlayer.getName())) {
            return;
         }

         this.hostileMobs.add(e.entity);
      }

      if (e.target == null && this.hostileMobs.contains(e.entity)) {
         this.hostileMobs.remove(e.entity);
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "&7mob stopped attack player");
         }
      }
   }

   public void onSwapSlot() {
      this.delayTicks = 1;
      if (this.autoBlockMode.getInput() > 0.0 && !this.manualBlock()) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
      }
   }

   public void onCustomMouse(int button, boolean state) {
      if (!this.autoBlockOverride()) {
         if (button == 1) {
            if (state) {
               if (target != null) {
                  if (this.basicCondition()
                     && this.settingCondition()
                     && !ModuleManager.bedAura.breakTick
                     && this.isLookingAtEntity()
                     && (!mc.thePlayer.isBlocking() || !this.disableWhileBlocking.isToggled())) {
                     this.interactAt(true, true, false, true);
                  }

                  ReflectionUtils.setItemInUse(this.blockingClient = true);
                  this.sendBlockPacket();
               } else {
                  this.delayTicks = 1;
               }
            } else {
               KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
               if (this.blockingClient) {
                  this.srb();
                  this.sendUnBlock = true;
               }
            }
         } else if (button == 0) {
            if (!state) {
               this.delayTicks = 1;
            }

            if (mc.currentScreen == null
               && state
               && mc.objectMouseOver != null
               && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK
               && !Mouse.isButtonDown(1)) {
               KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
               KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            } else if (!state) {
               KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            }
         }
      }
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.hitMap.clear();
         this.lastSet = 0;
         this.hostileMobs.clear();
         this.golems.clear();
      }
   }

   private void setTarget(Entity entity) {
      if (entity != null && entity instanceof EntityLivingBase) {
         target = (EntityLivingBase)entity;
         this.sAttacked++;
         this.t = true;
      } else {
         this.srb();
         if (this.autoBlockOverride()) {
            this.resetAutoblock(true);
         }

         this.swapped = false;
         this.partialTicks = 0;
         this.interactTicks = 0;
         if (target != null) {
            this.stoppedTargeting = true;
            ModuleUtils.unTargetTicks = 0;
         }

         target = null;
         attackingEntity = null;
         this.sAttacked = 0;
         this.t = false;
         this.attacked = false;
      }
   }

   private void handleTarget() {
      List<EntityLivingBase> availableTargets = new ArrayList<>();
      double maxRange = this.getMaxRange();

      for (Entity entity : mc.theWorld.loadedEntityList) {
         if (entity != null
            && entity != mc.thePlayer
            && !entity.isDead
            && (
               entity instanceof EntityPlayer
                  ? !Utils.isFriended((EntityPlayer)entity)
                     && ((EntityPlayer)entity).deathTime == 0
                     && !AntiBot.isBot(entity)
                     && (!Utils.isTeammate(entity) || !this.ignoreTeammates.isToggled())
                  : entity instanceof EntityCreature
                     && this.attackMobs.isToggled()
                     && ((EntityCreature)entity).deathTime == 0
                     && entity.getClass().getCanonicalName().startsWith("net.minecraft.entity.monster.")
                     && (Utils.getBedwarsStatus() != 2 || !(entity instanceof EntityPigZombie))
            )
            && (!entity.isInvisible() || this.targetInvis.isToggled())) {
            float fovInput = (float)this.fov.getInput();
            if ((fovInput == 360.0F || Utils.inFov(fovInput, entity)) && mc.thePlayer.getDistanceToEntity(entity) < maxRange + maxRange / 3.0) {
               availableTargets.add((EntityLivingBase)entity);
            }
         }
      }

      List<KillAura.KillAuraTarget> toClassTargets = new ArrayList<>();

      for (EntityLivingBase target : availableTargets) {
         double distanceRayCasted = getDistanceToBoundingBox(target);
         if (!(distanceRayCasted > maxRange)
            && (target instanceof EntityPlayer || !this.attackMobs.isToggled() || this.isHostile((EntityCreature)target))
            && (this.hitThroughBlocks.isToggled() || Utils.canPlayerBeSeen(target) && this.inRange(target, this.attackRange.getInput() - this.attackv))) {
            toClassTargets.add(
               new KillAura.KillAuraTarget(
                  distanceRayCasted,
                  target.getHealth(),
                  target.hurtTime,
                  RotationUtils.distanceFromYaw(target, false),
                  target.getEntityId(),
                  target instanceof EntityPlayer ? Utils.isEnemy((EntityPlayer)target) : false
               )
            );
         }
      }

      Comparator<KillAura.KillAuraTarget> comparator = null;
      switch ((int)this.sortMode.getInput()) {
         case 0:
            comparator = Comparator.comparingDouble(entity -> entity.distance);
            break;
         case 1:
            comparator = Comparator.comparingDouble(entityPlayer -> entityPlayer.health);
            break;
         case 2:
            comparator = Comparator.comparingDouble(entityPlayer2 -> entityPlayer2.hurttime);
            break;
         case 3:
            comparator = Comparator.comparingDouble(entity2 -> entity2.yawDelta);
      }

      if (this.prioritizeEnemies.isToggled()) {
         List<KillAura.KillAuraTarget> enemies = new ArrayList<>();

         for (KillAura.KillAuraTarget entity : toClassTargets) {
            if (entity.isEnemy) {
               enemies.add(entity);
            }
         }

         if (!enemies.isEmpty()) {
            toClassTargets = new ArrayList<>(enemies);
         }
      }

      if (this.sortMode.getInput() != 0.0) {
         Collections.sort(toClassTargets, Comparator.comparingDouble(entity -> entity.distance));
      }

      Collections.sort(toClassTargets, comparator);
      List<KillAura.KillAuraTarget> attackTargets = new ArrayList<>();

      for (KillAura.KillAuraTarget killAuraTarget : toClassTargets) {
         if (killAuraTarget.distance <= this.attackRange.getInput() - this.attackv) {
            attackTargets.add(killAuraTarget);
         }
      }

      if (attackTargets.isEmpty()) {
         if (!toClassTargets.isEmpty()) {
            KillAura.KillAuraTarget killAuraTarget = toClassTargets.get(0);
            this.setTarget(mc.theWorld.getEntityByID(killAuraTarget.entityId));
         } else {
            this.setTarget(null);
         }
      } else {
         if (this.sAttacked == 0) {
            this.lastSet++;
         }

         int ticksExisted = this.lastSet;
         int switchDelayTicks = (int)(this.switchDelay.getInput() / 50.0);
         long noHitTicks = (long)Math.min(attackTargets.size(), this.targets.getInput()) * switchDelayTicks;

         for (KillAura.KillAuraTarget auraTarget : attackTargets) {
            Integer firstHit = this.hitMap.get(auraTarget.entityId);
            if (firstHit != null && ticksExisted - firstHit < switchDelayTicks && auraTarget.distance < this.attackRange.getInput() - this.attackv) {
               this.setTarget(mc.theWorld.getEntityByID(auraTarget.entityId));
               return;
            }
         }

         for (KillAura.KillAuraTarget auraTarget : attackTargets) {
            Integer firstHit = this.hitMap.get(auraTarget.entityId);
            if (firstHit == null || ticksExisted >= firstHit.intValue() + noHitTicks) {
               this.hitMap.put(auraTarget.entityId, this.lastSet);
               this.setTarget(mc.theWorld.getEntityByID(auraTarget.entityId));
               return;
            }
         }
      }
   }

   private void handleSwingAndAttack(double distance, boolean swung) {
      boolean inAttackDistance = this.inRange(target, this.attackRange.getInput() - this.attackv);
      if ((distance <= this.swingRange.getInput() || inAttackDistance)
         && this.shouldAttack
         && !swung
         && (!mc.thePlayer.isBlocking() || !this.disableWhileBlocking.isToggled())) {
         this.swingItem();
      }

      if (inAttackDistance) {
         attackingEntity = target;
         if (this.shouldAttack) {
            this.shouldAttack = false;
            if (ModuleManager.bedAura.breakTick) {
               return;
            }

            if (!this.isLookingAtEntity()) {
               return;
            }

            if (!mc.thePlayer.isBlocking() || !this.disableWhileBlocking.isToggled()) {
               mc.playerController.attackEntity(mc.thePlayer, target);
               this.sAttacked = 0;
            }
         }
      } else {
         attackingEntity = null;
      }
   }

   private boolean canNb() {
      return Mouse.isButtonDown(1)
         && (ModuleManager.noSlow == null || !ModuleManager.noSlow.isEnabled() || NoSlow.sword.getInput() != 2.0 || !NoSlow.cantBlock);
   }

   private boolean isHostile(EntityCreature entityCreature) {
      if (SkyWars.onlyAuraHostiles()) {
         return entityCreature instanceof EntityGiantZombie ? false : !ModuleManager.skyWars.spawnedMobs.contains(entityCreature.getEntityId());
      }

      if (entityCreature instanceof EntitySilverfish) {
         String teamColor = Utils.getFirstColorCode(entityCreature.getCustomNameTag());
         String teamColorSelf = Utils.getFirstColorCode(mc.thePlayer.getDisplayName().getFormattedText());
         return teamColor.isEmpty() || !teamColorSelf.equals(teamColor) && !Utils.isTeammate(entityCreature);
      }

      if (!(entityCreature instanceof EntityIronGolem)) {
         return entityCreature instanceof EntityPigZombie && Utils.getBedwarsStatus() != 2 ? false : this.hostileMobs.contains(entityCreature);
      }

      if (Utils.getBedwarsStatus() != 2) {
         return true;
      }

      if (this.golems.containsKey(entityCreature.getEntityId())) {
         return !this.golems.getOrDefault(entityCreature.getEntityId(), false);
      }

      double nearestDistance = -1.0;
      EntityArmorStand nearestArmorStand = null;

      for (Entity entity : mc.theWorld.loadedEntityList) {
         if (entity instanceof EntityArmorStand) {
            String stripped = Utils.stripString(entity.getDisplayName().getFormattedText());
            if (stripped.contains("[") && stripped.endsWith("]")) {
               double distanceSq = entity.getDistanceSq(entityCreature.posX, entityCreature.posY, entityCreature.posZ);
               if (distanceSq < nearestDistance || nearestDistance == -1.0) {
                  nearestDistance = distanceSq;
                  nearestArmorStand = (EntityArmorStand)entity;
               }
            }
         }
      }

      if (nearestArmorStand != null) {
         String teamColor = Utils.getFirstColorCode(nearestArmorStand.getDisplayName().getFormattedText());
         String teamColorSelf = Utils.getFirstColorCode(mc.thePlayer.getDisplayName().getFormattedText());
         boolean isTeam = false;
         if (!teamColor.isEmpty() && (teamColorSelf.equals(teamColor) || Utils.isTeammate(nearestArmorStand))) {
            isTeam = true;
         }

         this.golems.put(entityCreature.getEntityId(), isTeam);
         return !isTeam;
      } else {
         return !ModuleManager.bedwars.spawnedMobs.contains(entityCreature.getEntityId());
      }
   }

   private double getAPSToTicks(double cap) {
      double apsv = this.aps.getInput();
      if (apsv > cap) {
         apsv = cap;
      }

      if (apsv >= 20.0) {
         return 0.0;
      } else if (apsv >= 16.0) {
         return (int)Utils.randomizeDouble(0.0, 1.0);
      } else if (apsv >= 15.0) {
         return 1.0;
      } else if (apsv >= 11.0) {
         return (int)Utils.randomizeDouble(1.0, 2.0);
      } else if (apsv >= 10.0) {
         return 2.0;
      } else if (apsv >= 8.0) {
         return (int)Utils.randomizeDouble(2.0, 3.0);
      } else if (apsv == 7.0) {
         return 3.0;
      } else if (apsv >= 6.0) {
         return (int)Utils.randomizeDouble(3.0, 4.0);
      } else if (apsv >= 5.0) {
         return 4.0;
      } else if (apsv >= 4.0) {
         return 5.0;
      } else if (apsv >= 3.0) {
         return (int)Utils.randomizeDouble(6.0, 7.0);
      } else if (apsv >= 2.0) {
         return 10.0;
      } else if (apsv >= 1.0) {
         return 20.0;
      } else {
         return apsv >= 0.0 ? -1.0 : -1.0;
      }
   }

   private void handleBlocking() {
      if (Utils.holdingSword()) {
         if (this.autoBlockMode.getInput() != this.previousAutoBlockMode && this.previousAutoBlockMode > 0) {
            this.resetAutoblock(true);
         }

         this.previousAutoBlockMode = (int)this.autoBlockMode.getInput();
         if (this.t && this.hasAutoblocked) {
            ReflectionUtils.setItemInUse(this.blockingClient);
         }
      }
   }

   private double getMaxRange() {
      return Math.max(Math.max(this.swingRange.getInput(), this.attackRange.getInput() - this.attackv), this.blockRange.getInput());
   }

   public boolean autoBlockOverride() {
      return this.autoBlockMode.getInput() > 0.0 && Utils.holdingSword() && this.manualBlock();
   }

   private float unwrapYaw(float yaw, float prevYaw) {
      return prevYaw + (((yaw - prevYaw + 180.0F) % 360.0F + 360.0F) % 360.0F - 180.0F);
   }

   private boolean isLookingAtEntity() {
      return this.rotationMode.getInput() == 0.0 && this.rotationSmoothing.getInput() > 0.0
         ? RotationUtils.isPossibleToHit(attackingEntity, this.attackRange.getInput() - this.attackv, RotationUtils.serverRotations)
         : true;
   }

   private void handleAutoBlock(double distance) {
      boolean inAttackDistance = this.inRange(target, this.attackRange.getInput() - this.attackv);
      if (inAttackDistance) {
         attackingEntity = target;
      }

      this.dst2 = distance;
      boolean swung = false;
      if (!this.hasAutoblocked) {
         this.hasAutoblocked = true;
         if ((this.autoBlockMode.getInput() == 3.0 || this.autoBlockMode.getInput() == 6.0) && ModuleUtils.swapTick > 0) {
            return;
         }
      }

      switch ((int)this.autoBlockMode.getInput()) {
         case 1:
            this.blockingClient = true;
            this.interactTicks++;
            if (!this.hasBlocked) {
               this.handleInteractAndAttack(distance, true, true, swung);
               this.sendBlockPacket();
               this.interactTicks = 0;
            } else if (this.interactTicks >= this.getAPSToTicks(20.0)) {
               this.handleInteractAndAttack(distance, true, true, swung);
               this.interactTicks = 0;
            }
            break;
         case 2:
            if (this.interactTicks == 0) {
               this.getTicks = (int)this.getAPSToTicks(10.0);
            }

            this.interactTicks++;
            if (this.interactTicks == 1 && ModuleUtils.isBlocked) {
               this.sendDigPacket();
               this.blockingClient = false;
            }

            if (this.interactTicks == 2) {
               this.handleInteractAndAttack(distance, true, true, swung);
               this.sendBlockPacket();
               this.blockingClient = true;
            }

            if (this.interactTicks >= this.getTicks) {
               this.interactTicks = 0;
            }
            break;
         case 3:
            this.blockingClient = true;
            if (this.interactTicks == 0) {
               this.getTicks = (int)this.getAPSToTicks(10.0);
            }

            this.interactTicks++;
            if (this.interactTicks == 1 && ModuleUtils.isBlocked) {
               this.sendDigPacket();
            }

            if (this.interactTicks == 2) {
               this.handleInteractAndAttack(distance, true, true, swung);
               this.sendBlockPacket();
               BlinkHandler.release();
               this.blink = true;
            }

            if (this.interactTicks >= this.getTicks) {
               this.interactTicks = 0;
            }
            break;
         case 4:
            this.blockingClient = true;
            if (this.interactTicks == 0) {
               this.getTicks = (int)this.getAPSToTicks(10.0);
            }

            this.interactTicks++;
            if (this.interactTicks == 1 && ModuleUtils.isBlocked) {
               this.setSwapSlot();
            }

            if (this.interactTicks == 2) {
               this.setCurrentSlot();
               this.handleInteractAndAttack(distance, true, true, swung);
               this.sendBlockPacket();
               BlinkHandler.release();
               this.blink = true;
            }

            if (this.getTicks >= 10) {
               if (this.interactTicks >= 2 && this.cycle <= 3 || this.interactTicks >= 3) {
                  this.cycle++;
                  if (this.cycle >= 5) {
                     this.cycle = 0;
                  }

                  this.interactTicks = 0;
               }
            } else if (this.interactTicks >= this.getTicks) {
               this.interactTicks = 0;
            }
            break;
         case 5:
            this.blockingClient = true;
            if (this.interactTicks == 0) {
               this.getTicks = (int)this.getAPSToTicks(20.0);
            }

            this.interactTicks++;
            if (this.getTicks >= 11) {
               if (this.interactTicks <= 1 && this.cycle == 0 || this.interactTicks <= 4 && this.cycle >= 1) {
                  if (ModuleUtils.isBlocked) {
                     this.setSwapSlot();
                     this.setCurrentSlot();
                  }

                  this.handleInteractAndAttack(distance, true, true, swung);
                  this.sendBlockPacket();
               } else {
                  this.interactTicks = 0;
                  this.cycle++;
                  if (this.cycle > 1) {
                     this.cycle = 0;
                  }
               }
            } else {
               if (this.interactTicks == 1) {
                  if (ModuleUtils.isBlocked) {
                     this.setSwapSlot();
                     this.setCurrentSlot();
                  }

                  this.handleInteractAndAttack(distance, true, true, swung);
                  this.sendBlockPacket();
               }

               if (this.interactTicks >= this.getTicks) {
                  this.interactTicks = 0;
               }
            }
            break;
         case 6:
            this.blockingClient = true;
            if (this.interactTicks == 0) {
               this.getTicks = (int)this.getAPSToTicks(10.0);
            }

            this.interactTicks++;
            if (this.interactTicks == 1 && ModuleUtils.isBlocked) {
               this.sendDigPacket();
            }

            if (!inAttackDistance) {
               if (this.interactTicks >= 6) {
                  this.interactTicks = 0;
                  this.handleInteractAndAttack(distance, true, true, swung);
                  this.sendBlockPacket();
                  BlinkHandler.release();
                  this.blink = true;
               }
            } else {
               if (this.getTicks < 5) {
                  if (this.interactTicks >= 2 && this.cycle >= 1) {
                     this.cycle = 0;
                     this.interactTicks = 0;
                     this.handleInteractAndAttack(distance, true, true, swung);
                     this.sendBlockPacket();
                     BlinkHandler.release();
                     this.blink = true;
                  }

                  if (this.cycle == 0) {
                     if (this.interactTicks == 2) {
                        this.handleInteractAndAttack(distance, true, true, swung);
                     }

                     if (this.interactTicks >= 4) {
                        this.cycle = 1;
                        this.interactTicks = 0;
                        this.sendBlockPacket();
                        BlinkHandler.release();
                        this.blink = true;
                     }
                  }
               }

               if (this.getTicks >= 5) {
                  if (this.interactTicks == 2) {
                     this.handleInteractAndAttack(distance, true, true, swung);
                  }

                  if (this.interactTicks >= this.getTicks) {
                     this.interactTicks = 0;
                     this.sendBlockPacket();
                     BlinkHandler.release();
                     this.blink = true;
                  }
               }
            }
            break;
         case 7:
            this.blockingClient = true;
            if (this.interactTicks == 0) {
               this.getTicks = (int)this.getAPSToTicks(20.0);
            }

            this.blink = true;
            this.interactTicks++;
            this.sendBlockPacket();
            BlinkHandler.release();
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      boolean swung = false;
      if (this.hb2) {
         if (this.hasAutoblocked || this.autoBlockMode.getInput() != 3.0 && this.autoBlockMode.getInput() != 6.0 || ModuleUtils.swapTick <= 0) {
            switch ((int)this.autoBlockMode.getInput()) {
               case 7:
                  this.blink = true;
                  if (!this.attacked) {
                     this.handleInteractAndAttack(this.dst2, true, true, swung);
                     this.attacked = true;
                  }

                  if (this.interactTicks >= this.getTicks) {
                     this.interactTicks = 0;
                     this.attacked = false;
                  }
               case 1:
            }
         }
      }
   }

   private void resetYaw(ClientRotationEvent event) {
      this.reset = true;
      event.yaw = RotationUtils.serverRotations[0];
      event.pitch = RotationUtils.serverRotations[1];
      this.rotated = false;
   }

   private void srb() {
      if (this.blockingClient) {
         ReflectionUtils.setItemInUse(this.blockingClient = false);
      }
   }

   private boolean basicCondition() {
      return !Utils.nullCheck() ? false : !mc.thePlayer.isDead;
   }

   private void setCurrentSlot() {
      if (this.swapped) {
         mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
         keystrokesmod.Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
         this.swapped = false;
      }
   }

   private void setSwapSlot() {
      int bestSwapSlot = this.getBestSwapSlot();
      mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
      keystrokesmod.Raven.packetsHandler.playerSlot.set(bestSwapSlot);
      this.swapped = true;
      this.blockingServer = false;
      this.hasBlocked = false;
   }

   private void sendDigPacket() {
      if (Utils.holdingSword()) {
         mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
         this.blockingServer = false;
         this.hasBlocked = false;
      }
   }

   private void sendBlockPacket() {
      mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
      this.blockingServer = true;
      this.hasBlocked = true;
   }

   private boolean settingCondition() {
      if (this.requireMouseDown.isToggled() && !Mouse.isButtonDown(0)) {
         return false;
      } else if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
         return false;
      } else if (this.disableWhileMining.isToggled() && Utils.isMining()) {
         return false;
      } else {
         return this.disableInInventory.isToggled() && mc.currentScreen != null
            ? false
            : ModuleManager.bedAura == null
               || !ModuleManager.bedAura.isEnabled()
               || ModuleManager.bedAura.allowAura.isToggled()
               || ModuleManager.bedAura.currentBlock == null;
      }
   }

   private void setKeyBindState(int keycode, boolean state, boolean invokeTick) {
      KeyBinding.setKeyBindState(keycode, state);
      if (invokeTick) {
         KeyBinding.onTick(keycode);
      }
   }

   private void updateAttackDelay() {
      this.delay = (long)(1000.0 / this.aps.getInput() + Utils.randomizeInt(-4, 4));
   }

   private void swingItem() {
      if (this.silentSwing.isToggled() && mc.thePlayer.isBlocking()) {
         mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
      } else {
         mc.thePlayer.swingItem();
      }
   }

   public static double getDistanceToBoundingBox(Entity target) {
      if (mc.thePlayer == null) {
         return 0.0;
      }

      Vec3 playerEyePos = mc.thePlayer.getPositionEyes(((IAccessorMinecraft)mc).getTimer().renderPartialTicks);
      AxisAlignedBB boundingBox = target.getEntityBoundingBox();
      double nearestX = MathHelper.clamp_double(playerEyePos.xCoord, boundingBox.minX, boundingBox.maxX);
      double nearestY = MathHelper.clamp_double(playerEyePos.yCoord, boundingBox.minY, boundingBox.maxY);
      double nearestZ = MathHelper.clamp_double(playerEyePos.zCoord, boundingBox.minZ, boundingBox.maxZ);
      Vec3 nearestPoint = new Vec3(nearestX, nearestY, nearestZ);
      return playerEyePos.distanceTo(nearestPoint);
   }

   private int getBestSwapSlot() {
      int currentSlot = mc.thePlayer.inventory.currentItem;
      int bestSlot = -1;
      double bestDamage = -1.0;

      for (int i = 0; i < 9; i++) {
         if (i != currentSlot) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            double damage = Utils.getDamageLevel(stack);
            if (damage != 0.0 && damage > bestDamage) {
               bestDamage = damage;
               bestSlot = i;
            }
         }
      }

      if (bestSlot == -1) {
         for (int i = 0; i < 9; i++) {
            if (i != currentSlot) {
               ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
               if (stack == null || Arrays.stream(this.swapBlacklist).noneMatch(stack.getUnlocalizedName().toLowerCase()::contains)) {
                  bestSlot = i;
                  break;
               }
            }
         }
      }

      return bestSlot;
   }

   private int getNextSlot() {
      int currentSlot = mc.thePlayer.inventory.currentItem;
      int next = -1;
      if (currentSlot < 8) {
         next = currentSlot + 1;
      } else {
         next = currentSlot - 1;
      }

      return next;
   }

   public void resetYaw() {
      float serverYaw = RotationUtils.serverRotations[0];
      float unwrappedYaw = this.unwrapYaw(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw), serverYaw);
      mc.thePlayer.rotationYaw = unwrappedYaw;
      mc.thePlayer.prevRotationYaw = unwrappedYaw;
   }

   private void interactAt(boolean interactAt, boolean interact, boolean noEvent, boolean requireInteractAt) {
      if (attackingEntity != null) {
         if (!ModuleManager.bedAura.breakTick) {
            boolean sent = false;
            if (interactAt) {
               boolean canHit = RotationUtils.isPossibleToHit(attackingEntity, this.attackRange.getInput() - this.attackv, RotationUtils.serverRotations);
               if (!canHit) {
                  return;
               }

               MovingObjectPosition mov = RotationUtils.rayTrace(
                  10.0,
                  ((IAccessorMinecraft)mc).getTimer().renderPartialTicks,
                  RotationUtils.serverRotations,
                  this.hitThroughBlocks.isToggled() ? attackingEntity : null
               );
               if (mov != null && mov.typeOfHit == MovingObjectType.ENTITY && mov.entityHit == attackingEntity) {
                  Vec3 hitVec = mov.hitVec;
                  hitVec = new Vec3(
                     hitVec.xCoord - attackingEntity.posX,
                     hitVec.yCoord - attackingEntity.posY,
                     hitVec.zCoord - attackingEntity.posZ
                  );
                  if (!noEvent) {
                     mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(attackingEntity, hitVec));
                  } else {
                     PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(attackingEntity, hitVec));
                  }

                  sent = true;
               }
            }

            if (!requireInteractAt || sent) {
               if (interact) {
                  if (!noEvent) {
                     mc.thePlayer
                        .sendQueue
                        .addToSendQueue(new C02PacketUseEntity(attackingEntity, net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT));
                  } else {
                     PacketUtils.sendPacketNoEvent(
                        new C02PacketUseEntity(attackingEntity, net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT)
                     );
                  }
               }
            }
         }
      }
   }

   private float[] getRotationsSmoothed(float[] rotations) {
      float serverYaw = RotationUtils.serverRotations[0];
      float serverPitch = RotationUtils.serverRotations[1];
      float unwrappedYaw = this.unwrapYaw(rotations[0], serverYaw);
      float deltaYaw = unwrappedYaw - serverYaw;
      float deltaPitch = rotations[1] - serverPitch;
      float yawSmoothing = (float)this.rotationSmoothing.getInput();
      float pitchSmoothing = yawSmoothing;
      float strafe = mc.thePlayer.moveStrafing;
      if (strafe < 0.0F && deltaYaw < 0.0F || strafe > 0.0F && deltaYaw > 0.0F) {
         yawSmoothing = Math.max(1.0F, yawSmoothing / 2.0F);
      }

      float motionY = (float)mc.thePlayer.motionY;
      if (motionY > 0.0F && deltaPitch > 0.0F || motionY < 0.0F && deltaPitch < 0.0F) {
         pitchSmoothing = Math.max(1.0F, pitchSmoothing / 2.0F);
      }

      serverYaw += deltaYaw / Math.max(1.0F, yawSmoothing);
      serverPitch += deltaPitch / Math.max(1.0F, pitchSmoothing);
      return new float[]{serverYaw, serverPitch};
   }

   private void handleInteractAndAttack(double distance, boolean interactAt, boolean interact, boolean swung) {
      if (ModuleManager.antiFireball != null
         && ModuleManager.antiFireball.isEnabled()
         && ModuleManager.antiFireball.fireball != null
         && ModuleManager.antiFireball.attack) {
         if (ModuleManager.bedAura.breakTick) {
            return;
         }

         if (!ModuleManager.antiFireball.silentSwing.isToggled()) {
            mc.thePlayer.swingItem();
         } else {
            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
         }

         mc.playerController.attackEntity(mc.thePlayer, ModuleManager.antiFireball.fireball);
         if (interact) {
            mc.thePlayer
               .sendQueue
               .addToSendQueue(new C02PacketUseEntity(ModuleManager.antiFireball.fireball, net.minecraft.network.play.client.C02PacketUseEntity.Action.INTERACT));
         }
      } else {
         this.handleSwingAndAttack(distance, swung);
         this.interactAt(interactAt, interact, false, false);
      }
   }

   public void resetAutoblock(boolean unblock) {
      if (this.hasAutoblocked) {
         this.blink = false;
         if (keystrokesmod.Raven.packetsHandler.playerSlot.get() != mc.thePlayer.inventory.currentItem && this.swapped) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            keystrokesmod.Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
         } else if (!ModuleManager.scaffold.isEnabled && ModuleUtils.isBlocked) {
            this.sendUnBlock = unblock;
         }

         this.swapped = false;
         this.interactTicks = 0;
         this.hasAutoblocked = false;
         this.hasBlocked = false;
      }
   }

   private boolean inRange(Entity target, double distance) {
      return RotationUtils.isPossibleToHit(target, distance, RotationUtils.getRotations(target));
   }

   private boolean manualBlock() {
      return (!this.manualBlock.isToggled() || Mouse.isButtonDown(1)) && Utils.holdingSword();
   }

   static class KillAuraTarget {
      double distance;
      float health;
      int hurttime;
      double yawDelta;
      int entityId;
      boolean isEnemy;

      public KillAuraTarget(double distance, float health, int hurttime, double yawDelta, int entityId, boolean isEnemy) {
         this.distance = distance;
         this.health = health;
         this.hurttime = hurttime;
         this.yawDelta = yawDelta;
         this.entityId = entityId;
         this.isEnemy = isEnemy;
      }
   }
}
