package keystrokesmod.module.impl.render;

import java.awt.Color;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChestESP extends Module {
   private SliderSetting red;
   private SliderSetting green;
   private SliderSetting blue;
   private ButtonSetting rainbow;
   private ButtonSetting outline;
   private ButtonSetting shade;
   private ButtonSetting disableIfOpened;

   public ChestESP() {
      super("ChestESP", Module.category.render, 0);
      this.registerSetting(this.red = new SliderSetting("Red", 0.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.green = new SliderSetting("Green", 0.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.blue = new SliderSetting("Blue", 255.0, 0.0, 255.0, 1.0));
      this.registerSetting(this.rainbow = new ButtonSetting("Rainbow", false));
      this.registerSetting(this.outline = new ButtonSetting("Outline", false));
      this.registerSetting(this.shade = new ButtonSetting("Shade", false));
      this.registerSetting(this.disableIfOpened = new ButtonSetting("Disable if opened", false));
   }

   @SubscribeEvent
   public void o(RenderWorldLastEvent ev) {
      if (Utils.nullCheck()) {
         int rgb = this.rainbow.isToggled()
            ? Utils.getChroma(2L, 0L)
            : new Color((int)this.red.getInput(), (int)this.green.getInput(), (int)this.blue.getInput()).getRGB();

         for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest) {
               if (!this.disableIfOpened.isToggled() || !(((TileEntityChest)tileEntity).lidAngle > 0.0F)) {
                  RenderUtils.renderChest(tileEntity.getPos(), rgb, this.outline.isToggled(), this.shade.isToggled());
               }
            } else if (tileEntity instanceof TileEntityEnderChest
               && (!this.disableIfOpened.isToggled() || !(((TileEntityEnderChest)tileEntity).lidAngle > 0.0F))) {
               RenderUtils.renderChest(tileEntity.getPos(), rgb, this.outline.isToggled(), this.shade.isToggled());
            }
         }
      }
   }
}
