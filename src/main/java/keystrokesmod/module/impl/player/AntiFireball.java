package keystrokesmod.module.impl.player;

import java.util.HashSet;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class AntiFireball extends Module {
   private SliderSetting fov;
   private SliderSetting range;
   private ButtonSetting disableWhileFlying;
   private ButtonSetting disableWhileScaffold;
   private ButtonSetting blocksRotate;
   private ButtonSetting projectileRotate;
   public ButtonSetting silentSwing;
   public EntityFireball fireball;
   private HashSet<Entity> fireballs = new HashSet<>();
   public boolean attack;

   public AntiFireball() {
      super("AntiFireball", Module.category.player);
      this.registerSetting(this.fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
      this.registerSetting(this.range = new SliderSetting("Range", 8.0, 3.0, 15.0, 0.5));
      this.registerSetting(this.disableWhileFlying = new ButtonSetting("Disable while flying", false));
      this.registerSetting(this.disableWhileScaffold = new ButtonSetting("Disable while scaffold", false));
      this.registerSetting(this.blocksRotate = new ButtonSetting("Rotate with blocks", false));
      this.registerSetting(this.projectileRotate = new ButtonSetting("Rotate with projectiles", false));
      this.registerSetting(this.silentSwing = new ButtonSetting("Silent swing", false));
   }

   @SubscribeEvent(priority = EventPriority.LOW)
   public void onClientRotation(ClientRotationEvent e) {
      if (this.condition() && !this.stopAttack()) {
         if (this.fireball != null) {
            ItemStack getHeldItem = mc.thePlayer.getHeldItem();
            if (getHeldItem != null && getHeldItem.getItem() instanceof ItemBlock && !this.blocksRotate.isToggled() && Mouse.isButtonDown(1)) {
               return;
            }

            if (getHeldItem != null
               && (
                  getHeldItem.getItem() instanceof ItemBow
                     || getHeldItem.getItem() instanceof ItemSnowball
                     || getHeldItem.getItem() instanceof ItemEgg
                     || getHeldItem.getItem() instanceof ItemFishingRod
               )
               && !this.projectileRotate.isToggled()) {
               return;
            }

            if (ModuleManager.scaffold != null && ModuleManager.scaffold.stopRotation()) {
               return;
            }

            float[] rotations = RotationUtils.getRotations(this.fireball, RotationUtils.prevRenderYaw, RotationUtils.prevRenderPitch);
            e.setYaw(rotations[0]);
            e.setPitch(rotations[1]);
         }
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.condition() && !this.stopAttack()) {
         if (this.fireball != null) {
            if (ModuleManager.killAura != null
               && ModuleManager.killAura.isEnabled()
               && ModuleManager.killAura.blockingServer
               && ModuleManager.killAura.autoBlockOverride()) {
               if (KillAura.target != null) {
                  this.attack = false;
                  return;
               }

               this.attack = true;
            } else {
               Utils.attackEntity(this.fireball, !this.silentSwing.isToggled(), this.silentSwing.isToggled());
            }
         }
      }
   }

   private EntityFireball getFireball() {
      for (Entity entity : mc.theWorld.loadedEntityList) {
         if (entity instanceof EntityFireball
            && this.fireballs.contains(entity)
            && !(mc.thePlayer.getDistanceSqToEntity(entity) > this.range.getInput() * this.range.getInput())) {
            float n = (float)this.fov.getInput();
            if (n == 360.0F || Utils.inFov(n, entity)) {
               return (EntityFireball)entity;
            }
         }
      }

      return null;
   }

   @SubscribeEvent
   public void onEntityJoin(EntityJoinWorldEvent e) {
      if (Utils.nullCheck()) {
         if (e.entity == mc.thePlayer) {
            this.fireballs.clear();
         } else if (e.entity instanceof EntityFireball && mc.thePlayer.getDistanceSqToEntity(e.entity) > 16.0) {
            this.fireballs.add(e.entity);
         }
      }
   }

   @Override
   public void onDisable() {
      this.fireballs.clear();
      this.fireball = null;
      this.attack = false;
   }

   @Override
   public void onUpdate() {
      if (this.condition()) {
         if (mc.currentScreen != null) {
            this.attack = false;
            this.fireball = null;
         } else {
            this.fireball = this.getFireball();
         }
      }
   }

   private boolean stopAttack() {
      return ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.currentBlock != null
         || ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && KillAura.attackingEntity != null;
   }

   private boolean condition() {
      if (!Utils.nullCheck()) {
         return false;
      } else {
         return mc.thePlayer.capabilities.isFlying && this.disableWhileFlying.isToggled()
            ? false
            : ModuleManager.scaffold == null || !ModuleManager.scaffold.isEnabled() || !this.disableWhileScaffold.isToggled();
      }
   }
}
