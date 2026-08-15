package keystrokesmod.clickgui.components.impl;

import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class BindComponent extends Component {
   public boolean isBinding;
   public ModuleComponent moduleComponent;
   public float o;
   public float x;
   private float y;
   public KeySetting keySetting;
   public float xOffset;

   public BindComponent(ModuleComponent moduleComponent, float o) {
      this.moduleComponent = moduleComponent;
      this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
      this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
      this.o = o;
   }

   public BindComponent(ModuleComponent moduleComponent, KeySetting keySetting, float o) {
      this.moduleComponent = moduleComponent;
      this.x = moduleComponent.categoryComponent.getX() + moduleComponent.categoryComponent.getWidth();
      this.y = moduleComponent.categoryComponent.getY() + moduleComponent.yPos;
      this.keySetting = keySetting;
      this.o = o;
   }

   @Override
   public void updateHeight(float n) {
      this.o = n;
   }

   @Override
   public void render() {
      GL11.glPushMatrix();
      GL11.glScaled(0.5, 0.5, 0.5);
      if (this.keySetting == null) {
         this.drawString(
            !this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.mod.script == null
               ? "Module cannot be bound."
               : (this.isBinding ? "Press a key..." : "Current bind: '§e" + this.getKeyAsStr(false) + "§r'")
         );
      } else {
         Minecraft.getMinecraft()
            .fontRendererObj
            .drawStringWithShadow(
               this.isBinding ? "Press a key..." : this.keySetting.getName() + ": '§e" + this.getKeyAsStr(true) + "§r'",
               (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset,
               (this.moduleComponent.categoryComponent.getY() + this.o + (this.keySetting == null ? 3 : 4)) * 2.0F,
               Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0)
            );
      }

      GL11.glPopMatrix();
   }

   @Override
   public void drawScreen(int x, int y) {
      this.y = this.moduleComponent.categoryComponent.getModuleY() + this.o;
      this.x = this.moduleComponent.categoryComponent.getX();
   }

   @Override
   public boolean onClick(int x, int y, int button) {
      if (this.overSetting(x, y) && this.moduleComponent.isOpened && this.moduleComponent.mod.canBeEnabled() && this.moduleComponent.isVisible(this)) {
         if (button == 0) {
            this.isBinding = !this.isBinding;
         } else if (button == 1 && this.moduleComponent.mod.moduleCategory() != Module.category.profiles && this.keySetting == null) {
            this.moduleComponent.mod.setHidden(!this.moduleComponent.mod.isHidden());
            if (keystrokesmod.Raven.currentProfile != null) {
               ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
            }
         } else if (button > 1 && this.isBinding) {
            if (this.keySetting != null) {
               this.keySetting.setKey(button + 1000);
            } else {
               this.moduleComponent.mod.setBind(button + 1000);
            }

            if (keystrokesmod.Raven.currentProfile != null) {
               ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
            }

            this.isBinding = false;
         }
      }

      return false;
   }

   @Override
   public void onScroll(int scroll) {
      if (this.isBinding && scroll != 0) {
         if (this.keySetting != null) {
            this.keySetting.setKey(scroll > 0 ? 1069 : 1070);
         } else {
            this.moduleComponent.mod.setBind(scroll > 0 ? 1069 : 1070);
         }

         if (keystrokesmod.Raven.currentProfile != null) {
            ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
         }

         this.isBinding = false;
      }
   }

   @Override
   public void keyTyped(char t, int keybind) {
      if (this.isBinding) {
         if (keybind != 11 && keybind != 1) {
            if (keystrokesmod.Raven.currentProfile != null) {
               ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
            }

            if (this.keySetting != null) {
               this.keySetting.setKey(keybind);
            } else {
               this.moduleComponent.mod.setBind(keybind);
            }
         } else {
            if (this.moduleComponent.mod instanceof Gui) {
               this.moduleComponent.mod.setBind(54);
            } else if (this.keySetting != null) {
               this.keySetting.setKey(0);
            } else {
               this.moduleComponent.mod.setBind(0);
            }

            if (keystrokesmod.Raven.currentProfile != null) {
               ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
            }
         }

         this.isBinding = false;
      }
   }

   public boolean overSetting(int x, int y) {
      return x > this.x && x < this.x + this.moduleComponent.categoryComponent.getWidth() && y > this.y - 1.0F && y < this.y + 12.0F;
   }

   public String getKeyAsStr(boolean isKey) {
      int key = isKey ? this.keySetting.getKey() : this.moduleComponent.mod.getKeycode();
      return key >= 1000 ? (key != 1069 && key != 1070 ? "M" + (key - 1000) : this.getScroll(key)) : Keyboard.getKeyName(key);
   }

   public String getScroll(int key) {
      if (key == 1069) {
         return "MScrollUp";
      } else {
         return key == 1070 ? "MScrollDown" : "&cERROR";
      }
   }

   @Override
   public int getHeight() {
      return this.keySetting != null ? 0 : 16;
   }

   private void drawString(String s) {
      Minecraft.getMinecraft()
         .fontRendererObj
         .drawStringWithShadow(
            s,
            (this.moduleComponent.categoryComponent.getX() + 4.0F) * 2.0F + this.xOffset,
            (this.moduleComponent.categoryComponent.getY() + this.o + (this.keySetting == null ? 3 : 4)) * 2.0F,
            !this.moduleComponent.mod.hidden
               ? Theme.getGradient(Theme.descriptor[0], Theme.descriptor[1], 0.0)
               : Theme.getGradient(Theme.hiddenBind[0], Theme.hiddenBind[1], 0.0)
         );
   }

   @Override
   public void onGuiClosed() {
      this.isBinding = false;
   }
}
