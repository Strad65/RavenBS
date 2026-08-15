package keystrokesmod.module.impl.player;

import java.util.ArrayList;
import java.util.List;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import org.lwjgl.input.Mouse;

public class InvManager extends Module {
   private static SliderSetting stealerDelay;
   private static SliderSetting sortDelay;
   private static SliderSetting armorDelay;
   private static SliderSetting cleanerDelay;
   private SliderSetting swordSlot;
   private SliderSetting blocksSlot;
   private SliderSetting goldenAppleSlot;
   private SliderSetting projectileSlot;
   private SliderSetting speedSlot;
   private SliderSetting pearlSlot;
   private ButtonSetting autoArmor;
   private ButtonSetting autoSort;
   private ButtonSetting customChests;
   private ButtonSetting chestSteal;
   private ButtonSetting closeAfterStealing;
   private ButtonSetting invCleaner;
   private ButtonSetting clickToClean;
   private long ticks = 0L;
   private long nextDelay = 0L;
   private boolean closeGui = false;
   private double[] currentSword = new double[]{-1.0, -1.0};
   private InvManager.CurrentArmor[] armorArr = InvManager.CurrentArmor.values();

   public InvManager() {
      super("InvManager", Module.category.player);
      this.registerSetting(this.autoArmor = new ButtonSetting("Auto armor", false));
      this.registerSetting(armorDelay = new SliderSetting("Auto armor delay", 3.0, 1.0, 20.0, 1.0));
      this.registerSetting(this.autoSort = new ButtonSetting("Auto sort", false));
      this.registerSetting(sortDelay = new SliderSetting("Sort delay", 3.0, 1.0, 20.0, 1.0));
      this.registerSetting(this.chestSteal = new ButtonSetting("Steal chests", false));
      this.registerSetting(this.customChests = new ButtonSetting("Custom chest", false));
      this.registerSetting(this.closeAfterStealing = new ButtonSetting("Close after stealing", false));
      this.registerSetting(stealerDelay = new SliderSetting("Stealer delay", 3.0, 1.0, 20.0, 1.0));
      this.registerSetting(this.invCleaner = new ButtonSetting("Inventory cleaner", false));
      this.registerSetting(this.clickToClean = new ButtonSetting("Middle click to clean", false));
      this.registerSetting(cleanerDelay = new SliderSetting("Cleaner delay", 5.0, 1.0, 20.0, 1.0));
      this.registerSetting(this.swordSlot = new SliderSetting("Sword slot", true, -1.0, 1.0, 9.0, 1.0));
      this.registerSetting(this.blocksSlot = new SliderSetting("Blocks slot", true, -1.0, 1.0, 9.0, 1.0));
      this.registerSetting(this.goldenAppleSlot = new SliderSetting("Golden apple slot", true, -1.0, 1.0, 9.0, 1.0));
      this.registerSetting(this.projectileSlot = new SliderSetting("Projectile slot", true, -1.0, 1.0, 9.0, 1.0));
      this.registerSetting(this.speedSlot = new SliderSetting("Speed potion slot", true, -1.0, 1.0, 9.0, 1.0));
      this.registerSetting(this.pearlSlot = new SliderSetting("Pearl slot", true, -1.0, 1.0, 9.0, 1.0));
   }

   @Override
   public void onDisable() {
      this.nextDelay = 0L;
      this.ticks = 0L;
      this.closeGui = false;
   }

   @Override
   public void onUpdate() {
      if (mc.currentScreen != null) {
         if (this.closeAfterStealing.isToggled() && this.closeGui) {
            this.closeGui = false;
            mc.thePlayer.closeScreen();
         } else {
            long ticks = this.ticks + 1L;
            this.ticks = ticks;
            if (ticks >= this.nextDelay) {
               int slot2;
               label589: {
                  this.ticks = 0L;
                  if (mc.currentScreen instanceof GuiInventory) {
                     if (this.autoArmor.isToggled() || this.autoSort.isToggled() || this.invCleaner.isToggled()) {
                        this.updateCurrentArmor();
                        InvManager.InventoryData data = new InvManager.InventoryData(mc.thePlayer.inventory, true, !this.autoSort.isToggled());
                        if (this.autoArmor.isToggled()) {
                           for (int i = 0; i < data.armorData[0].length; i++) {
                              if (data.armorData[1][i] != -1) {
                                 InvManager.CurrentArmor currentArmor = this.armorArr[i];
                                 if (data.armorData[1][i] > currentArmor.defenceLevel) {
                                    if (currentArmor.getItemStack() != null) {
                                       this.guiClick(currentArmor.invSlot, 0, 4, InvManager.Delay.AUTOARMOR);
                                    }

                                    int slot = data.armorData[0][i];
                                    this.guiClick(slot < 9 ? slot + 36 : slot, 0, 1, InvManager.Delay.AUTOARMOR);
                                    return;
                                 }
                              }
                           }
                        }

                        if (this.autoSort.isToggled()) {
                           if (this.fixSwordSlot(data, true)) {
                              return;
                           }

                           for (int i = 0; i < data.size; i++) {
                              ItemStack itemStack = data.inventory.getStackInSlot(i);
                              if (itemStack != null && !this.isSword(itemStack) && !this.isArmor(itemStack)) {
                                 if (itemStack.getItem() instanceof ItemBlock) {
                                    int slot = (int)(this.blocksSlot.getInput() - 1.0);
                                    if (slot <= -1 && i != slot) {
                                       ItemStack currentBlocks = data.inventory.getStackInSlot(slot);
                                       slot2 = i < 9 ? i + 36 : i;
                                       if (currentBlocks == null || !(currentBlocks.getItem() instanceof ItemBlock)) {
                                          this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                          return;
                                       }

                                       if (currentBlocks.stackSize < 64) {
                                          if (itemStack.getItem() == currentBlocks.getItem()
                                             && itemStack.getMetadata() == currentBlocks.getMetadata()) {
                                             if (itemStack.stackSize == 64) {
                                                this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                                return;
                                             }

                                             if (data.emptyWithoutHotbar != 0 || i == slot2) {
                                                this.guiClick(slot2, 0, 1, InvManager.Delay.SORT);
                                                return;
                                             }
                                          } else if (itemStack.stackSize > currentBlocks.stackSize) {
                                             this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                             return;
                                          }
                                       }
                                    }
                                 } else if (itemStack.getItem() instanceof ItemAppleGold) {
                                    int slot = (int)(this.goldenAppleSlot.getInput() - 1.0);
                                    if (slot <= -1 && i != slot) {
                                       ItemStack currentGoldenApple = data.inventory.getStackInSlot(slot);
                                       if (currentGoldenApple == null || !(currentGoldenApple.getItem() instanceof ItemAppleGold)) {
                                          slot2 = i < 9 ? i + 36 : i;
                                          this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                          return;
                                       }
                                    }
                                 } else if (!(itemStack.getItem() instanceof ItemSnowball) && !(itemStack.getItem() instanceof ItemEgg)) {
                                    if (itemStack.getItem() instanceof ItemPotion) {
                                       int slot = (int)(this.speedSlot.getInput() - 1.0);
                                       if (slot <= -1 && i != slot && this.isSpeedPotion(itemStack)) {
                                          ItemStack currentPotion = data.inventory.getStackInSlot(slot);
                                          if (!this.isSpeedPotion(currentPotion)) {
                                             slot2 = i < 9 ? i + 36 : i;
                                             this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                             return;
                                          }
                                       }
                                    } else if (itemStack.getItem() instanceof ItemEnderPearl) {
                                       int slot = (int)(this.pearlSlot.getInput() - 1.0);
                                       if (slot <= -1 && i != slot) {
                                          ItemStack currentPearl = data.inventory.getStackInSlot(slot);
                                          if (currentPearl == null || !(currentPearl.getItem() instanceof ItemEnderPearl)) {
                                             slot2 = i < 9 ? i + 36 : i;
                                             this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                             return;
                                          }
                                       }
                                    }
                                 } else {
                                    int slot = (int)(this.projectileSlot.getInput() - 1.0);
                                    if (slot <= -1 && i != slot) {
                                       ItemStack currentProjectile = data.inventory.getStackInSlot(slot);
                                       slot2 = i < 9 ? i + 36 : i;
                                       if (currentProjectile == null
                                          || !(currentProjectile.getItem() instanceof ItemSnowball)
                                             && !(currentProjectile.getItem() instanceof ItemEgg)) {
                                          this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                          return;
                                       }

                                       if (itemStack.stackSize >= 16) {
                                          if (itemStack.stackSize > currentProjectile.stackSize) {
                                             this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                             return;
                                          }
                                       } else if (currentProjectile.stackSize < 16) {
                                          if (itemStack.getItem() == currentProjectile.getItem()) {
                                             if (data.emptyWithoutHotbar != 0 || i == slot2) {
                                                this.guiClick(slot2, 0, 1, InvManager.Delay.SORT);
                                                return;
                                             }
                                          } else if (itemStack.stackSize > currentProjectile.stackSize) {
                                             this.guiClick(slot2, slot, 2, InvManager.Delay.SORT);
                                             return;
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        if (this.invCleaner.isToggled()) {
                           if (this.clickToClean.isToggled() && !Mouse.isButtonDown(2)) {
                              return;
                           }

                           List<Integer> duplicateItems = new ArrayList<>();

                           for (int j = 0; j < data.size; j++) {
                              ItemStack itemStack2 = data.inventory.getStackInSlot(j);
                              if (itemStack2 != null) {
                                 Item item = itemStack2.getItem();
                                 slot2 = j < 9 ? j + 36 : j;
                                 if (this.isSword(itemStack2)) {
                                    if (!(this.currentSword[1] <= Utils.getDamageLevel(itemStack2))) {
                                       break label589;
                                    }
                                 } else if (this.isArmor(itemStack2)) {
                                    ItemArmor armor = (ItemArmor)item;
                                    int armorSlot = 3 - armor.armorType;
                                    int defenceLevel = this.getDefenceLevel(itemStack2);
                                    if (this.armorArr[armorSlot].defenceLevel > defenceLevel) {
                                       break label589;
                                    }
                                 } else if (!(item instanceof ItemBlock)
                                    && !(item instanceof ItemAppleGold)
                                    && !(item instanceof ItemSnowball)
                                    && !(item instanceof ItemEgg)
                                    && !(item instanceof ItemEnderPearl)
                                    && item != Items.arrow
                                    && item != Items.spawn_egg) {
                                    if (!(item instanceof ItemPotion)) {
                                       if (itemStack2.getMaxStackSize() != 1) {
                                          break label589;
                                       }

                                       int id = Item.getIdFromItem(item);
                                       if (duplicateItems.contains(id)) {
                                          break label589;
                                       }

                                       duplicateItems.add(id);
                                    } else {
                                       boolean shitPotion = false;

                                       for (PotionEffect effect : ((ItemPotion)item).getEffects(itemStack2)) {
                                          String desc = effect.toString();
                                          if (desc.contains("poison")) {
                                             shitPotion = true;
                                             break;
                                          }
                                       }

                                       if (shitPotion) {
                                          break label589;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  } else if (mc.currentScreen instanceof GuiChest && this.chestSteal.isToggled()) {
                     IInventory chestInventory = ((ContainerChest)mc.thePlayer.openContainer).getLowerChestInventory();
                     if (!this.customChests.isToggled() && !chestInventory.getName().contains("Chest")) {
                        return;
                     }

                     this.updateCurrentArmor();
                     InvManager.InventoryData chestData = new InvManager.InventoryData(chestInventory, false, false);
                     InvManager.InventoryData playerData = new InvManager.InventoryData(mc.thePlayer.inventory, true, false);
                     chestData.compareAndRemove(playerData);
                     if (playerData.size != playerData.filled) {
                        if (this.fixSwordSlot(chestData, false)) {
                           return;
                        }

                        for (int k = 0; k < chestData.armorData[0].length; k++) {
                           if (chestData.armorData[1][k] != -1) {
                              InvManager.CurrentArmor currentArmor2 = this.armorArr[k];
                              if (chestData.armorData[1][k] > currentArmor2.defenceLevel) {
                                 this.guiClick(chestData.armorData[0][k], 0, 1, InvManager.Delay.STEALER);
                                 return;
                              }
                           }
                        }

                        for (int k = 0; k < chestData.size; k++) {
                           ItemStack itemStack3 = chestData.inventory.getStackInSlot(k);
                           if (itemStack3 != null && !this.isSword(itemStack3) && !this.isArmor(itemStack3)) {
                              if (itemStack3.getItem() instanceof ItemBlock) {
                                 slot2 = (int)(this.blocksSlot.getInput() - 1.0);
                                 if (slot2 <= -1) {
                                    this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                    return;
                                 }

                                 ItemStack currentBlocks2 = playerData.inventory.getStackInSlot(slot2);
                                 if (currentBlocks2 == null
                                    || !(currentBlocks2.getItem() instanceof ItemBlock)
                                    || ((ItemBlock)itemStack3.getItem()).getBlock() != ((ItemBlock)currentBlocks2.getItem()).getBlock()
                                       && itemStack3.stackSize > currentBlocks2.stackSize) {
                                    this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                    return;
                                 }
                              } else if (itemStack3.getItem() instanceof ItemAppleGold) {
                                 slot2 = (int)(this.goldenAppleSlot.getInput() - 1.0);
                                 if (slot2 <= -1) {
                                    this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                    return;
                                 }

                                 ItemStack currentGoldenApple2 = playerData.inventory.getStackInSlot(slot2);
                                 if (currentGoldenApple2 == null || !(currentGoldenApple2.getItem() instanceof ItemAppleGold)) {
                                    this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                    return;
                                 }
                              } else if (itemStack3.getItem() instanceof ItemSnowball || itemStack3.getItem() instanceof ItemEgg) {
                                 slot2 = (int)(this.projectileSlot.getInput() - 1.0);
                                 if (slot2 <= -1) {
                                    this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                    return;
                                 }

                                 ItemStack currentProjectile2 = playerData.inventory.getStackInSlot(slot2);
                                 if (currentProjectile2 == null
                                    || !(currentProjectile2.getItem() instanceof ItemSnowball) && !(currentProjectile2.getItem() instanceof ItemEgg)) {
                                    this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                    return;
                                 }

                                 if (itemStack3.stackSize > currentProjectile2.stackSize) {
                                    if (itemStack3.getItem() == currentProjectile2.getItem()) {
                                       if (itemStack3.stackSize <= 16) {
                                          this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                       } else {
                                          this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                       }
                                    } else {
                                       this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                    }

                                    return;
                                 }
                              } else if (itemStack3.getItem() instanceof ItemPotion) {
                                 if (this.isSpeedPotion(itemStack3)) {
                                    slot2 = (int)(this.speedSlot.getInput() - 1.0);
                                    if (slot2 <= -1) {
                                       this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                       return;
                                    }

                                    ItemStack currentPotion2 = playerData.inventory.getStackInSlot(slot2);
                                    if (!this.isSpeedPotion(currentPotion2)) {
                                       this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                       return;
                                    }
                                 }
                              } else if (itemStack3.getItem() instanceof ItemEnderPearl) {
                                 slot2 = (int)(this.pearlSlot.getInput() - 1.0);
                                 if (slot2 <= -1) {
                                    this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                    return;
                                 }

                                 ItemStack currentPearl2 = playerData.inventory.getStackInSlot(slot2);
                                 if (currentPearl2 == null || !(currentPearl2.getItem() instanceof ItemEnderPearl)) {
                                    this.guiClick(k, slot2, 2, InvManager.Delay.STEALER);
                                    return;
                                 }
                              }

                              this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                              return;
                           }
                        }
                     } else {
                        for (int k = 0; k < chestData.size; k++) {
                           ItemStack itemStack3 = chestData.inventory.getStackInSlot(k);
                           if (itemStack3 != null
                              && (
                                 itemStack3.getItem() instanceof ItemBlock
                                    || itemStack3.getItem() instanceof ItemAppleGold
                                    || itemStack3.getItem() instanceof ItemSnowball
                                    || itemStack3.getItem() instanceof ItemEgg
                                    || itemStack3.getItem() instanceof ItemEnderPearl
                              )) {
                              for (int l = 0; l < playerData.size; l++) {
                                 ItemStack itemStack4 = playerData.inventory.getStackInSlot(l);
                                 if (itemStack4 != null
                                    && itemStack3.getItem() == itemStack4.getItem()
                                    && itemStack4.stackSize < itemStack4.getMaxStackSize()
                                    && (
                                       !(itemStack3.getItem() instanceof ItemBlock)
                                          || ((ItemBlock)itemStack3.getItem()).getBlock() == ((ItemBlock)itemStack4.getItem()).getBlock()
                                    )) {
                                    this.guiClick(k, 0, 1, InvManager.Delay.STEALER);
                                    return;
                                 }
                              }
                           }
                        }
                     }

                     if (this.closeAfterStealing.isToggled()) {
                        this.closeGui = true;
                     }
                  }

                  return;
               }

               this.guiClick(slot2, 1, 4, InvManager.Delay.CLEANER);
            }
         }
      }
   }

   private boolean isSword(ItemStack itemStack) {
      return itemStack.getItem() instanceof ItemSword;
   }

   private boolean fixSwordSlot(InvManager.InventoryData data, boolean playerInventory) {
      if (this.swordSlot.getInput() != 0.0 && data.swordData[1] != -1.0 && data.swordData[1] > this.currentSword[1]) {
         int slot = (int)data.swordData[0];
         if (playerInventory) {
            slot = slot < 9 ? slot + 36 : slot;
         }

         this.guiClick(slot, (int)(this.swordSlot.getInput() - 1.0), 2, InvManager.Delay.SORT);
         return true;
      } else {
         return false;
      }
   }

   private boolean isArmor(ItemStack itemStack) {
      return itemStack.getItem() instanceof ItemArmor;
   }

   private int getDefenceLevel(ItemStack itemStack) {
      return ((ItemArmor)itemStack.getItem()).damageReduceAmount + EnchantmentHelper.getEnchantmentModifierDamage(new ItemStack[]{itemStack}, DamageSource.generic);
   }

   private void updateCurrentArmor() {
      for (InvManager.CurrentArmor armor : this.armorArr) {
         ItemStack itemStack = armor.getItemStack();
         if (itemStack != null && this.isArmor(itemStack)) {
            armor.defenceLevel = this.getDefenceLevel(itemStack);
         } else {
            armor.defenceLevel = 0;
         }
      }
   }

   private boolean isSpeedPotion(ItemStack itemStack) {
      if (itemStack != null && itemStack.getItem() instanceof ItemPotion) {
         for (PotionEffect effect : ((ItemPotion)itemStack.getItem()).getEffects(itemStack)) {
            String desc = effect.toString();
            if (desc.contains("moveSpeed")) {
               return true;
            }
         }
      }

      return false;
   }

   private void guiClick(int slot, int mouse, int mode, InvManager.Delay delayType) {
      this.nextDelay = (long)delayType.slider.getInput();
      guiClick(slot, mouse, mode);
   }

   public static void guiClick(int slot, int mouse, int mode) {
      mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, mouse, mode, mc.thePlayer);
   }

   enum CurrentArmor {
      BOOTS(0, 8),
      LEGGINGS(1, 7),
      CHESTPLATE(2, 6),
      HELMET(3, 5);

      int slot;
      int invSlot;
      int defenceLevel;

      CurrentArmor(int slot, int invSlot) {
         this.slot = slot;
         this.invSlot = invSlot;
      }

      public ItemStack getItemStack() {
         return InvManager.mc.thePlayer.inventory.armorItemInSlot(this.slot);
      }
   }

   enum Delay {
      AUTOARMOR(InvManager.armorDelay),
      SORT(InvManager.sortDelay),
      STEALER(InvManager.stealerDelay),
      CLEANER(InvManager.cleanerDelay);

      SliderSetting slider;

      Delay(SliderSetting slider) {
         this.slider = slider;
      }
   }

   class InventoryData {
      IInventory inventory;
      int size;
      int filled;
      int emptyWithoutHotbar;
      double[] swordData = new double[]{-1.0, -1.0};
      int[][] armorData = new int[][]{{-1, -1, -1, -1}, {-1, -1, -1, -1}};

      InventoryData(IInventory inventory, boolean playerInventory, boolean armorOnly) {
         this.inventory = inventory;
         this.size = playerInventory ? inventory.getSizeInventory() - 4 : inventory.getSizeInventory();
         int currentSwordSlot = (int)(InvManager.this.swordSlot.getInput() - 1.0) + (playerInventory ? 0 : (this.size == 54 ? 81 : 54));

         for (int i = 0; i < this.size; i++) {
            ItemStack itemStack = inventory.getStackInSlot(i);
            if (itemStack != null) {
               this.filled++;
            } else if (i >= 9) {
               this.emptyWithoutHotbar++;
            }

            if (!armorOnly) {
               if (i == currentSwordSlot) {
                  if (itemStack != null && InvManager.this.isSword(itemStack)) {
                     InvManager.this.currentSword[1] = Utils.getDamageLevel(itemStack);
                     InvManager.this.currentSword[0] = i;
                     continue;
                  }

                  InvManager.this.currentSword[0] = InvManager.this.currentSword[1] = -1.0;
               } else if (itemStack != null && InvManager.this.isSword(itemStack)) {
                  double damageLevel = Utils.getDamageLevel(itemStack);
                  if (damageLevel > this.swordData[1]) {
                     this.swordData[1] = damageLevel;
                     this.swordData[0] = i;
                  }
                  continue;
               }
            }

            if (itemStack != null && InvManager.this.isArmor(itemStack)) {
               ItemArmor armor = (ItemArmor)itemStack.getItem();
               int slot = 3 - armor.armorType;
               int defenceLevel = InvManager.this.getDefenceLevel(itemStack);
               if (defenceLevel > this.armorData[1][slot]) {
                  this.armorData[1][slot] = defenceLevel;
                  this.armorData[0][slot] = i;
               }
            }
         }
      }

      void compareAndRemove(InvManager.InventoryData data) {
         if (data.swordData[1] > this.swordData[1]) {
            this.swordData[1] = -1.0;
         }

         for (int i = 0; i < this.armorData[0].length; i++) {
            if (data.armorData[1][i] > this.armorData[1][i]) {
               this.armorData[1][i] = -1;
            }
         }
      }
   }
}
