package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import java.util.ArrayList;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.KeySetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.Manager;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

public class ModuleComponent extends Component {
   private int originalHoverAlpha = 120;
   private final int hoverColor = new Color(0, 0, 0, this.originalHoverAlpha).getRGB();
   private final int unsavedColor = new Color(114, 188, 250).getRGB();
   private final int invalidColor = new Color(255, 80, 80).getRGB();
   private final int enabledColor = new Color(24, 154, 255).getRGB();
   private final int disabledColor = new Color(192, 192, 192).getRGB();
   public Module mod;
   public CategoryComponent categoryComponent;
   public float yPos;
   public ArrayList<Component> settings;
   public boolean isOpened;
   private boolean hovering;
   private Timer hoverTimer;
   private boolean hoverStarted;
   private Timer smoothTimer;
   private int smoothingY = 16;

   public ModuleComponent(Module mod, CategoryComponent p, float yPos) {
      this.mod = mod;
      this.categoryComponent = p;
      this.yPos = yPos;
      this.settings = new ArrayList<>();
      this.isOpened = false;
      float y = yPos + 12.0F;
      if (mod != null && !mod.getSettings().isEmpty()) {
         for (Setting v : mod.getSettings()) {
            if (v.visible) {
               if (v instanceof SliderSetting) {
                  SliderSetting n = (SliderSetting)v;
                  SliderComponent s = new SliderComponent(n, this, y);
                  this.settings.add(s);
                  y += 12.0F;
               } else if (v instanceof ButtonSetting) {
                  ButtonSetting b = (ButtonSetting)v;
                  ButtonComponent c = new ButtonComponent(mod, b, this, y);
                  this.settings.add(c);
                  y += 12.0F;
               } else if (v instanceof DescriptionSetting) {
                  DescriptionSetting d = (DescriptionSetting)v;
                  DescriptionComponent m = new DescriptionComponent(d, this, y);
                  this.settings.add(m);
                  y += 12.0F;
               } else if (v instanceof KeySetting) {
                  KeySetting setting = (KeySetting)v;
                  BindComponent keyComponent = new BindComponent(this, setting, y);
                  this.settings.add(keyComponent);
                  y += 12.0F;
               } else if (v instanceof GroupSetting) {
                  GroupSetting b = (GroupSetting)v;
                  GroupComponent c = new GroupComponent(b, this, y);
                  this.settings.add(c);
                  y += 12.0F;
               }
            }
         }
      }

      this.settings.add(new BindComponent(this, y));
   }

   @Override
   public void updateHeight(float newY) {
      this.yPos = newY;
      float y = this.yPos + 16.0F;

      for (Component co : this.settings) {
         if (this.isVisible(co)) {
            co.updateHeight(y);
            if (co instanceof SliderComponent) {
               y += 16.0F;
            } else if (co instanceof ButtonComponent || co instanceof BindComponent || co instanceof DescriptionComponent || co instanceof GroupComponent) {
               y += 12.0F;
            }
         }
      }
   }

   @Override
   public void render() {
      if (this.hovering || this.hoverTimer != null) {
         double hoverAlpha = this.hovering && this.hoverTimer != null
            ? this.hoverTimer.getValueFloat(0.0F, this.originalHoverAlpha, 1)
            : (
               this.hoverTimer != null && !this.hovering
                  ? this.originalHoverAlpha - this.hoverTimer.getValueFloat(0.0F, this.originalHoverAlpha, 1)
                  : this.originalHoverAlpha
            );
         if (hoverAlpha == 0.0) {
            this.hoverTimer = null;
         }

         RenderUtils.drawRoundedRectangle(
            this.categoryComponent.getX(),
            this.categoryComponent.getY() + this.yPos,
            this.categoryComponent.getX() + this.categoryComponent.getWidth(),
            this.categoryComponent.getY() + 16.0F + this.yPos,
            8.0F,
            Utils.mergeAlpha(this.hoverColor, (int)hoverAlpha)
         );
      }

      int button_rgb = this.mod.isEnabled() ? this.enabledColor : this.disabledColor;
      if (this.mod.script != null && this.mod.script.error) {
         button_rgb = this.invalidColor;
      }

      if (this.mod.moduleCategory() == Module.category.profiles
         && !(this.mod instanceof Manager)
         && !((ProfileModule)this.mod).saved
         && keystrokesmod.Raven.currentProfile.getModule() == this.mod) {
         button_rgb = this.unsavedColor;
      }

      if (this.smoothTimer != null && System.currentTimeMillis() - this.smoothTimer.last >= 300L) {
         this.smoothTimer = null;
      }

      if (this.smoothTimer != null) {
         int height = this.getModuleHeight();
         if (this.isOpened) {
            this.smoothingY = this.smoothTimer.getValueInt(16, height, 1);
            if (this.smoothingY == height) {
               this.smoothTimer = null;
            }
         } else {
            this.smoothingY = this.smoothTimer.getValueInt(height, 16, 1);
            if (this.smoothingY == 16) {
               this.smoothTimer = null;
            }
         }

         this.categoryComponent.updateHeight();
      }

      Minecraft.getMinecraft()
         .fontRendererObj
         .drawStringWithShadow(
            this.mod.getName(),
            this.categoryComponent.getX()
               + this.categoryComponent.getWidth() / 2.0F
               - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.mod.getName()) / 2,
            this.categoryComponent.getY() + this.yPos + 4.0F,
            button_rgb
         );
      boolean scissorRequired = this.smoothTimer != null;
      if (scissorRequired) {
         GL11.glPushMatrix();
         GL11.glEnable(3089);
         RenderUtils.scissor(
            this.categoryComponent.getX() - 2.0F,
            this.categoryComponent.getY() + this.yPos + 4.0F,
            this.categoryComponent.getWidth() + 4.0F,
            this.smoothingY + 4
         );
      }

      if (this.isOpened || this.smoothTimer != null) {
         for (Component settingComponent : this.settings) {
            if (this.isVisible(settingComponent)) {
               settingComponent.render();
            }
         }
      }

      if (scissorRequired) {
         GL11.glDisable(3089);
         GL11.glPopMatrix();
      }
   }

   @Override
   public int getHeight() {
      if (this.smoothTimer != null) {
         return this.smoothingY;
      }

      if (!this.isOpened) {
         return 16;
      }

      int h = 16;

      for (Component c : this.settings) {
         if (this.isVisible(c)) {
            if (c instanceof SliderComponent) {
               h += 16;
            } else if (c instanceof ButtonComponent || c instanceof BindComponent || c instanceof DescriptionComponent || c instanceof GroupComponent) {
               h += 12;
            }
         }
      }

      return h;
   }

   public void onSliderChange() {
      for (Component c : this.settings) {
         if (c instanceof SliderComponent) {
            ((SliderComponent)c).onSliderChange();
         }
      }
   }

   public int getModuleHeight() {
      int h = 16;

      for (Component c : this.settings) {
         if (this.isVisible(c)) {
            if (c instanceof SliderComponent) {
               h += 16;
            } else if (c instanceof ButtonComponent || c instanceof BindComponent || c instanceof DescriptionComponent || c instanceof GroupComponent) {
               h += 12;
            }
         }
      }

      return h;
   }

   @Override
   public void drawScreen(int x, int y) {
      for (Component c : this.settings) {
         c.drawScreen(x, y);
      }

      if (this.overModuleName(x, y) && this.categoryComponent.opened) {
         this.hovering = true;
         if (this.hoverTimer == null) {
            (this.hoverTimer = new Timer(75.0F)).start();
            this.hoverStarted = true;
         }
      } else {
         if (this.hovering && this.hoverStarted) {
            (this.hoverTimer = new Timer(75.0F)).start();
         }

         this.hoverStarted = false;
         this.hovering = false;
      }
   }

   public String getName() {
      return this.mod.getName();
   }

   @Override
   public boolean onClick(int x, int y, int mouse) {
      if (this.overModuleName(x, y) && mouse == 0 && this.mod.canBeEnabled()) {
         this.mod.toggle();
         if (this.mod.moduleCategory() != Module.category.profiles && keystrokesmod.Raven.currentProfile != null) {
            ((ProfileModule)keystrokesmod.Raven.currentProfile.getModule()).saved = false;
         }
      }

      if (this.overModuleName(x, y) && mouse == 1) {
         this.isOpened = !this.isOpened;
         (this.smoothTimer = new Timer(200.0F)).start();
         this.categoryComponent.updateHeight();
         return true;
      }

      for (Component settingComponent : this.settings) {
         settingComponent.onClick(x, y, mouse);
      }

      return false;
   }

   @Override
   public void mouseReleased(int x, int y, int m) {
      for (Component c : this.settings) {
         c.mouseReleased(x, y, m);
      }
   }

   @Override
   public void keyTyped(char t, int k) {
      for (Component c : this.settings) {
         c.keyTyped(t, k);
      }
   }

   @Override
   public void onScroll(int scroll) {
      for (Component component : this.settings) {
         component.onScroll(scroll);
      }
   }

   @Override
   public void onGuiClosed() {
      for (Component c : this.settings) {
         c.onGuiClosed();
      }

      this.smoothTimer = null;
      this.hoverTimer = null;
      this.smoothingY = this.getHeight();
   }

   public boolean overModuleName(int x, int y) {
      return x > this.categoryComponent.getX()
         && x < this.categoryComponent.getX() + this.categoryComponent.getWidth()
         && y > this.categoryComponent.getModuleY() + this.yPos
         && y < this.categoryComponent.getModuleY() + 16.0F + this.yPos;
   }

   public void updateSettingPositions(int xOffset) {
      float y = this.yPos + 12.0F;

      for (Component c : this.settings) {
         if (this.isVisible(c)) {
            if (c instanceof DescriptionComponent) {
               ((DescriptionComponent)c).o = y;
               y += 12.0F;
            } else if (c instanceof BindComponent) {
               ((BindComponent)c).o = y;
               if (((BindComponent)c).keySetting != null) {
                  if (xOffset != 0 & this.isGroupOpened(c, false)) {
                     ((BindComponent)c).x += xOffset;
                     ((BindComponent)c).xOffset = xOffset;
                  }

                  y += 12.0F;
               }
            } else if (c instanceof SliderComponent) {
               ((SliderComponent)c).o = y;
               if (xOffset != 0 & this.isGroupOpened(c, false)) {
                  ((SliderComponent)c).x += xOffset;
                  ((SliderComponent)c).xOffset = xOffset;
                  ((SliderComponent)c).renderLine = true;
               } else {
                  ((SliderComponent)c).renderLine = false;
               }

               y += 16.0F;
            } else if (c instanceof ButtonComponent) {
               ((ButtonComponent)c).o = y;
               if (xOffset != 0 & this.isGroupOpened(c, false)) {
                  ((ButtonComponent)c).x += xOffset;
                  ((ButtonComponent)c).xOffset = xOffset;
                  ((ButtonComponent)c).renderLine = true;
               } else {
                  ((ButtonComponent)c).renderLine = false;
               }

               y += 12.0F;
            }
         }
      }

      this.categoryComponent.updateHeight();
   }

   public boolean isVisible(Component component) {
      if (component instanceof SliderComponent) {
         return this.isGroupOpened(component, ((SliderComponent)component).sliderSetting.visible);
      } else if (component instanceof ButtonComponent) {
         return this.isGroupOpened(component, ((ButtonComponent)component).buttonSetting.visible);
      } else if (component instanceof DescriptionComponent) {
         return ((DescriptionComponent)component).desc.visible;
      } else {
         return component instanceof BindComponent && ((BindComponent)component).keySetting != null
            ? this.isGroupOpened(component, ((BindComponent)component).keySetting.visible)
            : true;
      }
   }

   public boolean isGroupOpened(Component component, boolean defaultBool) {
      String groupName = "";
      if (component instanceof SliderComponent && ((SliderComponent)component).sliderSetting.groupSetting != null) {
         groupName = ((SliderComponent)component).sliderSetting.groupSetting.getName();
      }

      if (component instanceof ButtonComponent && ((ButtonComponent)component).buttonSetting.group != null) {
         groupName = ((ButtonComponent)component).buttonSetting.group.getName();
      }

      if (component instanceof BindComponent && ((BindComponent)component).keySetting != null && ((BindComponent)component).keySetting.group != null) {
         groupName = ((BindComponent)component).keySetting.group.getName();
      }

      if (groupName.isEmpty()) {
         return defaultBool;
      }

      for (Component c : this.settings) {
         if (c instanceof GroupComponent && ((GroupComponent)c).setting.getName().equals(groupName)) {
            return ((GroupComponent)c).opened;
         }
      }

      return defaultBool;
   }
}
