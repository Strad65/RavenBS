package keystrokesmod.helper;

import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class DebugHelper {
   private static Minecraft mc = Minecraft.getMinecraft();
   public static boolean MIXIN;
   public static boolean BACKGROUND;

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent ev) {
      if (keystrokesmod.Raven.debug && ev.phase == Phase.END && Utils.nullCheck()) {
         if (mc.currentScreen == null) {
            RenderUtils.renderBPS(true, true);
         }
      }
   }

   public static void debugMixin(Object obj, String message) {
      if (MIXIN) {
         Utils.sendMessage("&d" + obj.getClass().getSimpleName() + "&7: " + message);
      }
   }
}
