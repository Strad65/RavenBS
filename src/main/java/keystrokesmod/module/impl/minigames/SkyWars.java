package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.event.UseItemEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SkyWars extends Module {
   public ButtonSetting strengthIndicator;
   public ButtonSetting onlyAuraHostileMobs;
   public ButtonSetting renderTimeWarp;
   public Map<EntityPlayer, Long> strengthPlayers = new HashMap<>();
   private Map<String, SkyWars.SpawnEggInfo> entitySpawnQueue = new LinkedHashMap<>();
   private Map<Vec3, Long> timeWarpPositions = new LinkedHashMap<>();
   public List<Integer> spawnedMobs = new ArrayList<>();
   private final int strengthColor = new Color(255, 0, 0).getRGB();
   private final int timeWarpColor = new Color(210, 0, 255, 64).getRGB();
   private String[] killMessages = new String[]{" by ", " to ", " with ", " of ", " from ", " knight ", " for "};
   private boolean thrownPearl;
   public static boolean isSkyWarsTeams = false;

   public SkyWars() {
      super("Sky Wars", Module.category.minigames);
      this.registerSetting(this.onlyAuraHostileMobs = new ButtonSetting("Only aura hostile mobs", true));
      this.registerSetting(this.renderTimeWarp = new ButtonSetting("Render time warp", true));
      this.registerSetting(this.strengthIndicator = new ButtonSetting("Strength indicator", true));
   }

   @Override
   public void onDisable() {
      this.clear();
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.strengthIndicator.isToggled() && Utils.nullCheck() && !this.strengthPlayers.isEmpty() && Utils.getSkyWarsStatus() == 2) {
         int customMode = this.getCustomMode();
         if (customMode != 2) {
            isSkyWarsTeams = customMode == 1;
            long duration = isSkyWarsTeams ? 2000L : 5000L;

            for (EntityPlayer entityPlayer : new ArrayList<>(this.strengthPlayers.keySet())) {
               long storedTime = this.strengthPlayers.get(entityPlayer);
               long timePassed = System.currentTimeMillis() - storedTime;
               if (timePassed >= duration || AntiBot.isBot(entityPlayer)) {
                  this.strengthPlayers.remove(entityPlayer);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent e) {
      if (e.type != 2 && Utils.nullCheck()) {
         String stripped = Utils.stripColor(e.message.getUnformattedText());
         if (!stripped.isEmpty()) {
            if (stripped.equals("You will be warped back in 3 seconds!") && this.thrownPearl) {
               this.timeWarpPositions
                  .put(new Vec3(mc.thePlayer.lastTickPosX, mc.thePlayer.lastTickPosY, mc.thePlayer.lastTickPosZ), System.currentTimeMillis());
               this.thrownPearl = false;
            } else {
               if (this.strengthIndicator.isToggled() && Utils.getSkyWarsStatus() == 2) {
                  if (this.getCustomMode() == 2) {
                     return;
                  }

                  if (stripped.endsWith(".") && Arrays.stream(this.killMessages).anyMatch(stripped::contains)) {
                     String[] parts = stripped.split(" ");

                     for (String part : parts) {
                        if (part.endsWith(".")) {
                           String name = part.substring(0, part.length() - 1);

                           for (EntityPlayer entity : mc.theWorld.playerEntities) {
                              if (entity.getName().trim().equals(name) && entity != mc.thePlayer) {
                                 this.strengthPlayers.put(entity, System.currentTimeMillis());
                                 break;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (Utils.nullCheck() && Utils.getSkyWarsStatus() == 2) {
         if (this.strengthIndicator.isToggled()) {
            for (EntityPlayer entityPlayer : this.strengthPlayers.keySet()) {
               if (!AntiBot.isBot(entityPlayer)) {
                  RenderUtils.renderEntity(entityPlayer, 2, 0.0, 0.0, this.strengthColor, false);
               }
            }
         }

         if (this.renderTimeWarp.isToggled()) {
            Iterator<Entry<Vec3, Long>> iterator = this.timeWarpPositions.entrySet().iterator();
            long currentTime = System.currentTimeMillis();

            while (iterator.hasNext()) {
               Entry<Vec3, Long> entry = iterator.next();
               Vec3 position = entry.getKey();
               long timeThrown = entry.getValue();
               if (currentTime - timeThrown >= 3050L) {
                  iterator.remove();
               } else {
                  RenderUtils.drawPlayerBoundingBox(position, this.timeWarpColor);
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.clear();
      } else if (e.entity != null) {
         if (Utils.getSkyWarsStatus() != 2) {
            return;
         }

         String entityClassName = e.entity.getClass().getSimpleName();
         if (this.entitySpawnQueue.containsKey(entityClassName)) {
            Vec3 spawnPosition = new Vec3(e.entity.posX, e.entity.posY, e.entity.posZ);
            SkyWars.SpawnEggInfo eggInfo = this.entitySpawnQueue.get(entityClassName);
            if (eggInfo.spawnPos.distanceTo(spawnPosition) > 3.0 || Utils.timeBetween(mc.thePlayer.ticksExisted, eggInfo.tickSpawned) > 60L) {
               return;
            }

            if (!this.entitySpawnQueue.remove(entityClassName, eggInfo)) {
               return;
            }

            this.spawnedMobs.add(e.entity.getEntityId());
         }
      }
   }

   @SubscribeEvent
   public void onSendPacket(SendPacketEvent e) {
      if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
         C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement)e.getPacket();
         if (p.getPlacedBlockDirection() != 255 && p.getStack() != null && p.getStack().getItem() != null) {
            if (!(p.getStack().getItem() instanceof ItemMonsterPlacer)) {
               return;
            }

            Class<? extends Entity> oclass = (Class<? extends Entity>)EntityList.stringToClassMapping.get(ItemMonsterPlacer.getEntityName(p.getStack()));
            if (oclass == null) {
               return;
            }

            this.entitySpawnQueue.put(oclass.getSimpleName(), new SkyWars.SpawnEggInfo(p.getPosition(), mc.thePlayer.ticksExisted));
         }
      }
   }

   @SubscribeEvent
   public void onUseItem(UseItemEvent e) {
      if (e.usedItemStack != null && e.usedItemStack.getItem() instanceof ItemEnderPearl && Utils.getSkyWarsStatus() == 2) {
         ItemStack stack = e.usedItemStack;
         if (Utils.stripString(stack.getDisplayName()).equals("Time Warp Pearl")) {
            this.thrownPearl = true;
         } else if (stack.getDisplayName().startsWith("§b§l")) {
            List<String> toolTip = stack.getTooltip(mc.thePlayer, true);
            if (toolTip != null && toolTip.size() > 1 && Utils.stripString(toolTip.get(1)).contains("Teleports you back to your")) {
               this.thrownPearl = true;
            }
         }
      }
   }

   private void clear() {
      this.strengthPlayers.clear();
      this.spawnedMobs.clear();
      this.entitySpawnQueue.clear();
      this.timeWarpPositions.clear();
      this.thrownPearl = false;
   }

   public static boolean onlyAuraHostiles() {
      return ModuleManager.skyWars != null
         && ModuleManager.skyWars.isEnabled()
         && ModuleManager.skyWars.onlyAuraHostileMobs.isToggled()
         && Utils.getSkyWarsStatus() == 2;
   }

   public int getCustomMode() {
      List<String> sidebar = Utils.getSidebarLines();
      if (sidebar != null && !sidebar.isEmpty()) {
         for (String line : sidebar) {
            line = Utils.stripColor(line);
            if (line.startsWith("Teams left: ")) {
               return 1;
            }

            if (line.startsWith("Lab: ")) {
               return 2;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   public static class SpawnEggInfo {
      public Vec3 spawnPos;
      public int tickSpawned;

      public SpawnEggInfo(BlockPos spawnPos, int tickSpawned) {
         this.spawnPos = new Vec3(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
         this.tickSpawned = tickSpawned;
      }
   }
}
