package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class ButtonComponent extends Component {
   private final int enabledColor = new Color(20, 255, 0).getRGB();
   private Module mod;
   public ButtonSetting buttonSetting;
   private ModuleComponent moduleComponent;
   public float o;
   public float x;
   private float y;
   public float xOffset;
   public boolean renderLine;

   public ButtonComponent(Module mod, ButtonSetting op, ModuleComponent b, float o) {
      this.mod = mod;
      this.buttonSetting = op;
      this.moduleComponent = b;
      this.x = b.categoryComponent.getX() + b.categoryComponent.getWidth();
      this.y = b.categoryComponent.getY() + b.yPos;
      this.o = o;
   }

   @Override
   public void render() {
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      Minecraft.getMinecraft()
         .fontRendererObj
         .drawString(
            (this.buttonSetting.isMethodButton ? "[=]  " : (this.buttonSetting.isToggled() ? "[+]  " : "[-]  ")) + this.buttonSetting.getName(),
            (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset,
            (this.moduleComponent.categoryComponent.getY() + this.o + 4.0F) * 2.0F,
            this.buttonSetting.isToggled() ? this.enabledColor : -1,
            false
         );
      GL11.glScaled(1.0, 1.0, 1.0);
      if (this.renderLine) {
      }

      GL11.glPopMatrix();
   }

   @Override
   public void updateHeight(float n) {
      this.o = n;
   }

   @Override
   public void drawScreen(int x, int y) {
      this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
      this.x = this.moduleComponent.categoryComponent.getX();
   }

   @Override
   public boolean onClick(int x, int y, int b) {
      if (this.i(x, y) && b == 0 && this.moduleComponent.isOpened && this.moduleComponent.isVisible(this)) {
         if (this.buttonSetting.isMethodButton) {
            this.buttonSetting.runMethod();
            return false;
         }

         this.buttonSetting.toggle();
         this.mod.guiButtonToggled(this.buttonSetting);
         if (keystrokesmod.Raven.currentProfile != null) {
            ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
         }
      }

      return false;
   }

   public boolean i(int x, int y) {
      return x > this.x && x < this.x + this.moduleComponent.categoryComponent.getWidth() && y > this.y && y < this.y + 11.0F;
   }
}
