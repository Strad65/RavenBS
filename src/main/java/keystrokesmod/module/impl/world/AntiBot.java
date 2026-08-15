package keystrokesmod.module.impl.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AntiBot extends Module {
   private static final HashMap<EntityPlayer, Long> entities = new HashMap<>();
   private static SliderSetting delay;
   private static SliderSetting pitSpawn;
   private static ButtonSetting tablist;
   private ButtonSetting printWorldJoin;

   public AntiBot() {
      super("AntiBot", Module.category.world, 0);
      this.registerSetting(delay = new SliderSetting("Delay", " second", true, -1.0, 0.5, 15.0, 0.5));
      this.registerSetting(pitSpawn = new SliderSetting("Pit spawn", true, -1.0, 70.0, 120.0, 1.0));
      this.registerSetting(tablist = new ButtonSetting("Tab list", false));
      this.registerSetting(this.printWorldJoin = new ButtonSetting("Print world join", false));
      this.closetModule = true;
   }

   @SubscribeEvent
   public void c(EntityJoinWorldEvent e) {
      if ((e.entity instanceof EntityPlayer || keystrokesmod.Raven.debug) && e.entity != mc.thePlayer) {
         if (delay.getInput() != -1.0 && e.entity instanceof EntityPlayer) {
            entities.put((EntityPlayer)e.entity, System.currentTimeMillis());
         }

         if (this.printWorldJoin.isToggled()) {
            Utils.sendMessage("&7Entity &b" + e.entity.getEntityId() + " &7joined: &r" + e.entity.getDisplayName().getFormattedText());
         }
      }
   }

   @Override
   public void onUpdate() {
      if (delay.getInput() != -1.0 && !entities.isEmpty()) {
         entities.values().removeIf(n -> n.longValue() < System.currentTimeMillis() - delay.getInput());
      }
   }

   @Override
   public void onDisable() {
      entities.clear();
   }

   public static boolean isBot(Entity entity) {
      if (!ModuleManager.antiBot.isEnabled()) {
         return false;
      }

      if (Freecam.freeEntity != null && Freecam.freeEntity == entity) {
         return true;
      }

      if (entity != null && entity instanceof EntityPlayer) {
         EntityPlayer entityPlayer = (EntityPlayer)entity;
         if (delay.getInput() != -1.0 && !entities.isEmpty() && entities.containsKey(entityPlayer)) {
            return true;
         }

         if (entityPlayer.isDead) {
            return true;
         }

         if (entityPlayer.getName().isEmpty()) {
            return true;
         }

         if (tablist.isToggled() && !getTablist().contains(entityPlayer.getName())) {
            return true;
         }

         if (entityPlayer.getHealth() != 20.0F && entityPlayer.getName().startsWith("§c")) {
            return true;
         }

         if (pitSpawn.getInput() != -1.0
            && entityPlayer.posY >= pitSpawn.getInput()
            && entityPlayer.posY <= 130.0
            && entityPlayer.getDistance(0.0, 114.0, 0.0) <= 25.0
            && Utils.isHypixel()) {
            List<String> sidebarLines = Utils.getSidebarLines();
            if (!sidebarLines.isEmpty() && Utils.stripColor(sidebarLines.get(0)).contains("THE HYPIXEL PIT")) {
               return true;
            }
         }

         if (entityPlayer.maxHurtTime == 0) {
            if (entityPlayer.getHealth() == 20.0F) {
               String unformattedText = entityPlayer.getDisplayName().getUnformattedText();
               if (unformattedText.length() == 10 && unformattedText.charAt(0) != 167) {
                  return true;
               }

               if (unformattedText.length() == 12 && entityPlayer.isPlayerSleeping() && unformattedText.charAt(0) == 167) {
                  return true;
               }

               if (unformattedText.length() >= 7 && unformattedText.charAt(2) == '[' && unformattedText.charAt(3) == 'N' && unformattedText.charAt(6) == ']') {
                  return true;
               }

               if (entityPlayer.getName().contains(" ")) {
                  return true;
               }
            } else if (entityPlayer.isInvisible()) {
               String unformattedText = entityPlayer.getDisplayName().getUnformattedText();
               if (unformattedText.length() >= 3 && unformattedText.charAt(0) == 167 && unformattedText.charAt(1) == 'c') {
                  return true;
               }
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static List<String> getTablist() {
      List<String> tab = new ArrayList<>();

      for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(true)) {
         if (networkPlayerInfo != null) {
            tab.add(networkPlayerInfo.getGameProfile().getName());
         }
      }

      return tab;
   }
}
