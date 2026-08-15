package keystrokesmod.module.impl.render;

import java.awt.Color;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Xray extends Module {
   private SliderSetting range;
   private SliderSetting rate;
   private ButtonSetting iron;
   private ButtonSetting gold;
   private ButtonSetting diamond;
   private ButtonSetting emerald;
   private ButtonSetting lapis;
   private ButtonSetting redstone;
   private ButtonSetting coal;
   private ButtonSetting spawner;
   private ButtonSetting obsidian;
   private Set<BlockPos> blocks = ConcurrentHashMap.newKeySet();
   private long lastCheck = 0L;

   public Xray() {
      super("Xray", Module.category.render);
      this.registerSetting(this.range = new SliderSetting("Range", 20.0, 5.0, 50.0, 1.0));
      this.registerSetting(this.rate = new SliderSetting("Rate", " second", 0.5, 0.1, 3.0, 0.1));
      this.registerSetting(this.coal = new ButtonSetting("Coal", true));
      this.registerSetting(this.diamond = new ButtonSetting("Diamond", true));
      this.registerSetting(this.emerald = new ButtonSetting("Emerald", true));
      this.registerSetting(this.gold = new ButtonSetting("Gold", true));
      this.registerSetting(this.iron = new ButtonSetting("Iron", true));
      this.registerSetting(this.lapis = new ButtonSetting("Lapis", true));
      this.registerSetting(this.obsidian = new ButtonSetting("Obsidian", true));
      this.registerSetting(this.redstone = new ButtonSetting("Redstone", true));
      this.registerSetting(this.spawner = new ButtonSetting("Spawner", true));
   }

   @Override
   public void onDisable() {
      this.blocks.clear();
   }

   @Override
   public void onUpdate() {
      if (!(System.currentTimeMillis() - this.lastCheck < this.rate.getInput() * 1000.0)) {
         this.lastCheck = System.currentTimeMillis();
         keystrokesmod.Raven.getCachedExecutor()
            .execute(
               () -> {
                  synchronized (this.blocks) {
                     int i;
                     for (int n = i = (int)this.range.getInput(); i >= -n; i--) {
                        for (int j = -n; j <= n; j++) {
                           for (int k = -n; k <= n; k++) {
                              BlockPos blockPos = new BlockPos(
                                 mc.thePlayer.posX + j, mc.thePlayer.posY + i, mc.thePlayer.posZ + k
                              );
                              if (!this.blocks.contains(blockPos)) {
                                 Block blockState = BlockUtils.getBlock(blockPos);
                                 if (blockState != null && this.canBreak(blockState)) {
                                    this.blocks.add(blockPos);
                                 }
                              }
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
         this.blocks.clear();
      }
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent ev) {
      if (Utils.nullCheck()) {
         synchronized (this.blocks) {
            if (!this.blocks.isEmpty()) {
               Iterator<BlockPos> iterator = this.blocks.iterator();

               while (iterator.hasNext()) {
                  BlockPos blockPos = iterator.next();
                  Block block = BlockUtils.getBlock(blockPos);
                  if (block != null && this.canBreak(block)) {
                     this.drawBox(blockPos);
                  } else {
                     iterator.remove();
                  }
               }
            }
         }
      }
   }

   private void drawBox(BlockPos p) {
      if (p != null) {
         int[] rgb = this.getColor(BlockUtils.getBlock(p));
         if (rgb[0] + rgb[1] + rgb[2] != 0) {
            RenderUtils.renderBlock(p, new Color(rgb[0], rgb[1], rgb[2]).getRGB(), false, true);
         }
      }
   }

   private int[] getColor(Block b) {
      int red = 0;
      int green = 0;
      int blue = 0;
      if (b.equals(Blocks.iron_ore)) {
         red = 255;
         green = 255;
         blue = 255;
      } else if (b.equals(Blocks.gold_ore)) {
         red = 255;
         green = 255;
      } else if (b.equals(Blocks.diamond_ore)) {
         green = 220;
         blue = 255;
      } else if (b.equals(Blocks.emerald_ore)) {
         red = 35;
         green = 255;
      } else if (b.equals(Blocks.lapis_ore)) {
         green = 50;
         blue = 255;
      } else if (b.equals(Blocks.redstone_ore)) {
         red = 255;
      } else if (b.equals(Blocks.mob_spawner)) {
         red = 30;
         blue = 135;
      }

      return new int[]{red, green, blue};
   }

   public boolean canBreak(Block block) {
      return this.iron.isToggled() && block.equals(Blocks.iron_ore)
         || this.gold.isToggled() && block.equals(Blocks.gold_ore)
         || this.diamond.isToggled() && block.equals(Blocks.diamond_ore)
         || this.emerald.isToggled() && block.equals(Blocks.emerald_ore)
         || this.lapis.isToggled() && block.equals(Blocks.lapis_ore)
         || this.redstone.isToggled() && block.equals(Blocks.redstone_ore)
         || this.coal.isToggled() && block.equals(Blocks.coal_ore)
         || this.spawner.isToggled() && block.equals(Blocks.mob_spawner)
         || this.obsidian.isToggled() && block.equals(Blocks.obsidian);
   }
}
