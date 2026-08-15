package keystrokesmod.module.impl.movement;

import java.awt.Color;
import java.util.ArrayList;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Teleport extends Module {
   private ButtonSetting rightClick;
   private ButtonSetting highlightTarget;
   private ButtonSetting highlightPath;
   private BlockPos targetPos;
   private ArrayList<Vec3> path = new ArrayList<>();

   public Teleport() {
      super("Teleport", Module.category.movement);
      this.registerSetting(this.rightClick = new ButtonSetting("Right click teleport", true));
      this.registerSetting(this.highlightTarget = new ButtonSetting("Highlight target", true));
      this.registerSetting(this.highlightPath = new ButtonSetting("Highlight path", false));
   }

   public void teleport(BlockPos targetBlock, boolean sendMessage) {
      targetBlock = targetBlock.up(1);
      ArrayList<Vec3> pathList = this.path = this.getPath(targetBlock);
      int packetsSent = 0;

      for (Vec3 pathPos : pathList) {
         mc.getNetHandler().addToSendQueue(new C04PacketPlayerPosition(pathPos.xCoord, pathPos.yCoord, pathPos.zCoord, true));
         if (++packetsSent >= 175) {
            if (sendMessage) {
               Utils.sendMessage("&eToo many packets, ending loop.");
            }
            break;
         }
      }

      mc.thePlayer.setPosition(targetBlock.getX(), targetBlock.getY(), targetBlock.getZ());
      if (sendMessage) {
         Utils.sendMessage(
            "&eTeleported to &d("
               + targetBlock.getX()
               + ", "
               + targetBlock.getY()
               + ", "
               + targetBlock.getZ()
               + ") &ewith &b"
               + packetsSent
               + " &epackets."
         );
      }
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (this.rightClick.isToggled() && this.highlightTarget.isToggled() && this.targetPos != null && Utils.nullCheck()) {
         RenderUtils.renderBlock(this.targetPos, Color.orange.getRGB(), true, true);
         if (this.highlightPath.isToggled()) {
            int positions = 0;

            for (Vec3 pos : this.path) {
               if (positions >= 175) {
                  break;
               }

               RenderUtils.renderBlock(new BlockPos(pos.xCoord, pos.yCoord, pos.zCoord), Color.yellow.getRGB(), false, true);
               positions++;
            }
         }
      }
   }

   private ArrayList getPath(BlockPos target) {
      ArrayList<Vec3> path = new ArrayList<>();
      double newX = target.getX() + 0.5;
      double newY = target.getY() + 1;
      double newZ = target.getZ() + 0.5;
      double distance = mc.thePlayer.getDistance(newX, newY, newZ);

      for (double d = 0.0; d < distance; d += 2.0) {
         path.add(
            new Vec3(
               mc.thePlayer.posX + (newX - mc.thePlayer.getHorizontalFacing().getFrontOffsetX() - mc.thePlayer.posX) * d / distance,
               mc.thePlayer.posY + (newY - mc.thePlayer.posY) * d / distance,
               mc.thePlayer.posZ + (newZ - mc.thePlayer.getHorizontalFacing().getFrontOffsetZ() - mc.thePlayer.posZ) * d / distance
            )
         );
      }

      return path;
   }

   @SubscribeEvent
   public void onMouse(MouseEvent mouseEvent) {
      if (mouseEvent.button == 1 && mouseEvent.buttonstate && this.rightClick.isToggled() && Utils.nullCheck()) {
         MovingObjectPosition rayCast = RotationUtils.rayCast(150.0, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true);
         if (rayCast != null && rayCast.typeOfHit == MovingObjectType.BLOCK) {
            BlockPos getBlockPos = rayCast.getBlockPos();
            this.targetPos = getBlockPos;
            this.teleport(getBlockPos, true);
         }
      }
   }

   @Override
   public void onEnable() {
      this.targetPos = null;
      this.path.clear();
      if (!this.rightClick.isToggled()) {
         MovingObjectPosition rayCast = RotationUtils.rayCast(150.0, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true);
         if (rayCast != null && rayCast.typeOfHit == MovingObjectType.BLOCK) {
            this.teleport(rayCast.getBlockPos(), true);
            this.disable();
         }
      }
   }
}
