package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.io.IOException;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class HUD extends Module {
   public static SliderSetting theme;
   private static SliderSetting outline;
   public static ButtonSetting alphabeticalSort;
   private static ButtonSetting backgroundBlur;
   private static SliderSetting backgroundBlurRadius;
   private static ButtonSetting backgroundBloom;
   private static SliderSetting backgroundBloomRadius;
   private static SliderSetting backgroundAlpha;
   private static SliderSetting gap;
   private static ButtonSetting alignRight;
   private static ButtonSetting lowercase;
   private static ButtonSetting removeCloset;
   private static ButtonSetting removeRender;
   private static ButtonSetting removeScripts;
   public static ButtonSetting showInfo;
   public static int posX = 5;
   public static int posY = 70;
   private boolean isAlphabeticalSort;
   private boolean canShowInfo;
   private String[] outlineModes = new String[]{"None", "Full", "Side"};
   private static double gapv;
   private static int a;

   public HUD() {
      super("HUD", Module.category.render);
      this.registerSetting(new DescriptionSetting("Right click bind to hide modules."));
      this.registerSetting(theme = new SliderSetting("Theme", 0, Theme.themes));
      this.registerSetting(outline = new SliderSetting("Outline", 0, this.outlineModes));
      this.registerSetting(new ButtonSetting("Edit position", () -> mc.displayGuiScreen(new HUD.EditScreen())));
      this.registerSetting(alignRight = new ButtonSetting("Align right", false));
      this.registerSetting(alphabeticalSort = new ButtonSetting("Alphabetical sort", false));
      this.registerSetting(backgroundBlur = new ButtonSetting("Background blur", false));
      this.registerSetting(backgroundBlurRadius = new SliderSetting("Blur radius", "px", 3.0, 0.0, 10.0, 0.5));
      this.registerSetting(backgroundBloom = new ButtonSetting("Background bloom", false));
      this.registerSetting(backgroundBloomRadius = new SliderSetting("Bloom radius", "px", 2.0, 0.0, 10.0, 0.5));
      this.registerSetting(backgroundAlpha = new SliderSetting("Background alpha", 50.0, 0.0, 100.0, 1.0));
      this.registerSetting(gap = new SliderSetting("Gap", 2.0, 0.0, 10.0, 0.5));
      this.registerSetting(lowercase = new ButtonSetting("Lowercase", false));
      this.registerSetting(removeCloset = new ButtonSetting("Remove closet modules", false));
      this.registerSetting(removeRender = new ButtonSetting("Remove render modules", false));
      this.registerSetting(removeScripts = new ButtonSetting("Remove scripts", false));
      this.registerSetting(showInfo = new ButtonSetting("Show module info", true));
   }

   @Override
   public void onEnable() {
      ModuleManager.sort();
   }

   @Override
   public void guiUpdate() {
      this.backgroundBlurRadius.setVisible(backgroundBlur.isToggled(), this);
      this.backgroundBloomRadius.setVisible(backgroundBloom.isToggled(), this);
   }

   @Override
   public void guiButtonToggled(ButtonSetting b) {
      if (b == alphabeticalSort || b == showInfo) {
         ModuleManager.sort();
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (ev.phase == Phase.END && Utils.nullCheck()) {
         gapv = gap.getInput();
         a = (int)(backgroundAlpha.getInput() * 2.55);
         int alpha = new Color(0, 0, 0, a).getRGB();
         if (this.isAlphabeticalSort != alphabeticalSort.isToggled()) {
            this.isAlphabeticalSort = alphabeticalSort.isToggled();
            ModuleManager.sort();
         }

         if (this.canShowInfo != showInfo.isToggled()) {
            this.canShowInfo = showInfo.isToggled();
            ModuleManager.sort();
         }

         if (mc.currentScreen == null && !mc.gameSettings.showDebugInfo) {
            for (Module module : ModuleManager.organizedModules) {
               module.getInfoUpdate();
               if (Module.sort) {
                  break;
               }
            }

            if (Module.sort) {
               ModuleManager.sort();
            }

            Module.sort = false;
            int yPos = posY;
            double n2 = 0.0;
            String previousModule = "";
            int lastXPos = 0;

            try {
               for (Module module : ModuleManager.organizedModules) {
                  if (module.isEnabled()
                     && module != this
                     && !module.isHidden()
                     && module != ModuleManager.commandLine
                     && (!removeRender.isToggled() || module.moduleCategory() != Module.category.render)
                     && (!removeScripts.isToggled() || module.moduleCategory() != Module.category.scripts)
                     && (!removeCloset.isToggled() || !module.closetModule)) {
                     String moduleName = module.getNameInHud();
                     if (showInfo.isToggled() && !module.getInfo().isEmpty()) {
                        moduleName = moduleName + " §7" + module.getInfo();
                     }

                     if (lowercase.isToggled()) {
                        moduleName = moduleName.toLowerCase();
                     }

                     int color = Theme.getGradient((int)theme.getInput(), n2);
                     int xPos = posX;
                     if (alignRight.isToggled()) {
                        xPos -= mc.fontRendererObj.getStringWidth(moduleName);
                     }

                     // Background rendering
                     if (backgroundAlpha.getInput() != 0.0 || backgroundBlur.isToggled() || backgroundBloom.isToggled()) {
                        int bgX1 = xPos - 1;
                        int bgY1 = yPos - 1;
                        int bgX2 = xPos + mc.fontRendererObj.getStringWidth(moduleName) + 1;
                        int bgY2 = (int)(yPos + mc.fontRendererObj.FONT_HEIGHT + gapv + 1);

                        // Bloom/shadow effect
                        if (backgroundBloom.isToggled()) {
                           float bloomRadius = (float) backgroundBloomRadius.getInput();
                           int maxAlphaBackground = a > 210 ? 210 : a;
                           BlurUtils.bloomRect(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 3, bloomRadius,
                                               new Color(0, 0, 0, maxAlphaBackground));
                        }

                        if (backgroundBlur.isToggled()) {
                           float blurRadius = (float) backgroundBlurRadius.getInput();
                           BlurUtils.blurRect(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 2, blurRadius);
                        } else if (!backgroundBloom.isToggled()) {
                           RenderUtils.drawRect(bgX1, bgY1, bgX2 + 0.5, bgY2, alpha);
                        }
                     }

                     if (outline.getInput() == 1.0 && n2 == 0.0) {
                        RenderUtils.drawRect(xPos - 2, yPos - 2, xPos + mc.fontRendererObj.getStringWidth(moduleName) + 1.5, yPos - 1, color);
                     }

                     if (theme.getInput() == 0.0) {
                        n2 -= 120.0;
                     } else {
                        n2 -= 12.0;
                     }

                     if (n2 != 0.0 && outline.getInput() == 1.0) {
                        double difference = mc.fontRendererObj.getStringWidth(previousModule) - mc.fontRendererObj.getStringWidth(moduleName);
                        if (alphabeticalSort.isToggled() && difference < 0.0) {
                           RenderUtils.drawRect(xPos - 2, yPos - 2, xPos - difference - 2.0, yPos - 1, color);
                        } else if (alignRight.isToggled()) {
                           RenderUtils.drawRect(xPos - difference - 2.0, yPos - 2, xPos - 1, yPos - 1, color);
                        } else {
                           RenderUtils.drawRect(
                              xPos + mc.fontRendererObj.getStringWidth(moduleName) + 0.5,
                              yPos - 2,
                              xPos + difference + mc.fontRendererObj.getStringWidth(moduleName) + 1.5,
                              yPos - 1,
                              color
                           );
                        }
                     }

                     if (outline.getInput() > 0.0) {
                        if (alignRight.isToggled()) {
                           RenderUtils.drawRect(
                              xPos + mc.fontRendererObj.getStringWidth(moduleName) + 0.5,
                              yPos - 1,
                              xPos + mc.fontRendererObj.getStringWidth(moduleName) + 1.5,
                              yPos + mc.fontRendererObj.FONT_HEIGHT + 1,
                              color
                           );
                        } else {
                           RenderUtils.drawRect(xPos - 2, yPos - 1, xPos - 1, yPos + mc.fontRendererObj.FONT_HEIGHT + 1, color);
                        }
                     }

                     if (outline.getInput() == 1.0) {
                        if (alignRight.isToggled()) {
                           RenderUtils.drawRect(xPos - 2, yPos - 1, xPos - 1, yPos + mc.fontRendererObj.FONT_HEIGHT + 1, color);
                        } else {
                           RenderUtils.drawRect(
                              xPos + mc.fontRendererObj.getStringWidth(moduleName) + 0.5,
                              yPos - 1,
                              xPos + mc.fontRendererObj.getStringWidth(moduleName) + 1.5,
                              yPos + mc.fontRendererObj.FONT_HEIGHT + gapv / 2.0,
                              color
                           );
                        }
                     }

                     mc.fontRendererObj.drawString(moduleName, xPos, yPos, color, true);
                     previousModule = moduleName;
                     lastXPos = xPos;
                     yPos = (int)(yPos + (mc.fontRendererObj.FONT_HEIGHT + gapv));
                  }
               }
            } catch (Exception e) {
               Utils.sendMessage("&cAn error occurred rendering HUD. check your logs");
               e.printStackTrace();
            }

            if (outline.getInput() == 1.0) {
               RenderUtils.drawRect(
                  lastXPos - 2, yPos - 1, lastXPos + mc.fontRendererObj.getStringWidth(previousModule) + 1.5, yPos, Theme.getGradient((int)theme.getInput(), n2)
               );
            }
         }
      }
   }

   public static int getLongestModule(FontRenderer fr) {
      int length = 0;

      for (Module module : ModuleManager.organizedModules) {
         if (module.isEnabled()) {
            String moduleName = module.getName();
            if (showInfo.isToggled() && !module.getInfo().isEmpty()) {
               moduleName = moduleName + " §7" + module.getInfo();
            }

            if (lowercase.isToggled()) {
               moduleName = moduleName.toLowerCase();
            }

            if (fr.getStringWidth(moduleName) > length) {
               length = fr.getStringWidth(moduleName);
            }
         }
      }

      return length;
   }

   static class EditScreen extends GuiScreen {
      final String example = "This is an-Example-HUD";
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
      int clickMinX = 0;

      public void initGui() {
         super.initGui();
         this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
         this.aX = HUD.posX;
         this.aY = HUD.posY;
      }

      public void drawScreen(int mX, int mY, float pt) {
         drawRect(0, 0, this.width, this.height, -1308622848);
         int miX = this.aX;
         int miY = this.aY;
         int maX = miX + 50;
         int maY = miY + 32;
         int[] clickPos = this.d(this.mc.fontRendererObj, "This is an-Example-HUD");
         this.miX = miX;
         this.miY = miY;
         if (clickPos == null) {
            this.maX = maX;
            this.maY = maY;
            this.clickMinX = miX;
         } else {
            this.maX = clickPos[0];
            this.maY = clickPos[1];
            this.clickMinX = clickPos[2];
         }

         HUD.posX = miX;
         HUD.posY = miY;
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

      private int[] d(FontRenderer fr, String t) {
         if (this.empty()) {
            int x = this.miX;
            int y = this.miY;
            String[] var5 = t.split("-");

            for (String s : var5) {
               if (HUD.alignRight.isToggled()) {
                  x += this.mc.fontRendererObj.getStringWidth(var5[0]) - this.mc.fontRendererObj.getStringWidth(s);
               }

               fr.drawString(s, x, y, Color.white.getRGB(), true);
               y += fr.FONT_HEIGHT + 2;
            }

            return null;
         } else {
            int longestModule = HUD.getLongestModule(this.mc.fontRendererObj);
            int n = this.miY;
            double n2 = 0.0;
            String previousModule = "";
            int lastXPos = 0;

            try {
               for (Module module : ModuleManager.organizedModules) {
                  if (module.isEnabled()
                     && !(module instanceof HUD)
                     && !module.isHidden()
                     && module != ModuleManager.commandLine
                     && (!HUD.removeRender.isToggled() || module.moduleCategory() != Module.category.render)
                     && (!HUD.removeScripts.isToggled() || module.moduleCategory() != Module.category.scripts)
                     && (!HUD.removeCloset.isToggled() || !module.closetModule)) {
                     String moduleName = module.getNameInHud();
                     if (HUD.showInfo.isToggled() && !module.getInfo().isEmpty()) {
                        moduleName = moduleName + " §7" + module.getInfo();
                     }

                     if (HUD.lowercase.isToggled()) {
                        moduleName = moduleName.toLowerCase();
                     }

                     int color = Theme.getGradient((int)HUD.theme.getInput(), n2);
                     int xPos = HUD.posX;
                     if (HUD.alignRight.isToggled()) {
                        xPos -= this.mc.fontRendererObj.getStringWidth(moduleName);
                     }

                     if (HUD.outline.getInput() == 1.0 && n2 == 0.0) {
                        RenderUtils.drawRect(xPos - 2, n - 2, xPos + this.mc.fontRendererObj.getStringWidth(moduleName) + 1.5, n - 1, color);
                     }

                     if (n2 != 0.0 && HUD.outline.getInput() == 1.0) {
                        double difference = this.mc.fontRendererObj.getStringWidth(previousModule)
                           - this.mc.fontRendererObj.getStringWidth(moduleName);
                        RenderUtils.drawRect(xPos - difference - 2.0, n - 2, xPos - 1, n - 1, color);
                     }

                     if (HUD.theme.getInput() == 0.0) {
                        n2 -= 120.0;
                     } else {
                        n2 -= 12.0;
                     }

                     // Background rendering in EditScreen
                     if (HUD.backgroundAlpha.getInput() != 0.0 || HUD.backgroundBlur.isToggled() || HUD.backgroundBloom.isToggled()) {
                        int bgX1 = xPos - 1;
                        int bgY1 = n - 1;
                        int bgX2 = xPos + this.mc.fontRendererObj.getStringWidth(moduleName) + 1;
                        int bgY2 = (int)(n + this.mc.fontRendererObj.FONT_HEIGHT + HUD.gapv + 1);

                        // Bloom/shadow effect
                        if (HUD.backgroundBloom.isToggled()) {
                           float bloomRadius = (float) HUD.backgroundBloomRadius.getInput();
                           int maxAlphaBackground = HUD.a > 210 ? 210 : HUD.a;
                           BlurUtils.bloomRect(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 3, bloomRadius,
                                               new Color(0, 0, 0, maxAlphaBackground));
                        }

                        if (HUD.backgroundBlur.isToggled()) {
                           float blurRadius = (float) HUD.backgroundBlurRadius.getInput();
                           BlurUtils.blurRect(bgX1, bgY1, bgX2 - bgX1, bgY2 - bgY1, 2, blurRadius);
                        } else if (!HUD.backgroundBloom.isToggled()) {
                           RenderUtils.drawRect(bgX1, bgY1, bgX2 + 0.5, bgY2, HUD.a);
                        }
                     }

                     if (n2 != 0.0 && HUD.outline.getInput() == 1.0) {
                        double difference = this.mc.fontRendererObj.getStringWidth(previousModule)
                           - this.mc.fontRendererObj.getStringWidth(moduleName);
                        if (HUD.alphabeticalSort.isToggled() && difference < 0.0) {
                           RenderUtils.drawRect(xPos - 2, n - 2, xPos - difference - 2.0, n - 1, color);
                        } else if (HUD.alignRight.isToggled()) {
                           RenderUtils.drawRect(xPos - difference - 2.0, n - 2, xPos - 1, n - 1, color);
                        } else {
                           RenderUtils.drawRect(
                              xPos + this.mc.fontRendererObj.getStringWidth(moduleName),
                              n - 2,
                              xPos - 1 + difference + this.mc.fontRendererObj.getStringWidth(moduleName),
                              n - 1,
                              color
                           );
                        }
                     }

                     if (HUD.outline.getInput() > 0.0) {
                        if (HUD.alignRight.isToggled()) {
                           RenderUtils.drawRect(
                              xPos + this.mc.fontRendererObj.getStringWidth(moduleName) + 0.5,
                              n - 1,
                              xPos + this.mc.fontRendererObj.getStringWidth(moduleName) + 1.5,
                              n + this.mc.fontRendererObj.FONT_HEIGHT + 1,
                              color
                           );
                        } else {
                           RenderUtils.drawRect(xPos - 2, n - 1, xPos - 1, n + this.mc.fontRendererObj.FONT_HEIGHT + 1, color);
                        }
                     }

                     if (HUD.outline.getInput() == 1.0) {
                        if (HUD.alignRight.isToggled()) {
                           RenderUtils.drawRect(xPos - 2, n - 1, xPos - 1, n + this.mc.fontRendererObj.FONT_HEIGHT + 1, color);
                        } else {
                           RenderUtils.drawRect(
                              xPos + this.mc.fontRendererObj.getStringWidth(moduleName) + 0.5,
                              n - 1,
                              xPos + this.mc.fontRendererObj.getStringWidth(moduleName) + 1.5,
                              n + this.mc.fontRendererObj.FONT_HEIGHT + 1,
                              color
                           );
                        }
                     }

                     this.mc.fontRendererObj.drawString(moduleName, xPos, n, color, true);
                     previousModule = moduleName;
                     lastXPos = xPos;
                     n += this.mc.fontRendererObj.FONT_HEIGHT + 2;
                  }
               }
            } catch (Exception e) {
               Utils.sendMessage("&cAn error occurred rendering HUD. check your logs");
               e.printStackTrace();
            }

            if (HUD.outline.getInput() == 1.0) {
               RenderUtils.drawRect(
                  lastXPos - 2,
                  n - 1,
                  lastXPos + this.mc.fontRendererObj.getStringWidth(previousModule) + 1.5,
                  n,
                  Theme.getGradient((int)HUD.theme.getInput(), n2)
               );
            }

            return new int[]{this.miX + longestModule, n, this.miX - longestModule};
         }
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
            HUD.posX = 5;
            this.aX = 5;
            HUD.posY = 70;
            this.aY = 70;
         }
      }

      public boolean doesGuiPauseGame() {
         return false;
      }

      private boolean empty() {
         for (Module module : ModuleManager.organizedModules) {
            if (module.isEnabled() && !module.getName().equals("HUD") && !module.isHidden() && module != ModuleManager.commandLine) {
               return false;
            }
         }

         return true;
      }
   }
}
