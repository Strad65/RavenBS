package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class MurderMystery extends Module {
   private ButtonSetting alert;
   private ButtonSetting highlightMurderer;
   private ButtonSetting highlightBow;
   private ButtonSetting highlightInnocent;
   private ButtonSetting highlightDead;
   private ButtonSetting goldEsp;
   private final List<EntityPlayer> murderers = new ArrayList<>();
   private final List<EntityPlayer> hasBow = new ArrayList<>();
   private boolean override;
   private List<Item> murderItems = Arrays.asList(
      Items.iron_sword,
      Items.stone_sword,
      Items.iron_shovel,
      Items.stick,
      Items.wooden_axe,
      Items.wooden_sword,
      Items.stone_shovel,
      Items.blaze_rod,
      Items.diamond_shovel,
      Items.quartz,
      Items.pumpkin_pie,
      Items.golden_pickaxe,
      Items.apple,
      Items.name_tag,
      Items.carrot_on_a_stick,
      Items.bone,
      Items.carrot,
      Items.golden_carrot,
      Items.cookie,
      Items.diamond_axe,
      Items.prismarine_shard,
      Items.cooked_beef,
      Items.netherbrick,
      Items.cooked_chicken,
      Items.golden_sword,
      Items.diamond_sword,
      Items.diamond_hoe,
      Items.shears,
      Items.fish,
      Items.dye,
      Items.boat,
      Items.speckled_melon,
      Items.book,
      Item.getItemFromBlock(Blocks.double_plant),
      Item.getItemFromBlock(Blocks.sponge),
      Item.getItemFromBlock(Blocks.deadbush)
   );

   public MurderMystery() {
      super("Murder Mystery", Module.category.minigames);
      this.registerSetting(this.alert = new ButtonSetting("Alert murderer", true));
      this.registerSetting(this.highlightMurderer = new ButtonSetting("Highlight murderer", true));
      this.registerSetting(this.highlightBow = new ButtonSetting("Highlight bow", true));
      this.registerSetting(this.highlightInnocent = new ButtonSetting("Highlight innocent", true));
      this.registerSetting(this.highlightDead = new ButtonSetting("Highlight dead", true));
      this.registerSetting(this.goldEsp = new ButtonSetting("Gold ESP", true));
   }

   @Override
   public void onDisable() {
      this.clear();
   }

   @SubscribeEvent
   public void onRenderWordLast(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         if (!this.isMurderMystery()) {
            this.clear();
         } else {
            this.override = false;

            for (EntityPlayer en : mc.theWorld.playerEntities) {
               if (en != mc.thePlayer && !en.isInvisible() && (!AntiBot.isBot(en) || this.highlightDead.isToggled())) {
                  if (en.getHeldItem() != null) {
                     Item heldItem = en.getHeldItem().getItem();
                     if (this.murderItems.contains(heldItem)) {
                        if (!this.murderers.contains(en)) {
                           this.murderers.add(en);
                           if (this.alert.isToggled()) {
                              mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
                              Utils.sendMessage(
                                 "&eAlert: &b" + en.getName() + " &7is the &cmurderer&7! (&d" + (int)mc.thePlayer.getDistanceToEntity(en) + "m&7)"
                              );
                           }
                        }
                     } else if (heldItem instanceof ItemBow && this.highlightBow.isToggled() && !this.hasBow.contains(en)) {
                        this.hasBow.add(en);
                     }
                  }

                  this.override = true;
                  int rgb = Color.green.getRGB();
                  if (this.murderers.contains(en) && this.highlightMurderer.isToggled()) {
                     rgb = Color.red.getRGB();
                  } else if (this.hasBow.contains(en) && this.highlightBow.isToggled()) {
                     rgb = Color.orange.getRGB();
                  } else if (!this.highlightInnocent.isToggled()) {
                     continue;
                  }

                  if (this.highlightDead.isToggled() || !(this.getBoundingBoxVolume(en) <= 0.009)) {
                     RenderUtils.renderEntity(en, 2, 0.0, 0.0, rgb, false);
                  }
               }
            }

            if (!this.goldEsp.isToggled()) {
               return;
            }

            float renderPartialTicks = ((IAccessorMinecraft)mc).getTimer().renderPartialTicks;
            int n4 = -331703;

            for (Entity entity : mc.theWorld.loadedEntityList) {
               if (entity instanceof EntityItem && entity.ticksExisted >= 3) {
                  EntityItem entityItem = (EntityItem)entity;
                  if (entityItem.getEntityItem().stackSize != 0) {
                     Item getItem = entityItem.getEntityItem().getItem();
                     if (getItem != null) {
                        double n5 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * renderPartialTicks;
                        double n6 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * renderPartialTicks;
                        double n7 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * renderPartialTicks;
                        double n8 = mc.thePlayer.lastTickPosX
                           + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * renderPartialTicks
                           - n5;
                        double n9 = mc.thePlayer.lastTickPosY
                           + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * renderPartialTicks
                           - n6;
                        double n10 = mc.thePlayer.lastTickPosZ
                           + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * renderPartialTicks
                           - n7;
                        GlStateManager.pushMatrix();
                        this.drawBox(n4, n5, n6, n7, MathHelper.sqrt_double(n8 * n8 + n9 * n9 + n10 * n10));
                        GlStateManager.popMatrix();
                     }
                  }
               }
            }
         }
      }
   }

   public void drawBox(int n, double n4, double n5, double n6, double n7) {
      n4 -= mc.getRenderManager().viewerPosX;
      n5 -= mc.getRenderManager().viewerPosY;
      n6 -= mc.getRenderManager().viewerPosZ;
      GL11.glPushMatrix();
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3042);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      float n8 = (n >> 16 & 0xFF) / 255.0F;
      float n9 = (n >> 8 & 0xFF) / 255.0F;
      float n10 = (n & 0xFF) / 255.0F;
      float min = Math.min(Math.max(0.2F, (float)(0.01F * n7)), 0.4F);
      RenderUtils.drawBoundingBox(new AxisAlignedBB(n4 - min, n5, n6 - min, n4 + min, n5 + min * 2.0F, n6 + min), n8, n9, n10, 0.35F);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
      GL11.glPopMatrix();
   }

   private boolean isMurderMystery() {
      if (Utils.isHypixel()) {
         if (mc.thePlayer.getWorldScoreboard() == null || mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1) == null) {
            return false;
         }

         String d = mc.thePlayer.getWorldScoreboard().getObjectiveInDisplaySlot(1).getDisplayName();
         if (!d.contains("MURDER") && !d.contains("MYSTERY")) {
            return false;
         }

         for (String l : Utils.getScoreBoardOld()) {
            String s = Utils.stripColor(l);
            if (s.contains("Role:") || s.contains("Innocents Left:")) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean isEmpty() {
      return this.murderers.isEmpty() && this.hasBow.isEmpty() && !this.override;
   }

   private void clear() {
      this.override = false;
      this.murderers.clear();
      this.hasBow.clear();
   }

   private double getBoundingBoxVolume(Entity entity) {
      AxisAlignedBB boundingBox = entity.getEntityBoundingBox();
      if (boundingBox == null) {
         return 0.0;
      }

      double length = boundingBox.maxX - boundingBox.minX;
      double width = boundingBox.maxZ - boundingBox.minZ;
      double height = boundingBox.maxY - boundingBox.minY;
      return length * width * height;
   }
}
