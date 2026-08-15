package keystrokesmod.module.impl.other;

import java.util.HashMap;
import java.util.UUID;
import keystrokesmod.event.AntiCheatFlagEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PlayerData;
import keystrokesmod.utility.Utils;
import net.minecraft.block.BlockAir;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Anticheat extends Module {
   private SliderSetting interval;
   private ButtonSetting enemyAdd;
   private ButtonSetting autoReport;
   private ButtonSetting ignoreTeammates;
   private ButtonSetting atlasSuspect;
   private ButtonSetting shouldPing;
   private ButtonSetting autoBlock;
   private ButtonSetting noFall;
   private ButtonSetting noSlow;
   private ButtonSetting scaffold;
   private ButtonSetting legitScaffold;
   private HashMap<UUID, HashMap<ButtonSetting, Long>> flags = new HashMap<>();
   private HashMap<UUID, PlayerData> players = new HashMap<>();
   private long lastAlert;
   private long lastClientBoundPacket;

   public Anticheat() {
      super("Anticheat", Module.category.other);
      this.registerSetting(new DescriptionSetting("Tries to detect cheaters."));
      this.registerSetting(this.interval = new SliderSetting("Flag interval", " second", 20.0, 0.0, 60.0, 1.0));
      this.registerSetting(this.enemyAdd = new ButtonSetting("Add cheaters as enemy", false));
      this.registerSetting(this.autoReport = new ButtonSetting("Auto report", false));
      this.registerSetting(this.ignoreTeammates = new ButtonSetting("Ignore teammates", false));
      this.registerSetting(this.atlasSuspect = new ButtonSetting("Only atlas suspect", false));
      this.registerSetting(this.shouldPing = new ButtonSetting("Should ping", true));
      this.registerSetting(new DescriptionSetting("Detected cheats"));
      this.registerSetting(this.autoBlock = new ButtonSetting("Autoblock", true));
      this.registerSetting(this.noFall = new ButtonSetting("NoFall", true));
      this.registerSetting(this.noSlow = new ButtonSetting("NoSlow", true));
      this.registerSetting(this.scaffold = new ButtonSetting("Scaffold", true));
      this.registerSetting(this.legitScaffold = new ButtonSetting("Legit scaffold", true));
      this.closetModule = true;
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      this.lastClientBoundPacket = System.currentTimeMillis();
   }

   private void alert(EntityPlayer entityPlayer, ButtonSetting mode) {
      if (!Utils.isFriended(entityPlayer) && (!this.ignoreTeammates.isToggled() || !Utils.isTeammate(entityPlayer))) {
         if (this.atlasSuspect.isToggled()) {
            if (!entityPlayer.getName().equals("Suspect§r")) {
               return;
            }
         } else if (this.enemyAdd.isToggled()) {
            Utils.addEnemy(entityPlayer.getName());
         }

         long currentTimeMillis = System.currentTimeMillis();
         if (this.interval.getInput() > 0.0) {
            HashMap<ButtonSetting, Long> hashMap = this.flags.get(entityPlayer.getUniqueID());
            if (hashMap == null) {
               hashMap = new HashMap<>();
            } else {
               Long n = hashMap.get(mode);
               if (n != null && Utils.timeBetween(n, currentTimeMillis) <= this.interval.getInput() * 1000.0) {
                  return;
               }
            }

            hashMap.put(mode, currentTimeMillis);
            this.flags.put(entityPlayer.getUniqueID(), hashMap);
         }

         ChatComponentText chatComponentText = new ChatComponentText(
            Utils.formatColor("&7[&dR&7]&r " + entityPlayer.getDisplayName().getUnformattedText() + " &7detected for &d" + mode.getName())
         );
         ChatStyle chatStyle = new ChatStyle();
         chatStyle.setChatClickEvent(new ClickEvent(Action.RUN_COMMAND, "/wdr " + entityPlayer.getName()));
         chatComponentText.appendSibling(new ChatComponentText(Utils.formatColor(" §7[§cWDR§7]")).setChatStyle(chatStyle));
         mc.thePlayer.addChatMessage(chatComponentText);
         this.postAntiCheatFlagEvent(mode.getName(), entityPlayer);
         if (this.shouldPing.isToggled() && Utils.timeBetween(this.lastAlert, currentTimeMillis) >= 1500L) {
            mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
            this.lastAlert = currentTimeMillis;
         }

         if (this.autoReport.isToggled() && !Utils.isFriended(entityPlayer)) {
            mc.thePlayer.sendChatMessage("/wdr " + Utils.stripColor(entityPlayer.getGameProfile().getName()));
         }
      }
   }

   @Override
   public void onUpdate() {
      if (!mc.isSingleplayer()) {
         for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
            if (entityPlayer != null && entityPlayer != mc.thePlayer && !AntiBot.isBot(entityPlayer)) {
               PlayerData data = this.players.get(entityPlayer.getUniqueID());
               if (data == null) {
                  data = new PlayerData();
               }

               data.update(entityPlayer);
               this.performCheck(entityPlayer, data);
               data.updateServerPos(entityPlayer);
               data.updateSneak(entityPlayer);
               this.players.put(entityPlayer.getUniqueID(), data);
            }
         }
      }
   }

   @SubscribeEvent
   public void onEntityJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.players.clear();
         this.flags.clear();
      }
   }

   @Override
   public void onDisable() {
      this.players.clear();
      this.flags.clear();
      this.lastAlert = 0L;
   }

   private void performCheck(EntityPlayer entityPlayer, PlayerData playerData) {
      if (this.autoBlock.isToggled() && playerData.autoBlockTicks >= 10) {
         this.alert(entityPlayer, this.autoBlock);
      } else if (this.legitScaffold.isToggled() && playerData.sneakTicks >= 3) {
         this.alert(entityPlayer, this.legitScaffold);
      } else if (this.noSlow.isToggled() && playerData.noSlowTicks == 11 && playerData.speed >= 0.08) {
         this.alert(entityPlayer, this.noSlow);
      } else {
         if (this.scaffold.isToggled()
            && entityPlayer.isSwingInProgress
            && entityPlayer.rotationPitch >= 70.0F
            && entityPlayer.getHeldItem() != null
            && entityPlayer.getHeldItem().getItem() instanceof ItemBlock
            && playerData.fastTick >= 20
            && entityPlayer.ticksExisted - playerData.lastSneakTick >= 30
            && entityPlayer.ticksExisted - playerData.aboveVoidTicks >= 20) {
            boolean overAir = true;
            BlockPos blockPos = entityPlayer.getPosition().down(2);

            for (int i = 0; i < 4; i++) {
               if (!(BlockUtils.getBlock(blockPos) instanceof BlockAir)) {
                  overAir = false;
                  break;
               }

               blockPos = blockPos.down();
            }

            if (overAir) {
               this.alert(entityPlayer, this.scaffold);
               return;
            }
         }

         if (this.noFall.isToggled()
            && !entityPlayer.capabilities.isFlying
            && Utils.timeBetween(System.currentTimeMillis(), this.lastClientBoundPacket) <= 150L) {
            double serverPosX = entityPlayer.serverPosX / 32;
            double serverPosY = entityPlayer.serverPosY / 32;
            double serverPosZ = entityPlayer.serverPosZ / 32;
            double deltaX = Math.abs(playerData.serverPosX - serverPosX);
            double deltaY = playerData.serverPosY - serverPosY;
            double deltaZ = Math.abs(playerData.serverPosZ - serverPosZ);
            if (deltaY >= 5.0
               && deltaX <= 10.0
               && deltaZ <= 10.0
               && deltaY <= 40.0
               && !Utils.overVoid(serverPosX, serverPosY, serverPosZ)
               && Utils.distanceToGround(entityPlayer) > 3.0
               && !Utils.onLadder(entityPlayer)
               && !entityPlayer.isInWater()
               && !entityPlayer.isInLava()) {
               this.alert(entityPlayer, this.noFall);
            }
         }
      }
   }

   public void postAntiCheatFlagEvent(String flag, Entity entity) {
      MinecraftForge.EVENT_BUS.post(new AntiCheatFlagEvent(flag, entity));
   }
}
