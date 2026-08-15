package keystrokesmod.module.impl.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class ItemESP extends Module {
   private final ButtonSetting renderIron;
   private final ButtonSetting renderGold;

   public ItemESP() {
      super("ItemESP", Module.category.render);
      this.registerSetting(this.renderIron = new ButtonSetting("Render iron", true));
      this.registerSetting(this.renderGold = new ButtonSetting("Render gold", true));
   }

   @SubscribeEvent
   public void onRenderWorldLast(RenderWorldLastEvent e) {
      HashMap<Item, ArrayList<EntityItem>> itemsMap = new HashMap<>();
      HashMap<Double, Integer> colorMap = new HashMap<>();

      for (Entity entity : mc.theWorld.loadedEntityList) {
         if (entity instanceof EntityItem && entity.ticksExisted >= 3) {
            EntityItem entityItem = (EntityItem)entity;
            if (entityItem.getEntityItem().stackSize != 0) {
               Item currentItem = entityItem.getEntityItem().getItem();
               if (currentItem != null) {
                  int stackSize = entityItem.getEntityItem().stackSize;
                  double colorDouble = this.getColorForItem(currentItem, entity.posX, entity.posY, entity.posZ);
                  Integer existingStackCount = colorMap.get(colorDouble);
                  int newStackCount;
                  if (existingStackCount == null) {
                     newStackCount = stackSize;
                     ArrayList<EntityItem> itemList = itemsMap.get(currentItem);
                     if (itemList == null) {
                        itemList = new ArrayList<>();
                     }

                     itemList.add(entityItem);
                     itemsMap.put(currentItem, itemList);
                  } else {
                     newStackCount = existingStackCount + stackSize;
                  }

                  colorMap.put(colorDouble, newStackCount);
               }
            }
         }
      }

      if (!itemsMap.isEmpty()) {
         float renderPartialTicks = ((IAccessorMinecraft)mc).getTimer().renderPartialTicks;
         Iterator var30 = itemsMap.entrySet().iterator();

         while (true) {
            Entry<Item, ArrayList<EntityItem>> entry;
            Item item;
            int boxColor;
            int textColor;
            while (true) {
               if (!var30.hasNext()) {
                  return;
               }

               entry = (Entry<Item, ArrayList<EntityItem>>)var30.next();
               item = entry.getKey();
               if (item == Items.iron_ingot && this.renderIron.isToggled()) {
                  boxColor = -1;
                  textColor = -1;
                  break;
               }

               if (item == Items.gold_ingot && this.renderGold.isToggled()) {
                  boxColor = -331703;
                  textColor = -152;
                  break;
               }

               if (item == Items.diamond) {
                  boxColor = -10362113;
                  textColor = -7667713;
                  break;
               }

               if (item == Items.emerald) {
                  boxColor = -15216030;
                  textColor = -14614644;
                  break;
               }
            }

            for (EntityItem entityItem2 : entry.getValue()) {
               double itemColor = this.getColorForItem(item, entityItem2.posX, entityItem2.posY, entityItem2.posZ);
               double interpolatedX = entityItem2.lastTickPosX + (entityItem2.posX - entityItem2.lastTickPosX) * renderPartialTicks;
               double interpolatedY = entityItem2.lastTickPosY + (entityItem2.posY - entityItem2.lastTickPosY) * renderPartialTicks;
               double interpolatedZ = entityItem2.lastTickPosZ + (entityItem2.posZ - entityItem2.lastTickPosZ) * renderPartialTicks;
               EntityPlayer self = (EntityPlayer)(Freecam.freeEntity == null ? mc.thePlayer : Freecam.freeEntity);
               double diffX = self.lastTickPosX + (self.posX - self.lastTickPosX) * renderPartialTicks - interpolatedX;
               double diffY = self.lastTickPosY + (self.posY - self.lastTickPosY) * renderPartialTicks - interpolatedY;
               double diffZ = self.lastTickPosZ + (self.posZ - self.lastTickPosZ) * renderPartialTicks - interpolatedZ;
               double dist = MathHelper.sqrt_double(diffX * diffX + diffY * diffY + diffZ * diffZ);
               GlStateManager.pushMatrix();
               this.drawBox(boxColor, textColor, colorMap.get(itemColor), interpolatedX, interpolatedY, interpolatedZ, dist);
               GlStateManager.popMatrix();
            }
         }
      }
   }

   public double getColor(double x, double y, double z) {
      if (x == 0.0) {
         x = 1.0;
      }

      if (y == 0.0) {
         y = 1.0;
      }

      if (z == 0.0) {
         z = 1.0;
      }

      return Math.round((x + 1.0) * Math.floor(y) * (z + 2.0));
   }

   private double getColorForItem(Item item, double x, double y, double z) {
      double color = this.getColor(x, y, z);
      if (item == Items.iron_ingot) {
         color += 0.155;
      } else if (item == Items.gold_ingot) {
         color += 0.255;
      } else if (item == Items.diamond) {
         color += 0.355;
      } else if (item == Items.emerald) {
         color += 0.455;
      }

      return color;
   }

   public void drawBox(int boxColor, int textColor, int size, double posY, double posX, double posZ, double dist) {
      posY -= mc.getRenderManager().viewerPosX;
      posX -= mc.getRenderManager().viewerPosY;
      posZ -= mc.getRenderManager().viewerPosZ;
      GL11.glPushMatrix();
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3042);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      float r = (boxColor >> 16 & 0xFF) / 255.0F;
      float g = (boxColor >> 8 & 0xFF) / 255.0F;
      float b = (boxColor & 0xFF) / 255.0F;
      float radius = Math.min(Math.max(0.2F, (float)(0.01F * dist)), 0.4F);
      RenderUtils.drawBoundingBox(new AxisAlignedBB(posY - radius, posX, posZ - radius, posY + radius, posX + radius * 2.0F, posZ + radius), r, g, b, 0.35F);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
      GlStateManager.pushMatrix();
      GlStateManager.translate((float)posY, (float)posX + 0.3, (float)posZ);
      GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
      GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
      float scale = Math.min(Math.max(0.02266667F, (float)(0.0015F * dist)), 0.07F);
      GlStateManager.scale(-scale, -scale, -scale);
      GlStateManager.depthMask(false);
      GlStateManager.disableDepth();
      String value = String.valueOf(size);
      mc.fontRendererObj.drawString(value, -(mc.fontRendererObj.getStringWidth(value) / 2) + scale * 3.5F, -(123.805F * scale - 2.47494F), textColor, true);
      GlStateManager.enableDepth();
      GlStateManager.depthMask(true);
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      GlStateManager.popMatrix();
   }
}
