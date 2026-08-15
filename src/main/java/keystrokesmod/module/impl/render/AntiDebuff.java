package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import net.minecraft.potion.Potion;

public class AntiDebuff extends Module {
   public ButtonSetting removeBlindness;
   public ButtonSetting removeNausea;
   public ButtonSetting removeSideEffects;

   public AntiDebuff() {
      super("AntiDebuff", Module.category.render);
      this.registerSetting(this.removeBlindness = new ButtonSetting("Remove blindness", true));
      this.registerSetting(this.removeNausea = new ButtonSetting("Remove nausea", true));
      this.registerSetting(this.removeSideEffects = new ButtonSetting("Remove side effects", false));
   }

   public boolean canRemoveBlindness(Potion potion) {
      return this.isEnabled() && potion == Potion.blindness && this.removeBlindness.isToggled();
   }

   public boolean canRemoveNausea(Potion potion) {
      return this.isEnabled() && potion == Potion.confusion && this.removeNausea.isToggled();
   }
}
