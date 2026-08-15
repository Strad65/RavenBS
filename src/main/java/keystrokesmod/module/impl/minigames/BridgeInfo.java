package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.io.IOException;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class BridgeInfo extends Module {
   private final int textRGB = new Color(0, 200, 200).getRGB();
   private final String bridge = "the brid";
   private final String start = "Defend!";
   private final String start2 = "Jump in to score!";
   private final String qt = "First player to score 5 goals wins";
   private final String enemyText = "Enemy: ";
   private final String distance = "Distance to goal: ";
   private final String enemyDistance = "Enemy distance to goal: ";
   private final String blocks = "Blocks: ";
   private static int hudX = 5;
   private static int hudY = 70;
   private String enemyName = "";
   private BlockPos g1p = null;
   private BlockPos g2p = null;
   private boolean q = false;
   private double d1 = 0.0;
   private double d2 = 0.0;
   private int blc = 0;

   public BridgeInfo() {
      super("Bridge Info", Module.category.minigames, 0);
      this.registerSetting(new DescriptionSetting(new String("Only for solos.")));
      this.registerSetting(new ButtonSetting("Edit position", () -> mc.displayGuiScreen(new BridgeInfo.EditScreen())));
   }

   @Override
   public void onDisable() {
      this.reset();
   }

   @Override
   public void onUpdate() {
      if (!this.enemyName.isEmpty() && this.isBridge()) {
         EntityPlayer enem = null;

         for (Entity e : mc.theWorld.loadedEntityList) {
            if (e instanceof EntityPlayer) {
               if (e.getName().equals(this.enemyName)) {
                  enem = (EntityPlayer)e;
               }
            } else if (e instanceof EntityArmorStand) {
               if (e.getName().contains("Defend!")) {
                  this.g1p = e.getPosition();
               } else if (e.getName().contains("Jump in to score!")) {
                  this.g2p = e.getPosition();
               }
            }
         }

         if (this.g1p != null && this.g2p != null) {
            this.d1 = Utils.round(mc.thePlayer.getDistance(this.g2p.getX(), this.g2p.getY(), this.g2p.getZ()) - 1.4, 1);
            if (this.d1 < 0.0) {
               this.d1 = 0.0;
            }

            this.d2 = enem == null
               ? 0.0
               : Utils.round(enem.getDistance(this.g1p.getX(), this.g1p.getY(), this.g1p.getZ()) - 1.4, 1);
            if (this.d2 < 0.0) {
               this.d2 = 0.0;
            }
         }

         int blc2 = 0;

         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && ((ItemBlock)stack.getItem()).block.equals(Blocks.stained_hardened_clay)) {
               blc2 += stack.stackSize;
            }
         }

         this.blc = blc2;
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (ev.phase == Phase.END && Utils.nullCheck() && this.isBridge()) {
         if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) {
            return;
         }

         mc.fontRendererObj.drawString("Enemy: " + this.enemyName, hudX, hudY, this.textRGB, true);
         mc.fontRendererObj.drawString("Distance to goal: " + this.d1, hudX, hudY + 11, this.textRGB, true);
         mc.fontRendererObj.drawString("Enemy distance to goal: " + this.d2, hudX, hudY + 22, this.textRGB, true);
         mc.fontRendererObj.drawString("Blocks: " + this.blc, hudX, hudY + 33, this.textRGB, true);
      }
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent c) {
      if (Utils.nullCheck()) {
         String s = Utils.stripColor(c.message.getUnformattedText());
         if (s.startsWith(" ")) {
            if (s.contains("First player to score 5 goals wins")) {
               this.q = true;
            } else if (this.q && s.contains("Opponent:")) {
               String n = s.split(":")[1].trim();
               if (n.contains("[")) {
                  n = n.split("] ")[1];
               }

               this.enemyName = n;
               this.q = false;
            }
         }
      }
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.reset();
      }
   }

   private boolean isBridge() {
      if (Utils.isHypixel()) {
         for (String s : Utils.getScoreBoardOld()) {
            String s2 = s.toLowerCase();
            if (s2.contains("mode") && s2.contains("the brid")) {
               return true;
            }
         }
      }

      return false;
   }

   private void reset() {
      this.enemyName = "";
      this.q = false;
      this.g1p = null;
      this.g2p = null;
      this.d1 = 0.0;
      this.d2 = 0.0;
      this.blc = 0;
   }

   private class EditScreen extends GuiScreen {
      String example = new String("Enemy: Player123-Distance to goal: 17.2-Enemy distance to goal: 16.3-Blocks: 98");
      GuiButtonExt resetPosition;
      boolean d = false;
      int miX = 0;
      int miY = 0;
      int maX = 0;
      int maY = 0;
      int aX = 5;
      int aY = 70;
      int laX = 0;
      int laY = 0;
      int lmX = 0;
      int lmY = 0;

      private EditScreen() {
      }

      public void initGui() {
         super.initGui();
         this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, 5, 85, 20, new String("Reset position")));
         this.aX = BridgeInfo.hudX;
         this.aY = BridgeInfo.hudY;
      }

      public void drawScreen(int mX, int mY, float pt) {
         drawRect(0, 0, this.width, this.height, -1308622848);
         int miX = this.aX;
         int miY = this.aY;
         int maX = miX + 140;
         int maY = miY + 41;
         this.d(this.mc.fontRendererObj, this.example);
         this.miX = miX;
         this.miY = miY;
         this.maX = maX;
         this.maY = maY;
         BridgeInfo.hudX = miX;
         BridgeInfo.hudY = miY;
         ScaledResolution res = new ScaledResolution(this.mc);
         int x = res.getScaledWidth() / 2 - 84;
         int y = res.getScaledHeight() / 2 - 20;
         RenderUtils.drawColoredString("Edit the HUD position by dragging.", '-', x, y, 2L, 0L, true, this.mc.fontRendererObj);

         try {
            this.handleInput();
         } catch (IOException var12) {
         }

         super.drawScreen(mX, mY, pt);
      }

      private void d(FontRenderer fr, String t) {
         int x = this.miX;
         int y = this.miY;

         for (String s : t.split("-")) {
            fr.drawString(s, x, y, BridgeInfo.this.textRGB, true);
            y += fr.FONT_HEIGHT + 2;
         }
      }

      protected void mouseClickMove(int mX, int mY, int b, long t) {
         super.mouseClickMove(mX, mY, b, t);
         if (b == 0) {
            if (this.d) {
               this.aX = this.laX + (mX - this.lmX);
               this.aY = this.laY + (mY - this.lmY);
            } else if (mX > this.miX && mX < this.maX && mY > this.miY && mY < this.maY) {
               this.d = true;
               this.lmX = mX;
               this.lmY = mY;
               this.laX = this.aX;
               this.laY = this.aY;
            }
         }
      }

      protected void mouseReleased(int mX, int mY, int s) {
         super.mouseReleased(mX, mY, s);
         if (s == 0) {
            this.d = false;
         }
      }

      public void actionPerformed(GuiButton b) {
         if (b == this.resetPosition) {
            this.aX = BridgeInfo.hudX = 5;
            this.aY = BridgeInfo.hudY = 70;
         }
      }

      public boolean doesGuiPauseGame() {
         return false;
      }
   }
}
