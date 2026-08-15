package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;

public class Gui extends Module {
   public static SliderSetting guiScale;
   public static SliderSetting backgroundBlur;
   public static SliderSetting scrollSpeed;
   public static ButtonSetting removePlayerModel;
   public static ButtonSetting darkBackground;
   public static ButtonSetting limitToScreen;
   public static ButtonSetting removeWatermark;
   public static ButtonSetting rainBowOutlines;

   public Gui() {
      super("Gui", Module.category.client, 54);
      this.registerSetting(guiScale = new SliderSetting("Gui scale", 1, new String[]{"Small", "Normal", "Large"}));
      this.registerSetting(backgroundBlur = new SliderSetting("Background blur", "%", 0.0, 0.0, 100.0, 1.0));
      this.registerSetting(scrollSpeed = new SliderSetting("Scroll speed", 50.0, 2.0, 90.0, 1.0));
      this.registerSetting(darkBackground = new ButtonSetting("Dark background", true));
      this.registerSetting(limitToScreen = new ButtonSetting("Limit to screen", false));
      this.registerSetting(rainBowOutlines = new ButtonSetting("Rainbow outlines", true));
      this.registerSetting(removePlayerModel = new ButtonSetting("Remove player model", false));
      this.registerSetting(removeWatermark = new ButtonSetting("Remove watermark", false));
   }

   @Override
   public void onEnable() {
      if (Utils.nullCheck() && mc.currentScreen != keystrokesmod.Raven.clickGui) {
         mc.displayGuiScreen(keystrokesmod.Raven.clickGui);
         keystrokesmod.Raven.clickGui.initMain();
      }

      this.disable();
   }
}
