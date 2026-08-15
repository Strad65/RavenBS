package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.nio.FloatBuffer;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Score;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityRenderer;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.SkyWars;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderLivingEvent.Specials.Pre;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Nametags extends Module {
   private SliderSetting scale;
   private ButtonSetting autoScale;
   private ButtonSetting drawBackground;
   private ButtonSetting onlyRenderName;
   private ButtonSetting dropShadow;
   private ButtonSetting showDistance;
   private ButtonSetting showHealth;
   private ButtonSetting showHitsToKill;
   private ButtonSetting showInvis;
   private ButtonSetting removeTags;
   private ButtonSetting renderSelf;
   private ButtonSetting showArmor;
   private ButtonSetting showEnchants;
   private ButtonSetting showDurability;
   private ButtonSetting showStackSize;
   private ButtonSetting renderRawTexts;
   private int backGroundColor = new Color(0, 0, 0, 100).getRGB();
   private int friendColor = new Color(0, 255, 0, 255).getRGB();
   private int enemyColor = new Color(255, 0, 0, 255).getRGB();
   private double normalizedThreshold = 8.0;

   public Nametags() {
      super("Nametags", Module.category.render, 0);
      this.registerSetting(this.scale = new SliderSetting("Scale", 1.0, 0.1, 5.0, 0.1));
      this.registerSetting(this.autoScale = new ButtonSetting("Auto-scale", true));
      this.registerSetting(this.drawBackground = new ButtonSetting("Draw background", true));
      this.registerSetting(this.onlyRenderName = new ButtonSetting("Only render name", false));
      this.registerSetting(this.renderSelf = new ButtonSetting("Render self", false));
      this.registerSetting(this.dropShadow = new ButtonSetting("Drop shadow", true));
      this.registerSetting(this.showDistance = new ButtonSetting("Show distance", false));
      this.registerSetting(this.showHealth = new ButtonSetting("Show health", true));
      this.registerSetting(this.showHitsToKill = new ButtonSetting("Show hits to kill", false));
      this.registerSetting(this.showInvis = new ButtonSetting("Show invis", true));
      this.registerSetting(this.removeTags = new ButtonSetting("Remove tags", false));
      this.registerSetting(new DescriptionSetting("Armor settings"));
      this.registerSetting(this.showArmor = new ButtonSetting("Show armor", false));
      this.registerSetting(this.showEnchants = new ButtonSetting("Show enchants", true));
      this.registerSetting(this.showDurability = new ButtonSetting("Show durability", true));
      this.registerSetting(this.showStackSize = new ButtonSetting("Show stack size", true));
      this.registerSetting(new DescriptionSetting("Raw text settings"));
      this.registerSetting(this.renderRawTexts = new ButtonSetting("Render raw texts", false));
   }

   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void onRenderWorldLast(RenderWorldLastEvent ev) {
      if (Utils.nullCheck()) {
         if (!this.removeTags.isToggled()) {
            double interpolatedX;
            double interpolatedY;
            double interpolatedZ;
            if (mc.gameSettings.thirdPersonView > 0) {
               Vec3 thirdPersonPos = Utils.getCameraPos(ev.partialTicks);
               interpolatedX = thirdPersonPos.xCoord;
               interpolatedY = thirdPersonPos.yCoord;
               interpolatedZ = thirdPersonPos.zCoord;
            } else {
               interpolatedX = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * ev.partialTicks;
               interpolatedY = mc.thePlayer.lastTickPosY
                  + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * ev.partialTicks
                  + mc.thePlayer.getEyeHeight();
               interpolatedZ = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * ev.partialTicks;
            }

            long strengthDuration = SkyWars.isSkyWarsTeams ? 2L : 5L;
            ScaledResolution scaledResolution = new ScaledResolution(mc);

            for (EntityPlayer en : mc.theWorld.playerEntities) {
               if ((this.showInvis.isToggled() || !en.isInvisible())
                  && (en != mc.thePlayer || this.renderSelf.isToggled() && mc.gameSettings.thirdPersonView != 0)
                  && !en.getDisplayNameString().isEmpty()
                  && (en == mc.thePlayer || !AntiBot.isBot(en))
                  && RenderUtils.isInViewFrustum(en)) {
                  double playerX = en.lastTickPosX + (en.posX - en.lastTickPosX) * ev.partialTicks;
                  double playerY = en.lastTickPosY + (en.posY - en.lastTickPosY) * ev.partialTicks;
                  double playerZ = en.lastTickPosZ + (en.posZ - en.lastTickPosZ) * ev.partialTicks;
                  double renderHeightOffset = playerY
                     - mc.getRenderManager().viewerPosY
                     + (!en.isSneaking() ? en.height : en.height - 0.3)
                     + 0.294;
                  double heightOffset = playerY + (!en.isSneaking() ? en.height : en.height - 0.3) + 0.294;
                  ((IAccessorEntityRenderer)mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft)mc).getTimer().renderPartialTicks, 0);
                  // Read projection matrix: projBuf[5] = 1/tan(verticalFov/2)
                  // Baseline is user's normal FOV=100°, baseProjY = 1/tan(50°)
                  // zoom→smaller FOV→larger projY→fovScale>1→nametag scales up
                  FloatBuffer projBuf = BufferUtils.createFloatBuffer(16);
                  GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projBuf);
                  float baseProjY = (float)(1.0 / Math.tan(Math.toRadians(50.0))); // baseline: 100° FOV
                  float fovScale = projBuf.get(5) / baseProjY; // >1 when zoomed in
                  Vec3 screenCords = RenderUtils.convertTo2D(
                     scaledResolution.getScaleFactor(),
                     playerX - mc.getRenderManager().viewerPosX,
                     renderHeightOffset,
                     playerZ - mc.getRenderManager().viewerPosZ
                  );
                  if (screenCords != null) {
                     boolean inFrustum = screenCords.zCoord < 1.0003684;
                     if (inFrustum) {
                        mc.entityRenderer.setupOverlayRendering();
                        float scaleSetting = (float)this.scale.getInput();
                        float newScale = scaleSetting;
                        if (this.autoScale.isToggled()) {
                           double deltaX = Math.abs(interpolatedX - playerX);
                           if (deltaX < this.normalizedThreshold + 1.0) {
                              double deltaZ = Math.abs(interpolatedZ - playerZ);
                              if (deltaZ < this.normalizedThreshold + 1.0) {
                                 double deltaY = Math.abs(interpolatedY - heightOffset);
                                 if (deltaY < this.normalizedThreshold + 1.0) {
                                    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                                    if (distance < this.normalizedThreshold) {
                                       newScale = Math.max((float)(scaleSetting * (this.normalizedThreshold / distance)), scaleSetting);
                                    }
                                 }
                              }
                           }
                        } else {
                           double deltaX = Math.abs(interpolatedX - playerX);
                           double deltaZ = Math.abs(interpolatedZ - playerZ);
                           double deltaY = Math.abs(interpolatedY - heightOffset);
                           double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
                           newScale = (float)((this.scale.getInput() + 4.0) / distance);
                        }

                        // FOV compensation: zoom(FOV<70°) → fovScale>1 → nametag scales up to match
                        newScale *= fovScale;

                        String name;
                        if (this.renderRawTexts.isToggled()) {
                           // Raw mode: use the vanilla display name as-is, no extra content appended
                           name = en.getDisplayName().getFormattedText();
                        } else {
                           if (this.onlyRenderName.isToggled()) {
                              String formattedName = Utils.getFirstColorCode(en.getDisplayName().getFormattedText());
                              String colorSuffix = "";
                              if (formattedName.length() >= 2 && formattedName.startsWith("§")) {
                                 colorSuffix = formattedName;
                              }

                              name = colorSuffix + en.getName();
                           } else {
                              name = en.getDisplayName().getFormattedText();
                           }

                           if (this.showHealth.isToggled()) {
                              name = name + " " + Utils.getHealthStr(en, false);
                           }

                           if (this.showHitsToKill.isToggled()) {
                              name = name + " " + Utils.getHitsToKillStr(en, mc.thePlayer.getCurrentEquippedItem());
                           }

                           if (this.showDistance.isToggled()) {
                              int distance = Math.round(mc.thePlayer.getDistanceToEntity(en));
                              String color = "§";
                              if (distance < 8) {
                                 color = color + "c";
                              } else if (distance < 30) {
                                 color = color + "6";
                              } else if (distance < 60) {
                                 color = color + "e";
                              } else if (distance < 90) {
                                 color = color + "a";
                              } else {
                                 color = color + "2";
                              }

                              name = color + distance + "m§r " + name;
                           }

                           if (ModuleManager.skyWars.isEnabled()
                              && ModuleManager.skyWars.strengthIndicator.isToggled()
                              && !ModuleManager.skyWars.strengthPlayers.isEmpty()
                              && ModuleManager.skyWars.strengthPlayers.get(en) != null) {
                              double startTime = ModuleManager.skyWars.strengthPlayers.get(en).longValue();
                              double timePassed = (System.currentTimeMillis() - startTime) / 1000.0;
                              double strengthRemaining = Math.max(0.0, Utils.round(strengthDuration - timePassed, 1));
                              String strengthInfo = "§4" + Utils.asWholeNum(strengthRemaining) + "s§r ";
                              name = strengthInfo + name;
                           }
                        }

                        // In raw texts mode, also fetch the belowname scoreboard line (vanilla second row)
                        String scoreLine = null;
                        if (this.renderRawTexts.isToggled()) {
                           Scoreboard sb = en.getWorldScoreboard();
                           ScoreObjective belowNameObj = sb.getObjectiveInDisplaySlot(2);
                           if (belowNameObj != null && sb.entityHasObjective(en.getName(), belowNameObj)) {
                              Score score = sb.getValueFromObjective(en.getName(), belowNameObj);
                              scoreLine = score.getScorePoints() + " " + belowNameObj.getDisplayName();
                           }
                        }

                        int strWidth = mc.fontRendererObj.getStringWidth(name) / 2;
                        int scoreWidth = scoreLine != null ? mc.fontRendererObj.getStringWidth(scoreLine) / 2 : 0;
                        int bgHalfWidth = Math.max(strWidth, scoreWidth);
                        // When two lines exist, shift everything up by one line (10px) so the
                        // bottom of the block stays at the same position above the player's head.
                        int yShift = scoreLine != null ? -10 : 0;
                        int x1 = -bgHalfWidth - 1;
                        int y1 = -10 + yShift;
                        int x2 = bgHalfWidth + 1;
                        int y2 = (scoreLine != null ? 9 : -1) + yShift;
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(newScale, newScale, newScale);
                        GlStateManager.translate(screenCords.xCoord / newScale, screenCords.yCoord / newScale, 0.0);
                        if (this.drawBackground.isToggled()) {
                           RenderUtils.drawRect(x1, y1, x2, y2, this.backGroundColor);
                        }

                        if (Utils.isFriended(en)) {
                           RenderUtils.drawOutline(x1, y1, x2, y2, 2.0F, this.friendColor);
                        } else if (Utils.isEnemy(en)) {
                           RenderUtils.drawOutline(x1, y1, x2, y2, 2.0F, this.enemyColor);
                        }

                        mc.fontRendererObj.drawString(name, -strWidth, -9.0F + yShift, -1, this.dropShadow.isToggled());
                        if (scoreLine != null) {
                           // Second line (belowname score) rendered in vanilla red
                           mc.fontRendererObj.drawString(scoreLine, -scoreWidth, 1.0F + yShift, 0xFFFF5555, this.dropShadow.isToggled());
                        }
                        if (this.showArmor.isToggled() && !this.renderRawTexts.isToggled()) {
                           this.renderArmor(en);
                        }

                        GlStateManager.scale(1.0F, 1.0F, 1.0F);
                        GlStateManager.popMatrix();
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderLiving(Pre e) {
      if (e.entity instanceof EntityPlayer && (e.entity != mc.thePlayer || this.renderSelf.isToggled()) && e.entity.deathTime == 0) {
         EntityPlayer entityPlayer = (EntityPlayer)e.entity;
         if (!this.showInvis.isToggled() && entityPlayer.isInvisible()) {
            return;
         }

         if (entityPlayer.getDisplayNameString().isEmpty() || entityPlayer != mc.thePlayer && AntiBot.isBot(entityPlayer)) {
            return;
         }

         e.setCanceled(true);
      }
   }

   private void renderArmor(EntityPlayer e) {
      int pos = 0;

      for (ItemStack is : e.inventory.armorInventory) {
         if (is != null) {
            pos -= 8;
         }
      }

      if (e.getHeldItem() != null) {
         pos -= 8;
         ItemStack item = e.getHeldItem().copy();
         if (item.hasEffect() && (item.getItem() instanceof ItemTool || item.getItem() instanceof ItemArmor)) {
            item.stackSize = 1;
         }

         this.renderItemStack(item, pos, -20);
         pos += 16;
      }

      for (int i = 3; i >= 0; i--) {
         ItemStack stack = e.inventory.armorInventory[i];
         if (stack != null) {
            this.renderItemStack(stack, pos, -20);
            pos += 16;
         }
      }
   }

   private void renderItemStack(ItemStack stack, int xPos, int yPos) {
      GlStateManager.pushMatrix();
      GlStateManager.disableAlpha();
      mc.getRenderItem().zLevel = -150.0F;
      GlStateManager.enableDepth();
      RenderHelper.enableGUIStandardItemLighting();
      mc.getRenderItem().renderItemAndEffectIntoGUI(stack, xPos, yPos - 8);
      mc.getRenderItem().zLevel = 0.0F;
      GlStateManager.disableDepth();
      GlStateManager.scale(0.5, 0.5, 0.5);
      GlStateManager.translate(0.0F, -10.0F, 0.0F);
      this.renderText(stack, xPos, yPos);
      GlStateManager.enableDepth();
      GlStateManager.scale(2.0F, 2.0F, 2.0F);
      GlStateManager.enableAlpha();
      GlStateManager.popMatrix();
   }

   private void renderText(ItemStack stack, int xPos, int yPos) {
      int newYPos = yPos - 24;
      if (this.showDurability.isToggled() && stack.getItem() instanceof ItemArmor) {
         int remainingDurability = stack.getMaxDamage() - stack.getItemDamage();
         mc.fontRendererObj.drawString(String.valueOf(remainingDurability), xPos * 2, yPos, 16777215, this.dropShadow.isToggled());
      }

      if (this.showEnchants.isToggled()
         && stack.getEnchantmentTagList() != null
         && stack.getEnchantmentTagList().tagCount() < 6
         && (
            stack.getItem() instanceof ItemTool
               || stack.getItem() instanceof ItemSword
               || stack.getItem() instanceof ItemBow
               || stack.getItem() instanceof ItemArmor
         )) {
         NBTTagList nbttaglist = stack.getEnchantmentTagList();

         for (int i = 0; i < nbttaglist.tagCount(); i++) {
            int id = nbttaglist.getCompoundTagAt(i).getShort("id");
            int lvl = nbttaglist.getCompoundTagAt(i).getShort("lvl");
            if (lvl > 0) {
               String abbreviated = this.getEnchantmentAbbreviated(id);
               mc.fontRendererObj.drawString(abbreviated + lvl, xPos * 2, newYPos, -1, this.dropShadow.isToggled());
               newYPos += 8;
            }
         }
      }

      if (this.showStackSize.isToggled()
         && !(stack.getItem() instanceof ItemSword)
         && !(stack.getItem() instanceof ItemBow)
         && !(stack.getItem() instanceof ItemTool)
         && !(stack.getItem() instanceof ItemArmor)) {
         mc.fontRendererObj.drawString(stack.stackSize + "x", xPos * 2, yPos, -1, this.dropShadow.isToggled());
      }
   }

   private String getEnchantmentAbbreviated(int id) {
      switch (id) {
         case 0:
            return "pt";
         case 1:
            return "frp";
         case 2:
            return "ff";
         case 3:
            return "blp";
         case 4:
            return "prp";
         case 5:
            return "thr";
         case 6:
            return "res";
         case 7:
            return "aa";
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 22:
         case 23:
         case 24:
         case 25:
         case 26:
         case 27:
         case 28:
         case 29:
         case 30:
         case 31:
         case 36:
         case 37:
         case 38:
         case 39:
         case 40:
         case 41:
         case 42:
         case 43:
         case 44:
         case 45:
         case 46:
         case 47:
         default:
            return null;
         case 16:
            return "sh";
         case 17:
            return "smt";
         case 18:
            return "ban";
         case 19:
            return "kb";
         case 20:
            return "fa";
         case 21:
            return "lot";
         case 32:
            return "eff";
         case 33:
            return "sil";
         case 34:
            return "ub";
         case 35:
            return "for";
         case 48:
            return "pow";
         case 49:
            return "pun";
         case 50:
            return "flm";
         case 51:
            return "inf";
      }
   }
}
