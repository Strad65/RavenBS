package keystrokesmod.module.impl.movement;

import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.player.Safewalk;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockSnow;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class Sprint extends Module {
   private ButtonSetting displayText;
   private ButtonSetting rainbow;
   public SliderSetting omniDirectional;
   private SliderSetting floatSetting;
   private ButtonSetting renderJumpRequired;
   public ButtonSetting disableBackwards;
   public String text = "[Sprint (Toggled)]";
   public float posX = 5.0F;
   public float posY = 5.0F;
   private float limit;
   public boolean canFloat;
   public boolean requireJump;
   public boolean sprintFloat;
   private int color = new Color(255, 0, 0, 255).getRGB();
   private String[] omniDirectionalModes = new String[]{"Disabled", "Vanilla", "Hypixel", "Float"};
   double[] floatSpeedLevels = new double[]{0.2, 0.22, 0.28, 0.29, 0.3};

   public Sprint() {
      super("Sprint", Module.category.movement, 0);
      this.registerSetting(new DescriptionSetting("Command: '§esprint [msg]§r'"));
      this.registerSetting(new ButtonSetting("Edit text position", () -> mc.displayGuiScreen(new Sprint.EditScreen())));
      this.registerSetting(this.displayText = new ButtonSetting("Display text", false));
      this.registerSetting(this.rainbow = new ButtonSetting("Rainbow", false));
      this.registerSetting(this.omniDirectional = new SliderSetting("Omni-Directional", 0, this.omniDirectionalModes));
      this.registerSetting(this.floatSetting = new SliderSetting("Float speed", "%", 100.0, 0.0, 100.0, 1.0));
      this.registerSetting(this.renderJumpRequired = new ButtonSetting("Render jump required", false));
      this.registerSetting(this.disableBackwards = new ButtonSetting("Disable backwards", false));
      this.closetModule = true;
   }

   @Override
   public void guiUpdate() {
      this.floatSetting.setVisible(this.omniDirectional.getInput() == 3.0, this);
      this.renderJumpRequired.setVisible(this.omniDirectional.getInput() == 3.0, this);
   }

   @SubscribeEvent
   public void onPostPlayerInput(PostPlayerInputEvent e) {
      if (Utils.jumpDown() && mc.thePlayer.onGround) {
         this.requireJump = true;
      }
   }

   @SubscribeEvent
   public void onPreMotion(PreMotionEvent e) {
      if (ModuleUtils.groundTicks <= 8 || this.floatConditions()) {
         this.canFloat = true;
      }

      if (!this.floatConditions()) {
         this.canFloat = false;
      }

      if (!mc.thePlayer.onGround) {
         this.requireJump = false;
      }

      if (this.canFloat && this.floatConditions() && !this.requireJump && this.omniSprint()) {
         e.setPosY(e.getPosY() + ModuleUtils.offsetValue);
         this.sprintFloat = true;
         ModuleUtils.groundTicks = 0;
         if (Utils.isMoving()) {
            Utils.setSpeed(this.getFloatSpeed(this.getSpeedLevel()));
         }
      } else {
         this.sprintFloat = false;
      }

      if (this.rotationConditions()) {
         float yaw = mc.thePlayer.rotationYaw;
         e.setYaw(yaw - 55.0F);
         RotationUtils.setFakeRotations(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
      }
   }

   boolean floatConditions() {
      int edgeY = (int)Math.round(mc.thePlayer.posY % 1.0 * 100.0);
      if (ModuleUtils.stillTicks > 200) {
         this.requireJump = true;
         return false;
      }

      if (edgeY >= 10 && !this.allowedBlocks()) {
         this.requireJump = true;
         return false;
      }

      if (Safewalk.canSafeWalk()) {
         this.requireJump = true;
         return false;
      }

      if (!ModuleManager.scaffold.isEnabled && !ModuleManager.bhop.isEnabled()) {
         if (ModuleManager.sprint.omniDirectional.getInput() != 3.0) {
            return false;
         } else if (!mc.thePlayer.onGround) {
            return false;
         } else if (Utils.jumpDown()) {
            return false;
         } else {
            return LongJump.function ? false : !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
         }
      } else {
         this.requireJump = true;
         return false;
      }
   }

   private boolean allowedBlocks() {
      Block block = BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
      return block instanceof BlockSnow ? true : block instanceof BlockCarpet;
   }

   private boolean rotationConditions() {
      if (Utils.noSlowingBackWithBow()) {
         ModuleManager.bhop.setRotation = false;
         return false;
      }

      if (this.omniDirectional.getInput() < 2.0) {
         return false;
      }

      if (!mc.thePlayer.onGround) {
         return false;
      }

      if (mc.thePlayer.moveForward >= 0.0F || mc.thePlayer.moveStrafing != 0.0F) {
         return false;
      }

      if (Utils.jumpDown()) {
         return false;
      }

      if (KillAura.attackingEntity != null) {
         return false;
      }

      if (Safewalk.canSafeWalk()) {
         return false;
      }

      if (!ModuleManager.scaffold.isEnabled && !ModuleManager.bhop.isEnabled()) {
         if (Utils.holdingFireball() && mc.thePlayer.moveStrafing == 0.0F && mc.thePlayer.moveForward <= -0.5) {
            return false;
         } else {
            return mc.thePlayer.getHeldItem() != null
                  && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock
                  && Mouse.isButtonDown(1)
                  && mc.thePlayer.moveStrafing == 0.0F
                  && mc.thePlayer.moveForward <= -0.8
               ? false
               : !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
         }
      } else {
         return false;
      }
   }

   public boolean disableBackwards() {
      this.limit = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - Utils.getLastReportedYaw());
      double limitVal = 145.0;
      if (!this.disableBackwards.isToggled()) {
         return false;
      } else if (this.exceptions()) {
         return false;
      } else {
         return this.limit <= -limitVal || this.limit >= limitVal
            ? true
            : this.omniSprint() && ModuleManager.killAura.rotating && mc.thePlayer.moveForward <= 0.5;
      }
   }

   @Override
   public void onUpdate() {
      if (Utils.nullCheck() && mc.inGameHasFocus) {
         KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent e) {
      if (e.phase == Phase.END && Utils.nullCheck()) {
         if (mc.currentScreen == null) {
            if (this.displayText.isToggled() && !mc.gameSettings.showDebugInfo) {
               mc.fontRendererObj.drawStringWithShadow(this.text, this.posX, this.posY, this.rainbow.isToggled() ? Utils.getChroma(2L, 0L) : -1);
            }

            if (this.omniDirectional.getInput() == 3.0
               && this.renderJumpRequired.isToggled()
               && this.requireJump
               && !ModuleManager.scaffold.isEnabled
               && !ModuleManager.bhop.isEnabled()) {
               String text = "§c[Sprint]: Jump required to re-activate float";
               int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
               ScaledResolution scaledResolution = new ScaledResolution(mc);
               int[] display = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
               mc.fontRendererObj.drawString(text, display[0] / 2 - width + 104, display[1] / 2 + 272, this.color, true);
            }
         }
      }
   }

   public boolean omniSprint() {
      if (!this.isEnabled()) {
         return false;
      } else if (Utils.safeWalkBackwards()) {
         return false;
      } else if (!Utils.isMoving()) {
         return false;
      } else if (mc.thePlayer.moveForward <= 0.5 && Utils.jumpDown()) {
         return false;
      } else if (Utils.noSlowingBackWithBow()) {
         return false;
      } else if (Utils.holdingFireball() && mc.thePlayer.moveStrafing == 0.0F && mc.thePlayer.moveForward <= -0.5) {
         return false;
      } else {
         return mc.thePlayer.getHeldItem() != null
               && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock
               && Mouse.isButtonDown(1)
               && mc.thePlayer.moveStrafing == 0.0F
               && mc.thePlayer.moveForward <= -0.8
            ? false
            : this.omniDirectional.getInput() > 0.0;
      }
   }

   double getFloatSpeed(int speedLevel) {
      double min = 0.0;
      if (mc.thePlayer.moveStrafing != 0.0F && mc.thePlayer.moveForward != 0.0F) {
         min = 0.003;
      }

      return speedLevel >= 0
         ? (this.floatSpeedLevels[speedLevel] - min) * (this.floatSetting.getInput() / 100.0)
         : (this.floatSpeedLevels[0] - min) * (this.floatSetting.getInput() / 100.0);
   }

   private int getSpeedLevel() {
      Iterator var1 = mc.thePlayer.getActivePotionEffects().iterator();
      if (var1.hasNext()) {
         PotionEffect potionEffect = (PotionEffect)var1.next();
         return potionEffect.getEffectName().equals("potion.moveSpeed") ? potionEffect.getAmplifier() + 1 : 0;
      } else {
         return 0;
      }
   }

   private boolean exceptions() {
      return ModuleManager.scaffold.isEnabled || mc.thePlayer.hurtTime > 0;
   }

   static class EditScreen extends GuiScreen {
      GuiButtonExt resetPosition;
      boolean d = false;
      int miX = 0;
      int miY = 0;
      int maX = 0;
      int maY = 0;
      float aX = 5.0F;
      float aY = 5.0F;
      int laX = 0;
      int laY = 0;
      int lmX = 0;
      int lmY = 0;
      int clickMinX = 0;

      public void initGui() {
         super.initGui();
         this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
         this.aX = ModuleManager.sprint.posX;
         this.aY = ModuleManager.sprint.posY;
      }

      public void drawScreen(int mX, int mY, float pt) {
         drawRect(0, 0, this.width, this.height, -1308622848);
         int miX = (int)this.aX;
         int miY = (int)this.aY;
         String text = ModuleManager.sprint.text;
         int maX = miX + this.mc.fontRendererObj.getStringWidth(text);
         int maY = miY + this.mc.fontRendererObj.FONT_HEIGHT;
         this.mc.fontRendererObj.drawStringWithShadow(text, this.aX, this.aY, -1);
         this.miX = miX;
         this.miY = miY;
         this.maX = maX;
         this.maY = maY;
         this.clickMinX = miX;
         ModuleManager.sprint.posX = miX;
         ModuleManager.sprint.posY = miY;
         ScaledResolution res = new ScaledResolution(this.mc);
         int x = res.getScaledWidth() / 2 - 84;
         int y = res.getScaledHeight() / 2 - 20;
         RenderUtils.drawColoredString("Edit the HUD position by dragging.", '-', x, y, 2L, 0L, true, this.mc.fontRendererObj);

         try {
            this.handleInput();
         } catch (IOException var13) {
         }

         super.drawScreen(mX, mY, pt);
      }

      protected void mouseClickMove(int mX, int mY, int b, long t) {
         super.mouseClickMove(mX, mY, b, t);
         if (b == 0) {
            if (this.d) {
               this.aX = this.laX + (mX - this.lmX);
               this.aY = this.laY + (mY - this.lmY);
            } else if (mX > this.clickMinX && mX < this.maX && mY > this.miY && mY < this.maY) {
               this.d = true;
               this.lmX = mX;
               this.lmY = mY;
               this.laX = (int)this.aX;
               this.laY = (int)this.aY;
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
            this.aX = ModuleManager.sprint.posX = 5.0F;
            this.aY = ModuleManager.sprint.posY = 5.0F;
         }
      }

      public boolean doesGuiPauseGame() {
         return false;
      }
   }
}
