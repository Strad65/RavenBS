package keystrokesmod.clickgui.components.impl;

import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.setting.impl.GroupSetting;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class GroupComponent extends Component {
   public GroupSetting setting;
   private ModuleComponent component;
   public float o;
   private float x;
   private float y;
   public boolean opened;

   public GroupComponent(GroupSetting setting, ModuleComponent moduleComponent, float o) {
      this.setting = setting;
      this.component = moduleComponent;
      this.o = o;
      this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
      this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
   }

   @Override
   public void render() {
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      float strX = (this.component.categoryComponent.getX() + 4.0F) * 2.0F + 1.0F;
      float strY = (this.component.categoryComponent.getY() + this.o + 4.0F) * 2.0F;
      if (this.opened) {
         this.drawString("[", strX, strY);
         int firstBracketWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth("[");
         int arrowWidth = Minecraft.getMinecraft().fontRendererObj.getCharWidth('>');
         int fontHeight = Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
         GL11.glPushMatrix();
         GL11.glTranslatef(strX, strY, 0.0F);
         float arrowX = firstBracketWidth - 2;
         GL11.glTranslatef(arrowX + arrowWidth / 2.0F, fontHeight / 2.0F, 0.0F);
         GL11.glRotatef(90.0F, 0.0F, 0.0F, 1.0F);
         GL11.glTranslatef(-(arrowWidth / 2.0F), -(fontHeight / 2.0F), 0.0F);
         this.drawString(">", 0.0F, 0.0F);
         GL11.glPopMatrix();
         this.drawString("]  " + this.setting.getName(), strX + firstBracketWidth + arrowWidth, strY);
      } else {
         this.drawString("[>]  " + this.setting.getName(), strX, strY);
      }

      GL11.glPopMatrix();
   }

   @Override
   public void updateHeight(float n) {
      this.o = n;
   }

   @Override
   public void drawScreen(int x, int y) {
      this.y = this.component.categoryComponent.getModuleY() + this.o;
      this.x = this.component.categoryComponent.getX();
   }

   @Override
   public boolean onClick(int x, int y, int b) {
      if (this.i(x, y) && b == 1 && this.component.isOpened) {
         this.opened = !this.opened;

         for (CategoryComponent categoryComponent : ClickGui.categories) {
            if (categoryComponent.category == this.component.mod.moduleCategory()) {
               for (ModuleComponent moduleComponent : categoryComponent.modules) {
                  if (moduleComponent.mod.getName().equals(this.component.mod.getName())) {
                     moduleComponent.updateSettingPositions(7);
                     break;
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean i(int x, int y) {
      return x > this.x && x < this.x + this.component.categoryComponent.getWidth() && y > this.y && y < this.y + 11.0F;
   }

   public void drawString(String text, float x, float y) {
      Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, -1, false);
   }
}
