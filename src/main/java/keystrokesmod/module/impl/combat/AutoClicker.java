package keystrokesmod.module.impl.combat;

import java.util.Random;
import keystrokesmod.mixin.impl.accessor.IAccessorGuiScreen;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ReflectionUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AutoClicker extends Module {
   public SliderSetting minCPS;
   public SliderSetting maxCPS;
   public SliderSetting jitter;
   public SliderSetting blockHitChance;
   public static ButtonSetting leftClick;
   public ButtonSetting rightClick;
   public ButtonSetting breakBlocks;
   public ButtonSetting inventoryFill;
   public ButtonSetting weaponOnly;
   public ButtonSetting blocksOnly;
   public ButtonSetting disableCreative;
   private long nextReleaseTime;
   private long nextPressTime;
   private long nextMultiplierUpdateTime;
   private long nextExtraDelayUpdateTime;
   private double delayMultiplier;
   private boolean multiplierActive;
   private boolean isHoldingBlockBreak;
   private boolean isBlockHitActive;
   private Random rand = null;

   public AutoClicker() {
      super("AutoClicker", Module.category.combat, 0);
      this.registerSetting(new DescriptionSetting("Best with delay remover."));
      this.registerSetting(this.minCPS = new SliderSetting("Min CPS", 9.0, 1.0, 20.0, 0.5));
      this.registerSetting(this.maxCPS = new SliderSetting("Max CPS", 12.0, 1.0, 20.0, 0.5));
      this.registerSetting(this.jitter = new SliderSetting("Jitter", 0.0, 0.0, 3.0, 0.1));
      this.registerSetting(this.blockHitChance = new SliderSetting("Block hit chance", "%", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(leftClick = new ButtonSetting("Left click", true));
      this.registerSetting(this.rightClick = new ButtonSetting("Right click", false));
      this.registerSetting(this.breakBlocks = new ButtonSetting("Break blocks", false));
      this.registerSetting(this.inventoryFill = new ButtonSetting("Inventory fill", false));
      this.registerSetting(this.weaponOnly = new ButtonSetting("Weapon only", false));
      this.registerSetting(this.blocksOnly = new ButtonSetting("Blocks only", true));
      this.registerSetting(this.disableCreative = new ButtonSetting("Disable in creative", false));
      this.closetModule = true;
   }

   @Override
   public void onEnable() {
      this.isBlockHitActive = Mouse.isButtonDown(1);
      this.rand = new Random();
   }

   @Override
   public void onDisable() {
      this.nextReleaseTime = 0L;
      this.nextPressTime = 0L;
      this.isHoldingBlockBreak = false;
      this.isBlockHitActive = false;
   }

   @Override
   public void guiUpdate() {
      Utils.correctValue(this.minCPS, this.maxCPS);
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (ev.phase != Phase.END && Utils.nullCheck() && !Utils.isConsuming(mc.thePlayer)) {
         if (ModuleManager.killAura.isEnabled() && KillAura.target != null || ModuleManager.scaffold.isEnabled) {
            return;
         }

         if (this.disableCreative.isToggled() && mc.thePlayer.capabilities.isCreativeMode) {
            return;
         }

         if (mc.currentScreen == null && mc.inGameHasFocus) {
            if (this.weaponOnly.isToggled() && !Utils.holdingWeapon()) {
               return;
            }

            if (leftClick.isToggled() && Mouse.isButtonDown(0)) {
               this.performClick(mc.gameSettings.keyBindAttack.getKeyCode(), 0);
            } else if (this.rightClick.isToggled() && Mouse.isButtonDown(1)) {
               if (this.blocksOnly.isToggled()
                  && (mc.thePlayer.getCurrentEquippedItem() == null || !(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock))) {
                  return;
               }

               if (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBow) {
                  return;
               }

               this.performClick(mc.gameSettings.keyBindUseItem.getKeyCode(), 1);
            } else {
               this.nextReleaseTime = 0L;
               this.nextPressTime = 0L;
            }
         } else if (this.inventoryFill.isToggled() && mc.currentScreen instanceof GuiInventory) {
            if (Mouse.isButtonDown(0) && (Keyboard.isKeyDown(54) || Keyboard.isKeyDown(42))) {
               if (this.nextReleaseTime == 0L || this.nextPressTime == 0L) {
                  this.updateClickDelay();
               } else if (System.currentTimeMillis() > this.nextPressTime) {
                  this.updateClickDelay();
                  this.inventoryClick(mc.currentScreen);
               }
            } else {
               this.nextReleaseTime = 0L;
               this.nextPressTime = 0L;
            }
         }
      }
   }

   public void performClick(int key, int mouse) {
      if (this.breakBlocks.isToggled() && mouse == 0 && mc.objectMouseOver != null) {
         BlockPos pos = mc.objectMouseOver.getBlockPos();
         if (pos != null) {
            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (block != Blocks.air && !(block instanceof BlockLiquid)) {
               if (!this.isHoldingBlockBreak) {
                  KeyBinding.setKeyBindState(key, true);
                  KeyBinding.onTick(key);
                  this.isHoldingBlockBreak = true;
               }

               return;
            }

            if (this.isHoldingBlockBreak) {
               KeyBinding.setKeyBindState(key, false);
               this.isHoldingBlockBreak = false;
            }
         } else {
            this.isHoldingBlockBreak = false;
         }
      }

      if (this.jitter.getInput() > 0.0) {
         double jitterAmount = this.jitter.getInput() * 0.45;
         if (this.rand.nextBoolean()) {
            mc.thePlayer.rotationYaw = (float)(mc.thePlayer.rotationYaw + this.rand.nextFloat() * jitterAmount);
         } else {
            mc.thePlayer.rotationYaw = (float)(mc.thePlayer.rotationYaw - this.rand.nextFloat() * jitterAmount);
         }

         if (this.rand.nextBoolean()) {
            mc.thePlayer.rotationPitch = (float)(mc.thePlayer.rotationPitch + this.rand.nextFloat() * jitterAmount * 0.45);
         } else {
            mc.thePlayer.rotationPitch = (float)(mc.thePlayer.rotationPitch - this.rand.nextFloat() * jitterAmount * 0.45);
         }
      }

      if (this.nextPressTime > 0L && this.nextReleaseTime > 0L) {
         double blockHitC = this.blockHitChance.getInput();
         long currentTime = System.currentTimeMillis();
         if (currentTime > this.nextPressTime) {
            KeyBinding.setKeyBindState(key, true);
            KeyBinding.onTick(key);
            ReflectionUtils.setButton(mouse, true);
            if (mouse == 0 && blockHitC > 0.0 && Mouse.isButtonDown(1) && Math.random() >= (100.0 - blockHitC) / 100.0) {
               int useItemKey = mc.gameSettings.keyBindUseItem.getKeyCode();
               KeyBinding.setKeyBindState(useItemKey, true);
               KeyBinding.onTick(useItemKey);
               ReflectionUtils.setButton(1, true);
               this.isBlockHitActive = true;
            }

            this.updateClickDelay();
         } else if (currentTime > this.nextReleaseTime || this.isBlockHitActive) {
            KeyBinding.setKeyBindState(key, false);
            ReflectionUtils.setButton(mouse, false);
            if (mouse == 0 && blockHitC > 0.0) {
               KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
               ReflectionUtils.setButton(1, false);
               this.isBlockHitActive = false;
            }
         }
      } else {
         this.updateClickDelay();
      }
   }

   public void updateClickDelay() {
      double cps = Utils.getRandomValue(this.minCPS, this.maxCPS, this.rand) + 0.4 * this.rand.nextDouble();
      long delay = Math.round(1000.0 / cps);
      long currentTime = System.currentTimeMillis();
      if (currentTime > this.nextMultiplierUpdateTime) {
         if (!this.multiplierActive && this.rand.nextInt(100) >= 85) {
            this.multiplierActive = true;
            this.delayMultiplier = 1.1 + this.rand.nextDouble() * 0.15;
         } else {
            this.multiplierActive = false;
         }

         this.nextMultiplierUpdateTime = currentTime + 500L + this.rand.nextInt(1500);
      }

      if (currentTime > this.nextExtraDelayUpdateTime) {
         if (this.rand.nextInt(100) >= 80) {
            delay += 50L + this.rand.nextInt(100);
         }

         this.nextExtraDelayUpdateTime = currentTime + 500L + this.rand.nextInt(1500);
      }

      if (this.multiplierActive) {
         delay = (long)(delay * this.delayMultiplier);
      }

      this.nextPressTime = currentTime + delay;
      this.nextReleaseTime = currentTime + delay / 2L - this.rand.nextInt(10);
   }

   private void inventoryClick(GuiScreen screen) {
      int x = Mouse.getX() * screen.width / mc.displayWidth;
      int y = screen.height - Mouse.getY() * screen.height / mc.displayHeight - 1;
      ((IAccessorGuiScreen)screen).callMouseClicked(x, y, 0);
   }
}
