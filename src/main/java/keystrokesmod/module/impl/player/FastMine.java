package keystrokesmod.module.impl.player;

import keystrokesmod.mixin.impl.accessor.IAccessorPlayerControllerMP;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import org.lwjgl.input.Mouse;

public class FastMine extends Module {
   private SliderSetting delay;
   public SliderSetting multiplier;
   private SliderSetting mode;
   private ButtonSetting creativeDisable;
   private float lastCurBlockDamageMP;
   private String[] modes = new String[]{"Pre", "Post", "Increment"};

   public FastMine() {
      super("FastMine", Module.category.player);
      this.registerSetting(new DescriptionSetting("Vanilla is 5 delay & 1x speed."));
      this.registerSetting(this.delay = new SliderSetting("Break delay", " tick", 5.0, 0.0, 5.0, 1.0));
      this.registerSetting(this.multiplier = new SliderSetting("Break speed", "x", 1.0, 1.0, 2.0, 0.02));
      this.registerSetting(this.mode = new SliderSetting("Mode", 0, this.modes));
      this.registerSetting(this.creativeDisable = new ButtonSetting("Disable in creative", true));
      this.closetModule = true;
   }

   @Override
   public String getInfo() {
      return ((int)this.multiplier.getInput() == this.multiplier.getInput() ? (int)this.multiplier.getInput() + "" : this.multiplier.getInput())
         + this.multiplier.getSuffix();
   }

   @SubscribeEvent
   public void onPlayerTick(PlayerTickEvent e) {
      if (e.phase == Phase.END && mc.inGameHasFocus && Utils.nullCheck()) {
         if (!this.creativeDisable.isToggled() || !mc.thePlayer.capabilities.isCreativeMode) {
            int delay = (int)this.delay.getInput();
            if (delay < 5.0) {
               if (delay == 0) {
                  ((IAccessorPlayerControllerMP)mc.playerController).setBlockHitDelay(0);
               } else if (((IAccessorPlayerControllerMP)mc.playerController).getBlockHitDelay() > delay) {
                  ((IAccessorPlayerControllerMP)mc.playerController).setBlockHitDelay(delay);
               }
            }

            double multiplierInput = this.multiplier.getInput();
            if (multiplierInput > 1.0) {
               if (!mc.thePlayer.capabilities.isCreativeMode && Mouse.isButtonDown(0)) {
                  float curBlockDamage = ((IAccessorPlayerControllerMP)mc.playerController).getCurBlockDamageMP();
                  switch ((int)this.mode.getInput()) {
                     case 0:
                        float damage = (float)(1.0 - 1.0 / multiplierInput);
                        if (curBlockDamage > 0.0F && curBlockDamage < damage) {
                           ((IAccessorPlayerControllerMP)mc.playerController).setCurBlockDamageMP(damage);
                        }
                        break;
                     case 1:
                        double extra = 1.0 / multiplierInput;
                        if (curBlockDamage < 1.0F && curBlockDamage >= extra) {
                           ((IAccessorPlayerControllerMP)mc.playerController).setCurBlockDamageMP(1.0F);
                        }
                        break;
                     case 2:
                        float damage2 = -1.0F;
                        if (curBlockDamage < 1.0F) {
                           if (mc.objectMouseOver != null && curBlockDamage > this.lastCurBlockDamageMP) {
                              damage2 = (float)(
                                 this.lastCurBlockDamageMP
                                    + BlockUtils.getBlockHardness(
                                          mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock(),
                                          mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem),
                                          false,
                                          false
                                       )
                                       * (multiplierInput - 0.2152857 * (multiplierInput - 1.0))
                              );
                           }

                           if (damage2 != -1.0F && curBlockDamage > 0.0F) {
                              ((IAccessorPlayerControllerMP)mc.playerController).setCurBlockDamageMP(damage2);
                           }
                        }

                        this.lastCurBlockDamageMP = curBlockDamage;
                  }
               } else if (this.mode.getInput() == 2.0) {
                  this.lastCurBlockDamageMP = 0.0F;
               }
            }
         }
      }
   }
}
