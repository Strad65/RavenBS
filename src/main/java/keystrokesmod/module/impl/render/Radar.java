package keystrokesmod.module.impl.render;

import java.awt.Color;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.opengl.GL11;

public class Radar extends Module {
   private ButtonSetting tracerLines;
   private int scale = 2;
   private int rectColor = new Color(0, 0, 0, 125).getRGB();

   public Radar() {
      super("Radar", Module.category.render);
      this.registerSetting(this.tracerLines = new ButtonSetting("Show tracer lines", false));
   }

   @Override
   public void onUpdate() {
      this.scale = new ScaledResolution(mc).getScaleFactor();
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent e) {
      if (e.phase == Phase.END && Utils.nullCheck()) {
         if (!(mc.currentScreen instanceof ClickGui)) {
            if (mc.currentScreen == null && !mc.gameSettings.showDebugInfo) {
               int n = 5;
               int n2 = 70;
               int n3 = 105;
               int n4 = 170;
               Gui.drawRect(5, 70, 105, 170, this.rectColor);
               Gui.drawRect(4, 69, 106, 70, -1);
               Gui.drawRect(4, 170, 106, 171, -1);
               Gui.drawRect(4, 70, 5, 170, -1);
               Gui.drawRect(105, 70, 106, 170, -1);
               RenderUtils.drawPolygon(55.0, 122.0, 5.0, 3, -1);
               GL11.glPushMatrix();
               GL11.glEnable(3089);
               GL11.glScissor(5 * this.scale, mc.displayHeight - this.scale * 170, 105 * this.scale - this.scale * 5, this.scale * 100);

               for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                  if (entityPlayer != mc.thePlayer && entityPlayer.deathTime == 0 && !AntiBot.isBot(entityPlayer)) {
                     double getDistanceSqToEntity = entityPlayer.getDistanceSqToEntity(mc.thePlayer);
                     if (!(getDistanceSqToEntity > 360.0)) {
                        double n5 = (
                              mc.thePlayer.rotationYaw
                                 + Math.atan2(
                                       entityPlayer.posX - mc.thePlayer.posX, entityPlayer.posZ - mc.thePlayer.posZ
                                    )
                                    * (float) (180.0 / Math.PI)
                           )
                           % 360.0;
                        double n6 = getDistanceSqToEntity / 5.0;
                        double n7 = n6 * Math.sin(Math.toRadians(n5));
                        double n8 = n6 * Math.cos(Math.toRadians(n5));
                        if (this.tracerLines.isToggled()) {
                           GL11.glPushMatrix();
                           GL11.glEnable(3042);
                           GL11.glEnable(2848);
                           GL11.glDisable(2929);
                           GL11.glDisable(3553);
                           GL11.glBlendFunc(770, 771);
                           GL11.glEnable(3042);
                           GL11.glLineWidth(0.5F);
                           GL11.glColor3d(1.0, 1.0, 1.0);
                           GL11.glBegin(2);
                           GL11.glVertex2d(55.0, 122.0);
                           GL11.glVertex2d(55.0 - n7, 122.0 - n8);
                           GL11.glEnd();
                           GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                           GL11.glDisable(3042);
                           GL11.glEnable(3553);
                           GL11.glEnable(2929);
                           GL11.glDisable(2848);
                           GL11.glDisable(3042);
                           GL11.glPopMatrix();
                        }

                        RenderUtils.drawPolygon(55.0 - n7, 122.0 - n8, 3.0, 4, Color.red.getRGB());
                     }
                  }
               }

               GL11.glDisable(3089);
               GL11.glPopMatrix();
            }
         }
      }
   }
}
