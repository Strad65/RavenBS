package keystrokesmod.module.impl.player;

import java.awt.Color;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

public class Freecam extends Module {
   public SliderSetting speed;
   private ButtonSetting disableOnDamage;
   private ButtonSetting showArm;
   private ButtonSetting allowDigging;
   private ButtonSetting allowInteracting;
   private ButtonSetting allowPlacing;
   public static EntityOtherPlayerMP freeEntity = null;
   private int[] lcc = new int[]{Integer.MAX_VALUE, 0};
   private float[] sAng = new float[]{0.0F, 0.0F};

   public Freecam() {
      super("Freecam", Module.category.player, 0);
      this.registerSetting(this.speed = new SliderSetting("Speed", 2.5, 0.5, 10.0, 0.5));
      this.registerSetting(this.disableOnDamage = new ButtonSetting("Disable on damage", true));
      this.registerSetting(this.allowDigging = new ButtonSetting("Allow digging", false));
      this.registerSetting(this.allowInteracting = new ButtonSetting("Allow interacting", false));
      this.registerSetting(this.allowPlacing = new ButtonSetting("Allow placing", false));
      this.registerSetting(this.showArm = new ButtonSetting("Show arm", false));
   }

   @Override
   public void onEnable() {
      if (!mc.thePlayer.onGround) {
         this.disable();
      } else {
         freeEntity = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
         freeEntity.copyLocationAndAnglesFrom(mc.thePlayer);
         this.sAng[0] = freeEntity.rotationYawHead = mc.thePlayer.rotationYawHead;
         this.sAng[1] = mc.thePlayer.rotationPitch;
         freeEntity.setVelocity(0.0, 0.0, 0.0);
         freeEntity.setInvisible(true);
         mc.theWorld.addEntityToWorld(-8008, freeEntity);
         mc.setRenderViewEntity(freeEntity);
      }
   }

   @Override
   public void onDisable() {
      if (freeEntity != null) {
         mc.setRenderViewEntity(mc.thePlayer);
         mc.thePlayer.rotationYaw = mc.thePlayer.rotationYawHead = this.sAng[0];
         mc.thePlayer.rotationPitch = this.sAng[1];
         mc.theWorld.removeEntity(freeEntity);
         freeEntity = null;
      }

      this.lcc = new int[]{Integer.MAX_VALUE, 0};
      int x = mc.thePlayer.chunkCoordX;
      int z = mc.thePlayer.chunkCoordZ;

      for (int x2 = -1; x2 <= 1; x2++) {
         for (int z2 = -1; z2 <= 1; z2++) {
            int a = x + x2;
            int b = z + z2;
            mc.theWorld.markBlockRangeForRenderUpdate(a * 16, 0, b * 16, a * 16 + 15, 256, b * 16 + 15);
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.disableOnDamage.isToggled() && mc.thePlayer.hurtTime != 0) {
         this.disable();
      } else {
         mc.thePlayer.setSprinting(false);
         mc.thePlayer.moveForward = 0.0F;
         mc.thePlayer.moveStrafing = 0.0F;
         freeEntity.rotationYaw = freeEntity.rotationYawHead = mc.thePlayer.rotationYaw;
         freeEntity.rotationPitch = mc.thePlayer.rotationPitch;
         double s = 0.215 * this.speed.getInput();
         if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode())) {
            double rad = freeEntity.rotationYawHead * (Math.PI / 180.0);
            double dx = -1.0 * Math.sin(rad) * s;
            double dz = Math.cos(rad) * s;
            freeEntity.posX += dx;
            freeEntity.posZ += dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode())) {
            double rad = freeEntity.rotationYawHead * (Math.PI / 180.0);
            double dx = -1.0 * Math.sin(rad) * s;
            double dz = Math.cos(rad) * s;
            freeEntity.posX -= dx;
            freeEntity.posZ -= dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode())) {
            double rad = (freeEntity.rotationYawHead - 90.0F) * (Math.PI / 180.0);
            double dx = -1.0 * Math.sin(rad) * s;
            double dz = Math.cos(rad) * s;
            freeEntity.posX += dx;
            freeEntity.posZ += dz;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode())) {
            double rad = (freeEntity.rotationYawHead + 90.0F) * (Math.PI / 180.0);
            double dx = -1.0 * Math.sin(rad) * s;
            double dz = Math.cos(rad) * s;
            freeEntity.posX += dx;
            freeEntity.posZ += dz;
         }

         if (Utils.jumpDown()) {
            freeEntity.posY += 0.93 * s;
         }

         if (Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            freeEntity.posY -= 0.93 * s;
         }

         mc.thePlayer.setSneaking(false);
         if (this.lcc[0] != Integer.MAX_VALUE && (this.lcc[0] != freeEntity.chunkCoordX || this.lcc[1] != freeEntity.chunkCoordZ)) {
            int x = freeEntity.chunkCoordX;
            int z = freeEntity.chunkCoordZ;
            mc.theWorld.markBlockRangeForRenderUpdate(x * 16, 0, z * 16, x * 16 + 15, 256, z * 16 + 15);
         }

         this.lcc[0] = freeEntity.chunkCoordX;
         this.lcc[1] = freeEntity.chunkCoordZ;
      }
   }

   @SubscribeEvent
   public void re(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         if (!this.showArm.isToggled()) {
            mc.thePlayer.renderArmPitch = mc.thePlayer.prevRenderArmPitch = 700.0F;
         }

         RenderUtils.renderEntity(mc.thePlayer, 1, 0.0, 0.0, Color.green.getRGB(), false);
         RenderUtils.renderEntity(mc.thePlayer, 2, 0.0, 0.0, Color.green.getRGB(), false);
      }
   }

   @SubscribeEvent
   public void m(MouseEvent e) {
      if (Utils.nullCheck()) {
         if ((e.button == 0 && !this.allowDigging.isToggled() || e.button == 1 && !this.allowPlacing.isToggled())
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK) {
            e.setCanceled(true);
         }

         if (!this.allowInteracting.isToggled()
            && (e.button == 1 || e.button == 0)
            && mc.objectMouseOver != null
            && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public void onSendPacket(SendPacketEvent e) {
      if (Utils.nullCheck()) {
         if (!this.allowDigging.isToggled() && e.getPacket() instanceof C07PacketPlayerDigging) {
            e.setCanceled(true);
         }

         if (!this.allowPlacing.isToggled() && e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            e.setCanceled(true);
         }

         if (!this.allowInteracting.isToggled() && e.getPacket() instanceof C02PacketUseEntity) {
            e.setCanceled(true);
         }
      }
   }
}
