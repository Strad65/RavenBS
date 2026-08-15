package keystrokesmod.clickgui.components.impl;

import java.awt.Color;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.Manager;
import keystrokesmod.utility.profile.Profile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public class CategoryComponent {
   public List<ModuleComponent> modules = new CopyOnWriteArrayList<>();
   public Module.category category;
   public boolean opened;
   public float width;
   public float y;
   public float x;
   public float titleHeight;
   public boolean dragging;
   public float xx;
   public float yy;
   public boolean hovering = false;
   public boolean hoveringOverCategory = false;
   public Timer smoothTimer;
   private Timer textTimer;
   public Timer smoothScrollTimer;
   public ScaledResolution scale;
   public float big;
   private float bigSettings;
   private final int translucentBackground = new Color(0, 0, 0, 110).getRGB();
   private final int regularOutline = new Color(81, 99, 149).getRGB();
   private final int regularOutline2 = new Color(97, 67, 133).getRGB();
   private final int categoryNameColor = new Color(220, 220, 220).getRGB();
   private float lastHeight;
   public float moduleY;
   private float lastModuleY;
   private float screenHeight;
   private boolean scrolled;
   private float targetModuleY;
   private float closedHeight;

   public CategoryComponent(Module.category category) {
      this.category = category;
      this.width = 92.0F;
      this.x = 5.0F;
      this.moduleY = this.y = 5.0F;
      this.titleHeight = 13.0F;
      this.smoothTimer = null;
      this.textTimer = null;
      this.xx = 0.0F;
      this.opened = false;
      this.dragging = false;
      float moduleRenderY = this.titleHeight + 3.0F;
      this.scale = new ScaledResolution(Minecraft.getMinecraft());
      this.targetModuleY = this.moduleY;

      for (Module mod : keystrokesmod.Raven.getModuleManager().inCategory(this.category)) {
         ModuleComponent b = new ModuleComponent(mod, this, moduleRenderY);
         this.modules.add(b);
         moduleRenderY += 16.0F;
      }
   }

   public List<ModuleComponent> getModules() {
      return this.modules;
   }

   public void reloadModules(boolean isProfile) {
      this.modules.clear();
      this.titleHeight = 13.0F;
      float moduleRenderY = this.titleHeight + 3.0F;
      if (this.category == Module.category.profiles && isProfile || this.category == Module.category.scripts && !isProfile) {
         ModuleComponent manager = new ModuleComponent(isProfile ? new Manager() : new keystrokesmod.script.Manager(), this, moduleRenderY);
         this.modules.add(manager);
         if (keystrokesmod.Raven.profileManager == null && isProfile || keystrokesmod.Raven.scriptManager == null && !isProfile) {
            return;
         }

         if (isProfile) {
            for (Profile profile : keystrokesmod.Raven.profileManager.profiles) {
               moduleRenderY += 16.0F;
               ModuleComponent b = new ModuleComponent(profile.getModule(), this, moduleRenderY);
               this.modules.add(b);
            }
         } else {
            Collection<Module> modulesCollection = keystrokesmod.Raven.scriptManager.scripts.values();

            for (Module module : modulesCollection.stream()
               .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
               .collect(Collectors.toList())) {
               moduleRenderY += 16.0F;
               ModuleComponent b = new ModuleComponent(module, this, moduleRenderY);
               this.modules.add(b);
            }
         }
      }
   }

   public void setX(float newX, boolean limit) {
      if (limit) {
         ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
         float screenW = sr.getScaledWidth();
         newX = Math.max(newX, 2.0F);
         newX = Math.min(newX, screenW - this.width - 4.0F);
      }

      this.x = newX;
   }

   public void setY(float y, boolean limit) {
      if (limit) {
         ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
         float screenH = sr.getScaledHeight();
         float catHeight = this.titleHeight;
         y = Math.max(y, 1.0F);
         float maxY = screenH - catHeight - 5.0F;
         y = Math.min(y, maxY);
      }

      this.moduleY = this.y = y;
      this.targetModuleY = y;
   }

   public void overTitle(boolean d) {
      this.dragging = d;
   }

   public boolean isOpened() {
      return this.opened;
   }

   public void mouseClicked(boolean on) {
      this.opened = on;
      (this.smoothTimer = new Timer(500.0F)).start();
      (this.textTimer = new Timer(200.0F)).start();
   }

   public void openModule(ModuleComponent component) {
      if (!component.isOpened) {
         this.closedHeight = this.y + this.titleHeight + this.big + 4.0F;
      }

      (this.smoothTimer = new Timer(200.0F)).start();
   }

   public void onScroll(int mouseScrollInput) {
      for (Component component : this.modules) {
         component.onScroll(mouseScrollInput);
      }

      if (this.hoveringOverCategory && this.opened) {
         int scrollSpeed = (int)Gui.scrollSpeed.getInput();
         if (mouseScrollInput > 0) {
            this.targetModuleY += scrollSpeed;
         } else if (mouseScrollInput < 0) {
            this.targetModuleY -= scrollSpeed;
         }

         this.scrolled = true;
         (this.smoothScrollTimer = new Timer(200.0F)).start();
      }
   }

   public void render(FontRenderer renderer) {
      this.targetModuleY = Math.min(this.targetModuleY, this.y);
      if (this.targetModuleY + this.bigSettings < this.y + this.big + this.titleHeight) {
         this.targetModuleY = (int)(this.y + this.big - this.bigSettings);
      }

      this.width = 92.0F;
      int modulesHeight = 0;
      int settingsHeight = 0;
      if (!this.modules.isEmpty() && this.opened) {
         for (ModuleComponent c : this.modules) {
            settingsHeight += c.getHeight();
            int height = !c.isOpened ? 16 : c.getModuleHeight();
            if (modulesHeight + height > this.screenHeight * 0.9) {
               modulesHeight = (int)(this.screenHeight * 0.9);
            } else {
               modulesHeight += c.getHeight();
            }
         }

         this.big = modulesHeight;
         this.bigSettings = settingsHeight;
      }

      float middlePos = this.x + this.width / 2.0F - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.category.name()) / 2;
      float xPos = this.opened ? middlePos : this.x + 12.0F;
      float extra = this.y + this.titleHeight + modulesHeight + 4.0F;
      if (this.smoothTimer != null && System.currentTimeMillis() - this.smoothTimer.last >= 330L) {
         this.smoothTimer = null;
      }

      if (extra != this.lastHeight && this.smoothTimer != null) {
         double diff = this.lastHeight - extra;
         if (diff < 0.0) {
            extra = this.smoothTimer.getValueFloat(this.lastHeight, this.y + this.titleHeight + modulesHeight + 4.0F, 1);
         } else if (diff > 0.0) {
            extra = this.smoothTimer.getValueFloat(this.opened ? this.closedHeight : this.lastHeight, this.y + this.titleHeight + modulesHeight + 4.0F, 1);
         }
      }

      float namePos = this.textTimer == null ? xPos : this.textTimer.getValueFloat(this.x + 12.0F, middlePos, 1);
      if (!this.opened) {
         namePos = this.textTimer == null
            ? xPos
            : middlePos
               - this.textTimer
                  .getValueFloat(0.0F, this.width / 2.0F - Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.category.name()) / 2 - 12.0F, 1);
      }

      if (this.scrolled && this.smoothScrollTimer != null) {
         if (System.currentTimeMillis() - this.smoothScrollTimer.last <= 200L) {
            float interpolated = this.smoothScrollTimer.getValueFloat(this.lastModuleY, this.targetModuleY, 4);
            this.moduleY = (int)interpolated;
         } else {
            this.moduleY = this.targetModuleY;
            this.scrolled = false;
            this.smoothScrollTimer = null;
         }
      } else {
         this.moduleY = this.targetModuleY;
      }

      this.lastModuleY = this.moduleY;
      this.lastHeight = extra;
      GL11.glPushMatrix();
      GL11.glEnable(3089);
      RenderUtils.scissor(0.0, this.y - 2.0F, this.x + this.width + 4.0F, extra - this.y + 4.0F);
      RenderUtils.drawRoundedGradientOutlinedRectangle(
         this.x - 2.0F,
         this.y,
         this.x + this.width + 2.0F,
         extra,
         10.0F,
         this.translucentBackground,
         (this.opened || this.hovering) && Gui.rainBowOutlines.isToggled() ? RenderUtils.setAlpha(Utils.getChroma(2L, 0L), 0.5) : this.regularOutline,
         (this.opened || this.hovering) && Gui.rainBowOutlines.isToggled() ? RenderUtils.setAlpha(Utils.getChroma(2L, 700L), 0.5) : this.regularOutline2
      );
      this.renderItemForCategory(this.category, (int)(this.x + 1.0F), (int)(this.y + 4.0F), this.opened || this.hovering);
      renderer.drawString(this.category.name(), namePos, this.y + 4.0F, this.categoryNameColor, false);
      RenderUtils.scissor(0.0, this.y + this.titleHeight + 3.0F, this.x + this.width + 4.0F, extra - this.y - 4.0F - this.titleHeight);
      float prevY = this.y;
      this.y = this.moduleY;
      if (this.opened || this.smoothTimer != null) {
         for (Component c2 : this.modules) {
            c2.render();
         }
      }

      this.y = prevY;
      GL11.glDisable(3089);
      GL11.glPopMatrix();
   }

   public void updateHeight() {
      float y = this.titleHeight + 3.0F;

      for (Component component : this.modules) {
         component.updateHeight(y);
         y += component.getHeight();
      }
   }

   public float getX() {
      return this.x;
   }

   public float getY() {
      return this.y;
   }

   public float getModuleY() {
      return this.moduleY;
   }

   public float getWidth() {
      return this.width;
   }

   public void mousePosition(int mouseX, int mouseY) {
      if (this.dragging) {
         float newX = mouseX - this.xx;
         float newY = mouseY - this.yy;
         if (Gui.limitToScreen.isToggled()) {
            ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
            int screenW = sr.getScaledWidth();
            int screenH = sr.getScaledHeight();
            float catHeight = this.titleHeight;
            newX = Math.max(newX, 2.0F);
            newX = Math.min(newX, screenW - this.width - 4.0F);
            newY = Math.max(newY, 1.0F);
            int maxY = (int)(screenH - catHeight - 5.0F);
            newY = Math.min(newY, maxY);
         }

         this.setX(newX, false);
         this.setY(newY, false);
      }

      this.hoveringOverCategory = this.overCategory(mouseX, mouseY);
      this.hovering = this.overTitle(mouseX, mouseY);
   }

   public boolean overTitle(int x, int y) {
      return x >= this.x && x <= this.x + this.width && y >= this.y + 2.0F && y <= this.y + this.titleHeight + 1.0F;
   }

   public boolean overCategory(int x, int y) {
      return x >= this.x - 2.0F && x <= this.x + this.width + 2.0F && y >= this.y + 2.0F && y <= this.y + this.titleHeight + this.big + 1.0F;
   }

   public boolean draggable(int x, int y) {
      return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.titleHeight;
   }

   public boolean overRect(int x, int y) {
      return x >= this.x - 2.0F && x <= this.x + this.width + 2.0F && y >= this.y && y <= this.lastHeight;
   }

   private void renderItemForCategory(Module.category category, int x, int y, boolean enchant) {
      RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
      double scale = 0.55;
      GlStateManager.pushMatrix();
      GlStateManager.scale(scale, scale, scale);
      ItemStack itemStack = null;
      switch (category) {
         case combat:
            itemStack = new ItemStack(Items.diamond_sword);
            break;
         case movement:
            itemStack = new ItemStack(Items.diamond_boots);
            break;
         case player:
            itemStack = new ItemStack(Items.golden_apple);
            break;
         case world:
            itemStack = new ItemStack(Items.map);
            break;
         case render:
            itemStack = new ItemStack(Items.ender_eye);
            break;
         case minigames:
            itemStack = new ItemStack(Items.gold_ingot);
            break;
         case fun:
            itemStack = new ItemStack(Items.slime_ball);
            break;
         case other:
            itemStack = new ItemStack(Items.clock);
            break;
         case client:
            itemStack = new ItemStack(Items.compass);
            break;
         case profiles:
            itemStack = new ItemStack(Items.book);
            break;
         case scripts:
            itemStack = new ItemStack(Items.redstone);
      }

      if (itemStack != null) {
         if (enchant) {
            if (category != Module.category.player) {
               itemStack.addEnchantment(Enchantment.unbreaking, 2);
            } else {
               itemStack.setItemDamage(1);
            }
         }

         RenderHelper.enableGUIStandardItemLighting();
         GlStateManager.disableBlend();
         renderItem.renderItemAndEffectIntoGUI(itemStack, (int)(x / scale), (int)(y / scale));
         GlStateManager.enableBlend();
         RenderHelper.disableStandardItemLighting();
      }

      GlStateManager.scale(1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }

   public void setScreenHeight(int screenHeight) {
      this.screenHeight = screenHeight;
   }

   public void limitPositions() {
      this.setX(this.x, true);
      this.setY(this.y, true);
   }
}
