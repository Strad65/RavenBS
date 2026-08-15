package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class SliderComponent extends Component {
   public SliderSetting sliderSetting;
   private ModuleComponent moduleComponent;
   public float o;
   public float x;
   private float y;
   private boolean heldDown = false;
   private double width;
   public float xOffset;
   public boolean renderLine;
   private double targetValue;
   private double displayedValue;
   private static final double SLIDER_SPEED = 0.6;

   public SliderComponent(SliderSetting sliderSetting, ModuleComponent moduleComponent, float o) {
      this.sliderSetting = sliderSetting;
      this.moduleComponent = moduleComponent;
      this.o = o;
      double initial = sliderSetting.getInput() == -1.0 && sliderSetting.canBeDisabled ? -1.0 : sliderSetting.getInput();
      this.targetValue = initial;
      this.displayedValue = initial;
      this.width = this.sliderSetting.getInput() == -1.0
         ? 0.0
         : (this.moduleComponent.categoryComponent.getWidth() - 8.0F)
            * (this.sliderSetting.getInput() - this.sliderSetting.getMin())
            / (this.sliderSetting.getMax() - this.sliderSetting.getMin());
   }

   @Override
   public void render() {
      RenderUtils.drawRoundedRectangle(
         this.moduleComponent.categoryComponent.getX() + 4.0F + this.xOffset / 2.0F,
         this.moduleComponent.categoryComponent.getY() + this.o + 11.0F,
         this.moduleComponent.categoryComponent.getX() + 4.0F + this.moduleComponent.categoryComponent.getWidth() - 8.0F,
         this.moduleComponent.categoryComponent.getY() + this.o + 15.0F,
         4.0F,
         -12302777
      );
      float left = this.moduleComponent.categoryComponent.getX() + 4.0F + this.xOffset / 2.0F;
      float right = (float)(left + this.width);
      if (right - left > 84.0F) {
         right = left + 84.0F;
      }

      RenderUtils.drawRoundedRectangle(
         left,
         this.moduleComponent.categoryComponent.getY() + this.o + 11.0F,
         right,
         this.moduleComponent.categoryComponent.getY() + this.o + 15.0F,
         4.0F,
         Color.getHSBColor((float)(System.currentTimeMillis() % 11000L) / 11000.0F, 0.75F, 0.9F).getRGB()
      );
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      double input = this.sliderSetting.getInput();
      String suffix = this.sliderSetting.getSuffix();
      String valueText;
      if (input == -1.0 && this.sliderSetting.canBeDisabled) {
         valueText = "§cDisabled";
         suffix = "";
      } else {
         if (input != 1.0
            && (suffix.equals(" second") || suffix.equals(" block") || suffix.equals(" tick"))
            && this.moduleComponent.mod.moduleCategory() != Module.category.scripts) {
            suffix = suffix + "s";
         }

         if (this.sliderSetting.isString) {
            int idx = (int)Math.round(input);
            idx = Math.max(0, Math.min(idx, this.sliderSetting.getOptions().length - 1));
            valueText = this.sliderSetting.getOptions()[idx];
         } else {
            valueText = Utils.asWholeNum(input);
         }
      }

      Minecraft.getMinecraft()
         .fontRendererObj
         .drawStringWithShadow(
            this.sliderSetting.getName() + ": " + (this.sliderSetting.isString ? "§e" : "§b") + valueText + suffix,
            (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset,
            (this.moduleComponent.categoryComponent.getY() + this.o + 3.0F) * 2.0F,
            -1
         );
      GL11.glPopMatrix();
   }

   @Override
   public void drawScreen(int mouseX, int mouseY) {
      this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
      this.x = this.moduleComponent.categoryComponent.getX();
      if (this.heldDown) {
         double d = Math.min(this.moduleComponent.categoryComponent.getWidth() - 8.0F, Math.max(0.0F, mouseX - this.x));
         if (d == 0.0 && this.sliderSetting.canBeDisabled) {
            this.targetValue = -1.0;
         } else {
            double n = roundToInterval(
               d / (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * (this.sliderSetting.getMax() - this.sliderSetting.getMin())
                  + this.sliderSetting.getMin(),
               4
            );
            this.targetValue = n;
         }

         this.displayedValue = this.displayedValue + (this.targetValue - this.displayedValue) * 0.6;
         if (this.targetValue == -1.0) {
            this.sliderSetting.setValueRaw(-1.0);
         } else {
            this.sliderSetting.setValue(this.targetValue);
         }

         if (this.displayedValue == -1.0) {
            this.width = 0.0;
         } else {
            double range = this.sliderSetting.getMax() - this.sliderSetting.getMin();
            double fraction = (this.displayedValue - this.sliderSetting.getMin()) / range;
            this.width = (this.moduleComponent.categoryComponent.getWidth() - 8.0F) * fraction;
         }

         if (this.sliderSetting.getInput() != this.sliderSetting.getMin()
            && ModuleManager.hud != null
            && ModuleManager.hud.isEnabled()
            && !ModuleManager.organizedModules.isEmpty()) {
            ModuleManager.sort();
         }

         if (keystrokesmod.Raven.currentProfile != null) {
            ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
         }
      }
   }

   public void onSliderChange() {
      double initial = this.sliderSetting.getInput() == -1.0 && this.sliderSetting.canBeDisabled ? -1.0 : this.sliderSetting.getInput();
      this.targetValue = initial;
      this.displayedValue = initial;
      this.width = this.sliderSetting.getInput() == -1.0
         ? 0.0
         : (this.moduleComponent.categoryComponent.getWidth() - 8.0F)
            * (this.sliderSetting.getInput() - this.sliderSetting.getMin())
            / (this.sliderSetting.getMax() - this.sliderSetting.getMin());
   }

   private static double roundToInterval(double value, int places) {
      if (places < 0) {
         return 0.0;
      }

      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(places, RoundingMode.HALF_UP);
      return bd.doubleValue();
   }

   @Override
   public boolean onClick(int mouseX, int mouseY, int button) {
      if ((this.u(mouseX, mouseY) || this.i(mouseX, mouseY)) && button == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
         this.heldDown = true;
      }

      return false;
   }

   @Override
   public void mouseReleased(int mouseX, int mouseY, int button) {
      this.heldDown = false;
   }

   public boolean u(int mouseX, int mouseY) {
      return mouseX > this.x && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth() / 2.0F + 1.0F && mouseY > this.y && mouseY < this.y + 16.0F;
   }

   public boolean i(int mouseX, int mouseY) {
      return mouseX > this.x + this.moduleComponent.categoryComponent.getWidth() / 2.0F
         && mouseX < this.x + this.moduleComponent.categoryComponent.getWidth()
         && mouseY > this.y
         && mouseY < this.y + 16.0F;
   }

   @Override
   public void onGuiClosed() {
      this.heldDown = false;
   }

   @Override
   public void updateHeight(float n) {
      this.o = n;
   }
}
