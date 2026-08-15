package keystrokesmod.module.impl.minigames;

import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoRequeue extends Module {
   private SliderSetting delay;
   private String receivedMessage = "";
   private long receiveTime = 0L;

   public AutoRequeue() {
      super("AutoRequeue", Module.category.minigames);
      this.registerSetting(new DescriptionSetting("Automatically requeues games."));
      this.registerSetting(this.delay = new SliderSetting("Delay", " second", 0.5, 0.0, 5.0, 0.1));
      this.closetModule = true;
   }

   @Override
   public void onDisable() {
      this.receivedMessage = "";
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (!this.receivedMessage.isEmpty() && System.currentTimeMillis() - this.receiveTime >= this.delay.getInput() * 1000.0) {
         mc.thePlayer.sendChatMessage(this.receivedMessage);
         this.receivedMessage = "";
      }
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent e) {
      if (e.type != 2 && Utils.nullCheck()) {
         String stripped = Utils.stripColor(e.message.getUnformattedText());
         if (!stripped.isEmpty() && stripped.contains("play again")) {
            if (e.message != null) {
               for (IChatComponent component : e.message.getSiblings()) {
                  if (component != null
                     && component.getFormattedText().contains("Click here")
                     && component.getChatStyle() != null
                     && component.getChatStyle().getChatClickEvent() != null
                     && component.getChatStyle().getChatClickEvent().getValue().startsWith("/")) {
                     this.receivedMessage = component.getChatStyle().getChatClickEvent().getValue();
                     this.receiveTime = System.currentTimeMillis();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.receivedMessage = "";
      }
   }
}
