package keystrokesmod.utility;

import java.awt.Color;
import java.util.concurrent.ConcurrentLinkedQueue;
import keystrokesmod.event.PostMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.movement.NoSlow;
import keystrokesmod.module.impl.render.HUD;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class BlinkHandler {
   private static Minecraft mc = null;
   private static ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();
   private static boolean active;
   public static int blinkTicks;
   private int color = new Color(0, 187, 255, 255).getRGB();
   private Vec3 bPos;
   public static boolean released;

   public BlinkHandler(Minecraft mc) {
      BlinkHandler.mc = mc;
   }

   public static boolean blinkModule() {
      if (ModuleManager.antiVoid != null && ModuleManager.antiVoid.isEnabled() && ModuleManager.antiVoid.blink) {
         return true;
      } else if (ModuleManager.blink != null && ModuleManager.blink.isEnabled() && ModuleManager.blink.blink) {
         return true;
      } else if (ModuleManager.noFall != null && ModuleManager.noFall.isEnabled() && ModuleManager.noFall.mode.getInput() == 5.0 && ModuleManager.noFall.blink) {
         return true;
      } else if (ModuleManager.noSlow != null && ModuleManager.noSlow.isEnabled() && NoSlow.mode.getInput() == 5.0 && ModuleManager.noSlow.blink) {
         return true;
      } else if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && ModuleManager.killAura.blink) {
         return true;
      } else if (ModuleManager.scaffold != null && ModuleManager.scaffold.isEnabled && ModuleManager.scaffold.blink) {
         return true;
      } else if (ModuleManager.tower != null && ModuleManager.tower.canTower() && ModuleManager.tower.blink) {
         return true;
      } else if (ModuleManager.velocity != null && ModuleManager.velocity.isEnabled() && ModuleManager.velocity.blink) {
         return true;
      } else {
         return ModuleManager.lagRange != null && ModuleManager.lagRange.isEnabled() && ModuleManager.lagRange.blink
            ? true
            : ModuleManager.momentum != null && ModuleManager.momentum.isEnabled() && ModuleManager.momentum.blink;
      }
   }

   private boolean renderTimer() {
      if (ModuleManager.antiVoid != null
         && ModuleManager.antiVoid.isEnabled()
         && ModuleManager.antiVoid.blink
         && ModuleManager.antiVoid.renderTimer.isToggled()) {
         return true;
      } else if (ModuleManager.blink != null && ModuleManager.blink.isEnabled() && ModuleManager.blink.blink && ModuleManager.blink.renderTimer.isToggled()) {
         return true;
      } else if (ModuleManager.noFall != null
         && ModuleManager.noFall.isEnabled()
         && ModuleManager.noFall.blink
         && ModuleManager.noFall.mode.getInput() == 5.0
         && ModuleManager.noFall.renderTimer.isToggled()) {
         return true;
      } else {
         return ModuleManager.noSlow != null
               && ModuleManager.noSlow.isEnabled()
               && ModuleManager.noSlow.blink
               && NoSlow.mode.getInput() == 5.0
               && ModuleManager.noSlow.renderTimer.isToggled()
            ? true
            : ModuleManager.momentum != null
               && ModuleManager.momentum.isEnabled()
               && ModuleManager.momentum.blink
               && ModuleManager.momentum.renderTimer.isToggled();
      }
   }

   private boolean renderBox() {
      return ModuleManager.blink != null && ModuleManager.blink.isEnabled() && ModuleManager.blink.blink && ModuleManager.blink.initialPosition.isToggled()
         ? true
         : ModuleManager.lagRange != null
            && ModuleManager.lagRange.isEnabled()
            && ModuleManager.lagRange.blink
            && ModuleManager.lagRange.initialPosition.isToggled();
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (Utils.nullCheck()) {
         if (!this.renderBox()) {
            this.bPos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
         }

         if (active) {
            blinkTicks++;
         }

         if (active && !blinkModule()) {
            release();
         }
      }
   }

   public static void release() {
      if (!blinkedPackets.isEmpty()) {
         if (ModuleManager.antiVoid.setPos) {
            PacketUtils.sendPacketNoEvent(
               new C06PacketPlayerPosLook(
                  mc.thePlayer.posX,
                  -0.55,
                  mc.thePlayer.posZ,
                  mc.thePlayer.rotationYaw,
                  mc.thePlayer.rotationPitch,
                  mc.thePlayer.onGround
               )
            );
            ModuleManager.antiVoid.setPos = false;
         }

         synchronized (blinkedPackets) {
            for (Packet packet : blinkedPackets) {
               keystrokesmod.Raven.packetsHandler.handlePacket(packet);
               PacketUtils.sendPacketNoEvent(packet);
            }
         }

         blinkedPackets.clear();
         blinkTicks = 0;
         released = true;
         if (!blinkModule()) {
            active = false;
         }
      }
   }

   @SubscribeEvent
   public void onPostMotion(PostMotionEvent e) {
      released = false;
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onSendPacket(SendPacketEvent e) {
      if (Utils.nullCheck()
         && !mc.isSingleplayer()
         && !e.isCanceled()
         && !e.getPacket().getClass().getSimpleName().startsWith("S")
         && !(e.getPacket() instanceof C00PacketLoginStart)
         && !(e.getPacket() instanceof C00Handshake)) {
         if (blinkModule()) {
            active = true;
         }

         if (active) {
            blinkedPackets.add(e.getPacket());
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (Utils.nullCheck() && blinkTicks != 0 && this.renderTimer() && blinkModule()) {
         if (ev.phase != Phase.END || mc.currentScreen == null) {
            this.ticksTimer(blinkTicks);
         }
      }
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (Utils.nullCheck() && this.bPos != null && this.renderBox() && blinkModule()) {
         this.drawBox(this.bPos);
      }
   }

   private void ticksTimer(int ticks) {
      this.color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
      int widthOffset = ticks < 10 ? 4 : (ticks >= 10 && ticks < 100 ? 7 : (ticks >= 100 && ticks < 1000 ? 10 : (ticks >= 1000 ? 13 : 16)));
      String text = "" + ticks;
      int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
      ScaledResolution scaledResolution = new ScaledResolution(mc);
      int[] display = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
      mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, this.color, true);
   }

   private void secondsTimer(int ticks) {
      this.color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
      ticks /= 20;
      int widthOffset = ticks < 10 ? 4 : (ticks >= 10 && ticks < 100 ? 7 : (ticks >= 100 && ticks < 1000 ? 10 : (ticks >= 1000 ? 13 : 16)));
      String text = ticks + "s";
      int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
      ScaledResolution scaledResolution = new ScaledResolution(mc);
      int[] display = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
      mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, this.color, true);
   }

   private void drawBox(Vec3 pos) {
      GlStateManager.pushMatrix();
      this.color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
      double x = pos.xCoord - mc.getRenderManager().viewerPosX;
      double y = pos.yCoord - mc.getRenderManager().viewerPosY;
      double z = pos.zCoord - mc.getRenderManager().viewerPosZ;
      AxisAlignedBB bbox = mc.thePlayer.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
      AxisAlignedBB axis = new AxisAlignedBB(
         bbox.minX - mc.thePlayer.posX + x,
         bbox.minY - mc.thePlayer.posY + y,
         bbox.minZ - mc.thePlayer.posZ + z,
         bbox.maxX - mc.thePlayer.posX + x,
         bbox.maxY - mc.thePlayer.posY + y,
         bbox.maxZ - mc.thePlayer.posZ + z
      );
      float a = (this.color >> 24 & 0xFF) / 255.0F;
      float r = (this.color >> 16 & 0xFF) / 255.0F;
      float g = (this.color >> 8 & 0xFF) / 255.0F;
      float b = (this.color & 0xFF) / 255.0F;
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3042);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      GL11.glLineWidth(2.0F);
      GL11.glColor4f(r, g, b, a);
      RenderUtils.drawBoundingBox(axis, r, g, b);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GlStateManager.popMatrix();
   }
}
