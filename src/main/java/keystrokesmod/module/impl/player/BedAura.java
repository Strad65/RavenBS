package keystrokesmod.module.impl.player;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import keystrokesmod.event.ClientRotationEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.minigames.BedWars;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBed.EnumPartType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BedAura extends Module {
   public SliderSetting mode;
   private SliderSetting breakSpeed;
   private SliderSetting fov;
   public SliderSetting range;
   private SliderSetting rate;
   public ButtonSetting allowAura;
   private ButtonSetting breakNearBlock;
   private ButtonSetting cancelKnockback;
   private ButtonSetting disableBreakEffects;
   public ButtonSetting groundSpoof;
   private ButtonSetting onlyWhileVisible;
   private ButtonSetting renderOutline;
   private ButtonSetting sendAnimations;
   private ButtonSetting silentSwing;
   private String[] modes = new String[]{"Legit", "Instant", "Swap"};
   private BlockPos[] bedPos;
   private BlockPos packetPos;
   public float breakProgress;
   private int lastSlot = -1;
   public BlockPos currentBlock;
   public BlockPos lastBlock;
   private long lastCheck = 0L;
   public boolean stopAutoblock;
   public boolean breakTick;
   private int outlineColor = new Color(226, 65, 65).getRGB();
   private BlockPos nearestBlock;
   private Map<BlockPos, Float> breakProgressMap = new HashMap<>();
   public double lastProgress;
   public float vanillaProgress;
   private int defaultOutlineColor = new Color(226, 65, 65).getRGB();
   private BlockPos previousBlockBroken;
   private BlockPos rotateLastBlock;
   private boolean spoofGround;
   private boolean firstStop;
   private boolean isBreaking;
   private boolean startPacket;
   private boolean stopPacket;
   private boolean ignoreSlow;
   private boolean delayStop;
   private boolean ra;
   public boolean shouldUnblock;

   public BedAura() {
      super("BedAura", Module.category.player, 0);
      this.registerSetting(this.mode = new SliderSetting("Break mode", 0, this.modes));
      this.registerSetting(this.breakSpeed = new SliderSetting("Break speed", "x", 1.0, 1.0, 2.0, 0.01));
      this.registerSetting(this.fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
      this.registerSetting(this.range = new SliderSetting("Range", 4.5, 1.0, 8.0, 0.5));
      this.registerSetting(this.rate = new SliderSetting("Rate", " second", 0.2, 0.05, 3.0, 0.05));
      this.registerSetting(this.allowAura = new ButtonSetting("Allow aura", true));
      this.registerSetting(this.breakNearBlock = new ButtonSetting("Break near block", false));
      this.registerSetting(this.cancelKnockback = new ButtonSetting("Cancel knockback", false));
      this.registerSetting(this.disableBreakEffects = new ButtonSetting("Disable break effects", false));
      this.registerSetting(this.groundSpoof = new ButtonSetting("Ground spoof", false));
      this.registerSetting(this.onlyWhileVisible = new ButtonSetting("Only while visible", false));
      this.registerSetting(this.renderOutline = new ButtonSetting("Render block outline", true));
      this.registerSetting(this.sendAnimations = new ButtonSetting("Send animations", false));
      this.registerSetting(this.silentSwing = new ButtonSetting("Silent swing", false));
   }

   @Override
   public String getInfo() {
      return this.modes[(int)this.mode.getInput()];
   }

   @Override
   public void onDisable() {
      this.reset(true, true);
      this.bedPos = null;
      this.ra = false;
   }

   @SubscribeEvent
   public void onWorldJoin(EntityJoinWorldEvent e) {
      if (e.entity == mc.thePlayer) {
         this.reset(true, true);
         this.bedPos = null;
      }
   }

   @SubscribeEvent
   public void onReceivePacket(ReceivePacketEvent e) {
      if (Utils.nullCheck() && this.cancelKnockback.isToggled() && this.currentBlock != null) {
         if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity)e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
               e.setCanceled(true);
            }
         } else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onClientRotation(ClientRotationEvent e) {
      if (Utils.nullCheck()) {
         if (this.ra) {
            this.ra = false;
         }

         if (this.delayStop) {
            this.delayStop = false;
            if (this.shouldUnblock) {
               this.shouldUnblock = false;
               if (this.stopAutoblock && ModuleUtils.isBlocked) {
                  return;
               }
            }
         } else {
            this.stopAutoblock = false;
         }

         this.breakTick = false;
         if (this.currentBlock == null || !RotationUtils.inRange(this.currentBlock, this.range.getInput())) {
            this.reset(true, true);
            this.bedPos = null;
         }

         if (!Utils.isBedwarsPracticeOrReplay()) {
            if (ModuleManager.bedwars != null && ModuleManager.bedwars.isEnabled() && BedWars.whitelistOwnBed.isToggled() && !BedWars.outsideSpawn) {
               this.reset(true, true);
            } else if (mc.thePlayer.capabilities.allowEdit && !mc.thePlayer.isSpectator()) {
               if (this.bedPos == null) {
                  if (!this.isBreaking && System.currentTimeMillis() - this.lastCheck >= this.rate.getInput() * 1000.0) {
                     this.lastCheck = System.currentTimeMillis();
                     this.bedPos = this.getBedPos();
                  }

                  if (this.bedPos == null) {
                     this.reset(true, true);
                     return;
                  }
               } else if (!(BlockUtils.getBlock(this.bedPos[0]) instanceof BlockBed) || this.currentBlock != null && BlockUtils.replaceable(this.currentBlock)) {
                  this.reset(true, true);
                  return;
               }

               if (this.breakNearBlock.isToggled() && this.isCovered(this.bedPos[0]) && this.isCovered(this.bedPos[1])) {
                  if (this.nearestBlock == null) {
                     this.nearestBlock = this.getBestBlock(this.bedPos, true);
                  }

                  this.breakBlock(e, this.nearestBlock);
               } else {
                  this.nearestBlock = null;
                  this.breakBlock(e, this.bedPos[0]);
               }
            } else {
               this.reset(true, true);
            }
         }
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onPreMotion(PreMotionEvent e) {
      if (this.stopAutoblock && keystrokesmod.Raven.debug) {
         Utils.sendModuleMessage(this, "&7stopping autoblock (&3" + mc.thePlayer.ticksExisted + "&7).");
      }

      if (!mc.thePlayer.isInWater() && this.spoofGround) {
         e.setOnGround(true);
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "&7ground spoof (&3" + mc.thePlayer.ticksExisted + "&7).");
         }
      }

      this.spoofGround = false;
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public void onPreUpdate(PreUpdateEvent e) {
      if (this.startPacket) {
         mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.START_DESTROY_BLOCK, this.packetPos, EnumFacing.UP));
         this.swing();
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "sending c07 &astart &7break &7(&b" + mc.thePlayer.ticksExisted + "&7)");
         }
      }

      if (this.stopPacket) {
         mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(Action.STOP_DESTROY_BLOCK, this.packetPos, EnumFacing.UP));
         this.swing();
         if (keystrokesmod.Raven.debug) {
            Utils.sendModuleMessage(this, "sending c07 &cstop &7break &7(&b" + mc.thePlayer.ticksExisted + "&7)");
         }
      }

      if (this.isBreaking && !this.startPacket && !this.stopPacket) {
         this.swing();
      }

      this.startPacket = this.stopPacket = false;
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public void onRenderWorld(RenderWorldLastEvent e) {
      if (this.renderOutline.isToggled() && this.currentBlock != null && Utils.nullCheck()) {
         if (ModuleManager.bedESP != null && ModuleManager.bedESP.isEnabled()) {
            this.outlineColor = Theme.getGradient((int)ModuleManager.bedESP.theme.getInput(), 0.0);
         } else if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            this.outlineColor = Theme.getGradient((int)HUD.theme.getInput(), 0.0);
         } else {
            this.outlineColor = this.defaultOutlineColor;
         }

         RenderUtils.renderBlock(this.currentBlock, this.outlineColor, Arrays.asList(this.bedPos).contains(this.currentBlock) ? 0.5625 : 1.0, true, false);
      }
   }

   private void resetSlot() {
      if (keystrokesmod.Raven.packetsHandler != null
         && keystrokesmod.Raven.packetsHandler.playerSlot != null
         && Utils.nullCheck()
         && keystrokesmod.Raven.packetsHandler.playerSlot.get() != mc.thePlayer.inventory.currentItem
         && this.mode.getInput() == 2.0) {
         this.setPacketSlot(mc.thePlayer.inventory.currentItem);
      } else if (this.lastSlot != -1) {
         this.lastSlot = mc.thePlayer.inventory.currentItem = this.lastSlot;
      }
   }

   public boolean cancelKnockback() {
      return this.cancelKnockback.isToggled() && this.currentBlock != null && RotationUtils.inRange(this.currentBlock, this.range.getInput());
   }

   private BlockPos[] getBedPos() {
      int range;
      label40:
      for (int n = range = (int)this.range.getInput(); range >= -n; range--) {
         for (int j = -n; j <= n; j++) {
            for (int k = -n; k <= n; k++) {
               BlockPos blockPos = new BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + range, mc.thePlayer.posZ + k);
               IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
               if (getBlockState.getBlock() == Blocks.bed && getBlockState.getValue(BlockBed.PART) == EnumPartType.FOOT) {
                  float fov = (float)this.fov.getInput();
                  if (fov == 360.0F || Utils.inFov(fov, blockPos)) {
                     return new BlockPos[]{blockPos, blockPos.offset((EnumFacing)getBlockState.getValue(BlockBed.FACING))};
                  }
                  continue label40;
               }
            }
         }
      }

      return null;
   }

   private void setRots(ClientRotationEvent e) {
      float[] rotations = RotationUtils.getRotations(this.currentBlock == null ? this.rotateLastBlock : this.currentBlock, e.getYaw(), e.getPitch());
      e.setYaw(RotationUtils.applyVanilla(rotations[0]));
      e.setPitch(rotations[1]);
      if (keystrokesmod.Raven.debug) {
         Utils.sendModuleMessage(this, "&7rotating (&3" + mc.thePlayer.ticksExisted + "&7).");
      }
   }

   public BlockPos getBestBlock(BlockPos[] positions, boolean getSurrounding) {
      if (positions != null && positions.length != 0) {
         HashMap<BlockPos, double[]> blockMap = new HashMap<>();

         for (BlockPos pos : positions) {
            if (pos != null) {
               if (getSurrounding) {
                  for (EnumFacing enumFacing : EnumFacing.values()) {
                     if (enumFacing != EnumFacing.DOWN) {
                        BlockPos offset = pos.offset(enumFacing);
                        if (!Arrays.asList(positions).contains(offset) && RotationUtils.inRange(offset, this.range.getInput())) {
                           double efficiency = this.getEfficiency(offset);
                           double distance = mc.thePlayer.getDistanceSqToCenter(offset);
                           blockMap.put(offset, new double[]{distance, efficiency});
                        }
                     }
                  }
               } else if (RotationUtils.inRange(pos, this.range.getInput())) {
                  double efficiency = this.getEfficiency(pos);
                  double distance = mc.thePlayer.getDistanceSqToCenter(pos);
                  blockMap.put(pos, new double[]{distance, efficiency});
               }
            }
         }

         List<Entry<BlockPos, double[]>> sortedByDistance = this.sortByDistance(blockMap);
         List<Entry<BlockPos, double[]>> sortedByEfficiency = this.sortByEfficiency(sortedByDistance);
         List<Entry<BlockPos, double[]>> sortedByPreviousBlocks = this.sortByPreviousBlocks(sortedByEfficiency);
         return sortedByPreviousBlocks.isEmpty() ? null : sortedByPreviousBlocks.get(0).getKey();
      } else {
         return null;
      }
   }

   private List<Entry<BlockPos, double[]>> sortByDistance(HashMap<BlockPos, double[]> blockMap) {
      List<Entry<BlockPos, double[]>> list = new ArrayList<>(blockMap.entrySet());
      list.sort(Comparator.comparingDouble(entry -> entry.getValue()[0]));
      return list;
   }

   private List<Entry<BlockPos, double[]>> sortByEfficiency(List<Entry<BlockPos, double[]>> blockList) {
      blockList.sort((entry1, entry2) -> Double.compare(entry2.getValue()[1], entry1.getValue()[1]));
      return blockList;
   }

   private List<Entry<BlockPos, double[]>> sortByPreviousBlocks(List<Entry<BlockPos, double[]>> blockList) {
      blockList.sort((entry1, entry2) -> {
         boolean isEntry1Previous = entry1.getKey().equals(this.previousBlockBroken);
         boolean isEntry2Previous = entry2.getKey().equals(this.previousBlockBroken);
         if (isEntry1Previous && !isEntry2Previous) {
            return -1;
         } else {
            return !isEntry1Previous && isEntry2Previous ? 1 : 0;
         }
      });
      return blockList;
   }

   private double getEfficiency(BlockPos pos) {
      Block block = BlockUtils.getBlock(pos);
      ItemStack tool = this.mode.getInput() == 2.0 && Utils.getTool(block) != -1
         ? mc.thePlayer.inventory.getStackInSlot(Utils.getTool(block))
         : mc.thePlayer.getHeldItem();
      double efficiency = BlockUtils.getBlockHardness(block, tool, false, this.ignoreSlow);
      if (this.breakProgressMap.get(pos) != null) {
         efficiency = this.breakProgressMap.get(pos).floatValue();
      }

      return efficiency;
   }

   private void reset(boolean resetSlot, boolean stopAutoblock) {
      if (resetSlot) {
         this.resetSlot();
      }

      this.breakProgress = 0.0F;
      this.breakProgressMap.clear();
      this.lastSlot = -1;
      this.vanillaProgress = 0.0F;
      this.lastProgress = 0.0;
      if (stopAutoblock) {
         this.stopAutoblock = false;
      }

      this.rotateLastBlock = null;
      this.firstStop = false;
      if (this.isBreaking) {
         ModuleUtils.isBreaking = false;
         this.isBreaking = false;
      }

      this.breakTick = false;
      this.currentBlock = null;
      this.nearestBlock = null;
      this.ignoreSlow = false;
      this.delayStop = false;
      this.shouldUnblock = false;
   }

   public void setPacketSlot(int slot) {
      if (slot != -1) {
         keystrokesmod.Raven.packetsHandler.updateSlot(slot);
         this.stopAutoblock = true;
      }
   }

   private void startBreak(ClientRotationEvent e, BlockPos blockPos) {
      this.setRots(e);
      this.packetPos = blockPos;
      this.startPacket = true;
      this.isBreaking = true;
      this.breakTick = true;
      if (mc.thePlayer.motionY >= -0.4 && this.groundSpoof.isToggled()) {
         this.ignoreSlow = true;
         this.spoofGround = true;
      }

      this.ra = true;
   }

   private void stopBreak(ClientRotationEvent e, BlockPos blockPos) {
      this.setRots(e);
      this.packetPos = blockPos;
      this.stopPacket = true;
      this.isBreaking = false;
      this.breakTick = true;
      if (this.ignoreSlow) {
         this.spoofGround = true;
      }

      this.ignoreSlow = false;
      this.ra = true;
   }

   private void swing() {
      if (!this.silentSwing.isToggled()) {
         mc.thePlayer.swingItem();
      } else {
         mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
      }
   }

   private void breakBlock(ClientRotationEvent e, BlockPos blockPos) {
      if (blockPos == null) {
         this.reset(true, true);
      } else {
         this.lastBlock = blockPos;
         float fov = (float)this.fov.getInput();
         if (fov == 360.0F || Utils.inFov(fov, blockPos)) {
            if (!this.onlyWhileVisible.isToggled()
               || mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos().equals(blockPos)) {
               if (BlockUtils.replaceable(this.currentBlock == null ? blockPos : this.currentBlock)) {
                  this.reset(true, true);
               } else {
                  Block block = BlockUtils.getBlock(blockPos);
                  this.currentBlock = blockPos;
                  if ((this.breakProgress <= 0.0F || this.breakProgress >= 1.0F) && this.mode.getInput() == 2.0 && !this.firstStop) {
                     this.firstStop = true;
                     if (Utils.getTool(block) == -1) {
                        this.shouldUnblock = true;
                     }

                     this.stopAutoblock = this.delayStop = true;
                     this.setRots(e);
                  } else {
                     if (this.mode.getInput() != 2.0 && this.mode.getInput() != 0.0) {
                        if (this.mode.getInput() == 1.0) {
                           this.swing();
                           this.startBreak(e, blockPos);
                           this.setSlot(Utils.getTool(block));
                           this.stopBreak(e, blockPos);
                        }
                     } else {
                        if (this.breakProgress == 0.0F) {
                           this.resetSlot();
                           if (this.mode.getInput() == 0.0) {
                              this.setSlot(Utils.getTool(block));
                           }

                           this.startBreak(e, blockPos);
                        } else {
                           if (this.breakProgress >= 1.0F) {
                              if (this.mode.getInput() == 2.0) {
                                 this.setPacketSlot(Utils.getTool(block));
                              }

                              this.stopBreak(e, blockPos);
                              this.previousBlockBroken = this.currentBlock;
                              this.reset(false, false);
                              Iterator<Entry<BlockPos, Float>> iterator = this.breakProgressMap.entrySet().iterator();

                              while (iterator.hasNext()) {
                                 Entry<BlockPos, Float> entry = iterator.next();
                                 if (entry.getKey().equals(blockPos)) {
                                    iterator.remove();
                                 }
                              }

                              if (!this.disableBreakEffects.isToggled()) {
                                 mc.playerController.onPlayerDestroyBlock(blockPos, EnumFacing.UP);
                              }

                              this.rotateLastBlock = this.previousBlockBroken;
                              return;
                           }

                           if (this.mode.getInput() == 0.0) {
                           }
                        }

                        double progress = this.vanillaProgress = (float)(
                           BlockUtils.getBlockHardness(
                                 block,
                                 this.mode.getInput() == 2.0 && Utils.getTool(block) != -1
                                    ? mc.thePlayer.inventory.getStackInSlot(Utils.getTool(block))
                                    : mc.thePlayer.getHeldItem(),
                                 false,
                                 this.ignoreSlow
                              )
                              * this.breakSpeed.getInput()
                        );
                        if (this.lastProgress != 0.0
                           && this.breakProgress >= this.lastProgress - this.vanillaProgress
                           && this.breakProgress >= this.lastProgress
                           && this.mode.getInput() == 2.0) {
                           if (keystrokesmod.Raven.debug) {
                              Utils.sendModuleMessage(this, "&7setting slot &7(&b" + mc.thePlayer.ticksExisted + "&7)");
                           }

                           this.setPacketSlot(Utils.getTool(block));
                        }

                        this.breakProgress = (float)(this.breakProgress + progress);
                        this.breakProgressMap.put(blockPos, this.breakProgress);
                        if (this.breakProgress > 0.0F) {
                           this.firstStop = false;
                        }

                        if (this.sendAnimations.isToggled()) {
                           mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), blockPos, (int)(this.breakProgress * 10.0F - 1.0F));
                        }

                        this.lastProgress = 0.0;

                        while (this.lastProgress + progress < 1.0) {
                           this.lastProgress += progress;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void setSlot(int slot) {
      if (slot != -1 && slot != mc.thePlayer.inventory.currentItem) {
         if (this.lastSlot == -1) {
            this.lastSlot = mc.thePlayer.inventory.currentItem;
         }

         mc.thePlayer.inventory.currentItem = slot;
      }
   }

   private boolean isCovered(BlockPos blockPos) {
      for (EnumFacing enumFacing : EnumFacing.values()) {
         BlockPos offset = blockPos.offset(enumFacing);
         if (BlockUtils.replaceable(offset) || BlockUtils.notFull(BlockUtils.getBlock(offset))) {
            return false;
         }
      }

      return true;
   }
}
