package keystrokesmod.module.impl.minigames;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoublePlant;
import net.minecraft.block.BlockDropper;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class SpeedBuilders extends Module {
   private SliderSetting placeDelay;
   private ButtonSetting antiMiss;
   private ButtonSetting autoPlace;
   private ButtonSetting autoSwap;
   private ButtonSetting hoverPlace;
   private ButtonSetting infoHud;
   private ButtonSetting renderBlocks;
   private ButtonSetting renderOnlyPlaceable;
   private ConcurrentHashMap<BlockPos, SpeedBuilders.BuildBlockInfo> buildInfo = new ConcurrentHashMap<>();
   private BlockPos platformCenter;
   private boolean listenForPacket;
   public List<BlockPos> platformPositions = Arrays.asList(
      new BlockPos(45, 71, -18),
      new BlockPos(-16, 71, 45),
      new BlockPos(18, 71, 45),
      new BlockPos(45, 71, 16),
      new BlockPos(-18, 71, -45),
      new BlockPos(-45, 71, -16),
      new BlockPos(-45, 71, 18),
      new BlockPos(16, 71, -45)
   );
   private int highlightColor = new Color(31, 255, 22, 44).getRGB();
   private int notPlaceableColor = new Color(184, 255, 183, 30).getRGB();
   private boolean doneCollecting;
   private double blockCount;
   private int lastPlaceTick = 0;
   private boolean eliminated;

   public SpeedBuilders() {
      super("Speed Builders", Module.category.minigames);
      this.registerSetting(new DescriptionSetting("Middle click to toggle auto."));
      this.registerSetting(this.placeDelay = new SliderSetting("Place delay", " tick", 0.5, 0.0, 10.0, 0.5));
      this.registerSetting(this.antiMiss = new ButtonSetting("Anti miss", false));
      this.registerSetting(this.autoPlace = new ButtonSetting("Auto place", false));
      this.registerSetting(this.autoSwap = new ButtonSetting("Auto swap", true));
      this.registerSetting(this.hoverPlace = new ButtonSetting("Hover place", true));
      this.registerSetting(this.infoHud = new ButtonSetting("Info HUD", true));
      this.registerSetting(this.renderBlocks = new ButtonSetting("Render blocks", true));
      this.registerSetting(this.renderOnlyPlaceable = new ButtonSetting("Render only placeable", false));
   }

   @Override
   public void onDisable() {
      this.lastPlaceTick = 0;
   }

   @SubscribeEvent
   public void onPreUpdate(PreUpdateEvent e) {
      int gameStatus = this.getGameStatus();
      if (gameStatus != -1 && this.platformCenter != null) {
         if (gameStatus == 4) {
            this.doneCollecting = true;
         }

         if (gameStatus == 1 && !this.doneCollecting) {
            this.buildInfo = this.getBuildInfo(this.platformCenter);
            if (!this.buildInfo.isEmpty()) {
               this.blockCount = this.buildInfo.size();
            }
         }

         if (gameStatus == 2) {
            this.doneCollecting = false;

            for (Entry<BlockPos, SpeedBuilders.BuildBlockInfo> entry : this.buildInfo.entrySet()) {
               IBlockState currentState = mc.theWorld.getBlockState(entry.getKey());
               IBlockState requiredState = entry.getValue().requiredState;
               if (currentState == null || requiredState == null) {
                  entry.getValue().isPlaced = false;
               } else if (!currentState.equals(requiredState)
                  && (!(requiredState.getBlock() instanceof BlockLeaves) || !currentState.getBlock().equals(requiredState.getBlock()))) {
                  entry.getValue().isPlaced = false;
               } else {
                  entry.getValue().isPlaced = true;
               }
            }

            if (this.getLookInfo() != null) {
               MovingObjectPosition mop = this.getLookInfo();
               if (mop.sideHit != null) {
                  BlockPos targetPos = mop.getBlockPos();
                  BlockPos facePos = targetPos.offset(mop.sideHit);
                  SpeedBuilders.BuildBlockInfo info = this.buildInfo.get(facePos);
                  if (info != null && !info.isPlaced) {
                     if (this.autoSwap.isToggled()) {
                        int requiredMeta = info.requiredState.getBlock().getMetaFromState(info.requiredState);
                        int slot = this.getSlot(info.requiredState.getBlock(), requiredMeta);
                        if (slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
                           mc.thePlayer.inventory.currentItem = slot;
                        }
                     }

                     if (this.hoverPlace.isToggled()
                        && this.holdingSameBlock(info.requiredState)
                        && this.correctPlaceState(info.requiredState, targetPos, mop.sideHit, mop.hitVec, mc.thePlayer.getHeldItem())) {
                        if (this.lastPlaceTick++ < this.placeDelay.getInput()) {
                           return;
                        }

                        ((IAccessorMinecraft)mc).callRightClickMouse();
                        this.lastPlaceTick = 0;
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onMouse(MouseEvent e) {
      if (e.buttonstate && Utils.nullCheck() && mc.currentScreen == null) {
         if (e.button == 1 && this.antiMiss.isToggled() && this.getLookInfo() != null && this.getGameStatus() == 2) {
            MovingObjectPosition mop = this.getLookInfo();
            if (mop.sideHit != null) {
               BlockPos targetPos = mop.getBlockPos();
               BlockPos facePos = targetPos.offset(mop.sideHit);
               SpeedBuilders.BuildBlockInfo info = this.buildInfo.get(facePos);
               if (info == null
                  || !this.holdingSameBlock(info.requiredState)
                  || !this.correctPlaceState(info.requiredState, targetPos, mop.sideHit, mop.hitVec, mc.thePlayer.getHeldItem())) {
                  e.setCanceled(true);
               }
            }
         } else if (e.button == 2) {
            if (this.autoSwap.isToggled()) {
               this.autoSwap.disable();
               this.hoverPlace.disable();
            } else {
               this.autoSwap.enable();
               this.hoverPlace.enable();
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderWorld(RenderWorldLastEvent ev) {
      if (Utils.nullCheck() && this.getGameStatus() == 2 && this.renderBlocks.isToggled()) {
         for (Entry<BlockPos, SpeedBuilders.BuildBlockInfo> buildData : this.buildInfo.entrySet()) {
            SpeedBuilders.BuildBlockInfo info = buildData.getValue();
            if (!info.isPlaced && this.holdingSameBlock(info.requiredState)) {
               BlockPos pos = buildData.getKey();
               boolean useWhite = true;

               for (EnumFacing dir : EnumFacing.values()) {
                  BlockPos neighborPos = pos.offset(dir);
                  if (BlockUtils.getBlock(neighborPos) != Blocks.air) {
                     useWhite = false;
                  }
               }

               if (!this.renderOnlyPlaceable.isToggled() || !useWhite) {
                  RenderUtils.renderBlockModel(
                     buildData.getValue().requiredState,
                     pos.getX(),
                     pos.getY(),
                     pos.getZ(),
                     useWhite ? this.notPlaceableColor : this.highlightColor
                  );
               }
            }
         }
      }
   }

   @SubscribeEvent
   public void onRenderTick(RenderTickEvent e) {
      if (e.phase == Phase.END && Utils.nullCheck() && mc.currentScreen == null) {
         int gameStatus = this.getGameStatus();
         if (this.infoHud.isToggled()) {
            List<String> lines = new ArrayList<>();
            lines.add("§6Speed Builders");
            lines.add(
               "§7Status: §b"
                  + (gameStatus != 1 && gameStatus != 4 ? (gameStatus == 2 ? "Building" : (gameStatus == 3 ? "Judging" : "§cDisabled")) : "Showing")
            );
            if (gameStatus == 2 && !this.eliminated) {
               double placedCount = 0.0;

               for (SpeedBuilders.BuildBlockInfo info : this.buildInfo.values()) {
                  if (info.isPlaced) {
                     placedCount++;
                  }
               }

               double percentage = 0.0;
               if (this.buildInfo.isEmpty()) {
                  percentage = 100.0;
                  placedCount = this.blockCount;
               } else if (this.blockCount > 0.0) {
                  percentage = placedCount / this.blockCount * 100.0;
               }

               lines.add("§7Progress: §b" + (int)placedCount + "§7/§b" + (int)this.blockCount + " " + Math.round(percentage) + "%");
            }

            lines.add("§7Auto: " + (this.autoEnabled() ? "§aENABLED" : "§cDISABLED"));
            int padding = 4;
            int maxWidth = 0;

            for (String line : lines) {
               int lineWidth = mc.fontRendererObj.getStringWidth(line);
               if (lineWidth > maxWidth) {
                  maxWidth = lineWidth;
               }
            }

            int lineHeight = mc.fontRendererObj.FONT_HEIGHT;
            int lineSpacing = 3;
            int totalHeight = lines.size() * lineHeight + (lines.size() - 1) * lineSpacing + padding * 2;
            int totalWidth = maxWidth + padding * 2;
            float x = -5.0F;
            float y = 110.0F;
            RenderUtils.drawRoundedRectangle(x, y, x + totalWidth + 7.0F, y + totalHeight - 2.0F, 7.0F, Utils.mergeAlpha(Color.black.getRGB(), 120));
            float textX = x + padding;
            float textY = y + padding;

            for (int i = 0; i < lines.size(); i++) {
               mc.fontRendererObj.drawString(lines.get(i), (int)(textX + 5.0F), (int)(textY + i * (lineHeight + lineSpacing)), -1);
            }
         }
      }
   }

   @SubscribeEvent
   public void onEntityJoin(EntityJoinWorldEvent e) {
      if (Utils.nullCheck() && e.entity != null) {
         if (e.entity == mc.thePlayer) {
            this.buildInfo.clear();
            this.platformCenter = null;
            this.listenForPacket = false;
            this.doneCollecting = false;
            this.eliminated = false;
         }
      }
   }

   @SubscribeEvent
   public void onChat(ClientChatReceivedEvent e) {
      if (e.type != 2 && Utils.nullCheck() && this.getGameStatus() != -1 && !this.listenForPacket) {
         String stripped = Utils.stripColor(e.message.getUnformattedText());
         if (!stripped.isEmpty()) {
            if (stripped.contains("Perfectly recreate the build you are shown each") || stripped.contains("The game starts in 1 second!")) {
               this.listenForPacket = true;
            }

            if (stripped.startsWith(Utils.getServerName()) && stripped.contains(" got a perfect build in ") && stripped.endsWith("s!")) {
               this.buildInfo.clear();
               this.doneCollecting = false;
            }

            if (stripped.startsWith("Player eliminated: " + Utils.getServerName()) && stripped.endsWith("%)")) {
               this.eliminated = true;
            }
         }
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (this.listenForPacket && Utils.nullCheck() && e.getPacket() instanceof S08PacketPlayerPosLook) {
         Vec3 setPos = new Vec3(
            ((S08PacketPlayerPosLook)e.getPacket()).getX(),
            ((S08PacketPlayerPosLook)e.getPacket()).getY(),
            ((S08PacketPlayerPosLook)e.getPacket()).getZ()
         );
         if (this.platformCenter == null) {
            this.platformCenter = this.findCenter(setPos);
         }

         this.listenForPacket = false;
      }
   }

   public int getGameStatus() {
      List<String> sidebar = Utils.getSidebarLines();
      if (sidebar != null && !sidebar.isEmpty()) {
         if (!Utils.stripColor(sidebar.get(0)).startsWith("BUILD BATTLE")) {
            return -1;
         }

         for (int i = 0; i < sidebar.size() - 1; i++) {
            String currentLine = Utils.stripColor(sidebar.get(i));
            String nextLine = Utils.stripColor(sidebar.get(i + 1));
            if (currentLine.startsWith("Round:")) {
               if (nextLine.startsWith("Starts In: 00:03") && Utils.stripColor(sidebar.get(i + 3)).startsWith("Theme:")) {
                  return 4;
               }

               if (nextLine.startsWith("Starts In:")) {
                  return 1;
               }

               if (nextLine.startsWith("Time Left:")) {
                  return 2;
               }

               if (nextLine.startsWith("Judging:")) {
                  return 3;
               }
            }
         }

         return 0;
      } else {
         return -1;
      }
   }

   public BlockPos findCenter(Vec3 position) {
      BlockPos closestPos = null;
      double closestDistSq = Double.MAX_VALUE;
      double maxDistance = 30.0;
      double maxDistSq = maxDistance * maxDistance;

      for (BlockPos pos : this.platformPositions) {
         double dx = pos.getX() - position.xCoord;
         double dy = pos.getY() - position.yCoord;
         double dz = pos.getZ() - position.zCoord;
         double distSq = Math.abs(dx * dx + dy * dy + dz * dz);
         if (distSq <= maxDistSq && distSq < closestDistSq) {
            closestDistSq = distSq;
            closestPos = pos;
         }
      }

      return closestPos;
   }

   public ConcurrentHashMap<BlockPos, SpeedBuilders.BuildBlockInfo> getBuildInfo(BlockPos centerPos) {
      ConcurrentHashMap<BlockPos, SpeedBuilders.BuildBlockInfo> blockInfo = new ConcurrentHashMap<>();
      int startX = centerPos.getX() - 3;
      int endX = centerPos.getX() + 3;
      int startZ = centerPos.getZ() - 3;
      int endZ = centerPos.getZ() + 3;
      int startY = centerPos.getY() + 1;
      int endY = startY + 30;

      for (int x = startX; x <= endX; x++) {
         for (int z = startZ; z <= endZ; z++) {
            for (int y = startY; y <= endY; y++) {
               BlockPos currentPos = new BlockPos(x, y, z);
               IBlockState state = mc.theWorld.getBlockState(currentPos);
               if (state.getBlock() != Blocks.air) {
                  blockInfo.put(currentPos, new SpeedBuilders.BuildBlockInfo(state));
               }
            }
         }
      }

      return blockInfo;
   }

   public boolean autoEnabled() {
      return this.autoSwap.isToggled() && this.hoverPlace.isToggled();
   }

   public boolean holdingSameBlock(IBlockState requiredState) {
      if (mc.thePlayer != null && requiredState != null) {
         ItemStack heldItem = mc.thePlayer.getHeldItem();
         if (heldItem == null) {
            return false;
         }

         Item item = heldItem.getItem();
         Block requiredBlock = requiredState.getBlock();
         if ((requiredBlock == Blocks.water || requiredBlock == Blocks.flowing_water) && item == Items.water_bucket) {
            return true;
         }

         if (!(item instanceof ItemBlock)) {
            return false;
         }

         Block heldBlock = ((ItemBlock)item).getBlock();
         int heldMeta = heldItem.getItemDamage();
         int requiredMeta = requiredBlock.getMetaFromState(requiredState);
         if (requiredBlock == Blocks.leaves || requiredBlock == Blocks.leaves2) {
            requiredMeta &= 3;
            heldMeta &= 3;
         }

         if (this.removeMeta(heldBlock)) {
            heldMeta = 0;
            requiredMeta = 0;
         }

         return heldBlock == requiredBlock && heldMeta == requiredMeta;
      } else {
         return false;
      }
   }

   public MovingObjectPosition getLookInfo() {
      MovingObjectPosition movingObjectPosition = mc.objectMouseOver;
      return movingObjectPosition != null && movingObjectPosition.typeOfHit == MovingObjectType.BLOCK && movingObjectPosition.getBlockPos() != null
         ? mc.objectMouseOver
         : null;
   }

   private int getSlot(Block block, int meta) {
      if (this.removeMeta(block)) {
         meta = 0;
      }

      if (block == Blocks.leaves || block == Blocks.leaves2) {
         meta &= 3;
      }

      for (int i = 0; i < 9; i++) {
         ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
         if (itemStack != null && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
            Block invBlock = ((ItemBlock)itemStack.getItem()).getBlock();
            int invMeta = itemStack.getItemDamage();
            if (this.removeMeta(block)) {
               invMeta = 0;
            }

            if (invBlock == Blocks.leaves || invBlock == Blocks.leaves2) {
               invMeta &= 3;
            }

            if (invBlock == block && invMeta == meta) {
               return i;
            }
         }
      }

      return -1;
   }

   private boolean removeMeta(Block block) {
      return block instanceof BlockStairs
         || block instanceof BlockDoublePlant
         || block instanceof BlockFlower
         || block instanceof BlockSkull
         || block instanceof BlockLadder
         || block instanceof BlockPumpkin
         || block instanceof BlockCauldron
         || block instanceof BlockRail
         || block instanceof BlockRailBase
         || block instanceof BlockTripWireHook
         || block instanceof BlockTripWire
         || block instanceof BlockDispenser
         || block instanceof BlockDropper
         || block instanceof BlockHopper
         || block instanceof BlockTorch
         || block instanceof BlockButton
         || block instanceof BlockLever
         || block instanceof BlockTrapDoor
         || block instanceof BlockSlab;
   }

   private boolean correctPlaceState(IBlockState requiredState, BlockPos blockPos, EnumFacing enumFacing, Vec3 hitVec, ItemStack heldItem) {
      if (requiredState == null
         || blockPos == null
         || enumFacing == null
         || hitVec == null
         || heldItem == null
         || !(heldItem.getItem() instanceof ItemBlock)) {
         return false;
      }

      if (!(requiredState.getBlock() instanceof BlockLeaves) && !(requiredState.getBlock() instanceof BlockButton)) {
         ItemBlock itemBlock = (ItemBlock)heldItem.getItem();
         Block block = itemBlock.getBlock();
         int meta = heldItem.getItemDamage();
         Vec3 relativeHitVec = hitVec.subtract(new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
         IBlockState simulatedState = block.onBlockPlaced(
            mc.theWorld,
            blockPos,
            enumFacing,
            (float)relativeHitVec.xCoord,
            (float)relativeHitVec.yCoord,
            (float)relativeHitVec.zCoord,
            meta,
            mc.thePlayer
         );
         if (simulatedState == null) {
            return false;
         }

         if (simulatedState.getBlock() != requiredState.getBlock()) {
            return false;
         }

         int simulatedMeta = simulatedState.getBlock().getMetaFromState(simulatedState);
         int requiredMeta = requiredState.getBlock().getMetaFromState(requiredState);
         if (simulatedMeta != requiredMeta) {
            return false;
         }

         if (simulatedState.getProperties().containsKey(BlockDirectional.FACING)
            && requiredState.getProperties().containsKey(BlockDirectional.FACING)) {
            EnumFacing simulatedFacing = (EnumFacing)simulatedState.getValue(BlockDirectional.FACING);
            EnumFacing requiredFacing = (EnumFacing)requiredState.getValue(BlockDirectional.FACING);
            if (simulatedFacing != requiredFacing) {
               return false;
            }
         }

         if (simulatedState.getBlock() instanceof BlockStairs && requiredState.getBlock() instanceof BlockStairs) {
            EnumFacing simulatedFacing = (EnumFacing)simulatedState.getValue(BlockStairs.FACING);
            EnumFacing requiredFacing = (EnumFacing)requiredState.getValue(BlockStairs.FACING);
            EnumHalf simulatedHalf = (EnumHalf)simulatedState.getValue(BlockStairs.HALF);
            EnumHalf requiredHalf = (EnumHalf)requiredState.getValue(BlockStairs.HALF);
            if (simulatedFacing != requiredFacing || simulatedHalf != requiredHalf) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   class BuildBlockInfo {
      public IBlockState requiredState;
      public boolean isPlaced;

      public BuildBlockInfo(IBlockState state) {
         this.requiredState = state;
         this.isPlaced = false;
      }
   }
}
