package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BedWars extends Module {
   public static ButtonSetting whitelistOwnBed;
   private ButtonSetting diamondArmor;
   private ButtonSetting fireball;
   private ButtonSetting enderPearl;
   private ButtonSetting obsidian;
   private ButtonSetting shouldPing;
   private BlockPos spawnPos;
   private boolean check;
   private boolean waitForRespawn;
   private long respawnMessageTime;
   public static boolean outsideSpawn = true;
   private List<String> armoredPlayer = new ArrayList<>();
   private Map<String, String> lastHeldMap = new ConcurrentHashMap<>();
   private Map<BlockPos, Long> obsidianPos = new HashMap<>();
   public List<SkyWars.SpawnEggInfo> entitySpawnQueue = new ArrayList<>();
   public List<Integer> spawnedMobs = new ArrayList<>();
   private int obsidianColor = new Color(106, 13, 173).getRGB();

   public BedWars() {
      super("Bed Wars", Module.category.minigames);
      this.registerSetting(whitelistOwnBed = new ButtonSetting("Whitelist own bed", true));
      this.registerSetting(new DescriptionSetting("Game alerts"));
      this.registerSetting(this.diamondArmor = new ButtonSetting("Diamond armor", true));
      this.registerSetting(this.fireball = new ButtonSetting("Fireball", false));
      this.registerSetting(this.obsidian = new ButtonSetting("Obsidian", true));
      this.registerSetting(this.enderPearl = new ButtonSetting("Ender pearl", true));
      this.registerSetting(this.shouldPing = new ButtonSetting("Should ping", true));
      this.closetModule = true;
   }

   @Override
   public void onEnable() {
      this.check = false;
      outsideSpawn = true;
   }

   @Override
   public void onDisable() {
      outsideSpawn = true;
      this.entitySpawnQueue.clear();
      this.spawnedMobs.clear();
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (Utils.nullCheck() && this.obsidian.isToggled()) {
         if (this.obsidianPos.isEmpty()) {
            return;
         }

         try {
            Iterator<Entry<BlockPos, Long>> iterator = this.obsidianPos.entrySet().iterator();

            while (iterator.hasNext()) {
               Entry<BlockPos, Long> entry = iterator.next();
               BlockPos blockPos = entry.getKey();
               Long receivedMs = entry.getValue();
               if (!(mc.theWorld.getBlockState(blockPos).getBlock() instanceof BlockObsidian)
                  && Utils.timeBetween(System.currentTimeMillis(), receivedMs) >= 500L) {
                  iterator.remove();
               } else {
                  RenderUtils.renderBlock(blockPos, this.obsidianColor, false, true);
               }
            }
         } catch (Exception var6) {
         }
      }
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.armoredPlayer.clear();
         this.lastHeldMap.clear();
         this.obsidianPos.clear();
         this.entitySpawnQueue.clear();
         this.spawnedMobs.clear();
         this.waitForRespawn = false;
      } else if (e.entity != null && e.entity instanceof EntityIronGolem) {
         if (Utils.getBedwarsStatus() != 2) {
            return;
         }

         Vec3 spawnPosition = new Vec3(e.entity.posX, e.entity.posY, e.entity.posZ);

         for (SkyWars.SpawnEggInfo eggInfo : this.entitySpawnQueue) {
            if (eggInfo.spawnPos.distanceTo(spawnPosition) > 3.0 || Utils.timeBetween(mc.thePlayer.ticksExisted, eggInfo.tickSpawned) > 60L) {
               return;
            }

            if (!this.entitySpawnQueue.remove(eggInfo)) {
               return;
            }

            this.spawnedMobs.add(e.entity.getEntityId());
         }
      }
   }

   @Override
   public void onUpdate() {
      if (Utils.getBedwarsStatus() == 2) {
         if (this.diamondArmor.isToggled() || this.enderPearl.isToggled() || this.obsidian.isToggled()) {
            for (EntityPlayer p : mc.theWorld.playerEntities) {
               if (p != null && p != mc.thePlayer && !AntiBot.isBot(p)) {
                  String name = p.getName();
                  ItemStack item = p.getHeldItem();
                  if (this.diamondArmor.isToggled()) {
                     ItemStack leggings = p.inventory.armorInventory[1];
                     if (!this.armoredPlayer.contains(name)
                        && p.inventory != null
                        && leggings != null
                        && leggings.getItem() != null
                        && leggings.getItem() == Items.diamond_leggings) {
                        this.armoredPlayer.add(name);
                        Utils.sendMessage("&eAlert: &r" + p.getDisplayName().getFormattedText() + " &7has purchased &bDiamond Armor");
                        this.ping();
                     }
                  }

                  if (item != null && !this.lastHeldMap.containsKey(name)) {
                     String itemType = this.getItemType(item);
                     if (itemType != null) {
                        this.lastHeldMap.put(name, itemType);
                        double distance = Math.round(mc.thePlayer.getDistanceToEntity(p));
                        this.handleAlert(itemType, p.getDisplayName().getFormattedText(), Utils.asWholeNum(distance));
                     }
                  } else if (this.lastHeldMap.containsKey(name)) {
                     String itemType = this.lastHeldMap.get(name);
                     if (!itemType.equals(this.getItemType(item))) {
                        this.lastHeldMap.remove(name);
                     }
                  }
               }
            }
         }

         if (whitelistOwnBed.isToggled()) {
            if (this.check) {
               this.spawnPos = mc.thePlayer.getPosition();
               this.check = false;
            }

            if (this.spawnPos == null) {
               outsideSpawn = true;
            } else {
               outsideSpawn = mc.thePlayer.getDistanceSq(this.spawnPos) > 800.0;
            }
         } else {
            outsideSpawn = true;
         }
      }
   }

   @SubscribeEvent
   public void onSendPacket(SendPacketEvent e) {
      if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
         C08PacketPlayerBlockPlacement p = (C08PacketPlayerBlockPlacement)e.getPacket();
         if (p.getPlacedBlockDirection() != 255
            && p.getStack() != null
            && p.getStack().getItem() != null
            && p.getStack().getItem() instanceof ItemMonsterPlacer) {
            Class<? extends Entity> oclass = (Class<? extends Entity>)EntityList.stringToClassMapping.get(ItemMonsterPlacer.getEntityName(p.getStack()));
            if (oclass == null) {
               return;
            }

            if (oclass.getSimpleName().equals("EntityIronGolem")) {
               this.entitySpawnQueue.add(new SkyWars.SpawnEggInfo(p.getPosition(), mc.thePlayer.ticksExisted));
            }
         }
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (e.getPacket() instanceof S23PacketBlockChange) {
         S23PacketBlockChange p = (S23PacketBlockChange)e.getPacket();
         if (p.getBlockState() != null && p.getBlockState().getBlock() instanceof BlockObsidian && this.isNextToBed(p.getBlockPosition())) {
            this.obsidianPos.put(p.getBlockPosition(), System.currentTimeMillis());
         }
      }
   }

   private boolean isNextToBed(BlockPos blockPos) {
      for (EnumFacing enumFacing : EnumFacing.values()) {
         BlockPos offset = blockPos.offset(enumFacing);
         if (BlockUtils.getBlock(offset) instanceof BlockBed) {
            return true;
         }
      }

      return false;
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent c) {
      if (Utils.nullCheck()) {
         String strippedMessage = Utils.stripColor(c.message.getUnformattedText());
         if (strippedMessage.startsWith(" ") && strippedMessage.contains("Protect your bed and destroy the enemy beds.")) {
            this.check = true;
            this.waitForRespawn = false;
         } else if (strippedMessage.equals("You will respawn because you still have a bed!")) {
            this.waitForRespawn = true;
            this.respawnMessageTime = System.currentTimeMillis();
         } else if (strippedMessage.equals("You have respawned!")
            && this.waitForRespawn
            && Utils.timeBetween(System.currentTimeMillis(), this.respawnMessageTime) <= 12000L) {
            this.check = true;
            this.waitForRespawn = false;
         }
      }
   }

   private String getItemType(ItemStack item) {
      if (item != null && item.getItem() != null) {
         String unlocalizedName = item.getItem().getUnlocalizedName();
         if (item.getItem() instanceof ItemEnderPearl && this.enderPearl.isToggled()) {
            return "&7an §3Ender Pearl";
         } else if (unlocalizedName.contains("tile.obsidian") && this.obsidian.isToggled()) {
            return "§dObsidian";
         } else {
            return item.getItem() instanceof ItemFireball && this.fireball.isToggled() ? "&7a §6Fireball" : null;
         }
      } else {
         return null;
      }
   }

   private void handleAlert(String itemType, String name, String info) {
      String alert = "&eAlert: &r" + name + " &7is holding " + itemType + " &7(§d" + info + "m&7)";
      Utils.sendMessage(alert);
      this.ping();
   }

   private void ping() {
      if (this.shouldPing.isToggled()) {
         Utils.ping();
      }
   }
}
