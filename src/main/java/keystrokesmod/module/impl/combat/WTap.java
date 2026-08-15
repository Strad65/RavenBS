package keystrokesmod.module.impl.combat;

import java.util.HashMap;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class WTap extends Module {
   private SliderSetting delay;
   private SliderSetting hurttime;
   private SliderSetting chance;
   private ButtonSetting playersOnly;
   private final HashMap<Integer, Long> hits = new HashMap<>();
   public static boolean stopSprint = false;

   public WTap() {
      super("WTap", Module.category.combat);
      this.registerSetting(this.delay = new SliderSetting("Delay", "ms", 200.0, 0.0, 1000.0, 50.0));
      this.registerSetting(this.hurttime = new SliderSetting("Hurttime", 0.0, 0.0, 10.0, 1.0));
      this.registerSetting(this.chance = new SliderSetting("Chance", "%", 100.0, 0.0, 100.0, 1.0));
      this.registerSetting(this.playersOnly = new ButtonSetting("Players only", true));
      this.closetModule = true;
   }

   @Override
   public String getInfo() {
      return (int)this.delay.getInput() + "ms";
   }

   @SubscribeEvent
   public void onAttack(AttackEntityEvent event) {
      if (Utils.nullCheck() && event.entityPlayer == mc.thePlayer && mc.thePlayer.isSprinting()) {
         if (this.chance.getInput() != 0.0) {
            if (this.playersOnly.isToggled()) {
               if (!(event.target instanceof EntityPlayer)) {
                  return;
               }

               if (AntiBot.isBot(event.target)) {
                  return;
               }
            } else if (!(event.target instanceof EntityLivingBase)) {
               return;
            }

            if (((EntityLivingBase)event.target).deathTime == 0) {
               if (!(((EntityLivingBase)event.target).hurtTime > this.hurttime.getInput())) {
                  long currentMs = System.currentTimeMillis();
                  Long lastHit = this.hits.get(event.target.getEntityId());
                  if (lastHit == null || Utils.timeBetween(lastHit, currentMs) > (long)this.delay.getInput()) {
                     if (this.chance.getInput() != 100.0) {
                        double ch = Math.random();
                        if (ch >= this.chance.getInput() / 100.0) {
                           return;
                        }
                     }

                     this.hits.put(event.target.getEntityId(), currentMs);
                     stopSprint = true;
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDisable() {
      stopSprint = false;
      this.hits.clear();
   }
}
