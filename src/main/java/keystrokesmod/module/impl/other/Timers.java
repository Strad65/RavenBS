package keystrokesmod.module.impl.other;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class Timers extends Module {
   private ButtonSetting consumables;
   public boolean isEnabled;
   private int a;
   private int f1;
   private int f2;
   private boolean d1;
   private boolean d2;
   private int consumeTicks = -1;
   private int bot = -1;
   private int consumeOffset;
   private int bo;

   public Timers() {
      super("Timers", Module.category.other);
      this.registerSetting(this.consumables = new ButtonSetting("Consumables", true));
      this.alwaysOn = true;
   }

   @Override
   public void onEnable() {
      this.isEnabled = true;
   }

   @Override
   public void onDisable() {
      this.isEnabled = false;
   }

   @SubscribeEvent
   public void onPacketSent(SendPacketEvent e) {
      if (Utils.nullCheck()) {
         if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && this.isConsumable()) {
            this.consumeTicks = 34;
         }
      }
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (Utils.nullCheck()) {
         if (mc.thePlayer.isUsingItem() && this.isConsumable()) {
            if (this.f1 == 0) {
               this.add();
               this.f1 = 1;
               this.consumeOffset = this.handleX();
            }

            if (this.consumeTicks > 1 && mc.thePlayer.ticksExisted % 2 == 0) {
               this.consumeTicks -= 2;
            }

            this.d1 = true;
         } else if (this.d1) {
            this.consumeTicks = -1;
            this.resetA();
            this.f1 = 0;
            this.d1 = false;
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (Utils.nullCheck() && this.isEnabled) {
         if (ev.phase != Phase.END || mc.currentScreen == null) {
            if (this.consumeTicks > -1 && this.consumables.isToggled()) {
               this.handleTimer(this.consumeTicks, "6", this.consumeOffset);
            }

            if (this.bot > -1) {
               this.handleTimer(this.bot, "c", this.bo);
            }
         }
      }
   }

   private boolean isConsumable() {
      if (mc.thePlayer.getHeldItem() == null) {
         return false;
      }

      Item heldItem = mc.thePlayer.getHeldItem().getItem();
      return heldItem == null
         ? false
         : heldItem instanceof ItemFood
            || heldItem instanceof ItemBucketMilk
            || heldItem instanceof ItemPotion && !ItemPotion.isSplash(mc.thePlayer.getHeldItem().getItemDamage());
   }

   private void add() {
      this.a++;
   }

   private int handleX() {
      int value = 0;
      if (this.a == 1) {
         value = 12;
      } else if (this.a == 2) {
         value = 38;
      }

      return value;
   }

   private void resetA() {
      this.f1 = this.f2 = 0;
      this.a = 0;
   }

   private void handleTimer(int ticks, String colorcode, int wo) {
      int color = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
      double s = ticks / 20.0;
      int eo = s >= 10.0 && wo == 12 ? 4 : 0;
      int widthOffset = wo;
      String text = "§" + colorcode + s + "s";
      int width = mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text) / 2;
      ScaledResolution scaledResolution = new ScaledResolution(mc);
      int[] display = new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
      mc.fontRendererObj.drawString(text, display[0] / 2 - width + widthOffset + eo, display[1] / 2 + 8, color, true);
   }
}
