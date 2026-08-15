package keystrokesmod.module.impl.render;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public class BedESP extends Module {
   public SliderSetting theme;
   private SliderSetting range;
   private SliderSetting rate;
   private ButtonSetting firstBed;
   private ButtonSetting renderFullBlock;
   private BlockPos[] bed;
   private Timer firstBedTimer;
   private Map<BlockPos[], Timer> beds = Collections.synchronizedMap(new HashMap<>());
   private long lastCheck = 0L;

   public BedESP() {
      super("BedESP", Module.category.render);
      this.registerSetting(this.theme = new SliderSetting("Theme", 0, Theme.themes));
      this.registerSetting(this.range = new SliderSetting("Range", 10.0, 2.0, 200.0, 2.0));
      this.registerSetting(this.rate = new SliderSetting("Rate", " second", 0.4, 0.1, 3.0, 0.1));
      this.registerSetting(this.firstBed = new ButtonSetting("Only render first bed", false));
      this.registerSetting(this.renderFullBlock = new ButtonSetting("Render full block", false));
   }

   @Override
   public void onUpdate() {
      if (!(System.currentTimeMillis() - this.lastCheck < this.rate.getInput() * 1000.0)) {
         this.lastCheck = System.currentTimeMillis();
         keystrokesmod.Raven.getCachedExecutor()
            .execute(
               () -> {
                  int i;
                  label59:
                  for (int n = i = (int)this.range.getInput(); i >= -n; i--) {
                     for (int j = -n; j <= n; j++) {
                        for (int k = -n; k <= n; k++) {
                           BlockPos blockPos = new BlockPos(
                              mc.thePlayer.posX + j, mc.thePlayer.posY + i, mc.thePlayer.posZ + k
                           );
                           IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
                           if (getBlockState.getBlock() == Blocks.bed
                              && getBlockState.getValue(BlockBed.PART) == EnumPartType.FOOT) {
                              if (this.firstBed.isToggled()) {
                                 if (this.bed != null && BlockUtils.isSamePos(blockPos, this.bed[0])) {
                                    return;
                                 }

                                 this.bed = new BlockPos[]{blockPos, blockPos.offset((EnumFacing)getBlockState.getValue(BlockBed.FACING))};
                                 return;
                              }

                              for (BlockPos[] pos : this.beds.keySet()) {
                                 if (BlockUtils.isSamePos(blockPos, pos[0])) {
                                    continue label59;
                                 }
                              }

                              this.beds
                                 .put(new BlockPos[]{blockPos, blockPos.offset((EnumFacing)getBlockState.getValue(BlockBed.FACING))}, null);
                           }
                        }
                     }
                  }
               }
            );
      }
   }

   @SubscribeEvent
   public void onEntityJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.beds.clear();
         this.bed = null;
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (Utils.nullCheck()) {
         float blockHeight = this.getBlockHeight();
         if (this.firstBed.isToggled() && this.bed != null) {
            float customAlpha = 0.25F;
            if (!this.isBed(this.bed[0])) {
               if (this.firstBedTimer == null) {
                  (this.firstBedTimer = new Timer(300.0F)).start();
               }

               int alpha = this.firstBedTimer == null ? 230 : 230 - this.firstBedTimer.getValueInt(0, 230, 1);
               if (alpha <= 0) {
                  this.bed = null;
                  return;
               }

               customAlpha = alpha / 255.0F;
            } else {
               this.firstBedTimer = null;
            }

            this.renderBed(this.bed, blockHeight, customAlpha);
            return;
         }

         synchronized (this.beds) {
            Iterator<Entry<BlockPos[], Timer>> iterator = this.beds.entrySet().iterator();

            while (iterator.hasNext()) {
               float customAlpha = 0.25F;
               Entry<BlockPos[], Timer> entry = iterator.next();
               BlockPos[] blockPos = entry.getKey();
               if (!this.isBed(blockPos[0])) {
                  if (entry.getValue() == null) {
                     entry.setValue(new Timer(300.0F));
                     entry.getValue().start();
                  }

                  int alpha = entry.getValue() == null ? 230 : 230 - entry.getValue().getValueInt(0, 230, 1);
                  if (alpha <= 0) {
                     iterator.remove();
                     continue;
                  }

                  customAlpha = alpha / 255.0F;
               } else {
                  entry.setValue(null);
               }

               this.renderBed(blockPos, blockHeight, customAlpha);
            }
         }
      }
   }

   @Override
   public void onDisable() {
      this.bed = null;
      this.beds.clear();
   }

   private void renderBed(BlockPos[] array, float height, float alpha) {
      double n = array[0].getX() - mc.getRenderManager().viewerPosX;
      double n2 = array[0].getY() - mc.getRenderManager().viewerPosY;
      double n3 = array[0].getZ() - mc.getRenderManager().viewerPosZ;
      GL11.glBlendFunc(770, 771);
      GL11.glEnable(3042);
      GL11.glLineWidth(2.0F);
      GL11.glDisable(3553);
      GL11.glDisable(2929);
      GL11.glDepthMask(false);
      int color = Theme.getGradient((int)this.theme.getInput(), 0.0);
      float a = (color >> 24 & 0xFF) / 255.0F;
      float r = (color >> 16 & 0xFF) / 255.0F;
      float g = (color >> 8 & 0xFF) / 255.0F;
      float b = (color & 0xFF) / 255.0F;
      GL11.glColor4d(r, g, b, a);
      AxisAlignedBB axisAlignedBB;
      if (array[0].getX() != array[1].getX()) {
         if (array[0].getX() > array[1].getX()) {
            axisAlignedBB = new AxisAlignedBB(n - 1.0, n2, n3, n + 1.0, n2 + height, n3 + 1.0);
         } else {
            axisAlignedBB = new AxisAlignedBB(n, n2, n3, n + 2.0, n2 + height, n3 + 1.0);
         }
      } else if (array[0].getZ() > array[1].getZ()) {
         axisAlignedBB = new AxisAlignedBB(n, n2, n3 - 1.0, n + 1.0, n2 + height, n3 + 1.0);
      } else {
         axisAlignedBB = new AxisAlignedBB(n, n2, n3, n + 1.0, n2 + height, n3 + 2.0);
      }

      RenderUtils.drawBoundingBox(axisAlignedBB, r, g, b, alpha);
      GL11.glEnable(3553);
      GL11.glEnable(2929);
      GL11.glDepthMask(true);
      GL11.glDisable(3042);
   }

   private float getBlockHeight() {
      return this.renderFullBlock.isToggled() ? 1.0F : 0.5625F;
   }

   public boolean isBed(BlockPos blockPos) {
      return mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockBed;
   }
}
