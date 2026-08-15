package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.Objects;
import keystrokesmod.event.PrePlayerInputEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class CTWFly extends Module {
   public static SliderSetting horizontalSpeed;
   private SliderSetting verticalSpeed;
   private SliderSetting maxFlyTicks;
   private boolean d;
   private boolean a = false;
   private boolean begin;
   private boolean placed;
   private int flyTicks;
   private int ticks321;
   private int percent;
   private int percentDisplay;
   private static int widthOffset = 55;
   private static String get321 = "Waiting for explosion...";
   private int color = new Color(0, 187, 255, 255).getRGB();

   public CTWFly() {
      super("CTW Fly", Module.category.minigames);
      this.registerSetting(new DescriptionSetting("Use TNT to fly"));
      this.registerSetting(new DescriptionSetting("(High speed values will dog)"));
      this.registerSetting(horizontalSpeed = new SliderSetting("Horizontal speed", 4.0, 1.0, 9.0, 0.1));
      this.registerSetting(this.verticalSpeed = new SliderSetting("Vertical speed", 2.0, 1.0, 9.0, 0.1));
      this.registerSetting(this.maxFlyTicks = new SliderSetting("Max fly ticks", 40.0, 1.0, 80.0, 1.0));
   }

   @Override
   public void onDisable() {
      if (this.begin || this.placed) {
         this.disabled();
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      Packet packet = e.getPacket();
      if (packet instanceof S29PacketSoundEffect) {
         S29PacketSoundEffect s29 = (S29PacketSoundEffect)packet;
         if (!Objects.equals(String.valueOf(s29.getSoundName()), "random.explode")) {
            return;
         }

         if (mc.thePlayer.getPosition().distanceSq(s29.getX(), s29.getY(), s29.getZ()) <= 30.0) {
            this.begin = true;
            this.placed = false;
            this.flyTicks = 0;
         }
      }
   }

   @SubscribeEvent
   public void onSendPacket(SendPacketEvent e) {
      if (Utils.nullCheck()) {
         if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && Utils.holdingTNT()) {
            this.placed = true;
         }
      }
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent e) {
      if (Utils.nullCheck()) {
         String stripped = Utils.stripColor(e.message.getUnformattedText());
         if (stripped.contains("You cannot place blocks here!") && this.placed) {
            this.disabled();
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onMoveInput(PrePlayerInputEvent e) {
      if (this.placed) {
         e.setForward(0.0F);
         e.setStrafe(0.0F);
         Utils.setSpeed(0.0);
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.placed) {
         if (this.ticks321 >= 45) {
            this.ticks321 = 0;
         }

         this.ticks321++;
      }

      if (this.begin) {
         this.d = mc.thePlayer.capabilities.isFlying;
         if (++this.flyTicks >= this.maxFlyTicks.getInput() + 1.0) {
            this.disabled();
         }

         double percentCalc = 1000.0 / this.maxFlyTicks.getInput();
         if (this.percent < 1000) {
            this.percent += (int)percentCalc;
         }

         this.percentDisplay = this.percent / 10;
         if (this.flyTicks >= this.maxFlyTicks.getInput()) {
            this.percentDisplay = 100;
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (Utils.nullCheck() && (this.begin || this.placed)) {
         if (ev.phase != Phase.END || mc.currentScreen == null) {
            this.color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
            if (!this.placed) {
               widthOffset = this.percentDisplay < 10 ? 8 : (this.percentDisplay < 100 ? 12 : 14);
            } else {
               switch (this.ticks321) {
                  case 15:
                     get321 = "Waiting for explosion.";
                     widthOffset = 51;
                     break;
                  case 30:
                     get321 = "Waiting for explosion..";
                     widthOffset = 53;
                     break;
                  case 45:
                     get321 = "Waiting for explosion...";
                     widthOffset = 55;
               }
            }

            String text = this.placed ? get321 : this.percentDisplay + "%";
            int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
            ScaledResolution scaledResolution = new ScaledResolution(mc);
            int[] display = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
            mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset, display[1] / 2 + 8, this.color, true);
         }
      }
   }

   @Override
   public void onUpdate() {
      if (this.begin) {
         if (mc.currentScreen == null) {
            if (Utils.jumpDown()) {
               mc.thePlayer.motionY = 0.3 * this.verticalSpeed.getInput();
            } else if (Utils.sneakDown()) {
               mc.thePlayer.motionY = -0.3 * this.verticalSpeed.getInput();
            } else {
               mc.thePlayer.motionY = 0.0;
            }
         } else {
            mc.thePlayer.motionY = 0.0;
         }

         mc.thePlayer.capabilities.setFlySpeed(0.2F);
         mc.thePlayer.capabilities.isFlying = true;
         setSpeed(0.85 * horizontalSpeed.getInput());
      }
   }

   public static void setSpeed(double n) {
      if (n == 0.0) {
         mc.thePlayer.motionZ = 0.0;
         mc.thePlayer.motionX = 0.0;
      } else {
         double n3 = mc.thePlayer.movementInput.moveForward;
         double n4 = mc.thePlayer.movementInput.moveStrafe;
         float rotationYaw = mc.thePlayer.rotationYaw;
         if (n3 == 0.0 && n4 == 0.0) {
            mc.thePlayer.motionZ = 0.0;
            mc.thePlayer.motionX = 0.0;
         } else {
            if (n3 != 0.0) {
               if (n4 > 0.0) {
                  rotationYaw += n3 > 0.0 ? -45 : 45;
               } else if (n4 < 0.0) {
                  rotationYaw += n3 > 0.0 ? 45 : -45;
               }

               n4 = 0.0;
               if (n3 > 0.0) {
                  n3 = 1.0;
               } else if (n3 < 0.0) {
                  n3 = -1.0;
               }
            }

            double radians = Math.toRadians(rotationYaw + 90.0F);
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);
            mc.thePlayer.motionX = n3 * n * cos + n4 * n * sin;
            mc.thePlayer.motionZ = n3 * n * sin - n4 * n * cos;
         }
      }
   }

   private void disabled() {
      this.begin = this.placed = false;
      if (mc.thePlayer.capabilities.allowFlying) {
         mc.thePlayer.capabilities.isFlying = this.d;
      } else {
         mc.thePlayer.capabilities.isFlying = false;
      }

      this.d = false;
      if (this.flyTicks > 0) {
         mc.thePlayer.motionX = 0.0;
         mc.thePlayer.motionY = 0.0;
         mc.thePlayer.motionZ = 0.0;
      }

      this.flyTicks = this.percent = this.ticks321 = 0;
      get321 = "Waiting for explosion...";
      widthOffset = 55;
   }
}
