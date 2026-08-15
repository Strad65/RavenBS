package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;

public class AutoSwap extends Module {
   public ButtonSetting sameType;
   public ButtonSetting spoofItem;
   public ButtonSetting swapToGreaterStack;
   public SliderSetting swapAt;
   public ButtonSetting legit;

   public AutoSwap() {
      super("AutoSwap", Module.category.player);
      this.registerSetting(new DescriptionSetting("Automatically swaps blocks."));
      this.registerSetting(this.sameType = new ButtonSetting("Only same type", false));
      this.registerSetting(this.spoofItem = new ButtonSetting("Spoof item", false));
      this.registerSetting(this.swapToGreaterStack = new ButtonSetting("Swap to greater stack", true));
      this.registerSetting(this.swapAt = new SliderSetting("Swap at", " blocks", 3.0, 1.0, 7.0, 1.0));
      this.registerSetting(this.legit = new ButtonSetting("Legit", false));
      this.canBeEnabled = false;
   }

   @Override
   public void guiUpdate() {
      this.swapAt.setVisible(!this.swapToGreaterStack.isToggled(), this);
   }
}
